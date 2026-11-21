package com.poseit.app.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.ui.components.StudioChip
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

/** Longest pose name the forms accept. */
internal const val POSE_NAME_MAX_LENGTH = 40

/**
 * Name, category and optional tags of a pose being saved (used by "create from photo" and the editor's save
 * sheet): a Sheet text field, then text-only chips for the category (pick one) and the tags (pick any). Each part
 * has a Label caption above it.
 */
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xl)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Caption("Name")
            NameField(name, onNameChange, onDone = { focusManager.clearFocus() })
        }
        ChipGroup("Category") {
            PoseCategory.entries.forEach { option ->
                StudioChip(label = option.label, selected = option == category, onClick = { onCategoryChange(option) })
            }
        }
        TagGroup("Mood (optional)", PoseTag.entries.filter { !it.isLocation }, tags, onTagsChange)
        TagGroup("Where (optional)", PoseTag.entries.filter { it.isLocation }, tags, onTagsChange)
    }
}

/** A Sheet field with a Hairline outline: Ink when focused, Miss while the name is empty. */
@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit, onDone: () -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = { onNameChange(it.take(POSE_NAME_MAX_LENGTH)) },
        singleLine = true,
        isError = name.isBlank(),
        supportingText = {
            // The count sits at the end, under the field's right edge; the prompt reads from the start.
            if (name.isBlank()) {
                Text("Give your pose a name")
            } else {
                Text("${name.length} / $POSE_NAME_MAX_LENGTH", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            }
        },
        textStyle = StudioType.body,
        shape = StudioShapes.Small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Studio.Sheet,
            unfocusedContainerColor = Studio.Sheet,
            errorContainerColor = Studio.Sheet,
            focusedTextColor = Studio.Ink,
            unfocusedTextColor = Studio.Ink,
            errorTextColor = Studio.Ink,
            cursorColor = Studio.Ink,
            errorCursorColor = Studio.Miss,
            focusedBorderColor = Studio.Ink,
            unfocusedBorderColor = Studio.Hairline,
            errorBorderColor = Studio.Miss,
            focusedSupportingTextColor = Studio.Graphite,
            unfocusedSupportingTextColor = Studio.Graphite,
            errorSupportingTextColor = Studio.Miss,
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        // The caption above is a separate Text, so the field names itself for screen readers.
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Name" },
    )
}

@Composable
private fun TagGroup(title: String, options: List<PoseTag>, selected: Set<PoseTag>, onChange: (Set<PoseTag>) -> Unit) {
    ChipGroup(title) {
        options.forEach { tag ->
            val isSelected = tag in selected
            StudioChip(
                label = tag.label,
                selected = isSelected,
                onClick = { onChange(if (isSelected) selected - tag else selected + tag) },
            )
        }
    }
}

/** The Label-style Graphite caption over a field or a chip group. */
@Composable
private fun Caption(text: String) {
    Text(text, style = StudioType.label, color = Studio.Graphite)
}

/** A caption over a wrapping row of chips, 8dp apart. */
@Composable
private fun ChipGroup(title: String, chips: @Composable () -> Unit) {
    Column {
        Caption(title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) { chips() }
    }
}
