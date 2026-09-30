package com.example.glarmto.data.util

import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.random.Random

data class GeneratedWorkout(
    val title: String,
    val totalTimeMins: Int,
    val exercises: List<GeneratedExercise>,
    val warnings: List<String> = emptyList(),
    val insights: List<String> = emptyList()
)

data class GeneratedExercise(
    val name: String,
    val targetSets: Int,
    val targetReps: String, // e.g., "10-12"
    val suggestedWeight: Double? = null, // null = bodyweight or no history yet
    val note: String? = null
)

/** A single exercise's recent training history, used for progressive overload, trend detection, and variety. */
data class ExerciseHistoryStats(
    val lastWeight: Double,
    val lastReps: Int,
    val maxWeight: Double,
    val lastPerformedMillis: Long,
    /** Chronological (oldest first) (weight, reps) of the last few logged sets, for plateau/deload detection. */
    val recentSets: List<Pair<Double, Int>> = emptyList()
)

/** Everything [WorkoutGenerator] needs from the database to make its picks smarter. */
data class AiGeneratorContext(
    val muscleRecovery: Map<MuscleGroup, Float> = emptyMap(), // 1.0 = fully recovered
    val exerciseHistory: Map<String, ExerciseHistoryStats> = emptyMap(),
    /** Sets logged per muscle group in the last 7 days, for weekly push/pull-style balance. */
    val weeklyMuscleSets: Map<MuscleGroup, Int> = emptyMap(),
    /** Learned average seconds-per-set from past sessions (work + rest); null falls back to the 2-min default. */
    val avgSecondsPerSet: Double? = null,
    /** Learned average sets actually completed per exercise per session; null skips the blend. */
    val avgSetsPerExercise: Double? = null
)

object WorkoutGenerator {
    private const val LOW_RECOVERY_THRESHOLD = 0.5f
    private const val VARIETY_COOLDOWN_DAYS = 3f
    private const val DEFAULT_SECONDS_PER_SET = 120.0 // ~2 min/set (work + rest), used when no history exists

