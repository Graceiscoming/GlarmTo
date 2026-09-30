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
}
