package io.github.muntasimulhaque.ninetynine.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroSubtext
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import java.util.Locale

@Composable
internal fun DailyHeroCard(name: Name, onClick: () -> Unit) {
    // The card yields slightly under the finger, paper, not glass.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = Motion.soft(),
        label = "heroPress",
    )
    // clickable on the modifier rather than Card(onClick): M3's clickable card
    // takes no onClickLabel, and its content (a Name, an epithet, some Arabic)
    // never says what tapping it does. This way TalkBack offers "Open
    // today's name" instead of its bare "double-tap to activate", exactly as
    // the list rows already do. The card's own press ripple is preserved by
    // feeding clickable the same interaction source the scale animation reads.
    Card(
        modifier = Modifier
            // Horizontal 20dp puts the card on the same edge as the list
            // beneath it; 12dp is the sheet's own top air, so the card
            // never hugs the app bar.
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClickLabel = stringResource(R.string.cd_open_daily),
                onClick = { onClick() },
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = HeroContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.notification_title).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium,
                color = HeroGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(scaledGap(14.dp)))
            ArabicText(
                text = name.arabic,
                fontSize = ArabicSize.Panel,
                color = HeroGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            // The name is a proper noun and gets set whole. Left to wrap, a
            // long one at a large font scale breaks mid-word ("Al-Wa / asi'")
            // which is the one thing the app is careful never to do.
            FitText(
                text = name.transliteration,
                style = MaterialTheme.typography.displaySmall.copy(
                    textAlign = TextAlign.Center,
                ),
                color = HeroText,
                minScale = 0.45f,
            )
            Spacer(Modifier.height(2.dp))
            // Three lines: the hero and the notification's plate below both
            // wrap the epithet to at most three. On one line this cut the
            // meaning of the day in half, several of the 99 epithets do not
            // fit a phone at default size, so roughly one morning in eight the
            // app opened on "The Perfect Lord And Master Upon Whom Th…". The
            // card has the height to spare, and only the longest handful of
            // epithets ever reach the third line.
            Text(
                text = name.title,
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = HeroSubtext,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
