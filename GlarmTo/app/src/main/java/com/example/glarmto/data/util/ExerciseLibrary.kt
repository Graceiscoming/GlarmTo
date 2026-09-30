package com.example.glarmto.data.util

enum class MuscleGroup {
    Chest, Back, Legs, Shoulders, Arms, Core
}

enum class Equipment {
    Barbell, Dumbbell, Machine, Cable, Bodyweight, None
}

data class ExerciseDef(
    val name: String,
    val primaryMuscle: MuscleGroup,
    val equipment: Equipment
)

object ExerciseLibrary {
    val exercises = listOf(
        // Chest
        ExerciseDef("Bench Press", MuscleGroup.Chest, Equipment.Barbell),
        ExerciseDef("Incline DB Press", MuscleGroup.Chest, Equipment.Dumbbell),
        ExerciseDef("Chest Fly", MuscleGroup.Chest, Equipment.Dumbbell),
        ExerciseDef("Cable Crossover", MuscleGroup.Chest, Equipment.Cable),
        ExerciseDef("Pushups", MuscleGroup.Chest, Equipment.Bodyweight),
        ExerciseDef("Dips", MuscleGroup.Chest, Equipment.Bodyweight),
        
        // Back
        ExerciseDef("Deadlift", MuscleGroup.Back, Equipment.Barbell),
        ExerciseDef("Lat Pulldown", MuscleGroup.Back, Equipment.Cable),
        ExerciseDef("Bent Over Row", MuscleGroup.Back, Equipment.Barbell),
        ExerciseDef("Pullups", MuscleGroup.Back, Equipment.Bodyweight),
        ExerciseDef("Seated Cable Row", MuscleGroup.Back, Equipment.Cable),
        ExerciseDef("Dumbbell Row", MuscleGroup.Back, Equipment.Dumbbell),
        
        // Legs
        ExerciseDef("Squat", MuscleGroup.Legs, Equipment.Barbell),
        ExerciseDef("Leg Press", MuscleGroup.Legs, Equipment.Machine),
        ExerciseDef("Leg Extension", MuscleGroup.Legs, Equipment.Machine),
        ExerciseDef("Leg Curl", MuscleGroup.Legs, Equipment.Machine),
        ExerciseDef("Lunges", MuscleGroup.Legs, Equipment.Dumbbell),
        ExerciseDef("Bulgarian Split Squat", MuscleGroup.Legs, Equipment.Dumbbell),
        ExerciseDef("Calf Raise", MuscleGroup.Legs, Equipment.Machine),

        // Shoulders
        ExerciseDef("Overhead Press", MuscleGroup.Shoulders, Equipment.Barbell),
        ExerciseDef("Lateral Raise", MuscleGroup.Shoulders, Equipment.Dumbbell),
        ExerciseDef("Front Raise", MuscleGroup.Shoulders, Equipment.Dumbbell),
        ExerciseDef("Face Pulls", MuscleGroup.Shoulders, Equipment.Cable),
        ExerciseDef("Arnold Press", MuscleGroup.Shoulders, Equipment.Dumbbell),

        // Arms
        ExerciseDef("Bicep Curl", MuscleGroup.Arms, Equipment.Dumbbell),
        ExerciseDef("Barbell Curl", MuscleGroup.Arms, Equipment.Barbell),
        ExerciseDef("Tricep Pushdown", MuscleGroup.Arms, Equipment.Cable),
        ExerciseDef("Hammer Curl", MuscleGroup.Arms, Equipment.Dumbbell),
        ExerciseDef("Skull Crusher", MuscleGroup.Arms, Equipment.Barbell),
        ExerciseDef("Overhead Tricep Extension", MuscleGroup.Arms, Equipment.Dumbbell),
        
        // Core
        ExerciseDef("Crunch", MuscleGroup.Core, Equipment.Bodyweight),
        ExerciseDef("Plank", MuscleGroup.Core, Equipment.Bodyweight),
        ExerciseDef("Cable Woodchopper", MuscleGroup.Core, Equipment.Cable),
        ExerciseDef("Leg Raises", MuscleGroup.Core, Equipment.Bodyweight)
    )

    fun getMuscleFor(exerciseName: String): MuscleGroup? {
        return exercises.find { it.name.equals(exerciseName, ignoreCase = true) }?.primaryMuscle
    }

    /**
     * Muscle for any exercise name: an exact library match first, then a keyword guess for names the
     * user typed themselves. Returns null when neither knows the exercise.
     */
    fun classify(exerciseName: String): MuscleGroup? =
        getMuscleFor(exerciseName.trim()) ?: keywordMuscle(exerciseName)

    // Checked in order, so the specific wins over the generic: "Leg Raises" is Core before it is Legs,
    // "Leg Curl" is Legs before it is Arms, "Overhead Tricep Extension" is Arms before it is Shoulders,
    // and a bare "press"/"raise"/"pull" only applies once nothing more specific matched.
    private val keywordRules: List<Pair<MuscleGroup, Regex>> = listOf(
        MuscleGroup.Core to Regex("""crunch|plank|sit ?ups?|\babs?\b|woodchop|leg raise|knee raise|russian twist|ab wheel|\bcore\b"""),
        MuscleGroup.Legs to Regex("""\blegs?\b|squat|lunge|calf|hamstring|quad|glute|hip thrust|romanian|\brdl\b|step ?ups?"""),
        MuscleGroup.Arms to Regex("""tricep|bicep|curl|skull ?crusher|push ?down|preacher|forearm|\barms?\b|hammer"""),
        MuscleGroup.Shoulders to Regex("""shoulder|overhead press|military|arnold|lateral raise|front raise|rear delt|\bdelts?\b|face pull|upright row|shrug|\braise"""),
        MuscleGroup.Back to Regex("""pull ?ups?|chin ?ups?|pull ?down|lat pull|\blats?\b|\brow|deadlift|\bback\b|\bpull"""),
        MuscleGroup.Chest to Regex("""bench|chest|push ?ups?|\bpecs?\b|\bfly\b|\bflye\b|\bdips?\b|crossover|\bpress|\bpush""")
    )

    internal fun keywordMuscle(exerciseName: String): MuscleGroup? {
        val normalized = exerciseName.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
        if (normalized.isEmpty()) return null
        return keywordRules.firstOrNull { (_, regex) -> regex.containsMatchIn(normalized) }?.first
    }
}
