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
 * The verses the reader has jumped away from, most recent first. The verse in view is the current position, so a verse
 * is only added to the timeline when the reader moves to a different chapter.
 */
object ReadingTimeline {
    const val DEFAULT_LIMIT = 10
    const val MIN_LIMIT = 1
    const val MAX_LIMIT = 50
    val LIMIT_OPTIONS = listOf(5, 10, 15, 20, 30, MAX_LIMIT)

    fun isJump(from: VerseIndex, to: VerseIndex): Boolean =
            from.isValid() && to.isValid() && (from.bookIndex != to.bookIndex || from.chapterIndex != to.chapterIndex)

    /** Adds [verse] as the most recent entry, dropping any earlier entry for the same verse and keeping at most [MAX_LIMIT]. */
    fun push(timeline: List<VerseIndex>, verse: VerseIndex): List<VerseIndex> =
            (listOf(verse) + timeline.filter { it != verse }).take(MAX_LIMIT)

    fun serialize(timeline: List<VerseIndex>): String =
            timeline.joinToString(separator = ",") { "${it.bookIndex}:${it.chapterIndex}:${it.verseIndex}" }

    fun parse(serialized: String): List<VerseIndex> = serialized.split(',').mapNotNull { entry ->
        val parts = entry.split(':').map { it.toIntOrNull() }
        if (parts.size != 3 || parts.any { it == null }) return@mapNotNull null
        VerseIndex(parts[0]!!, parts[1]!!, parts[2]!!).takeIf { it.isValid() }
    }
}
