package io.github.muntasimulhaque.ninetynine.daily

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.ceil

/**
 * Renders [text] in [typeface] onto a transparent bitmap, stepping the size
 * down from [targetSp] until the whole line box (HAFS runs tall, and the
 * marks climb well above the letters) fits inside the given bounds. The
 * platform's text stack shapes the vocalized Arabic correctly (Canvas text
 * drawing goes through the same shaping the app's Compose text does), so the
 * widget's Name is finally set in the bundled HAFS rather than a system
 * approximation of it.
 *
 * Returns null when even the floor size cannot fit: the caller falls back to
 * the system-font Text path. A few pixels of slack pad each side, because
 * marks and swashes can exceed the advance width, and the baseline sits one
 * pixel in from the top so nothing kisses the edge.
 */
internal fun arabicBitmap(
    typeface: Typeface,
    text: String,
    targetSp: Float,
    maxWidthPx: Float,
    maxHeightPx: Float,
    pxPerSp: Float,
    color: Int,
): Bitmap? {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.color = color
        textAlign = Paint.Align.CENTER
    }
    var sizeSp = targetSp
    while (sizeSp > 8f) {
        paint.textSize = sizeSp * pxPerSp
        val width = paint.measureText(text)
        val metrics = paint.fontMetrics
        val height = metrics.bottom - metrics.top
        if (width <= maxWidthPx && height <= maxHeightPx) {
            val w = ceil(width + 8f).toInt().coerceAtLeast(1)
            val h = ceil(height + 2f).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawText(text, w / 2f, -metrics.top + 1f, paint)
            return bitmap
        }
        sizeSp -= maxOf(1f, sizeSp * 0.08f)
    }
    return null
}
