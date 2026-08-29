package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.HairlineProgress
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NavRow
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageRule
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SectionLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageMessage
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SettleOnce
import io.github.muntasimulhaque.ninetynine.ui.theme.components.pageMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import io.github.muntasimulhaque.ninetynine.util.QuizBuilder
import io.github.muntasimulhaque.ninetynine.util.QuizQuestion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/** Session state for one quiz round; survives rotation with the ViewModel. */
class QuizViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    var questions by mutableStateOf<List<QuizQuestion>>(restoredQuestions()); private set
    var index by mutableIntStateOf(savedState.get<Int>(KEY_INDEX) ?: 0); private set
    var score by mutableIntStateOf(savedState.get<Int>(KEY_SCORE) ?: 0); private set
    var selected by mutableIntStateOf(savedState.get<Int>(KEY_SELECTED) ?: -1); private set

    /**
     * The question index [selected] was made on.
     *
     * [next] deliberately does not clear the selection: the outgoing question
     * keeps its answered state for the length of its turn-away animation, so
     * the green verdict never blinks off mid-fade. What any question reads as
     * ITS answer is derived — see [chosenFor].
     */
    var selectedAt by mutableIntStateOf(savedState.get<Int>(KEY_SELECTED_AT) ?: -1); private set
    var finished by mutableStateOf(savedState.get<Boolean>(KEY_FINISHED) ?: false); private set

    /**
     * The standing best at the instant this round finished.
     *
     * Captured once, when [finished] turns true (see [noteBestBefore]), so
     * the result page knows whether the score it shows beat something. The
     * sentinel means "no round has finished in this sitting". Rides in the
     * SavedStateHandle with the rest of the round: a process death on the
     * result page must not demote a celebration, and a rotation must not
     * re-capture from a best the round has already raised.
     */
    var bestBefore by mutableIntStateOf(savedState.get<Int>(KEY_BEST_BEFORE) ?: BEST_BEFORE_UNSEEN); private set

    /**
     * The names answered wrongly, in the order they came up.
     *
     * The round used to keep only a score, so a reader saw "4 / 10" and had no
     * way to find out which four — the information existed a second earlier and
     * was thrown away. A book of exercises has an answers page.
     */
    var missed by mutableStateOf<List<Int>>(savedState.get<IntArray>(KEY_MISSED)?.toList() ?: emptyList()); private set

    /**
     * The round lives in the SavedStateHandle, not just memory: a process
     * death mid-quiz restores the exact question, score and missed list, the
     * same way the name pager restores its page. The keys are dropped in
     * [onCleared], so leaving the screen starts the next sitting fresh.
     */
    private fun restoredQuestions(): List<QuizQuestion> =
        savedState.get<String>(KEY_QUESTIONS)?.let {
            runCatching { json.decodeFromString<List<QuizQuestion>>(it) }.getOrNull()
        } ?: emptyList()

    private fun saveSession() {
        savedState[KEY_QUESTIONS] = json.encodeToString(questions)
        savedState[KEY_INDEX] = index
        savedState[KEY_SCORE] = score
        savedState[KEY_SELECTED] = selected
        savedState[KEY_SELECTED_AT] = selectedAt
        savedState[KEY_FINISHED] = finished
        savedState[KEY_MISSED] = missed.toIntArray()
        savedState[KEY_BEST_BEFORE] = bestBefore
    }

    /**
     * Records the standing best exactly once, and only for a finished round:
     * the caller's effect re-runs on every rotation while the result page is
     * up, by which time the round's own write may already have raised the
     * stored best — capturing again would compare the score against itself
     * and the "new best" moment would silently never fire.
     */
    fun noteBestBefore(currentBest: Int) {
        if (finished && bestBefore == BEST_BEFORE_UNSEEN) {
            bestBefore = currentBest
            saveSession()
        }
    }

    fun ensureQuiz(names: List<Name>, learned: Set<Int>) {
        if (questions.isEmpty() && names.isNotEmpty()) {
            questions = QuizBuilder.build(names, preferred = learned)
            saveSession()
        }
    }

    /**
     * The selection as question [questionIndex] sees it: -1 when unanswered.
     * A stale selection from an earlier question never leaks into a later
     * one, because the tag no longer matches.
     */
    fun chosenFor(questionIndex: Int): Int = if (selectedAt == questionIndex) selected else -1

    /** Returns true when the tapped option is the correct answer. */
    fun select(optionIndex: Int): Boolean {
        if (chosenFor(index) != -1) return false
        selected = optionIndex
        selectedAt = index
        val correct = optionIndex == questions[index].answerIndex
        if (correct) score++ else missed = missed + questions[index].number
        saveSession()
        return correct
    }

    fun next() {
        if (index < questions.lastIndex) {
            index++
            // The selection rides along, tagged to the question it answered,
            // so the outgoing turn fades out still showing its verdict.
        } else {
            finished = true
        }
        saveSession()
    }

    fun restart(names: List<Name>, learned: Set<Int>) {
        questions = QuizBuilder.build(names, preferred = learned)
        index = 0
        score = 0
        selected = -1
        selectedAt = -1
        finished = false
        missed = emptyList()
        bestBefore = BEST_BEFORE_UNSEEN
        saveSession()
    }

    override fun onCleared() {
        savedState.remove<String>(KEY_QUESTIONS)
        savedState.remove<Int>(KEY_INDEX)
        savedState.remove<Int>(KEY_SCORE)
        savedState.remove<Int>(KEY_SELECTED)
        savedState.remove<Int>(KEY_SELECTED_AT)
        savedState.remove<Boolean>(KEY_FINISHED)
        savedState.remove<IntArray>(KEY_MISSED)
        savedState.remove<Int>(KEY_BEST_BEFORE)
    }

    private companion object {
        /** [bestBefore] when no round has finished in this sitting. */
        const val BEST_BEFORE_UNSEEN = Int.MIN_VALUE

        const val KEY_QUESTIONS = "quiz.questions"
        const val KEY_INDEX = "quiz.index"
        const val KEY_SCORE = "quiz.score"
        const val KEY_SELECTED = "quiz.selected"
        const val KEY_SELECTED_AT = "quiz.selectedAt"
        const val KEY_FINISHED = "quiz.finished"
        const val KEY_MISSED = "quiz.missed"
        const val KEY_BEST_BEFORE = "quiz.bestBefore"
        val json = Json
    }
}

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

    LaunchedEffect(names, learned, learnedLoaded) {
        if (!learnedLoaded) return@LaunchedEffect
        quiz.ensureQuiz(names, learned)
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
            // "3 of 99" — pushed-screen TITLES sit left (Settings, About);
            // position counters sit centre. One system, no drift.
            CenterAlignedTopAppBar(
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
            when {
                // Until DataStore delivers, an empty question list is "not
                // built yet", not a load failure: the names-unavailable
                // message is alarming, and it told a first-day reader to
                // reinstall for no reason.
                !learnedLoaded -> Unit
                quiz.questions.isEmpty() ->
                    if (namesLoaded) PageMessage(stringResource(R.string.names_unavailable))
                quiz.finished -> QuizResultContent(
                    score = quiz.score,
                    total = quiz.questions.size,
                    best = quizBest,
                    isNewBest = quiz.bestBefore >= 0 && quiz.score > quiz.bestBefore,
                    missed = quiz.missed.mapNotNull { n -> names.firstOrNull { it.number == n } },
                    onRestart = { quiz.restart(names, learned) },
                    onNameClick = onNameClick,
                    onBack = onBack,
                )
                else -> QuizQuestionContent(
                    quiz = quiz,
                    names = names,
                )
            }
        }
    }
}

