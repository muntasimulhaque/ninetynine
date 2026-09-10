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
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
        assertEquals(6, vm.deck.size)
        assertEquals(0, vm.index)
        assertFalse(vm.flipped)
        assertFalse(vm.done)
    }

    @Test
    fun ensureDeckExcludesLearnedByDefault() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, names, learned = setOf(2, 5), includeLearned = false)
        assertEquals(4, vm.deck.size)
        assertFalse(2 in vm.deck)
        assertFalse(5 in vm.deck)
    }

    @Test
    fun ensureDeckNoOpsWhenAlreadyBuilt() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
        val first = vm.deck
        vm.ensureDeck(namesLoaded = true, names, learned = setOf(1), includeLearned = false)
        assertEquals(first, vm.deck)
    }

    @Test
    fun ensureDeckRebuildsWhenIncludeLearnedChanges() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, names, learned = setOf(2), includeLearned = false)
        assertEquals(5, vm.deck.size)
        vm.ensureDeck(namesLoaded = true, names, learned = setOf(2), includeLearned = true)
        assertEquals(6, vm.deck.size)
    }

    @Test
    fun emptyNamesGiveNoDeck() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, emptyList(), learned = emptySet(), includeLearned = false)
        assertTrue(vm.deck.isEmpty())
    }

    @Test
    fun flipToggles() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
        assertFalse(vm.flipped)
        vm.flip()
        assertTrue(vm.flipped)
        vm.flip()
        assertFalse(vm.flipped)
    }

    @Test
    fun advanceMovesToNextCard() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
        assertEquals(0, vm.index)
        vm.advance()
        assertEquals(1, vm.index)
        assertFalse(vm.done)
    }

    @Test
    fun advanceOnLastCardSetsDone() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
        repeat(vm.deck.lastIndex) { vm.advance() }
        assertEquals(vm.deck.lastIndex, vm.index)
        assertFalse(vm.done)
        vm.advance()
        assertTrue(vm.done)
    }

    @Test
    fun advanceResetsFlip() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
        vm.flip()
        assertTrue(vm.flipped)
        vm.advance()
        assertFalse(vm.flipped)
    }

    @Test
    fun recordCommitAndUndo() {
        val vm = vm()
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
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
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
        assertNull(vm.undo())
    }

    @Test
    fun undoClearsDoneState() {
        val vm = vm()
        val two = names.take(2)
        vm.ensureDeck(namesLoaded = true, two, learned = emptySet(), includeLearned = false)
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
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
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
        vm.ensureDeck(namesLoaded = true, names, learned = emptySet(), includeLearned = false)
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

    /**
     * The screen renders nothing until [FlashcardsViewModel.ready] turns:
     * an empty deck that has not been built yet must never be shown as the
     * all-learned page, which is what it used to flash — with the house
     * cross-fade — over the first card of every sitting.
     */
    @Test
    fun deckIsNotReadyUntilTheAssetReadHasSettled() {
        val vm = vm()
        assertFalse(vm.ready)
        // Still loading: an empty list is not yet an empty deck.
        vm.ensureDeck(namesLoaded = false, names = emptyList(), learned = emptySet(), includeLearned = false)
        assertFalse(vm.ready)
        // Read and empty: the failure is real, and the screen says so.
        vm.ensureDeck(namesLoaded = true, names = emptyList(), learned = emptySet(), includeLearned = false)
        assertTrue(vm.ready)
        assertTrue(vm.deck.isEmpty())
    }

    @Test
    fun allLearnedDeckIsReadyAndEmpty() {
        val vm = vm()
        vm.ensureDeck(
            namesLoaded = true,
            names = names,
            learned = names.map { it.number }.toSet(),
            includeLearned = false,
        )
        assertTrue(vm.ready)
        assertTrue(vm.deck.isEmpty())
    }

    @Test
    fun readinessRidesTheSavedStateAcrossProcessDeath() {
        // A shared handle stands in for a restored one: the second ViewModel
        // reads exactly what the first wrote into it, the way a recreated
        // ViewModel reads the bundle the system kept.
        val handle = SavedStateHandle()
        FlashcardsViewModel(handle).ensureDeck(
            namesLoaded = true,
            names = names,
            learned = emptySet(),
            includeLearned = false,
        )
        assertTrue(FlashcardsViewModel(handle).ready)
    }
}
