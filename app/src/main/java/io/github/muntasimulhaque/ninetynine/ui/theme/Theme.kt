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
 * Whether the theme actually renders dark — the reader's choice, not the
 * system's (BLACK on a light phone must still draw light-mode system bars).
 * The fixed hero plates (hero card, quiz card, flashcard front, share card)
 * draw the same emerald in every theme, with no border — a deliberate
 * symmetry across the app's plates.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

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

    CompositionLocalProvider(
        LocalTextScale provides textScale,
        LocalDarkTheme provides darkTheme,
    ) {
        val motionScale = rememberAnimatorDurationScale()
        CompositionLocalProvider(LocalMotionScale provides motionScale) {
            MaterialTheme(
                colorScheme = colors,
                typography = appTypography(textScale),
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

private fun readAnimatorDurationScale(context: Context): Float =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
