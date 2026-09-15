package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.HairlineProgress
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.pageMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import kotlinx.coroutines.launch

/**
 * What the deck area means right now: cards in hand, everything learned,
 * or the deck done. The change-key for its house-push switch: only a move
 * between these meanings animates, never a loading state.
 */
private enum class DeckState { Cards, AllLearned, Done }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    viewModel: NamesViewModel,
    onBack: () -> Unit,
) {
    val session: FlashcardsViewModel = viewModel()
    val names by viewModel.names.collectAsStateWithLifecycle()
    val namesLoaded by viewModel.namesLoaded.collectAsStateWithLifecycle()
    val learned by viewModel.learned.collectAsStateWithLifecycle()
    val learnedLoaded by viewModel.learnedLoaded.collectAsStateWithLifecycle()
    val includeLearned by viewModel.includeLearned.collectAsStateWithLifecycle()

    // Read once here, above the deck-state switch: AnimatedContent's
    // transitionSpec is not a composable context, so the motion scale has to
    // be captured outside it (same hoist as the quiz screen).
    val motionScale = LocalMotionScale.current

    LaunchedEffect(names, learned, learnedLoaded, includeLearned, namesLoaded) {
        if (!learnedLoaded) return@LaunchedEffect
        session.ensureDeck(namesLoaded, names, learned, includeLearned)
    }

    Scaffold(
        topBar = {
            // A sequence register, centred over the work like the quiz's and
            // the name page's, pushed-screen TITLES sit left; position
            // counters sit centre.
            CenterAlignedTopAppBar(
                modifier = Modifier.barMeasure(),
                colors = paperTopBarColors(),
                title = {
                    ScreenLabel(
                        if (session.deck.isNotEmpty() && !session.done) {
                            stringResource(
                                R.string.card_x_of_y,
                                session.index + 1,
                                session.deck.size,
                            )
                        } else {
                            stringResource(R.string.flashcards)
                        }
                    )
                },
                navigationIcon = {
                    BackButton(onBack)
                },
                actions = {
                    // A named menu, not two mute icons: the deck options say what
                    // they do, and "include learned" can show that it is on.
                    DeckMenu(
                        includeLearned = includeLearned,
                        onToggleIncludeLearned = {
                            viewModel.setIncludeLearned(!includeLearned)
                        },
                        onReshuffle = { session.restart(names, learned, includeLearned) },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Wide screens keep the book's column: the card, the verdict
                // row and the buttons hold page proportions instead of
                // stretching edge to edge. Phones never reach the cap.
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = pageMeasure())
                .padding(padding)
                .padding(horizontal = PageInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Loading guards stay outside the animation: no entrance of its
            // own for a first frame, motion only where meaning changes. The
            // deck → all-learned → done turns themselves ride the house push
            // like every other change of state (the quiz's question → result,
            // the daily card, pushed screens); this was the when-block that
            // still hard-cut.
            when {
                // Blank paper said nothing at all when the asset failed to
                // read. Home has explained this case since v2.6; these screens
                // are reachable without passing it.
                names.isEmpty() && namesLoaded ->
                    PageMessage(stringResource(R.string.names_unavailable))
                // Until the deck for this sitting has been built (a frame
                // after this composition) an empty deck is "not built yet",
                // not "everything is learned": the all-learned page is
                // alarming, and wrong for a brand-new reader.
                !session.ready -> Unit
                else -> AnimatedContent(
                    targetState = when {
                        session.deck.isEmpty() -> DeckState.AllLearned
                        session.done -> DeckState.Done
                        else -> DeckState.Cards
                    },
                    transitionSpec = {
                        (fadeIn(Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle)) +
                            slideInVertically(
                                Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle),
                            ) { it / 12 })
                            .togetherWith(fadeOut(Motion.spec(motionScale, Motion.QUICK)))
                    },
                    label = "deckState",
                ) { state ->
                    when (state) {
                        DeckState.AllLearned -> AllLearnedContent(
                            onReviewLearned = { viewModel.setIncludeLearned(true) },
                            onBack = onBack,
                        )
                        DeckState.Done -> DeckDoneContent(
                            onStartAgain = { session.restart(names, learned, includeLearned) },
                        )
                        DeckState.Cards -> Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // The deck can empty while an outgoing copy of this
                            // branch is still fading (a reshuffle into an
                            // all-learned rebuild): read the card back safely so
                            // the exit never crashes on a stale index.
                            val cardNumber = session.deck.getOrNull(session.index)
                                ?: return@AnimatedContent
                            val name = names.firstOrNull { it.number == cardNumber }
                                ?: return@AnimatedContent
                            val scope = rememberCoroutineScope()
                            val haptics = rememberHaptics()

                            // Horizontal offset of the current card; a fresh Animatable
                            // per card so each one starts centered.
                            val offsetX = remember(session.deck, session.index) { Animatable(0f) }
                            var cardWidth by remember { mutableFloatStateOf(0f) }

                            fun commit(know: Boolean) {
                                if (offsetX.isRunning && offsetX.targetValue != 0f) return
                                scope.launch {
                                    haptics.confirm()
                                    val target = (if (know) 1.3f else -1.3f) * cardWidth
                                    // Motion.spec, not tween: with "Remove animations"
                                    // the card must not still fly off-screen.
                                    offsetX.animateTo(target, Motion.spec(motionScale, 240))
                                    // A review pass only ever adds. "Still learning" must
                                    // not quietly delete a tick the reader already earned.
                                    val marked = know && name.number !in learned
                                    if (marked) viewModel.setLearned(name.number, true)
                                    session.recordCommit(name.number, marked)
                                    session.advance()
                                }
                            }

                            HairlineProgress(
                                progress = (session.index + 1) / session.deck.size.toFloat(),
                            )
                            // The card keeps card proportions instead of stretching into
                            // a full-height plane; it sits centred in whatever is left.
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                SwipeFlipCard(
                                    name = name,
                                    flipped = session.flipped,
                                    onFlip = {
                                        haptics.tick()
                                        session.flip()
                                    },
                                    offsetX = offsetX,
                                    cardWidth = cardWidth,
                                    onDragCommit = ::commit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // The plate takes a content-proportioned
                                        // height, centred in what is left (owner
                                        // decision): 340dp holds the Name with
                                        // presence, where the old full-deck
                                        // stretch (460dp) set two lines of ink in
                                        // an ocean of emerald, vacancy, not
                                        // calm. One height for BOTH faces: the
                                        // back's long meanings scroll inside it
                                        // (the thumb marks when), so the plate
                                        // never resizes mid-flip. heightIn BEFORE
                                        // fillMaxHeight: on short viewports the
                                        // card fills whatever remains below the
                                        // cap, never past it.
                                        .heightIn(max = 340.dp)
                                        .fillMaxHeight()
                                        .onSizeChanged { cardWidth = it.width.toFloat() },
                                )
                            }
                            // The way back from a mis-swipe. The row keeps a fixed
                            // height whether the undo is present or not: the empty
                            // placeholder is a short Text while the undo control is a
                            // TextButton with a 48dp minimum touch target, so without
                            // the fixed-height box the card would shrink ~30dp the
                            // moment the undo appeared. (The swipe instructions that
                            // once sat here are gone: the drag teaches itself, the
                            // card wears the I KNOW IT / STILL LEARNING overline toward
                            // the commit threshold, and the two buttons below name the
                            // same verdicts.)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                val undoable = session.undoable
                                if (undoable != null) {
                                    TextButton(
                                        onClick = {
                                            session.undo()?.let { (number, wasMarked) ->
                                                if (wasMarked) viewModel.setLearned(number, false)
                                            }
                                        },
                                    ) {
                                        Text(
                                            text = stringResource(R.string.undo_card),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                } else {
                                    Text("", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { commit(false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp),
                                ) {
                                    Text(stringResource(R.string.still_learning))
                                }
                                Button(
                                    onClick = { commit(true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp),
                                ) {
                                    Text(stringResource(R.string.i_know_it))
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
