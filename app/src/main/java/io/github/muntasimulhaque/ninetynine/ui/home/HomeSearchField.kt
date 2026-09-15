package io.github.muntasimulhaque.ninetynine.ui.home

import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import kotlinx.coroutines.delay

/**
 * The bar's search field, standing where the running head stood.
 *
 * One quiet input set in the app's own ink: no plate, no chrome of its
 * own: the bar is the field while search is open, the way a system app's
 * toolbar simply becomes what it is doing. Typing filters the list beneath
 * live through the shared ViewModel query; the query persists until cleared,
 * whether by Back (one layer per press, see [HomeScreen]'s handler), by the
 * corner ✕, or by an empty result page offering "Clear search".
 *
 * The hint rides on the field only while it is empty, and serves as its
 * accessible label in that state: no separate contentDescription: set
 * unconditionally it would replace the field's text, and a screen reader
 * would never read the query back.
 */
@Composable
internal fun HomeSearchField(
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
                // to fingers and TalkBack alike. The bar slot is already tall,
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
            // is to dismiss the keyboard; it must not be dead.
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
