package com.himanshu13ps.horton.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu13ps.horton.data.ConversationEntity
import com.himanshu13ps.horton.data.ConversationWithDetails
import com.himanshu13ps.horton.data.NoteDao
import com.himanshu13ps.horton.utils.FileDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class AppState {
    object Dashboard : AppState()
    data class ActiveRecording(val conversationId: Long) : AppState()
    data class Review(val conversationId: Long) : AppState()
    object Settings : AppState()
}

data class MLModel(
    val id: String,
    val name: String,
    val downloadUrl: String,
    val fileName: String
)

class MainViewModel(private val noteDao: NoteDao) : ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Dashboard)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    val allConversations: StateFlow<List<ConversationEntity>> = noteDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentConversationDetails = MutableStateFlow<ConversationWithDetails?>(null)
    val currentConversationDetails: StateFlow<ConversationWithDetails?> = _currentConversationDetails.asStateFlow()

    // Model Setup
    val requiredModels = listOf(
        MLModel("vad", "Silero VAD", "https://github.com/k2fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx", "silero_vad.onnx"),
        MLModel("stt_encoder", "Zipformer STT Encoder", "https://huggingface.co/csukuangfj/sherpa-onnx-zipformer-en-2023-06-26/resolve/main/encoder-epoch-99-avg-1.int8.onnx", "encoder.onnx"),
        MLModel("stt_decoder", "Zipformer STT Decoder", "https://huggingface.co/csukuangfj/sherpa-onnx-zipformer-en-2023-06-26/resolve/main/decoder-epoch-99-avg-1.int8.onnx", "decoder.onnx"),
        MLModel("stt_joiner", "Zipformer STT Joiner", "https://huggingface.co/csukuangfj/sherpa-onnx-zipformer-en-2023-06-26/resolve/main/joiner-epoch-99-avg-1.int8.onnx", "joiner.onnx"),
        MLModel("stt_tokens", "Zipformer Tokens", "https://huggingface.co/csukuangfj/sherpa-onnx-zipformer-en-2023-06-26/resolve/main/tokens.txt", "tokens.txt"),
        // Note: Gemma 3 INT4 is very large. In reality, a different smaller model might be used for testing, but we provide a placeholder URL.
        MLModel("llm", "Gemma 3 INT4 (GenAI)", "https://storage.googleapis.com/mediapipe-models/gemma_1b_int4.task", "gemma_1b_int4.task")
    )

    private val _missingModels = MutableStateFlow<List<MLModel>>(emptyList())
    val missingModels: StateFlow<List<MLModel>> = _missingModels.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, FileDownloader.DownloadState>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, FileDownloader.DownloadState>> = _downloadProgress.asStateFlow()

    fun checkModelsExist(context: Context) {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val missing = requiredModels.filter { model ->
            !File(modelsDir, model.fileName).exists()
        }
        _missingModels.value = missing
    }

    fun downloadModel(context: Context, model: MLModel) {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        
        val targetFile = File(modelsDir, model.fileName)

        viewModelScope.launch {
            FileDownloader.downloadFile(model.downloadUrl, targetFile).collect { state ->
                val currentProgress = _downloadProgress.value.toMutableMap()
                currentProgress[model.id] = state
                _downloadProgress.value = currentProgress
                
                if (state is FileDownloader.DownloadState.Finished) {
                    checkModelsExist(context)
                }
            }
        }
    }

    fun navigateToSettings() {
        _appState.value = AppState.Settings
    }

    fun startNewSession() {
        viewModelScope.launch {
            val newConv = ConversationEntity(
                title = "Meeting ${System.currentTimeMillis()}",
                startTimestamp = System.currentTimeMillis(),
                duration = 0,
                processingStatus = "RECORDING"
            )
            val id = noteDao.insertConversation(newConv)
            _appState.value = AppState.ActiveRecording(id)
            
            noteDao.getConversationWithDetails(id).collect { details ->
                _currentConversationDetails.value = details
            }
        }
    }

    fun endSession() {
        val currentState = _appState.value
        if (currentState is AppState.ActiveRecording) {
            viewModelScope.launch {
                noteDao.updateConversationStatus(currentState.conversationId, 0, "PENDING_SYNTHESIS")
                _appState.value = AppState.Review(currentState.conversationId)
            }
        }
    }

    fun viewConversation(id: Long) {
        viewModelScope.launch {
            noteDao.getConversationWithDetails(id).collect { details ->
                _currentConversationDetails.value = details
            }
        }
        _appState.value = AppState.Review(id)
    }
    
    fun navigateToDashboard() {
        _appState.value = AppState.Dashboard
    }
}
