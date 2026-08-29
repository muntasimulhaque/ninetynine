package io.github.muntasimulhaque.ninetynine.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

val Context.dataStore by preferencesDataStore(
    name = "settings",
    // A half-written file (interrupted write, bad shutdown, filesystem damage)
    // would otherwise throw on every read forever. Starting over from defaults
    // loses the stored values, but the alternative is an app that cannot launch.
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

enum class ThemeMode { SYSTEM, LIGHT, DARK, BLACK }

class Prefs(private val context: Context) {

    /**
     * Every read goes through here.
     *
     * DataStore's `data` flow throws when the file cannot be read. These flows
     * are collected in `stateIn(viewModelScope, …)`, which has no exception
     * handler, so an uncaught throw reaches the thread's default handler and
     * kills the process — on launch, every launch, with no way out but clearing
     * app data. That would destroy the one thing this app stores: which of the
     * 99 names the reader has learned.
     *
     * `retryWhen`, not `catch`: `catch` emits and then *completes* the flow, so
     * a single transient read failure would end every derived flow for the rest
     * of the process. `stateIn` would pin the empty value, and Memorize would
     * read "0 learned" and Bookmarks "nothing kept" — a lie about intact data,
     * until the app was restarted. This emits the same fallback and then lets
     * DataStore try again.
     *
     * It retries on *any* exception, not just IOException. DataStore's real
     * failure mode is IOException (corruption is already handled by the file's
     * corruptionHandler), but a cold start can surface a transient race while
     * the store initialises; letting a non-IO exception escape would crash the
     * process on a launch. A fallback value for one read is harmless — the next
     * retry delivers the stored truth.
     */
    private val data: Flow<Preferences> = context.dataStore.data
        .retryWhen { _, _ ->
            emit(emptyPreferences())
            delay(250)
            true
        }

    private object Keys {
        val LEARNED = stringSetPreferencesKey("learned")
        val BOOKMARKED = stringSetPreferencesKey("bookmarked")
        val THEME = stringPreferencesKey("theme")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val DAILY_HOUR = intPreferencesKey("daily_hour")
        val DAILY_MINUTE = intPreferencesKey("daily_minute")
        val NOTIFICATIONS_ASKED = booleanPreferencesKey("notifications_asked")
        val QUIZ_BEST = intPreferencesKey("quiz_best")
        val INCLUDE_LEARNED = booleanPreferencesKey("include_learned")
    }

    val learned: Flow<Set<Int>> = data
        .map { p -> p[Keys.LEARNED]?.mapNotNull(String::toIntOrNull)?.toSet() ?: emptySet() }

    /**
     * The names the reader has kept. A separate axis from [learned] on purpose:
     * that one is memorization progress and feeds the Memorize screen, this one
     * is attachment and feeds nothing. Resetting progress deliberately leaves
     * these alone.
     */
    val bookmarked: Flow<Set<Int>> = data
        .map { p -> p[Keys.BOOKMARKED]?.mapNotNull(String::toIntOrNull)?.toSet() ?: emptySet() }

    val themeMode: Flow<ThemeMode> = data
        .map { p -> runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM) }

    val textScale: Flow<Float> = data
        .map { p -> p[Keys.TEXT_SCALE] ?: 1f }

    /**
     * On by default: the daily reminder is the app's rhythm, and its consent
     * lives in the system permission dialog (MainActivity's one-time ask),
     * not in a switch a reader has to go and find. A stored false always
     * wins — a reader who turned the reminder off keeps it off.
     */
    val dailyEnabled: Flow<Boolean> = data
        .map { p -> p[Keys.DAILY_ENABLED] ?: true }

    /** Whether the one-time notification-permission ask has already happened. */
    val notificationsAsked: Flow<Boolean> = data
        .map { p -> p[Keys.NOTIFICATIONS_ASKED] ?: false }

    val dailyTime: Flow<Pair<Int, Int>> = data
        .map { p -> (p[Keys.DAILY_HOUR] ?: 8) to (p[Keys.DAILY_MINUTE] ?: 0) }

    /** Best quiz score so far, or -1 when no round has been finished. */
    val quizBest: Flow<Int> = data
        .map { p -> p[Keys.QUIZ_BEST] ?: -1 }

    val includeLearned: Flow<Boolean> = data
        .map { p -> p[Keys.INCLUDE_LEARNED] ?: false }

    /**
     * Writes fail the same way reads do, and from a `viewModelScope.launch`
     * they crash just as hard. A setting that failed to save is not worth the
     * process; the value simply stays as it was.
     *
     * Any exception is swallowed, not just IOException — the read side
     * retries on any cause for the same reason: a cold start can surface a
     * transient race while the store initialises, and a non-IO failure
     * escaping a `viewModelScope.launch` kills the process over a toggle the
     * reader just tapped. Cancellation is rethrown, of course: swallowing it
     * would break the scope's shutdown.
     */
    private suspend fun write(block: (MutablePreferences) -> Unit) {
        try {
            context.dataStore.edit(block)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun setLearned(number: Int, value: Boolean) = write { p ->
        val current = p[Keys.LEARNED]?.toMutableSet() ?: mutableSetOf()
        if (value) current.add(number.toString()) else current.remove(number.toString())
        p[Keys.LEARNED] = current
    }

    suspend fun setBookmarked(number: Int, value: Boolean) = write { p ->
        val current = p[Keys.BOOKMARKED]?.toMutableSet() ?: mutableSetOf()
        if (value) current.add(number.toString()) else current.remove(number.toString())
        p[Keys.BOOKMARKED] = current
    }

    /** Clears learned names and the quiz score only — bookmarks are not progress. */
    suspend fun resetLearned() = write {
        it[Keys.LEARNED] = emptySet()
        it.remove(Keys.QUIZ_BEST)
    }

    suspend fun setThemeMode(mode: ThemeMode) = write { it[Keys.THEME] = mode.name }

    suspend fun setTextScale(scale: Float) = write { it[Keys.TEXT_SCALE] = scale }

    suspend fun setDailyEnabled(enabled: Boolean) = write { it[Keys.DAILY_ENABLED] = enabled }

    suspend fun setNotificationsAsked() = write { it[Keys.NOTIFICATIONS_ASKED] = true }

    suspend fun setDailyTime(hour: Int, minute: Int) = write {
        it[Keys.DAILY_HOUR] = hour
        it[Keys.DAILY_MINUTE] = minute
    }

    /** Keeps the running maximum; lower scores are ignored. */
    suspend fun setQuizBest(score: Int) = write {
        if (score > (it[Keys.QUIZ_BEST] ?: -1)) it[Keys.QUIZ_BEST] = score
    }

    suspend fun setIncludeLearned(include: Boolean) = write { it[Keys.INCLUDE_LEARNED] = include }
}
