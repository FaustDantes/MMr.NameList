package com.michalmarcin.namelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michalmarcin.namelist.ui.theme.IconSquare

/** Folder that holds person photos. Set once in MainActivity; tests set their own. */
val LocalPhotoDirectory = staticCompositionLocalOf<File?> { null }

/**
 * Square avatar: the photo when there is one, else initials on a color from the name.
 * The initials stay under the photo, so a missing or broken file still shows a good avatar.
 */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 48.dp, photoFile: String? = null) {
    val (background, content) = AvatarColors.forName(name, MaterialTheme.colorScheme.isDark())
    Box(
        modifier = modifier
            .size(size)
            .clip(IconSquare)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(name),
            color = content,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.38f).sp,
        )
        val directory = LocalPhotoDirectory.current
        if (photoFile != null && directory != null) {
            AsyncImage(
                model = File(directory, photoFile),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** "Anna Nováková" -> "AN", "eva" -> "E", "" -> "?". */
fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    return words.take(2).joinToString("") { it.substring(0, it.offsetByCodePoints(0, 1)).uppercase() }
}

private fun androidx.compose.material3.ColorScheme.isDark(): Boolean = background.luminance() < 0.5f

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

object AvatarColors {
    // (background, text). Each pair has a contrast ratio of 4.5:1 or more.
    private val light = listOf(
        Color(0xFFB2DFDB) to Color(0xFF00332E),
        Color(0xFFF3DDBF) to Color(0xFF3D2A12),
        Color(0xFFC5DAF5) to Color(0xFF0D2A4D),
        Color(0xFFF6D0D6) to Color(0xFF4A1520),
        Color(0xFFCDE6C2) to Color(0xFF173A0D),
        Color(0xFFDCD3F2) to Color(0xFF2A1A4F),
        Color(0xFFF7E1A6) to Color(0xFF3F2E00),
        Color(0xFFD3DDE2) to Color(0xFF1E2C33),
    )
    private val dark = listOf(
        Color(0xFF0F4D47) to Color(0xFFB2DFDB),
        Color(0xFF5A4425) to Color(0xFFF3DDBF),
        Color(0xFF1F3F66) to Color(0xFFC5DAF5),
        Color(0xFF6A2A36) to Color(0xFFF6D0D6),
        Color(0xFF2C5220) to Color(0xFFCDE6C2),
        Color(0xFF3F2F6E) to Color(0xFFDCD3F2),
        Color(0xFF5C4600) to Color(0xFFF7E1A6),
        Color(0xFF34444C) to Color(0xFFD3DDE2),
    )

    fun index(name: String): Int = Math.floorMod(name.trim().lowercase().hashCode(), light.size)

    fun forName(name: String, dark: Boolean): Pair<Color, Color> =
        (if (dark) this.dark else light)[index(name)]
}
