package com.himanshu13ps.horton.ml

import android.content.Context
import com.himanshu13ps.horton.data.ExtractedNoteEntity
import com.himanshu13ps.horton.data.NoteDao
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class NoteSynthesisEngine(
    private val context: Context,
    private val noteDao: NoteDao
) {
    private var llmInference: LlmInference? = null

    init {
        val modelsDir = File(context.filesDir, "models")
        val llmFile = File(modelsDir, "gemma_1b_int4.task")

        if (llmFile.exists()) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(llmFile.absolutePath)
                .setMaxTokens(1024)
                .setTemperature(0.2f)
                .setTopK(40)
                .build()
            
            try {
                // llmInference = LlmInference.createFromOptions(context, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun synthesizeNotes(conversationId: Long) {
        withContext(Dispatchers.Default) {
            val conversationData = noteDao.getConversationWithDetailsSync(conversationId)
            val rawTranscripts = conversationData.transcripts
                .sortedBy { it.chunkId }
                .joinToString(" ") { it.rawText }

            if (rawTranscripts.isBlank()) {
                noteDao.updateConversationStatus(conversationId, conversationData.conversation.duration, "COMPLETED")
                return@withContext
            }

            val systemPrompt = """
                You are a professional executive assistant. Analyze the provided conversation transcript. 
                Extract the critical decisions, summarize the primary themes, and generate a concise list of actionable tasks. 
                Format your entire response using Markdown, utilizing clear headers and bullet points. 
                Do not include conversational filler or pleasantries. 
                Transcript: $rawTranscripts
            """.trimIndent()

            // Simulate Inference Session
            // val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder().build()
            // val session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
            
            // Stub generation
            // val generatedMarkdown = session.generateResponse(systemPrompt)
            val generatedMarkdown = """
                # Executive Summary
                This is a stubbed generated note from the local LLM.
                
                ## Key Decisions
                - Decided to use Jetpack Compose for the UI.
                - Chosen Sherpa-ONNX for STT.
                
                ## Action Items
                - [ ] Finalize the database schema
                - [ ] Implement the UI components
            """.trimIndent()

            noteDao.insertExtractedNote(
                ExtractedNoteEntity(
                    conversationId = conversationId,
                    markdownContent = generatedMarkdown,
                    generationTimestamp = System.currentTimeMillis()
                )
            )
            
            noteDao.updateConversationStatus(conversationId, conversationData.conversation.duration, "COMPLETED")
            
            // Critical: Native Memory Cleanup
            // session.close()
        }
    }

    fun release() {
        llmInference?.close()
        llmInference = null
    }
}
