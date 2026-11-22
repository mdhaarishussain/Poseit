package com.poseit.app

import android.app.Application
import android.content.Context
import com.poseit.app.data.PhotoRepository
import com.poseit.app.data.PosePhotos
import com.poseit.app.data.PoseRepository
import com.poseit.app.data.SettingsRepository

class PoseitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // The sample photos' credits are read once; do it off the main thread before a screen asks for them.
        container.posePhotos.warmUp()
    }

    // Decoded sample photos are a cache: give the memory back when the system asks, so a backgrounded Poseit is a
    // less likely target for the low-memory killer (and keeps the camera or editor state the user returns to).
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::container.isInitialized) container.posePhotos.trim(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::container.isInitialized) container.posePhotos.clear()
    }
}

/** Manual dependency container (no DI framework needed for an app this size). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val poseRepository: PoseRepository by lazy { PoseRepository(appContext) }
    val photoRepository: PhotoRepository by lazy { PhotoRepository(appContext) }

    /** Sample photos of the built-in poses and their credits. */
    val posePhotos: PosePhotos by lazy { PosePhotos(appContext) }
}

val Context.appContainer: AppContainer get() = (applicationContext as PoseitApp).container
