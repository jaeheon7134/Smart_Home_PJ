package com.example.smart_home_wallpad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ControlSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    enabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    fun valueAt(x: Float, width: Float): Float {
        val inset = 12f
        return ((x - inset) / (width - inset * 2f) * 100f).coerceIn(0f, 100f)
    }
    val gestureModifier = if (enabled) {
        Modifier
            .pointerInput(Unit) { detectTapGestures { onValueChange(valueAt(it.x, size.width.toFloat())); onValueChangeFinished() } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onValueChange(valueAt(it.x, size.width.toFloat())) },
                    onDrag = { change, _ -> onValueChange(valueAt(change.position.x, size.width.toFloat())) },
                    onDragEnd = onValueChangeFinished,
                )
            }
    } else Modifier

    Box(modifier.fillMaxWidth().height(36.dp).then(gestureModifier)) {
        Canvas(Modifier.matchParentSize()) {
            val inset = 12.dp.toPx()
            val trackHeight = 8.dp.toPx()
            val trackWidth = size.width - inset * 2f
            val top = size.height / 2f - trackHeight / 2f
            val background = if (enabled) Color(0xFFE8E1D7) else Color(0xFFEEE9E2)
            val active = if (enabled) accentColor else accentColor.copy(alpha = .35f)
            drawRoundRect(background, Offset(inset, top), Size(trackWidth, trackHeight), CornerRadius(trackHeight / 2f))
            drawRoundRect(active, Offset(inset, top), Size(trackWidth * (value / 100f), trackHeight), CornerRadius(trackHeight / 2f))
            val thumbX = inset + trackWidth * (value / 100f)
            drawCircle(Color(0x22000000), 10.dp.toPx(), Offset(thumbX + 1.dp.toPx(), size.height / 2f + 2.dp.toPx()))
            drawCircle(Color.White, 9.dp.toPx(), Offset(thumbX, size.height / 2f))
            drawCircle(Color(0xFFE0D8CF), 9.dp.toPx(), Offset(thumbX, size.height / 2f), style = Stroke(1.dp.toPx()))
        }
    }
}
