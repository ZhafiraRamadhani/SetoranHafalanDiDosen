package com.example.setoranhafalandosen

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger

class SetoranApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Gunakan maksimal 25% dari available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Gunakan maksimal 2% dari storage
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(false) // Disable hardware bitmaps untuk kompatibilitas
            .crossfade(true)
            .logger(DebugLogger()) // Enable logging untuk debugging
            .build()
    }
}
