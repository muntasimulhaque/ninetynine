package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroSubtext
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.readingMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScrollbarThumb
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
internal fun SwipeFlipCard(
    name: Name,
    flipped: Boolean,
    onFlip: () -> Unit,
    offsetX: Animatable<Float, *>,
    cardWidth: Float,
    onDragCommit: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    // A finger mid-drag is asking a question the card should answer: which
    // verdict is it heading toward? The sign alone flips cheaply, so it is a
    // Boolean state; the graded strength of the hint is read in the draw
    // phase below, where per-frame changes cost no recomposition.
    var dragging by remember(offsetX) { mutableStateOf(false) }
    val dragKnow by remember(offsetX) { derivedStateOf { offsetX.value >= 0f } }

    // Each card arrives with a soft rise (keyed to the card's own offset state).
    val appear = remember(offsetX) { Animatable(0f) }
    val motionScale = LocalMotionScale.current
    LaunchedEffect(offsetX) {
        appear.animateTo(1f, Motion.spec(motionScale, Motion.GENTLE, Motion.Settle))
    }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = Motion.tween(Motion.GENTLE),
        label = "flip",
    )

    // The card is one merged node (`clickable` merges its descendants) so
    // flipping it swaps the text in place and emits only a content-changed
    // event, which TalkBack does not speak. Without a live region the whole
    // memorisation loop is silent: you tap to reveal the meaning and hear
    // nothing, then press "I know it" and cannot tell what happened or which
    // name is now in front of you.
    val faceLabel = if (flipped) {
        stringResource(R.string.cd_card_back, name.transliteration, name.meaning)
    } else {
        stringResource(R.string.cd_card_front, name.transliteration)
    }
    Card(
        onClick = onFlip,
        modifier = modifier
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = faceLabel
            }
            .graphicsLayer {
                val w = size.width.coerceAtLeast(1f)
                val leaving = offsetX.value / (w * 1.2f)
                translationX = offsetX.value
                rotationZ = (offsetX.value / w) * 8f
                alpha = (appear.value * (1f - leaving * leaving)).coerceIn(0f, 1f)
                scaleX = 0.96f + 0.04f * appear.value
                scaleY = 0.96f + 0.04f * appear.value
                translationY = (1f - appear.value) * 24.dp.toPx()
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .pointerInput(offsetX) {
                var crossedThreshold = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = true
                        crossedThreshold = false
                    },
                    onDragEnd = {
                        dragging = false
                        val threshold = size.width * 0.3f
                        when {
                            offsetX.value > threshold -> onDragCommit(true)
                            offsetX.value < -threshold -> onDragCommit(false)
                            else -> scope.launch { offsetX.animateTo(0f, Motion.softSpec(motionScale)) }
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch { offsetX.animateTo(0f, Motion.softSpec(motionScale)) }
                    },
                ) { change, amount ->
                    change.consume()
                    val next = offsetX.value + amount
                    scope.launch { offsetX.snapTo(next) }
                    // One featherweight tick the instant the drag crosses the
                    // commit threshold, the finger hears the point of no
                    // return, so releasing past it stops being a guess.
                    if (!crossedThreshold && next.absoluteValue >= size.width * 0.3f) {
                        crossedThreshold = true
                        haptics.tick()
                    }
                }
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (rotation <= 90f) HeroContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (rotation <= 90f) null
        // `outline`: this border is the flipped card's entire boundary, and its
        // fill is only 1.06:1 against the page. At outlineVariant's 1.42:1 the
        // card lost its edge completely on flip; it went from a clearly
        // bounded emerald object to a shape with no perceivable outline.
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                // Front: the name itself, set like the share card. Scrollable like
                // the back: in landscape, or at a large system font, the card's
                // height can drop below what the name needs, and the Card clips to
                // its rounded shape, the one place a supported configuration
                // could otherwise lose the Name entirely.
                val frontScroll = rememberScrollState()
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(frontScroll)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ArabicText(
                            text = name.arabic,
                            fontSize = ArabicSize.Panel,
                            color = HeroGold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(scaledGap(8.dp)))
                        FitText(
                            text = name.transliteration,
                            style = MaterialTheme.typography.displaySmall,
                            color = HeroText,
                            minScale = 0.45f,
                        )
                        // No instruction line: the whole face is one plate
                        // holding one Name, and tapping is the only thing a
                        // finger can do with it.
                    }
                    // Same right-edge thumb as the back: only present when the
                    // name overflows the card and needs scrolling.
                    ScrollbarThumb(
                        scrollState = frontScroll,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 20.dp, bottom = 20.dp, end = 10.dp),
                    )
                }
            } else {
                // Back: the meaning alone (counter-rotated so it reads correctly).
                val backScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(backScroll)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // Centred like the share card, the reading line, set the
                        // same way on every surface that carries the full meaning.
                        Text(
                            text = name.meaning,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = readingMeasure()),
                        )
                    }
                    // A quiet scrollbar thumb on the card's right edge: position
                    // says where you are, size says how long the meaning runs.
                    // Only there while more lies below.
                    ScrollbarThumb(
                        scrollState = backScroll,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 20.dp, bottom = 20.dp, end = 10.dp),
                    )
                }
            }
            // The verdict the drag is heading toward, fading in as the finger
            // approaches the commit threshold: the same words the two buttons
            // beneath the card carry, set as a tracked overline at the card's
            // head. Composed only while a drag is live, an invisible merged
            // child would still reach TalkBack through the card's merged node,
            // and its graded alpha is read in the draw phase, so a moving
            // finger redraws without recomposing the faces at all.
            if (dragging) {
                val front = rotation <= 90f
                Text(
                    text = stringResource(
                        if (dragKnow) R.string.i_know_it else R.string.still_learning
                    ).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        dragKnow && front -> HeroGold
                        dragKnow -> MaterialTheme.colorScheme.secondary
                        front -> HeroSubtext
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp)
                        .graphicsLayer {
                            val fraction = if (cardWidth <= 0f) 0f
                            else (offsetX.value / (cardWidth * 0.3f)).coerceIn(-1f, 1f)
                            alpha = ((fraction.absoluteValue - 0.15f) / 0.85f)
                                .coerceIn(0f, 1f)
                        },
                )
            }
        }
    }
}
