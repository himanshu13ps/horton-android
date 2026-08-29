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

        // Read audio from file - this requires parsing the WAV file or using Sherpa's WaveReader
        // We will simulate the transcription since we don't have the real model loaded
        val text = "Transcribed text for segment $sequenceIndex" // recognizer?.decode(filePath) or similar
        val timestampOffset = 0L // Extract from result if supported

        noteDao.insertTranscript(
            TranscriptEntity(
                conversationId = conversationId,
                segmentId = segmentId,
                rawText = text,
                timestampOffset = timestampOffset
            )
        )

        // Cleanup: delete the processed wav file to save storage
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
        noteDao.deleteAudioSegmentByPath(filePath)
    }

    fun release() {
        // recognizer?.release()
        recognizer = null
    }
}
