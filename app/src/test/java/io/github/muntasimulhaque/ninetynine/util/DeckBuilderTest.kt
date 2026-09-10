package io.github.muntasimulhaque.ninetynine.util

import io.github.muntasimulhaque.ninetynine.data.Name
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckBuilderTest {

    private val names = (1..6).map { n ->
        Name(n, "arabic$n", "Name-$n", "Title $n", "Meaning $n")
    }
    private val learned = setOf(2, 5)

    @Test
    fun emptyNamesGiveEmptyDeck() {
        assertTrue(DeckBuilder.build(emptyList(), learned, includeLearned = true).isEmpty())
    }

    @Test
    fun learnedAreExcludedByDefault() {
        val deck = DeckBuilder.build(names, learned, includeLearned = false)
        assertEquals(setOf(1, 3, 4, 6), deck.toSet())
    }

    @Test
    fun includeLearnedAppendsThemAfterTheUnlearned() {
        val deck = DeckBuilder.build(names, learned, includeLearned = true)
        assertEquals(6, deck.size)
        assertEquals(setOf(1, 3, 4, 6), deck.take(4).toSet())
        assertEquals(setOf(2, 5), deck.drop(4).toSet())
    }

    @Test
    fun everyNameAppearsExactlyOnceWhenIncluded() {
        val deck = DeckBuilder.build(names, learned, includeLearned = true)
        assertEquals((1..6).toSet(), deck.toSet())
        assertEquals(deck.size, deck.distinct().size)
    }

    @Test
    fun sameSeedGivesSameDeck() {
        val a = DeckBuilder.build(names, learned, includeLearned = true, random = Random(7))
        val b = DeckBuilder.build(names, learned, includeLearned = true, random = Random(7))
        assertEquals(a, b)
    }

    @Test
    fun deckIsCappedToOneSitting() {
        // A deck used to be every unlearned name, "1 OF 97", which nobody
        // finishes, so the done screen was never seen and the progress
        // hairline barely moved.
        val ninetyNine = (1..99).map { Name(it, "arabic$it", "Name-$it", "Title $it", "Meaning $it") }
        val deck = DeckBuilder.build(ninetyNine, learned = emptySet(), includeLearned = false)
        assertEquals(DeckBuilder.SESSION, deck.size)
        assertEquals(deck.size, deck.distinct().size)
    }

    @Test
    fun aShortDeckIsNotPaddedToTheSessionLength() {
        val deck = DeckBuilder.build(names, learned, includeLearned = false)
        assertEquals(4, deck.size)   // six names, two already learned
    }
}
