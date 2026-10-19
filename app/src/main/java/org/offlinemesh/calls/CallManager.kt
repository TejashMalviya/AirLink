package org.offlinemesh.calls

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.offlinemesh.audio.AudioEngine
import org.offlinemesh.identity.IdentityManager
import org.offlinemesh.mesh.MeshNeighbor
import org.offlinemesh.mesh.MeshPacketDeliveryCallback
import org.offlinemesh.mesh.MeshRouter
import org.offlinemesh.security.CryptoManager
import org.offlinemesh.service.ActiveCallService
import org.offlinemesh.transport.MeshPacket
import org.offlinemesh.transport.PacketType
import java.security.KeyPair
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.SecretKey

sealed interface CallState {
    data object Idle : CallState
    data class OutgoingRinging(
        val callId: String,
        val peerDeviceId: String,
        val peerName: String,
        val hopCount: Int = 1
    ) : CallState
    data class IncomingRinging(
        val callId: String,
        val peerDeviceId: String,
        val peerName: String,
        val hopCount: Int = 1
    ) : CallState
    data class Connected(
        val callId: String,
        val peerDeviceId: String,
        val peerName: String,
        val durationSeconds: Long = 0,
        val hopCount: Int = 1,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false,
        val isGroupCall: Boolean = false,
        val groupParticipants: List<String> = emptyList()
    ) : CallState
    data class GroupCallRinging(
        val callId: String,
        val initiatorDeviceId: String,
        val initiatorName: String,
        val participants: List<String>    // device IDs invited
    ) : CallState
}

