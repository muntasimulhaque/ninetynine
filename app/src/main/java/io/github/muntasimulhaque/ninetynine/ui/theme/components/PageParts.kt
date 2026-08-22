package io.github.muntasimulhaque.ninetynine.ui.theme.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.times
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalTextScale

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
 * The way to Settings from a tab screen.
 *
 * It sits on all three — Names, Bookmarks, Memorize — and always last in the
 * bar, so it reads as a fixed corner rather than something one screen happens
 * to offer. A gear present on some tabs and missing from others would be worse
 * than either extreme.
 */
/**
 * The way to About from a tab screen.
 *
 * About is content, not configuration — the source it is drawn from, the
 * typefaces, the du'a, the hadith the whole app exists for. Reaching it took
 * Names → gear → About → About, so most readers never would. It sits to the
 * LEFT of the gear because the gear is the fixed corner of every tab bar, and
 * because About is the lighter of the two. The Settings row to it stays: a
 * second path costs nothing.
 */
@Composable
fun AboutAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.about),
        )
    }
}

@Composable
fun SettingsAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.Filled.Settings,
            contentDescription = stringResource(R.string.settings),
        )
    }
}

/** The way back, identical on every pushed screen. */
@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
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
        Text(text = text, style = fitted, color = color, maxLines = 1, softWrap = false)
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
