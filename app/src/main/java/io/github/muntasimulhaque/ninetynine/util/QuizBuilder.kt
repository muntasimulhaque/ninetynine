package io.github.muntasimulhaque.ninetynine.util

import io.github.muntasimulhaque.ninetynine.data.Name
import java.util.Locale
import kotlin.random.Random
import kotlinx.serialization.Serializable

@Serializable
data class QuizQuestion(
    val number: Int,
    val options: List<String>,
    val answerIndex: Int,
)

object QuizBuilder {

    const val DEFAULT_COUNT = 10

    /** Words that carry no meaning of their own when comparing two titles. */
    private val STOP_WORDS = setOf(
        "the", "and", "or", "of", "to", "in", "on", "for", "with", "from", "by",
        "a", "an", "his", "their", "who", "that", "is", "are", "one", "ones",
    )

    private val WORD_NOISE = Regex("[^a-z ]")

    internal fun contentWords(title: String): Set<String> {
        val words = WORD_NOISE.replace(title.lowercase(Locale.ROOT), " ")
            .split(' ')
            .filter { it.isNotBlank() && it !in STOP_WORDS }
            .toSet()
        // A title made entirely of stop words ("The One") yields an empty set,
        // and an empty answer set makes EVERY other title look ambiguous —
        // {}.containsAll(other) is always true — so the fallback would offer
        // subsuming distractors ("The One Who Guides His Servants…" against
        // "The One"). Fall back to the whole title as a single token: "the
        // one" is contained by no other title's word set, so the ambiguity
        // filter keeps doing its job.
        return if (words.isEmpty()) setOf(title.lowercase(Locale.ROOT).trim()) else words
    }

    /**
     * True when one title says everything the other says and no less — "The
     * Guardian" against "The Ever-Watchful Guardian", or "The Bestower"
     * against "The Bestower of Mercy".
     *
     * Offered as alternatives these are not a test of memory but a trick: both
     * answers are defensible, so the reader is marked wrong for knowing the
     * meaning. Roughly one round in eleven contained a pair like this.
     */
    private fun ambiguousAgainst(answer: Set<String>, other: Set<String>): Boolean =
        other.isNotEmpty() && (answer.containsAll(other) || other.containsAll(answer))

    /** How many learned names it takes before a round is worth drawing from them. */
    const val MIN_LEARNED_POOL = 4

    /**
     * Builds [count] multiple-choice questions: pick the correct title for a name.
     *
     * [preferred] is asked about first when there are enough of them — the names
     * the reader has marked learned. Without it a reader who opened the quiz on
     * their first day was examined on all 99, scored two or three, and told to
     * keep at it: the least kind moment in an app whose whole register is
     * encouragement, and a waste of the one thing it knows about them. Below
     * [MIN_LEARNED_POOL] the round is drawn from everything, because a two-name
     * quiz is not a quiz.
     *
     * Distractors always come from the full list of titles: the point is to
     * recognise the right meaning among plausible ones, and narrowing the wrong
     * answers to the learned set would make it easier the more you knew.
     */
    fun build(
        all: List<Name>,
        count: Int = DEFAULT_COUNT,
        random: Random = Random,
        preferred: Set<Int> = emptySet(),
    ): List<QuizQuestion> {
        val titles = all.map { it.title }.distinct()
        val words = titles.associateWith(::contentWords)
        val pooled = all.filter { it.number in preferred }
        val asked = if (pooled.size >= MIN_LEARNED_POOL) {
            // Learned names first, topped up from the rest if the reader has
            // fewer learned than a full round.
            pooled.shuffled(random) + all.filter { it.number !in preferred }.shuffled(random)
        } else {
            all.shuffled(random)
        }
        return asked.take(count.coerceAtMost(all.size)).map { name ->
            val answerWords = words[name.title] ?: contentWords(name.title)
            val usable = titles.filter {
                it != name.title && !ambiguousAgainst(answerWords, words.getValue(it))
            }
            // If a name is so generic that too few titles are safe, fall back
            // to any other title rather than showing fewer than four options.
            val pool = if (usable.size >= 3) usable else titles.filter { it != name.title }
            val options = (pool.shuffled(random).take(3) + name.title).shuffled(random)
            QuizQuestion(
                number = name.number,
                options = options,
                answerIndex = options.indexOf(name.title),
            )
        }
    }
}
