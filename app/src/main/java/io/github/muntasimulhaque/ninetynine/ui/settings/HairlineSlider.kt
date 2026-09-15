package io.github.muntasimulhaque.ninetynine.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import kotlin.math.roundToInt

internal const val SCALE_MIN = 0.85f
internal const val SCALE_MAX = 1.4f

/** A gold bead on a hairline, the Material slider stripped to the app's line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HairlineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val gold = MaterialTheme.colorScheme.secondary
    // Same ruling as HairlineProgress: a meaningful hairline carries `outline` (3:1), not `outlineVariant` (1.42:1).
    val track = MaterialTheme.colorScheme.outline
    val fraction = ((value - SCALE_MIN) / (SCALE_MAX - SCALE_MIN)).coerceIn(0f, 1f)
    val label = stringResource(R.string.text_size)
    val percent = stringResource(R.string.percent, (value * 100).roundToInt())
    // The custom thumb below replaces Material's whole thumb slot, which is
    // where its focus ring was drawn, so the ring has to be drawn by hand
    // here, or keyboard users get no indication at all on the one control
    // in the app that takes keyboard input.
    var focused by remember { mutableStateOf(false) }
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = SCALE_MIN..SCALE_MAX,
        // A bare Slider announces "seek control, 27 percent" with no subject
        // and no unit; the hairline also leaves only a 16dp focus rectangle.
        modifier = Modifier
            .heightIn(min = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                contentDescription = label
                stateDescription = percent
            },
        thumb = {
            Box(
                Modifier
                    .size(14.dp)
                    .background(gold, CircleShape)
                    .then(
                        if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    )
            )
        },
        track = { _ ->
            Box(Modifier.fillMaxWidth().height(1.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(track)
                )
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(1.dp)
                        .background(gold)
                )
            }
        },
    )
}
