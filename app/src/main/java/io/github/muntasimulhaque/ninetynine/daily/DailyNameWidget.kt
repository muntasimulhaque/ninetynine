package io.github.muntasimulhaque.ninetynine.daily

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.muntasimulhaque.ninetynine.MainActivity
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.NamesRepository
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroSubtext
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.util.DailyName
import kotlin.math.PI
import kotlinx.coroutines.CancellationException
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class DailyNameWidget : GlanceAppWidget() {

    companion object {        // Responsive height buckets: show only as many lines as fit completely.
        // The longest title (#39, 71 chars) wraps to three lines and ellipsizes
        // at the minimum 110dp width — the Arabic + transliteration above carry
        // the day's name, and the title's full sense is one tap away.
        //
        // COMPACT is 48dp, not 40: the names are fully vocalized (fatha ×109,
        // kasra ×67, shadda ×53, sukun ×86 in names.json), and a 22sp Noto
        // Naskh line box is taller than a 24dp content area, so the marks
        // clipped — worse at a system font scale above 1.0. At 18sp the marks
        // fit; minResizeHeight follows (daily_name_widget_info.xml). The
        // bitmap path below guarantees the fit regardless: the Arabic steps
        // down until its whole line box — HAFS runs tall — is inside the
        // bucket.
        private val COMPACT = DpSize(110.dp, 48.dp) // Arabic only
        private val MEDIUM = DpSize(110.dp, 90.dp) // + transliteration
        private val TALL = DpSize(110.dp, 140.dp) // + title (wrapping)
        private val XTALL = DpSize(110.dp, 180.dp) // everything, larger

        /**
         * The system serif (Noto Naskh) misplaces the marks of the vocalized
         * الله over the lam-heh joint — the very bug that once forced stripping
         * them app-wide. The app's bundled HAFS renders it correctly; the
         * widget now draws its Arabic in HAFS (see the bitmap path), but the
         * NOTIFICATION still draws with system fonts, so it shows the plain
         * form for this one word. The name is stored with a standing fathah
         * (dagger alif), so strip that form too. Also the widget's fallback
         * path, should the bitmap render ever be unavailable.
         */
        fun systemFontSafeArabic(text: String): String =
            text.replace("اللّٰه", "الله").replace("اللَّه", "الله")
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, MEDIUM, TALL, XTALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // This render executes inside a Glance SessionWorker on the main
        // thread — outside any guard the callers (MainActivity.onResume,
        // the WorkManager workers, TimeChangeReceiver) can wrap. A cold-start
        // hiccup here must be a skipped refresh, never a crash.
        try {
            render(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun render(context: Context): Unit {
        val names = NamesRepository.load(context)
        val name = names.firstOrNull { it.number == DailyName.numberFor(System.currentTimeMillis()) }
        // Ask the device what radius ITS widgets round to, so this plate's
        // corners agree with the system's on every launcher. Falls back to
        // 20dp when an OEM does not publish the dimen. Used on every API
        // level: the plate draws its own squircle at this radius, so even
        // pre-12 home screens get corners for the first time.
        val plateRadius = systemCornerRadius(context)
        // The app's own Arabic typeface, loaded once per render. Glance's Text
        // cannot wear a bundled font — RemoteViews text draws with system
        // fonts — so the Arabic is set in HAFS by drawing it into a bitmap
        // (Canvas shapes the vocalized text correctly through the platform's
        // text stack). Null only if the resource read fails, which should
        // never happen: the TTF ships in the APK. The fallback keeps the old
        // system-serif path so a widget always renders.
        val hafs = try {
            ResourcesCompat.getFont(context, R.font.kfgqpc_hafs_uthmanic)
        } catch (_: Exception) {
            null
        }
        // Always call provideContent, even when name is null. Without this, a
        // transient load failure (empty list from NamesRepository) would skip
        // the render entirely, and the Glance SessionWorker would consider the
        // update successful — but the widget would keep its OLD RemoteViews
        // and the OLD PendingIntent inside them. On Android 8.0–8.1 after an
        // update, that old PendingIntent is invalidated, so the widget would
        // render but never answer a tap. Rendering an empty-but-tappable plate
        // ensures the RemoteViews are always fresh and carry a valid
        // PendingIntent from the current APK. The text content reappears on
        // the next successful render (the same process start or the next
        // worker run).
        provideContent {
            val size = LocalSize.current
            val height = size.height
            val showTransliteration = height >= MEDIUM.height
            val showTitle = height >= TALL.height
            val roomy = height >= XTALL.height
            // Glance exposes no density composition local; the render's own
            // context carries the device's — and the reader's font scale, so
            // the Arabic grows with the system setting exactly as the Latin
            // sp sizes below it do.
            val density = context.resources.displayMetrics.density
            // The squircle plate below is drawn at the widget's real pixel
            // size in this same density.
            // fontScale reads 0 on a few misbehaved builds; a zero scale
            // would render the Name at zero size. 1.0 is the honest floor.
            val fontScale = context.resources.configuration.fontScale
                .takeIf { it > 0f } ?: 1f

            // Latin falls back to the system serif — close kin of Spectral,
            // and the accepted cost of RemoteViews. The Arabic does NOT fall
            // back: the Name must wear its own script everywhere it appears.
            val serif = FontFamily("serif")
            // One identity on every home screen: the emerald-and-gold of the
            // hero and share cards, deliberately NOT day/night switched. Bound
            // to the theme's constants rather than copied — if the plates are
            // ever tuned, the widget moves with them instead of drifting.
            val background = ColorProvider(HeroContainer)
            val gold = ColorProvider(HeroGold)
            val textColor = ColorProvider(HeroText)
            val subtextColor = ColorProvider(HeroSubtext)

            val arabicTargetSp = when {
                roomy -> ArabicSize.Widget.value
                // TALL and MEDIUM share one "small display" size (the app's
                // list rows are set at the same 30sp); only XTALL steps up.
                showTitle -> ArabicSize.Row.value
                showTransliteration -> ArabicSize.Row.value
                else -> ArabicSize.Compact.value
            }
            // The share of the content box the Arabic line may occupy — the
            // rest belongs to the transliteration and title lines below it.
            val arabicHeightFraction = when {
                roomy -> 0.50f
                showTransliteration -> 0.60f
                else -> 1f
            }
            // The plate pads 16dp horizontally and 8dp vertically; the
            // Arabic bitmap must fit inside what is left.
            val maxWidthPx = (size.width - 32.dp).value * density
            val maxHeightPx = (height - 16.dp).value * density * arabicHeightFraction

            // A single smooth plate: the emerald alone, no frame ring. The
            // bare emerald is the widget's edge on the home screen, exactly as
            // it was before the frame was added.
            //
            // The plate is drawn HERE, as its own bitmap, not delegated to a
            // corner-radius modifier: the emerald is painted into an ARGB_8888
            // bitmap the size of the widget's real pixel surface, through a
            // Path sampled from the same superellipse math as the app's
            // SquircleShape (exponent n = 4, SAMPLES_PER_CORNER = 48), so the
            // corners curve off in one continuous tangent-continuous sweep —
            // the shape Samsung and Pixel plates wear — at THIS device's own
            // system radius. systemCornerRadius is read on every API level
            // now: below 12 it still returns the 20dp fallback, so for the
            // first time the pre-12 plate has corners at all (cornerRadius
            // must never be applied there — its no-op path breaks the
            // clickable modifier that follows it, verified on Android 8.1;
            // here no such modifier sits between shape and clickable, because
            // the shape IS the background).
            //
            // The launcher still clips the whole widget with its own circular
            // mask at the same radius, which simply cuts nothing away — the
            // squircle sits just inside the circle — while launchers that
            // ignore the preferred radius see the old look, never a worse
            // one. The bitmap is built in this composition worker lambda,
            // since only LocalSize knows the real surface here; nothing is
            // cached across renders because responsive sizes change it. A
            // zero-size or failed allocation falls back to the flat
            // ColorProvider background — a square plate always beats no
            // plate.
            val plateBitmap = try {
                squirclePlateBitmap(
                    // DpSize's width/height are Dp; .value times the density
                    // is the real pixel surface. ceil so the plate never
                    // under-covers the node by a fraction of a pixel.
                    widthPx = ceil(size.width.value * density).toInt().coerceAtLeast(1),
                    heightPx = ceil(size.height.value * density).toInt().coerceAtLeast(1),
                    radiusPx = plateRadius.value * density,
                    argbColor = HeroContainer.toArgb(),
                )
            } catch (_: Exception) {
                null
            }
            val plate = GlanceModifier
                .fillMaxSize()
                .let { m ->
                    if (plateBitmap != null) {
                        m.background(ImageProvider(plateBitmap))
                    } else {
                        m.background(background)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(
                            ActionParameters.Key<Int>(MainActivity.EXTRA_NAME_NUMBER) to
                                (name?.number ?: 1)
                        )
                    )
                )

            Column(
                modifier = plate,
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (name != null) {
                    val arabicBitmap = hafs?.let {
                        arabicBitmap(
                            typeface = it,
                            text = name.arabic,
                            targetSp = arabicTargetSp,
                            maxWidthPx = maxWidthPx,
                            maxHeightPx = maxHeightPx,
                            pxPerSp = density * fontScale,
                            color = HeroGold.toArgb(),
                        )
                    }
                    if (arabicBitmap != null) {
                        Image(
                            provider = ImageProvider(arabicBitmap),
                            // The Name itself is the widget's content; a screen
                            // reader is told it in Latin, the way the
                            // notification's line pairs the two.
                            contentDescription = name.transliteration,
                            modifier = GlanceModifier
                                .width((arabicBitmap.width / density).dp)
                                .height((arabicBitmap.height / density).dp),
                            contentScale = ContentScale.FillBounds,
                        )
                    } else {
                        Text(
                            text = systemFontSafeArabic(name.arabic),
                            maxLines = 1,
                            style = TextStyle(
                                color = gold,
                                fontSize = arabicTargetSp.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = serif,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    if (showTransliteration) {
                        Text(
                            text = name.transliteration,
                            maxLines = 1,
                            style = TextStyle(
                                color = textColor,
                                fontSize = if (roomy) 18.sp else 16.sp,
                                fontFamily = serif,
                                textAlign = TextAlign.Center
                            ),
                            // The hero card pairs Arabic and transliteration
                            // at 6dp when there is room; 4dp when not.
                            modifier = GlanceModifier.padding(top = if (roomy) 6.dp else 4.dp)
                        )
                    }
                    if (showTitle) {
                        Text(
                            text = name.title,
                            // Two lines fit the TALL bucket with a 30sp Arabic;
                            // only the tallest bucket may wrap to three.
                            maxLines = if (roomy) 3 else 2,
                            style = TextStyle(
                                color = subtextColor,
                                fontSize = if (roomy) 14.sp else 12.sp,
                                fontStyle = FontStyle.Italic,
                                fontFamily = serif,
                                textAlign = TextAlign.Center
                            ),
                            modifier = GlanceModifier.padding(top = 4.dp)
                        )
                    }
                }
                // When name is null the widget shows an empty emerald plate
                // that is still tappable — this guarantees the RemoteViews
                // and PendingIntent are always refreshed, even after a
                // transient NamesRepository load failure. The text content
                // will appear on the next successful render.
            }
        }
    }
}

/**
 * Renders [text] in [typeface] onto a transparent bitmap, stepping the size
 * down from [targetSp] until the whole line box — HAFS runs tall, and the
 * marks climb well above the letters — fits inside the given bounds. The
 * platform's text stack shapes the vocalized Arabic correctly (Canvas text
 * drawing goes through the same shaping the app's Compose text does), so the
 * widget's Name is finally set in the bundled HAFS rather than a system
 * approximation of it.
 *
 * Returns null when even the floor size cannot fit — the caller falls back to
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

/**
 * Paints [argbColor] into a [widthPx] × [heightPx] bitmap whose corners round
 * off as a squircle — the same superellipse the app's SquircleShape clips the
 * app's own surfaces with (exponent n = 4, sampled at [SAMPLES_PER_CORNER]
 * points per corner rather than fitted with beziers, so the geometry is
 * exact). The widget's RemoteViews has no Compose shape engine, so the shape
 * is rasterized here and served as the plate's background image; the corners
 * match the system plates Samsung and Pixel launchers wear, and the launcher's
 * own circular mask clips nothing away because the superellipse sits just
 * inside the circle of the same radius.
 *
 * [radiusPx] is the corner radius in pixels — this device's system radius.
 * A zero or negative size returns null (the caller falls back to the flat
 * ColorProvider background), while an oversized radius only bends the corner
 * geometry back toward the rectangle, never to a missing plate.
 */
private fun squirclePlateBitmap(
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
 * ported one-to-one onto an android.graphics.Path — the widget's RemoteViews
 * cannot wear a Compose shape, so the geometry is repeated rather than
 * shared. Direction and formula both matter: each loop runs from the corner's
 * start seam to its end seam so the arc lands exactly where the straight
 * edges already reached, and cos/sin are clamped to [0, 1] because at the
 * seam theta = π/2 the float32 π/2 rounds a hair above the true value — a
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

class DailyNameWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyNameWidget()
}

/**
 * This device's own widget corner radius, from the framework dimen Android 12
 * publishes so widgets can match the launcher's rounding (`16dp` on Pixel,
 * other values elsewhere). Read by name — the dimen is hidden, and OEM builds
 * may not carry it — with a fallback to the 20dp the widget has always used.
 * Called on every API level: the widget's squircle plate is painted at this
 * radius everywhere, so below 12 the 20dp fallback is what gives the plate
 * its corners at all. getDimension returns px; convert once here so callers
 * stay in dp.
 */
private fun systemCornerRadius(context: Context): Dp = try {
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
