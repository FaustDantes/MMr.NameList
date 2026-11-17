package com.michalmarcin.namelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.michalmarcin.namelist.ui.theme.IconSquare
import java.io.File

/** Folder that holds person photos. Set once in MainActivity; tests set their own. */
val LocalPhotoDirectory = staticCompositionLocalOf<File?> { null }

/**
 * The person's photo in a square (ADR-038). A person without a photo shows nothing: no initials,
 * no placeholder. Callers check [hasPhoto] so the row lines up without an empty gap.
 */
@Composable
fun Avatar(photoFile: String?, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    if (photoFile == null) return
    val directory = LocalPhotoDirectory.current
    Box(
        modifier = modifier
            .size(size)
            .clip(IconSquare)
            // Shows while the file loads, or if the file is missing.
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("photo"),
    ) {
        if (directory != null) {
            AsyncImage(
                model = File(directory, photoFile),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** True when a photo is set; then the row shows [Avatar] and a gap after it. */
fun hasPhoto(photoFile: String?): Boolean = photoFile != null
