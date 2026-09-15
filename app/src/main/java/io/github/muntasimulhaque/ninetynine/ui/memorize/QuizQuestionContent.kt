package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.HairlineProgress
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics

@Composable
internal fun QuizQuestionContent(
    quiz: QuizViewModel,
    names: List<Name>,
) {
    val haptics = rememberHaptics()

    // Same footer pattern as a name page: the answer button anchors just above
    // the system bar when the question is short, and scrolls when it is not.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minPageHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.defaultMinSize(minHeight = minPageHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HairlineProgress(
                    progress = (quiz.index + 1) / quiz.questions.size.toFloat(),
                )
                Spacer(Modifier.height(20.dp))
                // Questions TURN like pages instead of cutting: each new one
                // rises gently into place while the last fades away, exactly
                // how a pushed screen arrives everywhere else in the app. The
                // frame around them (progress hairline, Next button) never
                // moves. The outgoing question keeps its verdict through the
                // turn: [QuizViewModel.chosenFor] still answers for the index
                // it left, so the green highlight fades with its card instead
                // of blinking off a frame before.
                val motionScale = LocalMotionScale.current
                AnimatedContent(
                    targetState = quiz.index,
                    transitionSpec = {
                        (fadeIn(Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle)) +
                            slideInVertically(
                                Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle),
                            ) { it / 12 })
                            .togetherWith(fadeOut(Motion.spec(motionScale, Motion.QUICK)))
                    },
                    label = "quizTurn",
                ) { index ->
                    // Read back safely: the outgoing copy of this branch can
                    // outlive its questions for the length of the turn.
                    val turnQuestion = quiz.questions.getOrNull(index)
                        ?: return@AnimatedContent
                    val turnName = names.firstOrNull { it.number == turnQuestion.number }
                    if (turnName != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = HeroContainer),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    ArabicText(
                                        text = turnName.arabic,
                                        fontSize = ArabicSize.Panel,
                                        color = HeroGold,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(scaledGap(6.dp)))
                                    FitText(
                                        text = turnName.transliteration,
                                        style = MaterialTheme.typography.displaySmall,
                                        color = HeroText,
                                        minScale = 0.45f,
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            val chosen = quiz.chosenFor(index)
                            turnQuestion.options.forEachIndexed { optionIndex, option ->
                                OptionButton(
                                    text = option,
                                    state = when {
                                        chosen == -1 -> OptionState.IDLE
                                        optionIndex == turnQuestion.answerIndex -> OptionState.CORRECT
                                        optionIndex == chosen -> OptionState.WRONG
                                        else -> OptionState.DIMMED
                                    },
                                    onClick = {
                                        val wasUnanswered = quiz.chosenFor(index) == -1
                                        val correct = quiz.select(optionIndex)
                                        if (wasUnanswered) {
                                            if (correct) haptics.confirm() else haptics.reject()
                                        } else {
                                            // Already answered: the option deliberately
                                            // stays enabled for accessibility, so a sighted
                                            // tap on another option must not be dead air.
                                            haptics.tick()
                                        }
                                    },
                                )
                                Spacer(Modifier.height(10.dp))
                            }

                            // The green fill tells a sighted reader which answer was right.
                            // A screen reader was told nothing: `stateDescription` sits on
                            // each option, so the one the reader TAPPED re-announces
                            // ("Wrong answer") because it holds focus, while the option
                            // that turns green is a different, unfocused node and stays
                            // silent. Being told you are wrong and never told the answer
                            // defeats the point of a quiz. An empty, zero-height live
                            // region carries it without putting anything on screen.
                            // Composed only once an answer exists: an assertive region
                            // that enters composition empty makes some TalkBack versions
                            // announce (or clear) it before the real verdict arrives.
                            if (chosen != -1) {
                                val verdict = if (chosen == turnQuestion.answerIndex) {
                                    stringResource(R.string.quiz_answer_correct)
                                } else {
                                    stringResource(
                                        R.string.quiz_answer_wrong,
                                        turnQuestion.options[turnQuestion.answerIndex],
                                    )
                                }
                                Box(
                                    Modifier.semantics {
                                        liveRegion = LiveRegionMode.Assertive
                                        contentDescription = verdict
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = quiz::next,
                    enabled = quiz.chosenFor(quiz.index) != -1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    // Disabled as a quiet ghost of itself, not mud: the scheme's
                    // own containers instead of Material's 12% ink wash, which
                    // sat like a dirty band under the four crisp options.
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        stringResource(
                            if (quiz.index == quiz.questions.lastIndex) R.string.see_result
                            else R.string.next
                        )
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private enum class OptionState { IDLE, CORRECT, WRONG, DIMMED }

@Composable
private fun OptionButton(
    text: String,
    state: OptionState,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = when (state) {
            OptionState.CORRECT -> colors.primaryContainer
            OptionState.WRONG -> colors.errorContainer
            else -> colors.surface
        },
        animationSpec = Motion.tween(Motion.QUICK),
        label = "optionContainer",
    )
    val (content, border) = when (state) {
        OptionState.IDLE -> colors.onSurface to colors.outline
        OptionState.CORRECT -> colors.onPrimaryContainer to colors.primary
        OptionState.WRONG -> colors.onErrorContainer to colors.error
        // Quiet, not unreadable. These options stay deliberately enabled (see
        // below) and on screen, so they are content, WCAG's inactive-component
        // exemption does not apply. At 45% alpha the text was 2.84:1 and the
        // border 1.68:1, which is unreadable in sunlight and to anyone with low
        // vision, exactly when a reader most wants to compare the answers.
        // onSurfaceVariant is 5.79:1 and reads as quiet without disappearing.
        OptionState.DIMMED -> colors.onSurfaceVariant to colors.outline
    }
    val stateCd = when (state) {
        OptionState.CORRECT -> stringResource(R.string.cd_correct)
        OptionState.WRONG -> stringResource(R.string.cd_wrong)
        else -> null
    }
    Surface(
        onClick = onClick,
        // Stays enabled after answering, select() already ignores the second
        // tap, and a disabled Surface would have the correct answer announced
        // as unavailable. stateDescription appends to the option's own text
        // instead of replacing it, the way contentDescription did.
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (stateCd != null) Modifier.semantics { stateDescription = stateCd }
                else Modifier
            ),
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(if (state == OptionState.IDLE) 1.dp else 1.5.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = content,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(
                visible = state == OptionState.CORRECT || state == OptionState.WRONG,
                enter = fadeIn(Motion.tween(Motion.QUICK)) +
                    scaleIn(Motion.lively(), initialScale = 0.4f),
            ) {
                when (state) {
                    OptionState.CORRECT -> Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = colors.primary,
                    )
                    OptionState.WRONG -> Icon(
                        Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = colors.error,
                    )
                    else -> Unit
                }
            }
        }
    }
}
