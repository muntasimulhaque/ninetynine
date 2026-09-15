package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.MarkSeal
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NavRow
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageRule
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SectionLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SettleOnce
import kotlin.math.roundToInt

@Composable
internal fun QuizResultContent(
    score: Int,
    total: Int,
    best: Int,
    isNewBest: Boolean,
    missed: List<Name>,
    onRestart: () -> Unit,
    onNameClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        // A perfect round earns the app's seal: the square-Kufic mark inside
        // the share card's gold hairline circle, popping in softly, once.
        if (score == total) {
            PerfectSeal()
            Spacer(Modifier.height(20.dp))
        }
        ScoreCount(score = score, total = total)
        // A round that beat the standing best says so, once, quietly, the
        // tracked gold overline the app reserves for what matters. First
        // rounds stay silent: everything beats nothing, and saying so would
        // cheapen the moment a real best falls.
        if (isNewBest) {
            Spacer(Modifier.height(12.dp))
            SectionLabel(stringResource(R.string.quiz_new_best))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                when {
                    score == total -> R.string.quiz_perfect
                    score >= total / 2 -> R.string.quiz_good
                    else -> R.string.quiz_keep_trying
                }
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (best >= 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.quiz_best, maxOf(best, score)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The answers page. A round that only scores you is a measuring
        // instrument; naming what you missed makes it a teaching one, and the
        // rows are the same table-of-contents row Memorize and Settings draw,
        // so nothing new arrives on screen. Empty on a perfect round, which is
        // exactly when the page should stay quiet.
        if (missed.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            SectionLabel(
                text = stringResource(R.string.names_to_revisit),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            missed.forEach { name ->
                NavRow(
                    title = name.transliteration,
                    subtitle = name.title,
                    titleStyle = MaterialTheme.typography.titleMedium,
                    onClickLabel = stringResource(R.string.cd_open_name),
                    onClick = { onNameClick(name.number) },
                )
                PageRule()
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.try_another_round))
        }
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_memorize))
        }
    }
}

/**
 * The score settles like everything else in the app: it counts up once,
 * calmly, instead of appearing already over. Plays once per result:
 * rememberSaveable keeps a rotation from replaying it, the way the name
 * page's entrance fade does not replay.
 */
@Composable
private fun ScoreCount(score: Int, total: Int) {
    var played by rememberSaveable { mutableStateOf(false) }
    val shown = remember { Animatable(0f) }
    val motionScale = LocalMotionScale.current
    LaunchedEffect(score) {
        if (played) {
            shown.snapTo(score.toFloat())
            return@LaunchedEffect
        }
        played = true
        if (motionScale == 0f) shown.snapTo(score.toFloat())
        else shown.animateTo(
            score.toFloat(),
            Motion.spec(motionScale, Motion.CALM, easing = Motion.Settle),
        )
    }
    Text(
        text = stringResource(
            R.string.quiz_score_format,
            shown.value.roundToInt().toString(),
            total.toString(),
        ),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * The share card's maker mark in its hairline gold circle: earned here,
 * not worn. The pop is the same lively spring the bookmark and the learned
 * pill answer with, so the reward speaks the app's own tactile language.
 */
@Composable
private fun PerfectSeal() {
    SettleOnce(fromScale = 0.6f) {
        MarkSeal()
    }
}
