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

package com.github.danzuep.yeshua.android.ui.recyclerview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The scroll bar of a chapter, however many neighbouring chapters are loaded around it. */
class SectionScrollTest {
    // Items 3 until 7 are the chapter, 100px each. Items 0-2 and 7-9 are other chapters with much taller items.
    private val heights = (0 until 10).associateWith { if (it in 3 until 7) 100 else 777 }

    private fun chapter(firstPosition: Int, firstTop: Int, viewport: Int = 250) =
        assertNotNull(SectionScroll.of(3, 7, viewport, heights, firstPosition, firstTop))

    @Test
    fun rangeIsTheHeightOfTheChapterOnly() {
        assertEquals(400, chapter(3, 0).range)
    }

    @Test
    fun neighbouringChaptersDoNotAffectTheRange() {
        val taller = heights.mapValues { (position, height) -> if (position in 3 until 7) height else 5000 }
        assertEquals(chapter(3, 0).range, SectionScroll.of(3, 7, 250, taller, 3, 0)!!.range)
    }

    @Test
    fun topOfChapterIsAtTheTopOfTheBar() {
        val scroll = chapter(3, 0)
        assertEquals(0, scroll.offset)
        assertEquals(250, scroll.extent)
    }

    @Test
    fun scrollingMovesTheOffsetByTheDistanceScrolled() {
        assertEquals(130, chapter(4, -30).offset)
        assertEquals(30, chapter(3, -30).offset)
    }

    @Test
    fun bottomOfChapterIsAtTheBottomOfTheBar() {
        val scroll = chapter(5, -50)
        assertEquals(scroll.range, scroll.offset + scroll.extent)
    }

    @Test
    fun scrollingPastTheBottomStaysAtTheBottom() {
        val scroll = chapter(5, -300)
        assertEquals(scroll.range, scroll.offset + scroll.extent)
    }

    @Test
    fun withTheNextChapterOnScreenTheBarStaysAtTheBottom() {
        val scroll = chapter(8, -10)
        assertEquals(scroll.range, scroll.offset + scroll.extent)
    }

    @Test
    fun withThePreviousChapterOnScreenTheBarStaysAtTheTop() {
        assertEquals(0, chapter(1, -20).offset)
    }

    @Test
    fun thumbStaysInsideTheBarAtEveryScrollPosition() {
        for (firstPosition in 0 until 10) {
            for (firstTop in listOf(-700, -250, -99, -1, 0, 40)) {
                val scroll = chapter(firstPosition, firstTop)
                assertTrue(scroll.offset >= 0, "offset at $firstPosition/$firstTop")
                assertTrue(scroll.offset + scroll.extent <= scroll.range, "end at $firstPosition/$firstTop")
            }
        }
    }

    @Test
    fun scrollingDownNeverMovesTheThumbUp() {
        var previous = -1
        for (firstPosition in 2..8) {
            for (firstTop in listOf(0, -25, -50, -75)) {
                val offset = chapter(firstPosition, firstTop).offset
                assertTrue(offset >= previous, "offset at $firstPosition/$firstTop")
                previous = offset
            }
        }
    }

    @Test
    fun loadingNeighboursShiftsItemPositionsWithoutChangingTheBar() {
        val before = chapter(4, -30)
        val shifted = heights.mapKeys { it.key + 5 }
        val after = SectionScroll.of(8, 12, 250, shifted, 9, -30)!!
        assertEquals(before.range, after.range)
        assertEquals(before.offset, after.offset)
        assertEquals(before.extent, after.extent)
    }

    @Test
    fun aChapterShorterThanTheScreenFillsTheBar() {
        val scroll = chapter(3, 0, viewport = 1000)
        assertEquals(scroll.range, scroll.extent)
        assertEquals(0, scroll.offset)
    }

    @Test
    fun unmeasuredItemsAreEstimatedFromTheAverageHeight() {
        val scroll = SectionScroll.of(0, 4, 100, mapOf(0 to 100, 1 to 300), 0, 0)!!
        assertEquals(800, scroll.range)
    }

    @Test
    fun unmeasuredItemsAreEstimatedFromTheirWeight() {
        val scroll = SectionScroll.of(0, 3, 100, mapOf(0 to 100), 0, 0, weightOf = { if (it == 2) 30 else 10 })!!
        assertEquals(100 + 100 + 300, scroll.range)
    }

    @Test
    fun itemsWithoutWeightStillTakeUpSpace() {
        val scroll = SectionScroll.of(0, 3, 100, mapOf(0 to 100), 0, 0, weightOf = { 0 })!!
        assertEquals(300, scroll.range)
    }

    @Test
    fun rangeDoesNotChangeAsMoreItemsAreMeasured() {
        val weights = listOf(10, 40, 20, 30, 5, 25)
        val truth = weights.map { it * 10 }
        val ranges = (1..weights.size).map { measured ->
            val known = (0 until measured).associateWith { truth[it] }
            SectionScroll.of(0, weights.size, 100, known, 0, 0, weights::get)!!.range
        }
        assertEquals(setOf(truth.sum()), ranges.toSet())
    }

    @Test
    fun anExactMeasurementReplacesItsEstimate() {
        val estimated = SectionScroll.of(0, 2, 100, mapOf(0 to 100), 0, 0)!!
        val measured = SectionScroll.of(0, 2, 100, mapOf(0 to 100, 1 to 500), 0, 0)!!
        assertEquals(200, estimated.range)
        assertEquals(600, measured.range)
    }

    @Test
    fun noBarWithoutAnyMeasuredItemInTheChapter() {
        assertNull(SectionScroll.of(3, 7, 250, mapOf(0 to 777, 8 to 777), 8, 0))
        assertNull(SectionScroll.of(3, 7, 250, emptyMap(), 3, 0))
    }

    @Test
    fun noBarForAnEmptyOrInvertedChapter() {
        assertNull(SectionScroll.of(3, 3, 250, heights, 3, 0))
        assertNull(SectionScroll.of(7, 3, 250, heights, 3, 0))
    }
}
