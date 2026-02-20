package com.example.archivetok.ui

import android.content.Intent
import androidx.compose.runtime.DisposableEffect
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.archivetok.data.model.ArchiveItem
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Tv
import com.example.archivetok.ui.theme.NeonGreen
import com.example.archivetok.ui.theme.ArchiveYellow
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoFeedScreen(
    viewModel: VideoFeedViewModel = hiltViewModel()
) {
    val videoList by viewModel.videoList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showBookmarks by viewModel.showBookmarks.collectAsState()
    val videoUrls by viewModel.videoUrls.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isAudioMode by viewModel.isAudioMode.collectAsState()
    val context = LocalContext.current

    val brandColor = if(isAudioMode) NeonGreen else ArchiveYellow

    // Dynamic Status Bar Color
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = brandColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true // Always dark text for these bright colors
        }
    }

    val pagerState = rememberPagerState(pageCount = { videoList.size })
    
    // TUTORIAL STATE (Hoisted)
    var showTutorial by remember { mutableStateOf(!viewModel.isTutorialShown() && viewModel.isOnboardingCompleted()) }
    
    // FULL SCREEN & FILTER STATE (Hoisted)
    var isFullScreen by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val currentFilter by viewModel.filterDecade.collectAsState()
    val currentLanguage by viewModel.filterLanguage.collectAsState()

    // MUSEUM STATE
    val exhibits by viewModel.exhibits.collectAsState()
    val activeExhibitId by viewModel.activeExhibitId.collectAsState()
    var showAddToExhibitSheet by remember { mutableStateOf(false) }
    var videoToAdd by remember { mutableStateOf<ArchiveItem?>(null) }
    var showCreateExhibitDialog by remember { mutableStateOf(false) }
    
    // BACK NAVIGATION
    androidx.activity.compose.BackHandler(enabled = showBookmarks) {
        if (activeExhibitId != null) {
            viewModel.closeExhibit()
        } else {
            viewModel.toggleShowBookmarks()
        }
    }

    

    
    // COMMENTS STATE
    var showCommentsSheet by remember { mutableStateOf(false) }
    var currentReviews by remember { mutableStateOf<List<com.example.archivetok.data.model.ArchiveReview>>(emptyList()) }

    // DESCRIPTION STATE
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var currentDescriptionItem by remember { mutableStateOf<ArchiveItem?>(null) }

    var showSplash by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(2000) // 2 Seconds Splash
        showSplash = false
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("VideoFeedScreen", "Screen Launched")
    }

    if (showSplash) {
        SplashScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Initialization check
            LaunchedEffect(Unit) {
                if (videoList.isEmpty() && !isLoading && !showBookmarks) {
                    viewModel.loadMoreVideos()
                }
            }
            
            // 1. MUSEUM SCREEN (Root)
            if (showBookmarks && activeExhibitId == null) {
                 MuseumScreen(
                     exhibits = exhibits,
                     onExhibitSelected = { viewModel.openExhibit(it) },
                     onCreateExhibit = { showCreateExhibitDialog = true },
                     onUpdateExhibit = { id, name, theme, useCover -> viewModel.updateExhibit(id, name, theme, useCover) },
                     onUpdateAllThemes = { theme -> viewModel.updateAllExhibitThemes(theme) },
                     isAudioMode = isAudioMode,
                     brandColor = brandColor,
                     modifier = Modifier.padding(top = 80.dp) 
                 )
            }

            // 2. VIDEO FEED (Explore OR Inside Exhibit)
            if (!showBookmarks || activeExhibitId != null) {
                if (videoList.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    
                    Text(
                        text = if (showBookmarks) "No bookmarks yet" else (errorMessage ?: "No videos found"),
                        color = if (errorMessage != null) Color.Red else Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (!showBookmarks) {
                        androidx.compose.material3.Button(
                            onClick = { 
                                android.util.Log.d("VideoFeedScreen", "Retry clicked")
                                viewModel.loadMoreVideos() 
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = brandColor)
                        ) {
                            Text("Retry", color = Color.Black)
                        }
                    }
                }
            }
        }
        
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page < videoList.size) {
                 val item = videoList[page]

                 VideoItem(
                     index = page,
                     item = item,
                     viewModel = viewModel,
                     isSelected = pagerState.currentPage == page,
                     isTutorialActive = showTutorial,
                     isFullScreen = isFullScreen,
                     isAudioMode = isAudioMode,
                     brandColor = brandColor,
                     onToggleFullScreen = { isFullScreen = !isFullScreen },
                     onBookmark = { 
                         videoToAdd = item
                         showAddToExhibitSheet = true 
                     },
                     onOpenSource = {
                          val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/details/${item.identifier}"))

                          context.startActivity(browserIntent)
                     },
                     onShowComments = {
                          val result = videoUrls[item.identifier]
                          currentReviews = result?.reviews ?: emptyList()
                          showCommentsSheet = true
                     },
                     onShowDescription = {
                          currentDescriptionItem = item
                          showDescriptionSheet = true
                     }
                 )
            }
            
            // Load more when reaching end (only if not showing bookmarks)
            if (!showBookmarks && page >= videoList.size - 2 && !isLoading && videoList.isNotEmpty()) {
                viewModel.loadMoreVideos()
            }
        }
        
        // Listen for page changes to trigger preloading
        LaunchedEffect(pagerState.currentPage) {
            viewModel.onPageChanged(pagerState.currentPage)
        }
        
        if (isLoading && videoList.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (isLoading && videoList.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Top Bar & Filter
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp),
                     horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Archive Dive",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Explore",
                            color = if (!showBookmarks) Color.White else Color.Gray,
                            fontWeight = if (!showBookmarks) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { if (showBookmarks) viewModel.toggleShowBookmarks() }
                                .padding(8.dp)
                        )
                        Text(
                            text = "|",
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                        Text(
                            text = "Museum",
                            color = if (showBookmarks) Color.White else Color.Gray,
                            fontWeight = if (showBookmarks) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { 
                                     if (!showBookmarks) viewModel.toggleShowBookmarks() 
                                     else if (activeExhibitId != null) viewModel.closeExhibit() // Tap Museum while in exhibit -> Back to root
                                }
                                .padding(8.dp)
                        )
                    }
                }

                // Filter Icon (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (currentFilter != null || currentLanguage != null) brandColor else Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { showFilterSheet = true }
                    )
                }

                // Mode Toggle Icon (Top Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 48.dp, start = 16.dp)
                ) {
                    androidx.compose.material3.IconButton(
                         onClick = { viewModel.toggleAudioMode() }
                    ) {
                        Icon(
                            imageVector = if (isAudioMode) androidx.compose.material.icons.Icons.Default.Tv else androidx.compose.material.icons.Icons.Default.Headphones,
                            contentDescription = if (isAudioMode) "Switch to Video" else "Switch to Audio",
                            tint = if (isAudioMode) NeonGreen else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }



        }
        
        // MUSEUM CREATION DIALOG (Quick Hack: Use AddToExhibitSheet in "Create Mode" or just a separate small dialog? 
        // Actually MuseumScreen has a visual placeholder. Let's just implement the AddToExhibitSheet.
        
        if (showAddToExhibitSheet) {
            AddToExhibitSheet(
                exhibits = exhibits,
                onAddToExhibit = { exhibit ->
                    videoToAdd?.let { item ->
                        viewModel.addVideoToExhibit(exhibit.id, item)
                    }
                    showAddToExhibitSheet = false
                    videoToAdd = null
                },
                onCreateExhibit = { name ->
                    viewModel.createExhibit(name)
                },
                brandColor = brandColor,
                onDismissRequest = { 
                    showAddToExhibitSheet = false 
                    videoToAdd = null
                }
            )
        }
        
        // Explicit Exhibit Creation Dialog for Museum Screen FAB
        // We'll reuse AddToExhibitSheet logic or just force it open in a "Create Only" mode?
        // AddToExhibitSheet is designed for ADDING a video.
        // Let's just add a simple state for the FAB in MuseumScreen.
        
        // Create Exhibit Dialog (from Museum Screen FAB)
        if (showCreateExhibitDialog) {
            var newExhibitName by remember { mutableStateOf("") }
            
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCreateExhibitDialog = false },
                title = { Text("New Exhibit") },
                text = {
                    androidx.compose.material3.TextField(
                        value = newExhibitName,
                        onValueChange = { newExhibitName = it },
                        placeholder = { Text("Exhibit Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (newExhibitName.isNotBlank()) {
                                viewModel.createExhibit(newExhibitName)
                                showCreateExhibitDialog = false
                                newExhibitName = ""
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showCreateExhibitDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // To properly hook up MuseumScreen FAB:

        

        
        if (showFilterSheet) {
            val currentTags by viewModel.selectedTags.collectAsState()
            val availableTags by viewModel.availableTags.collectAsState()
            
            FilterBottomSheet(
                currentFilter = currentFilter,
                onFilterSelected = { viewModel.setFilterDecade(it) },
                currentLanguage = currentLanguage,
                onLanguageSelected = { viewModel.setFilterLanguage(it) },
                currentTags = currentTags,
                onTagsSelected = { viewModel.updateTags(it) },
                availableTags = availableTags,
                brandColor = brandColor,
                onDismissRequest = { showFilterSheet = false }
            )
        }
        
        if (showCommentsSheet) {
            CommentsBottomSheet(
                reviews = currentReviews,
                onDismissRequest = { showCommentsSheet = false }
            )
        }
        
        if (showDescriptionSheet && currentDescriptionItem != null) {
            DescriptionBottomSheet(
                title = currentDescriptionItem!!.title ?: "No Title",
                description = currentDescriptionItem!!.description ?: "No Description",
                onDismissRequest = { showDescriptionSheet = false }
            )
        }
        
        // TUTORIAL OVERLAY
        if (showTutorial && !showSplash) {
            TutorialOverlay(
                brandColor = brandColor,
                onDismiss = {
                    showTutorial = false
                    viewModel.markTutorialShown()
                }
            )
        }
    }
  }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoItem(
    index: Int,
    item: ArchiveItem,
    viewModel: VideoFeedViewModel,
    isSelected: Boolean,
    isTutorialActive: Boolean = false, // Added param
    isFullScreen: Boolean,
    isAudioMode: Boolean,
    brandColor: Color,
    onToggleFullScreen: () -> Unit,
    onBookmark: () -> Unit,
    onOpenSource: () -> Unit,
    onShowComments: () -> Unit,
    onShowDescription: () -> Unit
) {
    val context = LocalContext.current
    // QoL: Default to FIT (Original Aspect Ratio) - Feature Removed

    
    // Playback State (Hoisted for Overlay Visibility logic)
    var isPaused by remember { mutableStateOf(false) }
    
    // Overlay Visibility State
    var areControlsVisible by remember { mutableStateOf(true) }
    
    // Full Screen Mode State (Hoisted)
    // var isFullScreen by remember { mutableStateOf(false) } // Removed
    
    // System UI Controller
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as android.app.Activity).window
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
        
        LaunchedEffect(isFullScreen) {
            if (isFullScreen) {
                // Hide System Bars
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // Show System Bars
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    // Auto-hide controls mechanism
    // If FullScreen: Controls are HIDDEN by default. Tap Toggle.
    // If Normal: Controls AUTO-HIDE after delay. Tap Toggle.
    
    LaunchedEffect(isPaused, areControlsVisible, isTutorialActive, isFullScreen) {
        if (isTutorialActive) {
            areControlsVisible = true
        } else {
             if (isFullScreen) {
                 // In Full Screen, controls follow explicit visibility only (or auto-hide if just shown)
                 if (areControlsVisible && !isPaused) {
                      delay(3000) // Short delay in full screen
                      areControlsVisible = false
                 }
             } else {
                 // Normal Mode
                 if (!isPaused && areControlsVisible) {
                    delay(5000) // 5 seconds
                    areControlsVisible = false
                }
             }
        }
    }

    // Get metadata result
    val videoUrls by viewModel.videoUrls.collectAsState()
    val metadataResult = videoUrls[item.identifier]
    
    val currentSubIndex = remember { mutableIntStateOf(0) }
    
    // HOISTED PLAYER MANAGEMENT
    // Acquire player from pool 
    val player = remember(index) { viewModel.getPlayerFor(index) }
    
    // Track current index from Player
    LaunchedEffect(player) {
         val listener = object : androidx.media3.common.Player.Listener {
             override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                 currentSubIndex.intValue = player.currentMediaItemIndex
             }
         }
         player.addListener(listener)
    }
    
    androidx.compose.runtime.DisposableEffect(index) {
        onDispose {
            viewModel.releasePlayer(player)
        }
    }

    // Reactive Playlist Update
    LaunchedEffect(metadataResult) {
         viewModel.updatePlayerItems(player, metadataResult)
    }
    
    // Gesture Feedback State
    var seekAction by remember { mutableStateOf<String?>(null) } // "backward" or "forward"
    
    // Animation State for "Fake" Swipe (Hoisted from Container)
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Video Player Container (Applied with Offset)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { 
                    androidx.compose.ui.unit.IntOffset(offsetX.value.roundToInt(), 0) 
                }
        ) {
            VideoPlayerContainer(
                player = player,
                isFullyVisible = isSelected,
                isPaused = isPaused, 
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                onPageChange = { newIndex -> currentSubIndex.intValue = newIndex },
                onVideoTap = {}, // No-op, handled by overlay
                brandColor = brandColor
            )
            
            // Audio Mode Art Overlay
            if (isAudioMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Background blurred or dimmed
                    AsyncImage(
                        model = "https://archive.org/services/img/${item.identifier}",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.3f },
                        contentScale = ContentScale.Crop
                    )
                    
                    // Center Art
                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "Pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "PulseScale"
                    )

                    AsyncImage(
                        model = "https://archive.org/services/img/${item.identifier}",
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .size(250.dp)
                            .align(Alignment.Center)
                            .graphicsLayer {
                                scaleX = if (isSelected && !isPaused) scale else 1f
                                scaleY = if (isSelected && !isPaused) scale else 1f
                                shadowElevation = 20f
                                shape = RoundedCornerShape(12.dp)
                                clip = true
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // 2. Gesture Surface (Invisible, Top Level)
        // Handles BOTH Taps and Swipes on the same surface to avoid conflicts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val width = size.width
                            if (offset.x < width * 0.4f) {
                                // Double Tap LEFT: Rewind 10s
                                player.seekTo(player.currentPosition - 10000)
                                seekAction = "backward"
                            } else if (offset.x > width * 0.6f) {
                                // Double Tap RIGHT: Forward 30s
                                player.seekTo(player.currentPosition + 30000)
                                seekAction = "forward"
                            }
                            areControlsVisible = true
                        },
                        onTap = {
                            if (!areControlsVisible) {
                                // If hidden, just SHOW. Don't toggle playback.
                                areControlsVisible = true
                            } else {
                                // If visible, toggle playback.
                                isPaused = !isPaused
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = 100f
                            if (offsetX.value > threshold) {
                                 // Swipe Right (Previous)
                                 scope.launch {
                                     // Animate out
                                     offsetX.animateTo(1000f, androidx.compose.animation.core.tween(300))
                                     if (player.hasPreviousMediaItem()) {
                                         val prevIndex = player.currentMediaItemIndex - 1
                                         if (prevIndex >= 0) {
                                             player.seekTo(prevIndex, 0L)
                                             // Update index locally and via callback
                                             currentSubIndex.intValue = prevIndex
                                         }
                                     }
                                     // Snap to Left and animate in
                                     offsetX.snapTo(-1000f)
                                     offsetX.animateTo(0f, androidx.compose.animation.core.tween(300))
                                 }
                            } else if (offsetX.value < -threshold) {
                                 // Swipe Left (Next)
                                 scope.launch {
                                     // Animate out
                                     offsetX.animateTo(-1000f, androidx.compose.animation.core.tween(300))
                                     if (player.hasNextMediaItem()) {
                                         val nextIndex = player.currentMediaItemIndex + 1
                                         if (nextIndex < player.mediaItemCount) {
                                             player.seekTo(nextIndex, 0L)
                                             // Update index locally and via callback
                                             currentSubIndex.intValue = nextIndex
                                         }
                                     }
                                     // Snap to Right and animate in
                                     offsetX.snapTo(1000f)
                                     offsetX.animateTo(0f, androidx.compose.animation.core.tween(300))
                                 }
                            } else {
                                 // Snap back
                                 scope.launch { offsetX.animateTo(0f) }
                            }
                        },
                        onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                            change.consume()
                            scope.launch { 
                                val target = offsetX.value + dragAmount
                                offsetX.snapTo(target) 
                            }
                        }
                    )
                }
        )

        // SEEK FEEDBACK ANIMATION
        androidx.compose.animation.AnimatedVisibility(
            visible = seekAction != null,
            enter = fadeIn() + androidx.compose.animation.scaleIn(),
            exit = fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            LaunchedEffect(seekAction) {
                if (seekAction != null) {
                    delay(600)
                    seekAction = null
                }
            }
            
            Box(
                 modifier = Modifier
                     .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                     .padding(24.dp)
            ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Icon(
                         imageVector = if (seekAction == "backward") androidx.compose.material.icons.Icons.Default.Refresh else androidx.compose.material.icons.Icons.Default.PlayArrow, // Using generic icons for now
                         contentDescription = null,
                         tint = Color.White,
                         modifier = Modifier.size(48.dp)
                     )
                     Text(
                         text = if (seekAction == "backward") "-10s" else "+30s",
                         color = Color.White,
                         fontWeight = FontWeight.Bold
                     )
                 }
            }
        }

        // Overlay UI - WRAPPED in FillMaxSize Box to ensure overlap
        // We use AnimatedVisibility to handle the fade
        androidx.compose.animation.AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Scrubber (Slider) - Positioned just above the bottom gradient content
                // We need to poll the player position
                var currentPos by remember { mutableStateOf(0L) }
                var duration by remember { mutableStateOf(1L) } // Avoid /0
                var isDragging by remember { mutableStateOf(false) }
                
                LaunchedEffect(player, isDragging) {
                     while(true) {
                         if (!isDragging && player.duration > 0) {
                             currentPos = player.currentPosition
                             duration = player.duration
                         }
                         delay(200) // 5Hz update
                     }
                }

                // Bottom Content Container (Slider + Description)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        // Ensure bottom padding so it doesn't touch the navigation bar area excessively
                        // The original Description had padding(16.dp)
                ) {
                     // Slider (Now above text)
                     androidx.compose.material3.Slider(
                         value = currentPos.toFloat(),
                         onValueChange = { 
                             isDragging = true
                             currentPos = it.toLong()
                         },
                         onValueChangeFinished = {
                             player.seekTo(currentPos)
                             isDragging = false
                         },
                         valueRange = 0f..duration.toFloat(),
                         colors = androidx.compose.material3.SliderDefaults.colors(
                             thumbColor = brandColor,
                             activeTrackColor = brandColor,
                             inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                         ),
                         modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 72.dp) // Shortened to avoid side buttons
                            .height(20.dp) 
                     )
                     
                     // Description & Playlist Info
                     Column(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                            .fillMaxWidth(0.8f) 
                     ) {
                         // Playlist Indicator
                         if (metadataResult?.isMultiPart == true) {
                             Row(
                                 verticalAlignment = Alignment.CenterVertically,
                                 modifier = Modifier
                                     .padding(bottom = 8.dp)
                                     .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                     .padding(horizontal = 6.dp, vertical = 2.dp)
                             ) {
                                 Icon(
                                     imageVector = androidx.compose.material.icons.Icons.Default.List,
                                     contentDescription = "Playlist",
                                     tint = brandColor,
                                     modifier = Modifier.size(16.dp)
                                 )
                                 Spacer(modifier = Modifier.size(4.dp))
                                 Text(
                                     // Part X of Y (1-based index)
                                     text = "Part ${currentSubIndex.intValue + 1} of ${metadataResult.fileCount}",
                                     color = brandColor,
                                     style = MaterialTheme.typography.labelMedium
                                 )
                             }
                         }
 
                         Text(
                             text = item.title ?: "Untitled",
                             style = MaterialTheme.typography.titleLarge.copy(
                                 fontWeight = FontWeight.Bold,
                                 shadow = Shadow(color = Color.Black, blurRadius = 4f),
                                 color = Color.White
                             )
                         )
                         Spacer(modifier = Modifier.height(8.dp))
                         
                         val descriptionText = item.description ?: "No description"
                         val isLongDescription = descriptionText.length > 100 || descriptionText.lines().size > 2
                         
                         Text(
                             text = descriptionText,
                             style = MaterialTheme.typography.bodyMedium.copy(
                                 shadow = Shadow(color = Color.Black, blurRadius = 4f),
                                 color = Color.White
                             ),
                             maxLines = 2,
                             overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                         )
                         
                         if (isLongDescription) {
                             Text(
                                 text = "Show more",
                                 color = brandColor,
                                 style = MaterialTheme.typography.bodyMedium.copy(
                                     fontWeight = FontWeight.Bold,
                                     shadow = Shadow(color = Color.Black, blurRadius = 4f)
                                 ),
                                 modifier = Modifier
                                     .padding(top = 4.dp)
                                     .clickable { onShowDescription() }
                             )
                         }
                     }
                }
                
                // Right Side Actions
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Like/Bookmark Animation
                    val isBookmarked by viewModel.isBookmarked(item.identifier).collectAsState(initial = false)
                    
                    // Animation State
                    val transition = androidx.compose.animation.core.updateTransition(targetState = isBookmarked, label = "LikeTransition")
                    val tint by transition.animateColor(label = "Tint") { state ->
                        if (state) brandColor else Color.White
                    }
                    val scale by transition.animateFloat(
                        label = "Scale",
                        transitionSpec = {
                            if (targetState) {
                                androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                )
                            } else {
                                androidx.compose.animation.core.tween(durationMillis = 100)
                            }
                        }
                    ) { state ->
                        if (state) 1.2f else 1.0f
                    }

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Bookmark",
                        tint = tint,
                        modifier = Modifier
                            .size(40.dp)
                            .scale(scale)
                            .clickable { onBookmark() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Save", color = Color.White, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Full Screen Toggle
                    Icon(
                        imageVector = if (isFullScreen) androidx.compose.material.icons.Icons.Default.FullscreenExit else androidx.compose.material.icons.Icons.Default.Fullscreen, 
                        contentDescription = "Full Screen",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onToggleFullScreen() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (isFullScreen) "Exit" else "Full", color = Color.White, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(24.dp))
                    



                    


                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Info,
                        contentDescription = "Source",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onOpenSource() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Web", color = Color.White, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Comments Button
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Comment, // Ensure this icon exists or use generic
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onShowComments() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Reviews", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun VideoPlayerContainer(
    player: androidx.media3.exoplayer.ExoPlayer, // Received from parent
    isFullyVisible: Boolean,
    isPaused: Boolean, // Received from parent
    resizeMode: Int,
    onPageChange: (Int) -> Unit,
    onVideoTap: () -> Unit, // Callback to parent
    brandColor: Color = Color.Yellow
) {
    var showPauseIcon by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) } // Start with true/loading assumption
    
    // NOTE: Player creation/release is now handled by VideoItem (Parent)
    
    LaunchedEffect(isPaused) {
        if (isPaused) {
            showPauseIcon = true
        } else {
            showPauseIcon = true
            delay(1000)
            showPauseIcon = false
        }
    }

    // Player State Listener for Buffering
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
                    isBuffering = true
                } else if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    isBuffering = false
                }
            }
        }
        player.addListener(listener)
        // Set initial state
        if (player.playbackState == androidx.media3.common.Player.STATE_READY) {
            isBuffering = false
        }
        onDispose {
            player.removeListener(listener)
        }
    }
    
    // Explicitly handle play/pause based on selection and user interaction
    LaunchedEffect(isFullyVisible, isPaused) {
        if (isFullyVisible && !isPaused) {
            player.play()
        } else {
            player.pause()
        }
    }
    
    // Passive: No Internal Scroll. Logic moved to Parent.

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Just the player
        VideoPlayer(
            player = player,
            isPlaying = isFullyVisible && !isPaused,
            resizeMode = resizeMode,
            onVideoTap = onVideoTap,
            modifier = Modifier.fillMaxSize()
        )
        
        // BUFFERING / LOADING OVERLAY
        if (isBuffering && isFullyVisible) {
             Box(
                 modifier = Modifier
                     .fillMaxSize()
                     .background(Color.Black), // Fully opaque black background to hide "dead air"
                 contentAlignment = Alignment.Center
             ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     CircularProgressIndicator(
                         color = brandColor,
                         modifier = Modifier.size(48.dp)
                     )
                     Spacer(modifier = Modifier.height(16.dp))
                     Text(
                         text = "Diving into the Archive...",
                         style = MaterialTheme.typography.bodyLarge,
                         color = Color.White,
                         fontWeight = FontWeight.Bold
                     )
                 }
             }
        }

         // Pause/Play Icon Overlay
        AnimatedVisibility(
            visible = showPauseIcon && !isBuffering, // Don't show play/pause icon over loading screen
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Play" else "Pause",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(72.dp)
            )
        }
    }
}



