package com.michalmarcin.namelist.ui.learn

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.data.db.LearnRound
import com.michalmarcin.namelist.data.repo.LearnCard
import com.michalmarcin.namelist.ui.AppViewModelFactory
import com.michalmarcin.namelist.ui.components.EmptyState
import com.michalmarcin.namelist.ui.components.topHairline
import com.michalmarcin.namelist.ui.person.formatEpochDay
import com.michalmarcin.namelist.ui.person.millisToEpochDay
import kotlin.math.abs

@Composable
fun LearnRoute(
    onBack: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    viewModel: LearnViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LearnScreen(
        state = state,
        onBack = onBack,
        onFlip = viewModel::flip,
        onAnswer = viewModel::answer,
        onNewRound = viewModel::newRound,
        onOpenPerson = onOpenPerson,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    state: LearnUiState,
    onBack: () -> Unit,
    onFlip: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onNewRound: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.learn)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("learnBack")) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val content = Modifier
            .padding(padding)
            .fillMaxSize()
        when (state) {
            LearnUiState.Loading -> Box(content, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            LearnUiState.NoPeople -> EmptyState(
                icon = R.drawable.ic_book,
                title = stringResource(R.string.learn_empty_title),
                body = stringResource(R.string.learn_empty_body),
                modifier = content.testTag("learnEmpty"),
            )
            is LearnUiState.Card -> CardPage(state, onFlip, onAnswer, content)
            is LearnUiState.Result -> ResultPage(state, onNewRound, onBack, onOpenPerson, content)
        }
    }
}

@Composable
private fun CardPage(state: LearnUiState.Card, onFlip: () -> Unit, onAnswer: (Boolean) -> Unit, modifier: Modifier) {
    Column(modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.learn_progress, state.number, state.total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("learnProgress"),
            )
            LinearProgressIndicator(
                progress = { (state.number - 1).toFloat() / state.total },
                modifier = Modifier.fillMaxWidth(),
                drawStopIndicator = {},
            )
        }
        FlipCard(state.card, state.flipped, onFlip, Modifier.weight(1f).fillMaxWidth())
        Text(
            stringResource(if (state.flipped) R.string.learn_tap_to_turn else R.string.learn_tap_to_flip),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedButton(
                onClick = { onAnswer(false) },
                enabled = state.flipped,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("learnNotKnew"),
            ) { Text(stringResource(R.string.learn_not_knew)) }
            Button(
                onClick = { onAnswer(true) },
                enabled = state.flipped,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("learnKnew"),
            ) { Text(stringResource(R.string.learn_knew)) }
        }
    }
}

/** The card turns around its vertical axis. Past the half turn the back is drawn, mirrored back to normal. */
@Composable
private fun FlipCard(card: LearnCard, flipped: Boolean, onFlip: () -> Unit, modifier: Modifier) {
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(durationMillis = 350), label = "flip")
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(
                onClickLabel = stringResource(if (flipped) R.string.learn_tap_to_turn else R.string.learn_tap_to_flip),
                onClick = onFlip,
            )
            .testTag("learnCard"),
    ) {
        if (rotation <= 90f) {
            CardFront(card, Modifier.fillMaxSize().testTag("learnFront"))
        } else {
            CardBack(card, Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.testTag("learnCardBack"))
        }
    }
}

/** F35 order: tags at the top, then the conversation spark, then the place; notes always at the bottom. */
@Composable
private fun CardFront(card: LearnCard, modifier: Modifier) {
    Column(modifier.padding(24.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (card.tags.isNotEmpty()) {
                Text(
                    card.tags.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("learnTags"),
                )
            }
            Text(stringResource(R.string.learn_who), style = MaterialTheme.typography.headlineMedium)
            card.spark?.let { CardFact(R.string.ask_about, it, "learnSpark", MaterialTheme.typography.titleLarge) }
            card.place?.let { CardFact(R.string.where_met, it, "learnPlace", MaterialTheme.typography.bodyLarge) }
        }
        card.notes?.let { notes ->
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().topHairline(MaterialTheme.colorScheme.outlineVariant).padding(top = 12.dp)) {
                Label(R.string.notes)
                Text(
                    notes,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("learnNotes"),
                )
            }
        }
    }
}

