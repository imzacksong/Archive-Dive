package com.example.archivetok.ui

import android.content.Intent
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
    val context = LocalContext.current

    val pagerState = rememberPagerState(pageCount = { videoList.size })
    
    // TUTORIAL STATE (Hoisted)
    var showTutorial by remember { mutableStateOf(!viewModel.isTutorialShown() && viewModel.isOnboardingCompleted()) }
    
    // SPLASH SCREEN STATE
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
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow)
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
                     onBookmark = { viewModel.toggleBookmark(item) },
                     onOpenSource = {
                          val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/details/${item.identifier}"))
                          context.startActivity(browserIntent)
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

        // Top Bar
        // REBRANDING: Added Title
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
                    text = "Saved",
                    color = if (showBookmarks) Color.White else Color.Gray,
                    fontWeight = if (showBookmarks) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { if (!showBookmarks) viewModel.toggleShowBookmarks() }
                        .padding(8.dp)
                )
            }
        }

        // Filter Icon (Top Right)
        var showFilterSheet by remember { mutableStateOf(false) }
        val currentFilter by viewModel.filterDecade.collectAsState()
        val currentLanguage by viewModel.filterLanguage.collectAsState()

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.FilterList, // Use FilterList or similar
                contentDescription = "Filter",
                tint = if (currentFilter != null || currentLanguage != null) Color.Yellow else Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { showFilterSheet = true }
            )
        }

        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = currentFilter,
                onFilterSelected = { viewModel.setFilterDecade(it) },
                currentLanguage = currentLanguage,
                onLanguageSelected = { viewModel.setFilterLanguage(it) },
                onDismissRequest = { showFilterSheet = false }
            )
        }
        
        // TUTORIAL OVERLAY
        if (showTutorial && !showSplash) {
            TutorialOverlay(
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
    onBookmark: () -> Unit,
    onOpenSource: () -> Unit
) {
    val context = LocalContext.current
    // QoL: Default to FIT (Original Aspect Ratio)
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    
    // Playback State (Hoisted for Overlay Visibility logic)
    var isPaused by remember { mutableStateOf(false) }
    
    // Overlay Visibility State
    var areControlsVisible by remember { mutableStateOf(true) }
    
    // Auto-hide controls when playing (override if tutorial is active)
    LaunchedEffect(isPaused, areControlsVisible, isTutorialActive) {
        if (isTutorialActive) {
            areControlsVisible = true
        } else if (!isPaused && areControlsVisible) {
            delay(8000) // Wait 8 seconds
            areControlsVisible = false
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
                resizeMode = resizeMode,
                onPageChange = { newIndex -> currentSubIndex.intValue = newIndex },
                onVideoTap = {} // No-op, handled by overlay
            )
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

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
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
                                tint = Color.Yellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                // Part X of Y (1-based index)
                                text = "Part ${currentSubIndex.intValue + 1} of ${metadataResult.fileCount}",
                                color = Color.Yellow,
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
                    Text(
                        text = item.description?.take(100) ?: "No description",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            shadow = Shadow(color = Color.Black, blurRadius = 4f),
                            color = Color.White
                        ),
                        maxLines = 2
                    )
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
                        if (state) Color.Yellow else Color.White
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
                    
                    // Aspect Ratio Toggle
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Resize",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable {
                                resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                } else {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                }
                            }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) "Fill" else "Fit",
                        color = Color.White,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Source",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onOpenSource() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Web", color = Color.White, fontSize = 12.sp)
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
    onVideoTap: () -> Unit // Callback to parent
) {
    var showPauseIcon by remember { mutableStateOf(false) }
    
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

         // Pause/Play Icon Overlay
        AnimatedVisibility(
            visible = showPauseIcon,
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
fun TutorialOverlay(onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    // 0: Browse (Vertical Swipe)
    // 1: Navigation (Horizontal Swipe)
    // 2: Seek (Double Tap)
    // 3: Filter (Top Right)
    // 4: Save (Bottom Right Heart)
    // 5: Aspect Ratio (Bottom Right)
    // 6: Web Browser (Bottom Right)

    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            // .navigationBarsPadding() // Removed to match absolute positioning of VideoItem
            // alpha 0.99f is needed for BlendMode.Clear to work on some Android versions
            .graphicsLayer { this.alpha = 0.99f }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                 // Prevent clicks passing through
            }
    ) {
        // Scrim with Holes
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val filterX = width - with(density) { 30.dp.toPx() }
            val filterY = with(density) { 62.dp.toPx() }
            
            // Calculated based on VideoItem layout (Bottom -> Top)
            // Web: 40(pad) + 14(text) + 4 + 20(icon half) = 78
            // Aspect: 78 + 20 + 24(spacer) + 14 + 4 + 20 = 160
            // Save: 160 + 20 + 24 + 14 + 4 + 20 = 242
            
            val saveX = width - with(density) { 36.dp.toPx() }
            val saveY = height - with(density) { 242.dp.toPx() }

            val aspectX = width - with(density) { 36.dp.toPx() }
            val aspectY = height - with(density) { 160.dp.toPx() }

            val webX = width - with(density) { 36.dp.toPx() }
            val webY = height - with(density) { 78.dp.toPx() }

            // Draw Dimmed Background
            drawRect(Color.Black.copy(alpha = 0.6f))

            // Draw Holes / Highlights based on step
            when (step) {
                3 -> { // Filter
                    drawCircle(
                        color = Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(filterX, filterY),
                        radius = with(density) { 30.dp.toPx() },
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                        color = Color.Yellow,
                        center = androidx.compose.ui.geometry.Offset(filterX, filterY),
                        radius = with(density) { 34.dp.toPx() },
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 4.dp.toPx() })
                    )
                }
                4 -> { // Save
                    drawCircle(
                        color = Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(saveX, saveY),
                        radius = with(density) { 30.dp.toPx() },
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                        color = Color.Yellow,
                        center = androidx.compose.ui.geometry.Offset(saveX, saveY),
                        radius = with(density) { 34.dp.toPx() },
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 4.dp.toPx() })
                    )
                }
                5 -> { // Aspect Ratio
                    drawCircle(
                        color = Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(aspectX, aspectY),
                        radius = with(density) { 30.dp.toPx() },
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                        color = Color.Yellow,
                        center = androidx.compose.ui.geometry.Offset(aspectX, aspectY),
                        radius = with(density) { 34.dp.toPx() },
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 4.dp.toPx() })
                    )
                }
                6 -> { // Web
                    drawCircle(
                        color = Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(webX, webY),
                        radius = with(density) { 30.dp.toPx() },
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawCircle(
                        color = Color.Yellow,
                        center = androidx.compose.ui.geometry.Offset(webX, webY),
                        radius = with(density) { 34.dp.toPx() },
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 4.dp.toPx() })
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
                    }
                 }
                 3 -> { // Filter
                     Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 110.dp, end = 24.dp), 
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Filter Content",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "By Decade or Language",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                 }
                 4 -> { // Save (Heart)
                     Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 290.dp, end = 24.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                         Text(
                            text = "Save Favorites",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                         Text(
                            text = "Tap the heart to bookmark",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                 }
                 5 -> { // Aspect Ratio
                     Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 210.dp, end = 24.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                         Text(
                            text = "Adjust View",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                         Text(
                            text = "Toggle Fit/Fill Mode",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                 }
                 6 -> { // Web
                     Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 130.dp, end = 24.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                         Text(
                            text = "View Source",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                         Text(
                            text = "Open in Web Browser",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                 }
             }
        }

        // Navigation Buttons (Bottom Center)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            // Skip Button (Left)
            if (step < 6) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Skip", color = Color.Gray)
                }
            } else {
                Spacer(modifier = Modifier.width(64.dp)) // Spacer to balance layout
            }

            // Next/Done Button (Center)
            androidx.compose.material3.Button(
                onClick = {
                    if (step < 6) {
                        step++
                    } else {
                        onDismiss()
                    }
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow),
                modifier = Modifier.width(140.dp)
            ) {
                Text(
                    text = if (step < 6) "Next" else "Got it!",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
             
            // Right spacer for initial centered look of "Next" or just balance
             if (step < 6) {
                 Spacer(modifier = Modifier.width(64.dp))
             } else {
                 Spacer(modifier = Modifier.width(64.dp))
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
                        .background(if (index == step) Color.Yellow else Color.Gray)
                )
            }
        }
    }
}
