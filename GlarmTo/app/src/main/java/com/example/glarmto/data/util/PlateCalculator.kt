package com.example.glarmto.data.util

import kotlin.math.abs

/**
 * Symmetric bar loading: total bar weight = bar + 2 × (sum per side).
 * Greedy largest-plate-first per side.
 */
object PlateCalculator {

    data class Result(
        val weightPerSideKg: Double,
        val platesPerSide: List<Pair<Double, Int>>,
        val residualKg: Double
    )

    /**
     * @param targetTotalKg desired total on the bar (bar + plates both sides)
     * @param barKg empty bar weight
     * @param plateSizesKg distinct plate weights available (e.g. 20, 15, 10, 5, 2.5, 1.25)
     */
    fun computeLoad(targetTotalKg: Double, barKg: Double, plateSizesKg: List<Double>): Result? {
        if (targetTotalKg <= 0 || barKg < 0) return null
        val perSide = (targetTotalKg - barKg) / 2.0
        if (perSide < -1e-6) return null
        if (perSide <= 1e-6) {
            return Result(0.0, emptyList(), 0.0)
        }
        val sorted = plateSizesKg.filter { it > 0 }.distinct().sortedDescending()
        if (sorted.isEmpty()) return null
        var remaining = perSide
        val plates = mutableListOf<Pair<Double, Int>>()
        for (p in sorted) {
            val n = (remaining / p).toInt()
            if (n > 0) {
                plates.add(p to n)
                remaining -= n * p
            }
        }
        return Result(perSide, plates, remaining)
    }

    fun isGoodEnough(result: Result, toleranceKg: Double = 0.25): Boolean =
        abs(result.residualKg) <= toleranceKg
}
