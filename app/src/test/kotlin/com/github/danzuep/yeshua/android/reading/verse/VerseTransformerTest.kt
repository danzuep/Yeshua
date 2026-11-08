/*
 * Copyright (C) 2023 Xizhi Zhu
 * Copyright (C) 2026 Daniel Collingwood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.danzuep.yeshua.android.reading.verse

import io.mockk.mockk
import com.github.danzuep.yeshua.android.core.*
import com.github.danzuep.yeshua.android.tests.BaseUnitTest
import com.github.danzuep.yeshua.android.tests.MockContents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import com.github.danzuep.yeshua.android.ui.recyclerview.BaseItem
import kotlin.test.assertTrue

class VerseTransformerTest : BaseUnitTest() {
    private fun verse(index: Int, text: String) = Verse(VerseIndex(0, 0, index), Verse.Text("T", text), emptyList())

    private fun List<BaseItem>.paragraphs() = map { it as ParagraphItem }

    @Test
    fun `test toParagraphItems without structure is one paragraph`() {
        val items = listOf(verse(0, "In"), verse(1, "the"), verse(2, "beginning")).toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs()
        assertEquals(1, items.size)
        assertNull(items[0].heading)
        assertEquals(listOf("In", "the", "beginning"), items[0].segments.map { it.text })
        assertEquals(listOf("1", "2", "3"), items[0].segments.map { it.label })
    }

    @Test
    fun `test toParagraphItems marks the start of a chapter with its number`() {
        val verses = listOf(
                Verse(VerseIndex(0, 7, 0), Verse.Text("T", "One"), emptyList()),
                Verse(VerseIndex(0, 7, 1), Verse.Text("T", "Two"), emptyList())
        )
        val segments = verses.toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs().single().segments
        assertEquals(8, segments[0].chapterNumber)
        assertNull(segments[1].chapterNumber)
    }

    @Test
    fun `test toParagraphItems with headings and paragraphs`() {
        val verses = listOf(
                verse(0, "${HEADING}Creation${HEADING}${PARAGRAPH}One"),
                verse(1, "Two ${PARAGRAPH}Three"),
                verse(2, "${PARAGRAPH}Four")
        )
        val items = verses.toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs()
        assertEquals(3, items.size)
        assertEquals("Creation", items[0].heading)
        assertEquals(listOf("One", "Two"), items[0].segments.map { it.text })
        assertEquals(listOf("Three"), items[1].segments.map { it.text })
        assertNull(items[1].segments[0].label)
        assertEquals(listOf("Four"), items[2].segments.map { it.text })
        assertEquals("3", items[2].segments[0].label)
    }

    @Test
    fun `test toParagraphItems only the last part of a split verse ends the verse`() {
        val items = listOf(verse(0, "One ${PARAGRAPH}Two"), verse(1, "Three"))
                .toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs()
        assertEquals(listOf(false), items[0].segments.map { it.endsVerse })
        assertEquals(listOf(true, true), items[1].segments.map { it.endsVerse })
    }

    @Test
    fun `test toParagraphItems with a heading in the middle of a verse`() {
        val items = listOf(verse(0, "In the beginning${HEADING}Creation${HEADING}${PARAGRAPH}God made"))
                .toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs()
        assertEquals(2, items.size)
        assertNull(items[0].heading)
        assertEquals(listOf("In the beginning"), items[0].segments.map { it.text })
        assertEquals("Creation", items[1].heading)
        assertEquals(listOf("God made"), items[1].segments.map { it.text })
    }

    @Test
    fun `test toParagraphItems keeps a heading of a verse without text`() {
        val items = listOf(verse(0, "${HEADING}Title${HEADING}"), verse(1, "${PARAGRAPH}Text"))
                .toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs()
        assertEquals(1, items.size)
        assertEquals("Title", items[0].heading)
        assertEquals(listOf("Text"), items[0].segments.map { it.text })
        assertEquals("2", items[0].segments[0].label)
    }

    @Test
    fun `test toParagraphItems leaves notes out of the text`() {
        val items = listOf(verse(0, "In${NOTE_MARK}fa note${NOTE_MARK} the beginning"), verse(1, "God${NOTE_MARK}x1:1 ref${NOTE_MARK}"))
                .toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs()
        assertEquals(listOf("In the beginning", "God"), items.single().segments.map { it.text })
    }

    @Test
    fun `test toParagraphItems with bridged verses`() {
        val items = listOf(verse(0, "One"), verse(1, "Two"), verse(2, ""), verse(3, "Four"))
                .toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs()
        assertEquals(listOf("1", "2-3", "4"), items.single().segments.map { it.label })
    }

    @Test
    fun `test toParagraphItems with poetry`() {
        val items = listOf(verse(0, "${PARAGRAPH}\nLine one"), verse(1, "\n\u2003Line two"))
                .toParagraphItems(mockk(), emptyList(), emptyList(), emptyList()).paragraphs()
        assertEquals(listOf("Line one", "\n\u2003Line two"), items.single().segments.map { it.text })
    }

    @Test
    fun `test toStringForCopy keeps non-UTF8 characters and adds the translation name`() {
        val text = "${HEADING}Title${HEADING}${PARAGRAPH}\u201CHello\u201D\u2014world\u00A0now\n\u2003Line two"
        assertEquals(
                "John 1:16 T\n> \u201CHello\u201D\u2014world\u00A0now\n> \u2003Line two\n\n",
                listOf(verse(15, text)).toStringForCopy("John")
        )
    }

    @Test
    fun `test toStringForCopy as Markdown for a single verse`() {
        assertEquals(
                "John 1:16 T\n> For God so loved the world\n\n",
                listOf(verse(15, "For God so loved the world")).toStringForCopy("John")
        )
    }

    @Test
    fun `test toStringForCopy as Markdown for poetry lines`() {
        assertEquals(
                "John 1:16 T\n> Line one\n> \u2003Line two\n\n",
                listOf(verse(15, "${PARAGRAPH}\nLine one\n\u2003Line two")).toStringForCopy("John")
        )
    }

    @Test
    fun `test toStringForCopy uses the uppercase display name of the translation`() {
        val verse = Verse(VerseIndex(0, 3, 8), Verse.Text("engwebu", "A"), emptyList())
        assertEquals("Genesis 4:9 WEBU\n> A\n\n", listOf(verse).toStringForCopy("Genesis"))
    }

    @Test
    fun `test toStringForCopy splits verses that are not consecutive`() {
        assertEquals("John 1:16 T\n> A\n\nJohn 1:18 T\n> C\n\n", listOf(verse(17, "C"), verse(15, "A")).toStringForCopy("John"))
    }

    @Test
    fun `test toStringForCopy as Markdown for consecutive verses`() {
        assertEquals(
                "John 1:16-17 T\n> 16 A 17 B\n\n",
                listOf(verse(15, "A"), verse(16, "B")).toStringForCopy("John")
        )
    }

    @Test
    fun `test toStringForCopy adds the chapter mark only across chapters`() {
        fun isaiah(chapter: Int, verse: Int, text: String) = Verse(VerseIndex(22, chapter - 1, verse - 1), Verse.Text("T", text), emptyList())
        assertEquals(
                "Isaiah 52:13-53:1 T\n> 13 A 14 B 15 C \u00B653:1 D\n\n",
                listOf(isaiah(52, 13, "A"), isaiah(52, 14, "B"), isaiah(52, 15, "C"), isaiah(53, 1, "D"))
                        .toStringForCopy("Isaiah")
        )
        assertEquals(
                "Isaiah 53:1-2 T\n> 1 D 2 E\n\n",
                listOf(isaiah(53, 1, "D"), isaiah(53, 2, "E")).toStringForCopy("Isaiah")
        )
    }

    @Test
    fun `test toStringForCopy gives each parallel translation the same kind of reference`() {
        val verse = Verse(VerseIndex(42, 0, 15), Verse.Text("engwebu", "A"), listOf(Verse.Text("kjv", "B")))
        assertEquals("John 1:16 WEBU\n> A\nJohn 1:16 KJV\n> B\n\n", listOf(verse).toStringForCopy("John"))
    }

    @Test
    fun `test toStringForCopy isolates the chapter and reference in right to left text`() {
        fun matthew(chapter: Int, verse: Int, text: String) = Verse(VerseIndex(39, chapter - 1, verse - 1), Verse.Text("T", text), emptyList())
        assertEquals(
                "Matthew \u20661:25-2:1\u2069 T\n> 25 \u0628\u0635\u062D \u00B6\u20662:1\u2069 \u0628\u0639\u062F\n\n",
                listOf(matthew(1, 25, "\u0628\u0635\u062D"), matthew(2, 1, "\u0628\u0639\u062F")).toStringForCopy("Matthew")
        )
    }

    @Test
    fun `test stripStructure`() {
        assertEquals("One Two Three", "${HEADING}Title${HEADING}${PARAGRAPH}One\n\u2003Two${PARAGRAPH}Three".stripStructure())
    }


    private val book = MockContents.kjvBookNames[0]
    private val kjv = MockContents.kjvShortName.uppercase()
    private val cuv = MockContents.cuvShortName.uppercase()
    private fun kjvText(i: Int) = MockContents.kjvVerses[i].text.text
    private fun cuvText(i: Int) = MockContents.cuvVerses[i].text.text

    @Test
    fun testVersesToStringForCopy() {
        assertEquals("$book 1:1 $kjv\n> ${kjvText(0)}\n\n", MockContents.kjvVerses[0].toStringForCopy(book))
        // random order is sorted, and runs of consecutive verses are grouped
        assertEquals(
                "$book 1:1-2 $kjv\n> 1 ${kjvText(0)} 2 ${kjvText(1)}\n\n$book 1:10 $kjv\n> ${kjvText(9)}\n\n",
                listOf(MockContents.kjvVerses[9], MockContents.kjvVerses[0], MockContents.kjvVerses[1]).toStringForCopy(book)
        )
    }

    @Test
    fun testVersesWithParallelTranslationsToStringForCopy() {
        val parallel = MockContents.kjvVersesWithCuvParallel
        assertEquals(
                "$book 1:1 $kjv\n> ${kjvText(0)}\n$book 1:1 $cuv\n> ${cuvText(0)}\n\n",
                parallel[0].toStringForCopy(book)
        )
        assertEquals(
                "$book 1:1-2 $kjv\n> 1 ${kjvText(0)} 2 ${kjvText(1)}\n$book 1:1-2 $cuv\n> 1 ${cuvText(0)} 2 ${cuvText(1)}\n\n",
                listOf(parallel[1], parallel[0]).toStringForCopy(book)
        )
    }
}
