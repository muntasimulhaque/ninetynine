package io.github.muntasimulhaque.ninetynine.ui.theme.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalDeviceFactor
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalTextScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion

/*
 * The furniture every page is built from. Kept in one place so Memorize,
 * Settings and About cannot drift apart: a tracked gold overline, a hairline
 * rule, and a row set the way a table of contents is set.
 */

/**
 * Named page insets — three tiers, documented so screens pick deliberately.
 * Lists are tighter (the eye scans), reading pages are roomier (the eye dwells).
 */
val ListInset = 20.dp
val PageInset = 24.dp
val ReadingInset = 28.dp

/** True while the bottom bar floats OVER content (scroll-under); screens grow
 *  their bottom content padding so the last rows clear the plate. */
val LocalBottomBarOverlay = staticCompositionLocalOf { false }

/** Small caps in gold, widely tracked — the app's only kind of heading label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * The title of a pushed screen. Same tracked small caps as a section label but
 * in quiet ink, so being one level down reads the same everywhere.
 */
@Composable
fun ScreenLabel(text: String, modifier: Modifier = Modifier) {
    // FitText, not Text: the counters ("QUESTION 1 OF 10", "3 OF 99") sit in
    // the fixed 64dp app bar, and at a combined 2.8x scale they wrapped to
    // two lines and clipped. Shrinking beats wrapping for a running register.
    FitText(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
        minScale = 0.4f,
    )
}

/**
 * Top bars are paper, exactly like the page beneath them — never a tinted
 * band, and never one that tints itself on scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun paperTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
)

/**
 * The tab bar's single quiet corner.
 *
 * Settings and About were two permanent icons on every tab bar — chrome a
 * reader meets every hour but uses weekly — then one glyph opening a small
 * overflow menu carrying both rows. The menu is gone too: a popup of exactly
 * two rows was ceremony where one tap would do, and the stock DropdownMenu
 * was the only floating tonal surface in an app that is otherwise paper on
 * paper. The corner now holds ONE plain outlined gear — the platform's most
 * universally understood glyph — straight to Settings in a single tap.
 *
 * About, the other row the menu used to carry, sits at the foot of Settings:
 * it is front matter a reader meets once, and one hop from the corner it
 * always was only one tap further. Present on ALL THREE tab screens — Names,
 * Bookmarks, Memorize — so it still reads as a fixed corner rather than
 * something one screen happens to offer.
 */
@Composable
fun TabSettingsAction(
    onSettings: () -> Unit,
) {
    IconButton(onClick = onSettings) {
        Icon(
            Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.settings),
        )
    }
}

/**
 * A tab screen's running head.
 *
 * Set at 0.85 of headlineSmall — about 16sp rather than 19. "The Ninety Nine
 * Names of Allah" is 14.864 em in Spectral SemiBold, so at 19sp it needed 282dp
 * and shrank to 0.875 on a Pixel 4 once About joined the bar; at 16sp it needs
 * 240dp and renders whole there. A running head is meant to be quieter than the
 * page beneath it — a book sets them smaller than the body — and all three tab
 * screens now wear the same quiet register, instead of one shrinking while the
 * other two shouted.
 *
 * [minScale] is FitText's floor. Names passes 0.25f because its worst case is
 * real: a 320dp screen at the in-app 1.4x on top of a 2.0 system font scale
 * needs 672dp of the 172dp available, and the app's own name must never
 * ellipsize — "…Names of A…" would cut Allah's name. Shorter titles never
 * reach the floor, so they take the default.
 *
 * [sizeScale] resizes the head itself. With the search field and the About/
 * Settings glyphs gone from the Home bar, its title has an entire row to
 * itself, so Home passes 1f and runs at the full headlineSmall — the one tab
 * that reads like a title page rather than a running head. FitText still does
 * the fitting: the size only rises where the measured width allows it, so a
 * large font scale or a narrow screen simply shrinks it back inside the bar,
 * never over the overflow corner.
 */
@Composable
fun TabTitle(
    text: String,
    minScale: Float = 0.55f,
    sizeScale: Float = RunningHeadScale,
) {
    val base = MaterialTheme.typography.headlineSmall
    FitText(
        text = text,
        style = base.copy(fontSize = base.fontSize * sizeScale),
        color = MaterialTheme.colorScheme.onSurface,
        minScale = minScale,
        // The tab screens' titles are headings, so heading navigation covers
        // the whole top level — the running head included.
        modifier = Modifier.semantics { heading() },
    )
}

private const val RunningHeadScale = 0.85f

/** The bottom bar's tab label: the ramp's labelSmall at the device factor only —
 *  chrome never follows the reader's text-size slider (the clipping guarantee). */
@Composable
fun tabLabelStyle(): TextStyle =
    MaterialTheme.typography.labelSmall.copy(
        fontSize = (9 * LocalDeviceFactor.current).sp,
        letterSpacing = 1.2.sp,
    )

/** The way back, identical on every pushed screen. */
@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
        )
    }
}

