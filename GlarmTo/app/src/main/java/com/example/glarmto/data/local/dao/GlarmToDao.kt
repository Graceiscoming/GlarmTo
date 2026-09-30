package com.example.glarmto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.RoutineEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlarmToDao {

    // User Queries
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertUser(user: UserEntity): Long

    @androidx.room.Update
    fun updateUser(user: UserEntity): Int

    @Query("SELECT * FROM user_log WHERE username = :username LIMIT 1")
    fun getUser(username: String): Flow<UserEntity?>

    // Workout Queries
    @Query("SELECT * FROM workout_log WHERE dateInMillis >= :startOfDay AND dateInMillis <= :endOfDay AND username = :username ORDER BY id ASC")
    fun getWorkoutsForDate(startOfDay: Long, endOfDay: Long, username: String): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkout(workout: WorkoutEntity): Long

    @Query("DELETE FROM workout_log WHERE id = :id")
    fun deleteWorkout(id: Int): Int

    @Query("SELECT * FROM workout_log WHERE username = :username AND exerciseName = :name ORDER BY id DESC LIMIT 1")
    fun getLatestWorkoutByName(username: String, name: String): WorkoutEntity?

    // Workout Session Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @androidx.room.Update
    fun updateWorkoutSession(session: WorkoutSessionEntity): Int

    @Query("SELECT * FROM workout_sessions WHERE dateInMillis >= :startOfDay AND dateInMillis <= :endOfDay AND username = :username ORDER BY startTimeInMillis DESC")
    fun getWorkoutSessionsForDate(startOfDay: Long, endOfDay: Long, username: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_log WHERE sessionId = :sessionId AND username = :username ORDER BY id ASC")
    fun getWorkoutsForSession(sessionId: Int, username: String): Flow<List<WorkoutEntity>>

    // Nutrition Queries
    @Query("SELECT * FROM nutrition_log WHERE dateInMillis >= :startOfDay AND dateInMillis <= :endOfDay AND username = :username ORDER BY id ASC")
    fun getNutritionForDate(startOfDay: Long, endOfDay: Long, username: String): Flow<List<NutritionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNutrition(nutrition: NutritionEntity): Long

    @Query("DELETE FROM nutrition_log WHERE id = :id")
    fun deleteNutrition(id: Int): Int

    // Routine Queries
    @Query("SELECT * FROM routine_log WHERE username = :username ORDER BY id ASC")
    fun getRoutines(username: String): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRoutine(routine: RoutineEntity): Long

    @Query("DELETE FROM routine_log WHERE id = :id")
    fun deleteRoutine(id: Int): Int

    // Water
    @Query("SELECT * FROM water_log WHERE dateInMillis >= :startOfDay AND dateInMillis <= :endOfDay AND username = :username ORDER BY id ASC")
    fun getWaterForDate(startOfDay: Long, endOfDay: Long, username: String): Flow<List<WaterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWater(water: WaterEntity): Long

    @Query("DELETE FROM water_log WHERE id = :id")
    fun deleteWater(id: Int): Int

    // One-shot reads (export / stats). Blocking List queries — call only from Dispatchers.IO
    // (Room+KSP can emit invalid Java for suspend DAO methods alongside other overloads.)
    @Query("SELECT * FROM workout_log WHERE username = :username ORDER BY dateInMillis ASC, id ASC")
    fun getAllWorkoutsForUser(username: String): List<WorkoutEntity>

    @Query("SELECT * FROM nutrition_log WHERE username = :username ORDER BY dateInMillis ASC, id ASC")
    fun getAllNutritionForUser(username: String): List<NutritionEntity>

    @Query("SELECT * FROM water_log WHERE username = :username ORDER BY dateInMillis ASC, id ASC")
    fun getAllWaterForUser(username: String): List<WaterEntity>

    @Query(
        "SELECT * FROM workout_log WHERE username = :username AND dateInMillis >= :start AND dateInMillis <= :end ORDER BY id ASC"
    )
    fun getWorkoutsBetween(username: String, start: Long, end: Long): List<WorkoutEntity>

    @Query(
        "SELECT * FROM nutrition_log WHERE username = :username AND dateInMillis >= :start AND dateInMillis <= :end ORDER BY id ASC"
    )
    fun getNutritionBetween(username: String, start: Long, end: Long): List<NutritionEntity>
}
