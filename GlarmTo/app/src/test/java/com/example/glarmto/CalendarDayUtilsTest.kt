package com.example.glarmto

import com.example.glarmto.data.util.CalendarDayUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CalendarDayUtilsTest {

    @Test
    fun `normalizeToLocalDayStart is idempotent`() {
        val raw = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 10, 15, 30, 45)
        }.timeInMillis
        val once = CalendarDayUtils.normalizeToLocalDayStart(raw)
        val twice = CalendarDayUtils.normalizeToLocalDayStart(once)
        assertEquals(once, twice)
        val c = Calendar.getInstance().apply { timeInMillis = once }
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
    }

    @Test
    fun `nutritionEditableLocalRange spans eight local days inclusive`() {
        val (start, end) = CalendarDayUtils.nutritionEditableLocalRange()
        assertTrue(end >= start)
        val spanMs = end - start + 1
        val eightDaysMs = 8L * 24 * 60 * 60 * 1000
        assertEquals(eightDaysMs, spanMs)
    }

    @Test
    fun `isMillisInNutritionEditableRange rejects past local days`() {
        val (start, _) = CalendarDayUtils.nutritionEditableLocalRange()
        val past = start - (24 * 60 * 60 * 1000L)
        assertFalse(CalendarDayUtils.isMillisInNutritionEditableRange(past))
        assertTrue(CalendarDayUtils.isMillisInNutritionEditableRange(start))
    }

    @Test
    fun `localDayStartFromMaterialPickerUtc maps UTC calendar day to local start`() {
        // 2024-06-15 00:00:00 UTC
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2024, Calendar.JUNE, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val utcMillis = utcCal.timeInMillis
        val localStart = CalendarDayUtils.localDayStartFromMaterialPickerUtc(utcMillis)
        val local = Calendar.getInstance().apply { timeInMillis = localStart }
        assertEquals(2024, local.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, local.get(Calendar.MONTH))
        assertEquals(15, local.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, local.get(Calendar.HOUR_OF_DAY))
    }
}
