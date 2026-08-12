package io.github.muntasimulhaque.ninetynine.util

import io.github.muntasimulhaque.ninetynine.data.Name
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterTest {

    private val names = listOf(
        Name(1, "الله", "Allah", "The God", "The one truly venerated and worshipped."),
        Name(21, "الْحَكِيم", "Al-Hakeem", "The All-Wise", "The one fully wise in everything."),
        Name(56, "اللَّطِيف", "Al-Lateef", "The Subtle And Kind", "The one who is fully aware of the hidden details."),
        Name(30, "الرَّحْمَٰن", "Ar-Rahmaan", "The Extremely Merciful", "The one possessing tremendous mercy."),
        Name(3, "الْأَعْلَى", "Al-A'laa", "The Most High", "The one who is above everything."),
    )

    @Test
    fun blankQueryReturnsEverything() {
        assertEquals(names, SearchFilter.filter(names, ""))
        assertEquals(names, SearchFilter.filter(names, "   "))
    }

    @Test
    fun matchesNote() {
        val noted = listOf(
            Name(1, "الله", "Allah", "The God", "The one truly venerated.",
                note = "The difference between Ar-Rahman and Ar-Raheem is that Ar-Rahman is of Allah's self."),
            Name(2, "الرَّحْمَٰن", "Ar-Rahmaan", "The Extremely Merciful",
                "The one possessing tremendous mercy.",
                note = "The difference between Ar-Rahman and Ar-Raheem is that Ar-Rahman is of Allah's self."),
            Name(3, "الْحَكِيم", "Al-Hakeem", "The All-Wise", "The one fully wise.",
                note = "Everything He does is with wisdom."),
        )
        // A detail remembered from a note, with none of the named fields
        // matching — the note is the only way to this name.
        assertEquals(listOf(noted[2]), SearchFilter.filter(noted, "with wisdom"))
        // The note must not reach names whose note does not contain it.
        assertTrue(noted[0] in SearchFilter.filter(noted, "difference between"))
        assertTrue(noted[1] in SearchFilter.filter(noted, "difference between"))
        assertEquals(2, SearchFilter.filter(noted, "difference between").size)
    }

    @Test
    fun matchesTransliterationIgnoringCase() {
        assertEquals(listOf(names[1]), SearchFilter.filter(names, "hakeem"))
        assertEquals(listOf(names[2]), SearchFilter.filter(names, "AL-LAT"))
    }

    @Test
    fun matchesTitleAndMeaning() {
        assertEquals(listOf(names[1]), SearchFilter.filter(names, "all-wise"))
        assertEquals(listOf(names[2]), SearchFilter.filter(names, "hidden details"))
    }

    @Test
    fun matchesArabic() {
        assertEquals(listOf(names[0]), SearchFilter.filter(names, "الله"))
    }

    @Test
    fun matchesExactNumber() {
        assertEquals(listOf(names[1]), SearchFilter.filter(names, "21"))
    }

    @Test
    fun noMatchReturnsEmpty() {
        assertTrue(SearchFilter.filter(names, "zzzz").isEmpty())
    }

    @Test
    fun collapsedVowelsStillMatch() {
        assertEquals(listOf(names[3]), SearchFilter.filter(names, "rahman"))
        assertEquals(listOf(names[3]), SearchFilter.filter(names, "ar rahman"))
    }

    @Test
    fun hyphensSpacesAndApostrophesAreIgnored() {
        assertEquals(listOf(names[2]), SearchFilter.filter(names, "al lateef"))
        // Short keys match broadly while typing; the apostrophized name must be among them.
        assertTrue(names[4] in SearchFilter.filter(names, "alaa"))
    }

    @Test
    fun arabicQueryWithoutMarksFindsVocalizedNames() {
        assertEquals(listOf(names[2]), SearchFilter.filter(names, "اللطيف"))
    }

    /**
     * Readers arrive knowing these names from other books, where the same long
     * vowel is written "oo" or "u" and "ee" or "i". Before this, typing the
     * spelling you already knew could find nothing at all.
     */
    @Test
    fun alternateVowelSpellingsFindTheSameName() {
        val vowels = listOf(
            Name(25, "الْقَيُّوم", "Al-Qayyoom", "The Sustainer", "The one who sustains all."),
            Name(47, "الْغَفُور", "Al-Ghafoor", "The Oft-Forgiving", "The one who forgives extensively."),
            Name(52, "الْقُدُّوس", "Al-Quddoos", "The Most Pure", "The one free of every fault."),
            Name(77, "الْوَلِيّ", "Al-Walee", "The Protector", "The one who protects."),
        )
        assertEquals(listOf(vowels[0]), SearchFilter.filter(vowels, "qayyum"))
        assertEquals(listOf(vowels[1]), SearchFilter.filter(vowels, "ghafur"))
        assertEquals(listOf(vowels[2]), SearchFilter.filter(vowels, "quddus"))
        assertEquals(listOf(vowels[3]), SearchFilter.filter(vowels, "wali"))
        // The spellings the app itself uses must keep working.
        assertEquals(listOf(vowels[0]), SearchFilter.filter(vowels, "qayyoom"))
        assertEquals(listOf(vowels[2]), SearchFilter.filter(vowels, "quddoos"))
    }
}
