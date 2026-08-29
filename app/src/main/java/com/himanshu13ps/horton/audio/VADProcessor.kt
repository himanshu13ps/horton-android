package com.himanshu13ps.horton.audio

import android.content.Context
import com.k2fsa.sherpa.onnx.Vad
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VADProcessor(private val context: Context) {

    private val minSilenceDurationMs = 250L
    private val sampleRate = 16000

    private var currentState = VADState.PASSIVE
    private var silenceStartTime = 0L
    private var currentFileOutputStream: FileOutputStream? = null
    private var currentFilePath: String? = null

    private val _completedFilesFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val completedFilesFlow: SharedFlow<String> = _completedFilesFlow

    private val preRollBuffer = ArrayDeque<ByteArray>(5)

    private var vad: Vad? = null

    init {
        val modelsDir = File(context.filesDir, "models")
        val vadModelFile = File(modelsDir, "silero_vad.onnx")

        if (vadModelFile.exists()) {
            // Vad Config commented out for now since the model is not properly instantiated
            // vad = Vad(config) // Commented out to prevent crash without actual model file on device
        }
    }

    suspend fun processAudioChunk(chunk: ByteArray) {
        // We simulate the float array processing
        // In reality, chunk is ByteArray (16-bit PCM), we need to convert to FloatArray for sherpa-onnx
        val floatArray = FloatArray(chunk.size / 2)
        var floatIndex = 0
        for (i in chunk.indices step 2) {
            val sample = (chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)
            floatArray[floatIndex++] = sample / 32768.0f
        }

        // val isSpeech = vad?.isSpeech(floatArray) ?: false
        val isSpeech = Math.random() > 0.5 // Stub for simulation
        
        when (currentState) {
            VADState.PASSIVE -> {
                if (isSpeech) {
                    currentState = VADState.ACTIVE
                    startNewFile()
                    preRollBuffer.forEach { currentFileOutputStream?.write(it) }
                    preRollBuffer.clear()
                    currentFileOutputStream?.write(chunk)
                } else {
                    if (preRollBuffer.size == 5) {
                        preRollBuffer.removeFirst()
                    }
                    preRollBuffer.addLast(chunk)
                }
            }
            VADState.ACTIVE -> {
                currentFileOutputStream?.write(chunk)
                if (!isSpeech) {
                    currentState = VADState.TRAILING
                    silenceStartTime = System.currentTimeMillis()
                }
            }
            VADState.TRAILING -> {
                currentFileOutputStream?.write(chunk)
                if (isSpeech) {
                    currentState = VADState.ACTIVE
                } else {
                    val silenceDuration = System.currentTimeMillis() - silenceStartTime
                    if (silenceDuration > minSilenceDurationMs) {
                        closeCurrentFileAndEmit()
                        currentState = VADState.PASSIVE
                        // vad?.reset() // Reset VAD state after finalizing chunk
                    }
                }
            }
        }
    }

    private fun startNewFile() {
        val fileName = "segment_${UUID.randomUUID()}.wav"
        val file = File(context.cacheDir, fileName)
        currentFilePath = file.absolutePath
        currentFileOutputStream = FileOutputStream(file)
        currentFileOutputStream?.write(ByteArray(44)) // Dummy header
    }

    private suspend fun closeCurrentFileAndEmit() {
        currentFileOutputStream?.apply {
            flush()
            close()
        }
        currentFileOutputStream = null
        currentFilePath?.let {
            _completedFilesFlow.emit(it)
        }
        currentFilePath = null
    }

    fun finishSession() {
        if (currentState == VADState.ACTIVE || currentState == VADState.TRAILING) {
            currentFileOutputStream?.close()
        }
        // vad?.release()
    }

    enum class VADState {
        PASSIVE, ACTIVE, TRAILING
    }
}
