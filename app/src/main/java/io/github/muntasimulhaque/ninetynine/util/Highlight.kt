package io.github.muntasimulhaque.ninetynine.util

/**
 * Where a search query literally occurs in a piece of text, so the list can
 * show its work: matched ranges render in the app's gold instead of the
 * row's ink.
 *
 * Deliberately literal (case-insensitive substring) and NOT the forgiving
 * [SearchFilter] key: "Qayyum" finds "Al-Qayyoom" in the list, but there is
 * no honest span of the row to paint for it, and inventing one would teach
 * the reader a false correspondence. Fuzzy-only matches simply arrive
 * unhighlighted.
 */
object Highlight {

    /** Shorter queries paint noise — half of every row answers to "a". */
    private const val MIN_QUERY_LENGTH = 2

    /**
     * The ranges of [text] that match [query], in reading order,
     * non-overlapping. Empty when the query is shorter than
     * [MIN_QUERY_LENGTH] after trimming, or never occurs.
     */
    fun matches(text: String, query: String): List<IntRange> {
        val q = query.trim()
        if (q.length < MIN_QUERY_LENGTH || text.isEmpty()) return emptyList()
        val result = mutableListOf<IntRange>()
        var index = text.indexOf(q, ignoreCase = true)
        while (index >= 0) {
            result += index until index + q.length
            index = text.indexOf(q, index + q.length, ignoreCase = true)
        }
        return result
    }
}
