package com.disasterrelief.app.data.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlmRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager
) {
    private var llmInference: LlmInference? = null

    // Engine initialization takes time; execute on a background thread
    suspend fun initializeEngine() = withContext(Dispatchers.IO) {
        if (llmInference != null) return@withContext
        
        if (!modelDownloadManager.isModelDownloaded()) {
            throw IllegalStateException("Model file not found in internal storage. Trigger download first.")
        }

        val targetModelPath = modelDownloadManager.modelFile.absolutePath

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(targetModelPath)
            .setMaxTokens(512)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
    }

    // Stream responses token-by-token directly to your Jetpack Compose UI
    fun processTriageChat(userPrompt: String): Flow<String> = flow {
        val engine = llmInference ?: throw IllegalStateException("Engine must be initialized before use.")
        
        // Gemma requires its specific turn tokens for structured responses
        val formattedPrompt = """
            <start_of_turn>user
            Analyze the regional SOS requests. Extract key critical emergencies, shortages, and provide a clear assessment based ONLY on the provided data.
            
            $userPrompt
            <end_of_turn>
            <start_of_turn>model
        """.trimIndent()

        Log.d("LocalLlmRepository", "DATA GOING INTO LLM:\n$formattedPrompt")

        // LlmInference does have a generateResponseAsync with a progress listener, but we can also use generateResponse blockingly in IO dispatcher.
        val response = engine.generateResponse(formattedPrompt)
        
        // Simulate streaming for the UI effect
        val tokens = response.split(" ")
        for (token in tokens) {
            emit("$token ")
            kotlinx.coroutines.delay(20)
        }
    }.flowOn(Dispatchers.IO)
}
