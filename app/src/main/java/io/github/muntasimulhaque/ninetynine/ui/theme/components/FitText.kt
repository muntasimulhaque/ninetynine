package io.github.muntasimulhaque.ninetynine.ui.theme.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified

/**
 * Text set to fit the width it is given, stepping its size down instead of
 * wrapping or ellipsizing.
 *
 * For the handful of places where the words themselves have to survive at any
 * font scale: the app's own name, which must never be cut mid-"Allah", and the
 * bottom bar's labels, which at a system font scale of 2.0 would otherwise read
 * "MEM…" / "SETTI…". Shrinking still leaves them far larger than the default:
 * it only caps growth at what the space can hold. Measured up front, so there
 * is no first-frame flicker the way a layout-feedback loop would have.
 *
 * Tracking steps down with the size, because letter-spacing is part of a type
 * size and not a constant beside it. Held fixed it does two harmful things:
 * shrunken small caps look loose, and (worse) the fixed air sets a hard floor
 * on how narrow the line can ever get. The share card's wordmark is 27 tracked
 * characters, so 49dp of its 233dp is air that no amount of shrinking used to
 * remove; the 30-character wordmark it replaced could not render below 210dp
 * however far it shrank, and so clipped on any screen under ~357dp.
 *
 * [minScale] is a floor, never a target; it exists only so a pathological
 * constraint cannot loop forever. Set it low enough that the text always wins:
 * a caller that would rather be small than cut should say so.
 *
 * In a Row, a FitText measured before a fixed sibling sees the full row width
 * and declines to shrink, so the sibling then overflows the slot. Give it
 * Modifier.weight(1f, fill = false) so the fixed children are measured first;
 * the share wordmark already orders its seal before the text for this reason.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minScale: Float = 0.55f,
) {
    FitText(
        text = AnnotatedString(text),
        style = style,
        color = color,
        modifier = modifier,
        minScale = minScale,
    )
}

/**
 * [FitText] for text that already carries spans: the names list paints the
 * reader's literal search matches inside the transliteration, and a Divine
 * Name must still never lose its tail. The fit measures the styled text, so
 * the highlighted matches are part of the width the line is fitted to.
 */
@Composable
fun FitText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minScale: Float = 0.55f,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val available = constraints.maxWidth
        val fitted = remember(text, style, available, measurer, minScale) {
            val floor = style.fontSize * minScale
            var candidate = style
            while (candidate.fontSize > floor &&
                measurer.measure(text, candidate, softWrap = false).size.width > available
            ) {
                val tracking = candidate.letterSpacing
                candidate = candidate.copy(
                    fontSize = candidate.fontSize * 0.95f,
                    // Unspecified on every untracked style (headlineSmall,
                    // displaySmall), and multiplying that is not meaningful.
                    letterSpacing = if (tracking.isSpecified) tracking * 0.95f else tracking,
                )
            }
            candidate
        }
        // Ellipsis over clip: at every size that fits, this never renders;
        // past the minScale floor it degrades to a truncated label instead of
        // a mid-glyph cut. Insurance for pathological scale combinations.
        Text(
            text = text,
            style = fitted,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
