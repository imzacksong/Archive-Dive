package com.example.archivetok.data.repository

import com.example.archivetok.data.local.BookmarkDao
import com.example.archivetok.data.local.BookmarkEntity
import com.example.archivetok.data.model.ArchiveItem
import com.example.archivetok.data.model.MetadataResponse
import com.example.archivetok.data.remote.ArchiveService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveRepository @Inject constructor(
    private val archiveService: ArchiveService,
    private val bookmarkDao: BookmarkDao,
    private val exhibitDao: com.example.archivetok.data.local.ExhibitDao
) {

    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val exhibits: Flow<List<com.example.archivetok.data.local.ExhibitEntity>> 
        get() = throw NotImplementedError("Use getExhibits(mediaType)")

    fun getExhibits(mediaType: String): Flow<List<com.example.archivetok.data.local.ExhibitEntity>> = exhibitDao.getAllExhibits(mediaType)

    // Fetch popular videos (movies/shows) OR audio
    suspend fun getVideos(
        page: Int, 
        tags: Set<String> = emptySet(), 
        decadeQuery: String? = null, 
        languageQuery: String? = null, 
        mediaType: String = "video",
        sortOrder: String = "downloads desc"
    ): List<ArchiveItem> {
        val baseQuery = if (mediaType == "audio") {
            "mediatype:audio" // Broad query for audio
        } else {
            "mediatype:movies AND format:H.264"
        }
        
        var query = if (tags.isNotEmpty()) {
            // Optimization: Limit to specific number of tags to keep query length manageable
            val activeTags = tags.take(15) 
            if (tags.size > 15) {
                android.util.Log.w("ArchiveRepo", "Truncating tags for query. Requested: ${tags.size}, Used: 15")
            }
            val tagList = activeTags.joinToString(" OR ") { "\"$it\"" }
            "$baseQuery AND subject:($tagList)"
        } else {
            baseQuery
        }

        if (decadeQuery != null) {
            query += " AND $decadeQuery"
        }

        if (languageQuery != null) {
            query += " AND $languageQuery"
        }
        
        // EXCLUSION: Filter out "electricsheep" spam which dominates the feed
        query += " AND -title:(electricsheep) AND -identifier:(electricsheep)"
        
        android.util.Log.d("ArchiveRepo", "Fetching items ($mediaType). Page: $page, Query: $query, Sort: $sortOrder")
        
        try {
            val response = archiveService.search(
                query = query,
                page = page,
                rows = 15,
                sort = sortOrder
            ).response.docs
            
            android.util.Log.d("ArchiveRepo", "Fetched ${response.size} items")
            return response
        } catch (e: Exception) {
            android.util.Log.e("ArchiveRepo", "Error fetching items", e)
            throw e
        }
    }

    suspend fun getMetadata(identifier: String): MetadataResponse {
        return archiveService.getMetadata(identifier)
    }

    // Bookmark operations

    fun isBookmarked(identifier: String): Flow<Boolean> = bookmarkDao.isBookmarked(identifier)

    suspend fun toggleBookmark(item: ArchiveItem) {
        val exists = bookmarkDao.isBookmarked(item.identifier).first()
        if (exists) {
            removeBookmark(item.identifier)
        } else {
            addBookmark(item)
        }
    }
    
    suspend fun addBookmark(item: ArchiveItem) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                identifier = item.identifier,
                title = item.title ?: "Unknown",
                description = item.description,
                mediatype = item.mediatype
            )
        )
    }

    suspend fun removeBookmark(identifier: String) {
        bookmarkDao.deleteBookmarkById(identifier)
    }

    // Exhibit Operations
    suspend fun createExhibit(name: String, description: String? = null, mediaType: String = "video") {
        exhibitDao.insertExhibit(
            com.example.archivetok.data.local.ExhibitEntity(
                name = name,
                description = description,
                mediaType = mediaType
            )
        )
    }

    suspend fun updateExhibit(id: Long, name: String, colorTheme: Int?, useCoverImage: Boolean) {
        exhibitDao.updateExhibit(id, name, colorTheme, useCoverImage)
    }
    
    suspend fun updateAllExhibitThemes(colorTheme: Int?) {
        exhibitDao.updateAllExhibitThemes(colorTheme)
    }

    suspend fun deleteExhibit(exhibit: com.example.archivetok.data.local.ExhibitEntity) {
        exhibitDao.deleteExhibit(exhibit)
    }

    suspend fun addVideoToExhibit(exhibitId: Long, item: ArchiveItem) {
        // Ensure video is bookmarked first (as FK constraint)
        val isBookmarked = bookmarkDao.isBookmarked(item.identifier).first()
        if (!isBookmarked) {
             val entity = BookmarkEntity(
                identifier = item.identifier,
                title = item.title ?: "Untitled",
                description = item.description,
                mediatype = item.mediatype
            )
            bookmarkDao.insertBookmark(entity)
        }

        exhibitDao.addVideoToExhibit(
            com.example.archivetok.data.local.ExhibitItemEntity(
                exhibitId = exhibitId,
                videoId = item.identifier
            )
        )
        
        // Auto-set cover image if not set (using album art for audio too!)
        // For audio, use ia_thumb if valid
        exhibitDao.updateCoverImage(exhibitId, "https://archive.org/services/img/${item.identifier}")
    }

    suspend fun removeVideoFromExhibit(exhibitId: Long, videoId: String) {
        exhibitDao.removeVideoFromExhibit(exhibitId, videoId)
    }

    fun getVideosInExhibit(exhibitId: Long): Flow<List<BookmarkEntity>> {

        return exhibitDao.getVideosInExhibit(exhibitId)
    }

    fun getExhibitItemCount(exhibitId: Long): Flow<Int> {
        return exhibitDao.getExhibitItemCount(exhibitId)
    }


    suspend fun resolveVideoUrl(identifier: String): com.example.archivetok.data.model.MetadataResult? {
        return try {
            val metadata = archiveService.getMetadata(identifier)
            val files = metadata.files
            
            // Comprehensive Filtering Routine (Supports Video & Audio)
            // 1. Define allowed extensions/formats for both
            val allowedFormats = listOf("h.264", "mpeg4", "matroska", "vbr mp3", "mp3", "flac", "ogg", "vorbis", "wav", "64kbps") 
            // Note: Archive.org formats are specific strings like "VBR MP3", "Flac", "H.264", "Ogg Vorbis"
            
            val excludedExtensions = listOf(".xml", ".txt", ".png", ".jpg", ".jpeg", "_meta.sqlite", "_thumb.jpg", ".gif")
            
            val candidates = files.filter { file ->
                val name = file.name.lowercase()
                val format = file.format.lowercase()
                
                // Exclude invalid extensions
                val isExcluded = excludedExtensions.any { name.endsWith(it) }
                if (isExcluded) return@filter false
                
                // Keep if format is valid
                allowedFormats.any { format.contains(it) }
            }
            
            // 2. Group by Base Filename
            val grouped = candidates.groupBy { file ->
                // Heuristic: remove common suffixes to find base name
                file.name.replace("_vbr", "", ignoreCase = true)
                         .replace("_512kb", "", ignoreCase = true)
                         .replace("_64kb", "", ignoreCase = true)
                         .substringBeforeLast(".")
            }
            
            // 3. Select ONE "Winner" per Part based on Priority
            // Video Priority: H.264 > MPEG4
            // Audio Priority: VBR MP3/MP3 > Ogg Vorbis > FLAC/WAV > 64Kbps
            val selectedFiles = grouped.mapNotNull { (_, group) ->
                group.minByOrNull { file ->
                    val format = file.format.lowercase()
                    when {
                        // Video Formats (Top Priority for Video Mode)
                        format.contains("h.264") -> 1
                        format.contains("mpeg4") && !format.contains("512kb") -> 2
                        
                        // Audio Formats (User Specified Hierarchy)
                        format.contains("vbr mp3") -> 10 // P1: Best balance
                        format.contains("mp3") && !format.contains("64kb") -> 11 // P1: Standard MP3
                        format.contains("ogg") || format.contains("vorbis") -> 12 // P2: Ogg
                        format.contains("flac") -> 13 // P3: Lossless
                        format.contains("wav") -> 13 // P3: Lossless
                        format.contains("64kbps") || format.contains("64kb") -> 14 // P4: Lowest fallback
                        
                        // Other / Fallback
                        else -> 100
                    }
                }
            }
            
            // 4. Sort alphanumerically
            val sortedFiles = selectedFiles.sortedBy { it.name }
            
            if (sortedFiles.isNotEmpty()) {
                android.util.Log.d("PlaylistBuilder", "Resolved ${sortedFiles.size} unique parts from ${files.size} raw files.")
                
                val urls = sortedFiles.map { file ->
                    // Fix: Use URLEncoder for strict compliance, handling spaces as %20
                    val encodedName = file.name.split("/").joinToString("/") { segment ->
                        java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
                    }
                    "https://archive.org/download/$identifier/$encodedName"
                }

                // De-duplicate URLs
                val uniqueUrls = urls.distinct()

                com.example.archivetok.data.model.MetadataResult(
                    videoUrls = uniqueUrls,
                    isMultiPart = uniqueUrls.size > 1,
                    fileCount = uniqueUrls.size,
                    reviews = metadata.reviews ?: emptyList()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