class CallManager(
    private val context: Context,
    private val identityManager: IdentityManager,
    private val meshRouter: MeshRouter,
    val audioEngine: AudioEngine,
    private val cryptoManager: CryptoManager
) : MeshPacketDeliveryCallback {

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var myEphemeralKeyPair: KeyPair? = null
    private var sessionKey: SecretKey? = null
    private var callTimerJob: Job? = null
    private val audioSeqCounter = AtomicLong(0)

    // Group call: per-participant acceptance tracking
    private val groupParticipants = ConcurrentHashMap<String, Boolean>()

    init {
        meshRouter.addDeliveryCallback(this)

        audioEngine.onFrameReadyToSend = { compressedAudio ->
            val current = _callState.value
            if (current is CallState.Connected && sessionKey != null) {
                val encrypted = cryptoManager.encryptPayload(sessionKey!!, compressedAudio)
                val base64Payload = Base64.encodeToString(encrypted, Base64.NO_WRAP)
                val seq = audioSeqCounter.incrementAndGet()

                if (current.isGroupCall) {
                    // Fan-out audio to all group participants
                    current.groupParticipants.forEach { peerId ->
                        val audioPacket = MeshPacket(
                            type = PacketType.AUDIO_FRAME,
                            sourceDeviceId = identityManager.currentIdentity.deviceId,
                            sourceDisplayName = identityManager.currentIdentity.displayName,
                            destinationDeviceId = peerId,
                            callId = current.callId,
                            sequenceNumber = seq,
                            payload = base64Payload
                        )
                        meshRouter.sendOrRoute(audioPacket)
                    }
                } else {
                    val audioPacket = MeshPacket(
                        type = PacketType.AUDIO_FRAME,
                        sourceDeviceId = identityManager.currentIdentity.deviceId,
                        sourceDisplayName = identityManager.currentIdentity.displayName,
                        destinationDeviceId = current.peerDeviceId,
                        callId = current.callId,
                        sequenceNumber = seq,
                        payload = base64Payload
                    )
                    meshRouter.sendOrRoute(audioPacket)
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    // 1:1 Call
    // ──────────────────────────────────────────────

    fun startCall(peer: MeshNeighbor) {
        if (_callState.value !is CallState.Idle) return

        val callId = UUID.randomUUID().toString().substring(0, 8)
        val keyPair = cryptoManager.generateEphemeralKeyPair()
        myEphemeralKeyPair = keyPair
        sessionKey = null
        audioSeqCounter.set(0)

        val pubKeyBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val myId = identityManager.currentIdentity

        val invitePacket = MeshPacket(
            type = PacketType.CALL_INVITE,
            sourceDeviceId = myId.deviceId,
            sourceDisplayName = myId.displayName,
            destinationDeviceId = peer.deviceId,
            callId = callId,
            payload = pubKeyBase64
        )

        _callState.value = CallState.OutgoingRinging(
            callId = callId,
            peerDeviceId = peer.deviceId,
            peerName = peer.displayName
        )

        meshRouter.sendOrRoute(invitePacket)

        CoroutineScope(Dispatchers.Main).launch {
            delay(30_000)
            if (_callState.value is CallState.OutgoingRinging) endCall()
        }
    }

    // ──────────────────────────────────────────────
    // Group Call
    // ──────────────────────────────────────────────

    fun startGroupCall(peers: List<MeshNeighbor>) {
        if (_callState.value !is CallState.Idle) return
        if (peers.isEmpty()) return

        val callId = UUID.randomUUID().toString().substring(0, 8)
        val keyPair = cryptoManager.generateEphemeralKeyPair()
        myEphemeralKeyPair = keyPair
        // Group uses callId-derived shared key for simplicity
        sessionKey = cryptoManager.deriveGroupKey(callId)
        audioSeqCounter.set(0)
        groupParticipants.clear()

        val pubKeyBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val myId = identityManager.currentIdentity
        val participantIds = peers.map { it.deviceId }

        peers.forEach { peer ->
            groupParticipants[peer.deviceId] = false
            val invitePacket = MeshPacket(
                type = PacketType.CALL_INVITE,
                sourceDeviceId = myId.deviceId,
                sourceDisplayName = myId.displayName,
                destinationDeviceId = peer.deviceId,
                callId = callId,
                payload = "GROUP:$pubKeyBase64:${participantIds.joinToString(",")}"
            )
            meshRouter.sendOrRoute(invitePacket)
        }

        // Start connected immediately; others join as they accept
        _callState.value = CallState.Connected(
            callId = callId,
            peerDeviceId = peers.first().deviceId,
            peerName = peers.joinToString(", ") { it.displayName },
            isGroupCall = true,
            groupParticipants = participantIds
        )
        audioEngine.startAudioSession()
        ActiveCallService.start(context, "Group Call (${peers.size} peers)")
        startCallTimer(callId)
    }

    fun acceptIncomingCall() {
        val state = _callState.value
        if (state !is CallState.IncomingRinging) return

        val myKeyPair = cryptoManager.generateEphemeralKeyPair()
        myEphemeralKeyPair = myKeyPair
        val myPubBase64 = Base64.encodeToString(myKeyPair.public.encoded, Base64.NO_WRAP)

        val acceptPacket = MeshPacket(
            type = PacketType.CALL_ACCEPT,
            sourceDeviceId = identityManager.currentIdentity.deviceId,
            sourceDisplayName = identityManager.currentIdentity.displayName,
            destinationDeviceId = state.peerDeviceId,
            callId = state.callId,
            payload = myPubBase64
        )

        meshRouter.sendOrRoute(acceptPacket)
        transitionToConnected(state.callId, state.peerDeviceId, state.peerName, state.hopCount)
    }

    fun acceptGroupCallRinging() {
        val state = _callState.value
        if (state !is CallState.GroupCallRinging) return

        val myKeyPair = cryptoManager.generateEphemeralKeyPair()
        myEphemeralKeyPair = myKeyPair
        sessionKey = cryptoManager.deriveGroupKey(state.callId)
        val myPubBase64 = Base64.encodeToString(myKeyPair.public.encoded, Base64.NO_WRAP)

        val acceptPacket = MeshPacket(
            type = PacketType.CALL_ACCEPT,
            sourceDeviceId = identityManager.currentIdentity.deviceId,
            sourceDisplayName = identityManager.currentIdentity.displayName,
            destinationDeviceId = state.initiatorDeviceId,
            callId = state.callId,
            payload = myPubBase64
        )
        meshRouter.sendOrRoute(acceptPacket)

        _callState.value = CallState.Connected(
            callId = state.callId,
            peerDeviceId = state.initiatorDeviceId,
            peerName = state.initiatorName,
            isGroupCall = true,
            groupParticipants = state.participants
        )
        audioEngine.startAudioSession()
        ActiveCallService.start(context, "Group Call")
        startCallTimer(state.callId)
    }

    fun rejectIncomingCall() {
        val state = _callState.value
        val (peerId, cId) = when (state) {
            is CallState.IncomingRinging -> state.peerDeviceId to state.callId
            is CallState.GroupCallRinging -> state.initiatorDeviceId to state.callId
            else -> return
        }

        val rejectPacket = MeshPacket(
            type = PacketType.CALL_REJECT,
            sourceDeviceId = identityManager.currentIdentity.deviceId,
            sourceDisplayName = identityManager.currentIdentity.displayName,
            destinationDeviceId = peerId,
            callId = cId
        )
        meshRouter.sendOrRoute(rejectPacket)
        cleanupCall()
    }

    fun endCall() {
        val state = _callState.value
        val (peerId, cId) = when (state) {
            is CallState.Connected -> state.peerDeviceId to state.callId
            is CallState.OutgoingRinging -> state.peerDeviceId to state.callId
            is CallState.IncomingRinging -> state.peerDeviceId to state.callId
            is CallState.GroupCallRinging -> state.initiatorDeviceId to state.callId
            CallState.Idle -> return
        }

        val endPacket = MeshPacket(
            type = PacketType.CALL_END,
            sourceDeviceId = identityManager.currentIdentity.deviceId,
            sourceDisplayName = identityManager.currentIdentity.displayName,
            destinationDeviceId = peerId,
            callId = cId
        )
        meshRouter.sendOrRoute(endPacket)
        cleanupCall()
    }

    fun toggleMute() {
        val state = _callState.value
        if (state is CallState.Connected) {
            val newMute = !state.isMuted
            audioEngine.setMute(newMute)
            _callState.value = state.copy(isMuted = newMute)
        }
    }

    fun toggleSpeaker() {
        val state = _callState.value
        if (state is CallState.Connected) {
            val newSpeaker = !state.isSpeakerOn
            audioEngine.setSpeakerphone(newSpeaker)
            _callState.value = state.copy(isSpeakerOn = newSpeaker)
        }
    }

    override fun onLocalPacketDelivered(packet: MeshPacket) {
        when (packet.type) {
            PacketType.CALL_INVITE -> {
                if (_callState.value !is CallState.Idle) {
                    val busyPacket = MeshPacket(
                        type = PacketType.CALL_REJECT,
                        sourceDeviceId = identityManager.currentIdentity.deviceId,
                        sourceDisplayName = identityManager.currentIdentity.displayName,
                        destinationDeviceId = packet.sourceDeviceId,
                        callId = packet.callId,
                        payload = "BUSY"
                    )
                    meshRouter.sendOrRoute(busyPacket)
                    return
                }

                val payload = packet.payload
                if (payload.startsWith("GROUP:")) {
                    val parts = payload.removePrefix("GROUP:").split(":")
                    val pubKeyB64 = parts.getOrNull(0) ?: ""
                    val participantIds = parts.getOrNull(1)?.split(",") ?: emptyList()
                    pendingPeerPubKey = Base64.decode(pubKeyB64, Base64.NO_WRAP)
                    _callState.value = CallState.GroupCallRinging(
                        callId = packet.callId,
                        initiatorDeviceId = packet.sourceDeviceId,
                        initiatorName = packet.sourceDisplayName,
                        participants = participantIds
                    )
                } else {
                    val peerPubBytes = Base64.decode(payload, Base64.NO_WRAP)
                    pendingPeerPubKey = peerPubBytes
                    _callState.value = CallState.IncomingRinging(
                        callId = packet.callId,
                        peerDeviceId = packet.sourceDeviceId,
                        peerName = packet.sourceDisplayName,
                        hopCount = packet.hopCount + 1
                    )
                }
            }
            PacketType.CALL_ACCEPT -> {
                val state = _callState.value
                if (state is CallState.OutgoingRinging && state.callId == packet.callId) {
                    val peerPubBytes = Base64.decode(packet.payload, Base64.NO_WRAP)
                    if (myEphemeralKeyPair != null) {
                        sessionKey = cryptoManager.deriveSharedSecret(myEphemeralKeyPair!!.private, peerPubBytes)
                    }
                    transitionToConnected(state.callId, state.peerDeviceId, state.peerName, packet.hopCount + 1)
                } else if (state is CallState.Connected && state.isGroupCall && state.callId == packet.callId) {
                    // Group participant accepted
                    groupParticipants[packet.sourceDeviceId] = true
                }
            }
            PacketType.CALL_REJECT, PacketType.CALL_END -> {
                cleanupCall()
            }
            PacketType.AUDIO_FRAME -> {
                val state = _callState.value
                if (state is CallState.Connected && state.callId == packet.callId && sessionKey != null) {
                    val encrypted = Base64.decode(packet.payload, Base64.NO_WRAP)
                    val decrypted = cryptoManager.decryptPayload(sessionKey!!, encrypted)
                    if (decrypted != null) {
                        audioEngine.onIncomingAudioFrame(packet.sequenceNumber, decrypted)
                    }
                }
            }
            else -> {}
        }
    }

    private var pendingPeerPubKey: ByteArray? = null

    private fun transitionToConnected(callId: String, peerId: String, peerName: String, hopCount: Int) {
        if (pendingPeerPubKey != null && myEphemeralKeyPair != null && sessionKey == null) {
            sessionKey = cryptoManager.deriveSharedSecret(myEphemeralKeyPair!!.private, pendingPeerPubKey!!)
            pendingPeerPubKey = null
        }

        _callState.value = CallState.Connected(
            callId = callId,
            peerDeviceId = peerId,
            peerName = peerName,
            hopCount = hopCount,
            durationSeconds = 0
        )

        audioEngine.startAudioSession()
        ActiveCallService.start(context, peerName)
        startCallTimer(callId)
    }

    private fun startCallTimer(callId: String) {
        callTimerJob?.cancel()
        callTimerJob = CoroutineScope(Dispatchers.Main).launch {
            var seconds = 0L
            while (isActive && _callState.value is CallState.Connected) {
                delay(1000)
                seconds++
                val curr = _callState.value
                if (curr is CallState.Connected) {
                    _callState.value = curr.copy(durationSeconds = seconds)
                }
            }
        }
    }

    private fun cleanupCall() {
        callTimerJob?.cancel()
        callTimerJob = null
        audioEngine.stopAudioSession()
        ActiveCallService.stop(context)
        myEphemeralKeyPair = null
        sessionKey = null
        pendingPeerPubKey = null
        groupParticipants.clear()
        _callState.value = CallState.Idle
    }

    override fun onNeighborTableChanged(neighbors: List<MeshNeighbor>) {}
    override fun onRouteDiscovered(destinationDeviceId: String, hopCount: Int) {}
}
