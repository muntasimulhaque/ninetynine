package io.github.muntasimulhaque.ninetynine.ui.bookmarks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameListItem
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NameRowInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.EmptyState
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LazyScrollbarThumb
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.AboutAction
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SettingsAction
import io.github.muntasimulhaque.ninetynine.ui.theme.components.nameRowTextInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors

/**
 * The names the reader has kept.
 *
 * Set exactly like the names list — same rows, same divider, same indent — so a
 * name looks the same wherever it is met. There is no search here and no count:
 * a kept list is short by its nature, and unlike memorization it has no total to
 * be measured against.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    viewModel: NamesViewModel,
    onNameClick: (Int) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    listState: LazyListState,
) {
    val names by viewModel.names.collectAsStateWithLifecycle()
    val namesLoaded by viewModel.namesLoaded.collectAsStateWithLifecycle()
    val learned by viewModel.learned.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val bookmarkedLoaded by viewModel.bookmarkedLoaded.collectAsStateWithLifecycle()

    // Book order, not the order they were kept in: this is a shelf, not a feed.
    // `names` is already 1..99, so filtering preserves it for nothing.
    val kept = remember(names, bookmarked) { names.filter { it.number in bookmarked } }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = paperTopBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.bookmarks),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                actions = {
                    AboutAction(onAbout)
                    SettingsAction(onSettings)
                },
            )
        },
    ) { padding ->
        // The rule between rows starts where the names do, not under their numbers.
        val dividerInset = nameRowTextInset()
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (kept.isEmpty() && bookmarkedLoaded && namesLoaded) {
                    // Which emptiness this is matters. Saying "nothing kept yet"
                    // when the asset failed to read tells the reader their kept
                    // names are gone, which is false and alarming — the names
                    // simply could not be loaded at all.
                    if (namesLoaded && names.isEmpty()) {
                        item { PageMessage(stringResource(R.string.names_unavailable)) }
                    } else {
                        item(key = "empty") {
                            // Centred in the viewport: an empty shelf is the whole
                            // content of the screen while it lasts.
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                EmptyState(
                                    title = stringResource(R.string.empty_bookmarks_title),
                                    body = stringResource(R.string.empty_bookmarks_body),
                                )
                            }
                        }
                    }
                }
                items(kept, key = { it.number }) { name ->
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
            LazyScrollbarThumb(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp, end = 4.dp),
            )
        }
    }
}
