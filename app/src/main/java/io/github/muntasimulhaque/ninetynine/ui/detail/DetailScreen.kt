package io.github.muntasimulhaque.ninetynine.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.share.ShareSheet
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// The reading measure sits in from the page edges. The page-turner lives in
// the floating capsule below (see DetailNavPlate); the keep acts are not in
// it: Bookmark is in the top bar and Learned closes the entry. This inset is
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

    // The reader pages through the list they arrived from, all 99 from the
    // names list, or just the kept ones from Bookmarks. Taken once and then
    // held: un-bookmarking a name while reading it should un-fill the mark,
    // not pull pages out from under the reader and shift everything along.
    //
    // `rememberSaveable`, and it must be. A plain `remember` was wiped by every
    // Activity recreation, and the effect then rebuilt from the CURRENT
    // bookmarks, which, if the reader had just un-bookmarked the name they
    // were on, no longer contained it. The guard never passed again and the
    // screen spun for ever, escapable only by Back. Stored through the
    // List<Int> interface (an ArrayList under the hood, so it survives the
    // bundle the same way a bare ArrayList did), the type is immutable so
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
        // since been un-bookmarked; it was in the list when the screen opened,
        // and rebuilding without it is what used to strand the reader.
        val withStart =
            if (scoped.any { it.number == startNumber }) scoped
            else (scoped + names.filter { it.number == startNumber }).sortedBy { it.number }
        pageNumbers = withStart.map { it.number }
    }
    val pages = remember(names, pageNumbers) {
        pageNumbers.mapNotNull { number -> names.firstOrNull { it.number == number } }
    }

    // The neighbour labels share one size, fitted to the widest transliteration
    // in the book so the pair never resizes as the reader pages. Measured, not
    // counted: the three 13-character names do not all set at the same width.
    val labelStyle = MaterialTheme.typography.titleSmall
    val measurer = rememberTextMeasurer()
    val widestLabel = remember(names, labelStyle, measurer) {
        names.maxByOrNull {
            measurer.measure(it.transliteration, labelStyle, softWrap = false).size.width
        }?.transliteration.orEmpty()
    }

    if (pages.isEmpty()) {
        // Blank paper while the asset is still being read, quieter than a
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

    // A featherweight tick as each page settles, like a bead slipping past.
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
                // Keeping, then sending: the kept-page act sits inboard and
                // Share keeps the edge it has always had. Learned is not here:
                // it closes the entry on the page, where the reading ends.
                // Two actions leave the counter its room; five slots crowded
                // a 320dp phone.
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
                            Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.cd_share),
                        )
                    }
                },
            )
        },
    ) { padding ->
        // The capsule overlays the pager, the same scroll-under float the
        // tab bar gives the lists. When there is a neighbour the plate is the
        // only bottom furniture and the meaning passes beneath it; when the
        // pager holds a single name there is no page-turner, so the Scaffold's
        // bottom inset does the clearing instead. The plate reports its
        // laid-out height, measured, not guessed: it follows the system font
        // scale and the navigation mode (24dp gesture, 48dp three-button),
        // and that clearance is what NamePage scrolls its tail above.
        val density = LocalDensity.current
        var plateHeightPx by remember { mutableIntStateOf(0) }
        val previousLabel = pages.getOrNull(pagerState.currentPage - 1)?.transliteration
        val nextLabel = pages.getOrNull(pagerState.currentPage + 1)?.transliteration
        val hasNeighbours = previousLabel != null || nextLabel != null
        val plateClearance =
            if (hasNeighbours) with(density) { plateHeightPx.toDp() } else 0.dp
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = if (hasNeighbours) 0.dp else padding.calculateBottomPadding(),
                ),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val pageName = pages[page]
                NamePage(
                    name = pageName,
                    learned = pageName.number in learned,
                    onToggleLearned = {
                        viewModel.setLearned(pageName.number, pageName.number !in learned)
                    },
                    bottomClearance = plateClearance,
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
            // The name page's floating capsule is the page-turner, and only
            // the page-turner: Bookmark lives in the top bar and Learned closes
            // the entry. Fixed, unlike the footer it replaced, see
            // [DetailNavPlate].
            if (hasNeighbours) {
                DetailNavPlate(
                    previousLabel = previousLabel,
                    nextLabel = nextLabel,
                    widestLabel = widestLabel,
                    onPrevious = {
                        goToPage(scope, pagerState, pagerState.currentPage - 1, motionScale)
                    },
                    onNext = {
                        goToPage(scope, pagerState, pagerState.currentPage + 1, motionScale)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { plateHeightPx = it.height },
                )
            }
        }
    }
}

/**
 * Keeping a name, in the top bar.
 *
 * Bookmarking a page can happen at any moment, so the act stays in the chrome
 * and never travels below the fold. It sits beside Share, inboard of it: the
 * send-away act keeps the edge, and keeping is the more used of the two.
 * Learned is not here; it closes the entry on the page (see [NamePage]),
 * because that is a verdict made after reading, not a page-level keep. The
 * top bar stays at two actions, the counter keeps its room, and the bookmark
 * glyph is the one keep act a reader already knows.
 *
 * The pop and haptic are shared with the learned act at the foot deliberately:
 * two per-name toggles on two axes should at least feel made by the same hand.
 */
@Composable
private fun BookmarkAction(
    bookmarked: Boolean,
    number: Int,
    onToggle: () -> Unit,
) {
    val haptics = rememberHaptics()

    // The pop means "you just changed this", so it must not fire when the
    // reader swipes from an unkept name to a kept one. The button lives in the
    // bar and survives page changes, so the latch is per-name: arriving at a
    // new number adopts its state silently, and only a toggle pops.
    val scale = remember { Animatable(1f) }
    val seededFor = remember { mutableStateOf<Int?>(null) }
    val motionScale = LocalMotionScale.current
    LaunchedEffect(number, bookmarked) {
        if (seededFor.value != number) {
            seededFor.value = number
        } else {
            scale.snapTo(0.94f)
            scale.animateTo(1f, Motion.livelySpec(motionScale))
        }
    }

    // The button is named once and never changes; what changes is the state
    // announced after it, set on the button TalkBack focuses.
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
        Icon(
            imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = stringResource(R.string.cd_bookmark),
            // Same resting grey as Share; kept, it fills with the app's gold.
            tint = if (bookmarked) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

/**
 * Moves the pager one page: snapped instantly when the user has disabled
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
