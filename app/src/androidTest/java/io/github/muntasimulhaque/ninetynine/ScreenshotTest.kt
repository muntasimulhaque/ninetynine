package io.github.muntasimulhaque.ninetynine

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.ninetynine.data.ThemeMode
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.bookmarks.BookmarksScreen
import io.github.muntasimulhaque.ninetynine.ui.detail.DetailScreen
import io.github.muntasimulhaque.ninetynine.ui.home.HomeScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.FlashcardsScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.FlashcardsViewModel
import io.github.muntasimulhaque.ninetynine.ui.memorize.MemorizeScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.QuizScreen
import io.github.muntasimulhaque.ninetynine.ui.settings.SettingsScreen
import io.github.muntasimulhaque.ninetynine.ui.share.ShareCard
import io.github.muntasimulhaque.ninetynine.ui.theme.Names99Theme
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageInset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Renders the canonical Play-listing scene set directly (no app session, no
 * adb taps) at whatever resolution the device reports, and saves a PNG per
 * scene to the instrumentation run's additional test output directory
 * (falling back to the app's internal files dir). The CI workflow runs this
 * on a phone, 7-inch and 10-inch emulator; AGP copies the PNGs off-device
 * into the build's connected-androidTest additional output folder for the
 * workflow to upload.
 *
 * The canonical set (owner decision, 1.23 — replaces the earlier eight):
 *
 * - home (the Names page)
 * - memorize
 * - flashcards-front AND flashcards-back (both faces of the card)
 * - quiz
 * - bookmarks (a kept shelf, not the empty state)
 * - settings
 * - name (a name page)
 * - share (a name's share screen)
 *
 * No scene targets a particular name — any name will do. The name page takes
 * the first in the book, the share card the first loaded, the quiz whatever
 * the round draws; nothing downstream keys on a specific name.
 *
 * This mirrors the other app's screenshot pipeline: mount a screen with a real
 * ViewModel, wait for the names to load, capture the idle frame. Deterministic
 * and far lighter than driving the running app, so the slow CI tablet emulators
 * don't ANR. Pure render, no input injection: the flashcard's back face is
 * reached by flipping the session ViewModel directly, never by performing
 * clicks.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    // The Android variant exposes the host ComponentActivity, whose
    // ViewModelStore the flashcard scenes reach to flip the card directly
    // (renderFlashcards) — the same instance FlashcardsScreen finds.
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun app(): Application =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

    /**
     * The directory AGP copies off-device after the connected test run, as
     * wired by the instrumentation arg `additionalTestOutputDir` (falling
     * back to the app's internal files dir when absent, e.g. Android
     * Studio runs).
     */
    private fun resolveOutDir(): File {
        val path = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        if (path != null) {
            val dir = File(path)
            if (dir.isDirectory || dir.mkdirs()) return dir
            // Cold-booted emulators can lag mounting shared storage; fall
            // back rather than fail.
        }
        return File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
        ).apply { mkdirs() }
    }

    private fun saveScreenshot(name: String) {
        val dir = resolveOutDir()
        // The slow CI tablet emulators occasionally stall the PixelCopy behind
        // captureToImage ("Failed waiting for PixelCopy!"). The frame is static,
        // so a retry after a fresh idle wait is always safe.
        repeat(3) { attempt ->
            composeRule.waitForIdle()
            try {
                val bitmap = composeRule.onRoot(true).captureToImage().asAndroidBitmap()
                File(dir, "$name.png").outputStream().use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                return
            } catch (e: AssertionError) {
                if (attempt == 2) throw e
                Thread.sleep(2_000)
            }
        }
    }

    private fun waitForNames(viewModel: NamesViewModel) {
        composeRule.waitUntil(timeoutMillis = 20_000) { viewModel.names.value.isNotEmpty() }
    }

    @Test
    fun home() = render("home") {
        HomeScreen(it, {}, rememberLazyListState())
    }

    @Test
    fun memorize() = render("memorize") {
        MemorizeScreen(
            it,
            onFlashcards = {},
            onQuiz = {},
            onLearned = {},
        )
    }

    @Test
    fun quiz() = render("quiz") {
        QuizScreen(it, onNameClick = {}, onBack = {})
    }

    @Test
    fun settings() = render("settings", onNamesReady = { viewModel ->
        // The reminder is on by default; the capture must show the app's real
        // resting state, not whatever a reused CI device's DataStore carries —
        // the listing's settings screenshot once showed the advertised toggle
        // switched off. Same discipline as the bookmarks scene above: seed the
        // state, wait for it to reach the flow, then capture.
        viewModel.setDailyEnabled(true)
        composeRule.waitUntil(timeoutMillis = 20_000) { viewModel.dailyEnabled.value }
    }) {
        SettingsScreen(it, onAbout = {})
    }

    @Test
    fun namePage() = render("name") { viewModel ->
        // Any name will do; the first in the book is simply the one that is
        // always there.
        DetailScreen(viewModel, startNumber = 1, bookmarksOnly = false, onBack = {})
    }

    @Test
    fun flashcardsFront() = renderFlashcards("flashcards-front") { }

    @Test
    fun flashcardsBack() = renderFlashcards("flashcards-back") { session ->
        // The back face is reached by flipping the session ViewModel, not by
        // injecting a tap: the test stays a pure render, and the flip
        // animation settles before the capture (waitForIdle).
        session.flip()
    }

    @Test
    fun bookmarks() = render("bookmarks", onNamesReady = { viewModel ->
        // Populate the shelf with the first few loaded names — a kept-shelf
        // screenshot says what the tab is for, an empty one says nothing, and
        // which names are kept is deliberately arbitrary. The capture waits
        // until the writes have reached the flow, so the rows are on screen.
        viewModel.names.value.take(3).forEach { viewModel.setBookmarked(it.number, true) }
        composeRule.waitUntil(timeoutMillis = 20_000) {
            viewModel.bookmarked.value.size >= 3
        }
    }) {
        BookmarksScreen(it, {}, {}, rememberLazyListState())
    }

    @Test
    fun share() = render("share") { viewModel ->
        // Any name will do — the first loaded one. The plate renders directly,
        // not inside ShareSheet: the sheet lives in its own window, which the
        // compose test root cannot PixelCopy. Scrollable + Centre arrangement
        // = centred when the card fits, top-anchored and scrollable when it
        // overflows (tablet7).
        val names by viewModel.names.collectAsStateWithLifecycle()
        val anyName = names.firstOrNull()
        if (anyName != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PageInset, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShareCard(name = anyName, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    /**
     * The shared render: mount the screen, wait for the names, let the scene
     * do any last settling ([onNamesReady] — populating the bookmarks shelf,
     * say), and save.
     */
    private fun render(
        name: String,
        onNamesReady: (NamesViewModel) -> Unit = {},
        screen: @Composable (NamesViewModel) -> Unit,
    ) {
        val viewModel = NamesViewModel(app(), SavedStateHandle())
        composeRule.setContent {
            Names99Theme(themeMode = ThemeMode.LIGHT, textScale = 1f) {
                screen(viewModel)
            }
        }
        waitForNames(viewModel)
        onNamesReady(viewModel)
        saveScreenshot(name)
    }

    /**
     * The two flashcard scenes, front and back, from one deck. The session
     * ViewModel is resolved from the test host's own store — the same
     * instance FlashcardsScreen finds via viewModel() — so [onDeckReady] can
     * drive the deck's state directly (flip for the back face) without any
     * input injection.
     */
    private fun renderFlashcards(
        name: String,
        onDeckReady: (FlashcardsViewModel) -> Unit,
    ) {
        val viewModel = NamesViewModel(app(), SavedStateHandle())
        composeRule.setContent {
            Names99Theme(themeMode = ThemeMode.LIGHT, textScale = 1f) {
                FlashcardsScreen(viewModel, onBack = {})
            }
        }
        waitForNames(viewModel)
        val session = ViewModelProvider(composeRule.activity)[FlashcardsViewModel::class.java]
        composeRule.waitUntil(timeoutMillis = 20_000) { session.deck.isNotEmpty() }
        composeRule.waitForIdle()
        onDeckReady(session)
        saveScreenshot(name)
    }
}
