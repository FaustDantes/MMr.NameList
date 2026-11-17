package com.michalmarcin.namelist.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.ui.AppViewModelFactory
import com.michalmarcin.namelist.ui.components.Avatar
import com.michalmarcin.namelist.ui.components.hasPhoto
import com.michalmarcin.namelist.ui.components.topHairline
import com.michalmarcin.namelist.ui.components.EmptyState
import com.michalmarcin.namelist.ui.person.formatEpochDay
import com.michalmarcin.namelist.ui.person.millisToEpochDay

@Composable
fun ArchiveRoute(
    onBack: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    viewModel: ArchiveViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ArchiveScreen(
        state = state,
        onBack = onBack,
        onOpenPerson = onOpenPerson,
        onRestore = viewModel::restore,
        onRestoreAll = viewModel::restoreAll,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    state: ArchiveUiState,
    onBack: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    onRestore: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onRestoreAll: () -> Unit = {},
    now: Long = remember { System.currentTimeMillis() },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archive)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.people.isNotEmpty()) {
                        IconButton(onClick = onRestoreAll, modifier = Modifier.testTag("restoreAll")) {
                            Icon(painterResource(R.drawable.ic_unarchive), stringResource(R.string.restore))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!state.isLoading && state.people.isEmpty()) {
            EmptyState(
                icon = R.drawable.ic_archive,
                title = stringResource(R.string.archive_empty_title),
                body = stringResource(R.string.archive_empty_body),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .testTag("archiveEmpty"),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .testTag("archiveList"),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            ) {
                items(state.people, key = { it.id }) { person ->
                    ArchiveRow(person, now, onOpenPerson, onRestore)
                }
            }
        }
    }
}

@Composable
private fun ArchiveRow(person: Person, now: Long, onOpen: (Long) -> Unit, onRestore: (Long) -> Unit) {
    val archivedAt = person.archivedAt ?: now
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .topHairline(MaterialTheme.colorScheme.outlineVariant)
            .clickable { onOpen(person.id) }
            .testTag("archived-${person.id}"),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(end = 0.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasPhoto(person.photoFile)) {
                Avatar(photoFile = person.photoFile)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.archive_status,
                        formatEpochDay(millisToEpochDay(archivedAt)),
                        ArchiveAge.of(archivedAt, now).text(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("archiveStatus-${person.id}"),
                )
            }
            IconButton(onClick = { onRestore(person.id) }, modifier = Modifier.testTag("restore-${person.id}")) {
                Icon(painterResource(R.drawable.ic_unarchive), stringResource(R.string.restore))
            }
        }
    }
}
