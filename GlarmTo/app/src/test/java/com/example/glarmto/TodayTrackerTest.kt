package com.example.glarmto

import com.example.glarmto.data.util.TodayTracker
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayTrackerTest {

    private val day1 = 1_000_000L
    private val day2 = 2_000_000L
    private val day3 = 3_000_000L

    @Test
    fun `starts on the provided day`() {
        assertEquals(day1, TodayTracker { day1 }.today.value)
    }

    @Test
    fun `refresh with the same day changes nothing and reports false`() {
        val tracker = TodayTracker { day1 }

        assertFalse(tracker.refresh())
        assertEquals(day1, tracker.today.value)
    }

    @Test
    fun `refresh after the day changed updates today and reports true`() {
        var now = day1
        val tracker = TodayTracker { now }

        now = day2

        assertEquals("not picked up until refresh", day1, tracker.today.value)
        assertTrue(tracker.refresh())
        assertEquals(day2, tracker.today.value)
    }

    @Test
    fun `a second refresh on the new day reports false`() {
        var now = day1
        val tracker = TodayTracker { now }
        now = day2
        tracker.refresh()

        assertFalse(tracker.refresh())
    }

    @Test
    fun `can move forward several days at once`() {
        var now = day1
        val tracker = TodayTracker { now }

        now = day3
        tracker.refresh()

        assertEquals(day3, tracker.today.value)
    }

    @Test
    fun `following moves the selection when the user was on today`() {
        var now = day1
        val tracker = TodayTracker { now }
        val selected = MutableStateFlow(day1)

        now = day2
        assertTrue(tracker.refreshAndFollow(selected))

        assertEquals(day2, selected.value)
    }

    @Test
    fun `following leaves a day the user picked on purpose`() {
        var now = day1
        val tracker = TodayTracker { now }
        val pickedLastWeek = 500_000L
        val selected = MutableStateFlow(pickedLastWeek)

        now = day2
        assertTrue(tracker.refreshAndFollow(selected))

        assertEquals(pickedLastWeek, selected.value)
        assertEquals(day2, tracker.today.value)
    }

    @Test
    fun `following leaves a future day the user picked`() {
        var now = day1
        val tracker = TodayTracker { now }
        val selected = MutableStateFlow(day3)

        now = day2
        tracker.refreshAndFollow(selected)

        assertEquals(day3, selected.value)
    }

    @Test
    fun `following does nothing when the day did not change`() {
        val tracker = TodayTracker { day1 }
        val selected = MutableStateFlow(day1)

        assertFalse(tracker.refreshAndFollow(selected))
        assertEquals(day1, selected.value)
    }

    @Test
    fun `selection tracks across two rollovers`() {
        var now = day1
        val tracker = TodayTracker { now }
        val selected = MutableStateFlow(day1)

        now = day2
        tracker.refreshAndFollow(selected)
        now = day3
        tracker.refreshAndFollow(selected)

        assertEquals(day3, selected.value)
    }

    @Test
    fun `provider is read at construction and on every refresh`() {
        var calls = 0
        val tracker = TodayTracker { calls++; day1 }
        assertEquals(1, calls)

        tracker.refresh()
        tracker.refresh()

        assertEquals(3, calls)
    }
}
