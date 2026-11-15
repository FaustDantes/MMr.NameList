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
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.RelationView
import com.michalmarcin.namelist.ui.components.Avatar

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
        Avatar(name = relation.other.name, size = 40.dp, photoFile = relation.other.photoFile)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                relation.other.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(relation.type.labelRes()),
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
fun RelationTypeChips(selected: RelationType?, onSelect: (RelationType) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RelationType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(stringResource(type.labelRes())) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.testTag("type-${type.name}"),
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
    onAdd: (otherId: Long, type: RelationType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var type by rememberSaveable { mutableStateOf(RelationType.FRIEND) }
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.add_relationship), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.relationship_hint, personName, stringResource(type.labelRes()).lowercase()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RelationTypeChips(selected = type, onSelect = { type = it })
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
            items(candidates, key = { it.id }) { person ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable { onAdd(person.id, type) }
                        .testTag("candidate-${person.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(name = person.name, size = 40.dp, photoFile = person.photoFile)
                    Spacer(Modifier.width(12.dp))
                    Text(person.name, style = MaterialTheme.typography.bodyLarge)
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
    current: RelationType,
    onSelect: (RelationType) -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.change_type_title, name), style = MaterialTheme.typography.titleLarge)
        RelationTypeChips(selected = current, onSelect = onSelect)
        TextButton(shape = MaterialTheme.shapes.large, onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.cancel))
        }
    }
}