@Composable
private fun QuizQuestionContent(
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
                // rises gently into place while the last fades away — exactly
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
                    val turnQuestion = quiz.questions[index]
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
                                    Spacer(Modifier.height(6.dp))
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
        // below) and on screen, so they are content — WCAG's inactive-component
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
        // Stays enabled after answering — select() already ignores the second
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

@Composable
private fun QuizResultContent(
    score: Int,
    total: Int,
    best: Int,
    isNewBest: Boolean,
    missed: List<Name>,
    onRestart: () -> Unit,
    onNameClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        // A perfect round earns the app's seal: the square-Kufic mark inside
        // the share card's gold hairline circle, popping in softly — once.
        if (score == total) {
            PerfectSeal()
            Spacer(Modifier.height(20.dp))
        }
        ScoreCount(score = score, total = total)
        // A round that beat the standing best says so, once, quietly — the
        // tracked gold overline the app reserves for what matters. First
        // rounds stay silent: everything beats nothing, and saying so would
        // cheapen the moment a real best falls.
        if (isNewBest) {
            Spacer(Modifier.height(12.dp))
            SectionLabel(stringResource(R.string.quiz_new_best))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                when {
                    score == total -> R.string.quiz_perfect
                    score >= total / 2 -> R.string.quiz_good
                    else -> R.string.quiz_keep_trying
                }
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (best >= 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.quiz_best, maxOf(best, score)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The answers page. A round that only scores you is a measuring
        // instrument; naming what you missed makes it a teaching one, and the
        // rows are the same table-of-contents row Memorize and Settings draw,
        // so nothing new arrives on screen. Empty on a perfect round, which is
        // exactly when the page should stay quiet.
        if (missed.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            SectionLabel(
                text = stringResource(R.string.names_to_revisit),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            missed.forEach { name ->
                NavRow(
                    title = name.transliteration,
                    subtitle = name.title,
                    titleStyle = MaterialTheme.typography.titleMedium,
                    onClickLabel = stringResource(R.string.cd_open_name),
                    onClick = { onNameClick(name.number) },
                )
                PageRule()
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.try_another_round))
        }
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_memorize))
        }
    }
}

/**
 * The score settles like everything else in the app: it counts up once,
 * calmly, instead of appearing already over. Plays once per result —
 * rememberSaveable keeps a rotation from replaying it, the way the name
 * page's entrance fade does not replay.
 */
@Composable
private fun ScoreCount(score: Int, total: Int) {
    var played by rememberSaveable { mutableStateOf(false) }
    val shown = remember { Animatable(0f) }
    val motionScale = LocalMotionScale.current
    LaunchedEffect(score) {
        if (played) {
            shown.snapTo(score.toFloat())
            return@LaunchedEffect
        }
        played = true
        if (motionScale == 0f) shown.snapTo(score.toFloat())
        else shown.animateTo(
            score.toFloat(),
            Motion.spec(motionScale, Motion.CALM, easing = Motion.Settle),
        )
    }
    Text(
        text = stringResource(
            R.string.quiz_score_format,
            shown.value.roundToInt().toString(),
            total.toString(),
        ),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * The share card's maker mark in its hairline gold circle — earned here,
 * not worn. The pop is the same lively spring the bookmark and the learned
 * pill answer with, so the reward speaks the app's own tactile language.
 */
@Composable
private fun PerfectSeal() {
    SettleOnce(fromScale = 0.6f) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mark),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
