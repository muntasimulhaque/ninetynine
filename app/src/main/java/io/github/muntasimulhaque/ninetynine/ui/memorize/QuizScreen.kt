package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.pageMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: NamesViewModel,
    onNameClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val quiz: QuizViewModel = viewModel()
    val names by viewModel.names.collectAsStateWithLifecycle()
    val namesLoaded by viewModel.namesLoaded.collectAsStateWithLifecycle()
    val learned by viewModel.learned.collectAsStateWithLifecycle()
    val learnedLoaded by viewModel.learnedLoaded.collectAsStateWithLifecycle()
    val quizBest by viewModel.quizBest.collectAsStateWithLifecycle()
    // Hoisted: the result turn's transitionSpec builds from it (a
    // transitionSpec is not a composable context), exactly like the
    // question-turn's own spec further down.
    val motionScale = LocalMotionScale.current

    LaunchedEffect(names, learned, learnedLoaded, namesLoaded) {
        if (!learnedLoaded) return@LaunchedEffect
        quiz.ensureQuiz(namesLoaded, names, learned)
    }
    LaunchedEffect(quiz.finished) {
        if (quiz.finished) {
            // Capture the standing best BEFORE this round's write lands: the
            // result page celebrates only a score that beat something.
            quiz.noteBestBefore(quizBest)
            viewModel.setQuizBest(quiz.score)
        }
    }

    Scaffold(
        topBar = {
            // A sequence register, centred over the plate like the name page's
            // "3 of 99", pushed-screen TITLES sit left (Settings, About);
            // position counters sit centre. One system, no drift.
            CenterAlignedTopAppBar(
                modifier = Modifier.barMeasure(),
                colors = paperTopBarColors(),
                title = {
                    ScreenLabel(
                        if (!quiz.finished && quiz.questions.isNotEmpty()) {
                            stringResource(
                                R.string.question_x_of_y,
                                quiz.index + 1,
                                quiz.questions.size,
                            )
                        } else {
                            stringResource(R.string.quiz)
                        }
                    )
                },
                navigationIcon = {
                    BackButton(onBack)
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Wide screens keep the book's column: the question card and
                // its options hold page proportions instead of stretching.
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = pageMeasure())
                .padding(padding)
                .padding(horizontal = PageInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The question → result turn rides the house push like every other
            // change of state (question→question turns below, daily card,
            // pushed screens), the one when-block that used to hard-cut.
            // Loading guards stay outside the animation: no entrance of its
            // own for a first frame, motion only where meaning changes.
            when {
                // Blank paper until the round exists. One guard covers both
                // hazards: the round is built a frame after this composition
                // (so an empty list here is "not yet", not "failed"), and
                // nothing below may index a list that has no questions, the
                // pager indexes it directly. [QuizViewModel.ready] is only set
                // once a settled input has been read, so an empty round past
                // this point really does mean the asset could not be read.
                !quiz.ready -> Unit
                quiz.questions.isEmpty() ->
                    PageMessage(stringResource(R.string.names_unavailable))
                else -> AnimatedContent(
                    targetState = quiz.finished,
                    transitionSpec = {
                        (fadeIn(Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle)) +
                            slideInVertically(
                                Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle),
                            ) { it / 12 })
                            .togetherWith(fadeOut(Motion.spec(motionScale, Motion.QUICK)))
                    },
                    label = "quizResult",
                ) { finished ->
                    if (finished) {
                        QuizResultContent(
                            score = quiz.score,
                            total = quiz.questions.size,
                            best = quizBest,
                            isNewBest = quiz.bestBefore >= 0 && quiz.score > quiz.bestBefore,
                            missed = quiz.missed.mapNotNull { n -> names.firstOrNull { it.number == n } },
                            onRestart = { quiz.restart(names, learned) },
                            onNameClick = onNameClick,
                            onBack = onBack,
                        )
                    } else {
                        QuizQuestionContent(
                            quiz = quiz,
                            names = names,
                        )
                    }
                }
            }
        }
    }
}
