package com.example.glarmto.data.util

import kotlin.math.ln

/**
 * Level curve: each level needs 10% more XP than the last.
 * Total XP to reach level L is 1000 * (1.1^(L-1) - 1); the XP between level L and L+1 is 100 * 1.1^(L-1).
 *
 * [levelForXp] is derived from the same [totalXpThreshold] the UI shows, so the level a user has and
 * the progress bar toward the next one can never disagree. (Computing the level straight from a
 * logarithm is off by one at exact thresholds, e.g. 210 XP came out as level 2 instead of 3.)
 */
object LevelMath {
    private const val GROWTH = 1.1

    fun totalXpThreshold(level: Int): Long {
        if (level <= 1) return 0L
        return (1000.0 * (Math.pow(GROWTH, (level - 1).toDouble()) - 1.0)).toLong()
    }

    fun xpRequiredForNextLevel(level: Int): Long =
        (100.0 * Math.pow(GROWTH, (level - 1).toDouble())).toLong()

    fun levelForXp(xp: Long): Int {
        if (xp <= 0L) return 1
        // The logarithm gives a starting guess that is at most one level off; the loops settle it exactly.
        var level = (ln(xp / 1000.0 + 1.0) / ln(GROWTH)).toInt() + 1
        while (totalXpThreshold(level + 1) <= xp) level++
        while (level > 1 && totalXpThreshold(level) > xp) level--
        return level
    }
}
