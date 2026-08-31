package io.github.muntasimulhaque.ninetynine.ui.about

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.muntasimulhaque.ninetynine.BuildConfig
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicSize
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ArabicText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.MixedText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.NavRow
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageRule
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ReadingInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.readingMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.scaledGap
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScrollbarThumb
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SectionLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BLOG_URL = "https://muntasimulhaque.bearblog.dev/99-names/"
private const val SOURCE_PDF_URL =
    "https://bear-images.sfo2.cdn.digitaloceanspaces.com/muntasimulhaque/ninety-nine-names-1_compressed.pdf"
private const val REPO_URL = "https://github.com/muntasimulhaque/ninetynine"

/** A quote's source sits in a trailing parenthesis: "…paradise." (Muslim) */
private val CITATION = Regex("\\s*\\(([^()]{1,40})\\)\\s*$")


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val intro by produceState(initialValue = "") {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("intro.txt").bufferedReader().use { it.readText() }
            }.getOrDefault("")
        }
    }
    // intro.txt may be checked out with CRLF endings; normalize before splitting.
    val paragraphs = remember(intro) {
        intro.replace("\r\n", "\n")
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barMeasure(),
                colors = paperTopBarColors(),
                title = { ScreenLabel(stringResource(R.string.about)) },
                navigationIcon = {
                    BackButton(onBack)
                },
            )
        },
    ) { padding ->
        // About is a reading page like the name pages, and it runs to several
        // screens on a phone — so it carries the same quiet edge cue for how
        // much front matter lies below, instead of being the one page without.
        val aboutScroll = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(aboutScroll)
                    .padding(horizontal = ReadingInset),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = readingMeasure()).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(28.dp))
                    ArabicText(
                        text = stringResource(R.string.basmala),
                        fontSize = ArabicSize.Line,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))

                    if (paragraphs.isEmpty()) {
                        // The asset failed to read; say so instead of jumping
                        // silently to the colophon.
                        Text(
                            text = stringResource(R.string.about_intro_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        paragraphs.forEachIndexed { index, para ->
                            when {
                                para.startsWith("##") -> ChapterHeading(para.trimStart('#').trim())
                                para.startsWith(">") -> Quote(para.removePrefix(">").trim())
                                else -> Text(
                                    text = para,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // A line that introduces a quote stays close to it.
                                        .padding(
                                            bottom = if (paragraphs.getOrNull(index + 1)
                                                    ?.startsWith(">") == true
                                            ) scaledGap(12.dp) else scaledGap(20.dp)
                                        ),
                                )
                            }
                        }
                    }

                    // The closing prayer, set apart as an envoi.
                    Spacer(Modifier.height(16.dp))
                    PageRule(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.about_dua),
                        style = MaterialTheme.typography.titleLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(40.dp))

                    Colophon(context)
                    Spacer(Modifier.height(40.dp))
                }
            }
            ScrollbarThumb(
                scrollState = aboutScroll,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, bottom = 16.dp, end = 8.dp),
            )
        }
    }
}

/** The document's one section break: a rule, then the name itself. */
@Composable
private fun ChapterHeading(text: String) {
    Spacer(Modifier.height(14.dp))
    PageRule(Modifier.fillMaxWidth())
    Spacer(Modifier.height(32.dp))
    MixedText(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
    )
    Spacer(Modifier.height(24.dp))
}

/**
 * Quoted matter, marked the way a book marks it: a hairline down the margin
 * and the text set to a fixed left edge, with the source lifted out below.
 */
@Composable
private fun Quote(raw: String) {
    val found = CITATION.find(raw)
    val body = found?.let { raw.removeRange(it.range).trim() } ?: raw
    val citation = found?.groupValues?.get(1)

    val rule = MaterialTheme.colorScheme.secondary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .drawBehind {
                // The rule marks the reading edge: `start`, mirroring with
                // the layout direction exactly like the text inset below it.
                // Drawn at physical x=0 it would sit on the wrong edge in
                // every RTL locale.
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width else 0f
                drawLine(
                    color = rule,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(start = 20.dp),
    ) {
        Text(
            text = body,
            style = MaterialTheme.typography.titleLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
        )
        if (citation != null) {
            Spacer(Modifier.height(12.dp))
            SectionLabel(citation)
        }
    }
}

/** Where the text came from, what it is set in, and what the app is. */
@Composable
private fun Colophon(context: Context) {
    PageRule(Modifier.fillMaxWidth())
    Spacer(Modifier.height(26.dp))
    SectionLabel(stringResource(R.string.about_source_label), Modifier.fillMaxWidth())
    Spacer(Modifier.height(scaledGap(14.dp)))
    Text(
        text = stringResource(R.string.about_attribution),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(scaledGap(22.dp)))

    PageRule(Modifier.fillMaxWidth())
    LinkRow(R.string.source_pdf) { context.openUrl(SOURCE_PDF_URL) }
    PageRule(Modifier.fillMaxWidth())
    LinkRow(R.string.read_blog) { context.openUrl(BLOG_URL) }
    PageRule(Modifier.fillMaxWidth())
    LinkRow(R.string.source_code) { context.openUrl(REPO_URL) }
    PageRule(Modifier.fillMaxWidth())

    Spacer(Modifier.height(34.dp))
    SectionLabel(stringResource(R.string.about_typefaces_label), Modifier.fillMaxWidth())
    Spacer(Modifier.height(scaledGap(14.dp)))
    Text(
        text = stringResource(R.string.about_fonts),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
    // Its own section, not a fourth source link: a way to reach the developer
    // is neither a source nor a typeface. It lives here, at the foot of the
    // one page a reader visits on purpose, rather than in the reading flow of
    // all 99 name pages — where it interrupted the meaning to offer something
    // almost nobody needs.
    Spacer(Modifier.height(34.dp))
    SectionLabel(stringResource(R.string.about_contact_label), Modifier.fillMaxWidth())
    Spacer(Modifier.height(scaledGap(14.dp)))
    PageRule(Modifier.fillMaxWidth())
    LinkRow(R.string.send_feedback) { context.sendFeedback() }
    PageRule(Modifier.fillMaxWidth())

    Spacer(Modifier.height(32.dp))
    Text(
        text = stringResource(R.string.foss_line),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LinkRow(labelRes: Int, onClick: () -> Unit) {
    NavRow(
        title = stringResource(labelRes),
        onClick = onClick,
        titleStyle = MaterialTheme.typography.titleMedium,
    )
}

private fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        .onFailure { Toast.makeText(this, R.string.link_failed, Toast.LENGTH_SHORT).show() }
}

/**
 * Hands off to whatever the reader writes mail with. ACTION_SENDTO on a
 * mailto: URI, exactly like the links above open a browser — the app itself
 * still has no way to reach the network.
 *
 * The subject carries the version so that a report about something broken is
 * answerable. Nothing else is filled in: no device details, nothing gathered
 * on the reader's behalf.
 */
private fun Context.sendFeedback() {
    val to = getString(R.string.contact_email)
    val subject = getString(
        R.string.feedback_subject,
        getString(R.string.app_title),
        BuildConfig.VERSION_NAME,
    )
    runCatching {
        startActivity(
            Intent(Intent.ACTION_SENDTO, "mailto:$to".toUri())
                .putExtra(Intent.EXTRA_SUBJECT, subject)
        )
    }.onFailure { Toast.makeText(this, R.string.link_failed, Toast.LENGTH_SHORT).show() }
}
