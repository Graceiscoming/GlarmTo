package com.example.glarmto.data.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Helpers so Material3 [androidx.compose.material3.DatePicker] (UTC midnight per selection)
 * matches local calendar days used by Room queries and [GlarmToRepository.getDayRange].
 */
object CalendarDayUtils {

    /** Start of "today" in the device default timezone. */
    fun localTodayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Floor any instant to start of that local calendar day. */
    fun normalizeToLocalDayStart(wallMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = wallMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Converts Material3 date picker millis (start of selected day in UTC) to
     * start of that same calendar date in the default timezone.
     */
    fun localDayStartFromMaterialPickerUtc(utcPickerMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcPickerMillis
        }
        val y = utc.get(Calendar.YEAR)
        val m = utc.get(Calendar.MONTH)
        val d = utc.get(Calendar.DAY_OF_MONTH)
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m)
            set(Calendar.DAY_OF_MONTH, d)
        }.timeInMillis
    }

    /** Inclusive range for nutrition editing: today .. today+7 local days (matches existing window). */
    fun nutritionEditableLocalRange(): Pair<Long, Long> {
        val todayStart = localTodayStartMillis()
        val end = todayStart + (8 * 24 * 60 * 60 * 1000L) - 1
        return Pair(todayStart, end)
    }

    fun isMillisInNutritionEditableRange(localDayStartMillis: Long): Boolean {
        val (start, end) = nutritionEditableLocalRange()
        return localDayStartMillis in start..end
    }
}
