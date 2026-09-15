package io.github.muntasimulhaque.ninetynine.daily

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Paints [argbColor] into a [widthPx] × [heightPx] bitmap whose corners round
 * off as a squircle: the same superellipse the app's SquircleShape clips the
 * app's own surfaces with (exponent n = 4, sampled at [SAMPLES_PER_CORNER]
 * points per corner rather than fitted with beziers, so the geometry is
 * exact). The widget's RemoteViews has no Compose shape engine, so the shape
 * is rasterized here and served as the plate's background image; the corners
 * match the system plates Samsung and Pixel launchers wear, and the launcher's
 * own circular mask clips nothing away because the superellipse sits just
 * inside the circle of the same radius.
 *
 * [radiusPx] is the corner radius in pixels: this device's system radius.
 * A zero or negative size returns null (the caller falls back to the flat
 * ColorProvider background), while an oversized radius only bends the corner
 * geometry back toward the rectangle, never to a missing plate.
 */
internal fun squirclePlateBitmap(
    widthPx: Int,
    heightPx: Int,
    radiusPx: Float,
    argbColor: Int,
): Bitmap? {
    if (widthPx <= 0 || heightPx <= 0) return null
    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val r = radiusPx.coerceIn(0f, min(w, h) / 2f)
    val n = 4f
    // Platform type: cannot be null; an allocation failure throws, which the
    // caller's guard turns into the flat-color fallback.
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    if (r <= 0f) {
        canvas.drawColor(argbColor)
        return bitmap
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argbColor }
    val path = Path()

    // Walk the outline clockwise exactly as SquircleShape.createOutline does:
    // a straight edge, a superellipse corner, a straight edge, a corner, and
    // so on. Each corner is sampled from its start seam to its end seam; the
    // superellipse is tangent to the edges at both seams, so there is no kink
    // anywhere on the plate's edge.
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

    canvas.drawPath(path, paint)
    return bitmap
}

/** Sub-pixel at every radius the app uses; cheap to pay once per render. */
private const val SAMPLES_PER_CORNER = 48

/**
 * The four corner walkers below are SquircleShape's sampled superellipse,
 * ported one-to-one onto an android.graphics.Path: the widget's RemoteViews
 * cannot wear a Compose shape, so the geometry is repeated rather than
 * shared. Direction and formula both matter: each loop runs from the corner's
 * start seam to its end seam so the arc lands exactly where the straight
 * edges already reached, and cos/sin are clamped to [0, 1] because at the
 * seam theta = π/2 the float32 π/2 rounds a hair above the true value: a
 * tiny negative cos() to a fractional power is NaN, which would poison the
 * whole path and blank the plate.
 */
private fun topLeft(path: Path, r: Float, n: Float) {
    val halfPi = (PI / 2).toFloat()
    val quad = 2f / n
    // Apex at (0, 0): the arc runs (0, r) -> (r, 0).
    for (i in 0..SAMPLES_PER_CORNER) {
        val theta = i.toFloat() / SAMPLES_PER_CORNER * halfPi
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

/**
 * This device's own widget corner radius, from the framework dimen Android 12
 * publishes so widgets can match the launcher's rounding (`16dp` on Pixel,
 * other values elsewhere). Read by name (the dimen is hidden, and OEM builds
 * may not carry it) with a fallback to the 20dp the widget has always used.
 * Called on every API level: the widget's squircle plate is painted at this
 * radius everywhere, so below 12 the 20dp fallback is what gives the plate
 * its corners at all. getDimension returns px; convert once here so callers
 * stay in dp.
 */
internal fun systemCornerRadius(context: Context): Dp = try {
    val id = context.resources.getIdentifier(
        "system_app_widget_background_radius",
        "dimen",
        "android",
    )
    if (id == 0) return 20.dp
    val density = context.resources.displayMetrics.density
    (context.resources.getDimension(id) / density).dp
} catch (_: Exception) {
    20.dp
}
