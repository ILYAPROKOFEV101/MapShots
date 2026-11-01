package com.ilya.safarshots.feature.collage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LayerTransformGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun twoPointersReportScaleAndRotationInOneGesture() {
        var cumulativeZoom = 1f
        var cumulativeRotation = 0f
        var committed = false

        composeRule.setContent {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .testTag(TARGET_TAG)
                    .pointerInput(Unit) {
                        detectLayerTransformGestures(
                            onStart = {},
                            onTransform = { _, zoom, rotation ->
                                cumulativeZoom *= zoom
                                cumulativeRotation += rotation
                            },
                            onEnd = { changed -> committed = changed },
                            onCancel = {},
                        )
                    },
            )
        }

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            val midpoint = center
            down(0, Offset(midpoint.x - 40f, midpoint.y))
            down(1, Offset(midpoint.x + 40f, midpoint.y))
            moveTo(0, Offset(midpoint.x, midpoint.y - 80f), delayMillis = 100L)
            moveTo(1, Offset(midpoint.x, midpoint.y + 80f), delayMillis = 100L)
            up(0)
            up(1)
        }

        composeRule.runOnIdle {
            assertTrue("The finished gesture must be committed", committed)
            assertTrue("Pinch-out must increase the layer scale", cumulativeZoom > 1.5f)
            assertTrue("Turning both pointers must rotate the layer", abs(cumulativeRotation) > 45f)
        }
    }

    private companion object {
        const val TARGET_TAG = "transform-target"
    }
}
