package com.himanshu13ps.horton.ml

import com.himanshu13ps.horton.data.NoteDao
import com.himanshu13ps.horton.data.TranscriptEntity
import com.himanshu13ps.horton.data.AudioSegmentEntity
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.File

class TranscriptionEngine(
    private val noteDao: NoteDao,
    private val conversationId: Long,
    private val context: android.content.Context
) {
    private var recognizer: OfflineRecognizer? = null
    private var sequenceIndex = 0

    init {
        val modelsDir = File(context.filesDir, "models")
        
        // Ensure models exist before initializing (this prevents crashes if started prematurely)
        if (File(modelsDir, "encoder.onnx").exists()) {
            val config = OfflineRecognizerConfig(
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = File(modelsDir, "encoder.onnx").absolutePath,
                        decoder = File(modelsDir, "decoder.onnx").absolutePath,
                        joiner = File(modelsDir, "joiner.onnx").absolutePath
                    ),
                    tokens = File(modelsDir, "tokens.txt").absolutePath,
                    modelType = "zipformer",
                    numThreads = 2,
                    debug = false
                ),
                decodingMethod = "greedy_search",
                maxActivePaths = 4
            )
            // recognizer = OfflineRecognizer(config)
        }
    }

    suspend fun startProcessing(completedFilesFlow: SharedFlow<String>) {
        withContext(Dispatchers.Default) {
            completedFilesFlow.collect { filePath ->
                processAudioFile(filePath)
            }
        }
    }

    private suspend fun processAudioFile(filePath: String) {
        val segmentId = noteDao.insertAudioSegment(
            AudioSegmentEntity(
                conversationId = conversationId,
                filePath = filePath,
                sequenceIndex = sequenceIndex++
            )
        )

        var text = ""
        var timestampOffset = 0L

        val file = File(filePath)
        if (file.exists() && recognizer != null) {
            try {
                // Read 16-bit PCM WAV and convert to FloatArray [-1.0, 1.0]
                val bytes = file.readBytes()
                // Skip 44-byte WAV header
                if (bytes.size > 44) {
                    val pcmData = ByteArray(bytes.size - 44)
                    System.arraycopy(bytes, 44, pcmData, 0, pcmData.size)
                    
                    val floatArray = FloatArray(pcmData.size / 2)
                    for (i in floatArray.indices) {
                        val low = pcmData[i * 2].toInt() and 0xFF
                        val high = pcmData[i * 2 + 1].toInt() shl 8
                        val sample = (high or low).toShort()
                        floatArray[i] = sample / 32768.0f
                    }

                    // Run STT Inference
                    val stream = recognizer!!.createStream()
                    stream.acceptWaveform(floatArray, 16000)
                    recognizer!!.decode(stream)
                    val result = recognizer!!.getResult(stream)
                    
                    text = result.text ?: ""
                    // Extract start time if available
                    timestampOffset = if (result.timestamps != null && result.timestamps.isNotEmpty()) {
                        (result.timestamps.first() * 1000).toLong()
                    } else {
                        0L
                    }
                    
                    stream.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (text.isNotBlank()) {
            noteDao.insertTranscript(
                TranscriptEntity(
                    conversationId = conversationId,
                    segmentId = segmentId,
                    rawText = text,
                    timestampOffset = timestampOffset
                )
            )
        }

        // Cleanup: delete the processed wav file to save storage
        if (file.exists()) {
            file.delete()
        }
        noteDao.deleteAudioSegmentByPath(filePath)
    }

    fun release() {
        recognizer?.release()
        recognizer = null
    }
}
