package io.github.muntasimulhaque.ninetynine

import android.app.Application
import android.graphics.Bitmap
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.ninetynine.data.ThemeMode
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.detail.DetailScreen
import io.github.muntasimulhaque.ninetynine.ui.home.HomeScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.FlashcardsScreen
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
 * Renders the eight Play-listing screens directly (no app session, no adb taps)
 * at whatever resolution the device reports, and saves a PNG per scene to the
 * instrumentation run's additional test output directory (falling back to the
 * app's internal files dir). The CI workflow runs this on a phone, 7-inch and
 * 10-inch emulator; AGP copies the PNGs off-device into the build's
 * connected-androidTest additional output folder for the workflow to upload.
 *
 * This mirrors the other app's screenshot pipeline: mount a screen with a real
 * ViewModel, wait for the names to load, capture the idle frame. Deterministic
 * and far lighter than driving the running app, so the slow CI tablet emulators
 * don't ANR.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

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

    private fun waitForNames(vm: NamesViewModel) {
        composeRule.waitUntil(timeoutMillis = 20_000) { vm.names.value.isNotEmpty() }
    }

    @Test
    fun homeLight() = render("home", ThemeMode.LIGHT) {
        HomeScreen(it, {}, {}, {}, rememberLazyListState())
    }

    @Test
    fun homeDark() = render("home-dark", ThemeMode.DARK) {
        HomeScreen(it, {}, {}, {}, rememberLazyListState())
    }

    @Test
    fun namePage() = render("name", ThemeMode.LIGHT) {
        DetailScreen(it, startNumber = 1, bookmarksOnly = false, onBack = {})
    }

    @Test
    fun memorize() = render("memorize", ThemeMode.LIGHT) {
        MemorizeScreen(
            it,
            onFlashcards = {},
            onQuiz = {},
            onSettings = {},
            onAbout = {},
            onLearned = {},
        )
    }

    @Test
    fun quiz() = render("quiz", ThemeMode.LIGHT) {
        QuizScreen(it, onNameClick = {}, onBack = {})
    }

    @Test
    fun flashcards() = render("flashcards", ThemeMode.LIGHT) {
        FlashcardsScreen(it, onBack = {})
    }

    @Test
    fun share() = render("share", ThemeMode.LIGHT) { vm ->
        // The share plate over Al-Aleem — the longest meaning, the card at
        // its fullest (the same name the adb recipes pick for detail/share).
        // The plate renders directly, not inside ShareSheet: the sheet lives
        // in its own window, which the compose test root cannot PixelCopy.
        // Scrollable + Centre arrangement = centred when the card fits,
        // top-anchored and scrollable when it overflows (tablet7).
        val names by vm.names.collectAsStateWithLifecycle()
        val aleem = names.firstOrNull { name ->
            name.transliteration == "Al-Aleem"
        }
        if (aleem != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PageInset, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShareCard(name = aleem, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Test
    fun settings() = render("settings", ThemeMode.LIGHT) {
        SettingsScreen(it, onBack = {})
    }

    private fun render(
        name: String,
        themeMode: ThemeMode,
        screen: @Composable (NamesViewModel) -> Unit,
    ) {
        val viewModel = NamesViewModel(app())
        composeRule.setContent {
            Names99Theme(themeMode = themeMode, textScale = 1f) {
                screen(viewModel)
            }
        }
        waitForNames(viewModel)
        saveScreenshot(name)
    }
}