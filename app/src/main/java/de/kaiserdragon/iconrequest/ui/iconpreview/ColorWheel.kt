package de.kaiserdragon.iconrequest.ui.iconpreview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp


@Composable
fun ColorWheel(
    selectedColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val hsv = remember(selectedColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toArgb(), hsv)
        hsv
    }

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                fun updateColor(offset: Offset) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val x = offset.x - centerX
                    val y = offset.y - centerY
                    var angle = Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble())).toFloat()
                    val hue = (angle + 360f) % 360f
                    onColorChanged(Color.hsv(hue, hsv[1], hsv[2]))
                }

                detectDragGestures { change, _ ->
                    updateColor(change.position)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val x = offset.x - centerX
                    val y = offset.y - centerY
                    val angle = Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble())).toFloat()
                    val hue = (angle + 360f) % 360f
                    onColorChanged(Color.hsv(hue, hsv[1], hsv[2]))
                }
            }
        ) {
            val radius = size.minDimension / 2f
            val strokeWidth = 32.dp.toPx()
            val innerRadius = radius - strokeWidth / 2
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    ),
                    center = center
                ),
                radius = innerRadius,
                style = Stroke(width = strokeWidth)
            )
            val angleRad = Math.toRadians(hsv[0].toDouble()).toFloat()
            val indicatorX = center.x + innerRadius * Math.cos(angleRad.toDouble()).toFloat()
            val indicatorY = center.y + innerRadius * Math.sin(angleRad.toDouble()).toFloat()
            val indicatorPos = Offset(indicatorX, indicatorY)
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = 15.dp.toPx(),
                center = indicatorPos
            )
            drawCircle(
                color = Color.White,
                radius = 13.dp.toPx(),
                center = indicatorPos
            )
            drawCircle(
                color = selectedColor,
                radius = 9.dp.toPx(),
                center = indicatorPos
            )
        }
    }
}