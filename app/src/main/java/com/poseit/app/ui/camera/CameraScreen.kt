package com.poseit.app.ui.camera

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** The live pose-guide camera. [initialPoseId] null = last used / first pose. */
@Composable
fun CameraScreen(initialPoseId: String?, onBack: () -> Unit, onOpenGallery: () -> Unit) {
    Text("CameraScreen – coming soon")
}
