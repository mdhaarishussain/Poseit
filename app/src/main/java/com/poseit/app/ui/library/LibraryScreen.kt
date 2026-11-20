package com.poseit.app.ui.library

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Home screen: browse / search / filter the pose library. */
@Composable
fun LibraryScreen(
    onOpenPose: (String) -> Unit,
    onOpenCamera: (String?) -> Unit,
    onOpenGallery: () -> Unit,
    onCreateFromPhoto: () -> Unit,
    onOpenEditor: (String?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Text("LibraryScreen – coming soon")
}
