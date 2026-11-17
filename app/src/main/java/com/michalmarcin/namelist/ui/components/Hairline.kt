package com.michalmarcin.namelist.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/** A 1 px line along the top edge. The Line design separates rows with these instead of cards (ADR-038). */
fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f)
}