@Composable
private fun CardFact(label: Int, value: String, tag: String, style: androidx.compose.ui.text.TextStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Label(label)
        Text(value, style = style, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun Label(label: Int) {
    Text(
        stringResource(label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CardBack(card: LearnCard, modifier: Modifier) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            card.name,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("learnName"),
        )
    }
}

@Composable
private fun ResultPage(
    state: LearnUiState.Result,
    onNewRound: () -> Unit,
    onDone: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("learnResult"),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.learn_result_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.learn_score, state.known, state.total),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.testTag("learnScore"),
            )
            val change = state.change
            Text(
                when {
                    change == null -> stringResource(R.string.learn_first_round)
                    change > 0 -> stringResource(R.string.learn_change_up, change)
                    change < 0 -> stringResource(R.string.learn_change_down, abs(change))
                    else -> stringResource(R.string.learn_change_same)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if ((change ?: 0) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("learnChange"),
            )
            Text(
                stringResource(R.string.learn_best, state.best),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.learn_chart_title), style = MaterialTheme.typography.titleMedium)
            ProgressChart(state.history.takeLast(MaxChartRounds))
        }

        Column {
            Text(
                stringResource(R.string.learn_missed_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (state.missed.isEmpty()) {
                Text(stringResource(R.string.learn_all_known), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("learnAllKnown"))
            }
            state.missed.forEach { card ->
                Text(
                    card.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .topHairline(MaterialTheme.colorScheme.outlineVariant)
                        .clickable { onOpenPerson(card.personId) }
                        .padding(vertical = 14.dp)
                        .testTag("learnMissed-${card.personId}"),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedButton(
                onClick = onDone,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("learnDone"),
            ) { Text(stringResource(R.string.learn_done)) }
            Button(
                onClick = onNewRound,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("learnAgain"),
            ) { Text(stringResource(R.string.learn_again)) }
        }
    }
}

/**
 * Score in percent (left axis, 0 to 100) against the time of each round (bottom axis).
 * The points sit at their real time, so a pause of a week shows as a wider gap.
 */
@Composable
internal fun ProgressChart(rounds: List<LearnRound>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val dotCenter = MaterialTheme.colorScheme.background
    val description = stringResource(R.string.learn_chart_description, rounds.size, rounds.lastOrNull()?.percent ?: 0)
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }.testTag("learnChart")) {
        Row(Modifier.fillMaxWidth().height(ChartHeight)) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                listOf("100%", "50%", "0%").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            androidx.compose.foundation.Canvas(Modifier.weight(1f).fillMaxHeight().padding(vertical = 6.dp)) {
                val stroke = 1.dp.toPx()
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = size.height * f
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
                }
                if (rounds.isEmpty()) return@Canvas
                val first = rounds.first().finishedAt
                val span = (rounds.last().finishedAt - first).toFloat()
                val inset = 6.dp.toPx()
                val width = size.width - 2 * inset
                val points = rounds.mapIndexed { index, round ->
                    val fx = when {
                        rounds.size == 1 -> 0.5f
                        span > 0f -> (round.finishedAt - first) / span
                        else -> index.toFloat() / (rounds.size - 1)
                    }
                    Offset(inset + width * fx, size.height * (1f - round.percent / 100f))
                }
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                points.forEachIndexed { index, p ->
                    val last = index == points.lastIndex
                    drawCircle(line, radius = (if (last) 6 else 4).dp.toPx(), center = p)
                    if (!last) drawCircle(dotCenter, radius = 2.dp.toPx(), center = p)
                }
            }
        }
        if (rounds.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(start = 40.dp, top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val style = MaterialTheme.typography.labelSmall
                val color = MaterialTheme.colorScheme.onSurfaceVariant
                Text(formatEpochDay(millisToEpochDay(rounds.first().finishedAt)), style = style, color = color)
                if (rounds.size > 1) Text(formatEpochDay(millisToEpochDay(rounds.last().finishedAt)), style = style, color = color)
            }
        }
    }
}

private val ChartHeight = 160.dp
private const val MaxChartRounds = 30
