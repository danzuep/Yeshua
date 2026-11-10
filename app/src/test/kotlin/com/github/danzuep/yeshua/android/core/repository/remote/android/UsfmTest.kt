/*
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

package com.github.danzuep.yeshua.android.core.repository.remote.android

import com.github.danzuep.yeshua.android.core.RUBY_MARK
import com.github.danzuep.yeshua.android.core.RUBY_SEPARATOR
import com.github.danzuep.yeshua.android.core.repository.remote.parseUsfm
import com.github.danzuep.yeshua.android.core.repository.remote.stripUsfmMarkup
import kotlin.test.Test
import kotlin.test.assertEquals

class UsfmTest {
    private fun ruby(base: String, reading: String) = "$RUBY_MARK$base$RUBY_SEPARATOR$reading$RUBY_MARK"

    @Test
    fun `test stripUsfmMarkup()`() {
        assertEquals(
            "In the beginning God\u2027f1:1 note\u2027 created",
            stripUsfmMarkup("\\w In|strong=\"H7225\"\\w* the \\add beginning\\add* God\\f + \\fr 1:1 \\ft note\\f* created")
        )
    }

    @Test
    fun `test stripUsfmMarkup() keeps footnotes and cross-references as notes`() {
        assertEquals("God\u2027x1:1 Gen 1:1\u2027 created", stripUsfmMarkup("God\\x - \\xo 1:1 \\xt Gen 1:1\\x* created"))
        assertEquals("God\u2027fend\u2027", stripUsfmMarkup("God\\fe + \\ft end\\fe*"))
        assertEquals("God", stripUsfmMarkup("God\\f + \\f*"))
    }

    @Test
    fun `test parseUsfm() keeps footnotes and cross-references`() {
        val book = parseUsfm(
            """
            \id MAT
            \c 1
            \v 1 First\f + \ft note\f* text
            \v 2 Second\x - \xo 1:2 \xt Gen 1:1\x* more
            """.trimIndent()
        )
        assertEquals(mapOf(1 to "First\u2027fnote\u2027 text", 2 to "Second\u2027x1:2 Gen 1:1\u2027 more"), book.chapters.getValue(1))
    }

    @Test
    fun `test parseUsfm()`() {
        val book = parseUsfm(
            """
            \id GEN
            \h Genesis
            \toc2 Genesis (long)
            \c 1
            \s1 Heading
            \p
            \v 1 First
            \q1 continued
            \q2 indented
            \v 2-3 Bridged
            \v 4 Fourth
            \c 2
            \v 1 Next chapter
            """.trimIndent()
        )
        assertEquals("GEN", book.code)
        assertEquals("Genesis (long)", book.name)
        assertEquals(mapOf(1 to "\u2028Heading\u2028\u2029First\ncontinued\n\u2003indented", 2 to "Bridged", 3 to "", 4 to "Fourth"), book.chapters.getValue(1))
        assertEquals(mapOf(1 to "Next chapter"), book.chapters.getValue(2))
    }

    @Test
    fun `test stripUsfmMarkup() keeps ruby with its reading`() {
        assertEquals("Generic ruby: ${ruby("base", "reading")}.", stripUsfmMarkup("Generic ruby: \\rb base|reading\\rb*."))
        assertEquals("${ruby("神", "かみ")}は${ruby("愛", "あい")}です。", stripUsfmMarkup("\\rb 神|かみ\\rb*は\\rb 愛|あい\\rb*です。"))
        assertEquals("${ruby("起初", "qǐ chū")},${ruby("神", "shén")}创造", stripUsfmMarkup("\\rb 起初|qǐ chū\\rb*,\\rb 神|shén\\rb*创造"))
        assertEquals("${ruby("שָׁלוֹם", "shalom")} לָכֶם", stripUsfmMarkup("\\rb שָׁלוֹם|shalom\\rb* לָכֶם"))
        assertEquals("${ruby("🕊️", "dove")} and ${ruby("👨‍👩‍👧‍👦", "family")}", stripUsfmMarkup("\\rb 🕊️|dove\\rb* and \\rb 👨‍👩‍👧‍👦|family\\rb*"))
        assertEquals(ruby("A long base phrase", "a phrase reading"), stripUsfmMarkup("\\rb A long \\add base\\add* phrase|a phrase reading\\rb*"))
    }

    @Test
    fun `test stripUsfmMarkup() keeps only the base when there is no reading`() {
        assertEquals("אֱלֹהִים and λόγος", stripUsfmMarkup("\\rb אֱלֹהִים|strong=\"H430\"\\rb* and \\rb λόγος|strong=\"G3056\"\\rb*"))
        assertEquals("visible base", stripUsfmMarkup("\\rb visible base|\\rb*"))
        assertEquals("光", stripUsfmMarkup("\\rb 光\\rb*"))
        assertEquals("left right", stripUsfmMarkup("left \\rb |anchor\\rb* right"))
        assertEquals("", stripUsfmMarkup("\\rb |anchor\\rb*"))
    }

    @Test
    fun `test stripUsfmMarkup() keeps notes next to ruby`() {
        assertEquals("${ruby("言葉", "ことば")}\u2027fnote\u2027 followed", stripUsfmMarkup("\\rb 言葉|ことば\\rb*\\f + \\ft note\\f* followed"))
        assertEquals(
            "${ruby("λόγος", "word")}\u2027x1:1 Gen 1:1\u2027 and ${ruby("φῶς", "light")}",
            stripUsfmMarkup("\\rb λόγος|word\\rb*\\x - \\xo 1:1 \\xt Gen 1:1\\x* and \\rb φῶς|light\\rb*")
        )
        assertEquals("note \u2027fa b\u2027", stripUsfmMarkup("note \\f + \\ft a \\rb b|c\\rb*\\f*"))
    }

    @Test
    fun `test parseUsfm() with ruby across a verse`() {
        val book = parseUsfm(
            """
            \id GEN
            \c 1
            \p
            \v 1 \rb 光|ひかり\rb* at the beginning of a verse.
            \v 2 At the end of a verse: \rb 光|ひかり\rb*
            \v 3 \rb λόγος|word\rb*
            \v 4 \add Italic\add* \rb 日|に\rb*\rb 本|ほん\rb*\rb 語|ご\rb*.
            """.trimIndent()
        )
        assertEquals(
            mapOf(
                1 to "\u2029${ruby("光", "ひかり")} at the beginning of a verse.",
                2 to "At the end of a verse: ${ruby("光", "ひかり")}",
                3 to ruby("λόγος", "word"),
                4 to "Italic ${ruby("日", "に")}${ruby("本", "ほん")}${ruby("語", "ご")}.",
            ),
            book.chapters.getValue(1)
        )
    }

    @Test
    fun `test parseUsfm() keeps paragraph and poetry layout`() {
        val book = parseUsfm(
            """
            \id GEN
            \c 1
            \p
            \v 1 Paragraph start
            \q1 Poetry one \rb 光|ひかり\rb*
            \q2 Poetry two
            \q3 Poetry three
            \p
            \v 2 Second paragraph
            \m
            \v 3 Flush paragraph
            """.trimIndent()
        )
        assertEquals(
            mapOf(
                1 to "\u2029Paragraph start\nPoetry one ${ruby("光", "ひかり")}\n\u2003Poetry two\n\u2003\u2003Poetry three",
                2 to "\u2029Second paragraph",
                3 to "\u2029Flush paragraph",
            ),
            book.chapters.getValue(1)
        )
    }

    @Test
    fun `test parseUsfm() keeps empty verses`() {
        val book = parseUsfm(
            """
            \id GEN
            \c 2
            \v 1 Before an empty verse
            \v 2
            \v 3 And after this two empty verses
            \v 4
            \v 5
            \v 6 You found me
            """.trimIndent()
        )
        assertEquals(
            mapOf(1 to "Before an empty verse", 2 to "", 3 to "And after this two empty verses", 4 to "", 5 to "", 6 to "You found me"),
            book.chapters.getValue(2)
        )
    }
}
