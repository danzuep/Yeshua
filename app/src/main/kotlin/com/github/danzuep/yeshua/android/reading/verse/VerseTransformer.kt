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

import android.content.Context
import com.github.danzuep.yeshua.android.core.Bookmark
import com.github.danzuep.yeshua.android.core.Highlight
import com.github.danzuep.yeshua.android.core.HEADING
import com.github.danzuep.yeshua.android.core.Note
import com.github.danzuep.yeshua.android.core.PARAGRAPH
import com.github.danzuep.yeshua.android.core.hasNotes
import com.github.danzuep.yeshua.android.core.removeNotes
import com.github.danzuep.yeshua.android.core.toDisplayedVerse
import com.github.danzuep.yeshua.android.core.Verse
import com.github.danzuep.yeshua.android.core.VerseIndex
import com.github.danzuep.yeshua.android.ui.recyclerview.BaseItem
import com.github.danzuep.yeshua.android.utils.toTranslationDisplayName
import com.github.danzuep.yeshua.android.utils.localized

private val STRUCTURE = Regex("$HEADING([^$HEADING]*)$HEADING|$PARAGRAPH")

/**
 * Builds the flowing paragraphs of a chapter from verses that still have their structure, see [PARAGRAPH] and [HEADING].
 * Empty verses (e.g. the second verse of a bridge like 2-3) are skipped.
 */
fun List<Verse>.toParagraphItems(
        context: Context, bookmarks: List<Bookmark>, highlights: List<Highlight>, notes: List<Note>
): List<BaseItem> {
    val bookmarkSet = bookmarks.mapTo(mutableSetOf()) { it.verseIndex.verseIndex }
    val highlightMap = highlights.filter { it.color != Highlight.COLOR_NONE }
            .associateTo(mutableMapOf()) { it.verseIndex.verseIndex to it.color }
    val noteSet = notes.mapTo(mutableSetOf()) { it.verseIndex.verseIndex }
    val footnoteSet = filter { it.text.text.hasNotes() }.mapTo(mutableSetOf()) { it.verseIndex.verseIndex }
    val selected = mutableSetOf<Int>()

    val items = ArrayList<BaseItem>()
    var previousSegment: ParagraphItem.Segment? = null
    var heading: String? = null
    var segments = ArrayList<ParagraphItem.Segment>()
    fun flush() {
        if (segments.isNotEmpty()) {
            items.add(ParagraphItem(context, heading, segments, bookmarkSet, highlightMap, noteSet, selected, footnoteSet))
            heading = null
            segments = ArrayList()
        }
    }

    forEachIndexed { index, verse ->
        val text = verse.text.text
        if (text.isEmpty()) return@forEachIndexed

        var followingEmptyVerseCount = 0
        while (index + followingEmptyVerseCount + 1 < size
                && this[index + followingEmptyVerseCount + 1].text.text.isEmpty()) {
            followingEmptyVerseCount++
        }
        if (index + followingEmptyVerseCount + 1 >= size) followingEmptyVerseCount = 0

        val verseNumber = verse.verseIndex.verseIndex + 1
        var label: String? = if (followingEmptyVerseCount > 0) "${verseNumber.localized()}-${(verseNumber + followingEmptyVerseCount).localized()}" else verseNumber.localized()
        fun addSegment(part: String) {
            val trimmed = (if (segments.isEmpty()) part.trimStart('\n') else part).removeNotes()
            if (trimmed.isNotBlank()) {
                val chapterNumber = if (label != null && verseNumber == 1) verse.verseIndex.chapterIndex + 1 else null
                val segment = ParagraphItem.Segment(verse, trimmed.trimEnd(), label, chapterNumber)
                if (previousSegment?.verse === verse) previousSegment?.endsVerse = false
                previousSegment = segment
                segments.add(segment)
                label = null
            }
        }

        // Headings can start anywhere in a verse, and each one starts a new paragraph.
        var cursor = 0
        for (match in STRUCTURE.findAll(text)) {
            addSegment(text.substring(cursor, match.range.first))
            flush()
            if (match.value.first() == HEADING) heading = match.groupValues[1]
            cursor = match.range.last + 1
        }
        addSegment(text.substring(cursor))
    }
    flush()
    return items
}

/**
 * Text for sharing and the clipboard: Markdown quotes, with the translation's display name after each reference and a blank line at the end.
 * Parallel translations follow as blocks of their own, each with the same kind of reference.
 */
