package com.github.danzuep.yeshua.android.ui.recyclerview

/** Scroll bar values, in pixels, for one section of a list. */
class SectionScroll(val range: Int, val offset: Int, val extent: Int) {
    companion object {
        /**
         * @param start first item of the section, inclusive
         * @param end last item of the section, exclusive
         * @param itemHeights measured heights by item position, estimated by their average for items not measured yet
         * @param firstPosition adapter position of the first laid out item
         * @param firstTop top edge of that item relative to the list, zero or negative once scrolled past
         */
        fun of(start: Int, end: Int, viewportHeight: Int, itemHeights: Map<Int, Int>, firstPosition: Int, firstTop: Int): SectionScroll? {
            if (end <= start) return null
            val measured = (start until end).mapNotNull(itemHeights::get)
            if (measured.isEmpty()) return null
            val average = (measured.sumOf { it.toLong() } / measured.size).toInt()
            fun heightOf(position: Int) = itemHeights[position] ?: average

            val total = (start until end).sumOf(::heightOf)
            val extent = minOf(viewportHeight, total)
            val offset = when {
                firstPosition < start -> 0
                firstPosition >= end -> total
                else -> (start until firstPosition).sumOf(::heightOf) - firstTop
            }
            return SectionScroll(total, offset.coerceIn(0, maxOf(0, total - extent)), extent)
        }
    }
}
