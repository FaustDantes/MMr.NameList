package com.michalmarcin.namelist.ui.intro

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.ui.components.topHairline

private data class IntroPoint(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val text: Int)

private val points = listOf(
    IntroPoint(R.drawable.ic_person_add, R.string.intro_1_title, R.string.intro_1_text),
    IntroPoint(R.drawable.ic_chat, R.string.intro_2_title, R.string.intro_2_text),
    IntroPoint(R.drawable.ic_search, R.string.intro_3_title, R.string.intro_3_text),
    IntroPoint(R.drawable.ic_lock, R.string.intro_4_title, R.string.intro_4_text),
)

/**
 * F34: the welcome shown once. It closes only with its button, not by tapping outside or Back,
 * so the user confirms it; then it never shows again (ADR-041).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroDialog(onConfirm: () -> Unit) {
    BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            IntroContent(onConfirm)
        }
    }
}

/** The dialog's content, separate from the window so tests can drive it. */
@Composable
fun IntroContent(onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("intro"),
    ) {
        Text(stringResource(R.string.intro_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.intro_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        points.forEach { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .topHairline(MaterialTheme.colorScheme.outlineVariant)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    painterResource(point.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(point.title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(point.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onConfirm,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("introConfirm"),
        ) {
            Text(stringResource(R.string.intro_confirm))
        }
    }
}
