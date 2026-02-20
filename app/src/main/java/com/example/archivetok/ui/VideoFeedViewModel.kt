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

    private val _isAudioMode = MutableStateFlow(false)
    val isAudioMode: StateFlow<Boolean> = _isAudioMode.asStateFlow()

    // Feed State Retention
    private var savedVideoList: List<ArchiveItem> = emptyList()
    private var savedAudioList: List<ArchiveItem> = emptyList()
    private var currentVideoPage = 1
    private var currentAudioPage = 1

    private val _videoList = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val videoList: StateFlow<List<ArchiveItem>> = _videoList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Simple pagination
    private val _showBookmarks = MutableStateFlow(false)
    val showBookmarks: StateFlow<Boolean> = _showBookmarks.asStateFlow()

    private val _videoUrls = MutableStateFlow<Map<String, com.example.archivetok.data.model.MetadataResult>>(emptyMap())
    val videoUrls: StateFlow<Map<String, com.example.archivetok.data.model.MetadataResult>> = _videoUrls.asStateFlow()

    // Museum / Exhibit State
    private val _exhibits = MutableStateFlow<List<com.example.archivetok.data.local.ExhibitEntity>>(emptyList())
    val exhibits: StateFlow<List<com.example.archivetok.data.local.ExhibitEntity>> = _exhibits.asStateFlow()

    private val _activeExhibitId = MutableStateFlow<Long?>(null)
    val activeExhibitId: StateFlow<Long?> = _activeExhibitId.asStateFlow()

    // Filter State
    private val _filterDecade = MutableStateFlow<String?>(null)
    val filterDecade: StateFlow<String?> = _filterDecade.asStateFlow()

    private val _filterLanguage = MutableStateFlow<String?>(null)
    val filterLanguage: StateFlow<String?> = _filterLanguage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()
    
    // Sort Strategy
    private var currentSortOrder: String = "downloads desc"

    private val _availableTags = MutableStateFlow<List<String>>(com.example.archivetok.data.model.Tags.videoTags)
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()
    
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
            }
        }
        
        // Collect Exhibits (Dynamic based on Mode)
        // We trigger this manually or observe a transformation?
        // Simpler: Launch a job that we can cancel/restart, or use a flatMapLatest equivalent if we had it.
        // Let's us observe _isAudioMode and switch the repository flow.
        viewModelScope.launch {
            _isAudioMode.collect { isAudio ->
                // Cancel previous collection implicitly by new collection? No, need switchMap.
                // Or just separate implementation. 
                // Let's use a simpler approach: Re-launch collection on mode change.
            }
        }
        
        // Initial Exhibits Load
        loadExhibits()
        
        // Initial Feed Load
        initializeFeed()
    }

    private var exhibitJob: kotlinx.coroutines.Job? = null
    
    private fun loadExhibits() {
        exhibitJob?.cancel()
        exhibitJob = viewModelScope.launch {
            val mode = if (_isAudioMode.value) "audio" else "video"
            repository.getExhibits(mode).collect { 
                _exhibits.value = it 
            }
        }
    }

    private fun initializeFeed() {
        // Load tags based on mode (Default: Video)
        val isAudio = _isAudioMode.value
        currentTags = if (isAudio) prefsManager.selectedAudioTags else prefsManager.selectedVideoTags
        _selectedTags.value = currentTags
        
        // Initialize pages with random start
        currentVideoPage = kotlin.random.Random.nextInt(1, 21)
        currentAudioPage = kotlin.random.Random.nextInt(1, 21)
        
        currentPage = if (isAudio) currentAudioPage else currentVideoPage
        
        // RANDOMIZE SORT ORDER
        // Variety is the spice of life, but speed is key.
        val sortOptions = listOf(
            "downloads desc",    // Popular (Fast)
            "publicdate desc",   // Newest (Fast)
            "addeddate desc"     // Just Added (Fast)
        )
        currentSortOrder = sortOptions.random() // Pick one for this session/feed
        android.util.Log.d("VideoFeedVM", "Feed Initialized with Sort Order: $currentSortOrder")
        
        loadMoreVideos()
    }
    
    fun toggleAudioMode() {
        val newModeIsAudio = !_isAudioMode.value
        
        // 1. Save current state
        if (_isAudioMode.value) {
            // Was Audio -> Switch to Video
            savedAudioList = _videoList.value
            currentAudioPage = currentPage
        } else {
            // Was Video -> Switch to Audio
            savedVideoList = _videoList.value
            currentVideoPage = currentPage
        }
        
        // 2. Switch Mode
        _isAudioMode.value = newModeIsAudio
        
        // Update Tags for new mode
        if (newModeIsAudio) {
            currentTags = prefsManager.selectedAudioTags
        } else {
            currentTags = prefsManager.selectedVideoTags
        }
        _selectedTags.value = currentTags
        
        // 3. Restore State
        if (newModeIsAudio) {
            _videoList.value = savedAudioList
            currentPage = currentAudioPage
            _availableTags.value = com.example.archivetok.data.model.Tags.audioTags
        } else {
            _videoList.value = savedVideoList
            currentPage = currentVideoPage
            _availableTags.value = com.example.archivetok.data.model.Tags.videoTags
        }
        
        // 4. Reload Exhibits for new mode
        loadExhibits()
        
        // 5. Load content if empty
        if (_videoList.value.isEmpty()) {
            loadMoreVideos()
        }
        
        // 6. Pause any active players (handled by UI reacting to list change/player pool, but good to ensure released)
        playerPool.releaseAll() 
    }

     fun updateTags(tags: Set<String>) {
        // Save to appropriate preference
        if (_isAudioMode.value) {
            prefsManager.selectedAudioTags = tags
        } else {
            prefsManager.selectedVideoTags = tags
        }
        
        currentTags = tags
        _selectedTags.value = tags
        resetAndReload()
    }

    fun toggleShowBookmarks() {
        _showBookmarks.value = !_showBookmarks.value
        
        if (_showBookmarks.value) {
            // Enter Museum Mode (Root)
            _activeExhibitId.value = null // No specific exhibit selected yet
            _videoList.value = emptyList() // Clear video feed while browsing exhibits
            _errorMessage.value = null
        } else {
            // Switch TO Explore: Reload feed
            _activeExhibitId.value = null
            // Restore current mode's feed
            if (_isAudioMode.value) {
                _videoList.value = savedAudioList // Or re-init? simplified: just re-load if empty
                currentPage = currentAudioPage
            } else {
                _videoList.value = savedVideoList
                currentPage = currentVideoPage
            }
            
            if (_videoList.value.isEmpty()) {
                 initializeFeed() 
            }
        }
    }
    
    fun openExhibit(exhibit: com.example.archivetok.data.local.ExhibitEntity) {
        _activeExhibitId.value = exhibit.id
        loadExhibitVideos(exhibit.id)
    }
    
    fun closeExhibit() {
        _activeExhibitId.value = null
        _videoList.value = emptyList() // Back to Museum root
    }

    private fun loadExhibitVideos(exhibitId: Long) {
        viewModelScope.launch {
            repository.getVideosInExhibit(exhibitId).collect { videos ->
                 _videoList.value = videos.map { 
                    ArchiveItem(it.identifier, it.title, it.description, it.mediatype, null, null) 
                }
            }
        }
    }

    fun createExhibit(name: String) {
        viewModelScope.launch {
            val mediaType = if (_isAudioMode.value) "audio" else "video"
            repository.createExhibit(name = name, mediaType = mediaType)
        }
    }
    
    fun updateExhibit(id: Long, name: String, theme: Int?, useCoverImage: Boolean) {
        viewModelScope.launch {
            repository.updateExhibit(id, name, theme, useCoverImage)
        }
    }
    
    fun updateAllExhibitThemes(theme: Int?) {
        viewModelScope.launch {
            repository.updateAllExhibitThemes(theme)
        }
    }

    fun addVideoToExhibit(exhibitId: Long, item: ArchiveItem) {
        viewModelScope.launch {
            repository.addVideoToExhibit(exhibitId, item)
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
        // Reset ONLY current mode's page
        if (_isAudioMode.value) currentAudioPage = 1 else currentVideoPage = 1
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
            "Future" -> "date:[2030-01-01 TO 2999-12-31]" // Why not
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
            val mode = if (_isAudioMode.value) "audio" else "video"
            android.util.Log.d("VideoFeedVM", "Loading $mode... Page: $currentPage")
            
            try {
                // Get query from filter
                val decadeQuery = getDecadeQuery(_filterDecade.value)
                val languageQuery = getLanguageQuery(_filterLanguage.value)
                
                val newVideos = repository.getVideos(
                    page = currentPage, 
                    tags = currentTags, 
                    decadeQuery = decadeQuery, 
                    languageQuery = languageQuery,
                    mediaType = mode,
                    sortOrder = currentSortOrder
                )
                
                android.util.Log.d("VideoFeedVM", "Raw items fetched: ${newVideos.size}")
                
                // FILTER: Exclude items that are already bookmarked
                val unbookmarked = newVideos.filter { !bookmarkedIds.contains(it.identifier) }
                
                // VARIETY SORT (Keep implementation, same for audio)
                val buckets = mutableMapOf<String, MutableList<ArchiveItem>>()
                val otherBucket = mutableListOf<ArchiveItem>()
                
                unbookmarked.forEach { item ->
                    val matchingTag = item.subject?.firstOrNull { currentTags.contains(it) }
                    if (matchingTag != null) {
                        buckets.getOrPut(matchingTag) { mutableListOf() }.add(item)
                    } else {
                        otherBucket.add(item)
                    }
                }
                
                buckets.values.forEach { it.shuffle() }
                otherBucket.shuffle()
                
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
                
                val currentList = _videoList.value
                _videoList.value = currentList + filteredVideos
                
                // Prefetching
                filteredVideos.take(5).forEach { item ->
                    launch { resolveUrlFor(item) }
                }

                if (newVideos.isEmpty()) {
                    if (currentList.isEmpty()) {
                         _errorMessage.value = "No items found (API returned 0). Try changing filters."
                    }
                } else if (filteredVideos.isEmpty() && newVideos.isNotEmpty()) {
                    currentPage++
                    _isLoading.value = false 
                    loadMoreVideos()
                    return@launch 
                } else {
                    currentPage++
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoFeedVM", "Error loading items", e)
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
        // Onboarding is primarily for VIDEO mode tags initially
        prefsManager.selectedVideoTags = tags
        prefsManager.isOnboardingCompleted = true
        _videoList.value = emptyList()
        initializeFeed() // This will pick up the tags
    }
    
    fun isBookmarked(identifier: String): kotlinx.coroutines.flow.Flow<Boolean> {
        return repository.isBookmarked(identifier)
    }
    
    override fun onCleared() {
        super.onCleared()
        playerPool.releaseAll()
    }
}
