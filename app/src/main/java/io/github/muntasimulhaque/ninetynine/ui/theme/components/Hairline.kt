package io.github.muntasimulhaque.ninetynine.ui.theme.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion

/**
 * The thumb never spans more than this fraction of its track.
 *
 * Exactly-proportional thumbs betray the metaphor on a book of short pages:
 * a meaning only 1.2 screens long yields a thumb covering ~90% of the edge,
 * which reads as the long line the fill bar was, not as a scrollbar. Capping
 * the length keeps it always reading as a thumb while position — the part
 * readers actually steer by — stays exact. The floor in [ScrollbarThumb] is
 * applied after this cap, so very short tracks still get the 24dp minimum.
 */
private const val THUMB_MAX_FRACTION = 0.40f

/**
 * A whisper of a progress bar: a hairline gold fill on a paper track.
 * Used for memorization progress, flashcard decks, and the quiz.
 */
@Composable
fun HairlineProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
) {
    val fraction by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = Motion.tween(Motion.CALM, easing = Motion.Settle),
        label = "hairlineProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            // `outline`, not `outlineVariant`: this track is where the bar
            // ends, so it carries meaning. With an invisible track there is
            // nothing to judge the gold fill against. outlineVariant is 1.42:1
            // on paper; WCAG 1.4.11 asks 3:1 of non-text that informs.
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(CircleShape)
                // Not `secondary`: the gold fill at 1.30:1 (light) / 2.26:1
                // (dark) against the track fails WCAG 1.4.11. The scheme's own
                // onSecondaryContainer is a deep bronze in light (3.38:1 vs
                // outline) and a pale gold in dark (3.42:1) — same family,
                // readable boundary, no new colours.
                .background(MaterialTheme.colorScheme.onSecondaryContainer),
        )
    }
}

/**
 * A quiet scrollbar thumb on the page's right edge — the platform's own shape
 * for "there is more below".
 *
 * Replaces an earlier fill bar (a 2dp sliver that filled downward). The thumb
 * was chosen over it because it is the one scroll signal every Android reader
 * has already learned — Settings lists, WebViews and RecyclerViews all show
 * one while flinging — and because it says more at a glance: its position is
 * where you are, and its size says how much of the page one screen holds, so
 * a short thumb says "several screens to go" without a word.
 *
 * Persistent by decision, not the hide-until-scroll kind: a reader sitting at
 * the top of a long meaning is exactly the person who needs telling. It fades
 * away only when there is nothing to tell — `canScrollForward` is false once
 * the content fits, so it never covers a word.
 *
 * Display-only. Dragging it would make it a fast-scroller, rejected earlier
 * as wrong for a book of short pages.
 *
 * Geometry is computed in the draw phase straight off the [ScrollState], so
 * scrolling redraws the thumb without recomposing anything. The thumb travels
 * the track minus its own height, the way every OS scrollbar behaves. Its
 * length is the honest viewport-to-content fraction, clamped both ways — see
 * [THUMB_MAX_FRACTION] for why the top clamp exists.
 *
 * Decorative: carries no semantics, so TalkBack reads the text and not the
 * chrome.
 */
@Composable
fun ScrollbarThumb(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val visible by animateFloatAsState(
        targetValue = if (scrollState.canScrollForward) 1f else 0f,
        animationSpec = Motion.tween(Motion.QUICK),
        label = "scrollThumb",
    )
    val color = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(3.dp)
            .graphicsLayer { alpha = visible }
            .drawBehind {
                val maxScroll = scrollState.maxValue
                if (maxScroll <= 0) return@drawBehind
                val minThumbPx = 24.dp.toPx()
                val thumbHeight = (
                    size.height * (size.height / (size.height + maxScroll))
                    ).coerceAtMost(size.height * THUMB_MAX_FRACTION)
                    .coerceAtLeast(minThumbPx)
                    .coerceAtMost(size.height)
                val travelled = scrollState.value.toFloat() / maxScroll
                val top = (size.height - thumbHeight) * travelled
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, thumbHeight),
                    cornerRadius = CornerRadius(size.width / 2f),
                )
            },
    )
}