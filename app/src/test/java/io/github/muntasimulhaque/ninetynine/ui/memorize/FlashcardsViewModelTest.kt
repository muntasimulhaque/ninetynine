package io.github.muntasimulhaque.ninetynine.ui.memorize

import io.github.muntasimulhaque.ninetynine.data.Name
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashcardsViewModelTest {

    private val names = (1..6).map { n ->
        Name(n, "arabic$n", "Name-$n", "Title $n", "Meaning $n")
    }

    private fun vm() = FlashcardsViewModel(SavedStateHandle())

    @Test
    fun ensureDeckBuildsFromNames() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        assertEquals(6, vm.deck.size)
        assertEquals(0, vm.index)
        assertFalse(vm.flipped)
        assertFalse(vm.done)
    }

    @Test
    fun ensureDeckExcludesLearnedByDefault() {
        val vm = vm()
        vm.ensureDeck(names, learned = setOf(2, 5), includeLearned = false)
        assertEquals(4, vm.deck.size)
        assertFalse(2 in vm.deck)
        assertFalse(5 in vm.deck)
    }

    @Test
    fun ensureDeckNoOpsWhenAlreadyBuilt() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        val first = vm.deck
        vm.ensureDeck(names, learned = setOf(1), includeLearned = false)
        assertEquals(first, vm.deck)
    }

    @Test
    fun ensureDeckRebuildsWhenIncludeLearnedChanges() {
        val vm = vm()
        vm.ensureDeck(names, learned = setOf(2), includeLearned = false)
        assertEquals(5, vm.deck.size)
        vm.ensureDeck(names, learned = setOf(2), includeLearned = true)
        assertEquals(6, vm.deck.size)
    }

    @Test
    fun emptyNamesGiveNoDeck() {
        val vm = vm()
        vm.ensureDeck(emptyList(), learned = emptySet(), includeLearned = false)
        assertTrue(vm.deck.isEmpty())
    }

    @Test
    fun flipToggles() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        assertFalse(vm.flipped)
        vm.flip()
        assertTrue(vm.flipped)
        vm.flip()
        assertFalse(vm.flipped)
    }

    @Test
    fun advanceMovesToNextCard() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        assertEquals(0, vm.index)
        vm.advance()
        assertEquals(1, vm.index)
        assertFalse(vm.done)
    }

    @Test
    fun advanceOnLastCardSetsDone() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        repeat(vm.deck.lastIndex) { vm.advance() }
        assertEquals(vm.deck.lastIndex, vm.index)
        assertFalse(vm.done)
        vm.advance()
        assertTrue(vm.done)
    }

    @Test
    fun advanceResetsFlip() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        vm.flip()
        assertTrue(vm.flipped)
        vm.advance()
        assertFalse(vm.flipped)
    }

    @Test
    fun recordCommitAndUndo() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        val number = vm.deck[0]
        vm.recordCommit(number, markedLearned = true)
        vm.advance()
        assertEquals(1, vm.index)

        val undone = vm.undo()
        assertEquals(number to true, undone)
        assertEquals(0, vm.index)
        assertFalse(vm.done)
        assertNull(vm.undoable)
    }

    @Test
    fun undoWithNothingReturnsNull() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        assertNull(vm.undo())
    }

    @Test
    fun undoClearsDoneState() {
        val vm = vm()
        val two = names.take(2)
        vm.ensureDeck(two, learned = emptySet(), includeLearned = false)
        vm.recordCommit(vm.deck[0], true)
        vm.advance()
        vm.recordCommit(vm.deck[1], true)
        vm.advance()
        assertTrue(vm.done)
        vm.undo()
        assertFalse(vm.done)
    }

    @Test
    fun restartRebuildsDeck() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        vm.advance()
        vm.advance()
        assertEquals(2, vm.index)
        vm.restart(names, learned = setOf(1, 2, 3), includeLearned = false)
        assertEquals(0, vm.index)
        assertEquals(3, vm.deck.size)
        assertFalse(vm.done)
    }

    @Test
    fun restartClearsUndoFromThePreviousSitting() {
        val vm = vm()
        vm.ensureDeck(names, learned = emptySet(), includeLearned = false)
        vm.recordCommit(vm.deck[0], markedLearned = true)
        vm.advance()
        vm.restart(names, learned = emptySet(), includeLearned = false)
        // Undo after a restart must be a no-op: the card it would un-learn
        // belongs to the previous sitting and is not in the new deck.
        assertNull(vm.undoable)
        assertNull(vm.undo())
    }

    @Test
    fun corruptedRestoredDeckIsFiltered() {
        val restored = FlashcardsViewModel(
            SavedStateHandle(mapOf("deck.cards" to intArrayOf(1, 0, 999, 2)))
        )
        assertEquals(listOf(1, 2), restored.deck)
    }

    @Test
    fun invalidRestoredUndoIsDropped() {
        val restored = FlashcardsViewModel(
            SavedStateHandle(mapOf("deck.undoNumber" to 999, "deck.undoMarked" to true))
        )
        assertNull(restored.undoable)
    }
}
