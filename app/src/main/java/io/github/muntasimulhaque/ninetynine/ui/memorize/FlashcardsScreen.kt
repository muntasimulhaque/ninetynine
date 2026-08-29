package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroSubtext
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.HairlineProgress
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SettleOnce
import io.github.muntasimulhaque.ninetynine.ui.theme.components.pageMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.readingMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScrollbarThumb
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import io.github.muntasimulhaque.ninetynine.util.DeckBuilder
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** Session state for one flashcard run; survives rotation with the ViewModel. */
class FlashcardsViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    var deck by mutableStateOf<List<Int>>(savedState.get<IntArray>(KEY_DECK)?.toList() ?: emptyList()); private set
    var index by mutableIntStateOf(savedState.get<Int>(KEY_INDEX) ?: 0); private set
    var flipped by mutableStateOf(savedState.get<Boolean>(KEY_FLIPPED) ?: false); private set
    var done by mutableStateOf(savedState.get<Boolean>(KEY_DONE) ?: false); private set
    private var lastInclude: Boolean? = savedState.get<Boolean>(KEY_LAST_INCLUDE)

    /**
     * The name the last card committed, so a mis-swipe can be taken back.
     *
     * A right-swipe writes a learned tick and moves on, and there was no way
     * back a card — the exact mirror of the guard that stops a left-swipe
     * silently *removing* one. Cleared as soon as the next card is committed,
     * so undo only ever reaches one step.
     */
    var undoable by mutableStateOf<Pair<Int, Boolean>?>(restoredUndo()); private set

    /**
     * The run lives in the SavedStateHandle, not just memory: a process death
     * mid-deck restores the exact card, flip state and undo, the same way the
     * name pager restores its page. The keys are dropped in [onCleared], so
     * leaving the screen starts the next sitting fresh.
     */
    private fun restoredUndo(): Pair<Int, Boolean>? {
        val number = savedState.get<Int>(KEY_UNDO_NUMBER) ?: return null
        return number to (savedState.get<Boolean>(KEY_UNDO_MARKED) ?: true)
    }

    private fun saveSession() {
        savedState[KEY_DECK] = deck.toIntArray()
        savedState[KEY_INDEX] = index
        savedState[KEY_FLIPPED] = flipped
        savedState[KEY_DONE] = done
        savedState[KEY_LAST_INCLUDE] = lastInclude
        val undo = undoable
        savedState[KEY_UNDO_NUMBER] = undo?.first
        savedState[KEY_UNDO_MARKED] = undo?.second
    }

    fun ensureDeck(names: List<Name>, learned: Set<Int>, includeLearned: Boolean) {
        if (names.isEmpty()) return
        if (deck.isNotEmpty() && lastInclude == includeLearned) return
        deck = DeckBuilder.build(names, learned, includeLearned)
        lastInclude = includeLearned
        index = 0
        flipped = false
        done = false
        // A rebuilt deck is a new sitting: the previous deck's last commit no
        // longer exists, so undoing it would silently un-learn a card that is
        // not even in this set.
        undoable = null
        saveSession()
    }

    fun flip() {
        flipped = !flipped
        saveSession()
    }

    fun advance() {
        flipped = false
        if (index < deck.lastIndex) index++ else done = true
        saveSession()
    }

    fun recordCommit(number: Int, markedLearned: Boolean) {
        undoable = number to markedLearned
        saveSession()
    }

    fun undo(): Pair<Int, Boolean>? {
        val last = undoable ?: return null
        undoable = null
        flipped = false
        done = false
        if (index > 0) index--
        saveSession()
        return last
    }

    fun restart(names: List<Name>, learned: Set<Int>, includeLearned: Boolean) {
        deck = emptyList()
        lastInclude = null
        ensureDeck(names, learned, includeLearned)
    }

    override fun onCleared() {
        savedState.remove<IntArray>(KEY_DECK)
        savedState.remove<Int>(KEY_INDEX)
        savedState.remove<Boolean>(KEY_FLIPPED)
        savedState.remove<Boolean>(KEY_DONE)
        savedState.remove<Boolean>(KEY_LAST_INCLUDE)
        savedState.remove<Int>(KEY_UNDO_NUMBER)
        savedState.remove<Boolean>(KEY_UNDO_MARKED)
    }

    private companion object {
        const val KEY_DECK = "deck.cards"
        const val KEY_INDEX = "deck.index"
        const val KEY_FLIPPED = "deck.flipped"
        const val KEY_DONE = "deck.done"
        const val KEY_LAST_INCLUDE = "deck.lastInclude"
        const val KEY_UNDO_NUMBER = "deck.undoNumber"
        const val KEY_UNDO_MARKED = "deck.undoMarked"
    }
}

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

    LaunchedEffect(names, learned, learnedLoaded, includeLearned) {
        if (!learnedLoaded) return@LaunchedEffect
        session.ensureDeck(names, learned, includeLearned)
    }

    Scaffold(
        topBar = {
            // A sequence register, centred over the work like the quiz's and
            // the name page's — pushed-screen TITLES sit left; position
            // counters sit centre.
            CenterAlignedTopAppBar(
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
            when {
                // Blank paper said nothing at all when the asset failed to
                // read. Home has explained this case since v2.6; these screens
                // are reachable without passing it.
                names.isEmpty() ->
                    if (namesLoaded) PageMessage(stringResource(R.string.names_unavailable))
                // Until DataStore delivers, an empty deck is "not built yet",
                // not "everything is learned": the all-learned state is
                // alarming, and wrong for a brand-new reader.
                !learnedLoaded -> Unit
                session.deck.isEmpty() -> AllLearnedContent(
                    onReviewLearned = { viewModel.setIncludeLearned(true) },
                    onBack = onBack,
                )
                session.done -> DeckDoneContent(
                    onStartAgain = { session.restart(names, learned, includeLearned) },
                )
                else -> {
                    val name = names.firstOrNull { it.number == session.deck[session.index] }
                        ?: return@Column
                    val scope = rememberCoroutineScope()
                    val haptics = rememberHaptics()

                    // Horizontal offset of the current card; a fresh Animatable
                    // per card so each one starts centered.
                    val offsetX = remember(session.deck, session.index) { Animatable(0f) }
                    var cardWidth by remember { mutableFloatStateOf(0f) }
                    val motionScale = LocalMotionScale.current

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
                                .heightIn(max = 460.dp)
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
                    // once sat here are gone: the drag teaches itself — the
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

/** Deck options, named: what each one does, and whether it is already on. */
@Composable
private fun DeckMenu(
    includeLearned: Boolean,
    onToggleIncludeLearned: () -> Unit,
    onReshuffle: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.cd_more),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.include_learned)) },
                onClick = {
                    onToggleIncludeLearned()
                    open = false
                },
                // The state belongs on the row itself: it is the focusable
                // node, so semantics on the tick box never reach a reader.
                modifier = Modifier.semantics {
                    role = Role.Checkbox
                    toggleableState =
                        if (includeLearned) ToggleableState.On else ToggleableState.Off
                },
                leadingIcon = { OptionCheck(checked = includeLearned) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reshuffle)) },
                onClick = {
                    onReshuffle()
                    open = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

/**
 * An empty box in the same ink as the menu's icons, so the option reads as
 * something you can turn on even while it is off; the tick alone is gold.
 * Purely visual — the row above carries the state for screen readers.
 */
@Composable
private fun OptionCheck(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(4.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun SwipeFlipCard(
    name: Name,
    flipped: Boolean,
    onFlip: () -> Unit,
    offsetX: Animatable<Float, *>,
    cardWidth: Float,
    onDragCommit: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    // A finger mid-drag is asking a question the card should answer: which
    // verdict is it heading toward? The sign alone flips cheaply, so it is a
    // Boolean state; the graded strength of the hint is read in the draw
    // phase below, where per-frame changes cost no recomposition.
    var dragging by remember(offsetX) { mutableStateOf(false) }
    val dragKnow by remember(offsetX) { derivedStateOf { offsetX.value >= 0f } }

    // Each card arrives with a soft rise (keyed to the card's own offset state).
    val appear = remember(offsetX) { Animatable(0f) }
    val motionScale = LocalMotionScale.current
    LaunchedEffect(offsetX) {
        appear.animateTo(1f, Motion.spec(motionScale, Motion.GENTLE, Motion.Settle))
    }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = Motion.tween(Motion.GENTLE),
        label = "flip",
    )

    // The card is one merged node — `clickable` merges its descendants — so
    // flipping it swaps the text in place and emits only a content-changed
    // event, which TalkBack does not speak. Without a live region the whole
    // memorisation loop is silent: you tap to reveal the meaning and hear
    // nothing, then press "I know it" and cannot tell what happened or which
    // name is now in front of you.
    val faceLabel = if (flipped) {
        stringResource(R.string.cd_card_back, name.transliteration, name.meaning)
    } else {
        stringResource(R.string.cd_card_front, name.transliteration)
    }
    Card(
        onClick = onFlip,
        modifier = modifier
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = faceLabel
            }
            .graphicsLayer {
                val w = size.width.coerceAtLeast(1f)
                val leaving = offsetX.value / (w * 1.2f)
                translationX = offsetX.value
                rotationZ = (offsetX.value / w) * 8f
                alpha = (appear.value * (1f - leaving * leaving)).coerceIn(0f, 1f)
                scaleX = 0.96f + 0.04f * appear.value
                scaleY = 0.96f + 0.04f * appear.value
                translationY = (1f - appear.value) * 24.dp.toPx()
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .pointerInput(offsetX) {
                var crossedThreshold = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = true
                        crossedThreshold = false
                    },
                    onDragEnd = {
                        dragging = false
                        val threshold = size.width * 0.3f
                        when {
                            offsetX.value > threshold -> onDragCommit(true)
                            offsetX.value < -threshold -> onDragCommit(false)
                            else -> scope.launch { offsetX.animateTo(0f, Motion.softSpec(motionScale)) }
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch { offsetX.animateTo(0f, Motion.softSpec(motionScale)) }
                    },
                ) { change, amount ->
                    change.consume()
                    val next = offsetX.value + amount
                    scope.launch { offsetX.snapTo(next) }
                    // One featherweight tick the instant the drag crosses the
                    // commit threshold — the finger hears the point of no
                    // return, so releasing past it stops being a guess.
                    if (!crossedThreshold && next.absoluteValue >= size.width * 0.3f) {
                        crossedThreshold = true
                        haptics.tick()
                    }
                }
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (rotation <= 90f) HeroContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (rotation <= 90f) null
        // `outline`: this border is the flipped card's entire boundary, and its
        // fill is only 1.06:1 against the page. At outlineVariant's 1.42:1 the
        // card lost its edge completely on flip — it went from a clearly
        // bounded emerald object to a shape with no perceivable outline.
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                // Front: the name itself, set like the share card. Scrollable like
                // the back: in landscape, or at a large system font, the card's
                // height can drop below what the name needs, and the Card clips to
                // its rounded shape — the one place a supported configuration
                // could otherwise lose the Name entirely.
                val frontScroll = rememberScrollState()
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(frontScroll)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ArabicText(
                            text = name.arabic,
                            fontSize = ArabicSize.Panel,
                            color = HeroGold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        FitText(
                            text = name.transliteration,
                            style = MaterialTheme.typography.displaySmall,
                            color = HeroText,
                            minScale = 0.45f,
                        )
                        // No instruction line: the whole face is one plate
                        // holding one Name, and tapping is the only thing a
                        // finger can do with it.
                    }
                    // Same right-edge thumb as the back: only present when the
                    // name overflows the card and needs scrolling.
                    ScrollbarThumb(
                        scrollState = frontScroll,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 20.dp, bottom = 20.dp, end = 10.dp),
                    )
                }
            } else {
                // Back: the meaning alone (counter-rotated so it reads correctly).
                val backScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(backScroll)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // Centred like the share card — the reading line, set the
                        // same way on every surface that carries the full meaning.
                        Text(
                            text = name.meaning,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = readingMeasure()),
                        )
                    }
                    // A quiet scrollbar thumb on the card's right edge: position
                    // says where you are, size says how long the meaning runs.
                    // Only there while more lies below.
                    ScrollbarThumb(
                        scrollState = backScroll,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 20.dp, bottom = 20.dp, end = 10.dp),
                    )
                }
            }
            // The verdict the drag is heading toward, fading in as the finger
            // approaches the commit threshold: the same words the two buttons
            // beneath the card carry, set as a tracked overline at the card's
            // head. Composed only while a drag is live — an invisible merged
            // child would still reach TalkBack through the card's merged node,
            // and its graded alpha is read in the draw phase, so a moving
            // finger redraws without recomposing the faces at all.
            if (dragging) {
                val front = rotation <= 90f
                Text(
                    text = stringResource(
                        if (dragKnow) R.string.i_know_it else R.string.still_learning
                    ).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        dragKnow && front -> HeroGold
                        dragKnow -> MaterialTheme.colorScheme.secondary
                        front -> HeroSubtext
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp)
                        .graphicsLayer {
                            val fraction = if (cardWidth <= 0f) 0f
                            else (offsetX.value / (cardWidth * 0.3f)).coerceIn(-1f, 1f)
                            alpha = ((fraction.absoluteValue - 0.15f) / 0.85f)
                                .coerceIn(0f, 1f)
                        },
                )
            }
        }
    }
}

@Composable
private fun AllLearnedContent(
    onReviewLearned: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Reaching this screen IS a change of meaning — the book is finished
        // — so the numeral settles once, the house way (SettleOnce).
        SettleOnce {
            ArabicText(
                text = "٩٩",
                fontSize = ArabicSize.Panel,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.all_learned_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.all_learned_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onReviewLearned) {
            Text(stringResource(R.string.review_learned))
        }
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_memorize))
        }
    }
}

@Composable
private fun DeckDoneContent(onStartAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // A finished set earns the house seal: the gold hairline circle and
        // check an answered quiz option wears, scaled up and alone. Completion
        // is a moment, not a dead end — the button below is already the way on.
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.deck_done),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStartAgain) {
            Text(stringResource(R.string.start_again))
        }
    }
}
