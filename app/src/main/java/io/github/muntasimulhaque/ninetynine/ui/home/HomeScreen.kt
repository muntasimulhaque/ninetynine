package io.github.muntasimulhaque.ninetynine.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
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
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameListItem
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameRowInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.AboutAction
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SettingsAction
import io.github.muntasimulhaque.ninetynine.ui.theme.components.nameRowTextInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.util.SearchFilter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NamesViewModel,
    onNameClick: (Int) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    listState: LazyListState,
) {
    val names by viewModel.names.collectAsStateWithLifecycle()
    val namesLoaded by viewModel.namesLoaded.collectAsStateWithLifecycle()
    val learned by viewModel.learned.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    var searching by rememberSaveable { mutableStateOf(false) }
    // Focus is requested the moment search mode is entered, and only then:
    // returning from a pushed screen restores the field and the query but
    // must not re-open the keyboard over a list the reader may consider done.
    var searchFocusRequested by rememberSaveable { mutableStateOf(false) }
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

    if (searching) {
        BackHandler {
            viewModel.setSearchQuery("")
            searching = false
        }
    }

    val filtered = remember(names, query) { SearchFilter.filter(names, query) }
    val dailyName = remember(names, dailyNumber) { names.firstOrNull { it.number == dailyNumber } }

    // The bar tucks itself away while reading and returns on the first upward pull.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = paperTopBarColors(),
                title = {
                    if (searching) {
                        val focusRequester = remember { FocusRequester() }
                        val focusManager = LocalFocusManager.current
                        val searchLabel = stringResource(R.string.cd_search)
                        BasicTextField(
                            value = query,
                            onValueChange = viewModel::setSearchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                // The label rides on the field only while it is
                                // empty. Set unconditionally, contentDescription
                                // would replace the field's text and a screen
                                // reader would never read the query back.
                                .semantics {
                                    if (query.isEmpty()) contentDescription = searchLabel
                                },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
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
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                        LaunchedEffect(Unit) {
                            if (searchFocusRequested) {
                                searchFocusRequested = false
                                focusRequester.requestFocus()
                            }
                        }
                    } else {
                        HomeTitle()
                    }
                },
                navigationIcon = {
                    if (searching) {
                        IconButton(onClick = {
                            viewModel.setSearchQuery("")
                            searching = false
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_close_search),
                            )
                        }
                    }
                },
                actions = {
                    if (searching) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = {
                            searching = true
                            searchFocusRequested = true
                        }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.cd_search),
                            )
                        }
                        // About then the gear, last in the bar, as on every tab
                        // screen. Absent while searching, which is a mode, not
                        // a place.
                        AboutAction(onAbout)
                        SettingsAction(onSettings)
                    }
                },
            )
        },
    ) { padding ->
        val contentPadding = PaddingValues(
            start = 0.dp,
            end = 0.dp,
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 16.dp,
        )
        // The rule between rows starts where the names do, not under their numbers.
        val dividerInset = nameRowTextInset()
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (query.isBlank() && dailyName != null) {
                item {
                    DailyHeroCard(dailyName, onClick = { onNameClick(dailyName.number) })
                }
            }
            if (names.isEmpty() && namesLoaded) {
                // The asset failed to read. Without this the screen would be
                // blank paper with no explanation at all.
                item { PageMessage(stringResource(R.string.names_unavailable)) }
            } else if (filtered.isEmpty() && names.isNotEmpty()) {
                item { PageMessage(stringResource(R.string.no_results)) }
            }
            items(filtered, key = { it.number }) { name ->
                NameListItem(
                    name = name,
                    learned = name.number in learned,
                    onClick = { onNameClick(name.number) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = dividerInset, end = NameRowInset),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/**
 * The app's full name, set to fit the bar on one line.
 *
 * It must never ellipsize: "The 99 Names of A…" would cut Allah's name, which
 * is the whole reason the launcher label is the short form instead. The bar's
 * height is fixed, so wrapping would clip it too — it shrinks to fit.
 *
 * Material measures a top bar's title with the width left over after the
 * navigation icon and the actions, so the three buttons on the right are
 * already accounted for. Each one costs 48dp, which on a Pixel 4 leaves 247dp
 * of the 343dp bar.
 *
 * Set at 0.85 of headlineSmall — about 16sp rather than 19. "The Ninety Nine
 * Names of Allah" is 14.864 em in Spectral SemiBold, so at 19sp it needed 282dp
 * and was shrinking to 0.875 on a Pixel 4 once About joined the bar; at 16sp it
 * needs 240dp and renders whole there. It is a running head, and a running head
 * is meant to be quieter than the page it sits over — a book sets them smaller
 * than the body, so this is closer to right than 19sp was.
 *
 * The floor is 0.25 because the worst case is real: on a 320dp screen with the
 * in-app slider at 1.4x on top of a 2.0 system font scale it needs 672dp of the
 * 172dp available. It must never ellipsize — "The Ninety Nine Names of A…"
 * would cut Allah's name, which is the whole reason the launcher label is the
 * short form instead.
 */
private const val RunningHeadScale = 0.85f

@Composable
private fun HomeTitle() {
    val base = MaterialTheme.typography.headlineSmall
    FitText(
        text = stringResource(R.string.app_title),
        style = base.copy(fontSize = base.fontSize * RunningHeadScale),
        color = MaterialTheme.colorScheme.onSurface,
        minScale = 0.25f,
        // The tab screens' titles are headings, so heading navigation covers
        // the whole top level — the running head included.
        modifier = Modifier.semantics { heading() },
    )
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
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
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
            Spacer(Modifier.height(14.dp))
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

