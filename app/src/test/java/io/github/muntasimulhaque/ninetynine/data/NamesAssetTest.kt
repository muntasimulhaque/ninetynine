package io.github.muntasimulhaque.ninetynine.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.text.Normalizer

/**
 * Guards the one file the whole app is made of.
 *
 * Every other test runs against fixtures. `assets/names.json` itself was
 * unguarded: a bad edit (a dropped entry, a duplicated number, a letter the
 * bundled Mushaf font has no glyph for) would sail through a green build and
 * only show up on somebody's phone. It is also the file most likely to change,
 * being where another language would land.
 *
 * No Android needed: the asset is read straight off disk, relative to the
 * module directory the test runs in.
 */
class NamesAssetTest {

    private companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    private val names: List<Name> =
        json.decodeFromString<List<Name>>(File("src/main/assets/names.json").readText())

    @Test
    fun holdsNinetyNineNamesNumberedOneToNinetyNine() {
        assertEquals(99, names.size)
        assertEquals((1..99).toList(), names.map { it.number }.sorted())
    }

    @Test
    fun noFieldIsBlank() {
        names.forEach {
            assertTrue("#${it.number} has a blank field", it.arabic.isNotBlank())
            assertTrue("#${it.number} has a blank field", it.transliteration.isNotBlank())
            assertTrue("#${it.number} has a blank field", it.title.isNotBlank())
            assertTrue("#${it.number} has a blank field", it.meaning.isNotBlank())
        }
    }

    @Test
    fun everyNameIsDistinct() {
        val translits = names.map { it.transliteration }
        assertEquals("duplicate transliteration", translits.size, translits.distinct().size)
        val arabic = names.map { it.arabic }
        assertEquals("duplicate Arabic", arabic.size, arabic.distinct().size)
    }

    /**
     * The Arabic must be stored NFC. `SearchFilter` strips harakat by codepoint
     * and `ArabicText` decomposes one specific letter at render time; both
     * assume a canonical starting point.
     */
    @Test
    fun arabicIsStoredInNfc() {
        names.forEach {
            assertEquals(
                "#${it.number} is not NFC-normalized",
                Normalizer.normalize(it.arabic, Normalizer.Form.NFC),
                it.arabic,
            )
        }
    }

    /**
     * Every Arabic character must be one the bundled typeface can actually
     * draw. KFGQPC Uthmanic HAFS has no precomposed U+0622, which is why
     * `ArabicText.forArabicFont()` decomposes it: so that one is allowed here
     * and everything else must have a real glyph. Catches a new name, or a new
     * translation, that quietly renders as an empty box.
     */
    @Test
    fun everyArabicCharacterHasAGlyphInTheBundledFont() {
        val covered = cmapCoverage(File("src/main/res/font/kfgqpc_hafs_uthmanic.ttf"))
        val decomposedAtRender = setOf('آ')
        val missing = names.flatMap { name ->
            name.arabic.filter { it !in covered && it !in decomposedAtRender && !it.isWhitespace() }
                .map { "#${name.number} U+%04X".format(it.code) }
        }
        assertEquals("characters with no glyph in the bundled font: $missing", emptyList<String>(), missing)
    }

    /**
     * The app no longer renders the short meaning (title) on the surfaces
     * that show the full meaning: the detail page, the share card and the
     * flashcard back all rely on `meaning` itself opening with the title
     * clause. A future edit that drops the clause silently removes the
     * epithet from all three surfaces with a green build.
     */
    @Test
    fun everyMeaningStartsWithItsTitle() {
        names.forEach {
            assertTrue(
                "#${it.number} meaning does not open with its title",
                it.meaning.startsWith(it.title),
            )
        }
    }

    /** Characters a TrueType font can draw, read from its format-4 cmap. */
    private fun cmapCoverage(file: File): Set<Char> = RandomAccessFile(file, "r").use { f ->
        val bytes = ByteArray(f.length().toInt()).also { f.readFully(it) }
        fun u8(i: Int) = bytes[i].toInt() and 0xFF
        fun u16(i: Int) = (u8(i) shl 8) or u8(i + 1)
        fun u32(i: Int) = (u16(i).toLong() shl 16) or u16(i + 2).toLong()

        var cmap = -1
        for (t in 0 until u16(4)) {
            val rec = 12 + t * 16
            if (String(bytes, rec, 4, Charsets.ISO_8859_1) == "cmap") cmap = u32(rec + 8).toInt()
        }
        require(cmap >= 0) { "font has no cmap table" }

        var sub = -1
        for (i in 0 until u16(cmap + 2)) {
            val rec = cmap + 4 + i * 8
            val platform = u16(rec)
            val encoding = u16(rec + 2)
            if (platform == 3 && encoding == 1 || platform == 0) sub = cmap + u32(rec + 4).toInt()
        }
        require(sub >= 0) { "font has no Unicode BMP cmap subtable" }
        require(u16(sub) == 4) { "unsupported cmap format ${u16(sub)}" }

        val segCount = u16(sub + 6) / 2
        val endBase = sub + 14
        val startBase = endBase + segCount * 2 + 2
        val deltaBase = startBase + segCount * 2
        val rangeBase = deltaBase + segCount * 2

        buildSet {
            for (s in 0 until segCount) {
                val end = u16(endBase + s * 2)
                val start = u16(startBase + s * 2)
                if (start > end || start == 0xFFFF) continue
                val delta = u16(deltaBase + s * 2)
                val rangeOffset = u16(rangeBase + s * 2)
                for (cp in start..end) {
                    val glyph = if (rangeOffset == 0) {
                        (cp + delta) and 0xFFFF
                    } else {
                        val at = rangeBase + s * 2 + rangeOffset + (cp - start) * 2
                        val g = u16(at)
                        if (g == 0) 0 else (g + delta) and 0xFFFF
                    }
                    if (glyph != 0) add(cp.toChar())
                }
            }
        }
    }
}