/**
 * Text set to fit the width it is given, stepping its size down instead of
 * wrapping or ellipsizing.
 *
 * For the handful of places where the words themselves have to survive at any
 * font scale: the app's own name, which must never be cut mid-"Allah", and the
 * bottom bar's labels, which at a system font scale of 2.0 would otherwise read
 * "MEM…" / "SETTI…". Shrinking still leaves them far larger than the default —
 * it only caps growth at what the space can hold. Measured up front, so there
 * is no first-frame flicker the way a layout-feedback loop would have.
 *
 * Tracking steps down with the size, because letter-spacing is part of a type
 * size and not a constant beside it. Held fixed it does two harmful things:
 * shrunken small caps look loose, and — worse — the fixed air sets a hard floor
 * on how narrow the line can ever get. The share card's wordmark is 27 tracked
 * characters, so 49dp of its 233dp is air that no amount of shrinking used to
 * remove; the 30-character wordmark it replaced could not render below 210dp
 * however far it shrank, and so clipped on any screen under ~357dp.
 *
 * [minScale] is a floor, never a target; it exists only so a pathological
 * constraint cannot loop forever. Set it low enough that the text always wins:
 * a caller that would rather be small than cut should say so.
 */
@Composable
fun FitText(
    text: String,
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

/**
 * A quiet line of italic explanation where a list would have been.
 *
 * Shared by the names list (nothing matched, or the asset failed to load) and
 * the bookmarks list (nothing kept yet), so an empty screen reads the same way
 * wherever the reader meets one.
 */
@Composable
fun PageMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * An empty screen that explains itself: an upright title, an italic line of
 * guidance beneath it, and an optional way out.
 *
 * Used for the empties a reader can act on — nothing kept, none learned, a
 * search with no matches — while [PageMessage] stays for the failure cases
 * that offer no action. Set like the rest of the book: title in the ink,
 * guidance in the quiet italic, and the house TextButton rather than a new
 * kind of control arriving on an empty page.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * The width a column of prose is allowed to reach.
 *
 * In ems, not dp, and it moves with the reading scale. Fixed at 560dp it gave
 * **87 characters a line** at the smallest text setting and 53 at the largest —
 * Spectral Regular averages 0.4475 em per character across the 99 meanings, so
 * the measure swung by two thirds while the book range is 60–66. The worst of
 * it fell on the reader who *chose* smaller text, often precisely to fit more
 * on screen, and who got the longest line as a result.
 *
 * 65 × 0.4475 em × 17sp = 494dp at the default scale, and it grows from there,
 * so the character count holds at every slider position. Only binds on tablets
 * and unfolded foldables; a phone column is narrower than this anyway.
 */
@Composable
fun readingMeasure(): Dp = (494 * LocalTextScale.current).dp

/**
 * The widest a screen's content column may run.
 *
 * Where [readingMeasure] caps the prose line, this caps the whole page —
 * lists, flashcards, quiz, settings — so a tablet gets the book's proportions
 * instead of rows stretched edge to edge. It grows with the reading slider,
 * like [readingMeasure], and never binds on a phone, whose screen is narrower
 * than the cap already.
 */
@Composable
fun pageMeasure(): Dp = (560 * LocalTextScale.current).dp

/**
 * Chrome joins the book's column.
 *
 * The content keeps the centred [pageMeasure] cap — but the bars did not: a
 * tablet read a title pinned to the screen's edge while its page floated
 * centred, chrome orphaned from the content it serves. This puts any top bar
 * (and the bottom bar's contents) inside the same centred cap, so the whole
 * screen — running head, page, footer — shares one set of margins. It is the
 * exact pattern the content uses (fillMaxWidth · wrapContentWidth · widthIn,
 * in that order), so on a phone, where the cap never binds, nothing changes.
 */
@Composable
fun Modifier.barMeasure(): Modifier = fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = pageMeasure())

/**
 * A gap that grows with the type it separates.
 *
 * Every space in the app was a dp constant while only the type responded to the
 * reading slider, so at 1.4x the text was 40% larger and the air between blocks
 * was unchanged — the page tightened exactly when the reader had asked for
 * room, and About's paragraphs began to read as one block. For structural gaps
 * only: touch targets and chrome padding stay fixed, because those answer to
 * the finger rather than to the text.
 */
@Composable
fun scaledGap(base: Dp): Dp = base * LocalTextScale.current

/**
 * Content that settles into place once — and only once — as it appears: the
 * scale rises from [fromScale] on the lively spring the toggles answer with,
 * while alpha fades in QUICK. A rotation replays nothing (the played flag
 * rides saved instance state), and with animations off the content is simply
 * there — every Motion spec collapses to snap at animator scale 0.
 *
 * Motion only where meaning changes: this is for arrivals that ARE a change
 * of meaning — the quiz's earned seal, the ٩٩ on the all-learned screen —
 * never decoration on a first frame.
 */
@Composable
fun SettleOnce(
    modifier: Modifier = Modifier,
    fromScale: Float = 0.85f,
    content: @Composable BoxScope.() -> Unit,
) {
    var played by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }
    val scale by animateFloatAsState(
        targetValue = if (played) 1f else fromScale,
        animationSpec = Motion.lively(),
        label = "settleScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = Motion.tween(Motion.QUICK),
        label = "settleAlpha",
    )
    Box(
        // Graded values read in the draw phase, so the settle never
        // recomposes its content.
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        content = content,
    )
}

/** The thinnest rule the screen can draw — separates matter, never decorates. */
@Composable
fun PageRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * A table-of-contents row: title (with an optional gloss beneath) and a gold
 * chevron. The whole row is the tap target.
 */
@Composable
fun NavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClickLabel: String = stringResource(R.string.cd_open),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // TalkBack speaks this instead of its bare "double-tap to
            // activate", so a contents row says what tapping it opens.
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(vertical = if (subtitle == null) 17.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = titleStyle, color = titleColor)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
    }
}
