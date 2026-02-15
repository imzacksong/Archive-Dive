package com.example.archivetok.data.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoPreloader @Inject constructor(
    private val dataSourceFactory: DataSource.Factory
) {
    private val preloadScope = CoroutineScope(Dispatchers.IO + Job())
    private val activePreloads = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    @OptIn(UnstableApi::class)
    fun preload(url: String) {
        if (activePreloads.contains(url)) return
        if (activePreloads.size >= 3) return // Limit concurrent preloads

        preloadScope.launch {
            activePreloads.add(url)
            try {
                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setLength(3 * 1024 * 1024) // Preload 3MB (Reduced for stability)
                    .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                    .build()
                
                val dataSource = dataSourceFactory.createDataSource()
                if (dataSource is androidx.media3.datasource.cache.CacheDataSource) {
                    val writer = CacheWriter(
                        dataSource,
                        dataSpec,
                        null,
                        null
                    )
                    writer.cache()
                }
            } catch (e: Exception) {
                // Log but don't crash
                android.util.Log.e("VideoPreloader", "Failed to preload $url", e)
            } finally {
                activePreloads.remove(url)
            }
        }
    }
}
