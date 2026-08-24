package io.github.muntasimulhaque.ninetynine

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.ninetynine.data.ThemeMode
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.detail.DetailScreen
import io.github.muntasimulhaque.ninetynine.ui.home.HomeScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.MemorizeScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.QuizScreen
import io.github.muntasimulhaque.ninetynine.ui.theme.Names99Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Renders the five Play-listing screens directly (no app session, no adb taps)
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