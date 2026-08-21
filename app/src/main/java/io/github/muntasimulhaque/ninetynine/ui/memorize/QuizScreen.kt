package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import io.github.muntasimulhaque.ninetynine.util.QuizBuilder
import io.github.muntasimulhaque.ninetynine.util.QuizQuestion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Session state for one quiz round; survives rotation with the ViewModel. */
class QuizViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    var questions by mutableStateOf<List<QuizQuestion>>(restoredQuestions()); private set
    var index by mutableIntStateOf(savedState.get<Int>(KEY_INDEX) ?: 0); private set
    var score by mutableIntStateOf(savedState.get<Int>(KEY_SCORE) ?: 0); private set
    var selected by mutableIntStateOf(savedState.get<Int>(KEY_SELECTED) ?: -1); private set
    var finished by mutableStateOf(savedState.get<Boolean>(KEY_FINISHED) ?: false); private set

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
        savedState[KEY_FINISHED] = finished
        savedState[KEY_MISSED] = missed.toIntArray()
    }

    fun ensureQuiz(names: List<Name>, learned: Set<Int>) {
        if (questions.isEmpty() && names.isNotEmpty()) {
            questions = QuizBuilder.build(names, preferred = learned)
            saveSession()
        }
    }

    /** Returns true when the tapped option is the correct answer. */
    fun select(optionIndex: Int): Boolean {
        if (selected != -1) return false
        selected = optionIndex
        val correct = optionIndex == questions[index].answerIndex
        if (correct) score++ else missed = missed + questions[index].number
        saveSession()
        return correct
    }

    fun next() {
        if (index < questions.lastIndex) {
            index++
            selected = -1
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
        finished = false
        missed = emptyList()
        saveSession()
    }

    override fun onCleared() {
        savedState.remove<String>(KEY_QUESTIONS)
        savedState.remove<Int>(KEY_INDEX)
        savedState.remove<Int>(KEY_SCORE)
        savedState.remove<Int>(KEY_SELECTED)
        savedState.remove<Boolean>(KEY_FINISHED)
        savedState.remove<IntArray>(KEY_MISSED)
    }

    private companion object {
        const val KEY_QUESTIONS = "quiz.questions"
        const val KEY_INDEX = "quiz.index"
        const val KEY_SCORE = "quiz.score"
        const val KEY_SELECTED = "quiz.selected"
        const val KEY_FINISHED = "quiz.finished"
        const val KEY_MISSED = "quiz.missed"
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
        if (quiz.finished) viewModel.setQuizBest(quiz.score)
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
    val question = quiz.questions[quiz.index]
    val name = names.firstOrNull { it.number == question.number } ?: return
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
                            text = name.arabic,
                            fontSize = ArabicSize.Panel,
                            color = HeroGold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        FitText(
                            text = name.transliteration,
                            style = MaterialTheme.typography.displaySmall,
                            color = HeroText,
                            minScale = 0.45f,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                question.options.forEachIndexed { optionIndex, option ->
                    OptionButton(
                        text = option,
                        state = when {
                            quiz.selected == -1 -> OptionState.IDLE
                            optionIndex == question.answerIndex -> OptionState.CORRECT
                            optionIndex == quiz.selected -> OptionState.WRONG
                            else -> OptionState.DIMMED
                        },
                        onClick = {
                            val wasUnanswered = quiz.selected == -1
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
                if (quiz.selected != -1) {
                    val verdict = if (quiz.selected == question.answerIndex) {
                        stringResource(R.string.quiz_answer_correct)
                    } else {
                        stringResource(
                            R.string.quiz_answer_wrong,
                            question.options[question.answerIndex],
                        )
                    }
                    Box(
                        Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            contentDescription = verdict
                        }
                    )
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = quiz::next,
                    enabled = quiz.selected != -1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
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
        Text(
            text = stringResource(R.string.quiz_score_format, score, total),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
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
