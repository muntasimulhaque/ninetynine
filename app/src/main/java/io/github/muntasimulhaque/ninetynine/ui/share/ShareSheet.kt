package io.github.muntasimulhaque.ninetynine.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
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
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageInset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(name: Name, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val wordmark = stringResource(R.string.store_title)
    var sharing by remember { mutableStateOf(false) }

    // The vibrancy fix, and the reason it initially failed.
    //
    // With skipPartiallyExpanded the sheet's only resting place is Expanded,
    // pressed hard against the status bar: an upward drag or fling on the
    // card pushes the sheet into its own bounds' rubber band while the inner
    // scroller still holds velocity, so the two fight — content scrolls up,
    // the sheet bounces back down, forever (material3 1.4.0; matches the
    // known upstream reports). A nested-scroll connection is the dam that
    // stops it — but a connection must be an ANCESTOR of the scroller it
    // guards to sit between that scroller and its parent (the sheet), and
    // this one was first chained AFTER verticalScroll on the same modifier,
    // which makes it a descendant. Leftover from the card's own scroller
    // never passed through it at all, so the fight survived every guard and
    // shipped shaking anyway.
    //
    // Chained BEFORE verticalScroll it is truly BETWEEN the card's scroller
    // and the sheet, where it takes each leftover exactly once: upward is
    // simply eaten, downward passes through untouched so swipe-to-dismiss
    // keeps its reach and feel. One rule for both drag and fling, no special
    // cases.
    val quenchUpward = remember {
        object : NestedScrollConnection {
            // Mixed upstream signatures: onPostScroll is non-suspend now,
            // onPostFling remains suspend.
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (available.y < 0f) {
                Offset(0f, available.y)
            } else {
                Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = if (available.y < 0f) {
                Velocity(0f, available.y)
            } else {
                Velocity.Zero
            }
        }
    }

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
            // The card grows to hold the complete meaning — never an ellipsis.
            // Long cards scroll in this preview; the export is the full card.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    // Order here is the whole fix — see quenchUpward: the
                    // connection must wrap the scroller (be its ancestor) to
                    // catch what the scroller leaves over before the sheet
                    // ever sees it.
                    .nestedScroll(quenchUpward)
                    .verticalScroll(rememberScrollState()),
            ) {
                // The exported image is a public artifact — render at the
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
                            }
                        ) {
                            ShareCard(name = name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
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
                            Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
                        } finally {
                            sharing = false
                        }
                    }
                },
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.padding(start = 8.dp))
                Text(stringResource(R.string.share_image))
            }
            // The card is the artifact, but most share contexts want the words
            // themselves — a caption, a quote, a note — so the sheet offers the
            // plain text beside the plate, set exactly as the card sets it.
            TextButton(
                onClick = {
                    val sent = shareNameText(context, name, wordmark)
                    if (sent) onDismiss()
                    else Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
                },
            ) {
                Text(stringResource(R.string.share_text))
            }
        }
    }
}

/**
 * The exported card: deep emerald + gold, identical to the widget/hero
 * identity, with a fine gold frame inside — like a printed plate.
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
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .border(width = 1.dp, color = frameGold, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_mark),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    // The store's name, not the app's own. This image is the
                    // only surface a stranger sees, so it carries the title
                    // they can type into Play and actually find — see the note
                    // over store_title in strings.xml.
                    //
                    // Fitted, not fixed: tracked small caps at 11sp put
                    // "THE NINETY NINE NAMES OF ALLAH" at 262dp — 18.880 em
                    // plus 30 characters of 1.8sp tracking — against the 246dp
                    // left beside the seal on a Pixel 4 (which is 393dp wide,
                    // not 411: 1080px at density 440), so it sits at 0.94 even
                    // there. The card follows the screen, so the room is always
                    // the screen width less 147dp of chrome — 48 sheet, 20 box,
                    // 44 inner padding, 26 seal, 9 gap — giving 213dp on a
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
        // deleted — one file per name per share, accumulating until the
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
 * The name as words, for the contexts a picture does not fit: the Arabic, the
 * name and epithet on one line, the full meaning, and the store title where a
 * stranger can find the app — the same hierarchy the exported card sets.
 */
private fun shareNameText(context: Context, name: Name, wordmark: String): Boolean {
    val text = buildString {
        appendLine(name.arabic)
        appendLine("${name.transliteration} — ${name.title}")
        appendLine()
        appendLine(name.meaning)
        appendLine()
        append(wordmark)
    }
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
