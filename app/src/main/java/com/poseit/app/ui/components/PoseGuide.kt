package com.poseit.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.poseit.app.pose.CameraAdvice
import com.poseit.app.pose.ShotType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.toneColors

/*
 * How to do a pose, shown the same way on the detail screen and in the camera's pose tips sheet: the numbered steps
 * and the camera setup.
 */

/**
 * A real sequence (the steps of a pose, the steps of making a pose from a photo): small Medium numerals in the
 * secondary colour in a 28dp column, on the text's baseline. Colours follow the surface.
 */
@Composable
fun NumberedSteps(items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier) {
        items.forEachIndexed { index, item ->
            Row(Modifier.padding(vertical = Space.s)) {
                Text(
                    "${index + 1}",
                    style = PoseitType.numeral(15.sp, FontWeight.Medium),
                    color = toneColors().textSecondary,
                    modifier = Modifier
                        .width(28.dp)
                        .alignByBaseline(),
                )
                Text(item, style = PoseitType.body, color = toneColors().textPrimary, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

/** "How to do it" and the pose's tips as [NumberedSteps]. */
@Composable
fun PoseSteps(tips: List<String>, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionHeading("How to do it")
        Spacer(Modifier.height(Space.xs))
        NumberedSteps(tips)
    }
}

/** "Camera setup": camera height, framing, orientation and the photographer's tip, as rows of one group. */
@Composable
fun CameraSetup(camera: CameraAdvice, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionHeading("Camera setup")
        Spacer(Modifier.height(Space.xs))
        SetupRow(camera.height.label, camera.height.advice)
        HairlineDivider()
        SetupRow(camera.shot.label, shotAdvice(camera.shot))
        HairlineDivider()
        // The camera is portrait-only: wide (landscape-frame) poses are letterboxed into the portrait scene.
        if (camera.landscape) SetupRow("Wide shot", WIDE_SHOT_ADVICE) else SetupRow("Portrait", "Hold the phone upright.")
        val note = camera.note
        if (!note.isNullOrBlank()) {
            HairlineDivider()
            SetupRow("Photographer tip", note)
        }
    }
}

/** What to do with a wide (landscape-frame) pose in the portrait-only camera. */
const val WIDE_SHOT_ADVICE = "Hold the phone upright and step back: the wide guide sits in the middle of the frame."

@Composable
private fun SetupRow(title: String, body: String) {
    SettingRow(title = title, subtitle = body, contentPadding = PaddingValues(vertical = Space.m))
}

private fun shotAdvice(shot: ShotType): String = when (shot) {
    ShotType.FULL_BODY -> "Head to toe, with a little space above the head and below the feet."
    ShotType.THREE_QUARTER -> "Frame from the head to just above the knees. Never crop right at a joint."
    ShotType.HALF_BODY -> "Frame from the head down to the waist."
    ShotType.CLOSE_UP -> "Head and shoulders fill the frame. Focus on the eyes."
}