    /**
     * Offline rule-based AI function to dynamically generate a workout.
     *
     * All fields of [context] default to empty/null, which reproduces the original random,
     * fixed-time-budget behavior. When history is available, picks are biased toward recovered
     * and weekly-under-trained muscles, exercises not done in the last few days, time budgeting
     * is personalized to the user's actual pace, and each exercise gets a progressive-overload
     * (or deload, if progress has stalled) weight suggestion.
     */
    fun generateWorkout(
        availableTimeMins: Int,
        equipmentConstraints: List<Equipment>,
        focusMuscles: List<MuscleGroup>,
        context: AiGeneratorContext = AiGeneratorContext(),
        now: Long = System.currentTimeMillis()
    ): GeneratedWorkout {
        val muscleRecovery = context.muscleRecovery
        val exerciseHistory = context.exerciseHistory
        val weeklyMuscleSets = context.weeklyMuscleSets

        // 1. Filter the library
        var pool = ExerciseLibrary.exercises

        if (equipmentConstraints.isNotEmpty() && !equipmentConstraints.contains(Equipment.None)) {
            // Need to match AT LEAST ONE of the available equipments (or Bodyweight which is always fine)
            pool = pool.filter {
                equipmentConstraints.contains(it.equipment) || it.equipment == Equipment.Bodyweight
            }
        }

        if (focusMuscles.isNotEmpty()) {
            pool = pool.filter { focusMuscles.contains(it.primaryMuscle) }
        }

        // 2. Personalized time budget: learn seconds/set from history, else assume ~2 min/set,
        // and ~3 sets/exercise for sizing the routine (this reproduces the original 6-min/exercise
        // default exactly when no history is supplied: 120s * 3 = 360s = 6 min).
        val secondsPerSet = (context.avgSecondsPerSet ?: DEFAULT_SECONDS_PER_SET).coerceIn(45.0, 240.0)
        val secondsPerExercise = secondsPerSet * 3
        val availableSeconds = availableTimeMins * 60.0
        val targetNumExercises = (availableSeconds / secondsPerExercise).roundToInt().coerceIn(2, 8)

        // 3. Select exercises, biased towards recovered + weekly-under-trained muscles and
        // exercises not done recently
        val selectedExercises = mutableListOf<ExerciseDef>()
        val insights = mutableListOf<String>()

        val poolGroups = pool.groupBy { it.primaryMuscle }
        if (focusMuscles.isNotEmpty()) {
            for (muscle in focusMuscles) {
                val exerciseForMuscle = weightedPick(
                    poolGroups[muscle] ?: emptyList(), muscleRecovery, exerciseHistory, weeklyMuscleSets, now
                )
                if (exerciseForMuscle != null) {
                    selectedExercises.add(exerciseForMuscle)
                }
            }
        }

        // Fill the rest, biased by recovery + weekly balance + variety, avoiding duplicates
        val remainingCandidates = pool.filter { !selectedExercises.contains(it) }.toMutableList()
        val needed = targetNumExercises - selectedExercises.size
        repeat(needed.coerceAtLeast(0)) {
            val picked = weightedPick(remainingCandidates, muscleRecovery, exerciseHistory, weeklyMuscleSets, now) ?: return@repeat
            selectedExercises.add(picked)
            remainingCandidates.remove(picked)
        }

        // Flag muscles that got picked (beyond what was explicitly requested) because they were
        // under-trained this week, so the user understands why they're here.
        if (weeklyMuscleSets.isNotEmpty()) {
            val avgWeeklySets = weeklyMuscleSets.values.average()
            selectedExercises
                .map { it.primaryMuscle }
                .distinct()
                .filter { it !in focusMuscles && (weeklyMuscleSets[it] ?: 0) < avgWeeklySets * 0.5 }
                .forEach { insights.add("💡 ${it.name} has had fewer sets this week, so it got extra focus today.") }
        }

        // Shuffle the final list for variety
        selectedExercises.shuffle(Random(System.currentTimeMillis()))

        // 4. Determine volume (sets and reps), blended with what the user actually tends to
        // complete per exercise (if we know it) rather than the time budget alone.
        val totalSetsCapacity = (availableSeconds / secondsPerSet).toInt()
        var setsPerExercise = if (selectedExercises.isNotEmpty()) totalSetsCapacity / selectedExercises.size else 3
        val avgSetsPerExercise = context.avgSetsPerExercise
        if (avgSetsPerExercise != null) {
            val blended = ((setsPerExercise + avgSetsPerExercise) / 2.0).roundToInt()
            if (blended != setsPerExercise) {
                insights.add("💡 Sets/exercise tuned to your usual session length (you typically do ~${"%.1f".format(avgSetsPerExercise)} sets/exercise).")
            }
            setsPerExercise = blended
        }
        setsPerExercise = setsPerExercise.coerceIn(2, 5) // At least 2, at most 5

        val warnings = mutableListOf<String>()

        val finalExercises = selectedExercises.map { def ->
            val reps = when (def.equipment) {
                Equipment.Bodyweight -> "15-20"
                Equipment.Barbell -> "5-8"
                Equipment.Machine -> "12-15"
                else -> "8-12"
            }

            // Explicitly requested but still fatigued: keep the pick (user asked for it),
            // but back off volume by one set and warn instead of silently ignoring the request.
            val recovery = muscleRecovery[def.primaryMuscle]
            var sets = setsPerExercise
            if (recovery != null && recovery < LOW_RECOVERY_THRESHOLD && def.primaryMuscle in focusMuscles) {
                sets = (setsPerExercise - 1).coerceAtLeast(2)
                val pct = (recovery * 100).toInt()
                warnings.add("⚠️ ${def.primaryMuscle.name} is only $pct% recovered — sets trimmed, consider going lighter today.")
            }

            val history = exerciseHistory[def.name]
            val (suggestedWeight, note) = progressiveOverload(def, reps, history)

            GeneratedExercise(
                name = def.name,
                targetSets = sets,
                targetReps = reps,
                suggestedWeight = suggestedWeight,
                note = note
            )
        }

        val muscleString = if (focusMuscles.isNotEmpty()) focusMuscles.joinToString(" & ") { it.name } else "Full Body"

        return GeneratedWorkout(
            title = "AI $muscleString Blast",
            totalTimeMins = availableTimeMins,
            exercises = finalExercises,
            warnings = warnings.distinct(),
            insights = insights.distinct()
        )
    }

