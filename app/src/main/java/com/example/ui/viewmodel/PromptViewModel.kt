package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Prompt
import com.example.data.model.Review
import com.example.data.repository.PromptRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface GenerateUiState {
    object Idle : GenerateUiState
    object Loading : GenerateUiState
    data class Success(val optimizedPrompt: String, val usageInstructions: String) : GenerateUiState
    data class Error(val message: String) : GenerateUiState
}

class PromptViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = PromptRepository(db.promptDao())

    // UI filters
    val selectedCategory = MutableStateFlow<String?>(null)
    val searchQuery = MutableStateFlow("")

    val mainCategories = listOf(
        "Image Generation",
        "Video Generation",
        "Text Generation",
        "Creative Writing",
        "Programming"
    )

    val subCategoriesMap = mapOf(
        "Image Generation" to listOf(
            "Photorealistic Portrait",
            "Fantasy Concept Art",
            "Sci-Fi Landscape",
            "3D Isometric Render",
            "Anime & Illustration"
        ),
        "Video Generation" to listOf(
            "Cinematic Drone Shot",
            "Time-lapse Nature",
            "Cyberpunk Animation",
            "Text-to-Video Sequence",
            "Sora Cinematic Hook"
        ),
        "Text Generation" to listOf(
            "Marketing Copywriting",
            "Academic Summarization",
            "Business Report Outline",
            "Social Media Hook",
            "SEO Article Generator"
        ),
        "Creative Writing" to listOf(
            "Character Sketching",
            "Sci-Fi Story Plot",
            "Poetry Meter Generator",
            "Dialogue Polisher",
            "RPG Quest Outliner"
        ),
        "Programming" to listOf(
            "Code Refactoring",
            "Unit Test Generator",
            "Database Schema Designer",
            "API Documentation",
            "Algorithm Optimizer"
        )
    )

    // All Prompts from repository
    val allPrompts: StateFlow<List<Prompt>> = repository.allPrompts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val showOnlyFavorites = MutableStateFlow(false)

    // Filtered Prompts reactive sequence
    val filteredPrompts: StateFlow<List<Prompt>> = combine(
        allPrompts,
        selectedCategory,
        searchQuery,
        showOnlyFavorites
    ) { prompts, category, query, favoritesOnly ->
        prompts.filter { prompt ->
            val matchesCategory = if (category == null) {
                true
            } else {
                prompt.categories.any { it.equals(category, ignoreCase = true) }
            }

            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                prompt.title.contains(query, ignoreCase = true) ||
                prompt.content.contains(query, ignoreCase = true) ||
                prompt.targetPlatform.contains(query, ignoreCase = true) ||
                prompt.categories.any { it.contains(query, ignoreCase = true) }
            }

            val matchesFavorite = if (favoritesOnly) prompt.isFavorite else true

            matchesCategory && matchesSearch && matchesFavorite
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Generation State
    private val _generateUiState = MutableStateFlow<GenerateUiState>(GenerateUiState.Idle)
    val generateUiState: StateFlow<GenerateUiState> = _generateUiState.asStateFlow()

    fun resetGenerateState() {
        _generateUiState.value = GenerateUiState.Idle
    }

    // AI Generation execution
    fun generatePromptWithAI(
        userIdea: String,
        targetPlatform: String,
        category: String,
        subCategory: String
    ) {
        if (userIdea.isBlank()) {
            _generateUiState.value = GenerateUiState.Error("Please describe your idea first.")
            return
        }

        viewModelScope.launch {
            _generateUiState.value = GenerateUiState.Loading
            repository.generatePromptWithAI(userIdea, targetPlatform, category, subCategory)
                .onSuccess { (optimized, instructions) ->
                    _generateUiState.value = GenerateUiState.Success(optimized, instructions)
                }
                .onFailure { error ->
                    _generateUiState.value = GenerateUiState.Error(error.localizedMessage ?: "Failed to generate prompt. Please check your network or API Key.")
                }
        }
    }

    // Sandbox State for testing execution
    private val _sandboxUiState = MutableStateFlow<SandboxUiState>(SandboxUiState.Idle)
    val sandboxUiState: StateFlow<SandboxUiState> = _sandboxUiState.asStateFlow()

    fun resetSandboxState() {
        _sandboxUiState.value = SandboxUiState.Idle
    }

    fun runPromptInSandbox(promptText: String) {
        if (promptText.isBlank()) {
            _sandboxUiState.value = SandboxUiState.Error("Prompt text is empty.")
            return
        }
        viewModelScope.launch {
            _sandboxUiState.value = SandboxUiState.Loading
            repository.testPromptInSandbox(promptText)
                .onSuccess { response ->
                    _sandboxUiState.value = SandboxUiState.Success(response)
                }
                .onFailure { error ->
                    _sandboxUiState.value = SandboxUiState.Error(error.localizedMessage ?: "Execution failed.")
                }
        }
    }

    // Remix Variations State
    private val _remixUiState = MutableStateFlow<RemixUiState>(RemixUiState.Idle)
    val remixUiState: StateFlow<RemixUiState> = _remixUiState.asStateFlow()

    fun resetRemixState() {
        _remixUiState.value = RemixUiState.Idle
    }

    fun generateVariationsOfPrompt(promptContent: String, toneType: String) {
        if (promptContent.isBlank()) {
            _remixUiState.value = RemixUiState.Error("Prompt content is empty.")
            return
        }
        viewModelScope.launch {
            _remixUiState.value = RemixUiState.Loading
            repository.remixPrompt(promptContent, toneType)
                .onSuccess { outcome ->
                    _remixUiState.value = RemixUiState.Success(outcome)
                }
                .onFailure { error ->
                    _remixUiState.value = RemixUiState.Error(error.localizedMessage ?: "Remixing failed.")
                }
        }
    }

    // Toggle favorite state
    fun toggleFavorite(prompt: Prompt) {
        viewModelScope.launch {
            repository.updatePrompt(prompt.copy(isFavorite = !prompt.isFavorite))
        }
    }

    // Insert user created prompt
    fun savePrompt(
        title: String,
        content: String,
        instruction: String,
        targetPlatform: String,
        categories: List<String>
    ) {
        viewModelScope.launch {
            repository.insertPrompt(
                Prompt(
                    title = title,
                    content = content,
                    instruction = instruction,
                    targetPlatform = targetPlatform,
                    categories = categories,
                    isPreloaded = false,
                    isFavorite = false
                )
            )
        }
    }

    // Delete custom prompt
    fun deletePrompt(prompt: Prompt) {
        viewModelScope.launch {
            repository.deletePrompt(prompt)
        }
    }

    // Get specific prompt state
    fun getPromptById(id: Int): Flow<Prompt?> = repository.getPromptById(id)

    // Reactive reviews flow for a prompt
    fun getReviewsForPrompt(promptId: Int): Flow<List<Review>> = repository.getReviewsForPrompt(promptId)

    // Insert new review
    fun submitReview(promptId: Int, rating: Int, reviewerName: String, reviewText: String) {
        viewModelScope.launch {
            repository.insertReview(
                Review(
                    promptId = promptId,
                    rating = rating,
                    reviewerName = reviewerName.ifBlank { "Anonymous user" },
                    reviewText = reviewText.ifBlank { "Awesome prompt, worked perfectly!" }
                )
            )
        }
    }
}

sealed interface SandboxUiState {
    object Idle : SandboxUiState
    object Loading : SandboxUiState
    data class Success(val response: String) : SandboxUiState
    data class Error(val message: String) : SandboxUiState
}

sealed interface RemixUiState {
    object Idle : RemixUiState
    object Loading : RemixUiState
    data class Success(val remixedText: String) : RemixUiState
    data class Error(val message: String) : RemixUiState
}
