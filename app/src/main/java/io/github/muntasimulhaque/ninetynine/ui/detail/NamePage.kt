package io.github.muntasimulhaque.ninetynine.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalDarkTheme
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.NameGoldDark
import io.github.muntasimulhaque.ninetynine.ui.theme.NameGoldLight
import io.github.muntasimulhaque.ninetynine.ui.theme.TransliterationTealDark
import io.github.muntasimulhaque.ninetynine.ui.theme.TransliterationTealLight
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
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
    learned: Boolean,
    onToggleLearned: () -> Unit,
    // The floating capsule overlays this page, so the scroll extent grows by
    // the plate's measured height: on a long meaning the tail can lift fully
    // above the plate, while a page shorter than the screen still does not
    // scroll at all (the spacer lives inside the min-height column).
    bottomClearance: Dp,
    modifier: Modifier = Modifier,
) {
    // Single scrollable page: the floating capsule below is the page-turner,
    // and the page grows its scroll extent by that plate's measured clearance
    // so its tail can lift above it. The learned act closes the entry at the
    // foot; the bookmark lives in the top bar. A page shorter than the screen
    // does not scroll at all.
    // The page keeps the book's measure on wide screens, the Name, the
    // meaning, the note and the learned act all hold `readingMeasure`'s column,
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
                // The entry's closing act. The weight above pushes it to the
                // foot of a short page, just above the page-turner, instead of
                // leaving it stranded in the middle of the air; on a long page
                // the weight collapses and the act follows the text. The
                // bookmark can be set at any moment and stays in the top bar;
                // learning is a verdict on the whole entry, so it sits where
                // the reading ends, with its word visible and its 48dp target
                // in the thumb zone.
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(scaledGap(28.dp)))
                LearnedAction(
                    learned = learned,
                    number = name.number,
                    onToggle = onToggleLearned,
                )
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

/**
 * Learning a name, closing the entry.
 *
 * The page is one scroll container, so this control travels with the meaning;
 * it sits after the text because the judgment is made when the reading ends.
 * When the text ends early, the page's weighted space holds the act just
 * above the page-turner: still at the foot of the entry, not stranded in the
 * middle of the air. The bookmark, the other keep act, stays in the top bar:
 * keeping a page can happen at any moment, while learning is a verdict on the
 * whole entry. The list already carries the learned tick, so the page does
 * not need a fixed readout for it.
 *
 * The glyph carries the state beside the word: an empty ring at rest, the
 * filled gold check once learned. That is the names list's own grammar (the
 * gold tick appears, and nothing else does), and the page must say the same
 * thing: a check reads "done", so an unlearned page that wore one would
 * contradict the list one tap away. The word never changes, a label for the
 * eye rather than a verdict that rewrites itself.
 *
 * The capsule under the finger is the target as well as the feedback: 48dp
 * tall, the waist the bar's tabs and the page-turner plate already wear. It
 * appears only while pressed (the page stays bare until touched), so its
 * geometry has to be honest: clip, clickable and padding share one box, and
 * the padding sits inside the click. The 20dp pill this replaces was a
 * smudge, and its 48dp reservation was dead air outside the click.
 *
 * TalkBack hears the button once ("Mark as learned") and the state after it
 * ("Learned" / "Not learned"), never a label that changes under it. The
 * role is the switch role, the action is the contentDescription and the
 * state is the stateDescription; a toggle action comes from toggleable, not
 * from the role. The visible word is a label, not an unlabeled glyph, so a
 * first-time reader does not have to infer the mark.
 */
@Composable
private fun LearnedAction(
    learned: Boolean,
    number: Int,
    onToggle: () -> Unit,
) {
    val haptics = rememberHaptics()

    // The pop means "you just changed this", so it must not fire when the
    // reader swipes to a page that is already learned. Each page composes its
    // own row, so the latch is local: arriving at a page seeds its state
    // silently, and only a toggle pops.
    val scale = remember { Animatable(1f) }
    val seededFor = remember { mutableStateOf<Int?>(null) }
    val motionScale = LocalMotionScale.current
    LaunchedEffect(number, learned) {
        if (seededFor.value != number) {
            seededFor.value = number
        } else {
            scale.snapTo(0.94f)
            scale.animateTo(1f, Motion.livelySpec(motionScale))
        }
    }

    val state = stringResource(if (learned) R.string.learned else R.string.not_learned)
    // A toggle, not a button that happens to carry state text: the switch
    // role marks it as one, the action lives in the contentDescription and
    // the state in stateDescription, and the bookmark wears the same pair so
    // the two keep acts speak alike. The toggle action itself comes from
    // toggleable, not from the role.
    val cd = stringResource(R.string.mark_learned)
    // clip before clickable, padding inside it: the capsule, the ripple and
    // the hit area are one box. 14dp of vertical padding lifts the 20dp of
    // content to the 48dp control waist; 20dp horizontal keeps the word
    // clear of the semicircular ends.
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Switch) {
                haptics.confirm()
                onToggle()
            }
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .semantics {
                contentDescription = cd
                stateDescription = state
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            // The empty ring, not a check: the check belongs to the learned
            // state, the same filled gold tick the list wears.
            imageVector = if (learned) Icons.Filled.CheckCircle
            else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            // Resting, it wears the page's emerald like the page's other
            // tappable words; learned, it fills with the app's gold.
            tint = if (learned) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.plate_learned),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}
