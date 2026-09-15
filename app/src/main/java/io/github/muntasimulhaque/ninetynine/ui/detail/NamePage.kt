package io.github.muntasimulhaque.ninetynine.ui.detail

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalDarkTheme
import io.github.muntasimulhaque.ninetynine.ui.theme.NameGoldDark
import io.github.muntasimulhaque.ninetynine.ui.theme.NameGoldLight
import io.github.muntasimulhaque.ninetynine.ui.theme.TransliterationTealDark
import io.github.muntasimulhaque.ninetynine.ui.theme.TransliterationTealLight
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.MixedText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ReadingInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.readingMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SectionLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScrollbarThumb

@Composable
internal fun NamePage(
    name: Name,
    pagerState: PagerState,
    page: Int,
    // The floating capsule overlays this page, so the scroll extent grows by
    // the plate's measured height: on a long meaning the tail can lift fully
    // above the plate, while a page shorter than the screen still does not
    // scroll at all (the spacer lives inside the min-height column).
    bottomClearance: Dp,
    modifier: Modifier = Modifier,
) {
    // Single scrollable page: the keep-acts live in the floating capsule
    // below (DetailNavPlate), and the page grows its scroll extent by that
    // plate's measured clearance so its tail can lift above it. A page
    // shorter than the screen does not scroll at all.
    // The page keeps the book's measure on wide screens, the Name, the
    // meaning, the note and the footer all hold `readingMeasure`'s column,
    // and the thumb hugs that column's edge. Phones never reach the cap.
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = readingMeasure())
    ) {
        val minPageHeight = maxHeight
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = ReadingInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.defaultMinSize(minHeight = minPageHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(30.dp))
                // The transliteration belongs to the Name: 8dp is the share
                // card's pairing (50sp Arabic), one step from the hero's 6dp
                // (48sp); this 52sp page sits in the 8dp family. The Arabic
                // line box at 1.60 leading no longer adds ~5sp of empty
                // descent air on top of the spacer, so the pair reads as one
                // unit while the meaning below keeps its clear step.
                //
                // A clear step below the Arabic, set in the same displaySmall
                // slot the share card, hero and flashcard faces use, the Name
                // leads its transliteration at the same ratio everywhere. FitText
                // keeps the proper noun whole: a Name split across lines reads
                // as two words, and this page must survive a large system font
                // the same way the hero card does.
                //
                // The Name and its transliteration are one selectable unit,
                // long-press copies either whole, exactly as the meaning below
                // copies. A name worth keeping travels further than the page,
                // and every serious reading surface answers a held finger with
                // selection. (The flashcard faces stay swipe surfaces on
                // purpose; the note below rides with its meaning rather than
                // joining the selection trio.)
                SelectionContainer {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ArabicText(
                            text = name.arabic,
                            fontSize = ArabicSize.Page,
                            // The Name wears the app's gold, as close to the hero
                            // plates' #D4B45A as paper contrast allows at large-text
                            // 3:1 (see NameGoldLight). Theme-aware: the warmed gold
                            // on paper, the brighter gold on the night page. It stands
                            // apart from the teal transliteration beneath it.
                            color = if (LocalDarkTheme.current) NameGoldDark else NameGoldLight,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(scaledGap(8.dp)))
                        FitText(
                            text = name.transliteration,
                            style = MaterialTheme.typography.displaySmall.copy(
                                textAlign = TextAlign.Center,
                            ),
                            // The transliteration is set apart from the meaning
                            // by its teal as well as its size, the meaning below
                            // stays in the page's ink. Theme-aware: dark ink on
                            // light paper, pale mint on the night page.
                            color = if (LocalDarkTheme.current) TransliterationTealDark
                            else TransliterationTealLight,
                            minScale = 0.45f,
                        )
                    }
                }
                Spacer(Modifier.height(scaledGap(20.dp)))
                // Long-press copies: a meaning worth keeping travels further
                // than the page, alongside the Name's own selectable unit
                // above, and every serious reading surface answers a held
                // finger with selection. The flashcard faces stay swipe
                // surfaces on purpose.
                SelectionContainer {
                    Text(
                        text = name.meaning,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = readingMeasure()),
                    )
                }
                if (name.note != null) {
                    // One step down from the meaning's 20: the note is an
                    // annex of the meaning, not its sibling (at 26 the two
                    // perceived gaps were statistically identical).
                    Spacer(Modifier.height(scaledGap(24.dp)))
                    Column(
                        modifier = Modifier.widthIn(max = readingMeasure()),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        // SectionLabel, not a bare Text: the identical styling,
                        // plus the heading semantics every other overline
                        // carries, heading navigation anchors on the note too.
                        SectionLabel(stringResource(R.string.note_label))
                        Spacer(Modifier.height(scaledGap(8.dp)))
                        MixedText(
                            text = name.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // The scroll-under room: this is inside the min-height
                // column, so short pages absorb it in the weighted spacer
                // (no phantom scroll) while long pages gain exactly the
                // extent needed to clear the plate, the same +16dp of air
                // the list screens leave above their bar.
                Spacer(Modifier.height(bottomClearance + 16.dp))
            }
        }

        // A quiet scrollbar thumb on the page's right edge, the platform's
        // own signal for "more of the meaning lies below", with its size
        // telling a reader at a glance how many screens the page runs to.
        // Only present while it does lie below.
        ScrollbarThumb(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, bottom = 16.dp + bottomClearance, end = 8.dp),
        )
    }
}
