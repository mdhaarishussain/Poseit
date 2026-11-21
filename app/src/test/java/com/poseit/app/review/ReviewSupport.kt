package com.poseit.app.review

import android.util.Log
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.shadows.ShadowLog
import java.util.Collections

/**
 * Fails the test when any thread (coroutine dispatchers, CameraX executors, ...) dies with an uncaught exception
 * that a device would also crash on, and prints every ERROR log line so caught failures stay visible.
 */
class CrashCollector : TestWatcher() {
    private val crashes = Collections.synchronizedList(mutableListOf<Pair<String, Throwable>>())
    private var previous: Thread.UncaughtExceptionHandler? = null

    override fun starting(description: Description) {
        ShadowLog.stream = null
        ShadowLog.clear()
        previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e -> crashes += t.name to e }
    }

    override fun finished(description: Description) {
        Thread.setDefaultUncaughtExceptionHandler(previous)
        ShadowLog.getLogs()
            .filter { it.type >= Log.WARN && it.tag !in NOISY_TAGS }
            .forEach { println("LOG ${if (it.type == Log.ERROR) "E" else "W"}/${it.tag}: ${it.msg} ${it.throwable?.let { t -> "| " + t }.orEmpty()}") }
    }

    /** Call at the end of a test body. */
    fun assertNoCrashes() {
        val list = synchronized(crashes) { crashes.toList() }
        if (list.isNotEmpty()) {
            val (thread, error) = list.first()
            throw AssertionError("Uncaught exception on thread '$thread' (${list.size} total)", error)
        }
    }

    private companion object {
        val NOISY_TAGS = setOf("OverlayConfig", "PackageParser", "WindowOnBackDispatcher", "ResourcesCompat")
    }
}
