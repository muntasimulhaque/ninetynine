package io.github.muntasimulhaque.ninetynine.ui.memorize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.util.DeckBuilder

/** Session state for one flashcard run; survives rotation with the ViewModel. */
class FlashcardsViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    var deck by mutableStateOf<List<Int>>(
        savedState.get<IntArray>(KEY_DECK)?.toList()
            ?.filter { it in 1..99 }?.take(DeckBuilder.SESSION) ?: emptyList()
    ); private set
    var index by mutableIntStateOf(
        (savedState.get<Int>(KEY_INDEX) ?: 0).coerceAtLeast(0)
    ); private set
    var flipped by mutableStateOf(savedState.get<Boolean>(KEY_FLIPPED) ?: false); private set
    var done by mutableStateOf(savedState.get<Boolean>(KEY_DONE) ?: false); private set

    /**
     * True once this sitting's deck has been built from a settled input.
     *
     * An empty [deck] means two very different things before that: "the build
     * has not happened yet" and "there is nothing left to draw". Mapping the
     * first onto the second flashed the ٩٩ and "All 99 names learned" (with
     * the house cross-fade, no less) over the first card of every sitting,
     * because the build runs in a LaunchedEffect one frame after the first
     * composition. The screen renders nothing until this turns true. Rides
     * the SavedStateHandle with the rest of the sitting.
     */
    var ready by mutableStateOf(savedState.get<Boolean>(KEY_READY) ?: false); private set
    private var lastInclude: Boolean? = savedState.get<Boolean>(KEY_LAST_INCLUDE)

    /**
     * The name the last card committed, so a mis-swipe can be taken back.
     *
     * A right-swipe writes a learned tick and moves on, and there was no way
     * back a card: the exact mirror of the guard that stops a left-swipe
     * silently *removing* one. Cleared as soon as the next card is committed,
     * so undo only ever reaches one step.
     */
    var undoable by mutableStateOf<Pair<Int, Boolean>?>(restoredUndo()); private set

    /**
     * The run lives in the SavedStateHandle, not just memory: a process death
     * mid-deck restores the exact card, flip state and undo, the same way the
     * name pager restores its page. The keys are dropped in [onCleared], so
     * leaving the screen starts the next sitting fresh.
     */
    private fun restoredUndo(): Pair<Int, Boolean>? {
        val number = savedState.get<Int>(KEY_UNDO_NUMBER) ?: return null
        if (number !in 1..99) return null
        return number to (savedState.get<Boolean>(KEY_UNDO_MARKED) ?: true)
    }

    private fun saveSession() {
        savedState[KEY_DECK] = deck.toIntArray()
        savedState[KEY_INDEX] = index
        savedState[KEY_FLIPPED] = flipped
        savedState[KEY_DONE] = done
        savedState[KEY_LAST_INCLUDE] = lastInclude
        savedState[KEY_READY] = ready
        val undo = undoable
        savedState[KEY_UNDO_NUMBER] = undo?.first
        savedState[KEY_UNDO_MARKED] = undo?.second
    }

    /**
     * Builds the deck once the input has settled.
     *
     * [namesLoaded] is what makes an empty deck unambiguous: the build runs in
     * a LaunchedEffect, a frame after the first composition, and until it
     * lands an empty deck means "not built yet": not "everything is
     * learned".
     */
    fun ensureDeck(
        namesLoaded: Boolean,
        names: List<Name>,
        learned: Set<Int>,
        includeLearned: Boolean,
    ) {
        // Nothing is decided until the asset read has finished: building from
        // an empty list would look exactly like a failed read.
        if (!namesLoaded) return
        // A rebuild is needed whenever the deck is not already this sitting's:
        // never built, or built under the other include-learned setting.
        if (names.isNotEmpty() && !(deck.isNotEmpty() && lastInclude == includeLearned)) {
            deck = DeckBuilder.build(names, learned, includeLearned)
            lastInclude = includeLearned
            index = 0
            flipped = false
            done = false
            // A rebuilt deck is a new sitting: the previous deck's last commit no
            // longer exists, so undoing it would silently un-learn a card that is
            // not even in this set.
            undoable = null
        }
        // Settled either way: with no unlearned names left this is the
        // all-learned page, and with an unreadable asset the screen says so.
        ready = true
        saveSession()
    }

    fun flip() {
        flipped = !flipped
        saveSession()
    }

    fun advance() {
        flipped = false
        if (index < deck.lastIndex) index++ else done = true
        saveSession()
    }

    fun recordCommit(number: Int, markedLearned: Boolean) {
        undoable = number to markedLearned
        saveSession()
    }

    fun undo(): Pair<Int, Boolean>? {
        val last = undoable ?: return null
        undoable = null
        flipped = false
        done = false
        if (index > 0) index--
        saveSession()
        return last
    }

    fun restart(names: List<Name>, learned: Set<Int>, includeLearned: Boolean) {
        deck = emptyList()
        lastInclude = null
        ready = false
        ensureDeck(namesLoaded = true, names, learned, includeLearned)
    }

    override fun onCleared() {
        savedState.remove<IntArray>(KEY_DECK)
        savedState.remove<Int>(KEY_INDEX)
        savedState.remove<Boolean>(KEY_FLIPPED)
        savedState.remove<Boolean>(KEY_DONE)
        savedState.remove<Boolean>(KEY_LAST_INCLUDE)
        savedState.remove<Boolean>(KEY_READY)
        savedState.remove<Int>(KEY_UNDO_NUMBER)
        savedState.remove<Boolean>(KEY_UNDO_MARKED)
    }

    private companion object {
        const val KEY_DECK = "deck.cards"
        const val KEY_INDEX = "deck.index"
        const val KEY_FLIPPED = "deck.flipped"
        const val KEY_DONE = "deck.done"
        const val KEY_LAST_INCLUDE = "deck.lastInclude"
        const val KEY_READY = "deck.ready"
        const val KEY_UNDO_NUMBER = "deck.undoNumber"
        const val KEY_UNDO_MARKED = "deck.undoMarked"
    }
}
