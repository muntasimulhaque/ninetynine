package io.github.muntasimulhaque.ninetynine.ui.theme.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.ninetynine.ui.theme.ArabicFamily
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalTextScale

/**
 * KFGQPC HAFS encodes madda the mushaf way (alef + combining maddah, U+0627
 * U+0653) and has no glyph for precomposed U+0622, so decompose before
 * rendering. Keeps the assets NFC-clean.
 */
fun String.forArabicFont(): String = replace("\u0622", "\u0627\u0653")

/**
 * The text is Arabic, and says so: the locale span lets a screen reader pick
 * an Arabic voice for the Name instead of attempting it with the device's
 * default (usually English) one. [ArabicFamily] is explicit, so font
 * resolution is unaffected. [MixedText]'s Arabic runs wear the same span.
 */
internal val ArabicLocale = LocaleList("ar")

/**
 * Arabic text in the bundled mushaf typeface with generous line height for
 * diacritics. Sizes are given explicitly rather than taken from the type
 * scale, so the reader's text-size preference is applied here by hand.
 */
/**
 * The Arabic sizes, named by the Latin they sit with.
 *
 * There is a rigorous fifteen-slot scale for Latin and, until this existed,
 * nothing at all for the script the book is actually about: every Arabic size
 * was a bare literal at its call site, and the app had drifted into seven
 * different Arabic-to-Latin ratios — 1.86 on the name page, 1.79 on the share
 * card, 1.57 on a flashcard, 1.43 on the hero and quiz cards, 1.31 in the list.
 *
 * That reversed the reading order between the two most-seen screens: on a name
 * page the eye lands on the Name and the transliteration is plainly
 * subordinate, while on the home screen it landed on "Al-Wadood" and the Arabic
 * read as a caption above it. On the front page of a book of the Names, the
 * Name should win.
 *
 * The name page's 52:24 is the reference the rest are tuned to. It is not
 * arbitrary: HAFS's body height is 0.346 em against Spectral's 0.450 x-height,
 * so at 2.17 the Arabic body sits above the Latin's cap height — the Name
 * dominates its transliteration without stranding the Latin.
 */
object ArabicSize {
    /** Pairs `displaySmall` on the name page — the reference pairing, 2.17x. */
    val Page = 52.sp

    /** Pairs `displaySmall` on the share card. */
    val Card = 50.sp

    /** Pairs `displaySmall` on the hero, quiz and flashcard faces. */
    val Panel = 48.sp

    /** Pairs `titleMedium` in the names list, restoring a display-like ratio. */
    val Row = 30.sp

    /** Pairs `titleLarge` — the basmala and other set-apart lines. */
    val Line = 30.sp

    /** Pairs `labelMedium` — the share card's small basmala. */
    val Caption = 15.sp
}

@Composable
fun ArabicText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    // Single-line displays need less leading than multi-line passages. 1.60
    // keeps ~6sp of headroom above the tallest shadda+fatha stack at 52sp
    // while reclaiming the empty descent air (HAFS declares a 0.586em
    // descent zone, but naskh letters barely use it) — the Name page's
    // perceived gap between Arabic and transliteration shrinks by ~5sp
    // without touching the spacer. Every current call site renders one
    // line; pass 1.85f if a multi-line passage is ever added.
    lineHeightFactor: Float = 1.60f,
) {
    val shaped = remember(text) { text.forArabicFont() }
    val size = fontSize * LocalTextScale.current
    Text(
        text = shaped,
        modifier = modifier,
        style = TextStyle(localeList = ArabicLocale),
        color = color,
        fontSize = size,
        fontFamily = ArabicFamily,
        // Pinned, never inherited: ArabicFamily declares Normal only, so any
        // heavier ambient style would make Compose synthesise the weight —
        // a fake-bold smear on a face whose licence forbids modification.
        fontWeight = FontWeight.Normal,
        textAlign = textAlign,
        // 1.60, not 1.7 or 1.85. HAFS declares ascender 1.172 + descender
        // 0.586 = 1.758 em of its own clearance, so anything below 1.7
        // shaves the line box exactly where the shadda-and-fatha stacks
        // live. Measured from the file's hhea table, not guessed: at 1.60
        // the ascent zone (1.067em) still clears the worst stack — lam ink
        // 0.806em plus ~0.14em of marks — with ~6sp to spare, and the
        // saving is all in the empty descent zone below, which is what made
        // the Arabic read as separated from its transliteration.
        lineHeight = size * lineHeightFactor,
    )
}
