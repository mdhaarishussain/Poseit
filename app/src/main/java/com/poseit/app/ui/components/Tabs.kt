package com.poseit.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** v1 text tabs, now drawn as [PillTabs] (the v2 tabs). */
@Deprecated("Use PillTabs", ReplaceWith("PillTabs(labels, selectedIndex, onSelect, modifier, edgePadding)"))
@Composable
fun StudioTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 0.dp,
) = PillTabs(labels, selectedIndex, onSelect, modifier, edgePadding)
