package io.github.muntasimulhaque.ninetynine.ui.memorize

import io.github.muntasimulhaque.ninetynine.data.Name
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizViewModelTest {

    private val names = (1..20).map { n ->
        Name(n, "arabic$n", "Name-$n", "Title $n", "Meaning $n")
    }

    private fun vm() = QuizViewModel(SavedStateHandle())

    @Test
    fun ensureQuizBuildsTenQuestions() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        assertEquals(10, vm.questions.size)
        assertEquals(0, vm.index)
        assertEquals(0, vm.score)
        assertFalse(vm.finished)
    }

    @Test
    fun ensureQuizNoOpsWhenAlreadyBuilt() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        val first = vm.questions
        vm.ensureQuiz(names, learned = setOf(1))
        assertEquals(first, vm.questions)
    }

    @Test
    fun emptyNamesGiveNoQuiz() {
        val vm = vm()
        vm.ensureQuiz(emptyList(), learned = emptySet())
        assertTrue(vm.questions.isEmpty())
    }

    @Test
    fun selectCorrectAnswerIncrementsScore() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        val correct = vm.questions[0].answerIndex
        assertTrue(vm.select(correct))
        assertEquals(1, vm.score)
    }

    @Test
    fun selectWrongAnswerDoesNotIncrementScore() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        val wrong = (vm.questions[0].answerIndex + 1) % 4
        assertFalse(vm.select(wrong))
        assertEquals(0, vm.score)
    }

    @Test
    fun selectRecordsMissedNames() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        val q = vm.questions[0]
        val wrong = (q.answerIndex + 1) % 4
        vm.select(wrong)
        assertEquals(listOf(q.number), vm.missed)
    }

    @Test
    fun doubleSelectIsIgnored() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        val correct = vm.questions[0].answerIndex
        vm.select(correct)
        val secondResult = vm.select((correct + 1) % 4)
        assertFalse(secondResult)
        assertEquals(1, vm.score)
    }

    @Test
    fun nextAdvancesAndNewQuestionReadsUnanswered() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        val firstCorrect = vm.questions[0].answerIndex
        vm.select(firstCorrect)
        assertEquals(firstCorrect, vm.chosenFor(0))
        vm.next()
        assertEquals(1, vm.index)
        // The outgoing question keeps its answer so its turn-away animation
        // still shows the verdict; the incoming one reads as untouched.
        assertEquals(-1, vm.chosenFor(1))
        assertFalse(vm.finished)
    }

    @Test
    fun staleSelectionNeverCountsTwiceOrLeaksForward() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        vm.select(vm.questions[0].answerIndex)
        vm.next()
        // The carried selection is tagged to question 0; answering question 1
        // must be possible exactly once and score exactly one step.
        val wrong = (vm.questions[1].answerIndex + 1) % 4
        assertFalse(vm.select(wrong))
        assertFalse(vm.select(wrong))
        // Score is still only question 0's; the miss is question 1's.
        assertEquals(1, vm.score)
        assertEquals(listOf(vm.questions[1].number), vm.missed)
        assertEquals(-1, vm.chosenFor(2))
    }

    @Test
    fun nextOnLastQuestionSetsFinished() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        repeat(vm.questions.lastIndex) {
            vm.select(vm.questions[vm.index].answerIndex)
            vm.next()
        }
        assertEquals(vm.questions.lastIndex, vm.index)
        assertFalse(vm.finished)
        vm.select(vm.questions[vm.index].answerIndex)
        vm.next()
        assertTrue(vm.finished)
    }

    @Test
    fun perfectRoundHasNoMissed() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        for (i in vm.questions.indices) {
            vm.select(vm.questions[i].answerIndex)
            vm.next()
        }
        assertTrue(vm.finished)
        assertEquals(10, vm.score)
        assertTrue(vm.missed.isEmpty())
    }

    @Test
    fun restartResetsEverything() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        vm.select(vm.questions[0].answerIndex)
        vm.next()
        vm.restart(names, learned = emptySet())
        assertEquals(0, vm.index)
        assertEquals(0, vm.score)
        assertEquals(-1, vm.selected)
        assertEquals(-1, vm.selectedAt)
        assertFalse(vm.finished)
        assertTrue(vm.missed.isEmpty())
        assertEquals(10, vm.questions.size)
        assertEquals(Int.MIN_VALUE, vm.bestBefore)
    }

    @Test
    fun bestBeforeIgnoredWhileRoundStillRunning() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        vm.noteBestBefore(6)
        assertEquals(Int.MIN_VALUE, vm.bestBefore)
    }

    @Test
    fun bestBeforeCapturedOnceOnFinishedRound() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        for (i in vm.questions.indices) {
            vm.select(vm.questions[i].answerIndex)
            vm.next()
        }
        assertTrue(vm.finished)
        vm.noteBestBefore(6)
        assertEquals(6, vm.bestBefore)
        // The caller's effect re-runs on every rotation while the result
        // page is up; the first capture must hold, or the celebration dies.
        vm.noteBestBefore(9)
        assertEquals(6, vm.bestBefore)
    }

    @Test
    fun restartClearsTheBestBeforeCapture() {
        val vm = vm()
        vm.ensureQuiz(names, learned = emptySet())
        for (i in vm.questions.indices) {
            vm.select(vm.questions[i].answerIndex)
            vm.next()
        }
        vm.noteBestBefore(6)
        vm.restart(names, learned = emptySet())
        assertEquals(Int.MIN_VALUE, vm.bestBefore)
    }
}
