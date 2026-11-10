package io.github.muntasimulhaque.ninetynine.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroGold
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroSubtext
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroText
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalTextScale
import io.github.muntasimulhaque.ninetynine.ui.theme.SquircleShape
import io.github.muntasimulhaque.ninetynine.ui.theme.appTypography
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.MarkSeal
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageInset
import io.github.muntasimulhaque.ninetynine.util.ShareText
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sending a name: the plate it was read on, whole, and the words it carried.
 *
 * The sheet shows the card complete at every meaning length (see [CardPreview])
 * and offers the two ways out side by side, the plate filled and the words
 * outlined. Owner decision, 1.37, on both counts: a preview that has to be
 * scrolled does not preview, and a row of two actions is a choice of artifact
 * rather than a hierarchy with a footnote under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(name: Name, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val wordmark = stringResource(R.string.store_title)
    var sharing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PageInset)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.share_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            CardPreview(
                name = name,
                graphicsLayer = graphicsLayer,
                modifier = Modifier
                    .fillMaxWidth()
                    // fill = false: a plate shorter than the room keeps its
                    // own height and the sheet closes up around it, instead
                    // of the sheet holding empty paper under the card.
                    .weight(1f, fill = false),
            )
            Spacer(Modifier.height(20.dp))
            // The two ways out, side by side (owner decision, 1.37). The
            // plate is the artifact, the words are what most contexts can
            // take, so the row keeps the hierarchy while showing them as
            // siblings: image filled, text outlined, equal widths, both on
            // the 52dp waist the app's other action rows wear.
            //
            // Neither carries a glyph. Every other button in the app is
            // words only (a deck's end, the quiz's next, the flashcards'
            // verdicts), and the share mark is already the top bar's way into
            // this sheet, so repeating it here added width and no meaning.
            //
            // Fitted, not wrapped: half of a 320dp phone's 272dp column is
            // ~130dp, and labelLarge is 13sp, so "Share image" is ~120dp of
            // text and padding at the design scale and outgrows that slot at
            // the reader's 1.4, with the system font scale multiplying on
            // top. FitText caps that growth instead of wrapping one label
            // onto a second line, which would leave two buttons of two
            // heights in a row that is meant to read as a pair.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    enabled = !sharing,
                    onClick = {
                        scope.launch {
                            sharing = true
                            try {
                                val bitmap = graphicsLayer.toImageBitmap()
                                shareNameImage(context, bitmap, name)
                                onDismiss()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    R.string.share_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } finally {
                                sharing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    FitText(
                        text = stringResource(R.string.share_image),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalContentColor.current,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                // The card is the artifact, but most share contexts want the
                // words themselves (a caption, a quote, a note), so the sheet
                // offers the plain text beside the plate.
                OutlinedButton(
                    onClick = {
                        val sent = shareNameText(context, ShareText.build(name, wordmark))
                        if (sent) onDismiss()
                        // Its own message: a text-share failure is not an
                        // image failure, and the toast must name the thing
                        // that failed.
                        else Toast.makeText(
                            context,
                            R.string.share_text_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    FitText(
                        text = stringResource(R.string.share_text),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalContentColor.current,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

/**
 * The plate, whole, at whatever size the sheet can hold.
 *
 * The card is composed at its natural size (the sheet's own column width, the
 * reader's text scale pinned to the design scale) and the drawn result is
 * scaled into the room left over: never clipped, never scrolled. Long meanings
 * therefore arrive small, which is the trade this rule buys: the reader sees
 * the whole card they are about to send, seals and frame included. The old
 * preview grew past the sheet and had to be dragged, which told the reader
 * less and, because the sheet's only resting place is Expanded, fought the
 * sheet's own drag hard enough to need a nested-scroll dam.
 *
 * The scale is applied to the PLACEMENT, deliberately not to the recording:
 * `graphicsLayer` records the card's own draw at its natural size, so
 * `toImageBitmap` still exports the plate at 1:1 whatever the preview had to
 * shrink to. Folding the scale into the recording chain would quietly ship
 * squashed images.
 *
 * One measurement pass, no first-frame flash: the natural height is measured
 * unconstrained before the layout reports the scaled height, so the slot and
 * the drawing agree on their very first frame.
 */
@Composable
private fun CardPreview(
    name: Name,
    graphicsLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val width = constraints.maxWidth
        val room = constraints.maxHeight
        val card = subcompose(PreviewCard) {
            // The exported image is a public artifact, render at the
            // design-intended scale regardless of the reader's slider.
            // The theme wraps the recording Box (not the other way round)
            // so the composition settles before the draw phase records it.
            CompositionLocalProvider(LocalTextScale provides 1f) {
                MaterialTheme(typography = appTypography(1f)) {
                    Box(
                        modifier = Modifier.drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        },
                    ) {
                        ShareCard(name = name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }.first().measure(
            Constraints(
                minWidth = width,
                maxWidth = width,
                // The height is the thing being fitted, so it is measured
                // against nothing: the card is laid out as tall as it reads.
                maxHeight = Constraints.Infinity,
            )
        )
        // Never enlarged: this is a preview of an artifact, not a zoomed
        // picture, and a card that already fits keeps its own size.
        val scale = if (room == Constraints.Infinity) {
            1f
        } else {
            (room.toFloat() / card.height.coerceAtLeast(1)).coerceAtMost(1f)
        }
        layout(width, (card.height * scale).roundToInt()) {
            card.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                // About the top centre: the card's own top stays where the
                // layout put it, so the title above and the card do not drift
                // apart as the scale changes from name to name.
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
        }
    }
}

/** One slot: the card is the only thing this layout composes. */
private const val PreviewCard = "share-card"

/**
 * The exported card: deep emerald + gold, identical to the widget/hero
 * identity, with a fine gold frame inside: like a printed plate.
 */
@Composable
internal fun ShareCard(name: Name, modifier: Modifier = Modifier) {
    // One hairline gold rule serves the whole plate: the frame around the card
    // and the seal around the mark are drawn with the identical stroke.
    val frameGold = HeroGold.copy(alpha = 0.4f)
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = HeroContainer),
    ) {
        Box(Modifier.padding(10.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = frameGold,
                        // Concentric with the card's 28dp squircle corner at a
                        // 10dp inset, so the frame reads as one line sitting
                        // inside the plate rather than a corner of its own.
                        shape = SquircleShape(18.dp),
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArabicText(
                    text = stringResource(R.string.basmala),
                    fontSize = ArabicSize.Caption,
                    color = HeroSubtext,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                ArabicText(
                    text = name.arabic,
                    fontSize = ArabicSize.Card,
                    color = HeroGold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                FitText(
                    text = name.transliteration,
                    style = MaterialTheme.typography.displaySmall,
                    // The transliteration's teal sets it apart from the meaning
                    // beneath it, which stays in the card's light ink.
                    color = HeroSubtext,
                    minScale = 0.45f,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = name.meaning,
                    // The whole meaning, always; very long ones step down a size.
                    style = if (name.meaning.length > 450) MaterialTheme.typography.bodySmall
                    else MaterialTheme.typography.bodyMedium,
                    color = HeroText,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                // Maker's mark: the app's square Kufic الله struck as a seal. The
                // circle repeats the card's own gold hairline, so the mark reads
                // as the logo rather than as part of the line beside it.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarkSeal(
                        ringColor = frameGold,
                        markColor = HeroGold,
                        size = 26.dp,
                        markSize = 12.dp,
                    )
                    Spacer(Modifier.width(9.dp))
                    // The store's name, not the app's own. This image is the
                    // only surface a stranger sees, so it carries the title
                    // they can type into Play and actually find: see the note
                    // over store_title in strings.xml.
                    //
                    // Fitted, not fixed: tracked small caps at 11sp put
                    // "THE NINETY NINE NAMES OF ALLAH" at 262dp (18.880 em
                    // plus 30 characters of 1.8sp tracking) against the 246dp
                    // left beside the seal on a Pixel 4 (which is 393dp wide,
                    // not 411: 1080px at density 440), so it sits at 0.94 even
                    // there. The card follows the screen, so the room is always
                    // the screen width less 147dp of chrome (48 sheet, 20 box,
                    // 44 inner padding, 26 seal, 9 gap) giving 213dp on a
                    // 360dp phone (0.81) and 173dp on a 320dp one (0.66). A
                    // floor of 0.45 clears the last of those with room to
                    // spare, and the system-font-scale-2.0 case as well (it
                    // needs 0.47 on a Pixel 4, and the floor is in sp, so the
                    // system scale multiplies the rendered width).
                    //
                    // It is the longest this wordmark has been, because the
                    // listing took the book's full title in v3.3. Shrinking a
                    // little everywhere was the accepted price of the store and
                    // the running head finally saying the same thing.
                    FitText(
                        text = stringResource(R.string.store_title).uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelMedium,
                        color = HeroSubtext,
                        minScale = 0.45f,
                    )
                }
            }
        }
    }
}

private suspend fun shareNameImage(context: Context, bitmap: ImageBitmap, name: Name) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        // One image at a time. Every share wrote a PNG that nothing ever
        // deleted; one file per name per share, accumulating until the
        // system cleared the cache. Any earlier file is dead by now: the
        // chooser that held it was either completed (stream already read)
        // or abandoned, so pruning it here is safe.
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "name_${name.number}.png")
        FileOutputStream(file).use {
            bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}

/**
 * The name as words, for the contexts a picture does not fit (see ShareText),
 * sent through the system chooser and reported back, so the sheet can toast
 * the one thing that failed when it did.
 */
private fun shareNameText(context: Context, text: String): Boolean {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return try {
        context.startActivity(Intent.createChooser(sendIntent, null))
        true
    } catch (_: Exception) {
        false
    }
}
