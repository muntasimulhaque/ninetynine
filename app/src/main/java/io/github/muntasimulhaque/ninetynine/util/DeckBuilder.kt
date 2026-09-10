package io.github.muntasimulhaque.ninetynine.util

import io.github.muntasimulhaque.ninetynine.data.Name
import kotlin.random.Random

object DeckBuilder {

    /**
     * Builds a flashcard deck of name numbers: the unlearned names first
     * (shuffled), then (only when requested) the learned ones (shuffled).
     */
    /**
     * How many cards a sitting is.
     *
     * The deck used to be every unlearned name, so a new reader's first one was
     * "1 OF 97": a session nobody finishes, a progress hairline that barely
     * moves, and a done screen almost no one ever sees. The quiz beside it is
     * ten questions and is correctly sized for a moment with the app; this
     * makes the deck the same shape, and "Reshuffle" means the next ten.
     */
    const val SESSION = 10

    fun build(
        all: List<Name>,
        learned: Set<Int>,
        includeLearned: Boolean,
        random: Random = Random,
    ): List<Int> {
        val (known, unknown) = all.partition { it.number in learned }
        val ordered = unknown.shuffled(random).map { it.number } +
            if (includeLearned) known.shuffled(random).map { it.number } else emptyList()
        return ordered.take(SESSION)
    }
}
