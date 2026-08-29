package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameListItem
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameRowInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.nameRowTextInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.EmptyState
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LazyScrollbarThumb
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.pageMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors

/**
 * The names the reader has marked learned.
 *
 * Memorize showed the count as a large, beautiful and completely inert number:
 * there was no way anywhere in the app to see *which* names it stood for,
 * short of scrolling all 99 looking for gold ticks. Bookmarks — the newer and
 * lighter of the two axes — had a whole tab. This is the same list in the same
 * rows, so a statistic becomes somewhere to go.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnedScreen(
    viewModel: NamesViewModel,
    onNameClick: (Int) -> Unit,
    onBrowseNames: () -> Unit,
    onBack: () -> Unit,
) {
    val names by viewModel.names.collectAsStateWithLifecycle()
    val namesLoaded by viewModel.namesLoaded.collectAsStateWithLifecycle()
    val learned by viewModel.learned.collectAsStateWithLifecycle()
    val learnedLoaded by viewModel.learnedLoaded.collectAsStateWithLifecycle()
    // A pushed screen keeps its own list state: Back from a name restores the
    // scroll position the reader left.
    val listState = rememberLazyListState()

    // Book order, like every other list in the app.
    val known = remember(names, learned) { names.filter { it.number in learned } }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = paperTopBarColors(),
                title = { ScreenLabel(stringResource(R.string.learned_names)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        // The rule between rows starts where the names do, not under their
        // numbers. And wide screens keep the book's column: the list and its
        // thumb hold page proportions on a tablet instead of stretching edge
        // to edge (phones never reach the cap).
        val dividerInset = nameRowTextInset()
        Box(
            Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = pageMeasure())
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (known.isEmpty() && learnedLoaded && namesLoaded) {
                    if (namesLoaded && names.isEmpty()) {
                        item { PageMessage(stringResource(R.string.names_unavailable)) }
                    } else {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                EmptyState(
                                    title = stringResource(R.string.empty_learned_title),
                                    body = stringResource(R.string.empty_learned_body),
                                    actionLabel = stringResource(R.string.action_browse_names),
                                    onAction = onBrowseNames,
                                )
                            }
                        }
                    }
                }
                items(known, key = { it.number }) { name ->
                    NameListItem(
                        name = name,
                        learned = true,
                        onClick = { onNameClick(name.number) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = dividerInset, end = NameRowInset),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            LazyScrollbarThumb(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp, end = 4.dp),
            )
        }
    }
}
