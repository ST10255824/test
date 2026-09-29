package com.mackson.delivery.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Generously rounded corners and pill-shaped buttons match the soft, friendly card/button style
// throughout the Part 1 Figma prototype (e.g. "Start shopping", "Confirm location").
val MacksonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
