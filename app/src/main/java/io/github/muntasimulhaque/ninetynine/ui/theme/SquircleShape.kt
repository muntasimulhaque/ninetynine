package io.github.muntasimulhaque.ninetynine.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * A rectangle whose corners round off in a single continuous curve: not the
 * quarter-circle `RoundedCornerShape` welds onto its straight edges.
 *
 * ## Why this looks smoother
 *
 * A circular-arc corner is a perfect circle joined to a straight line. Where
 * the arc meets the line the curvature jumps from 0 to 1/r in a single instant,
 * and that kink is what makes a circular corner read as "a rectangle with
 * clipped corners" next to a system widget. A superellipse corner (the Lamé
 * curve `|x|^n + |y|^n = r^n` that iOS, Samsung One UI and Material You all use)
 * lets the curvature build up *gradually* from the straight edge into the
 * apex, so there is no seam to see. The bend is tangent-continuous with the
 * edges at every point, which is the measurable difference behind "smoother".
 *
 * [exponent] is the superellipse power `n`. `n = 2` is a plain circle (identical
 * to `RoundedCornerShape`); values above 2 push the corner toward a squarer,
 * more continuous profile. The default `4f` is a gentle, book-like squircle:
 * clearly smoother than a circle without the aggressive squareness of Apple's
 * `n = 5` icon shape. Tune one number to taste; lower rounds, higher sharpens.
 *
 * The corner is sampled rather than drawn with fitted beziers so the geometry
 * is exact; the ~48 samples per corner are sub-pixel at any radius and the
 * [Outline] is cached by Compose per size, so the cost is paid once.
 */
@Immutable
class SquircleShape(
    private val radius: Dp,
    private val exponent: Float = 4f,
) : CornerBasedShape(
    topStart = CornerSize(radius),
    topEnd = CornerSize(radius),
    bottomEnd = CornerSize(radius),
    bottomStart = CornerSize(radius),
) {

    private companion object {
        /** Sub-pixel at every radius the app uses; cheap to cache. */
        const val SAMPLES_PER_CORNER = 48
    }

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        // `CornerBasedShape` has already resolved the dp radii to px here.
        // All four corners share one radius in this shape, so take the largest.
        val r = min(
            maxOf(topStart, topEnd, bottomEnd, bottomStart),
            min(size.width, size.height) / 2f,
        )
        if (size.width <= 0f || size.height <= 0f || r <= 0f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }

        val w = size.width
        val h = size.height
        val n = exponent.coerceAtLeast(2f)
        val path = Path()

        // Walk the outline clockwise: a straight edge, a superellipse corner,
        // a straight edge, a corner, and so on. Each corner is sampled from
        // its start seam to its end seam; the superellipse is tangent to the
        // edges at both seams, so there is no kink anywhere.
        path.moveTo(r, 0f)
        path.lineTo(w - r, 0f)
        topRight(path, w, r, n)          // (w - r, 0) -> (w, r)
        path.lineTo(w, h - r)
        bottomRight(path, w, h, r, n)    // (w, h - r) -> (w - r, h)
        path.lineTo(r, h)
        bottomLeft(path, h, r, n)        // (r, h) -> (0, h - r)
        path.lineTo(0f, r)
        topLeft(path, r, n)              // (0, r) -> (r, 0)
        path.close()

        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = SquircleShape(radius, exponent)

    private fun topLeft(path: Path, r: Float, n: Float) {
        val halfPi = (PI / 2).toFloat()
        val quad = 2f / n
        // Apex at (0, 0): superellipse (r, 0) -> (0, r), then (0, r) -> (r, 0).
        // The loop runs 0..SAMPLES so both ends land exactly on the seams the
        // straight edges already reached, no diagonal jump where they meet.
        for (i in 0..SAMPLES_PER_CORNER) {
            val theta = i.toFloat() / SAMPLES_PER_CORNER * halfPi
            // Clamp to [0,1]: at the seam theta = π/2 the float32 π/2 rounds a
            // hair above the true value, so cos() is a tiny negative and a
            // fractional power of a negative base is NaN, which would poison
            // the whole path and blank the clipped surface. Mathematically
            // cos/sin are in [0,1] here; the clamp only kills the rounding.
            val xs = r * cos(theta).coerceIn(0f, 1f).pow(quad)
            val ys = r * sin(theta).coerceIn(0f, 1f).pow(quad)
            path.lineTo(r - xs, r - ys)
        }
    }

    private fun topRight(path: Path, w: Float, r: Float, n: Float) {
        val halfPi = (PI / 2).toFloat()
        val quad = 2f / n
        // Apex at (w, 0), travelling from the top seam to the right seam.
        for (i in 0..SAMPLES_PER_CORNER) {
            val theta = (1f - i.toFloat() / SAMPLES_PER_CORNER) * halfPi
            val xs = r * cos(theta).coerceIn(0f, 1f).pow(quad)
            val ys = r * sin(theta).coerceIn(0f, 1f).pow(quad)
            path.lineTo(w - r + xs, r - ys)
        }
    }

    private fun bottomRight(path: Path, w: Float, h: Float, r: Float, n: Float) {
        val halfPi = (PI / 2).toFloat()
        val quad = 2f / n
        // Apex at (w, h), from the right seam to the bottom seam.
        for (i in 0..SAMPLES_PER_CORNER) {
            val theta = i.toFloat() / SAMPLES_PER_CORNER * halfPi
            val xs = r * cos(theta).coerceIn(0f, 1f).pow(quad)
            val ys = r * sin(theta).coerceIn(0f, 1f).pow(quad)
            path.lineTo(w - r + xs, h - r + ys)
        }
    }

    private fun bottomLeft(path: Path, h: Float, r: Float, n: Float) {
        val halfPi = (PI / 2).toFloat()
        val quad = 2f / n
        // Apex at (0, h), from the bottom seam to the left seam.
        for (i in 0..SAMPLES_PER_CORNER) {
            val theta = (1f - i.toFloat() / SAMPLES_PER_CORNER) * halfPi
            val xs = r * cos(theta).coerceIn(0f, 1f).pow(quad)
            val ys = r * sin(theta).coerceIn(0f, 1f).pow(quad)
            path.lineTo(r - xs, h - r + ys)
        }
    }
}