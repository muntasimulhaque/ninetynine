package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.util.QuizBuilder
import io.github.muntasimulhaque.ninetynine.util.QuizQuestion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Session state for one quiz round; survives rotation with the ViewModel. */
class QuizViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    var questions by mutableStateOf<List<QuizQuestion>>(restoredQuestions()); private set
    var index by mutableIntStateOf(
        (savedState.get<Int>(KEY_INDEX) ?: 0).coerceAtLeast(0)
    ); private set
    var score by mutableIntStateOf(
        (savedState.get<Int>(KEY_SCORE) ?: 0).coerceIn(0, 99)
    ); private set
    var selected by mutableIntStateOf(savedState.get<Int>(KEY_SELECTED) ?: -1); private set

    /**
     * The question index [selected] was made on.
     *
     * [next] deliberately does not clear the selection: the outgoing question
     * keeps its answered state for the length of its turn-away animation, so
     * the green verdict never blinks off mid-fade. What any question reads as
     * ITS answer is derived: see [chosenFor].
     */
    var selectedAt by mutableIntStateOf(savedState.get<Int>(KEY_SELECTED_AT) ?: -1); private set
    var finished by mutableStateOf(savedState.get<Boolean>(KEY_FINISHED) ?: false); private set

    /**
     * True once this sitting's round has been built from a settled input.
     *
     * An empty [questions] list means two very different things before that:
     * "the build has not happened yet" and "the bundled asset could not be
     * read". Mapping the first onto the second flashed the failure message on
     * every entry to this screen (the build runs in a LaunchedEffect, one
     * frame after the first composition), and it is also what keeps the
     * question pager from ever indexing an empty round: the screen renders
     * nothing at all until this turns true (see [QuizScreen]). Rides the
     * SavedStateHandle with the rest of the round, so a process death on the
     * result page does not replay the flash.
     */
    var ready by mutableStateOf(savedState.get<Boolean>(KEY_READY) ?: false); private set

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
     * way to find out which four: the information existed a second earlier and
     * was thrown away. A book of exercises has an answers page.
     */
    var missed by mutableStateOf<List<Int>>(
        savedState.get<IntArray>(KEY_MISSED)?.toList()?.filter { it in 1..99 } ?: emptyList()
    ); private set

    /**
     * The round lives in the SavedStateHandle, not just memory: a process
     * death mid-quiz restores the exact question, score and missed list, the
     * same way the name pager restores its page. The keys are dropped in
     * [onCleared], so leaving the screen starts the next sitting fresh.
     *
     * Restored questions are validated: the bundle is trusted storage, but a
     * corrupted write must read as "no round" and rebuild, never as an
     * out-of-bounds answerIndex that crashes the verdict line.
     */
    private fun restoredQuestions(): List<QuizQuestion> {
        val raw = savedState.get<String>(KEY_QUESTIONS) ?: return emptyList()
        val decoded = try {
            json.decodeFromString<List<QuizQuestion>>(raw)
        } catch (_: Exception) {
            return emptyList()
        }
        if (decoded.size > QuizBuilder.DEFAULT_COUNT + 10) return emptyList()
        if (decoded.any { q ->
                q.number !in 1..99 || q.options.size < 2 ||
                    q.answerIndex !in q.options.indices ||
                    q.options.any { it.isBlank() }
            }
        ) return emptyList()
        return decoded
    }

    private fun saveSession() {
        savedState[KEY_QUESTIONS] = json.encodeToString(questions)
        savedState[KEY_INDEX] = index
        savedState[KEY_SCORE] = score
        savedState[KEY_SELECTED] = selected
        savedState[KEY_SELECTED_AT] = selectedAt
        savedState[KEY_FINISHED] = finished
        savedState[KEY_MISSED] = missed.toIntArray()
        savedState[KEY_BEST_BEFORE] = bestBefore
        savedState[KEY_READY] = ready
    }

    /**
     * Records the standing best exactly once, and only for a finished round:
     * the caller's effect re-runs on every rotation while the result page is
     * up, by which time the round's own write may already have raised the
     * stored best: capturing again would compare the score against itself
     * and the "new best" moment would silently never fire.
     */
    fun noteBestBefore(currentBest: Int) {
        if (finished && bestBefore == BEST_BEFORE_UNSEEN) {
            bestBefore = currentBest
            saveSession()
        }
    }

    /**
     * Builds the round once the input has settled.
     *
     * [namesLoaded] is what makes an empty round unambiguous: the build runs
     * in a LaunchedEffect, a frame after the first composition, and until it
     * lands an empty list means "not built yet": never "the asset failed",
     * and never something the pager may index.
     */
    fun ensureQuiz(namesLoaded: Boolean, names: List<Name>, learned: Set<Int>) {
        // Nothing is decided until the asset read has finished: building from
        // an empty list would look exactly like a failed read.
        if (!namesLoaded) return
        if (index !in 0..maxOf(0, questions.lastIndex)) {
            index = 0
            selected = -1
            selectedAt = -1
        }
        if (questions.isEmpty() && names.isNotEmpty()) {
            questions = QuizBuilder.build(names, preferred = learned)
        }
        // Settled either way: with no names to draw from, the screen's own
        // message takes over; otherwise the round above is now live.
        ready = true
        saveSession()
    }

    /**
     * The selection as question [questionIndex] sees it: -1 when unanswered.
     * A stale selection from an earlier question never leaks into a later
     * one, because the tag no longer matches.
     */
    fun chosenFor(questionIndex: Int): Int = if (selectedAt == questionIndex) selected else -1

    /** Returns true when the tapped option is the correct answer. */
    fun select(optionIndex: Int): Boolean {
        val question = questions.getOrNull(index) ?: return false
        if (optionIndex !in question.options.indices) return false
        if (chosenFor(index) != -1) return false
        selected = optionIndex
        selectedAt = index
        val correct = optionIndex == question.answerIndex
        if (correct) score++ else missed = missed + question.number
        saveSession()
        return correct
    }

    fun next() {
        val last = questions.lastIndex
        if (last < 0) {
            finished = true
        } else if (index < last) {
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
        ready = true
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
        savedState.remove<Boolean>(KEY_READY)
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
        const val KEY_READY = "quiz.ready"
        val json = Json
    }
}
