package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.PromptDao
import com.example.data.model.Prompt
import com.example.data.model.Review
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PromptRepository(private val promptDao: PromptDao) {

    val allPrompts: Flow<List<Prompt>> = promptDao.getAllPrompts()

    fun getPromptById(id: Int): Flow<Prompt?> = promptDao.getPromptById(id)

    suspend fun insertPrompt(prompt: Prompt): Long = withContext(Dispatchers.IO) {
        promptDao.insertPrompt(prompt)
    }

    suspend fun updatePrompt(prompt: Prompt) = withContext(Dispatchers.IO) {
        promptDao.updatePrompt(prompt)
    }

    suspend fun deletePrompt(prompt: Prompt) = withContext(Dispatchers.IO) {
        promptDao.deletePrompt(prompt)
    }

    suspend fun insertReview(review: Review): Long = withContext(Dispatchers.IO) {
        promptDao.insertReview(review)
    }

    fun getReviewsForPrompt(promptId: Int): Flow<List<Review>> = promptDao.getReviewsForPrompt(promptId)

    suspend fun generatePromptWithAI(
        userIdea: String,
        targetPlatform: String,
        category: String,
        subCategory: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("API Key is missing or default. Please enter a valid Gemini API Key in the AI Studio Secrets panel."))
        }

        // Crafting the prompt to get styled, high-quality results
        val systemPrompt = """
            You are Prompt Architect, an elite world-class AI prompt engineer.
            Your job is to optimize the user's brief idea into a state-of-the-art, hyper-effective, descriptive prompt for their target platform: $targetPlatform.
            
            Format your response into EXACTLY two sections separated by '===SEPARATOR===':
            Section 1: The optimized, finished prompt (Include high-fidelity parameters, styles, lighting, camera angles, negative prompts, or structural guidelines as appropriate for $targetPlatform).
            Section 2: Concise usage instructions/tips (Max 3 friendly bullet points explaining how to execute it, what model version or parameters to customize, and expected output).
            
            Be highly creative. Do not output markdown section titles like "Section 1" or "Section 2" - just output the optimized prompt, then ===SEPARATOR===, then the instructions on the next lines.
        """.trimIndent()

        val userPrompt = """
            Platform: $targetPlatform
            Category: $category -> Sub-category: $subCategory
            User Idea: $userIdea
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.75f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Received empty response from Gemini AI. Please try again."))
            }

            val parts = responseText.split("===SEPARATOR===")
            if (parts.size >= 2) {
                val optimized = parts[0].trim().removePrefix("===SEPARATOR===").trim()
                val instructions = parts[1].trim()
                Result.success(Pair(optimized, instructions))
            } else {
                // Return everything in the optimized section and fallback for instruction
                Result.success(Pair(responseText.trim(), "Paste directly into ${'$'}targetPlatform to unleash stunning results."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testPromptInSandbox(promptText: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("API Key is missing or default. Please enter a valid Gemini API Key in the AI Studio Secrets panel."))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = promptText)))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText.isNullOrBlank()) {
                Result.failure(Exception("Received empty response from Gemini."))
            } else {
                Result.success(responseText)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun remixPrompt(promptContent: String, toneType: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("API Key is missing or default. Please enter a valid Gemini API Key in the AI Studio Secrets panel."))
        }

        val systemPrompt = """
            You are a master prompts remixer. Your task is to take an existing prompt, and remix its perspective, tone, and style based on the selected tuning type: '$toneType'.
            
            Here are the options:
            - "Anime/Illustration": Make it feel highly artistic, descriptive in drawing styles, outlines, vibrant aesthetics.
            - "Photorealistic Cinematic": Render with specific camera parameters (8k, lens, exposure, cinematic lightning, detailed atmospheric composition).
            - "Advanced/Detailed": Turn any short prompt into a hyper-detailed, structured instruction prompt with multiple negative guidelines.
            - "Concise/Brief": Prune unnecessary words while preserving its fundamental concepts.
            
            Produce ONLY the final, complete resubmitted / remixed prompt. None of your own introductory or ending remarks, just output the polished text.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Prompt to remix: $promptContent")))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.8f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText.isNullOrBlank()) {
                Result.failure(Exception("Received empty response from Gemini."))
            } else {
                Result.success(responseText)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
