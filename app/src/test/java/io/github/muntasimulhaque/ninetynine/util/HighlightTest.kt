package io.github.muntasimulhaque.ninetynine.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightTest {

    @Test
    fun findsCaseInsensitiveMatch() {
        assertEquals(listOf(3..7), Highlight.matches("Al-Aleem", "aleem"))
    }

    @Test
    fun findsEveryOccurrence() {
        assertEquals(
            listOf(0..2, 9..11),
            Highlight.matches("The One, The Only", "the"),
        )
    }

    @Test
    fun matchesAdvancePastThemselves() {
        // A match never reuses a letter of the previous one: "aa" in "aaa"
        // is 0..1 then 2..3's attempt starts too late to matter.
        assertEquals(listOf(0..1), Highlight.matches("aaa", "aa"))
    }

    @Test
    fun noMatchGivesNothing() {
        assertTrue(Highlight.matches("Ar-Rahmaan", "karim").isEmpty())
    }

    @Test
    fun queryIsTrimmed() {
        assertEquals(listOf(3..7), Highlight.matches("Al-Aleem", "  ALEEM  "))
    }

    @Test
    fun singleCharacterQueryStaysQuiet() {
        // One letter would paint half of every row; a match needs substance.
        assertTrue(Highlight.matches("Al-Aleem", "a").isEmpty())
        assertTrue(Highlight.matches("Al-Aleem", " ").isEmpty())
        assertTrue(Highlight.matches("Al-Aleem", "").isEmpty())
    }

    @Test
    fun emptyTextGivesNothing() {
        assertTrue(Highlight.matches("", "aleem").isEmpty())
    }

    @Test
    fun matchesInsideLongerTitle() {
        assertEquals(
            listOf(4..12),
            Highlight.matches("The Extremely Merciful", "extremely"),
        )
    }
}
