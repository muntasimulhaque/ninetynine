package io.github.muntasimulhaque.ninetynine.ui.home

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroSubtext
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LocalBottomBarOverlay
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameListItem
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameRowInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.nameRowTextInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.TabTitle
import io.github.muntasimulhaque.ninetynine.ui.theme.components.pageMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.EmptyState
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LazyScrollbarThumb
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import io.github.muntasimulhaque.ninetynine.util.SearchFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NamesViewModel,
    onNameClick: (Int) -> Unit,
    listState: LazyListState,
) {
    val names by viewModel.names.collectAsStateWithLifecycle()
    val namesLoaded by viewModel.namesLoaded.collectAsStateWithLifecycle()
    val learned by viewModel.learned.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    var dailyNumber by remember { mutableIntStateOf(viewModel.dailyNameNumber()) }

    // The daily name rolls over at local midnight. The recompute runs once a
    // minute while the screen is resumed, and repeatOnLifecycle re-enters the
    // block on the way back to the foreground — so the hero card never shows
    // yesterday's name after the widget has already turned, while a phone
    // left sitting on Home in the background is not woken every minute to
    // update a card nobody is looking at.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                dailyNumber = viewModel.dailyNameNumber()
                delay(60_000)
            }
        }
    }

    val filtered = remember(names, query) { SearchFilter.filter(names, query) }
    val dailyName = remember(names, dailyNumber) { names.firstOrNull { it.number == dailyNumber } }

    // Search is a place in the bar: a magnifier swaps the running head for a
    // field wherever the reader already is — mid-list after an upward pull,
    // not only at the head of the content. The plate this replaces scrolled
    // WITH the list, so from row sixty there was no path to search except
    // scrolling all the way home.
    //
    // Openness survives process death (rememberSaveable) and it re-derives
    // from a live query: returning to this tab with results showing must find
    // the field open — a filtered list without its visible field would be a
    // lie about where those rows came from.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(query) {
        if (query.isNotEmpty()) searchOpen = true
    }

    // Back always means "step out of what you are doing", one layer per
    // press while there are layers to step out of: with text typed, Back
    // clears it (the ✕'s job, for a thumb still resting on the gesture);
    // with the field empty, Back leaves search. Only past both does Back do
    // what it has always done on a top-level tab — leave the app. So a
    // reader mid-search can never be ejected by the gesture that everywhere
    // else retreats, and nothing outside search changed at all.
    //
    // PredictiveBackHandler, not BackHandler: while search is open the
    // system's back-to-home preview animates alongside the unwind, instead
    // of being suppressed until the gesture commits.
    if (searchOpen) {
        PredictiveBackHandler { events ->
            // The unwinding work happens on the gesture's COMMIT (the flow
            // completes); a release before commit cancels the collection and
            // search stays exactly as it was. Progress events are unused —
            // search has no preview of its own to morph.
            try {
                events.collect { }
                if (query.isNotEmpty()) {
                    viewModel.setSearchQuery("")
                } else {
                    searchOpen = false
                    focusManager.clearFocus()
                }
            } catch (e: CancellationException) {
                // Gesture released before commit.
            }
        }
    }

    // The bar tucks itself away while reading and returns on the first upward pull.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // The bar must never stay tucked when the list itself has come home.
    // Re-tapping NAMES animates item 0 back into view, but the enter-always
    // offset survived the journey — landing at the head with no bar left the
    // reader staring at the hero card believing they were short of the top.
    // Arrival at item 0 (the re-tap, or a reader who flung back themselves)
    // reveals the chrome beside it.
    //
    // Edge-triggered deliberately: parked AT the top, the index stays 0
    // through the first upward push while the enter-always connection hides
    // the bar before the list even moves — a continuous watch would snap it
    // straight back and the bar would never tuck. Only a false→true arrival
    // fires; who caused it (tab re-tap or a hand) is irrelevant.
    var wasAtTop by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 }.collect { atTop ->
            if (atTop && !wasAtTop) {
                scrollBehavior.state.heightOffset = 0f
            }
            wasAtTop = atTop
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                modifier = Modifier.barMeasure(),
                scrollBehavior = scrollBehavior,
                colors = paperTopBarColors(),
                title = {
                    // One bar, two registers, switching as a whole: the book's
                    // title page gives way to the search field and back again,
                    // both cross-fading on the house QUICK fade so the two
                    // slots read as one switch rather than two movements.
                    //
                    // Home is the book's title page — with only the
                    // magnifier beside it (or, searching, none) its title
                    // keeps the full headlineSmall register (sizeScale 1f)
                    // where the other tabs run quieter heads. Settings joined
                    // the bottom bar (1.18), so the corner the gear once took
                    // is freed and FitText renders the title larger in it; it
                    // still holds inside the narrower space the magnifier
                    // costs. 0.25f floor: see TabTitle — the app's own name
                    // must survive the narrowest bar at the largest scales.
                    Crossfade(
                        targetState = searchOpen,
                        animationSpec = Motion.tween(Motion.QUICK),
                        label = "homeHead",
                    ) { open ->
                        if (open) {
                            HomeSearchField(
                                query = query,
                                onQueryChange = viewModel::setSearchQuery,
                            )
                        } else {
                            TabTitle(
                                stringResource(R.string.app_title),
                                minScale = 0.25f,
                                sizeScale = 1f,
                            )
                        }
                    }
                },
                actions = {
                    // The two arms of the switch share the actions edge so the
                    // magnifier's position becomes the close button's — the
                    // thumb learns one corner of the screen.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(
                            visible = !searchOpen,
                            enter = fadeIn(Motion.tween(Motion.QUICK)),
                            exit = fadeOut(Motion.tween(Motion.QUICK)),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { searchOpen = true }) {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = stringResource(R.string.cd_search),
                                    )
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = searchOpen,
                            enter = fadeIn(Motion.tween(Motion.QUICK)),
                            exit = fadeOut(Motion.tween(Motion.QUICK)),
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    searchOpen = false
                                    focusManager.clearFocus()
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.cd_close_search),
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        val contentPadding = PaddingValues(
            start = 0.dp,
            end = 0.dp,
            top = padding.calculateTopPadding(),
            // Clears the floating plate plus its margins — the bar's measured
            // height (it follows font scale and the device's navigation mode)
            // plus a little air; the scrim needs none for the list itself.
            bottom = padding.calculateBottomPadding() +
                LocalBottomBarOverlay.current + 16.dp,
        )
        // The rule between rows starts where the names do, not under their
        // numbers. And wide screens keep the book's column: the list, the hero
        // card and the thumb hold page proportions on a tablet instead of
        // stretching edge to edge (phones never reach the cap).
        val dividerInset = nameRowTextInset()
        Box(
            Modifier
                .fillMaxSize()
                // The keyboard rises for the bar's search field; without this
                // the last result rows sit behind the IME. Edge-to-edge is
                // enabled app-wide, so the inset must be consumed here.
                .imePadding()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = pageMeasure())
        ) {
            LazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (query.isBlank() && dailyName != null) {
                    // A midnight sitter sees the card turn rather than cut:
                    // the new day's name rises gently into the plate, the way
                    // a pushed screen arrives. Motion.spec, not tween: a
                    // transitionSpec is not a composable context, so the
                    // scale-aware specs build from the hoisted scale exactly
                    // like the NavHost's do. The card itself is simply there —
                    // no entrance of its own; motion belongs to changes of
                    // meaning, not to first frames.
                    item {
                        val motionScale = LocalMotionScale.current
                        AnimatedContent(
                            targetState = dailyName.number,
                            transitionSpec = {
                                (fadeIn(Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle)) +
                                    slideInVertically(
                                        Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle),
                                    ) { it / 12 })
                                    .togetherWith(fadeOut(Motion.spec(motionScale, Motion.QUICK)))
                            },
                            label = "dailyHero",
                        ) { number ->
                            val turningName = names.firstOrNull { it.number == number }
                                ?: dailyName
                            DailyHeroCard(turningName, onClick = { onNameClick(turningName.number) })
                        }
                    }
                }
                if (names.isEmpty() && namesLoaded) {
                    // The asset failed to read. Without this the screen would be
                    // blank paper with no explanation at all.
                    item { PageMessage(stringResource(R.string.names_unavailable)) }
                } else if (filtered.isEmpty() && names.isNotEmpty()) {
                    item(key = "no-results") {
                        // Centred in the viewport, not hugging the bar: an empty
                        // result is the whole content of the screen while it lasts.
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                title = stringResource(R.string.no_results_title),
                                body = stringResource(R.string.no_results_body),
                                actionLabel = stringResource(R.string.action_clear_search),
                                onAction = { viewModel.setSearchQuery("") },
                            )
                        }
                    }
                }
                items(filtered, key = { it.number }) { name ->
                    NameListItem(
                        name = name,
                        learned = name.number in learned,
                        onClick = { onNameClick(name.number) },
                        // The list shows its work: every literal match of the
                        // query paints gold in the row's name and epithet.
                        query = query,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = dividerInset, end = NameRowInset),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            // The same quiet position thumb the reading pages carry, so a deep
            // fling through 99 rows answers the hand the way a long meaning does.
            LazyScrollbarThumb(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp, end = 4.dp),
            )
        }
    }
}

