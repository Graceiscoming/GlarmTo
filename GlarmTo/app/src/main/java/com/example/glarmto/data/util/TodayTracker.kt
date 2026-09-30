package com.example.glarmto.data.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The current local day (start of day, in millis) for a ViewModel.
 *
 * ViewModels used to read "today" once when they were created, so an app left open past midnight kept
 * showing and saving to yesterday. Call [refresh] when the screen comes back to the foreground (and
 * build "today" queries from [today] with flatMapLatest) and the day rolls over.
 */
class TodayTracker(private val provider: () -> Long = CalendarDayUtils::localTodayStartMillis) {
    private val _today = MutableStateFlow(provider())
    val today: StateFlow<Long> = _today.asStateFlow()

    /** Re-reads the day. Returns true if it changed. */
    fun refresh(): Boolean {
        val now = provider()
        val changed = now != _today.value
        _today.value = now
        return changed
    }

    /**
     * Like [refresh], and also moves [selected] to the new day, but only if the user was looking at
     * "today". A day they picked on purpose (say, last Tuesday) is left alone.
     */
    fun refreshAndFollow(selected: MutableStateFlow<Long>): Boolean {
        val previous = _today.value
        val changed = refresh()
        if (changed && selected.value == previous) selected.value = _today.value
        return changed
    }
}
