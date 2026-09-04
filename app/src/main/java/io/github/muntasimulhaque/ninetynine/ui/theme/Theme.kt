package io.github.muntasimulhaque.ninetynine.ui.theme

import android.app.Activity
import android.content.Context
import android.database.ContentObserver
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.muntasimulhaque.ninetynine.data.ThemeMode

/**
 * The reader's text-size preference, for text sized outside the type scale.
 * `ArabicText` takes explicit sp values, so without this the slider would move
 * the Latin and leave the Arabic behind.
 */
val LocalTextScale = staticCompositionLocalOf { 1f }

/**
 * The device factor: how far this device sits from the phone the type was set
 * on. sp type is physically identical on every screen — a 16sp line is ~2.5mm
 * on a phone and on a 10-inch tablet — which is right at hand-held phone
 * distance and small at the distance a large tablet is held. The factor
 * scales the whole book with the device: typography, Arabic, column caps and
 * gaps all multiply by [LocalTextScale], which carries the reader's scale ×
 * this factor, so a tablet prints the same book in a larger format with
 * identical proportions. Phones sit at 1.0 and are untouched.
 *
 * Read from smallest-width, not window width, so the factor belongs to the
 * device: a phone rotated to landscape keeps its factor, the way a book does
 * not re-set its type because you turned it sideways.
 */
val LocalDeviceFactor = staticCompositionLocalOf { 1f }

/** [LocalDeviceFactor] from the device's smallest window width, in dp. */
private fun deviceFactorFor(smallestWidthDp: Int): Float = when {
    smallestWidthDp >= 840 -> 1.25f // the 10-inch class
    smallestWidthDp >= 600 -> 1.125f // the 7-inch class
    else -> 1f                      // the phone the type was set on
}

/**
 * Whether the theme actually renders dark — the reader's choice, not the
 * system's (BLACK on a light phone must still draw light-mode system bars).
 * The fixed hero plates (hero card, quiz card, flashcard front, share card)
 * draw the same emerald in every theme, with no border — a deliberate
 * symmetry across the app's plates.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Whether the reader chose the AMOLED Black theme specifically. DARK and BLACK
 * both render dark ([LocalDarkTheme]), but BLACK's true-black page gives a
 * shadow nothing to darken, so a plate that lifts by tone — the bottom bar —
 * takes one container rung more there to read at the same perceived height.
 */
val LocalPureBlackTheme = staticCompositionLocalOf { false }

@Composable
fun Names99Theme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = when {
        themeMode == ThemeMode.BLACK -> BlackColors
        darkTheme -> DarkColors
        else -> LightColors
    }

    // The status-bar icons follow the theme the reader chose, not the one the
    // system is in — otherwise choosing Black on a light phone paints dark
    // icons onto a black bar and the clock disappears.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // One scale for the whole page: the reader's preference × the device
    // factor. Typography, Arabic, the column caps and the gaps all multiply
    // by it (see [LocalDeviceFactor]); the bottom bar's labels take the
    // device factor alone, so the reader's slider still cannot move them.
    // Sanitized: DataStore is trusted, but a restored backup can carry any
    // float, and a NaN scale would poison every sp size on the page.
    val deviceFactor = deviceFactorFor(LocalConfiguration.current.smallestScreenWidthDp)
    val safeText = (if (textScale.isFinite()) textScale else 1f).coerceIn(0.5f, 2f)
    val readingScale = safeText * deviceFactor

    CompositionLocalProvider(
        LocalTextScale provides readingScale,
        LocalDeviceFactor provides deviceFactor,
        LocalDarkTheme provides darkTheme,
        LocalPureBlackTheme provides (themeMode == ThemeMode.BLACK),
    ) {
        val motionScale = rememberAnimatorDurationScale()
        CompositionLocalProvider(LocalMotionScale provides motionScale) {
            MaterialTheme(
                colorScheme = colors,
                typography = appTypography(readingScale),
                shapes = AppShapes,
                content = content,
            )
        }
    }
}

/**
 * The system's animator duration scale, observed for as long as the theme
 * composes — not read once.
 *
 * The setting changes with no signal to a running app: the reader can flip
 * the accessibility "Remove animations" toggle, or step the developer-options
 * animator scale, while this app is open. Reading it once per composition
 * froze the choice at launch, and a reader who asked for no animations
 * mid-session kept receiving them until the process happened to die. A
 * ContentObserver on the setting's URI turns each change into a state write;
 * every Motion.* call site re-derives its specs from [LocalMotionScale] on
 * the next composition, so the app follows the toggle live.
 */
@Composable
private fun rememberAnimatorDurationScale(): Float {
    val context = LocalContext.current
    return produceState(initialValue = readAnimatorDurationScale(context), context) {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                value = readAnimatorDurationScale(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            true,
            observer,
        )
        // The setting may have moved between the initial read and registration.
        value = readAnimatorDurationScale(context)
        awaitDispose { context.contentResolver.unregisterContentObserver(observer) }
    }.value
}

private fun readAnimatorDurationScale(context: Context): Float {
    val raw = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    } catch (_: Exception) {
        1f
    }
    // A negative or non-finite scale would make Motion.spec build a tween
    // with a negative duration and crash; clamp to the meaningful range.
    return (if (raw.isFinite()) raw else 1f).coerceAtLeast(0f)
}