/**
 * The bar's search field, standing where the running head stood.
 *
 * One quiet input set in the app's own ink: no plate, no chrome of its
 * own — the bar is the field while search is open, the way a system app's
 * toolbar simply becomes what it is doing. Typing filters the list beneath
 * live through the shared ViewModel query; the query persists until cleared,
 * whether by Back (one layer per press, see [HomeScreen]'s handler), by the
 * corner ✕, or by an empty result page offering "Clear search".
 *
 * The hint rides on the field only while it is empty, and serves as its
 * accessible label in that state — no separate contentDescription: set
 * unconditionally it would replace the field's text, and a screen reader
 * would never read the query back.
 */
@Composable
private fun HomeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    // races composition on some devices; a few short retries settle it the
    // honest way instead of a fixed sleep.
    LaunchedEffect(Unit) {
        repeat(4) {
            try {
                focusRequester.requestFocus()
                return@LaunchedEffect
            } catch (_: IllegalStateException) {
                delay(50)
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                // The field's own box is one text line (~24dp); without this
                // the app's only search entry point offers a sub-48dp target
                // to fingers and TalkBack alike. The bar slot is already tall —
                // this hands that height to the control.
                .heightIn(min = 48.dp)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            // Filtering is live, so the action key's only job
            // is to dismiss the keyboard — it must not be dead.
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun DailyHeroCard(name: Name, onClick: () -> Unit) {
    // The card yields slightly under the finger — paper, not glass.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = Motion.soft(),
        label = "heroPress",
    )
    // clickable on the modifier rather than Card(onClick): M3's clickable card
    // takes no onClickLabel, and its content — a Name, an epithet, some Arabic
    // — never says what tapping it does. This way TalkBack offers "Open
    // today's name" instead of its bare "double-tap to activate", exactly as
    // the list rows already do. The card's own press ripple is preserved by
    // feeding clickable the same interaction source the scale animation reads.
    Card(
        modifier = Modifier
            // Horizontal 20dp puts the card on the same edge as the list
            // beneath it; 12dp is the sheet's own top air, so the card
            // never hugs the app bar.
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClickLabel = stringResource(R.string.cd_open_daily),
                onClick = { onClick() },
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = HeroContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.notification_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = HeroGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(scaledGap(14.dp)))
            ArabicText(
                text = name.arabic,
                fontSize = ArabicSize.Panel,
                color = HeroGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            // The name is a proper noun and gets set whole. Left to wrap, a
            // long one at a large font scale breaks mid-word — "Al-Wa / asi'"
            // — which is the one thing the app is careful never to do.
            FitText(
                text = name.transliteration,
                style = MaterialTheme.typography.displaySmall.copy(
                    textAlign = TextAlign.Center,
                ),
                color = HeroText,
                minScale = 0.45f,
            )
            Spacer(Modifier.height(2.dp))
            // Two lines. On one line this cut the meaning of the day in half —
            // several of the 99 epithets do not fit a phone at default size,
            // so roughly one morning in eight the app opened on "The Perfect
            // Lord And Master Upon Whom Th…". The card has the height to spare.
            Text(
                text = name.title,
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = HeroSubtext,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
