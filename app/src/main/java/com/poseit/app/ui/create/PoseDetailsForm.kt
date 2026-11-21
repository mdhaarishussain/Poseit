package com.poseit.app.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.ui.components.FilterPill
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space

/** Longest pose name the forms accept. */
internal const val POSE_NAME_MAX_LENGTH = 40

/**
 * Name, category and optional tags of a pose being saved (used by "create from photo" and the editor's save
 * sheet), laid out like Astryx fields: a label above each part (with an "Optional" indicator where it applies),
 * a cream text field with a 1dp dark outline, then filter pills for the category (pick one) and the tags (pick
 * any). On cream only: the canvas or a sheet.
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
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            FieldLabel("Name")
            NameField(name, onNameChange, onDone = { focusManager.clearFocus() })
        }
        PillGroup("Category") {
            PoseCategory.entries.forEach { option ->
                FilterPill(label = option.label, selected = option == category, onClick = { onCategoryChange(option) })
            }
        }
        TagGroup("Mood", PoseTag.entries.filter { !it.isLocation }, tags, onTagsChange)
        TagGroup("Where", PoseTag.entries.filter { it.isLocation }, tags, onTagsChange)
    }
}

/**
 * A cream field (the surface colour) with 12dp corners and a 1dp dark outline, 2dp while focused; signal red with
 * a prompt below while the name is empty, the character count below otherwise.
 */
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
                Text("Give your pose a name", style = PoseitType.supporting)
            } else {
                Text(
                    "${name.length} / $POSE_NAME_MAX_LENGTH",
                    style = PoseitType.numeral(PoseitType.supporting.fontSize),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        },
        textStyle = PoseitType.bodyLarge,
        shape = PoseitShapes.Control,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PoseitColors.backgroundSurface,
            unfocusedContainerColor = PoseitColors.backgroundSurface,
            errorContainerColor = PoseitColors.backgroundSurface,
            focusedTextColor = PoseitColors.textPrimary,
            unfocusedTextColor = PoseitColors.textPrimary,
            errorTextColor = PoseitColors.textPrimary,
            cursorColor = PoseitColors.textPrimary,
            errorCursorColor = PoseitColors.signalMiss,
            focusedBorderColor = PoseitColors.borderEmphasized,
            unfocusedBorderColor = PoseitColors.borderEmphasized,
            errorBorderColor = PoseitColors.signalMiss,
            focusedSupportingTextColor = PoseitColors.textSecondary,
            unfocusedSupportingTextColor = PoseitColors.textSecondary,
            errorSupportingTextColor = PoseitColors.signalMiss,
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        // The label above is a separate Text, so the field names itself for screen readers.
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Name" },
    )
}

@Composable
private fun TagGroup(title: String, options: List<PoseTag>, selected: Set<PoseTag>, onChange: (Set<PoseTag>) -> Unit) {
    PillGroup(title, optional = true) {
        options.forEach { tag ->
            val isSelected = tag in selected
            FilterPill(
                label = tag.label,
                selected = isSelected,
                onClick = { onChange(if (isSelected) selected - tag else selected + tag) },
            )
        }
    }
}

/** A field's label (Label, primary) and, for an optional part, "Optional" in Supporting secondary beside it. */
@Composable
private fun FieldLabel(text: String, optional: Boolean = false) {
    Row(Modifier.semantics(mergeDescendants = true) {}) {
        Text(text, style = PoseitType.label, color = PoseitColors.textPrimary, modifier = Modifier.alignByBaseline())
        if (optional) {
            Spacer(Modifier.width(Space.s))
            Text("Optional", style = PoseitType.supporting, color = PoseitColors.textSecondary, modifier = Modifier.alignByBaseline())
        }
    }
}

/** A label over a wrapping row of filter pills, 8dp apart. */
@Composable
private fun PillGroup(title: String, optional: Boolean = false, pills: @Composable () -> Unit) {
    Column {
        FieldLabel(title, optional)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) { pills() }
    }
}