@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             // You could add an Icon here if you had a resource
             Icon(
                 imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                 contentDescription = "Logo",
                 tint = Color.Yellow,
                 modifier = Modifier.size(64.dp)
             )
             Spacer(modifier = Modifier.height(16.dp))
             Text(
                 text = "Archive Dive",
                 style = MaterialTheme.typography.displayMedium,
                 fontWeight = FontWeight.Bold,
                 color = Color.White
             )
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                 text = "Discover history, one clip at a time.",
                 style = MaterialTheme.typography.bodyMedium,
                 color = Color.Gray
             )
        }
    }
}


@Composable
fun TutorialOverlay(brandColor: Color = Color.Yellow, onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    // 0: Browse (Vertical Swipe)
    // 1: Navigation (Horizontal Swipe)
    // 2: Seek (Double Tap)
    // 3: Filter (Top Right)
    // 4: Save (Bottom Right Heart)
    // 5: Full Screen (Bottom Right)
    // 6: Web Browser (Bottom Right)

    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            // .navigationBarsPadding() // Removed to match absolute positioning of VideoItem
            // alpha 0.99f is needed for BlendMode.Clear to work on some Android versions
            .graphicsLayer { this.alpha = 0.99f }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                 if (step < 7) {
                     step++
                 } else {
                     onDismiss()
                 }
            }
    ) {
        // Scrim with Holes
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val filterX = width - with(density) { 30.dp.toPx() }
            val filterY = with(density) { 62.dp.toPx() }
            
            // Adjusted X to be closer to edge (Icon center is approx 20dp + 16dp padding = 36dp)
            // But visual center might "feel" closer if the icon itself is the target
            // Let's widen the radius and nudge it slightly right to ensure coverage
            val rightActionX = width - with(density) { 28.dp.toPx() } // Moved closer to edge from 36
            
            val saveY = height - with(density) { 332.dp.toPx() } // Shifted up for 4th item
            val fullY = height - with(density) { 248.dp.toPx() } // Shifted up for 3rd item
            val webY = height - with(density) { 164.dp.toPx() } // Shifted up for 2nd item
            val commentY = height - with(density) { 80.dp.toPx() } // Bottom item

            // Radii
            val holeRadius = with(density) { 36.dp.toPx() } // Increased from 30
            val ringRadius = with(density) { 40.dp.toPx() } // Increased from 34
            val strokeWidth = with(density) { 4.dp.toPx() }

            // Draw Dimmed Background
            drawRect(Color.Black.copy(alpha = 0.6f))

            // Draw Holes / Highlights based on step
            when (step) {
                3 -> { // Filter
                    drawCircle(
                        color = Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(filterX, filterY),
                        radius = holeRadius,
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                        color = brandColor,
                        center = androidx.compose.ui.geometry.Offset(filterX, filterY),
                        radius = ringRadius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                }
                4 -> { // Save
                    drawCircle(
                        color = Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(rightActionX, saveY),
                        radius = holeRadius,
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                        color = brandColor,
                        center = androidx.compose.ui.geometry.Offset(rightActionX, saveY),
                        radius = ringRadius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                }
                5 -> { // Full Screen
                    drawCircle(
                         color = Color.Transparent,
                         center = androidx.compose.ui.geometry.Offset(rightActionX, fullY),
                         radius = holeRadius,
                         blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                         color = brandColor,
                         center = androidx.compose.ui.geometry.Offset(rightActionX, fullY),
                         radius = ringRadius,
                         style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                }
                6 -> { // Web
                    drawCircle(
                        color = Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(rightActionX, webY),
                        radius = holeRadius,
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                        color = brandColor,
                        center = androidx.compose.ui.geometry.Offset(rightActionX, webY),
                        radius = ringRadius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                }
                7 -> { // Comments
                    drawCircle(
                        color = Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(rightActionX, commentY),
                        radius = holeRadius,
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                        color = brandColor,
                        center = androidx.compose.ui.geometry.Offset(rightActionX, commentY),
                        radius = ringRadius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                }
            }
        }

        // Content Area (Text & Icons)
        Box(modifier = Modifier.fillMaxSize()) {
             when (step) {
                 0 -> { // Vertical Swipe
                     Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Swipe Up & Down",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "to browse videos",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            androidx.compose.material3.TextButton(onClick = onDismiss) {
                                Text("Skip", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { step++ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                            ) {
                                Text("Next", color = Color.Black)
                            }
                        }
                    }
                 }
                 1 -> { // Horizontal Swipe
                     Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.width(32.dp))
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Swipe Left & Right",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "to move through playlists",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            androidx.compose.material3.TextButton(onClick = onDismiss) {
                                Text("Skip", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { step++ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                            ) {
                                Text("Next", color = Color.Black)
                            }
                        }
                    }
                 }
                 2 -> { // Double Tap
                     Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row {
                             Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                             Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Double Tap Sides",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "to seek 10s backward/forward",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            androidx.compose.material3.TextButton(onClick = onDismiss) {
                                Text("Skip", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { step++ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                            ) {
                                Text("Next", color = Color.Black)
                            }
                        }
                    }
                 }
                 3 -> { // Filter
                     Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Filter Content",
                            color = brandColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "By Decade or Language",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            androidx.compose.material3.TextButton(onClick = onDismiss) {
                                Text("Skip", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { step++ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                            ) {
                                Text("Next", color = Color.Black)
                            }
                        }
                    }
                 }
                 4 -> { // Save (Heart)
                     Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                         Text(
                            text = "Save Favorites",
                            color = brandColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                         Text(
                            text = "Tap the heart to bookmark",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            androidx.compose.material3.TextButton(onClick = onDismiss) {
                                Text("Skip", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { step++ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                            ) {
                                Text("Next", color = Color.Black)
                            }
                        }
                    }
                 }
                 5 -> { // Full Screen
                     Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                         Text(
                            text = "Full Screen",
                            color = brandColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                         Text(
                            text = "Hide distractions",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            androidx.compose.material3.TextButton(onClick = onDismiss) {
                                Text("Skip", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { step++ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                            ) {
                                Text("Next", color = Color.Black)
                            }
                        }
                    }
                 }
                 6 -> { // Web
                     Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                         Text(
                            text = "View Source",
                            color = brandColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                         Text(
                            text = "Go to Archive.org page",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            androidx.compose.material3.TextButton(onClick = onDismiss) {
                                Text("Skip", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { step++ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                            ) {
                                Text("Next", color = Color.Black)
                            }
                        }
                    }
                 }
                 7 -> { // Comments
                     Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                         Text(
                            text = "Read Reviews",
                            color = brandColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                         Text(
                            text = "See what others are saying",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                         Spacer(modifier = Modifier.height(16.dp))
                         androidx.compose.material3.Button(
                             onClick = onDismiss,
                             colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = brandColor)
                         ) {
                             Text("Got it!", color = Color.Black)
                         }
                    }
                 }
             }
        }

        
        // Step Indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(7) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (index == step) brandColor else Color.Gray)
                )
            }
        }
    }
}
