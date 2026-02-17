package de.kaiserdragon.iconrequest.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class SquircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val radius = width * 0.5f
            moveTo(0f, radius)
            cubicTo(0f, 0f, 0f, 0f, radius, 0f)
            cubicTo(width, 0f, width, 0f, width, radius)
            cubicTo(width, height, width, height, radius, height)
            cubicTo(0f, height, 0f, height, 0f, radius)
            close()
        }
        return Outline.Generic(path)
    }
}
class PolygonShape(private val sides: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val radius = size.width / 2f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val angle = 2.0 * Math.PI / sides
            moveTo(
                centerX + radius * Math.cos(-Math.PI / 2).toFloat(),
                centerY + radius * Math.sin(-Math.PI / 2).toFloat()
            )
            for (i in 1 until sides) {
                lineTo(
                    centerX + radius * Math.cos(i * angle - Math.PI / 2).toFloat(),
                    centerY + radius * Math.sin(i * angle - Math.PI / 2).toFloat()
                )
            }
            close()
        }
        return Outline.Generic(path)
    }
}

class LeafShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(Path().apply {
            val width = size.width
            val height = size.height
            moveTo(0f, 0f)
            lineTo(width * 0.5f, 0f)
            quadraticTo(width, 0f, width, height * 0.5f)
            lineTo(width, height) // Bottom-right is sharp
            lineTo(width * 0.5f, height)
            quadraticTo(0f, height, 0f, height * 0.5f)
            close()
        })
    }
}

class TeardropShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(Path().apply {
            val width = size.width
            val height = size.height
            val radius = width * 0.5f
            moveTo(width / 2f, 0f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(width - (radius * 2), 0f, width, radius * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(width, height * 0.9f)
            quadraticTo(width, height, width * 0.9f, height)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, height - (radius * 2), radius * 2, height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, 0f, radius * 2, radius * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        })
    }
}