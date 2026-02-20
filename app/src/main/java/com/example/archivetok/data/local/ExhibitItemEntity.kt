package com.example.archivetok.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exhibit_items",
    foreignKeys = [
        ForeignKey(
            entity = ExhibitEntity::class,
            parentColumns = ["id"],
            childColumns = ["exhibitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["exhibitId"]),
        Index(value = ["videoId"]),
        Index(value = ["exhibitId", "videoId"], unique = true) // Prevent duplicates in same exhibit
    ]
)
data class ExhibitItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exhibitId: Long,
    val videoId: String,
    val addedAt: Long = System.currentTimeMillis()
)
