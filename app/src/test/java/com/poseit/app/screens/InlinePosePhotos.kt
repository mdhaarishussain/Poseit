package com.poseit.app.screens

import com.poseit.app.data.PosePhotos
import kotlinx.coroutines.Dispatchers
import org.junit.rules.ExternalResource

/**
 * For screenshot tests that show sample photos: decodes them (and reads licence texts) inside the composition's own
 * frame instead of on Dispatchers.IO, so `waitForIdle()` waits for them and the capture shows the real photo rather
 * than its placeholder. Production code is unchanged; the rule restores Dispatchers.IO afterwards.
 *
 *     @get:Rule val photos = InlinePosePhotos()
 */
class InlinePosePhotos : ExternalResource() {
    override fun before() {
        PosePhotos.ioDispatcher = Dispatchers.Unconfined
    }

    override fun after() {
        PosePhotos.ioDispatcher = Dispatchers.IO
    }
}
