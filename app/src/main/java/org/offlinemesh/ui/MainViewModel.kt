package org.offlinemesh.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.offlinemesh.audio.AudioEngine
import org.offlinemesh.calls.CallManager
import org.offlinemesh.calls.CallState
import org.offlinemesh.chat.ChatManager
import org.offlinemesh.discovery.NearbyConnectionsManager
import org.offlinemesh.identity.IdentityManager
import org.offlinemesh.mesh.MeshNeighbor
import org.offlinemesh.mesh.MeshRouter
import org.offlinemesh.security.CryptoManager

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val identityManager = IdentityManager(application)
    val nearbyManager = NearbyConnectionsManager(application, identityManager)
    val meshRouter = MeshRouter(identityManager, nearbyManager)
    val audioEngine = AudioEngine(application)
    val cryptoManager = CryptoManager()
    val callManager = CallManager(application, identityManager, meshRouter, audioEngine, cryptoManager)
    val chatManager = ChatManager(identityManager, meshRouter)

    val isMeshActive = nearbyManager.isMeshActive
    val knownPeers = meshRouter.knownPeers
    val callState = callManager.callState
    val diagnostics = meshRouter.diagnostics
    val messages = chatManager.messages

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _relayEnabled = MutableStateFlow(identityManager.isRelayEnabled)
    val relayEnabled: StateFlow<Boolean> = _relayEnabled.asStateFlow()

    private val _onboardingDone = MutableStateFlow(identityManager.hasCustomDisplayName)
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _isDarkMode = MutableStateFlow(identityManager.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        identityManager.isDarkMode = next
        _isDarkMode.value = next
    }

    fun completeOnboarding(displayName: String) {
        identityManager.updateDisplayName(displayName)
        _onboardingDone.value = true
        if (isMeshActive.value) {
            meshRouter.broadcastHello()
        }
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun toggleMesh() {
        if (isMeshActive.value) {
            nearbyManager.stopMesh()
        } else {
            nearbyManager.startMesh()
        }
    }

    fun startCall(peer: MeshNeighbor) {
        callManager.startCall(peer)
    }

    fun startGroupCall(peers: List<MeshNeighbor>) {
        callManager.startGroupCall(peers)
    }

    fun acceptCall() {
        when (callState.value) {
            is CallState.IncomingRinging -> callManager.acceptIncomingCall()
            is CallState.GroupCallRinging -> callManager.acceptGroupCallRinging()
            else -> {}
        }
    }

    fun rejectCall() {
        callManager.rejectIncomingCall()
    }

    fun endCall() {
        callManager.endCall()
    }

    fun toggleMute() {
        callManager.toggleMute()
    }

    fun toggleSpeaker() {
        callManager.toggleSpeaker()
    }

    fun sendMessage(recipientDeviceId: String, text: String) {
        if (text.isNotBlank()) {
            chatManager.sendMessage(recipientDeviceId, text)
        }
    }

    fun sendImage(recipientDeviceId: String, base64Image: String) {
        chatManager.sendImage(recipientDeviceId, base64Image)
    }

    fun setRelayEnabled(enabled: Boolean) {
        identityManager.isRelayEnabled = enabled
        _relayEnabled.value = enabled
    }

    fun updateDisplayName(name: String) {
        identityManager.updateDisplayName(name)
        if (isMeshActive.value) {
            meshRouter.broadcastHello()
        }
    }

    fun simulateTestCall() {
        viewModelScope.launch {
            val virtualPeer = MeshNeighbor(
                endpointId = "sim-endpoint-1",
                deviceId = "AIR-LINK-B",
                displayName = "AirLink Peer (Relay)",
                hopCount = 2,
                viaDisplayName = "Node B"
            )
            startCall(virtualPeer)
        }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyManager.stopMesh()
        audioEngine.stopAudioSession()
    }
}
