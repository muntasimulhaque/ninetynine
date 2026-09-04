package io.github.muntasimulhaque.ninetynine.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object NamesRepository {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: List<Name>? = null
    private val mutex = Mutex()

    /**
     * The 99 names, read once from the bundled asset.
     *
     * A missing or malformed asset returns an empty list rather than throwing.
     * This runs inside `stateIn(viewModelScope, …)` and in the widget and
     * notification workers, none of which handle exceptions, so a throw here
     * is a crash on launch. It is also the code path a release build's
     * shrinking could disturb, and the one most likely to change when another
     * language is added. An empty list is caught by the screens, which say so.
     */
    suspend fun load(context: Context): List<Name> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: withContext(Dispatchers.IO) {
                try {
                    val text = context.assets.open("names.json").bufferedReader().use { it.readText() }
                    json.decodeFromString<List<Name>>(text).sortedBy { it.number }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
            }.also { if (it.isNotEmpty()) cache = it }
        }
    }
}
