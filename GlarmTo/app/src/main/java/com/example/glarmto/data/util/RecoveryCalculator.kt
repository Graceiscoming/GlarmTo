package com.example.glarmto.data.util

import com.example.glarmto.data.local.entity.WorkoutEntity
import kotlin.math.max

/**
 * Shared muscle-fatigue model: each logged set deals 15% damage to its muscle group,
 * which then recovers linearly back to 100% over 48 hours.
 * Used by both [com.example.glarmto.ui.workout.RecoveryViewModel] (dashboard display)
 * and [WorkoutGenerator] (so AI-generated routines avoid still-fatigued muscles).
 */
object RecoveryCalculator {
    private const val DAMAGE_PER_SET = 0.15f
    private val RECOVERY_WINDOW_MS = 48L * 60 * 60 * 1000

    fun calculate(workouts: List<WorkoutEntity>, now: Long = System.currentTimeMillis()): Map<MuscleGroup, Float> {
        val muscleDamageMap = mutableMapOf<MuscleGroup, Float>()
        val muscleLastHitMap = mutableMapOf<MuscleGroup, Long>()

        for (w in workouts) {
            // Sets older than the recovery window are already fully healed; counting them would
            // make old history keep a muscle looking fatigued.
            if (now - w.dateInMillis > RECOVERY_WINDOW_MS) continue
            val muscle = ExerciseLibrary.getMuscleFor(w.exerciseName) ?: continue
            val currentDamage = muscleDamageMap.getOrDefault(muscle, 0f)
            muscleDamageMap[muscle] = (currentDamage + DAMAGE_PER_SET).coerceAtMost(1.0f)

            val lastHit = muscleLastHitMap.getOrDefault(muscle, 0L)
            if (w.dateInMillis > lastHit) {
                muscleLastHitMap[muscle] = w.dateInMillis
            }
        }

        return MuscleGroup.values().associateWith { muscle ->
            val damage = muscleDamageMap.getOrDefault(muscle, 0f)
            val lastHit = muscleLastHitMap.getOrDefault(muscle, 0L)

            if (damage == 0f || lastHit == 0L) {
                1.0f
            } else {
                val timeSinceLastHit = max(0L, now - lastHit)
                val recoveryProgress = timeSinceLastHit.toFloat() / RECOVERY_WINDOW_MS.toFloat()
                val remainingDamage = max(0f, damage - recoveryProgress)
                (1.0f - remainingDamage).coerceIn(0f, 1f)
            }
        }
    }
}
