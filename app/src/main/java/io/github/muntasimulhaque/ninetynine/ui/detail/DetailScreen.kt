package io.github.muntasimulhaque.ninetynine.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.share.ShareSheet
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
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
    ) { padding ->
        // The capsule overlays the pager, the same scroll-under float the
        // tab bar gives the lists (the Scaffold no longer reserves a bottom
        // slot, so only the top bar's padding is applied and the meaning
        // passes beneath the plate). The plate reports its laid-out height,
        // measured, not guessed: it follows the system font scale and the
        // navigation mode (24dp gesture, 48dp three-button), and that
        // clearance is what NamePage scrolls its tail above.
        val density = LocalDensity.current
        var plateHeightPx by remember { mutableIntStateOf(0) }
        val plateClearance = with(density) { plateHeightPx.toDp() }
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                NamePage(
                    name = pages[page],
                    pagerState = pagerState,
                    page = page,
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
            // The name page's floating capsule: previous and next, then the
            // two acts of keeping. Fixed, unlike the footer it replaced,
            // see [DetailNavPlate].
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { plateHeightPx = it.height },
            )
        }
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
