package com.example.archivetok.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exhibits")
data class ExhibitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverImage: String? = null, // URL or local path to cover image
    val useCoverImage: Boolean = true, // Whether to show the cover image or fallback to gradient
    val colorTheme: Int? = null, // Index of selected color palette (0-12) or null for auto-generated
    val mediaType: String = "video", // "video" or "audio"
    val createdAt: Long = System.currentTimeMillis()
)
