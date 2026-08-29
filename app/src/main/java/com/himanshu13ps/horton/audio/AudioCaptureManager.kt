package com.himanshu13ps.horton.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class AudioCaptureManager {

    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    // We need 520 samples for the Silero VAD (32.5ms at 16kHz)
    // 520 samples * 2 bytes per sample (16-bit) = 1040 bytes
    private val bufferSize = 1040 
    
    @SuppressLint("MissingPermission")
    fun startCapture(): Flow<ByteArray> = flow {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        // Ensure our buffer size is at least minBufferSize, though we will read exactly 1040 bytes chunks
        val recordBufferSize = maxOf(minBufferSize, bufferSize * 10)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            recordBufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord initialization failed")
        }

        audioRecord?.startRecording()

        val audioBuffer = ByteArray(bufferSize)

        while (coroutineContext.isActive) {
            val bytesRead = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
            if (bytesRead > 0) {
                // Yield a copy of the buffer to the flow
                emit(audioBuffer.copyOf(bytesRead))
            } else if (bytesRead < 0) {
                // Handle error
                break
            }
        }
    }.flowOn(Dispatchers.IO)

    fun stopCapture() {
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null
    }
}
