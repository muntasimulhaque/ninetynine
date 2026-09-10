package io.github.muntasimulhaque.ninetynine

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The house style forbids the em dash outright (owner decision, 1.31): every
 * dash in the repo's prose, comments and copy was replaced with the
 * punctuation its sentence wanted (a comma, a semicolon, a colon, or
 * parentheses), and this guard keeps one from creeping back in.
 *
 * It scans the app's own sources and resources, plus the repo's documents.
 * The bundle's third-party notices are out of scope: a licence document is
 * quoted verbatim and is not ours to re-punctuate.
 *
 * No Android needed: the files are read straight off disk.
 */
class NoEmDashTest {

    private val emDash = '\u2014'

    private val roots = listOf(
        "src/main",
        "src/test",
        "src/androidTest",
        "build.gradle.kts",
        "proguard-rules.pro",
    )

    private val repoDocs = listOf(
        "../README.md",
        "../AGENTS.md",
        "../PRIVACY.md",
        "../docs/play-listing.md",
        "../docs/privacy-policy.html",
    )

    private val extensions = setOf(
        ".kt", ".kts", ".xml", ".md", ".txt", ".html", ".yml", ".json", ".pro",
    )

    private fun filesUnder(root: File): List<File> =
        if (root.isFile) listOf(root)
        else root.walkTopDown()
            .filter { it.isFile && it.extension.let { e -> ".$e" in extensions } }
            .toList()

    @Test
    fun noEmDashAnywhere() {
        val offenders = mutableListOf<String>()
        val candidates = roots.map(::File).flatMap(::filesUnder) +
            repoDocs.map(::File).filter { it.isFile }
        for (file in candidates) {
            // The bundled font licences are quoted notices, not our prose.
            if (file.path.contains("assets/fonts")) continue
            val text = try {
                file.readText()
            } catch (_: Exception) {
                continue
            }
            text.lineSequence().forEachIndexed { index, line ->
                if (emDash in line) {
                    offenders += "${file.path}:${index + 1}: ${line.trim().take(90)}"
                }
            }
        }
        assertTrue(
            "The em dash is banned in this repo (see AGENTS.md); replace it " +
                "with a comma, semicolon, colon or parentheses:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }
}
