package org.offlinemesh.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var recordJob: Job? = null
    private var playJob: Job? = null

    private val isRecording = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)

    val isMuted = AtomicBoolean(false)
    val isSpeakerOn = AtomicBoolean(false)

    private val jitterBuffer = JitterBuffer()

    var onFrameReadyToSend: ((data: ByteArray) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startAudioSession() {
        if (isRecording.get()) return

        val minRecordBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recordBufSize = maxOf(minRecordBuf, FRAME_SIZE_BYTES * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufSize
            )
        } catch (e: Exception) {
            return
        }

        val minTrackBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBufSize = maxOf(minTrackBuf, FRAME_SIZE_BYTES * 4)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(trackBufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        audioRecord?.startRecording()
        audioTrack?.play()

        isRecording.set(true)
        isPlaying.set(true)
        jitterBuffer.clear()

        // 1. Audio Record Loop
        recordJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(FRAME_SIZE_BYTES)
            val silence = ByteArray(FRAME_SIZE_BYTES / 2) // Compressed silence
            while (isActive && isRecording.get()) {
                val read = audioRecord?.read(buffer, 0, FRAME_SIZE_BYTES) ?: -1
                if (read > 0) {
                    if (isMuted.get()) {
                        onFrameReadyToSend?.invoke(silence)
                    } else {
                        val compressed = AudioCodec.encodeMuLaw(buffer)
                        onFrameReadyToSend?.invoke(compressed)
                    }
                }
            }
        }

        // 2. Audio Playback Loop
        playJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isPlaying.get()) {
                val frame = jitterBuffer.pop()
                if (frame != null) {
                    val pcm = AudioCodec.decodeMuLaw(frame)
                    audioTrack?.write(pcm, 0, pcm.size)
                } else {
                    Thread.sleep(10)
                }
            }
        }
    }

    fun onIncomingAudioFrame(seq: Long, compressedData: ByteArray) {
        jitterBuffer.push(seq, compressedData)
    }

    fun setMute(muted: Boolean) {
        isMuted.set(muted)
    }

    fun setSpeakerphone(speakerOn: Boolean) {
        isSpeakerOn.set(speakerOn)
        try {
            audioManager.isSpeakerphoneOn = speakerOn
        } catch (ignored: Exception) {}
    }

    fun stopAudioSession() {
        isRecording.set(false)
        isPlaying.set(false)

        recordJob?.cancel()
        playJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null

        jitterBuffer.clear()
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (ignored: Exception) {}
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val FRAME_DURATION_MS = 20
        // 16000 samples/sec * 0.02s = 320 samples = 640 bytes (16-bit)
        const val SAMPLES_PER_FRAME = SAMPLE_RATE * FRAME_DURATION_MS / 1000
        const val FRAME_SIZE_BYTES = SAMPLES_PER_FRAME * 2
    }
}
