package io.github.muntasimulhaque.ninetynine.daily

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroSubtext
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText

/**
 * The expanded daily notification wears the same plate as the hero card, the
 * share card and the widget: deep emerald, the Name in gold HAFS drawn by
 * Canvas — which shapes the vocalized text correctly through the platform's
 * text stack — via the same [arabicBitmap] fit the widget uses, the
 * transliteration in Spectral Light, and the epithet in Spectral Medium
 * Italic beneath, over the "NAME OF THE DAY" overline the hero card wears.
 *
 * Drawn in a fixed 16:9 design space at 3px per dp; the system scales the
 * picture to the notification's picture slot. The stack is centred like the
 * hero card's content. Rendering is best-effort: any failure returns null and
 * the caller falls back to the plain-text style, so the morning notification
 * can never be lost to a bad render.
 */
internal object DailyPlate {

    private const val WIDTH = 1080
    private const val HEIGHT = 608

    /** The design space is 360dp wide at 3px per dp — sp sizes multiply by this. */
    private const val PX_PER_SP = 3f

    private const val PAD = 84f

    fun render(context: Context, name: Name): Bitmap? = runCatching {
        val hafs = ResourcesCompat.getFont(context, R.font.kfgqpc_hafs_uthmanic)
            ?: return@runCatching null
        val light = ResourcesCompat.getFont(context, R.font.spectral_light)
            ?: return@runCatching null
        val medium = ResourcesCompat.getFont(context, R.font.spectral_medium)
            ?: return@runCatching null
        val mediumItalic = ResourcesCompat.getFont(context, R.font.spectral_mediumitalic)
            ?: return@runCatching null

        // The Name, stepped down until its whole line box — HAFS runs tall —
        // fits the plate: the widget's own fit logic, reused verbatim. The
        // height share leaves room for the overline above and the
        // transliteration and epithet below even in their worst case.
        val arabic = arabicBitmap(
            typeface = hafs,
            text = name.arabic,
            targetSp = 48f,
            maxWidthPx = WIDTH - PAD * 2,
            maxHeightPx = HEIGHT * 0.40f,
            pxPerSp = PX_PER_SP,
            color = HeroGold.toArgb(),
        ) ?: return@runCatching null

        // Overline — the hero card's "NAME OF THE DAY", tracked small caps
        // (labelMedium's 1.8sp on 11sp, in em units for Paint).
        val overline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = medium
            textSize = 11f * PX_PER_SP
            letterSpacing = 1.8f / 11f
            color = HeroGold.toArgb()
            textAlign = Paint.Align.CENTER
        }
        val overlineText = context.getString(R.string.notification_title).uppercase()
        val overlineBox = overline.fontMetrics.let { it.bottom - it.top }

        // Transliteration — Spectral Light, stepping down like FitText so the
        // longest name never clips. The 0.45 floor is FitText's own.
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = light
            color = HeroText.toArgb()
            textAlign = Paint.Align.CENTER
        }
        var nameSize = 24f * PX_PER_SP
        val nameFloor = 24f * 0.45f * PX_PER_SP
        namePaint.textSize = nameSize
        while (nameSize > nameFloor &&
            namePaint.measureText(name.transliteration) > WIDTH - PAD * 2
        ) {
            nameSize *= 0.95f
            namePaint.textSize = nameSize
        }
        val nameBox = namePaint.fontMetrics.let { it.bottom - it.top }

        // Epithet — up to two lines, centred, ellipsized like the hero card.
        val titleLayout = StaticLayout.Builder.obtain(
            name.title,
            0,
            name.title.length,
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = mediumItalic
                textSize = 14f * PX_PER_SP
                color = HeroSubtext.toArgb()
            },
            (WIDTH - PAD * 2).toInt(),
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.25f)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()

        val gapOverlineToArabic = 12f * PX_PER_SP
        val gapArabicToName = 6f * PX_PER_SP
        val gapNameToTitle = 6f * PX_PER_SP
        val stack = overlineBox + gapOverlineToArabic + arabic.height +
            gapArabicToName + nameBox + gapNameToTitle + titleLayout.height

        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(HeroContainer.toArgb())

        var y = (HEIGHT - stack) / 2f
        if (y < 24f) y = 24f

        canvas.drawText(overlineText, WIDTH / 2f, y - overline.fontMetrics.top, overline)
        y += overlineBox + gapOverlineToArabic
        canvas.drawBitmap(arabic, (WIDTH - arabic.width) / 2f, y, null)
        y += arabic.height + gapArabicToName
        canvas.drawText(name.transliteration, WIDTH / 2f, y - namePaint.fontMetrics.top, namePaint)
        y += nameBox + gapNameToTitle
        canvas.save()
        canvas.translate(PAD, y)
        titleLayout.draw(canvas)
        canvas.restore()

        bitmap
    }.getOrNull()
}
