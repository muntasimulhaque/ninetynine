package io.github.muntasimulhaque.ninetynine.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ListInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameListItem
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameRowInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.TabTitle
import io.github.muntasimulhaque.ninetynine.ui.theme.components.EmptyState
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LazyScrollbarThumb
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.TabOverflowActions
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

    // The bar tucks itself away while reading and returns on the first upward pull.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = paperTopBarColors(),
                title = {
                    // The running head never leaves: search is a field at the
                    // head of the list now, not a mode the bar has to swap
                    // into, so there is nothing to cross between.
                    // 0.25f floor: see TabTitle — the app's own name must
                    // survive the narrowest bar at the largest scales.
                    TabTitle(stringResource(R.string.app_title), minScale = 0.25f)
                },
                actions = {
                    TabOverflowActions(onSettings = onSettings, onAbout = onAbout)
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
        // The rule between rows runs the row's own width now — there are no
        // folio numbers to indent past.
        Box(Modifier.fillMaxSize()) {
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
                item(key = "search") {
                    SearchField(
                        query = query,
                        onQueryChange = viewModel::setSearchQuery,
                    )
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
                        modifier = Modifier.padding(start = NameRowInset, end = NameRowInset),
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
 * The list's own search field — a quiet line at the head of the content,
 * below the day's name, above the rows.
 *
 * Search used to be a mode: an icon swapped the running head for a field, the
 * keyboard rose, and Back had to walk it all back. The machinery existed
 * because search was a place you went. It is a thing you do instead now —
 * the field is simply part of the page, the way an index sits at the head of
 * a book's contents. Nothing to open, nothing to close; typing filters live,
 * and the query stays until the reader clears it.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val searchLabel = stringResource(R.string.cd_search)
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ListInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    // The label rides on the field only while it is
                    // empty. Set unconditionally, contentDescription
                    // would replace the field's text and a screen
                    // reader would never read the query back.
                    .semantics {
                        if (query.isEmpty()) contentDescription = searchLabel
                    },
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
            // The clear button breathes in and out with the query it
            // serves instead of blinking into existence.
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(Motion.tween(Motion.QUICK)),
                exit = fadeOut(Motion.tween(Motion.QUICK)),
            ) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = ListInset),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
