package de.kaiserdragon.iconrequest.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import de.kaiserdragon.iconrequest.ui.theme.LeafShape
import de.kaiserdragon.iconrequest.ui.theme.PolygonShape
import de.kaiserdragon.iconrequest.ui.theme.SquircleShape
import de.kaiserdragon.iconrequest.ui.theme.TeardropShape

enum class IconShape(val label: String) {
    Square("Square"),
    Circle("Circle"),
    Squircle("Squircle"),
    Teardrop("Teardrop"), // New entry
    RoundedSquare("Rounded"),
    Hexagon("Hexagon"),
    Octagon("Octagon"),
    Leaf("Leaf");

    val shape: Shape
        get() = when (this) {
            Square -> RectangleShape
            Circle -> CircleShape
            Squircle -> SquircleShape()
            Teardrop -> TeardropShape() // Link here
            RoundedSquare -> RoundedCornerShape(12.dp)
            Hexagon -> PolygonShape(6)
            Octagon -> PolygonShape(8)
            Leaf -> LeafShape()
        }
}