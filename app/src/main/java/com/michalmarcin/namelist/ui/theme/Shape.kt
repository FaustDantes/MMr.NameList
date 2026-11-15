package com.michalmarcin.namelist.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Square design (ADR-010, ADR-018): very small corner radii everywhere, no circles or pills.
 * Avatars and icon badges use [IconSquare], a square with corners at 12 % of the size.
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

val IconSquare = RoundedCornerShape(percent = 12)
