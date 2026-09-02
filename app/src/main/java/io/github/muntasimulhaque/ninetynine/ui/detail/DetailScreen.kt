package io.github.muntasimulhaque.ninetynine.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.share.ShareSheet
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalDarkTheme
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.NameGoldDark
import io.github.muntasimulhaque.ninetynine.ui.theme.NameGoldLight
import io.github.muntasimulhaque.ninetynine.ui.theme.TransliterationTealDark
import io.github.muntasimulhaque.ninetynine.ui.theme.TransliterationTealLight
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FloatingBar
import io.github.muntasimulhaque.ninetynine.ui.theme.components.MixedText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ReadingInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.readingMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SectionLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScrollbarThumb
import io.github.muntasimulhaque.ninetynine.ui.theme.components.tabLabelStyle
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// The reading measure sits in from the page edges. The prev/next chevrons
// used to bleed back out to the screen edges from inside the page; they live
// in the floating capsule below now (see DetailNavPlate), and this inset is
// the page's margin.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: NamesViewModel,
    startNumber: Int,
    bookmarksOnly: Boolean,
    onBack: () -> Unit,
) {
    val names by viewModel.names.collectAsStateWithLifecycle()
    val namesLoaded by viewModel.namesLoaded.collectAsStateWithLifecycle()
    val learned by viewModel.learned.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val bookmarkedLoaded by viewModel.bookmarkedLoaded.collectAsStateWithLifecycle()
    var showShare by rememberSaveable { mutableStateOf(false) }

    // The reader pages through the list they arrived from — all 99 from the
    // names list, or just the kept ones from Bookmarks. Taken once and then
    // held: un-bookmarking a name while reading it should un-fill the mark,
    // not pull pages out from under the reader and shift everything along.
    //
    // `rememberSaveable`, and it must be. A plain `remember` was wiped by every
    // Activity recreation, and the effect then rebuilt from the CURRENT
    // bookmarks — which, if the reader had just un-bookmarked the name they
    // were on, no longer contained it. The guard never passed again and the
    // screen spun for ever, escapable only by Back. Stored through the
    // List<Int> interface (an ArrayList under the hood, so it survives the
    // bundle the same way a bare ArrayList did) — the type is immutable so
    // the state can only ever be replaced whole, never mutated in place,
    // keeping every write a visible recomposition.
    var pageNumbers by rememberSaveable { mutableStateOf(listOf<Int>()) }
    LaunchedEffect(names, bookmarked, bookmarksOnly, namesLoaded, bookmarkedLoaded) {
        if (pageNumbers.isNotEmpty() || !namesLoaded || names.isEmpty()) return@LaunchedEffect
        // An empty bookmark set is indistinguishable from one DataStore has not
        // delivered yet, so wait for the flag rather than guessing.
        if (bookmarksOnly && !bookmarkedLoaded) return@LaunchedEffect

        val scoped = if (bookmarksOnly) names.filter { it.number in bookmarked } else names
        // The name being read always belongs in its own pager, even if it has
        // since been un-bookmarked — it was in the list when the screen opened,
        // and rebuilding without it is what used to strand the reader.
        val withStart =
            if (scoped.any { it.number == startNumber }) scoped
            else (scoped + names.filter { it.number == startNumber }).sortedBy { it.number }
        pageNumbers = withStart.map { it.number }
    }
    val pages = remember(names, pageNumbers) {
        pageNumbers.mapNotNull { number -> names.firstOrNull { it.number == number } }
    }

    if (pages.isEmpty()) {
        // Blank paper while the asset is still being read — quieter than a
        // spinner, and this screen is reachable straight from the notification
        // and the widget, so it is the app's first impression that morning.
        // Once the read has finished and there is still nothing, say so: this
        // page can be opened without ever passing Home, which is where the
        // explanation used to live exclusively.
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.barMeasure(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    title = {},
                    navigationIcon = { BackButton(onBack) },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (namesLoaded && names.isEmpty()) {
                    PageMessage(stringResource(R.string.names_unavailable))
                }
            }
        }
        return
    }

    val startIndex = remember(pages) {
        pages.indexOfFirst { it.number == startNumber }.coerceAtLeast(0)
    }
    // The pager's initial page is saved so rotation restores the reader to
    // the page they were on, not the page they entered from.
    var savedPage by rememberSaveable { mutableIntStateOf(startIndex) }
    val pagerState = rememberPagerState(initialPage = savedPage) { pages.size }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { savedPage = it }
    }
    val current = pages[pagerState.currentPage.coerceIn(0, pages.lastIndex)]
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val motionScale = LocalMotionScale.current

    // A featherweight tick as each page settles — like a bead slipping past.
    LaunchedEffect(pagerState) {
        var first = true
        snapshotFlow { pagerState.currentPage }.collect {
            if (first) first = false else haptics.tick()
        }
    }

    if (showShare) {
        ShareSheet(name = current, onDismiss = { showShare = false })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.barMeasure(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    ScreenLabel(
                        stringResource(
                            R.string.detail_counter,
                            pagerState.currentPage + 1,
                            pages.size,
                        ),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                },
                navigationIcon = { BackButton(onBack) },
                // Share keeps the edge: a send-away act reads at the page's
                // edge, and five slots would crowd a 320dp phone. Learning
                // and keeping live in the capsule below, beside the chevrons
                // they turn pages with.
                actions = {
                    IconButton(onClick = { showShare = true }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.cd_share),
                        )
                    }
                },
            )
        },
        // The name page's floating capsule: previous and next, then the two
        // acts of keeping. Fixed, unlike the footer it replaces — see
        // [DetailNavPlate].
        bottomBar = {
            DetailNavPlate(
                current = current,
                previousLabel = pages.getOrNull(pagerState.currentPage - 1)?.transliteration,
                nextLabel = pages.getOrNull(pagerState.currentPage + 1)?.transliteration,
                learned = current.number in learned,
                bookmarked = current.number in bookmarked,
                onToggleLearned = {
                    val number = current.number
                    viewModel.setLearned(number, number !in learned)
                },
                onToggleBookmarked = {
                    viewModel.setBookmarked(current.number, current.number !in bookmarked)
                },
                onPrevious = {
                    goToPage(scope, pagerState, pagerState.currentPage - 1, motionScale)
                },
                onNext = {
                    goToPage(scope, pagerState, pagerState.currentPage + 1, motionScale)
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { page ->
            NamePage(
                name = pages[page],
                pagerState = pagerState,
                page = page,
                // Pages dim slightly while in motion, then settle to full
                // presence. Read inside the layer block so a swipe redraws
                // rather than recomposing every visible page each frame.
                modifier = Modifier.graphicsLayer {
                    val offset =
                        ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .absoluteValue.coerceIn(0f, 1f)
                    alpha = 1f - offset * 0.3f
                },
            )
        }
    }
}

/**
 * Learning a name, in the bar.
 *
 * The page is one scroll container, so a footer control travels with the text —
 * on a long meaning it is well below the fold at exactly the moment a name
 * strikes you. The bar does not move, and the learned axis now sits beside the
 * bookmark: two acts of keeping, one place, the same feel.
 *
 * The unfilled check-circle rests in the page's ink; learned, it fills and
 * wears the app's gold — the same filled-and-gold treatment the bookmark has.
 * TalkBack hears the button once ("Mark as learned") and the state after it
 * ("Learned" / "Not learned"), never a label that changes under it.
 */
@Composable
private fun LearnedAction(learned: Boolean, number: Int, onToggle: () -> Unit) {
    val haptics = rememberHaptics()

    // The pop means "you just changed this", so it must not fire when the
    // reader swipes from an unlearned name to a learned one. The button lives
    // in the bar and survives page changes, so the latch is per-name: arriving
    // at a new number adopts its state silently, and only a toggle pops.
    val scale = remember { Animatable(1f) }
    val seededFor = remember { mutableStateOf<Int?>(null) }
    val motionScale = LocalMotionScale.current
    LaunchedEffect(number, learned) {
        if (seededFor.value != number) {
            seededFor.value = number
        } else {
            scale.snapTo(0.94f)
            scale.animateTo(1f, Motion.livelySpec(motionScale))
        }
    }

    val state = stringResource(if (learned) R.string.learned else R.string.not_learned)
    // A toggle, not a button that happens to carry state text: Role.Switch
    // gives TalkBack a toggle action and a checked/unchecked reading, matching
    // how the deck menu's checkbox row already behaves.
    IconButton(
        onClick = {
            haptics.confirm()
            onToggle()
        },
        modifier = Modifier.semantics {
            stateDescription = state
            role = Role.Switch
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (learned) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.mark_learned),
                // Resting, it wears the top bar's grey — the same ink the
                // share icon carries, not the page's near-black. Learned, it
                // fills with the app's gold, as before.
                tint = if (learned) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            )
            Spacer(Modifier.height(2.dp))
            // The eye gets the tab bar's short chrome label — same register:
            // 9sp x the device factor, never the reader's slider. The ear
            // keeps the full action from the icon's description, so TalkBack
            // never hears the state word twice; the label itself is silent.
            FitText(
                text = stringResource(R.string.plate_learned).uppercase(),
                style = tabLabelStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/**
 * Keeping a name, in the bar.
 *
 * The page is one scroll container, so a footer control travels with the text —
 * on a long meaning it is well below the fold at exactly the moment a name
 * strikes you. The bar does not move.
 *
 * The pop and the haptic are shared with [LearnedAction] deliberately: the app
 * has two per-name toggles on two different axes, and they should at least feel
 * like they were made by the same hand.
 */
@Composable
private fun BookmarkAction(bookmarked: Boolean, number: Int, onToggle: () -> Unit) {
    val haptics = rememberHaptics()

    // The pop means "you just changed this", so it must not fire when the
    // reader swipes from an unkept name to a kept one. The button lives in the
    // bar and survives page changes, so the latch is per-name: arriving at a
    // new number adopts its state silently, and only a toggle pops.
    val scale = remember { Animatable(1f) }
    val seededFor = remember { mutableStateOf<Int?>(null) }
    val motionScale = LocalMotionScale.current
    // `bookmarked` is THIS page's membership, so the keys are (page, its
    // state): a DataStore write for a previous page (after a swipe) cannot
    // change the current page's key, and only a toggle on this page pops.
    LaunchedEffect(number, bookmarked) {
        if (seededFor.value != number) {
            seededFor.value = number
        } else {
            scale.snapTo(0.94f)
            scale.animateTo(1f, Motion.livelySpec(motionScale))
        }
    }

    // The button is named once and never changes; what changes is the state
    // announced after it. Set on the button, which is the node TalkBack focuses.
    val state = stringResource(if (bookmarked) R.string.bookmarked else R.string.not_bookmarked)
    IconButton(
        onClick = {
            haptics.confirm()
            onToggle()
        },
        modifier = Modifier.semantics {
            stateDescription = state
            role = Role.Switch
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = stringResource(R.string.cd_bookmark),
                // Same resting grey as the learned act and the share icon;
                // kept, it fills with the app's gold.
                tint = if (bookmarked) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            )
            Spacer(Modifier.height(2.dp))
            FitText(
                text = stringResource(R.string.plate_bookmark).uppercase(),
                style = tabLabelStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/**
 * The name page's floating capsule — the same [FloatingBar] plate the tab
 * screens float, carrying everything a reader does to a name: previous and
 * next (wearing the neighbour's transliteration, so the bar says what turning
 * the page brings), and the two acts of keeping, learned and bookmarked,
 * adjacent in the centre. Share stays in the top bar: a send-away act reads
 * at the page's edge, and five slots would crowd a 320dp phone.
 *
 * It replaces the footer that used to scroll with the text — on a long
 * meaning the chevrons sat below the fold at exactly the moment a name
 * strikes you. The bar does not move, so turning is always one tap away; its
 * labels change as the pager settles, the same moment the counter above does.
 * The weighted end slots keep the two keep-acts centred whether the
 * neighbours exist (page 0 has no previous; the last page no next).
 */
@Composable
private fun DetailNavPlate(
    current: Name,
    previousLabel: String?,
    nextLabel: String?,
    learned: Boolean,
    bookmarked: Boolean,
    onToggleLearned: () -> Unit,
    onToggleBookmarked: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    FloatingBar {
        Row(
            // The row takes its height from its children with the same 54dp
            // floor the tab bar's slots wear, so the two capsules read as one
            // register. Each weighted slot caps its label at the space it
            // owns, so two long transliterations can never overlap at any
            // font scale — the labels shrink through FitText instead of
            // wrapping mid-word, and the 20dp chevron survives every name.
            modifier = Modifier
                .barMeasure()
                .heightIn(min = 54.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (previousLabel != null) {
                    // The chevron carries the direction visually; the name
                    // alone would leave a screen-reader user unable to tell
                    // previous from next. (Computed here: a semantics block
                    // is not a composable context.)
                    val previousCd = stringResource(R.string.previous_name, previousLabel)
                    TextButton(
                        onClick = onPrevious,
                        // TextButton's default 16dp horizontal padding ate a
                        // quarter of the weighted slot; the slot's own weight
                        // keeps the tap honest with far less.
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.semantics {
                            contentDescription = previousCd
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            // The tab bar's icon register; the arrow survives
                            // every name and every font scale.
                            modifier = Modifier.size(20.dp),
                        )
                        // Roman, not italic. Italic means epithet, gloss or
                        // quote everywhere else in the app — the page above
                        // has just taught the reader that — so setting a
                        // Name in it says the wrong thing. titleSmall also
                        // rescues these from TextButton's labelLarge, which
                        // made the app's main keep-reading affordance the
                        // smallest Latin on the page. FitText, not Ellipsis:
                        // a Divine Name must never cut off, so the longest
                        // transliterations (Al-Muta'aalee, Al-Mutakabbir,
                        // Al-Mu'akhkhir) shrink a little instead of losing
                        // their tail. TextButton's own primary carries the
                        // colour, as the bare Text did before.
                        FitText(
                            text = previousLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            LearnedAction(
                learned = learned,
                number = current.number,
                onToggle = onToggleLearned,
            )
            BookmarkAction(
                bookmarked = bookmarked,
                number = current.number,
                onToggle = onToggleBookmarked,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (nextLabel != null) {
                    val nextCd = stringResource(R.string.next_name, nextLabel)
                    TextButton(
                        onClick = onNext,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.semantics {
                            contentDescription = nextCd
                        },
                    ) {
                        FitText(
                            text = nextLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NamePage(
    name: Name,
    pagerState: PagerState,
    page: Int,
    modifier: Modifier = Modifier,
) {
    // Single scrollable page: the controls scroll with the content, but a
    // weighted spacer pushes them to just above the system bar whenever the
    // content is shorter than the screen.
    // The page keeps the book's measure on wide screens — the Name, the
    // meaning, the note and the footer all hold `readingMeasure`'s column,
    // and the thumb hugs that column's edge. Phones never reach the cap.
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = readingMeasure())
    ) {
        val minPageHeight = maxHeight
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = ReadingInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.defaultMinSize(minHeight = minPageHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(30.dp))
                // The transliteration belongs to the Name: 8dp is the share
                // card's pairing (50sp Arabic), one step from the hero's 6dp
                // (48sp) — this 52sp page sits in the 8dp family. The Arabic
                // line box at 1.60 leading no longer adds ~5sp of empty
                // descent air on top of the spacer, so the pair reads as one
                // unit while the meaning below keeps its clear step.
                //
                // A clear step below the Arabic, set in the same displaySmall
                // slot the share card, hero and flashcard faces use — the Name
                // leads its transliteration at the same ratio everywhere. FitText
                // keeps the proper noun whole: a Name split across lines reads
                // as two words, and this page must survive a large system font
                // the same way the hero card does.
                //
                // The Name and its transliteration are one selectable unit —
                // long-press copies either whole, exactly as the meaning below
                // copies. A name worth keeping travels further than the page,
                // and every serious reading surface answers a held finger with
                // selection. (The flashcard faces stay swipe surfaces on
                // purpose; the note below rides with its meaning rather than
                // joining the selection trio.)
                SelectionContainer {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ArabicText(
                            text = name.arabic,
                            fontSize = ArabicSize.Page,
                            // The Name wears the app's gold — as close to the hero
                            // plates' #D4B45A as paper contrast allows at large-text
                            // 3:1 (see NameGoldLight). Theme-aware: the warmed gold
                            // on paper, the brighter gold on the night page. It stands
                            // apart from the teal transliteration beneath it.
                            color = if (LocalDarkTheme.current) NameGoldDark else NameGoldLight,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(scaledGap(8.dp)))
                        FitText(
                            text = name.transliteration,
                            style = MaterialTheme.typography.displaySmall.copy(
                                textAlign = TextAlign.Center,
                            ),
                            // The transliteration is set apart from the meaning
                            // by its teal as well as its size — the meaning below
                            // stays in the page's ink. Theme-aware: dark ink on
                            // light paper, pale mint on the night page.
                            color = if (LocalDarkTheme.current) TransliterationTealDark
                            else TransliterationTealLight,
                            minScale = 0.45f,
                        )
                    }
                }
                Spacer(Modifier.height(scaledGap(20.dp)))
                // Long-press copies: a meaning worth keeping travels further
                // than the page — alongside the Name's own selectable unit
                // above, and every serious reading surface answers a held
                // finger with selection. The flashcard faces stay swipe
                // surfaces on purpose.
                SelectionContainer {
                    Text(
                        text = name.meaning,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = readingMeasure()),
                    )
                }
                if (name.note != null) {
                    // One step down from the meaning's 20: the note is an
                    // annex of the meaning, not its sibling (at 26 the two
                    // perceived gaps were statistically identical).
                    Spacer(Modifier.height(scaledGap(24.dp)))
                    Column(
                        modifier = Modifier.widthIn(max = readingMeasure()),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        // SectionLabel, not a bare Text: the identical styling,
                        // plus the heading semantics every other overline
                        // carries — heading navigation anchors on the note too.
                        SectionLabel(stringResource(R.string.note_label))
                        Spacer(Modifier.height(scaledGap(8.dp)))
                        MixedText(
                            text = name.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }

        // A quiet scrollbar thumb on the page's right edge — the platform's
        // own signal for "more of the meaning lies below", with its size
        // telling a reader at a glance how many screens the page runs to.
        // Only present while it does lie below.
        ScrollbarThumb(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, bottom = 16.dp, end = 8.dp),
        )
    }
}

/**
 * Moves the pager one page — snapped instantly when the user has disabled
 * animations (animator scale 0), the same respect `Motion.*` gives every
 * other animation in the app. The pager's own animate call has no spec
 * parameter, so the choice has to be made here.
 */
private fun goToPage(
    scope: CoroutineScope,
    pagerState: PagerState,
    target: Int,
    motionScale: Float,
) {
    scope.launch {
        if (motionScale == 0f) pagerState.scrollToPage(target)
        else pagerState.animateScrollToPage(target)
    }
}
