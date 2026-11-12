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

package com.github.danzuep.yeshua.android.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RubyTest {
    private fun ruby(base: String, reading: String) = "$RUBY_MARK$base$RUBY_SEPARATOR$reading$RUBY_MARK"

    @Test
    fun toRubyParts_splitsAtRuby() {
        val parts = "Furigana: ${ruby("神", "かみ")}は${ruby("愛", "あい")}".toRubyParts()
        assertEquals(listOf("Furigana: ", "神", "は", "愛"), parts.map { it.text })
        assertEquals(listOf(null, "かみ", null, "あい"), parts.map { it.reading })
    }

    @Test
    fun toRubyParts_withoutRuby_isOnePart() {
        val parts = "In the beginning".toRubyParts()
        assertEquals(1, parts.size)
        assertEquals("In the beginning", parts.single().text)
        assertNull(parts.single().reading)
    }

    @Test
    fun toRubyParts_adjacentAndEdgeRuby() {
        val parts = "${ruby("日", "に")}${ruby("本", "ほん")}".toRubyParts()
        assertEquals(listOf("日", "本"), parts.map { it.text })
        assertEquals(listOf("に", "ほん"), parts.map { it.reading })
        assertEquals(listOf("x", "光"), "x${ruby("光", "ひかり")}".toRubyParts().map { it.text })
    }

    @Test
    fun toRubyParts_keepsRightToLeftAndEmoji() {
        val parts = "${ruby("שָׁלוֹם", "shalom")} ${ruby("🕊️", "dove")}".toRubyParts()
        assertEquals(listOf("שָׁלוֹם", " ", "🕊️"), parts.map { it.text })
        assertEquals(listOf("shalom", null, "dove"), parts.map { it.reading })
    }

    @Test
    fun removeRuby_keepsBase() {
        assertEquals("A 神 B", "A ${ruby("神", "かみ")} B".removeRuby())
        assertEquals("plain", "plain".removeRuby())
        assertTrue("a${ruby("b", "c")}".hasRuby())
        assertFalse("abc".hasRuby())
    }

    @Test
    fun stripStructureAndDisplayedText_removeRubyReadings() {
        val text = "${PARAGRAPH}Text ${ruby("神", "かみ")}は${NOTE_MARK}fa note${NOTE_MARK}"
        assertEquals("Text 神は", text.stripStructure())
        assertEquals("Text 神は", text.toDisplayedText())
    }

    @Test
    fun extractHeadings_removesRuby() {
        val text = "${HEADING}${ruby("神", "かみ")}の愛${HEADING}Text"
        assertEquals(listOf("神の愛"), text.extractHeadings())
    }

    @Test
    fun toDisplayedText_keepsRubyOnlyWhenOn() {
        val text = "${PARAGRAPH}${ruby("神", "かみ")}の愛"
        assertEquals("神の愛", text.toDisplayedText())
        assertEquals(text.removePrefix("$PARAGRAPH"), text.toDisplayedText(rubyOn = true))
    }
}
