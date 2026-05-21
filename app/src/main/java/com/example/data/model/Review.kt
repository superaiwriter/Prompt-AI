package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val promptId: Int,
    val rating: Int, // 1 to 5 stars
    val reviewerName: String,
    val reviewText: String,
    val timestamp: Long = System.currentTimeMillis()
)
