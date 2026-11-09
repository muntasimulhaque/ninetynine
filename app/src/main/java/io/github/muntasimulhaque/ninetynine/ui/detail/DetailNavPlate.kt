package io.github.muntasimulhaque.ninetynine.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FloatingBar

private val PlateHeight = 54.dp
private val PlateRadius = PlateHeight / 2
private val LabelPadding = 8.dp
private val ChevronGap = 6.dp
private val ChevronSize = 20.dp
private val DividerWidth = 0.5.dp
private val DividerHeight = 16.dp

// The two halves meet at a straight edge in the middle and keep the plate's
// own semicircle at the outer end. Start/end, not physical left/right: in RTL
// the previous button is on the right and must keep the rounded cap there.
// The caller's Surface already clips to the capsule, but clipping each half
// here makes the ripple follow the same silhouette instead of a bare
// rectangle.
private val StartHalfShape = RoundedCornerShape(
    topStart = PlateRadius,
    bottomStart = PlateRadius,
)
private val EndHalfShape = RoundedCornerShape(
    topEnd = PlateRadius,
    bottomEnd = PlateRadius,
)
private val WholePlateShape = RoundedCornerShape(50)

/**
 * The name page's floating capsule: the page-turner, and only the page-turner.
 *
 * Two half-buttons span the plate, each with its whole half as the tap target,
 * so there is no dead air around the label and no second pill inside the pill.
 * The neighbour's transliteration and an outward chevron say what turning the
 * page brings. The two labels share one fitted size, measured from the book's
 * widest transliteration and the space one half owns, so neither name looks
 * more important and the size does not change from page to page. A 0.5dp
 * hairline divides the halves only when both exist; at the first and last
 * pages the one available direction takes the whole plate. A Divine Name is
 * never ellipsized.
 *
 * The keep acts left this plate. Bookmark lives in the top bar, Learned at the
 * foot of the entry: the two acts have different timing, and page turning is a
 * reading act. The plate is simpler, the page is calmer, and the capsule no
 * longer crowds four actions into one row on a 320dp phone.
 *
 * It is an OVERLAY on the pager, not a Scaffold bottom bar: a reserved slot
 * clips the page's text at the plate's top edge. The caller measures this
 * composable's height and hands it to [NamePage] as the clearance its tail
 * scrolls above. When the current page has no neighbour at all, the caller
 * omits the plate rather than showing an empty capsule.
 */
@Composable
internal fun DetailNavPlate(
    modifier: Modifier = Modifier,
    previousLabel: String?,
    nextLabel: String?,
    widestLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    FloatingBar(modifier = modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(PlateHeight)
        ) {
            val density = LocalDensity.current
            val dividerPx = with(density) { DividerWidth.toPx() }
            val halfWidthPx = ((constraints.maxWidth - dividerPx) / 2f).coerceAtLeast(0f)
            // One half owns this much text: the half's width less its two
            // inner paddings, the chevron, and the gap before the label. The
            // single-button state deliberately keeps fitting to this same
            // width, so the surviving name does not grow when its neighbour
            // disappears.
            val textAreaPx = with(density) {
                (halfWidthPx -
                    LabelPadding.toPx() * 2f -
                    ChevronSize.toPx() -
                    ChevronGap.toPx()).coerceAtLeast(0f)
            }.toInt()
            val base = MaterialTheme.typography.titleSmall
            val measurer = rememberTextMeasurer()
            val fitted = remember(
                base,
                textAreaPx,
                previousLabel,
                nextLabel,
                widestLabel,
                measurer,
            ) {
                fittedLabelStyle(
                    measurer = measurer,
                    base = base,
                    labels = listOfNotNull(
                        previousLabel,
                        nextLabel,
                        widestLabel.takeIf { it.isNotBlank() },
                    ),
                    maxWidthPx = textAreaPx,
                )
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    previousLabel != null && nextLabel != null -> {
                        NeighbourButton(
                            label = previousLabel,
                            previous = true,
                            shape = StartHalfShape,
                            textStyle = fitted,
                            onClick = onPrevious,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .width(DividerWidth)
                                .height(DividerHeight)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        NeighbourButton(
                            label = nextLabel,
                            previous = false,
                            shape = EndHalfShape,
                            textStyle = fitted,
                            onClick = onNext,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    previousLabel != null -> NeighbourButton(
                        label = previousLabel,
                        previous = true,
                        shape = WholePlateShape,
                        textStyle = fitted,
                        onClick = onPrevious,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    nextLabel != null -> NeighbourButton(
                        label = nextLabel,
                        previous = false,
                        shape = WholePlateShape,
                        textStyle = fitted,
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * One half of the page-turner. The whole half is the button: its ripple is
 * clipped to the half's own end of the capsule, the outer corners follow the
 * plate, and the inner edge meets the hairline. The direction is carried by
 * the chevron, the position, and the spoken description; the visible word is
 * cleared from semantics so TalkBack hears the action once.
 */
@Composable
private fun NeighbourButton(
    label: String,
    previous: Boolean,
    shape: Shape,
    textStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(
        if (previous) R.string.previous_name else R.string.next_name,
        label,
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LabelPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (previous) {
                Chevron(left = true)
                Spacer(Modifier.width(ChevronGap))
            }
            Text(
                text = label,
                style = textStyle,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics {},
            )
            if (!previous) {
                Spacer(Modifier.width(ChevronGap))
                Chevron(left = false)
            }
        }
    }
}

@Composable
private fun Chevron(left: Boolean) {
    Icon(
        imageVector = if (left) {
            Icons.AutoMirrored.Filled.KeyboardArrowLeft
        } else {
            Icons.AutoMirrored.Filled.KeyboardArrowRight
        },
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(ChevronSize),
    )
}

/**
 * The one size both neighbour names wear.
 *
 * The reference is the widest label in the book, not the longer of the two
 * currently shown: fitting the current pair would keep them equal within a
 * page but let the size jump as the reader pages. The titleSmall base is the
 * register; at ordinary widths this returns it unchanged, and only a large
 * font scale or a narrow phone steps it down. The line height follows the
 * fitted size so the label cannot grow the 54dp row at large scales.
 *
 * The 0.25 floor is low enough that a 320dp phone at the maximum combined
 * scale can still hold the longest name; the emergency ellipsis is insurance,
 * not a plan.
 */
private fun fittedLabelStyle(
    measurer: TextMeasurer,
    base: TextStyle,
    labels: List<String>,
    maxWidthPx: Int,
): TextStyle {
    val floor = base.fontSize * 0.25f
    var candidate = base
    fun widest(style: TextStyle): Int = labels.maxOfOrNull {
        measurer.measure(it, style, softWrap = false).size.width
    } ?: 0
    while (candidate.fontSize > floor && widest(candidate) > maxWidthPx) {
        candidate = candidate.copy(fontSize = candidate.fontSize * 0.95f)
    }
    return candidate.copy(lineHeight = candidate.fontSize * 1.4f)
}
