package com.example.archivetok.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.archivetok.data.model.ArchiveItem
import com.example.archivetok.data.repository.ArchiveRepository
import com.example.archivetok.data.PrefsManager
import androidx.media3.datasource.DataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject

@HiltViewModel
class VideoFeedViewModel @Inject constructor(
    private val repository: ArchiveRepository,
    val dataSourceFactory: DataSource.Factory,
    private val prefsManager: PrefsManager,
    private val playerPool: com.example.archivetok.data.player.VideoPlayerPool,
    private val preloader: com.example.archivetok.data.player.VideoPreloader
) : ViewModel() {

    private val _videoList = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val videoList: StateFlow<List<ArchiveItem>> = _videoList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Simple pagination
    private val _showBookmarks = MutableStateFlow(false)
    val showBookmarks: StateFlow<Boolean> = _showBookmarks.asStateFlow()

    private val _videoUrls = MutableStateFlow<Map<String, com.example.archivetok.data.model.MetadataResult>>(emptyMap())
    val videoUrls: StateFlow<Map<String, com.example.archivetok.data.model.MetadataResult>> = _videoUrls.asStateFlow()

    // Filter State
    private val _filterDecade = MutableStateFlow<String?>(null)
    val filterDecade: StateFlow<String?> = _filterDecade.asStateFlow()

    private val _filterLanguage = MutableStateFlow<String?>(null)
    val filterLanguage: StateFlow<String?> = _filterLanguage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var currentPage = 1
    private var currentTags: Set<String> = emptySet()
    
    private var bookmarkedIds: Set<String> = emptySet()
    private var currentBookmarks: List<com.example.archivetok.data.local.BookmarkEntity> = emptyList()

    init {
        // Collect bookmarks continuously to update the set
        viewModelScope.launch {
            repository.bookmarks.collect { bookmarkedItems ->
                currentBookmarks = bookmarkedItems
                bookmarkedIds = bookmarkedItems.map { it.identifier }.toSet()
                
                if (_showBookmarks.value) {
                    _videoList.value = bookmarkedItems.map { 
                        ArchiveItem(it.identifier, it.title, it.description, it.mediatype, null, null) 
                    }
                }
            }
        }

        
        // Initial Feed Load
        initializeFeed()
    }

    private fun initializeFeed() {
        currentTags = prefsManager.selectedTags
        // Randomize start page (1-20) to ensure fresh content on reload
        currentPage = kotlin.random.Random.nextInt(1, 21)
        loadMoreVideos()
    }

    fun toggleShowBookmarks() {
        _showBookmarks.value = !_showBookmarks.value
        
        if (_showBookmarks.value) {
            // Switch TO Bookmarks: Load from cache
            _videoList.value = currentBookmarks.map { 
                ArchiveItem(it.identifier, it.title, it.description, it.mediatype, null, null) 
            }
            _errorMessage.value = null // Clear any explore errors
        } else {
            // Switch TO Explore: Reload feed
            _videoList.value = emptyList() 
            initializeFeed() 
        }
    }
    fun setFilterDecade(decade: String?) {
        _filterDecade.value = decade
        resetAndReload()
    }

    fun setFilterLanguage(language: String?) {
        _filterLanguage.value = language
        resetAndReload()
    }

    private fun resetAndReload() {
        _videoList.value = emptyList()
        currentPage = 1
        loadMoreVideos()
    }

    private fun getDecadeQuery(decade: String?): String? {
        return when (decade) {
            "1800s" -> "date:[1800-01-01 TO 1899-12-31]"
            "1900s" -> "date:[1900-01-01 TO 1909-12-31]"
            "1910s" -> "date:[1910-01-01 TO 1919-12-31]"
            "1920s" -> "date:[1920-01-01 TO 1929-12-31]"
            "1930s" -> "date:[1930-01-01 TO 1939-12-31]"
            "1940s" -> "date:[1940-01-01 TO 1949-12-31]"
            "1950s" -> "date:[1950-01-01 TO 1959-12-31]"
            "1960s" -> "date:[1960-01-01 TO 1969-12-31]"
            "1970s" -> "date:[1970-01-01 TO 1979-12-31]"
            "1980s" -> "date:[1980-01-01 TO 1989-12-31]"
            "1990s" -> "date:[1990-01-01 TO 1999-12-31]"
            "2000s" -> "date:[2000-01-01 TO 2009-12-31]"
            "2010s" -> "date:[2010-01-01 TO 2019-12-31]"
            "2020s" -> "date:[2020-01-01 TO 2029-12-31]"
            else -> null
        }
    }

    private fun getLanguageQuery(language: String?): String? {
        return when (language) {
            "English" -> "(language:eng OR language:English)"
            "Spanish" -> "(language:spa OR language:Spanish)"
            "French" -> "(language:fre OR language:French)"
            "German" -> "(language:ger OR language:German)"
            "Japanese" -> "(language:jpn OR language:Japanese)"
            "Chinese" -> "(language:chi OR language:Chinese)"
            "Russian" -> "(language:rus OR language:Russian)"
            "Italian" -> "(language:ita OR language:Italian)"
            "Portuguese" -> "(language:por OR language:Portuguese)"
            "Hindi" -> "(language:hin OR language:Hindi)"
            "Arabic" -> "(language:ara OR language:Arabic)"
            else -> null
        }
    }

    fun loadMoreVideos() {
        if (_isLoading.value) return
        if (_showBookmarks.value) return 
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null // Clear previous errors
            android.util.Log.d("VideoFeedVM", "Loading videos... Page: $currentPage")
            
            try {
                // Get query from filter
                val decadeQuery = getDecadeQuery(_filterDecade.value)
                val languageQuery = getLanguageQuery(_filterLanguage.value)
                val newVideos = repository.getVideos(currentPage, currentTags, decadeQuery, languageQuery)
                
                android.util.Log.d("VideoFeedVM", "Raw videos fetched: ${newVideos.size}")
                
                // FILTER: Exclude videos that are already bookmarked
                val unbookmarked = newVideos.filter { !bookmarkedIds.contains(it.identifier) }
                
                // VARIETY SORT: Round-Robin based on Tags
                // 1. Bucket items by their primary matching tag
                val buckets = mutableMapOf<String, MutableList<ArchiveItem>>()
                val otherBucket = mutableListOf<ArchiveItem>()
                
                unbookmarked.forEach { item ->
                    // Find the first tag in item.subject that matches a user-selected tag
                    val matchingTag = item.subject?.firstOrNull { currentTags.contains(it) }
                    
                    if (matchingTag != null) {
                        buckets.getOrPut(matchingTag) { mutableListOf() }.add(item)
                    } else {
                        otherBucket.add(item)
                    }
                }
                
                // 2. Shuffle each bucket internally
                buckets.values.forEach { it.shuffle() }
                otherBucket.shuffle()
                
                // 3. Round-Robin Merge
                val sortedList = mutableListOf<ArchiveItem>()
                var activeBuckets = buckets.values.toMutableList()
                if (otherBucket.isNotEmpty()) activeBuckets.add(otherBucket)
                
                while (activeBuckets.isNotEmpty()) {
                    val iterator = activeBuckets.iterator()
                    while (iterator.hasNext()) {
                        val bucket = iterator.next()
                        if (bucket.isNotEmpty()) {
                            sortedList.add(bucket.removeAt(0))
                        } else {
                            iterator.remove()
                        }
                    }
                }
                
                val filteredVideos = sortedList
                
                android.util.Log.d("VideoFeedVM", "After variety sort: ${filteredVideos.size} (Bookmarked: ${bookmarkedIds.size})")
                
                val currentList = _videoList.value
                _videoList.value = currentList + filteredVideos
                
                // Aggressive Prefetching for new items
                filteredVideos.take(5).forEach { item ->
                    launch { resolveUrlFor(item) }
                }

                if (newVideos.isEmpty()) {
                    android.util.Log.d("VideoFeedVM", "No videos returned from API.")
                    if (currentList.isEmpty()) {
                         _errorMessage.value = "No videos found (API returned 0). Try changing filters."
                    }
                } else if (filteredVideos.isEmpty() && newVideos.isNotEmpty()) {
                    android.util.Log.d("VideoFeedVM", "All videos filtered. Loading next page...")
                    currentPage++
                    _isLoading.value = false 
                    loadMoreVideos()
                    return@launch 
                } else {
                    currentPage++
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoFeedVM", "Error loading videos", e)
                e.printStackTrace()
                _errorMessage.value = "Error: ${e.localizedMessage ?: e.javaClass.simpleName}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Player Management
    fun getPlayerFor(index: Int): androidx.media3.exoplayer.ExoPlayer {
        val player = playerPool.acquirePlayer()
        val item = _videoList.value.getOrNull(index) ?: return player
        
        // Trigger resolution if needed
        if (!_videoUrls.value.containsKey(item.identifier)) {
             viewModelScope.launch {
                 resolveUrlFor(item)
             }
        }
        // Note: Actual media item setting is now handled via updatePlayerItems 
        // observed in the UI, to allow async updates without re-acquiring player.
        
        return player
    }

    fun updatePlayerItems(player: androidx.media3.exoplayer.ExoPlayer, result: com.example.archivetok.data.model.MetadataResult?) {
        if (result == null || result.videoUrls.isEmpty()) return

        val mediaItems = result.videoUrls.mapIndexed { i, url ->
            val uniqueId = "${url}_$i"
            androidx.media3.common.MediaItem.Builder()
                .setUri(android.net.Uri.parse(url))
                .setMediaId(uniqueId)
                .build()
        }

        val currentCount = player.mediaItemCount
        // Robust check: Compare unique IDs
        val firstItemChanged = if (currentCount > 0) {
            val currentId = player.getMediaItemAt(0).mediaId
            val newId = mediaItems[0].mediaId
            currentId != newId
        } else {
            true
        }

        // Only update if the playlist has ACTUALLY changed
        if (currentCount != mediaItems.size || firstItemChanged) {
             // Debug Logging
             for (i in mediaItems.indices) {
                 android.util.Log.d("PlaylistDebug", "Item $i: ${mediaItems[i].mediaId}")
             }
             
             player.setMediaItems(mediaItems, true) // Reset position
             player.prepare()
             player.playWhenReady = true
        }
    }

    private suspend fun resolveUrlFor(item: ArchiveItem) {
         if (_videoUrls.value.containsKey(item.identifier)) return
         try {
            val result = repository.resolveVideoUrl(item.identifier) ?: com.example.archivetok.data.model.MetadataResult(
                videoUrls = listOf("https://archive.org/download/${item.identifier}/${item.identifier}.mp4")
            )
            _videoUrls.value = _videoUrls.value + (item.identifier to result)
            
            // Preload the first video in the list
            if (result.videoUrls.isNotEmpty()) {
                preloader.preload(result.videoUrls[0])
            }
         } catch (e: Exception) {
             e.printStackTrace()
         }
    }

    fun releasePlayer(player: androidx.media3.exoplayer.ExoPlayer) {
        playerPool.releasePlayer(player)
    }
    
    fun onPageChanged(page: Int) {
         // Preload next 5 videos (Aggressive Lookahead)
         val list = _videoList.value
         for (i in 0..5) { // Ensure current and next 5 are resolved
             val nextIndex = page + i
             if (nextIndex < list.size) {
                 val item = list[nextIndex]
                 // Trigger resolution if missing
                 if (!_videoUrls.value.containsKey(item.identifier)) {
                     viewModelScope.launch { resolveUrlFor(item) }
                 } else {
                     // If already resolved, ensure file preload (caching)
                     _videoUrls.value[item.identifier]?.let { 
                         if (it.videoUrls.isNotEmpty()) preloader.preload(it.videoUrls[0])
                     }
                 }
             }
         }
    }

    fun toggleBookmark(item: ArchiveItem) {
        viewModelScope.launch {
            repository.toggleBookmark(item)
        }
    }
    
    fun isOnboardingCompleted(): Boolean {
        return prefsManager.isOnboardingCompleted
    }

    fun isTutorialShown(): Boolean {
        return prefsManager.isTutorialShown
    }

    fun markTutorialShown() {
        prefsManager.isTutorialShown = true
    }
    
    fun completeOnboarding(tags: Set<String>) {
        prefsManager.selectedTags = tags
        prefsManager.isOnboardingCompleted = true
        _videoList.value = emptyList()
        initializeFeed()
    }
    
    fun isBookmarked(identifier: String): kotlinx.coroutines.flow.Flow<Boolean> {
        return repository.isBookmarked(identifier)
    }
    
    override fun onCleared() {
        super.onCleared()
        playerPool.releaseAll()
    }
}
