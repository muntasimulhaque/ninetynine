package io.github.muntasimulhaque.ninetynine.daily

import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.muntasimulhaque.ninetynine.MainActivity
import io.github.muntasimulhaque.ninetynine.data.NamesRepository
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroSubtext
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.util.DailyName

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
        // fit; minResizeHeight follows (daily_name_widget_info.xml).
        private val COMPACT = DpSize(110.dp, 48.dp) // Arabic only
        private val MEDIUM = DpSize(110.dp, 90.dp) // + transliteration
        private val TALL = DpSize(110.dp, 140.dp) // + title (wrapping)
        private val XTALL = DpSize(110.dp, 180.dp) // everything, larger

        /**
         * The system serif (Noto Naskh) misplaces the marks of the vocalized
         * الله over the lam-heh joint — the very bug that once forced stripping
         * them app-wide. The app's bundled HAFS renders it correctly, but the
         * widget and notification draw with system fonts, so they show the
         * plain form for this one word. The name is stored with a standing
         * fathah (dagger alif), so strip that form too.
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
            val height = LocalSize.current.height
            val showTransliteration = height >= MEDIUM.height
            val showTitle = height >= TALL.height
            val roomy = height >= XTALL.height

            // Glance cannot load bundled fonts, so Arabic and Latin fall back
            // to the system serif — which matches the app's book-like feel.
            val serif = FontFamily("serif")
            // One identity on every home screen: the emerald-and-gold of the
            // hero and share cards, deliberately NOT day/night switched. Bound
            // to the theme's constants rather than copied — if the plates are
            // ever tuned, the widget moves with them instead of drifting.
            val background = ColorProvider(HeroContainer)
            val gold = ColorProvider(HeroGold)
            val textColor = ColorProvider(HeroText)
            val subtextColor = ColorProvider(HeroSubtext)

            val arabicSize = when {
                roomy -> 38.sp
                // TALL and MEDIUM share one "small display" size (the app's
                // list rows are set at the same 30sp); only XTALL steps up.
                showTitle -> 30.sp
                showTransliteration -> 30.sp
                else -> 18.sp
            }

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
                    Text(
                        text = systemFontSafeArabic(name.arabic),
                        maxLines = 1,
                        style = TextStyle(
                            color = gold,
                            fontSize = arabicSize,
                            fontWeight = FontWeight.Normal,
                            fontFamily = serif,
                            textAlign = TextAlign.Center
                        )
                    )
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