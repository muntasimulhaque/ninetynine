package io.github.muntasimulhaque.ninetynine.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The app's surfaces share two corner sizes — the full card (hero, flashcard,
 * quiz card, share card) and the smaller inset surface (the quiz options).
 *
 * Both are [SquircleShape], not `RoundedCornerShape`: a quarter circle welds
 * curvature 1/r onto a straight edge in one instant, which reads as a clipped
 * corner next to a system widget. The squircle ramps the bend up gradually, so
 * the edge flows into the corner with no seam. The radii are unchanged — only
 * the curve between them is smoother.
 *
 * (The bottom bar is the one exception: a wide, short plate in a squircle
 * reads as a rounded rectangle, while the bar's register is a true capsule —
 * see FloatingBar in MainActivity, which takes its shape explicitly rather
 * than from this map.)
 */
val AppShapes = Shapes(
    medium = SquircleShape(20.dp),
    large = SquircleShape(28.dp),
)