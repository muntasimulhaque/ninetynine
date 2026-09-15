package io.github.muntasimulhaque.ninetynine.ui.theme.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalDarkTheme
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalPureBlackTheme

/**
 * The floating bar's vessel: the tabs or actions themselves go inside; this
 * owns the floating, scroll-under plate: the capsule, halo and transparent
 * gesture strip. Shared verbatim by the main tab bar (BottomBar) and the
 * name page's capsule (DetailNavPlate) so the two cannot drift. The flat
 * variant that once lived beside it was discarded when the floating capsule
 * was chosen (see plan-of-record). The plate wears the page's own paper
 * colour (a floating sheet, not a separate band), and its ends are true
 * capsule/pill arcs ([RoundedCornerShape] at 50%, so the radius is always
 * half the bar's height: semicircular ends, exactly the Uber/Galaxy
 * register) rather than the superellipse [SquircleShape] the cards wear,
 * whose flatter corners read as a rounded rectangle on a wide short plate.
 * One construction in every theme: a paper plate lifted by a soft halo, no
 * borders anywhere (the hero plates' own symmetry): with only the colours
 * changing: light keeps the page's own paper; dark lifts the plate a
 * container rung above the page, because a shadow is black paint and on
 * near-black paper the tone is what reads; BLACK takes one rung more, its
 * true-black page leaving the halo (drawn there too, harmlessly) nothing to
 * darken.
 */
@Composable
fun FloatingBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = LocalDarkTheme.current
    val pureBlack = LocalPureBlackTheme.current
    // A capsule is CIRCULAR arcs (radius = half the short side), not a
    // superellipse. SquircleShape (n=4) even at max radius keeps its ends
    // flatter than a semicircle, so a wide plate still reads rounded-rt. A
    // percent corner size of 50% resolves to half the plate's height (the
    // short side), which makes the two ends meet in a true semicircle: a
    // stadium. RoundedCornerShape is correct here precisely because the bar
    // is NOT a card, cards keep the smooth squircle, the pill is a capsule
    // by definition.
    val plateShape = RoundedCornerShape(50)
    // The plate's paper, per theme: the page's own in light; a container rung
    // above it in dark, a shadow is black paint, so on near-black paper the
    // tone is what lifts the plate (Material's own dark-elevation grammar).
    // BLACK needs `surfaceContainerHigh` rather than the rung below it: on a
    // true-black page there is no shadow to read and #100F0C sits so close to
    // the page that the plate stops reading as a sheet at all. No theme draws
    // a border: light never had one, and the dark hairline it once wore was
    // the old stand-in for exactly this lift.
    val plateColor = when {
        pureBlack -> MaterialTheme.colorScheme.surfaceContainerHigh
        dark -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.background
    }
    // The Uber halo: not shadowElevation (directional, smudgy on paper)
    // but the plate's own outline, blurred: see [softHalo]. Softer and a
    // touch stronger than a Material elevation so it reads as a floating
    // sheet, the way Uber's does. The ink is the theme's own shadow colour:
    // the near-black surface ink in light, plain black in dark (at a higher
    // alpha, since it must darken an already-dark page); on BLACK it falls
    // invisible and the elevated tone above carries the plate alone.
    val plateModifier = Modifier.softHalo(
        shape = plateShape,
        color = if (dark) Color.Black.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
        radius = 28.dp,
        offsetY = 8.dp,
    )
    Column(modifier.fillMaxWidth()) {
        Box(
            // fillMaxWidth, then padding, then wrapContentWidth, then the cap,
            // barMeasure()'s own order. The squircle spans the padded width,
            // the cap binds only on wide screens, and the 14dp margins are the
            // plate's float (a Box paints nothing of its own).
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 12.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = pageMeasure()),
        ) {
            Surface(
                shape = plateShape,
                color = plateColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(plateModifier),
            ) {
                content()
            }
        }
        // Transparent gesture strip: the plate floats above it and nothing
        // draws here, so the system handle sits on the page itself.
        Spacer(Modifier.navigationBarsPadding().height(2.dp))
    }
}

/**
 * A soft, even halo: the shadow a floating plate wears on paper. Not
 * [androidx.compose.material3.Surface]'s shadowElevation, which is directional
 * (it lights from above) and reads smudgy on a flat page; that is the very
 * thing that made the old bar's shadow look heavy. Instead the plate's own
 * [shape] outline is drawn into a [android.graphics.Paint] whose
 * [BlurMaskFilter] spreads it outward evenly, so the plate appears to lift off
 * the page rather than cast a hard shadow. Drawn in every theme: near-black
 * surface ink in light, black in dark: where on the AMOLED Black page it
 * falls invisible and the elevated plate tone carries the lift alone.
 */
private fun Modifier.softHalo(
    shape: Shape,
    color: Color,
    radius: Dp,
    offsetY: Dp,
): Modifier = drawBehind {
    // The outline lives on the shape at the node's own size. Both outline
    // kinds the app's shapes produce are handled: Generic (SquircleShape's
    // sampled path) is drawn as a path, and Rounded (RoundedCornerShape's
    // capsule) is drawn as a round rect with its own resolved corner radius,
    // never a boxy fallback. Drawing into the node's own canvas keeps exact,
    // cache-friendly geometry with no extra allocation.
    val outline = shape.createOutline(size, layoutDirection, this)
    val blurRadius = radius.toPx()
    val shift = offsetY.toPx()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        native.save()
        native.translate(0f, shift)
        when (outline) {
            is Outline.Generic -> native.drawPath(outline.path.asAndroidPath(), paint)
            is Outline.Rounded -> {
                // The outline's RoundRect is already in px (createOutline gets
                // the size in px), so its corner radius needs no conversion. A
                // capsule's four corners share one radius; the packed value on
                // the top-left corner is as good as any.
                val rr = outline.roundRect
                val radius = rr.topLeftCornerRadius
                native.drawRoundRect(
                    android.graphics.RectF(rr.left, rr.top, rr.right, rr.bottom),
                    radius.x,
                    radius.y,
                    paint,
                )
            }
            else -> Unit
        }
        native.restore()
    }
}
