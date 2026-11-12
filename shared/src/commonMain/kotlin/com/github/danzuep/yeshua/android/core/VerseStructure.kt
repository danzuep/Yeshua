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

/**
 * USFM structure kept inside the stored verse text so that chapters can be read as flowing paragraphs:
 * [PARAGRAPH] starts a new paragraph, a heading is wrapped in [HEADING] (at the start of a verse), and poetry lines
 * are separated by '\n' (indented with em spaces). Footnotes and cross-references are wrapped in [NOTE_MARK]s, see
 * [extractNotes]. Ruby is stored as [RUBY_MARK]base[RUBY_SEPARATOR]reading[RUBY_MARK], see [toRubyParts]. Everything outside the reading screen sees [stripStructure]d text.
 */
const val PARAGRAPH = '\u2029'
const val HEADING = '\u2028'
const val NOTE_MARK = '\u2027'
const val RUBY_MARK = '\u2025'
const val RUBY_SEPARATOR = '\u2024'

private val HEADING_REGEX = Regex("$HEADING[^$HEADING]*$HEADING")
private val NOTE_REGEX = Regex("$NOTE_MARK[fx][^$NOTE_MARK]*$NOTE_MARK")
private val NOTE_CONTENT_REGEX = Regex("$NOTE_MARK([fx])([^$NOTE_MARK]*)$NOTE_MARK")
private val RUBY_REGEX = Regex("$RUBY_MARK([^$RUBY_MARK$RUBY_SEPARATOR]*)$RUBY_SEPARATOR([^$RUBY_MARK]*)$RUBY_MARK")
private val HEADING_CONTENT_REGEX = Regex("$HEADING([^$HEADING]*)$HEADING")

/** A footnote ([isCrossReference] = false) or a cross-reference of a verse. */
class VerseNote(val isCrossReference: Boolean, val text: String)

/** The footnotes and cross-references of a verse, in the order they appear. */
fun String.extractNotes(): List<VerseNote> =
        NOTE_CONTENT_REGEX.findAll(this).map { VerseNote(it.groupValues[1] == "x", it.groupValues[2]) }.toList()

/** The section headings of a verse, in the order they appear; with [rubyOn] their ruby marks are kept for [toRubyParts]. */
fun String.extractHeadings(rubyOn: Boolean = false): List<String> =
        HEADING_CONTENT_REGEX.findAll(this).map { it.groupValues[1].let { heading -> if (rubyOn) heading else heading.removeRuby() } }
                .filter { it.removeRuby().isNotBlank() }.toList()

/** A part of verse text; [reading] (e.g. furigana or pinyin) is only set where the text is ruby that goes above [text]. */
class RubyPart(val text: String, val reading: String? = null)

/** Splits the text at its ruby, e.g. "a<RUBY>b|c<RUBY>d" gives "a", "b" over "c", and "d". Text without ruby is one part. */
fun String.toRubyParts(): List<RubyPart> {
    if (indexOf(RUBY_MARK) < 0) return listOf(RubyPart(this))
    val parts = ArrayList<RubyPart>()
    var cursor = 0
    for (match in RUBY_REGEX.findAll(this)) {
        if (match.range.first > cursor) parts.add(RubyPart(substring(cursor, match.range.first)))
        parts.add(RubyPart(match.groupValues[1], match.groupValues[2]))
        cursor = match.range.last + 1
    }
    if (cursor < length) parts.add(RubyPart(substring(cursor)))
    return parts
}

fun String.hasRuby(): Boolean = indexOf(RUBY_MARK) >= 0

/** The text without its ruby readings, keeping the base text. */
fun String.removeRuby(): String = if (indexOf(RUBY_MARK) < 0) this else replace(RUBY_REGEX, "$1")

fun String.hasNotes(): Boolean = indexOf(NOTE_MARK) >= 0

/** The text without its footnotes and cross-references. */
fun String.removeNotes(): String = if (indexOf(NOTE_MARK) < 0) this else replace(NOTE_REGEX, "")

/** The plain verse text, without headings, notes, paragraph marks or the line breaks of poetry. */
fun String.stripStructure(): String =
        if (indexOf(PARAGRAPH) < 0 && indexOf(HEADING) < 0 && indexOf(NOTE_MARK) < 0 && indexOf(RUBY_MARK) < 0 && indexOf('\n') < 0) this
        else removeNotes().removeRuby().replace(HEADING_REGEX, "").replace(PARAGRAPH, ' ').replace(Regex("[\\s\u2003]+"), " ").trim()

fun Verse.stripStructure(): Verse = copy(
        text = text.copy(text = text.text.stripStructure()),
        parallel = parallel.map { it.copy(text = it.text.stripStructure()) }
)

/** The text as the reading screen shows it: headings and notes removed, paragraph and poetry line breaks kept as '\n'. */
fun String.toDisplayedText(): String = toDisplayedText(rubyOn = false)

/** The text to display; with [rubyOn] the ruby marks are kept for [toRubyParts], otherwise only the base text stays. */
fun String.toDisplayedText(rubyOn: Boolean): String =
        removeNotes().let { if (rubyOn) it else it.removeRuby() }.replace(HEADING_REGEX, "").replace(PARAGRAPH, '\n').trim { it == '\n' || it == ' ' }

fun Verse.toDisplayedVerse(): Verse = copy(
        text = text.copy(text = text.text.toDisplayedText()),
        parallel = parallel.map { it.copy(text = it.text.toDisplayedText()) }
)
