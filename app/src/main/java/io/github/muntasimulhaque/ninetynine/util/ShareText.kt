package io.github.muntasimulhaque.ninetynine.util

import io.github.muntasimulhaque.ninetynine.data.Name

/**
 * The name as words, for the contexts a picture does not fit: the Arabic, the
 * transliteration, the full meaning, and the store title where a stranger can
 * find the app. The same hierarchy the exported card sets, minus the short
 * meaning, which the full meaning opens with anyway.
 */
object ShareText {

    /**
     * U+200E, the left-to-right mark, and this text's alignment.
     *
     * Plain text carries no alignment of its own, so a bidi-aware app reads a
     * message's direction off its first strong character and, meeting the
     * Arabic first, sets everything after it on the right. One mark, at the
     * very start, puts the whole block on the left, where a caption belongs,
     * and the Arabic still shapes right-to-left inside its own line.
     */
    private const val LTR_MARK = '\u200E'

    fun build(name: Name, wordmark: String): String = buildString {
        append(LTR_MARK)
        appendLine(name.arabic)
        appendLine(name.transliteration)
        appendLine()
        appendLine(name.meaning)
        appendLine()
        append(wordmark)
    }
}
