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
import kotlin.test.assertTrue

class ReadingTimelineTest {
    @Test
    fun isJump_sameChapter_returnsFalse() {
        assertFalse(ReadingTimeline.isJump(VerseIndex(42, 0, 3), VerseIndex(42, 0, 9)))
    }

    @Test
    fun isJump_differentChapterOrBook_returnsTrue() {
        assertTrue(ReadingTimeline.isJump(VerseIndex(42, 0, 3), VerseIndex(42, 1, 0)))
        assertTrue(ReadingTimeline.isJump(VerseIndex(42, 0, 3), VerseIndex(0, 0, 0)))
    }

    @Test
    fun isJump_invalidPreviousPosition_returnsFalse() {
        assertFalse(ReadingTimeline.isJump(VerseIndex.INVALID, VerseIndex(0, 0, 0)))
    }

    @Test
    fun push_movesRepeatedVerseToFront() {
        val timeline = ReadingTimeline.push(listOf(VerseIndex(1, 0, 0), VerseIndex(2, 0, 0)), VerseIndex(2, 0, 0))
        assertEquals(listOf(VerseIndex(2, 0, 0), VerseIndex(1, 0, 0)), timeline)
    }

    @Test
    fun push_capsAtMaxLimit() {
        var timeline = emptyList<VerseIndex>()
        repeat(ReadingTimeline.MAX_LIMIT + 5) { timeline = ReadingTimeline.push(timeline, VerseIndex(0, 0, it)) }
        assertEquals(ReadingTimeline.MAX_LIMIT, timeline.size)
        assertEquals(VerseIndex(0, 0, ReadingTimeline.MAX_LIMIT + 4), timeline.first())
    }

    @Test
    fun parse_roundTripsAndSkipsGarbage() {
        val timeline = listOf(VerseIndex(42, 0, 3), VerseIndex(0, 1, 2))
        assertEquals(timeline, ReadingTimeline.parse(ReadingTimeline.serialize(timeline)))
        assertEquals(timeline, ReadingTimeline.parse("42:0:3,oops,1:2,999:0:0,0:1:2"))
        assertEquals(emptyList(), ReadingTimeline.parse(""))
    }
}
