package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.theme.SquircleShape

/** Deck options, named: what each one does, and whether it is already on. */
@Composable
internal fun DeckMenu(
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
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            // Paper on paper: the deck menu reads as a page of the book
            // lifted over the page (the app's own paper, no tonal lift)
            // not the stock floating tonal card, which was the last surface
            // in the app that didn't match the paper-on-paper system.
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
        ) {
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
 * Purely visual: the row above carries the state for screen readers.
 */
@Composable
internal fun OptionCheck(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = SquircleShape(4.dp),
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
