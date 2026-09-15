package io.github.muntasimulhaque.ninetynine.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FloatingBar
import io.github.muntasimulhaque.ninetynine.ui.theme.components.tabLabelStyle
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics

/**
 * Learning a name, in the bar.
 *
 * The page is one scroll container, so a footer control travels with the text:
 * on a long meaning it is well below the fold at exactly the moment a name
 * strikes you. The bar does not move, and the learned axis now sits beside the
 * bookmark: two acts of keeping, one place, the same feel.
 *
 * The unfilled check-circle rests in the page's ink; learned, it fills and
 * wears the app's gold: the same filled-and-gold treatment the bookmark has.
 * TalkBack hears the button once ("Mark as learned") and the state after it
 * ("Learned" / "Not learned"), never a label that changes under it.
 */
@Composable
private fun LearnedAction(learned: Boolean, number: Int, onToggle: () -> Unit) {
    val haptics = rememberHaptics()

    // The pop means "you just changed this", so it must not fire when the
    // reader swipes from an unlearned name to a learned one. The button lives
    // in the bar and survives page changes, so the latch is per-name: arriving
    // at a new number adopts its state silently, and only a toggle pops.
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
    // A toggle, not a button that happens to carry state text: Role.Switch
    // gives TalkBack a toggle action and a checked/unchecked reading, matching
    // how the deck menu's checkbox row already behaves.
    //
    // An explicit column, NOT an IconButton: the icon+label pair sits below
    // the centre line, where a 48dp circle's inscribed width is ~44dp, and
    // the IconButton CLIPS to that circle, so the first capture cut LEARNED
    // mid-glyph. Here the press highlight clips to the bar's own stadium
    // register (the same RoundedCornerShape(50) the tab bar clips to, set
    // before clickable), and minimumInteractiveComponentSize keeps the 48dp
    // touch floor. The full action lives in the contentDescription and the
    // state in stateDescription, exactly what the merged IconButton node
    // read before; the visible label is silent to TalkBack.
    val cd = stringResource(R.string.mark_learned)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Switch) {
                haptics.confirm()
                onToggle()
            }
            .padding(horizontal = 10.dp)
            .semantics {
                contentDescription = cd
                stateDescription = state
            },
    ) {
        Icon(
            imageVector = if (learned) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
            contentDescription = null,
            // Resting, it wears the top bar's grey, the same ink the
            // share icon carries, not the page's near-black. Learned, it
            // fills with the app's gold, as before.
            tint = if (learned) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
        Spacer(Modifier.height(2.dp))
        // The eye gets the tab bar's short chrome label, same register,
        // and the same mixed-case voice: chrome you tap reads as words;
        // tracked caps stay with the overlines (owner decision, 1.22).
        // 9sp x the device factor, never the reader's slider. The ear
        // keeps the full action from the button's description, so TalkBack
        // never hears the state word twice.
        FitText(
            text = stringResource(R.string.plate_learned),
            style = tabLabelStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/**
 * Keeping a name, in the bar.
 *
 * The page is one scroll container, so a footer control travels with the text:
 * on a long meaning it is well below the fold at exactly the moment a name
 * strikes you. The bar does not move.
 *
 * The pop and the haptic are shared with [LearnedAction] deliberately: the app
 * has two per-name toggles on two different axes, and they should at least feel
 * like they were made by the same hand.
 */
@Composable
private fun BookmarkAction(bookmarked: Boolean, number: Int, onToggle: () -> Unit) {
    val haptics = rememberHaptics()

    // The pop means "you just changed this", so it must not fire when the
    // reader swipes from an unkept name to a kept one. The button lives in the
    // bar and survives page changes, so the latch is per-name: arriving at a
    // new number adopts its state silently, and only a toggle pops.
    val scale = remember { Animatable(1f) }
    val seededFor = remember { mutableStateOf<Int?>(null) }
    val motionScale = LocalMotionScale.current
    // `bookmarked` is THIS page's membership, so the keys are (page, its
    // state): a DataStore write for a previous page (after a swipe) cannot
    // change the current page's key, and only a toggle on this page pops.
    LaunchedEffect(number, bookmarked) {
        if (seededFor.value != number) {
            seededFor.value = number
        } else {
            scale.snapTo(0.94f)
            scale.animateTo(1f, Motion.livelySpec(motionScale))
        }
    }

    // The button is named once and never changes; what changes is the state
    // announced after it. Set on the button, which is the node TalkBack focuses.
    // Same explicit column as the learned act: the IconButton's 48dp circle
    // clip cut BOOKMARK mid-glyph on the first capture.
    val state = stringResource(if (bookmarked) R.string.bookmarked else R.string.not_bookmarked)
    val cd = stringResource(R.string.cd_bookmark)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Switch) {
                haptics.confirm()
                onToggle()
            }
            .padding(horizontal = 10.dp)
            .semantics {
                contentDescription = cd
                stateDescription = state
            },
    ) {
        Icon(
            imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = null,
            // Same resting grey as the learned act and the share icon;
            // kept, it fills with the app's gold.
            tint = if (bookmarked) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
        Spacer(Modifier.height(2.dp))
        FitText(
            text = stringResource(R.string.plate_bookmark),
            style = tabLabelStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/**
 * The name page's floating capsule: the same [FloatingBar] plate the tab
 * screens float, carrying everything a reader does to a name: previous and
 * next (wearing the neighbour's transliteration, so the bar says what turning
 * the page brings), and the two acts of keeping, learned and bookmarked,
 * adjacent in the centre. Share stays in the top bar: a send-away act reads
 * at the page's edge, and five slots would crowd a 320dp phone.
 *
 * It replaces the footer that used to scroll with the text: on a long
 * meaning the chevrons sat below the fold at exactly the moment a name
 * strikes you. The bar does not move, so turning is always one tap away; its
 * labels change as the pager settles, the same moment the counter above does.
 * The weighted end slots keep the two keep-acts centred whether the
 * neighbours exist (page 0 has no previous; the last page no next).
 *
 * It is an OVERLAY on the pager, not a Scaffold bottom bar: a reserved slot
 * clips the page's text at the plate's top edge, and a floating sheet with
 * nothing passing beneath it is just a panel. The caller measures this
 * composable's height and hands it to [NamePage] as the clearance its tail
 * scrolls above: the same trick the tab bar uses in MainActivity.
 */
@Composable
internal fun DetailNavPlate(
    modifier: Modifier = Modifier,
    current: Name,
    previousLabel: String?,
    nextLabel: String?,
    learned: Boolean,
    bookmarked: Boolean,
    onToggleLearned: () -> Unit,
    onToggleBookmarked: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    FloatingBar(modifier = modifier) {
        Row(
            // The row takes its height from its children with the same 54dp
            // floor the tab bar's slots wear, so the two capsules read as one
            // register. Each weighted slot caps its label at the space it
            // owns, so two long transliterations can never overlap at any
            // font scale, the labels shrink through FitText instead of
            // wrapping mid-word, and the 20dp chevron survives every name.
            modifier = Modifier
                .barMeasure()
                .heightIn(min = 54.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (previousLabel != null) {
                    // The chevron carries the direction visually; the name
                    // alone would leave a screen-reader user unable to tell
                    // previous from next. (Computed here: a semantics block
                    // is not a composable context.)
                    val previousCd = stringResource(R.string.previous_name, previousLabel)
                    TextButton(
                        onClick = onPrevious,
                        // TextButton's default 16dp horizontal padding ate a
                        // quarter of the weighted slot; the slot's own weight
                        // keeps the tap honest with far less.
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.semantics {
                            contentDescription = previousCd
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            // The tab bar's icon register; the arrow survives
                            // every name and every font scale.
                            modifier = Modifier.size(20.dp),
                        )
                        // Roman, not italic. Italic means epithet, gloss or
                        // quote everywhere else in the app (the page above
                        // has just taught the reader that), so setting a
                        // Name in it says the wrong thing. titleSmall also
                        // rescues these from TextButton's labelLarge, which
                        // made the app's main keep-reading affordance the
                        // smallest Latin on the page. FitText, not Ellipsis:
                        // a Divine Name must never cut off, so the longest
                        // transliterations (Al-Muta'aalee, Al-Mutakabbir,
                        // Al-Mu'akhkhir) shrink a little instead of losing
                        // their tail. TextButton's own primary carries the
                        // colour, as the bare Text did before.
                        // weight(1f, fill = false) on each FitText keeps the
                        // pair symmetric about the Row's measurement order: a
                        // weighted child is measured after the fixed ones, so
                        // the text is fitted to the space LEFT AFTER its own
                        // chevron. Without it the NEXT button's FitText was
                        // measured first, saw the whole slot, declined to
                        // shrink, and the chevron then overflowed the slot and
                        // was cut off at any decent font scale, while the
                        // PREVIOUS button (icon first) always fitted.
                        FitText(
                            text = previousLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f, fill = false),
                            // The centre keep-acts crowd the end slots at large
                            // system font scales, so the longest transliteration
                            // needs more headroom than the 0.55 default: 0.4
                            // keeps the worst case at the same effective size
                            // the bottom bar's labels guarantee (~9sp at a
                            // 2.0 scale). A Divine Name never loses its tail.
                            minScale = 0.4f,
                        )
                    }
                }
            }
            LearnedAction(
                learned = learned,
                number = current.number,
                onToggle = onToggleLearned,
            )
            BookmarkAction(
                bookmarked = bookmarked,
                number = current.number,
                onToggle = onToggleBookmarked,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (nextLabel != null) {
                    val nextCd = stringResource(R.string.next_name, nextLabel)
                    TextButton(
                        onClick = onNext,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.semantics {
                            contentDescription = nextCd
                        },
                    ) {
                        FitText(
                            text = nextLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f, fill = false),
                            minScale = 0.4f,
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
