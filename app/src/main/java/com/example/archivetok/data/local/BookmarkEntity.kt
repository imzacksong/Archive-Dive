package com.example.archivetok.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val identifier: String,
    val title: String,
    val description: String?,
    val mediatype: String?,
    val timestamp: Long = System.currentTimeMillis()
)
