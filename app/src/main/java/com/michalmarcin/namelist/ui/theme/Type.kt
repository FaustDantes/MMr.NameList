package com.michalmarcin.namelist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.michalmarcin.namelist.R

/** Titles and names (ADR-038). Bundled, so no network is needed. SIL Open Font License, see licenses/. */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
)

/** Body text, labels and buttons (ADR-038). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val base = Typography()

private fun TextStyle.grotesk(size: Int, weight: FontWeight = FontWeight.Medium, spacing: Double = -0.02) =
    copy(fontFamily = SpaceGrotesk, fontWeight = weight, fontSize = size.sp, letterSpacing = spacing.em)

private fun TextStyle.inter() = copy(fontFamily = Inter)

internal val AppTypography = Typography(
    displayLarge = base.displayLarge.grotesk(52, spacing = -0.04),
    displayMedium = base.displayMedium.grotesk(44, spacing = -0.04),
    displaySmall = base.displaySmall.grotesk(36, spacing = -0.03),
    // The big "People" title (LargeTopAppBar uses headlineMedium).
    headlineLarge = base.headlineLarge.grotesk(40, spacing = -0.04),
    headlineMedium = base.headlineMedium.grotesk(34, spacing = -0.035),
    headlineSmall = base.headlineSmall.grotesk(26, spacing = -0.02),
    // Screen titles and dialog titles.
    titleLarge = base.titleLarge.grotesk(24, spacing = -0.02),
    // Person names in lists.
    titleMedium = base.titleMedium.grotesk(18, spacing = -0.01),
    titleSmall = base.titleSmall.grotesk(16, spacing = -0.01),
    bodyLarge = base.bodyLarge.inter(),
    bodyMedium = base.bodyMedium.inter(),
    bodySmall = base.bodySmall.inter(),
    labelLarge = base.labelLarge.inter(),
    labelMedium = base.labelMedium.inter(),
    labelSmall = base.labelSmall.inter(),
)
