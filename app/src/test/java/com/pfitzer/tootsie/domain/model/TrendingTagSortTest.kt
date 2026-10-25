package com.pfitzer.tootsie.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrendingTagSortTest {

    private val tags = listOf(tag("zebra", 1), tag("Apple", 5, 2), tag("banana", 5), tag("cherry", 9))

    @Test
    fun `trending keeps the server order`() {
        assertEquals(listOf("zebra", "Apple", "banana", "cherry"), TrendingTagSort.TRENDING.sort(tags).names())
    }

    @Test
    fun `alphabetical ignores case`() {
        assertEquals(listOf("Apple", "banana", "cherry", "zebra"), TrendingTagSort.ALPHABETICAL.sort(tags).names())
    }

    @Test
    fun `activity sums the uses of all days, ties by name`() {
        assertEquals(listOf("cherry", "Apple", "banana", "zebra"), TrendingTagSort.ACTIVITY.sort(tags).names())
    }

    @Test
    fun `unknown or missing name falls back to trending`() {
        assertEquals(TrendingTagSort.TRENDING, TrendingTagSort.fromName(null))
        assertEquals(TrendingTagSort.TRENDING, TrendingTagSort.fromName("UNKNOWN"))
        assertEquals(TrendingTagSort.ACTIVITY, TrendingTagSort.fromName("ACTIVITY"))
    }

    private fun List<TrendingTag>.names() = map { it.name }

    /** A tag with [uses] posts spread over the given days. */
    private fun tag(name: String, uses: Int, days: Int = 1) = TrendingTag(
        name = name,
        url = "",
        history = List(days) { day ->
            TagHistory(day = day.toString(), uses = if (day == 0) uses - (days - 1) else 1, accounts = 1)
        }
    )
}
