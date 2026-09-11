package io.github.muntasimulhaque.ninetynine.util

import io.github.muntasimulhaque.ninetynine.data.Name
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ShareTextTest {

    private val name = Name(
        number = 1,
        arabic = "اللّٰه",
        transliteration = "Allah",
        title = "With regard to the name Allah",
        meaning = "With regard to the name Allah, a brief indication of the " +
            "meaning is the one who is truly venerated and worshipped.",
    )
    private val wordmark = "The Ninety Nine Names of Allah"

    @Test
    fun theBlockIsArabicTransliterationMeaningThenWordmark() {
        assertEquals(
            "\u200E${name.arabic}\n" +
                "${name.transliteration}\n" +
                "\n" +
                "${name.meaning}\n" +
                "\n" +
                wordmark,
            ShareText.build(name, wordmark),
        )
    }

    @Test
    fun theShortMeaningIsNotRepeatedBesideTheName() {
        val secondLine = ShareText.build(name, wordmark).lines()[1]
        assertEquals(name.transliteration, secondLine)
        assertFalse(secondLine.contains(name.title))
        assertFalse(secondLine.contains('·'))
    }

    @Test
    fun oneLeftToRightMarkOpensTheMessageSoTheBlockSitsLeft() {
        val text = ShareText.build(name, wordmark)
        assertEquals('\u200E', text.first())
        assertEquals(1, text.count { it == '\u200E' })
    }
}
