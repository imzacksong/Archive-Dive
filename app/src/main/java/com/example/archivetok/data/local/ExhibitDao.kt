package com.example.archivetok.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExhibitDao {
    // Exhibit Operations
    @Query("SELECT * FROM exhibits WHERE mediaType = :mediaType ORDER BY createdAt DESC")
    fun getAllExhibits(mediaType: String): Flow<List<ExhibitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExhibit(exhibit: ExhibitEntity): Long

    @Delete
    suspend fun deleteExhibit(exhibit: ExhibitEntity)

    // Exhibit Item Operations
    @Query("UPDATE exhibits SET coverImage = :coverImage WHERE id = :exhibitId")
    suspend fun updateCoverImage(exhibitId: Long, coverImage: String)

    @Query("UPDATE exhibits SET name = :name, colorTheme = :colorTheme, useCoverImage = :useCoverImage WHERE id = :exhibitId")
    suspend fun updateExhibit(exhibitId: Long, name: String, colorTheme: Int?, useCoverImage: Boolean)

    @Query("UPDATE exhibits SET colorTheme = :colorTheme")
    suspend fun updateAllExhibitThemes(colorTheme: Int?)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVideoToExhibit(item: ExhibitItemEntity)

    @Query("DELETE FROM exhibit_items WHERE exhibitId = :exhibitId AND videoId = :videoId")
    suspend fun removeVideoFromExhibit(exhibitId: Long, videoId: String)

    // Query videos in an exhibit
    // We join with the bookmarks table to get the video details
    @Transaction
    @Query("""
        SELECT b.* FROM bookmarks b
        INNER JOIN exhibit_items ei ON b.identifier = ei.videoId
        WHERE ei.exhibitId = :exhibitId
        ORDER BY ei.addedAt DESC
    """)
    fun getVideosInExhibit(exhibitId: Long): Flow<List<BookmarkEntity>>

    // Check if video is in exhibit
    @Query("SELECT EXISTS(SELECT 1 FROM exhibit_items WHERE exhibitId = :exhibitId AND videoId = :videoId)")
    fun isVideoInExhibit(exhibitId: Long, videoId: String): Flow<Boolean>
    
    // Get count of items in each exhibit (for UI)
    @Query("SELECT COUNT(*) FROM exhibit_items WHERE exhibitId = :exhibitId")
    fun getExhibitItemCount(exhibitId: Long): Flow<Int>
}