    /**
     * Picks one candidate weighted by muscle recovery (favor recovered muscles), weekly balance
     * (favor muscles under-trained this week relative to the rest), and variety (favor exercises
     * not performed in the last [VARIETY_COOLDOWN_DAYS] days). Falls back to plain random when no
     * recovery/history/weekly data is available, matching the original behavior.
     */
    private fun weightedPick(
        candidates: List<ExerciseDef>,
        muscleRecovery: Map<MuscleGroup, Float>,
        exerciseHistory: Map<String, ExerciseHistoryStats>,
        weeklyMuscleSets: Map<MuscleGroup, Int>,
        now: Long
    ): ExerciseDef? {
        if (candidates.isEmpty()) return null
        val avgWeeklySets = weeklyMuscleSets.values.average().takeIf { !it.isNaN() } ?: 0.0

        return candidates.maxByOrNull { def ->
            val recoveryWeight = muscleRecovery[def.primaryMuscle]?.coerceAtLeast(0.15f) ?: 1f

            val lastPerformed = exerciseHistory[def.name]?.lastPerformedMillis
            val varietyWeight = if (lastPerformed == null) {
                1f
            } else {
                val daysSince = (now - lastPerformed).toFloat() / (24f * 60 * 60 * 1000)
                (daysSince / VARIETY_COOLDOWN_DAYS).coerceIn(0.15f, 1f)
            }

            val balanceWeight = if (avgWeeklySets > 0.0) {
                val weeklySets = weeklyMuscleSets[def.primaryMuscle] ?: 0
                ((avgWeeklySets + 1.0) / (weeklySets + 1.0)).toFloat().coerceIn(0.4f, 2.5f)
            } else {
                1f
            }

            recoveryWeight * varietyWeight * balanceWeight * Random.nextFloat()
        }
    }

    /**
     * Suggests next weight from recent performance:
     * - No history yet -> no suggestion, just a heads-up.
     * - Progress stalled over the last 3+ logged sets (weight flat/declining, rep-range top not hit) -> deload ~10%.
     * - Rep-range top was hit last time -> bump ~2.5%, rounded to a realistic plate/dumbbell increment.
     * - Otherwise -> repeat last weight and chase more reps.
     */
    internal fun progressiveOverload(
        def: ExerciseDef,
        targetReps: String,
        history: ExerciseHistoryStats?
    ): Pair<Double?, String?> {
        if (history == null) {
            val note = if (def.equipment == Equipment.Bodyweight) null else "First time on this one — start light and find your weight."
            return null to note
        }

        val repRangeTop = targetReps.substringAfterLast("-").toIntOrNull()
        val hitTop = repRangeTop != null && history.lastReps >= repRangeTop

        val recent = history.recentSets
        val isStalled = recent.size >= 3 && !hitTop && recent.last().first <= recent.first().first

        return when {
            isStalled -> {
                val deloaded = roundToIncrement(history.lastWeight * 0.9, def.equipment)
                deloaded to "Progress has stalled the last few sessions — deload to %.1fkg to reset and chase reps.".format(deloaded)
            }
            hitTop -> {
                val rounded = roundToIncrement(history.lastWeight * 1.025, def.equipment)
                // 2.5% of a light weight is smaller than half a plate step and would round back to the same weight.
                val bumped = if (rounded > history.lastWeight) rounded else history.lastWeight + incrementFor(def.equipment)
                val delta = bumped - history.lastWeight
                bumped to "+%.1fkg from last time 💪 (was %.1fkg x %d)".format(delta, history.lastWeight, history.lastReps)
            }
            else -> {
                history.lastWeight to "Same as last time — beat %d reps at %.1fkg.".format(history.lastReps, history.lastWeight)
            }
        }
    }

    private fun incrementFor(equipment: Equipment): Double = if (equipment == Equipment.Barbell) 2.5 else 1.25

    private fun roundToIncrement(value: Double, equipment: Equipment): Double {
        val increment = incrementFor(equipment)
        return round(value / increment) * increment
    }
}
