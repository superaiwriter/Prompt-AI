package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompts")
data class Prompt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val instruction: String,
    val targetPlatform: String,
    val categories: List<String>, // Stores multiple categories/sub-categories
    val createdAt: Long = System.currentTimeMillis(),
    val isPreloaded: Boolean = false
)
