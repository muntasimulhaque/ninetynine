package io.github.muntasimulhaque.ninetynine.daily

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
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
import androidx.glance.appwidget.cornerRadius
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
import io.github.muntasimulhaque.ninetynine.util.DailyName
import kotlin.math.ceil

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
        // thread — outside any runCatching the callers (MainActivity.onResume,
        // the WorkManager workers, TimeChangeReceiver) can wrap. A cold-start
        // hiccup here must be a skipped refresh, never a crash.
        runCatching { render(context) }
    }

    private suspend fun render(context: Context): Unit {
        val names = NamesRepository.load(context)
        val name = names.firstOrNull { it.number == DailyName.numberFor(System.currentTimeMillis()) }
        // Ask the device what radius ITS widgets round to, so this plate's
        // corners agree with the system's on every launcher. Falls back to
        // 20dp when an OEM does not publish the dimen.
        val corner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            systemCornerRadius(context)
        } else null
        // The app's own Arabic typeface, loaded once per render. Glance's Text
        // cannot wear a bundled font — RemoteViews text draws with system
        // fonts — so the Arabic is set in HAFS by drawing it into a bitmap
        // (Canvas shapes the vocalized text correctly through the platform's
        // text stack). Null only if the resource read fails, which should
        // never happen: the TTF ships in the APK. The fallback keeps the old
        // system-serif path so a widget always renders.
        val hafs = runCatching {
            ResourcesCompat.getFont(context, R.font.kfgqpc_hafs_uthmanic)
        }.getOrNull()
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
                roomy -> 38f
                // TALL and MEDIUM share one "small display" size (the app's
                // list rows are set at the same 30sp); only XTALL steps up.
                showTitle -> 30f
                showTransliteration -> 30f
                else -> 18f
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
            // cornerRadius is applied ONLY on API 31+ (Android 12+): on older
            // Android its no-op path breaks the clickable modifier that follows
            // it, so the widget rendered but never answered a tap (verified on
            // an Android 8.1 device/emulator). On API < 31 the corners stay
            // square; on 31+ they round through the system — at THIS device's
            // own system radius, not a hardcoded guess. The order matters —
            // cornerRadius must precede clickable, which is how API 31+ shipped.
            val plate = GlanceModifier
                .fillMaxSize()
                .background(background)
                .let { m -> if (corner != null) m.cornerRadius(corner) else m }
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

class DailyNameWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyNameWidget()
}

/**
 * This device's own widget corner radius, from the framework dimen Android 12
 * publishes so widgets can match the launcher's rounding (`16dp` on Pixel,
 * other values elsewhere). Read by name — the dimen is hidden, and OEM builds
 * may not carry it — with a fallback to the 20dp the widget has always used.
 * getDimension returns px; convert once here so callers stay in dp.
 */
private fun systemCornerRadius(context: Context): Dp = runCatching {
    val id = context.resources.getIdentifier(
        "system_app_widget_background_radius",
        "dimen",
        "android",
    )
    if (id == 0) return@runCatching 20.dp
    val density = context.resources.displayMetrics.density
    (context.resources.getDimension(id) / density).dp
}.getOrDefault(20.dp)
