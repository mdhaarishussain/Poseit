package com.poseit.app.ui.editor

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Drag joints of a mannequin to create / edit a custom pose. [basePoseId] null = start from neutral. */
@Composable
fun PoseEditorScreen(basePoseId: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    Text("PoseEditorScreen – coming soon")
}
