package com.michalmarcin.namelist.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Square design (ADR-010, ADR-018): very small corner radii everywhere, no circles or pills.
 * Photos and icon badges use [IconSquare], a square with corners at 8 % of the size. Line design (ADR-038):
 * buttons 8 dp, sheets 12 dp.
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

val IconSquare = RoundedCornerShape(percent = 8)
