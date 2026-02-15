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
    private val bookmarkDao: BookmarkDao
) {

    // Fetch popular videos (movies/shows)
    suspend fun getVideos(page: Int, tags: Set<String> = emptySet(), decadeQuery: String? = null, languageQuery: String? = null): List<ArchiveItem> {
        val baseQuery = "mediatype:movies AND format:H.264"
        var query = if (tags.isNotEmpty()) {
            val tagQuery = tags.joinToString(" OR ") { "subject:\"$it\"" }
            "$baseQuery AND ($tagQuery)"
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
        
        android.util.Log.d("ArchiveRepo", "Fetching videos. Page: $page, Query: $query")
        
        try {
            val response = archiveService.search(
                query = query,
                page = page,
                sort = "downloads desc"
            ).response.docs
            
            android.util.Log.d("ArchiveRepo", "Fetched ${response.size} videos")
            return response
        } catch (e: Exception) {
            android.util.Log.e("ArchiveRepo", "Error fetching videos", e)
            throw e
        }
    }

    suspend fun getMetadata(identifier: String): MetadataResponse {
        return archiveService.getMetadata(identifier)
    }

    // Bookmark operations
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

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

    suspend fun resolveVideoUrl(identifier: String): com.example.archivetok.data.model.MetadataResult? {
        return try {
            val metadata = archiveService.getMetadata(identifier)
            val files = metadata.files
            
            // Strict Filtering Routine for Playlist
            // 1. Filter: Keep ONLY H.264, MPEG4, Matroska. Exclude specific extensions.
            val allowedFormats = listOf("h.264", "mpeg4", "matroska")
            val excludedExtensions = listOf(".xml", ".txt", ".png", ".jpg", ".jpeg", "_meta.sqlite", "_thumb.jpg")
            
            val candidates = files.filter { file ->
                val name = file.name.lowercase()
                val format = file.format.lowercase()
                
                // Exclude invalid extensions
                val isExcluded = excludedExtensions.any { name.endsWith(it) }
                if (isExcluded) return@filter false
                
                // Keep if format is valid
                allowedFormats.any { format.contains(it) }
            }
            
            // 2. Group by Base Filename (Treat video.mp4 and video_512kb.mp4 as same part)
            val grouped = candidates.groupBy { file ->
                // Heuristic: remove _512kb suffix and extension to find common "base" name
                file.name.replace("_512kb", "", ignoreCase = true).substringBeforeLast(".")
            }
            
            // 3. Select ONE "Winner" per Part based on Priority
            // Priority: H.264 > MPEG4 > 512kb as fallback
            val selectedFiles = grouped.mapNotNull { (_, group) ->
                group.minByOrNull { file ->
                    val format = file.format.lowercase()
                    when {
                        format.contains("h.264") -> 1
                        format.contains("mpeg4") && !format.contains("512kb") -> 2
                        format.contains("512kb") -> 3
                        else -> 4
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

                // De-duplicate URLs (just in case multiple files map to same URL, unlikely but safe)
                val uniqueUrls = urls.distinct()

                com.example.archivetok.data.model.MetadataResult(
                    videoUrls = uniqueUrls,
                    isMultiPart = uniqueUrls.size > 1,
                    fileCount = uniqueUrls.size
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
