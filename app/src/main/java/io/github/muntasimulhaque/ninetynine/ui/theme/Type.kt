package io.github.muntasimulhaque.ninetynine.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.ninetynine.R

/**
 * Arabic everywhere in the app: KFGQPC Uthmanic Script HAFS — the typeface of
 * the Madinah Mushaf, published free by the King Fahd Glorious Quran Printing
 * Complex (license in assets/fonts/). Single weight; bundled unmodified.
 */
val ArabicFamily = FontFamily(
    Font(R.font.kfgqpc_hafs_uthmanic, FontWeight.Normal),
)

/** Latin text identity (SIL Open Font License). */
val SpectralFamily = FontFamily(
    Font(R.font.spectral_light, FontWeight.Light),
    Font(R.font.spectral_regular, FontWeight.Normal),
    Font(R.font.spectral_medium, FontWeight.Medium),
    Font(R.font.spectral_semibold, FontWeight.SemiBold),
    Font(R.font.spectral_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.spectral_mediumitalic, FontWeight.Medium, FontStyle.Italic),
)

/*
 * The type system carries the whole hierarchy: large jumps in size, a light
 * weight for display text, semibold for headings, italics for the poetic
 * lines, and wide-tracked small caps for labels. Color only accents it.
 */
private val BaseTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Light,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp,
    ),
    /**
     * The card transliteration.
     *
     * Was 34sp and completely unused — a dead slot at the top of the display
     * ramp — while the hero, quiz, flashcard and share cards all needed a Light
     * face *between* titleLarge's 18 and displayMedium's 28, and the scale did
     * not have one. Setting those cards at displayMedium left the Arabic only
     * 1.43x the Latin, so on the home card the eye landed on the
     * transliteration and the Name read as a caption above it. At 24 the same
     * pairing is 2.0x and the Name leads, with no change to the Arabic.
     */
    displaySmall = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Light,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    ),
    // Tracked small caps: use with .uppercase(Locale.ROOT) for overlines
    // like "NAME OF THE DAY" (ROOT — the default-locale form renders a
    // dotted İ on Turkish devices).
    labelMedium = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SpectralFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
    ),
)

private fun TextStyle.scaled(factor: Float): TextStyle =
    copy(
        fontSize = fontSize * factor,
        lineHeight = lineHeight * factor,
        letterSpacing = if (letterSpacing.isSpecified) letterSpacing * factor else letterSpacing,
    )

/** Typography with every size multiplied by the user's text-scale preference. */
fun appTypography(scale: Float): Typography {
    if (scale == 1f) return BaseTypography
    return Typography(
        displayLarge = BaseTypography.displayLarge.scaled(scale),
        displayMedium = BaseTypography.displayMedium.scaled(scale),
        displaySmall = BaseTypography.displaySmall.scaled(scale),
        headlineLarge = BaseTypography.headlineLarge.scaled(scale),
        headlineMedium = BaseTypography.headlineMedium.scaled(scale),
        headlineSmall = BaseTypography.headlineSmall.scaled(scale),
        titleLarge = BaseTypography.titleLarge.scaled(scale),
        titleMedium = BaseTypography.titleMedium.scaled(scale),
        titleSmall = BaseTypography.titleSmall.scaled(scale),
        bodyLarge = BaseTypography.bodyLarge.scaled(scale),
        bodyMedium = BaseTypography.bodyMedium.scaled(scale),
        bodySmall = BaseTypography.bodySmall.scaled(scale),
        labelLarge = BaseTypography.labelLarge.scaled(scale),
        labelMedium = BaseTypography.labelMedium.scaled(scale),
        labelSmall = BaseTypography.labelSmall.scaled(scale),
    )
}
