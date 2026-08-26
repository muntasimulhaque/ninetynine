package io.github.muntasimulhaque.ninetynine.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFF17624E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDAE9E0),
    onPrimaryContainer = Color(0xFF0E2E24),
    // Deep enough to clear WCAG AA on paper with margin (5.14:1) — this gold
    // carries the epithets and every tracked section label, not just ornament.
    // (#8C6A1B was 4.61:1, only 0.11 of headroom for the app's tightest
    // normal-text pair; #856216 is imperceptibly deeper.)
    secondary = Color(0xFF856216),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E7C3),
    onSecondaryContainer = Color(0xFF3D2F05),
    // Tertiary is the gold again. Material components that reach for it — the
    // time picker's AM/PM selector is the only one here — would otherwise draw
    // baseline lilac-pink in the middle of an emerald and gold palette.
    // Kept literally identical to secondary (the hardened #856216), so the
    // two slots can never drift apart.
    tertiary = Color(0xFF856216),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E7C3),
    onTertiaryContainer = Color(0xFF3D2F05),
    // A warm brick, not Material's fire-engine #B3261E: this red sits on paper
    // and has to live directly beneath the emerald "correct" fill in the quiz.
    // 7.48:1 on the page, and #4A1710 on the container is 11.40:1.
    error = Color(0xFF8E2F24),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF5DDD6),
    onErrorContainer = Color(0xFF4A1710),
    background = Color(0xFFFAF5EA),
    onBackground = Color(0xFF211C12),
    surface = Color(0xFFFFFCF4),
    onSurface = Color(0xFF211C12),
    surfaceVariant = Color(0xFFEFE7D6),
    onSurfaceVariant = Color(0xFF6B6353),
    // Component boundaries, not hairlines: this draws the unmarked "Mark as
    // learned" pill and the unanswered quiz options. At the old #B5AB97 it was
    // 2.09:1 and failed WCAG 1.4.11's 3:1; now 3.54:1 on the page and 3.21:1
    // in a dialog. outlineVariant below is decorative rules only, so it stays.
    outline = Color(0xFF8A8170),
    outlineVariant = Color(0xFFD8CFBB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF8EE),
    surfaceContainer = Color(0xFFF6F0E2),
    surfaceContainerHigh = Color(0xFFF0EADA),
    surfaceContainerHighest = Color(0xFFEAE3D2),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF93CBB5),
    onPrimary = Color(0xFF0E3529),
    primaryContainer = Color(0xFF1D4A3C),
    onPrimaryContainer = Color(0xFFD4EAE0),
    secondary = Color(0xFFD8BC6A),
    onSecondary = Color(0xFF3A2E07),
    secondaryContainer = Color(0xFF54431B),
    onSecondaryContainer = Color(0xFFF4E8C4),
    tertiary = Color(0xFFD8BC6A),
    onTertiary = Color(0xFF3A2E07),
    tertiaryContainer = Color(0xFF54431B),
    onTertiaryContainer = Color(0xFFF4E8C4),
    error = Color(0xFFE9A99A),
    onError = Color(0xFF5C1A10),
    errorContainer = Color(0xFF5C2419),
    onErrorContainer = Color(0xFFF7D8CF),
    background = Color(0xFF14120D),
    onBackground = Color(0xFFEAE2D1),
    surface = Color(0xFF1B1913),
    onSurface = Color(0xFFEAE2D1),
    surfaceVariant = Color(0xFF2B2820),
    onSurfaceVariant = Color(0xFFB7AE9C),
    outline = Color(0xFF837B69),
    outlineVariant = Color(0xFF3D392F),
    surfaceContainerLowest = Color(0xFF0E0D0A),
    surfaceContainerLow = Color(0xFF1D1B15),
    surfaceContainer = Color(0xFF1F1D17),
    surfaceContainerHigh = Color(0xFF2A2721),
    surfaceContainerHighest = Color(0xFF353127),
)

/** AMOLED variant: true-black background, near-black surfaces. */
val BlackColors = DarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0D0C0A),
    surfaceVariant = Color(0xFF171613),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0B0A08),
    surfaceContainer = Color(0xFF100F0C),
    surfaceContainerHigh = Color(0xFF1B1916),
    surfaceContainerHighest = Color(0xFF26231E),
)

/**
 * Fixed deep-emerald tones for the daily-name hero card and the share card,
 * identical in light and dark themes (matches the home-screen widget).
 */
val HeroContainer = Color(0xFF1F4E42)
val HeroGold = Color(0xFFD4B45A)
val HeroText = Color(0xFFF2EDE2)
val HeroSubtext = Color(0xFFBFD5CB)

/**
 * The English transliteration's teal, set apart from the full meaning that
 * follows it. The two once shared one ink and differed only in size; colour
 * now carries the distinction the way the widget's subtext does.
 *
 * Theme-aware, because the details page swings between light paper and
 * near-black: the light teal clears WCAG AA on the page (5.78:1 on the
 * background), the dark teal on the night page (10.65:1 on #14120D). Both
 * echo the pale mint of the fixed emerald plates (HeroSubtext) without
 * reading as the app's primary emerald, which stays reserved for the Arabic.
 */
val TransliterationTealLight = Color(0xFF1F6B63)
val TransliterationTealDark = Color(0xFF8FD0C0)

/**
 * The Name's gold on the name page — the one Arabic set on paper rather
 * than on an emerald plate. Warmer and lighter than `secondary`: at 52sp
 * the Name is WCAG large text, where 3:1 governs, so it can afford to be
 * the app's actual gold instead of the bronze `secondary` must be for
 * 11sp tracked labels. Measured against the page colors:
 * - NameGoldLight #A67F1A on paper #FAF5EA = 3.41:1 (large-text AA is 3:1;
 *   `secondary` holds 5.14:1 there for its small text)
 * - NameGoldDark #E2C36A on night #14120D = 10.92:1 (`secondary` 10.09:1)
 * (HeroGold #D4B45A itself is 1.84:1 on paper — plate-only by measure,
 * not just by convention.) Every other gold on paper — epithets, section
 * labels, chevrons, ticks — keeps `secondary`, whose job hasn't changed.
 */
val NameGoldLight = Color(0xFFA67F1A)
val NameGoldDark = Color(0xFFE2C36A)
