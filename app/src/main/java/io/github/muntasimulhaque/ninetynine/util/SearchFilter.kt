package io.github.muntasimulhaque.ninetynine.util

import io.github.muntasimulhaque.ninetynine.data.Name
import java.util.Locale

object SearchFilter {

    private val LATIN_NOISE = Regex("[^a-z0-9]")
    private val ARABIC_MARKS = Regex("[\\u064B-\\u065F\\u0670\\u0653]")

    /**
     * Forgiving Latin key: lowercase, punctuation and spaces dropped, the two
     * vowels that Arabic transliteration spells inconsistently folded together,
     * and runs of a repeated letter collapsed.
     *
     * So "rahman", "ar rahman" and "a'laa" all find "Ar-Rahmaan", and — because
     * the same long vowel is written "oo"/"u" and "ee"/"i" by different sources
     * — "Qayyum" finds "Al-Qayyoom", "Ghafur" finds "Al-Ghafoor", "Quddus"
     * finds "Al-Quddoos" and "Wali" finds "Al-Walee". Readers arrive knowing
     * these names from elsewhere; they should not have to guess our spelling.
     */
    private fun latinKey(s: String): String {
        val stripped = LATIN_NOISE.replace(s.lowercase(Locale.ROOT), "")
        val sb = StringBuilder(stripped.length)
        for (c in stripped) {
            val folded = when (c) {
                'o' -> 'u'
                'e' -> 'i'
                else -> c
            }
            if (sb.isEmpty() || sb.last() != folded) sb.append(folded)
        }
        return sb.toString()
    }

    /** Arabic without harakat, so a bare query still finds the vocalized names. */
    private fun arabicKey(s: String): String = ARABIC_MARKS.replace(s, "")

    /**
     * Matches transliteration, title, meaning, note (case-insensitive),
     * Arabic, or the exact number.
     *
     * The note is searched because a reader who remembers a detail from one
     * — the distinction between Ar-Rahmaan and Ar-Raheem is the example
     * everyone reaches for — has no other way to find that name again. Only a
     * few of the 99 carry a note (see names.json), and a note is a bound
     * commentary on its own name, so it answers to the same forgiving Latin
     * key as the named fields below it.
     */
    fun filter(names: List<Name>, query: String): List<Name> {
        val q = query.trim()
        if (q.isEmpty()) return names
        val number = q.toIntOrNull()
        val lq = latinKey(q)
        val aq = arabicKey(q)
        return names.filter {
            it.transliteration.contains(q, ignoreCase = true) ||
                it.title.contains(q, ignoreCase = true) ||
                it.meaning.contains(q, ignoreCase = true) ||
                it.note?.contains(q, ignoreCase = true) == true ||
                (lq.length >= 3 && lq in latinKey(it.transliteration)) ||
                it.arabic.contains(q) ||
                (aq.isNotEmpty() && aq in arabicKey(it.arabic)) ||
                it.number == number
        }
    }
}
