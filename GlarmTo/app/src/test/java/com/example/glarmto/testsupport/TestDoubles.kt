package com.example.glarmto.testsupport

import com.example.glarmto.data.local.dao.GlarmToDao
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.RoutineEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.local.entity.WorkoutSessionEntity
import com.example.glarmto.data.preferences.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory DAO for JVM unit tests (no Room).
 */
class RecordingFakeGlarmToDao : GlarmToDao {

    val mockUserFlow = MutableStateFlow<UserEntity?>(null)
    var lastUpdatedUser: UserEntity? = null
    val insertedUsers = mutableListOf<UserEntity>()
    val insertedWorkouts = mutableListOf<WorkoutEntity>()
    val insertedNutrition = mutableListOf<NutritionEntity>()
    val insertedSessions = mutableListOf<WorkoutSessionEntity>()
    val updatedSessions = mutableListOf<WorkoutSessionEntity>()
    val insertedRoutines = mutableListOf<RoutineEntity>()
    val insertedWater = mutableListOf<WaterEntity>()
    var nextSessionId: Long = 1L

    override fun getUser(username: String): Flow<UserEntity?> = mockUserFlow

    override fun insertUser(user: UserEntity): Long {
        insertedUsers.add(user)
        mockUserFlow.value = user
        return 1L
    }

    override fun updateUser(user: UserEntity): Int {
        lastUpdatedUser = user
        mockUserFlow.value = user
        return 1
    }

    override fun getWorkoutsForDate(startOfDay: Long, endOfDay: Long, username: String) =
        flowOf(insertedWorkouts.filter { it.username == username && it.dateInMillis in startOfDay..endOfDay })

    override fun insertWorkout(workout: WorkoutEntity): Long {
        val id = insertedWorkouts.size + 1
        insertedWorkouts.add(workout.copy(id = id))
        return id.toLong()
    }

    override fun deleteWorkout(id: Int): Int {
        val before = insertedWorkouts.size
        insertedWorkouts.removeAll { it.id == id }
        return if (insertedWorkouts.size < before) 1 else 0
    }

    override fun getLatestWorkoutByName(username: String, name: String): WorkoutEntity? =
        insertedWorkouts
            .filter { it.username == username && it.exerciseName == name }
            .maxByOrNull { it.id }

    override fun insertWorkoutSession(session: WorkoutSessionEntity): Long {
        insertedSessions.add(session)
        return nextSessionId++
    }

    override fun updateWorkoutSession(session: WorkoutSessionEntity): Int {
        updatedSessions.add(session)
        return 1
    }

    override fun getWorkoutSessionsForDate(startOfDay: Long, endOfDay: Long, username: String) =
        flowOf(
            insertedSessions.filter {
                it.username == username && it.dateInMillis in startOfDay..endOfDay
            }
        )

    override fun getWorkoutsForSession(sessionId: Int, username: String) =
        flowOf(insertedWorkouts.filter { it.sessionId == sessionId && it.username == username })

    override fun getNutritionForDate(startOfDay: Long, endOfDay: Long, username: String) =
        flowOf(insertedNutrition.filter { it.username == username && it.dateInMillis in startOfDay..endOfDay })

    override fun insertNutrition(nutrition: NutritionEntity): Long {
        val id = insertedNutrition.size + 1
        insertedNutrition.add(nutrition.copy(id = id))
        return id.toLong()
    }

    override fun deleteNutrition(id: Int): Int {
        val before = insertedNutrition.size
        insertedNutrition.removeAll { it.id == id }
        return if (insertedNutrition.size < before) 1 else 0
    }

    override fun getRoutines(username: String) =
        flowOf(insertedRoutines.filter { it.username == username })

    override fun insertRoutine(routine: RoutineEntity): Long {
        val id = insertedRoutines.size + 1
        insertedRoutines.add(routine.copy(id = id))
        return id.toLong()
    }

    override fun deleteRoutine(id: Int): Int {
        val before = insertedRoutines.size
        insertedRoutines.removeAll { it.id == id }
        return if (insertedRoutines.size < before) 1 else 0
    }

    override fun getWaterForDate(startOfDay: Long, endOfDay: Long, username: String) =
        flowOf(insertedWater.filter { it.username == username && it.dateInMillis in startOfDay..endOfDay })

    override fun insertWater(water: WaterEntity): Long {
        val id = insertedWater.size + 1
        insertedWater.add(water.copy(id = id))
        return id.toLong()
    }

    override fun deleteWater(id: Int): Int {
        val before = insertedWater.size
        insertedWater.removeAll { it.id == id }
        return if (insertedWater.size < before) 1 else 0
    }

    override fun getAllWorkoutsForUser(username: String): List<WorkoutEntity> =
        insertedWorkouts.filter { it.username == username }

    override fun getAllNutritionForUser(username: String): List<NutritionEntity> =
        insertedNutrition.filter { it.username == username }

    override fun getAllWaterForUser(username: String): List<WaterEntity> =
        insertedWater.filter { it.username == username }

    override fun getWorkoutsBetween(username: String, start: Long, end: Long): List<WorkoutEntity> =
        insertedWorkouts.filter { it.username == username && it.dateInMillis in start..end }

    override fun getNutritionBetween(username: String, start: Long, end: Long): List<NutritionEntity> =
        insertedNutrition.filter { it.username == username && it.dateInMillis in start..end }
}

/** Fixed logged-in user for XP / day-range tests. */
class StaticFakeSessionManager : SessionManager(null) {
    override fun loginUser(username: String) {}
    override fun logoutUser() {}
    override fun getCurrentUser(): String? = "testuser"
    override fun isProfileSetup(): Boolean = true
    override fun setProfileSetup(setup: Boolean) {}
}

/** Mutable session for login / isolation tests. */
class MutableFakeSessionManager : SessionManager(null) {
    private var username: String? = null
    private val profileSetupByUser = mutableMapOf<String, Boolean>()

    override fun loginUser(username: String) {
        this.username = username
    }

    override fun getCurrentUser(): String? = username

    override fun setProfileSetup(setup: Boolean) {
        val u = username ?: return
        profileSetupByUser[u] = setup
    }

    override fun isProfileSetup(): Boolean {
        val u = username ?: return false
        return profileSetupByUser[u] ?: false
    }

    override fun logoutUser() {
        username = null
    }
}
