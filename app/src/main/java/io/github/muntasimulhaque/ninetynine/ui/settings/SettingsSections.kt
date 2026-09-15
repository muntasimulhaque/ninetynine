package io.github.muntasimulhaque.ninetynine.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.data.ThemeMode
import io.github.muntasimulhaque.ninetynine.ui.theme.BlackColors
import io.github.muntasimulhaque.ninetynine.ui.theme.DarkColors
import io.github.muntasimulhaque.ninetynine.ui.theme.LightColors
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageRule

/**
 * The space and rule that separate one group of choices from the next.
 * Controls that carry their own touch padding pass a smaller [top].
 */
@Composable
internal fun SectionBreak(top: Dp = 30.dp, bottom: Dp = 30.dp) {
    Spacer(Modifier.height(top))
    PageRule()
    Spacer(Modifier.height(bottom))
}

/**
 * One theme, chosen typographically: the current one steps up in weight and
 * ink and takes a gold check. No radio, no container.
 */
@Composable
internal fun ThemeOption(
    mode: ThemeMode,
    labelRes: Int,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val selected = current == mode
    // Fading the check keeps the row from shifting as the choice moves.
    val checkAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.tween(Motion.QUICK),
        label = "themeCheck",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // selectable() is a foundation modifier; it applies no M3
            // minimum-target size, and the row's height is computed from its
            // text: at the reader slider's 0.85 floor it computes to ~44dp.
            // No-op at the default scale; a guarantee everywhere below it.
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                onClick = { onSelect(mode) },
                role = Role.RadioButton,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSwatch(mode)
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { alpha = checkAlpha },
        )
    }
}

/**
 * A theme in miniature: its paper, and its ink as a bead: the eye picks
 * before the mind reads. System wears both papers split, because it is
 * whichever the device is in; its bead follows the theme actually rendering.
 * Purely visual: the row above carries the name and the state for readers.
 */
@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    val ink = when (mode) {
        ThemeMode.LIGHT -> LightColors.primary
        ThemeMode.DARK, ThemeMode.BLACK -> DarkColors.primary
        ThemeMode.SYSTEM -> MaterialTheme.colorScheme.primary
    }
    // Dark and Black papers differ by ~8% lightness (#14120D vs #000000),
    // which a 22dp circle cannot show, side by side the two dark options
    // read as one choice. The Black swatch takes a firmer ring so the
    // deeper theme is distinguishable at a glance: true black is the
    // switched-off display, and the ring marks it.
    val ringColor = if (mode == ThemeMode.BLACK) MaterialTheme.colorScheme.outline
    else MaterialTheme.colorScheme.outlineVariant
    val ringWidth = if (mode == ThemeMode.BLACK) 1.5.dp else 1.dp
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(ringWidth, ringColor, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (mode) {
            ThemeMode.SYSTEM -> Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().background(LightColors.background))
                Box(Modifier.weight(1f).fillMaxHeight().background(DarkColors.background))
            }
            ThemeMode.LIGHT -> Box(Modifier.fillMaxSize().background(LightColors.background))
            ThemeMode.DARK -> Box(Modifier.fillMaxSize().background(DarkColors.background))
            ThemeMode.BLACK -> Box(Modifier.fillMaxSize().background(BlackColors.background))
        }
        // A 1dp mat of the page's own surface around the bead: the ink never
        // touches either paper directly, so it reads cleanly even on System's
        // split circle, where the bead straddles light and dark halves at once.
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(8.dp).background(ink, CircleShape))
        }
    }
}