fun Collection<Verse>.toStringForCopy(bookName: String): String {
    if (isEmpty()) return ""
    val displayed = map { it.toDisplayedVerse() }
    val translationCount = displayed.first().parallel.size + 1
    return (0 until translationCount).joinToString("\n") { index ->
        val verses = displayed.mapNotNull { verse ->
            (listOf(verse.text) + verse.parallel).getOrNull(index)?.let { Verse(verse.verseIndex, it, emptyList()) }
        }
        verses.toQuotedBlocks(bookName, verses.first().text.translationShortName.toTranslationDisplayName())
    } + "\n\n"
}

fun Verse.toStringForCopy(bookName: String): String = listOf(this).toStringForCopy(bookName)

/** One block per run of consecutive verses, e.g. "John 1:16-17 WEB" over "> 16 A 17 B". */
private fun List<Verse>.toQuotedBlocks(bookName: String, translationName: String): String {
    val sorted = sortedBy { it.verseIndex.bookIndex * 100000 + it.verseIndex.chapterIndex * 1000 + it.verseIndex.verseIndex }
    if (sorted.map { it.verseIndex.bookIndex to it.verseIndex.chapterIndex }.distinct().size > 1) {
        return sorted.toStringAcrossChapters(bookName, translationName)
    }
    val runs = sorted.fold(mutableListOf<MutableList<Verse>>()) { runs, verse ->
        if (runs.lastOrNull()?.last()?.verseIndex?.verseIndex?.plus(1) == verse.verseIndex.verseIndex) runs.last().add(verse)
        else runs.add(mutableListOf(verse))
        runs
    }
    return runs.joinToString("\n\n") { run ->
        val chapter = (run.first().verseIndex.chapterIndex + 1).localized()
        val first = run.first().verseIndex.verseIndex + 1
        val last = run.last().verseIndex.verseIndex + 1
        val reference = "$bookName $chapter:${first.localized()}" + if (last > first) "-${last.localized()}" else ""
        val body = if (run.size == 1) run.single().text.text
        else run.joinToString(" ") { "${(it.verseIndex.verseIndex + 1).localized()} ${it.text.text}" }
        "$reference $translationName\n${body.quoted()}"
    }
}

/**
 * One block for verses of several chapters, e.g. "Isaiah 52:13-53:1" over "13 A 14 B 15 C ¶53:1 D".
 * The chapter mark is only copied here, where it shows where the next chapter starts.
 */
private fun List<Verse>.toStringAcrossChapters(bookName: String, translationName: String): String {
    val rightToLeft = ParagraphItem.isRightToLeft(first().text.text)
    // Isolated, so references and chapter marks keep their reading order inside right to left text.
    fun String.isolated() = if (rightToLeft) "${ParagraphItem.LEFT_TO_RIGHT_ISOLATE}$this${ParagraphItem.POP_DIRECTIONAL_ISOLATE}" else this

    // Runs of consecutive verses, which can continue into the next chapter, e.g. "1:25-2:1".
    val runs = arrayListOf<Pair<VerseIndex, VerseIndex>>()
    forEach { verse ->
        val (start, end) = runs.lastOrNull() ?: (null to null)
        val continues = end != null && ((end.chapterIndex == verse.verseIndex.chapterIndex && end.verseIndex + 1 == verse.verseIndex.verseIndex) ||
                (end.chapterIndex + 1 == verse.verseIndex.chapterIndex && verse.verseIndex.verseIndex == 0))
        if (continues) runs[runs.size - 1] = start!! to verse.verseIndex else runs.add(verse.verseIndex to verse.verseIndex)
    }
    var previousChapter = -1
    val header = runs.joinToString("") { (start, end) ->
        val separator = when (previousChapter) { -1 -> ""; start.chapterIndex -> ","; else -> ", " }
        val first = if (previousChapter == start.chapterIndex) "${(start.verseIndex + 1).localized()}" else "${(start.chapterIndex + 1).localized()}:${(start.verseIndex + 1).localized()}"
        val last = when {
            end.chapterIndex != start.chapterIndex -> "-${(end.chapterIndex + 1).localized()}:${(end.verseIndex + 1).localized()}"
            end.verseIndex != start.verseIndex -> "-${(end.verseIndex + 1).localized()}"
            else -> ""
        }
        previousChapter = end.chapterIndex
        separator + first + last
    }.isolated()

    val body = mapIndexed { index, verse ->
        val number = (verse.verseIndex.verseIndex + 1).localized()
        val chapterStarts = index > 0 && verse.verseIndex.chapterIndex != this[index - 1].verseIndex.chapterIndex
        val label = if (chapterStarts) "${ParagraphItem.CHAPTER_MARK}${"${(verse.verseIndex.chapterIndex + 1).localized()}:$number".isolated()}" else "$number"
        "$label ${verse.text.text}"
    }.joinToString(" ")

    return "$bookName $header $translationName\n" + body.quoted()
}

private fun String.quoted(): String = split('\n').joinToString("\n") { "> $it" }
