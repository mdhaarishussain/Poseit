package com.poseit.app.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag

/** Longest pose name the forms accept. */
internal const val POSE_NAME_MAX_LENGTH = 40

/** Name, category and optional tags of a pose being saved (used by "create from photo" and the editor). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PoseDetailsForm(
    name: String,
    onNameChange: (String) -> Unit,
    category: PoseCategory,
    onCategoryChange: (PoseCategory) -> Unit,
    tags: Set<PoseTag>,
    onTagsChange: (Set<PoseTag>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { onNameChange(it.take(POSE_NAME_MAX_LENGTH)) },
            label = { Text("Name") },
            singleLine = true,
            isError = name.isBlank(),
            supportingText = {
                Text(if (name.isBlank()) "Give your pose a name" else "${name.length} / $POSE_NAME_MAX_LENGTH")
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = category.label,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PoseCategory.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        trailingIcon = if (option == category) {
                            { Icon(Icons.Rounded.Check, contentDescription = "Selected") }
                        } else {
                            null
                        },
                        onClick = {
                            onCategoryChange(option)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        TagSection("Mood", PoseTag.entries.filter { !it.isLocation }, tags, onTagsChange)
        TagSection("Where", PoseTag.entries.filter { it.isLocation }, tags, onTagsChange)
    }
}

@Composable
private fun TagSection(title: String, options: List<PoseTag>, selected: Set<PoseTag>, onChange: (Set<PoseTag>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$title · optional",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { tag ->
                val isSelected = tag in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onChange(if (isSelected) selected - tag else selected + tag) },
                    label = { Text(tag.label) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
