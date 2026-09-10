package io.github.muntasimulhaque.ninetynine.ui

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Guards the counters against drifting back to locale digits.
 *
 * The app's two-number counters and scores ("3 of 99", "7 / 10") are
 * formatted with `%1$s` on Int arguments, never `%1$d`. `%d` follows the
 * device locale, so on an ar/ur phone the numbers rendered in Arabic-Indic
 * digits and the bidi algorithm reversed the pair visually, while every other
 * number the app draws — the folio numbers, the learned count — is
 * `Int.toString()` and stayed Western. `%s` formats the Ints with
 * `String.valueOf`, which is Western digits everywhere, so the counters match
 * the rest of the UI on every device.
 *
 * No Android needed: the resource is read straight off disk, the same way
 * NamesAssetTest reads the names.
 */
class CounterFormatTest {

    private val strings = File("src/main/res/values/strings.xml").readText()

    @Test
    fun countersNeverUseLocaleDigits() {
        val localeDigit = Regex("%\\d*\\$?d")
        listOf(
            "detail_counter",
            "card_x_of_y",
            "question_x_of_y",
            "quiz_score_format",
            // Numbers inside sentences count too: on an ar/ur phone a %d here
            // rendered Arabic-Indic digits in the line directly under the big
            // Western learned count, and beside Western folio numbers. The
            // “percent” string is the same claim about the text-size slider.
            "quiz_best",
            "percent",
        ).forEach { name ->
            val entry = Regex("""<string name="$name">([^<]+)</string>""").find(strings)
                ?.groupValues?.get(1)
                ?: error("$name is missing from strings.xml")
            assertFalse(
                "$name uses %d, which follows the device locale; " +
                    "counters use %s so they stay Western digits (see strings.xml)",
                localeDigit.containsMatchIn(entry),
            )
        }
    }

    /** The plural sentence Memorize shows under its count, guarded the same way. */
    @Test
    fun remainingCountPluralNeverUsesLocaleDigits() {
        val block = Regex("""<plurals name="remaining_count">(.+?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .find(strings)?.groupValues?.get(1)
            ?: error("remaining_count is missing from strings.xml")
        assertFalse(
            "remaining_count uses %d, which follows the device locale; " +
                "the big count above it is Western, so this line must be too",
            Regex("%\\d*\\$?d").containsMatchIn(block),
        )
    }
}
