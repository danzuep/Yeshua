package com.github.danzuep.yeshua.android

import com.github.danzuep.yeshua.android.ui.recyclerview.SectionScroll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SectionScrollTest {
    // Items 3 until 7 are the chapter, each 100px; items 0-2 and 7-9 are neighbours of other sizes.
    private val heights = (0 until 10).associateWith { if (it in 3 until 7) 100 else 777 }

    @Test
    fun topOfChapterHasZeroOffset() {
        val scroll = SectionScroll.of(3, 7, 250, heights, firstPosition = 3, firstTop = 0)!!
        assertEquals(400, scroll.range)
        assertEquals(0, scroll.offset)
        assertEquals(250, scroll.extent)
    }

    @Test
    fun bottomOfChapterReachesRange() {
        val scroll = SectionScroll.of(3, 7, 250, heights, firstPosition = 5, firstTop = -50)!!
        assertEquals(scroll.range, scroll.offset + scroll.extent)
    }

    @Test
    fun scrolledIntoNextChapterStaysAtBottom() {
        val scroll = SectionScroll.of(3, 7, 250, heights, firstPosition = 8, firstTop = -10)!!
        assertEquals(scroll.range, scroll.offset + scroll.extent)
    }

    @Test
    fun scrolledAboveChapterStaysAtTop() {
        assertEquals(0, SectionScroll.of(3, 7, 250, heights, firstPosition = 1, firstTop = -20)!!.offset)
    }

    @Test
    fun neighbourShiftingIndicesDoesNotChangeTheResult() {
        val before = SectionScroll.of(3, 7, 250, heights, firstPosition = 4, firstTop = -30)!!
        val shifted = heights.mapKeys { it.key + 5 }
        val after = SectionScroll.of(8, 12, 250, shifted, firstPosition = 9, firstTop = -30)!!
        assertEquals(before.range, after.range)
        assertEquals(before.offset, after.offset)
        assertEquals(before.extent, after.extent)
    }

    @Test
    fun unmeasuredItemsUseTheAverage() {
        val scroll = SectionScroll.of(0, 4, 100, mapOf(0 to 100, 1 to 300), firstPosition = 0, firstTop = 0)!!
        assertEquals(800, scroll.range)
    }

    @Test
    fun shortChapterFillsTheViewport() {
        val scroll = SectionScroll.of(3, 7, 1000, heights, firstPosition = 3, firstTop = 0)!!
        assertEquals(scroll.range, scroll.extent)
        assertEquals(0, scroll.offset)
    }

    @Test
    fun nothingMeasuredOrEmptyRangeGivesNull() {
        assertNull(SectionScroll.of(3, 7, 250, emptyMap(), 3, 0))
        assertNull(SectionScroll.of(3, 3, 250, heights, 3, 0))
    }
}
