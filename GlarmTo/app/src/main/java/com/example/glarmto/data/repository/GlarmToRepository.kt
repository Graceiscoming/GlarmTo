package com.example.glarmto.data.repository

import com.example.glarmto.data.local.dao.GlarmToDao
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.RoutineEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.util.GlarmToExport
import com.example.glarmto.data.preferences.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar

data class PeriodTrainingStats(val totalVolume: Double, val totalSets: Int)

/**
 * The Single Source of Truth for the GlarmTo application.
 * 
 * This Repository pattern implementation manages data operations between the 
 * local Room database ([GlarmToDao]) and the application's ViewModels.
 * It encapsulates all complex business logic related to XP calculation, 
 * level progression, streaks, and data export.
 */
class GlarmToRepository(private val dao: GlarmToDao, private val sessionManager: SessionManager) {

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }

    // Helper to get start and end of a specific day in millis
    fun getDayRange(calendar: Calendar): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    // Helper to get start and end of a specific month in millis
    fun getMonthRange(calendar: Calendar): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    // Workout Streams
    fun getTodayWorkouts(): Flow<List<WorkoutEntity>> {
        val (start, end) = getDayRange(Calendar.getInstance())
        return getWorkoutsForRange(start, end)
    }

    fun getWorkoutsForDay(dateMillis: Long): Flow<List<WorkoutEntity>> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val (start, end) = getDayRange(cal)
        return getWorkoutsForRange(start, end)
    }

    fun getWorkoutsForRange(start: Long, end: Long): Flow<List<WorkoutEntity>> {
        val username = sessionManager.getCurrentUser() ?: return emptyFlow()
        return dao.getWorkoutsForDate(start, end, username)
    }

    suspend fun getSmartSuggestion(exerciseName: String): WorkoutEntity? = withContext(Dispatchers.IO) {
        val username = sessionManager.getCurrentUser() ?: return@withContext null
        dao.getLatestWorkoutByName(username, exerciseName)
    }

    suspend fun getWorkoutsBetweenRange(start: Long, end: Long): List<WorkoutEntity> = withContext(Dispatchers.IO) {
        val username = sessionManager.getCurrentUser() ?: return@withContext emptyList()
        dao.getWorkoutsBetween(username, start, end)
    }

    suspend fun insertWorkout(workout: WorkoutEntity, awardXp: Boolean = true) {
        val username = sessionManager.getCurrentUser() ?: return
        withContext(Dispatchers.IO) {
            dao.insertWorkout(workout.copy(username = username))
            if (awardXp) awardXP(10) // 10 XP per set
        }
    }

    suspend fun deleteWorkout(id: Int) {
        withContext(Dispatchers.IO) {
            dao.deleteWorkout(id)
            revokeXP(10)
        }
    }

    // Workout Session Streams
    suspend fun insertWorkoutSession(session: com.example.glarmto.data.local.entity.WorkoutSessionEntity): Long {
        val username = sessionManager.getCurrentUser() ?: return 0L
        return withContext(Dispatchers.IO) {
            dao.insertWorkoutSession(session.copy(username = username))
        }
    }

    suspend fun updateWorkoutSession(session: com.example.glarmto.data.local.entity.WorkoutSessionEntity) {
        val username = sessionManager.getCurrentUser() ?: return
        withContext(Dispatchers.IO) {
            dao.updateWorkoutSession(session.copy(username = username))
        }
    }

    fun getWorkoutSessionsForDay(dateMillis: Long): Flow<List<com.example.glarmto.data.local.entity.WorkoutSessionEntity>> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val (start, end) = getDayRange(cal)
        val username = sessionManager.getCurrentUser() ?: return emptyFlow()
        return dao.getWorkoutSessionsForDate(start, end, username)
    }

    fun getWorkoutSessionsForRange(start: Long, end: Long): Flow<List<com.example.glarmto.data.local.entity.WorkoutSessionEntity>> {
        val username = sessionManager.getCurrentUser() ?: return emptyFlow()
        return dao.getWorkoutSessionsForDate(start, end, username)
    }

    fun getWorkoutsForSession(sessionId: Int): Flow<List<WorkoutEntity>> {
        val username = sessionManager.getCurrentUser() ?: return emptyFlow()
        return dao.getWorkoutsForSession(sessionId, username)
    }

    // Nutrition Streams
    fun getTodayNutrition(): Flow<List<NutritionEntity>> {
        val (start, end) = getDayRange(Calendar.getInstance())
        return getNutritionForRange(start, end)
    }

    fun getNutritionForDay(dateMillis: Long): Flow<List<NutritionEntity>> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val (start, end) = getDayRange(cal)
        return getNutritionForRange(start, end)
    }

    fun getNutritionForRange(start: Long, end: Long): Flow<List<NutritionEntity>> {
        val username = sessionManager.getCurrentUser() ?: return emptyFlow()
        return dao.getNutritionForDate(start, end, username)
    }

    suspend fun insertNutrition(nutrition: NutritionEntity, awardXp: Boolean = true) {
        val username = sessionManager.getCurrentUser() ?: return
        withContext(Dispatchers.IO) {
            dao.insertNutrition(nutrition.copy(username = username))
            if (awardXp) awardXP(5) // 5 XP per food item
        }
    }

    fun getWaterForDay(dateMillis: Long): Flow<List<WaterEntity>> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val (start, end) = getDayRange(cal)
        val username = sessionManager.getCurrentUser() ?: return emptyFlow()
        return dao.getWaterForDate(start, end, username)
    }

    suspend fun insertWater(amountMl: Int, dateInMillis: Long) {
        val username = sessionManager.getCurrentUser() ?: return
        withContext(Dispatchers.IO) {
            dao.insertWater(WaterEntity(username = username, dateInMillis = dateInMillis, amountMl = amountMl))
        }
    }

    suspend fun deleteWater(id: Int) {
        withContext(Dispatchers.IO) { dao.deleteWater(id) }
    }

    suspend fun exportUserDataJson(): String = withContext(Dispatchers.IO) {
        val username = sessionManager.getCurrentUser() ?: return@withContext ""
        val user = dao.getUser(username).firstOrNull()
        val workouts = dao.getAllWorkoutsForUser(username)
        val nutrition = dao.getAllNutritionForUser(username)
        val water = dao.getAllWaterForUser(username)
        GlarmToExport.toJson(username, user, workouts, nutrition, water)
    }

    suspend fun exportUserDataCsv(): String = withContext(Dispatchers.IO) {
        val username = sessionManager.getCurrentUser() ?: return@withContext ""
        val workouts = dao.getAllWorkoutsForUser(username)
        val nutrition = dao.getAllNutritionForUser(username)
        val water = dao.getAllWaterForUser(username)
        GlarmToExport.toCsv(workouts, nutrition, water)
    }

    suspend fun getPeriodTrainingStats(daysBackInclusive: Int): PeriodTrainingStats = withContext(Dispatchers.IO) {
        val username = sessionManager.getCurrentUser()
            ?: return@withContext PeriodTrainingStats(0.0, 0)
        val endCal = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(daysBackInclusive - 1)) }
        val start = getDayRange(startCal).first
        val end = getDayRange(endCal).second
        val list = dao.getWorkoutsBetween(username, start, end)
        PeriodTrainingStats(
            totalVolume = list.sumOf { it.weight * it.reps },
            totalSets = list.size
        )
    }

    suspend fun getTrainingStreakDays(): Int = withContext(Dispatchers.IO) {
        val username = sessionManager.getCurrentUser() ?: return@withContext 0
        val todayCal = Calendar.getInstance()
        val todayStart = getDayRange(todayCal).first
        val pastCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -400) }
        val rangeStart = getDayRange(pastCal).first
        val rangeEnd = getDayRange(todayCal).second
        val workouts = dao.getWorkoutsBetween(username, rangeStart, rangeEnd)
        val dayStarts = workouts.map { w ->
            val c = Calendar.getInstance().apply {
                timeInMillis = w.dateInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            c.timeInMillis
        }.toSet()
        var cursor = todayStart
        if (!dayStarts.contains(cursor)) {
            cursor -= DAY_MS
            if (!dayStarts.contains(cursor)) return@withContext 0
        }
        var streak = 0
        while (dayStarts.contains(cursor)) {
            streak++
            cursor -= DAY_MS
        }
        streak
    }

    suspend fun copyWorkoutsFromPreviousDay(targetDayMillis: Long, sessionId: Int?) {
        val username = sessionManager.getCurrentUser() ?: return
        withContext(Dispatchers.IO) {
            val prevCal = Calendar.getInstance().apply {
                timeInMillis = targetDayMillis
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val (s, e) = getDayRange(prevCal)
            val list = dao.getWorkoutsBetween(username, s, e)
            for (w in list) {
                dao.insertWorkout(
                    w.copy(
                        id = 0,
                        dateInMillis = targetDayMillis,
                        sessionId = sessionId,
                        username = username
                    )
                )
            }
        }
    }

    suspend fun copyNutritionFromPreviousDay(targetDayMillis: Long) {
        val username = sessionManager.getCurrentUser() ?: return
        withContext(Dispatchers.IO) {
            val prevCal = Calendar.getInstance().apply {
                timeInMillis = targetDayMillis
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val (s, e) = getDayRange(prevCal)
            val list = dao.getNutritionBetween(username, s, e)
            for (n in list) {
                dao.insertNutrition(
                    n.copy(id = 0, dateInMillis = targetDayMillis, username = username)
                )
            }
        }
    }

    suspend fun deleteNutrition(id: Int) {
        withContext(Dispatchers.IO) {
            dao.deleteNutrition(id)
            revokeXP(5)
        }
    }

    // Routine Streams
    fun getRoutines(): Flow<List<RoutineEntity>> {
        val username = sessionManager.getCurrentUser() ?: return emptyFlow()
        return dao.getRoutines(username)
    }

    suspend fun insertRoutine(routine: RoutineEntity) {
        val username = sessionManager.getCurrentUser() ?: return
        withContext(Dispatchers.IO) {
            dao.insertRoutine(routine.copy(username = username))
        }
    }

    suspend fun deleteRoutine(id: Int) {
        withContext(Dispatchers.IO) {
            dao.deleteRoutine(id)
        }
    }
    
    // User functions
    suspend fun register(username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val user = dao.getUser(username).firstOrNull()
            if (user != null) {
                false // User already exists
            } else {
                val newUser = UserEntity(username = username, password = password)
                dao.insertUser(newUser)
                sessionManager.loginUser(username)
                sessionManager.setProfileSetup(false)
                true
            }
        }
    }

    suspend fun login(username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val user = dao.getUser(username).firstOrNull()
            // Check backward compatibility too: if DB migrated from older versions, password string is empty
            if (user != null && (user.password == password || user.password.isEmpty())) {
                sessionManager.loginUser(username)
                sessionManager.setProfileSetup(user.profileSetup)
                true
            } else {
                false // User not found or incorrect password
            }
        }
    }
    
    fun getUserFlow(): Flow<UserEntity?> {
        val username = sessionManager.getCurrentUser() ?: return emptyFlow()
        return dao.getUser(username)
    }

    suspend fun updateUser(user: UserEntity) {
        withContext(Dispatchers.IO) {
            dao.updateUser(user)
            sessionManager.setProfileSetup(user.profileSetup)
        }
    }
    
    fun logout() {
        sessionManager.logoutUser()
    }
    
    fun getCurrentUser() = sessionManager.getCurrentUser()
    fun isProfileSetup() = sessionManager.isProfileSetup()

    fun getTotalXPThreshold(level: Int): Long {
        if (level <= 1) return 0L
        return (1000.0 * (Math.pow(1.1, (level - 1).toDouble()) - 1.0)).toLong()
    }

    fun getXPRequiredForNextLevel(currentLevel: Int): Long {
        return (100.0 * Math.pow(1.1, (currentLevel - 1).toDouble())).toLong()
    }

    suspend fun awardXP(amount: Int) {
        val username = sessionManager.getCurrentUser() ?: return
        val user = dao.getUser(username).firstOrNull() ?: return
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val currentDailyXP = if (user.lastXPDate < today) 0L else user.dailyXPEarned
        
        // XP Cap: 300 per day
        val xpToAward = if (currentDailyXP + amount > 300) {
            (300 - currentDailyXP).coerceAtLeast(0).toInt()
        } else {
            amount
        }
        
        if (xpToAward <= 0) return

        val newTotalXP = user.xp + xpToAward
        val newDailyXP = currentDailyXP + xpToAward
        
        // Level Formula: L = log1.1(XP/1000 + 1) + 1
        val newLevel = (Math.log(newTotalXP / 1000.0 + 1.0) / Math.log(1.1)).toInt() + 1
        
        dao.updateUser(user.copy(
            xp = newTotalXP, 
            level = newLevel,
            dailyXPEarned = newDailyXP,
            lastXPDate = today
        ))
    }

    /**
     * Mirrors [awardXP] when removing a set (10) or meal (5). Total XP never below 0.
     * Daily XP bucket is reduced only when [UserEntity.lastXPDate] is still "today".
     */
    suspend fun revokeXP(amount: Int) {
        val username = sessionManager.getCurrentUser() ?: return
        val user = dao.getUser(username).firstOrNull() ?: return

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val newTotalXP = (user.xp - amount.toLong()).coerceAtLeast(0L)
        val newLevel = (Math.log(newTotalXP / 1000.0 + 1.0) / Math.log(1.1)).toInt() + 1

        val newDailyXP = if (user.lastXPDate >= startOfToday) {
            (user.dailyXPEarned - amount).coerceAtLeast(0L)
        } else {
            user.dailyXPEarned
        }

        dao.updateUser(
            user.copy(
                xp = newTotalXP,
                level = newLevel.coerceAtLeast(1),
                dailyXPEarned = newDailyXP,
                lastXPDate = user.lastXPDate
            )
        )
    }
}
