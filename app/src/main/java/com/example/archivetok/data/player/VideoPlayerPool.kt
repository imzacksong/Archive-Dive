package com.example.archivetok.data.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoPlayerPool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSourceFactory: DataSource.Factory
) {
    private val playerPool: Queue<ExoPlayer> = LinkedList()
    private val activePlayers = mutableListOf<ExoPlayer>()
    private val maxPoolSize = 5 // Enough for Prev, Curr, Next, +2 Vertical
    
    @OptIn(UnstableApi::class)
    fun acquirePlayer(): ExoPlayer {
        android.util.Log.d("VideoPlayerPool", "Acquire. Active: ${activePlayers.size}, Pool: ${playerPool.size}")
        if (playerPool.isNotEmpty()) {
            val player = playerPool.poll()!!
            activePlayers.add(player)
            return player
        }

        // Use default LoadControl for stability
        val loadControl = DefaultLoadControl.Builder().build()

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
        
        activePlayers.add(player)
        return player
    }

    fun releasePlayer(player: ExoPlayer) {
        activePlayers.remove(player)
        player.stop()
        player.clearMediaItems()
        player.clearVideoSurface() // Ensure surface is detached
        
        if (playerPool.size < maxPoolSize) {
            playerPool.offer(player)
        } else {
            player.release()
        }
        android.util.Log.d("VideoPlayerPool", "Released. Active: ${activePlayers.size}, Pool: ${playerPool.size}")
    }
    
    @OptIn(UnstableApi::class)
    fun prepare(url: String) {
        // Find an unused player in the pool and prepare it, or create one?
        // Actually, for "warming up", we want to prepare the *next* player the screen will request.
        // A simple way is to have the screen acquire it early.
        // For this pool, we just provide instances. 
        // Advanced: We could have a "preloadPlayer" method that prepares a player in the pool.
        
        if (playerPool.isEmpty() && activePlayers.size < maxPoolSize + 1) {
             val player = acquirePlayer()
             releasePlayer(player) // Create and put in pool
        }
        
        val player = playerPool.peek()
        if (player != null) {
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.prepare()
            // We leave it paused. When acquired, it's ready to play.
        }
    }

    fun releaseAll() {
        activePlayers.forEach { it.release() }
        playerPool.forEach { it.release() }
        activePlayers.clear()
        playerPool.clear()
    }
}
