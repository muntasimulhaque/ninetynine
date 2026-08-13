package io.github.muntasimulhaque.ninetynine.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.share.ShareSheet
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalDarkTheme
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.TransliterationTealDark
import io.github.muntasimulhaque.ninetynine.ui.theme.TransliterationTealLight
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LearnedButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.MixedText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ReadingInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.readingMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScrollProgressBar
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// The reading measure sits in from the page edges. The prev/next footer does
// not — it belongs to the edges themselves, so it bleeds back out through this
// inset (see NamePage).

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

    // A single calm fade as the screen settles in.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = Motion.tween(Motion.CALM),
        label = "detailEnter",
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
                // Keeping, then sending: the inward act sits inside, and Share
                // keeps the edge it has always had.
                actions = {
                    BookmarkAction(
                        bookmarked = current.number in bookmarked,
                        number = current.number,
                        onToggle = {
                            viewModel.setBookmarked(current.number, current.number !in bookmarked)
                        },
                    )
                    IconButton(onClick = { showShare = true }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(R.string.cd_share),
                        )
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer { alpha = enterAlpha },
        ) { page ->
            NamePage(
                name = pages[page],
                learned = pages[page].number in learned,
                onToggleLearned = {
                    val number = pages[page].number
                    viewModel.setLearned(number, number !in learned)
                },
                pagerState = pagerState,
                page = page,
                previousLabel = pages.getOrNull(page - 1)?.transliteration,
                nextLabel = pages.getOrNull(page + 1)?.transliteration,
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
 * Keeping a name, from the bar rather than the foot of the page.
 *
 * The page is one scroll container, so the footer travels with the text — on a
 * long meaning it is well below the fold at exactly the moment a name strikes
 * you. The bar does not move.
 *
 * The pop and the haptic are lifted from [LearnedButton] deliberately: the app
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
        modifier = Modifier.semantics { stateDescription = state },
    ) {
        Icon(
            imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = stringResource(R.string.cd_bookmark),
            tint = if (bookmarked) MaterialTheme.colorScheme.secondary else LocalContentColor.current,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

@Composable
private fun NamePage(
    name: Name,
    learned: Boolean,
    onToggleLearned: () -> Unit,
    pagerState: PagerState,
    page: Int,
    previousLabel: String?,
    nextLabel: String?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val motionScale = LocalMotionScale.current

    // Single scrollable page: the controls scroll with the content, but a
    // weighted spacer pushes them to just above the system bar whenever the
    // content is shorter than the screen.
    BoxWithConstraints(modifier.fillMaxSize()) {
        val minPageHeight = maxHeight
        // The two chevron labels share the footer row with their icons. Each
        // text is capped at half the row minus the chrome (two icons, two
        // internal gaps, two button paddings) so that two long transliterations
        // can never overlap at any font scale, and ellipsizes instead of
        // wrapping mid-word.
        val chevronTextMax = ((maxWidth - ReadingInset * 2 - 88.dp) / 2f).coerceAtLeast(48.dp)
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
                ArabicText(
                    text = name.arabic,
                    fontSize = ArabicSize.Page,
                    // The Name is set in the app's gold (the same family the
                    // share card's Arabic wears on its emerald plate), so it
                    // stands apart from the teal transliteration beneath it.
                    // Theme-aware: the hardened gold on paper, the lighter gold
                    // on the night page.
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )
                // The transliteration belongs to the Name: 8dp is the share
                // card's pairing (50sp Arabic), one step from the hero's 6dp
                // (48sp) — this 52sp page sits in the 8dp family. The Arabic
                // line box at 1.60 leading no longer adds ~5sp of empty
                // descent air on top of the spacer, so the pair reads as one
                // unit while the meaning below keeps its clear step.
                Spacer(Modifier.height(scaledGap(8.dp)))
                // A clear step below the Arabic, set in the same displaySmall
                // slot the share card, hero and flashcard faces use — the Name
                // leads its transliteration at the same ratio everywhere. FitText
                // keeps the proper noun whole: a Name split across lines reads
                // as two words, and this page must survive a large system font
                // the same way the hero card does.
                FitText(
                    text = name.transliteration,
                    style = MaterialTheme.typography.displaySmall.copy(
                        textAlign = TextAlign.Center,
                    ),
                    // The transliteration is set apart from the meaning by its
                    // teal as well as its size — the meaning below stays in the
                    // page's ink. Theme-aware: dark ink on light paper, pale
                    // mint on the night page.
                    color = if (LocalDarkTheme.current) TransliterationTealDark
                    else TransliterationTealLight,
                    minScale = 0.45f,
                )
                Spacer(Modifier.height(scaledGap(20.dp)))
                // Centred like the share card — the reading line, set the same
                // way on every surface that carries the full meaning.
                Text(
                    text = name.meaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = readingMeasure()),
                )
                if (name.note != null) {
                    // One step down from the meaning's 20: the note is an
                    // annex of the meaning, not its sibling (at 26 the two
                    // perceived gaps were statistically identical).
                    Spacer(Modifier.height(scaledGap(24.dp)))
                    Column(
                        modifier = Modifier.widthIn(max = readingMeasure()),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = stringResource(R.string.note_label).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        MixedText(
                            text = name.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(28.dp))
                LearnedButton(learned = learned, onToggle = onToggleLearned)
                Spacer(Modifier.height(10.dp))
                Row(
                    // Widened back out through the page inset so the chevrons
                    // land on the same vertical line as the back and share
                    // icons in the top bar, instead of floating in toward the
                    // middle of the page. Safe to overflow: the padding sits
                    // inside the scroll container, so this only reaches the
                    // viewport's own edge — nothing clips it.
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val bleed = ReadingInset.roundToPx()
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = constraints.minWidth + bleed * 2,
                                    maxWidth = constraints.maxWidth + bleed * 2,
                                )
                            )
                            layout(constraints.maxWidth, placeable.height) {
                                // Mirrors with the layout direction; in RTL the
                                // start side is physically right.
                                placeable.place(
                                    if (layoutDirection == LayoutDirection.Rtl) bleed else -bleed,
                                    0,
                                )
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (previousLabel != null) {
                        // The chevrons carry the direction visually; the name
                        // alone would leave a screen-reader user unable to tell
                        // previous from next. (Computed here: a semantics block
                        // is not a composable context.)
                        val previousCd = stringResource(R.string.previous_name, previousLabel)
                        TextButton(
                            onClick = { goToPage(scope, pagerState, page - 1, motionScale) },
                            modifier = Modifier.semantics {
                                contentDescription = previousCd
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                            // Roman, not italic. Italic means epithet, gloss or
                            // quote everywhere else in the app — the page above
                            // has just taught the reader that — so setting a
                            // Name in it says the wrong thing. titleSmall also
                            // rescues these from TextButton's labelLarge, which
                            // made the app's main keep-reading affordance the
                            // smallest Latin on the page.
                            Text(
                                previousLabel,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = chevronTextMax),
                            )
                        }
                    } else {
                        Spacer(Modifier.widthIn(min = 48.dp))
                    }
                    if (nextLabel != null) {
                        val nextCd = stringResource(R.string.next_name, nextLabel)
                        TextButton(
                            onClick = { goToPage(scope, pagerState, page + 1, motionScale) },
                            modifier = Modifier.semantics {
                                contentDescription = nextCd
                            },
                        ) {
                            Text(
                                nextLabel,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = chevronTextMax),
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    } else {
                        Spacer(Modifier.widthIn(min = 48.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // A thin bar on the page's right edge that fills as the reader scrolls,
        // and is only there while more of the meaning lies below — a clearer
        // nudge than the old fold-fade, which hid the last line and was easy
        // to miss.
        ScrollProgressBar(
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
