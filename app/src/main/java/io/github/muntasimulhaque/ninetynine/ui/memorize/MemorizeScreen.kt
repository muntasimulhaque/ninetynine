package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.HairlineProgress
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ListInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LocalBottomBarOverlay
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NavRow
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageRule
import io.github.muntasimulhaque.ninetynine.ui.theme.components.TabTitle
import io.github.muntasimulhaque.ninetynine.ui.theme.components.pageMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizeScreen(
    viewModel: NamesViewModel,
    onFlashcards: () -> Unit,
    onQuiz: () -> Unit,
    onLearned: () -> Unit,
) {
    val learned by viewModel.learned.collectAsStateWithLifecycle()
    val learnedLoaded by viewModel.learnedLoaded.collectAsStateWithLifecycle()
    val names by viewModel.names.collectAsStateWithLifecycle()
    val namesLoaded by viewModel.namesLoaded.collectAsStateWithLifecycle()
    val quizBest by viewModel.quizBest.collectAsStateWithLifecycle()
    val learnedCount = learned.size.coerceIn(0, 99)

    // The count rolls to meet the reader instead of teleporting: leave at 41,
    // learn five on the name pages, come back and watch 41 become 46. Where
    // the roll starts rides in saved instance state, so even a process death
    // mid-visit lands on a number that then moves. A first-ever composition
    // starts exactly on target — no theatrical roll up from zero — and with
    // animations off it simply is the number.
    var lastSeen by rememberSaveable { mutableIntStateOf(-1) }
    val rolled = remember {
        Animatable(if (lastSeen in 0..99) lastSeen.toFloat() else learnedCount.toFloat())
    }
    val motionScale = LocalMotionScale.current
    LaunchedEffect(learnedCount) {
        if (rolled.value != learnedCount.toFloat()) {
            if (motionScale == 0f) rolled.snapTo(learnedCount.toFloat())
            else rolled.animateTo(
                learnedCount.toFloat(),
                Motion.spec(motionScale, Motion.CALM, easing = Motion.Settle),
            )
        }
        lastSeen = learnedCount
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barMeasure(),
                colors = paperTopBarColors(),
                title = {
                    TabTitle(stringResource(R.string.memorize))
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Wide screens keep the book's column: the count, the
                // contents and the rows hold page proportions instead of
                // stretching edge to edge. Phones never reach the cap.
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = pageMeasure())
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ListInset),
        ) {
            Spacer(Modifier.height(20.dp))
            if (names.isEmpty() && namesLoaded) {
                // The asset failed to read; the count below would otherwise
                // claim a progress ("3 of 99 learned") every names surface
                // has just said is meaningless.
                PageMessage(stringResource(R.string.names_unavailable))
            } else if (learnedLoaded) {
                // The count is the screen's centrepiece, so it must not flash
                // "0 of 99 learned" for the frame or two before DataStore
                // delivers the real set — the same *Loaded gate every other
                // screen applies. Blank paper until then, exactly like the
                // flashcards and quiz waiting for their own flags.

                // Progress as typography: a big light number, a quiet caption,
                // and a hairline of gold — no rings, no dashboards. The number is
                // also the way in: it stood for a list the app never let anyone
                // see, while the newer bookmarks axis had a whole tab.
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        // onClickLabel, not just Role.Button: the big number
                        // and caption say what they count, not what tapping
                        // them opens.
                        .clickable(
                            onClickLabel = stringResource(R.string.cd_see_learned),
                            onClick = onLearned,
                        )
                        .semantics { role = Role.Button },
                ) {
                    Text(
                        text = rolled.value.roundToInt().coerceIn(0, 99).toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.progress_of_caption),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 9.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 6.dp).size(20.dp),
                    )
                }
                Spacer(Modifier.height(scaledGap(14.dp)))
                // announceProgress = false: the big count directly above
                // already says the number; a second "progress bar" stop for
                // the same datum is noise to a screen reader.
                HairlineProgress(progress = learnedCount / 99f, announceProgress = false)
                // Text-adjacent air scales with the type, like the gap above it.
                Spacer(Modifier.height(scaledGap(10.dp)))
                Text(
                    text = if (learnedCount >= 99) {
                        stringResource(R.string.all_learned_title)
                    } else {
                        val remaining = 99 - learnedCount
                        pluralStringResource(R.plurals.remaining_count, remaining, remaining)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(scaledGap(36.dp)))
            // A small table of contents, set like a book's.
            NavRow(
                title = stringResource(R.string.flashcards),
                subtitle = stringResource(R.string.flashcards_subtitle),
                onClick = onFlashcards,
            )
            PageRule()
            NavRow(
                title = stringResource(R.string.quiz),
                subtitle = stringResource(R.string.quiz_subtitle),
                onClick = onQuiz,
            )
            PageRule()
            if (quizBest >= 0) {
                Spacer(Modifier.height(scaledGap(24.dp)))
                Text(
                    text = stringResource(R.string.quiz_best, quizBest),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Floating bar: the NavRows must clear the plate — the bar's
            // measured height plus a little air.
            Spacer(Modifier.height(24.dp + LocalBottomBarOverlay.current))
        }
    }
}

