package io.github.muntasimulhaque.ninetynine.ui.theme.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R

/**
 * The maker's mark in its hairline ring: the app's own square-Kufic seal.
 *
 * Worn by the share card's foot, and earned (never merely worn) on the
 * quiz's perfect round and the finished flashcard set. One construction, so
 * the three cannot drift: the ring and the mark are both parameters, because
 * the share card sets them in its own plate gold on emerald while the two
 * earned seals wear the theme's `secondary`, the ink of every mark on paper.
 * Purely visual; the plate or page around it carries the meaning.
 */
@Composable
fun MarkSeal(
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
    markColor: Color = MaterialTheme.colorScheme.secondary,
    size: Dp = 52.dp,
    markSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .border(1.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mark),
            contentDescription = null,
            tint = markColor,
            modifier = Modifier.size(markSize),
        )
    }
}
