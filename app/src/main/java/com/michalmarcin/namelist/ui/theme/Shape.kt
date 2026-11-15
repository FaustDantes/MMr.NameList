package com.michalmarcin.namelist.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Square design (ADR-010): small corner radii everywhere, no circles.
 * Avatars and icon badges use [IconSquare], a square with corners at 22 % of the size.
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

val IconSquare = RoundedCornerShape(percent = 22)
