package com.michalmarcin.namelist.ui.person

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.RelationKind
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.RelationView
import com.michalmarcin.namelist.ui.components.Avatar
import com.michalmarcin.namelist.ui.components.hasPhoto

@StringRes
fun RelationType.labelRes(): Int = when (this) {
    RelationType.FRIEND -> R.string.rel_friend
    RelationType.FAMILY -> R.string.rel_family
    RelationType.PARTNER -> R.string.rel_partner
    RelationType.SIBLING -> R.string.rel_sibling
    RelationType.PARENT -> R.string.rel_parent
    RelationType.CHILD -> R.string.rel_child
    RelationType.COLLEAGUE -> R.string.rel_colleague
    RelationType.NEIGHBOUR -> R.string.rel_neighbour
    RelationType.OTHER -> R.string.rel_other
}

/** The name the user gave the type, or the built-in label in the app language (F30). */
@Composable
fun RelationKind.label(): String = name ?: stringResource((builtIn ?: RelationType.OTHER).labelRes())

/** Section on the detail screen: each line reads "[name] — is this person's [type]". */
@Composable
fun RelationshipsSection(
    relations: List<RelationView>,
    onOpenPerson: (Long) -> Unit,
    onAdd: () -> Unit,
    onChangeType: (RelationView) -> Unit,
    onRemove: (RelationView) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailSection(title = stringResource(R.string.relationships), modifier = modifier.testTag("relationsSection")) {
        if (relations.isEmpty()) {
            Text(
                stringResource(R.string.no_relationships),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        relations.forEach { relation ->
            RelationRow(relation, onOpenPerson, onChangeType, onRemove)
        }
        TextButton(
            shape = MaterialTheme.shapes.large,
            onClick = onAdd,
            modifier = Modifier.testTag("addRelationship"),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_relationship))
        }
    }
}

@Composable
private fun RelationRow(
    relation: RelationView,
    onOpenPerson: (Long) -> Unit,
    onChangeType: (RelationView) -> Unit,
    onRemove: (RelationView) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = !relation.other.isSelf) { onOpenPerson(relation.other.id) }
            .testTag("relation-${relation.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPhoto(relation.other.photoFile)) {
            Avatar(photoFile = relation.other.photoFile, size = 40.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                relation.other.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                relation.type.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.testTag("relationMenu-${relation.id}")) {
                Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.change_type)) },
                    onClick = {
                        menu = false
                        onChangeType(relation)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove)) },
                    onClick = {
                        menu = false
                        onRemove(relation)
                    },
                    modifier = Modifier.testTag("removeRelation"),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelationTypeChips(
    types: List<RelationKind>,
    selectedId: Long?,
    onSelect: (RelationKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { type ->
            FilterChip(
                selected = type.id == selectedId,
                onClick = { onSelect(type) },
                label = { Text(type.label()) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.testTag("type-${type.id}"),
            )
        }
    }
}

/**
 * Content of the "Add relationship" sheet: pick a type, then tap a person.
 * Separate from the sheet window so tests can drive it (see ManageTabsScreenTest).
 */
@Composable
fun AddRelationshipContent(
    personName: String,
    candidates: List<Person>,
    types: List<RelationKind>,
    onAdd: (otherId: Long, typeId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var typeId by rememberSaveable { mutableStateOf(RelationType.FRIEND.seedId) }
    // "Friend" may have been deleted in Settings; then the first type is picked.
    val type = types.firstOrNull { it.id == typeId } ?: types.firstOrNull()
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.add_relationship), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.relationship_hint, personName, type?.label().orEmpty().lowercase()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RelationTypeChips(types = types, selectedId = type?.id, onSelect = { typeId = it.id })
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
            items(candidates, key = { it.id }) { person ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(enabled = type != null) { type?.let { onAdd(person.id, it.id) } }
                        .testTag("candidate-${person.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasPhoto(person.photoFile)) {
                        Avatar(photoFile = person.photoFile, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(person.name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}

/** Content of the "Change type" dialog. */
@Composable
fun ChangeTypeContent(
    name: String,
    types: List<RelationKind>,
    current: RelationKind,
    onSelect: (RelationKind) -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.change_type_title, name), style = MaterialTheme.typography.titleLarge)
        RelationTypeChips(types = types, selectedId = current.id, onSelect = onSelect)
        TextButton(shape = MaterialTheme.shapes.large, onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.cancel))
        }
    }
}
