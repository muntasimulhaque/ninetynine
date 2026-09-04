package io.github.muntasimulhaque.ninetynine.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The system's animator duration scale (Settings → Developer options →
 * Animator duration scale, or the accessibility "Remove animations" toggle).
 * 1f = normal, 0f = animations off. Provided by [Names99Theme].
 */
val LocalMotionScale = staticCompositionLocalOf { 1f }

/**
 * One motion vocabulary for the whole app: nothing snaps, nothing bounces
 * hard — everything settles, like a page being laid down.
 *
 * When the system's animator scale is 0 (the user asked to remove
 * animations), every spec collapses to [snap] — the final state appears
 * instantly, with no intermediate frames.
 */
object Motion {
    /** Small state changes: tint, selection. */
    const val QUICK = 180

    /** Content appearing or turning: cards, pages, reveals. */
    const val GENTLE = 350

    /** Screen-level entrances. */
    const val CALM = 500

    /** Decelerating ease used for entrances. */
    val Settle: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * A tween that respects the system animator scale.
     * Generic so it satisfies FiniteAnimationSpec<Float>, <IntOffset>,
     * <IntSize>, AnimationSpec<Color>, etc. at the call site.
     */
    @Composable
    fun <T> tween(duration: Int, easing: Easing = FastOutSlowInEasing): FiniteAnimationSpec<T> {
        val scale = LocalMotionScale.current
        return spec(scale, duration, easing)
    }

    /** Non-composable variant for use inside coroutines and non-@Composable lambdas. */
    fun <T> spec(scale: Float, duration: Int, easing: Easing = FastOutSlowInEasing): FiniteAnimationSpec<T> {
        val safe = if (scale.isFinite()) scale.coerceAtLeast(0f) else 1f
        return if (safe == 0f) snap()
        else androidx.compose.animation.core.tween((duration * safe).toInt(), easing = easing)
    }

    /** Soft spring for tactile feedback (press scale, pops). */
    @Composable
    fun <T> soft(): FiniteAnimationSpec<T> {
        val scale = LocalMotionScale.current
        return softSpec(scale)
    }

    /** A little more life, for confirmation pops. */
    @Composable
    fun <T> lively(): FiniteAnimationSpec<T> {
        val scale = LocalMotionScale.current
        return livelySpec(scale)
    }

    /** Non-composable [soft] for coroutines and gesture callbacks. */
    fun <T> softSpec(scale: Float): FiniteAnimationSpec<T> {
        val safe = if (scale.isFinite()) scale else 1f
        return if (safe == 0f) snap()
        else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    }

    /** Non-composable [lively] for coroutines and gesture callbacks. */
    fun <T> livelySpec(scale: Float): FiniteAnimationSpec<T> {
        val safe = if (scale.isFinite()) scale else 1f
        return if (safe == 0f) snap()
        else spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
    }
}
