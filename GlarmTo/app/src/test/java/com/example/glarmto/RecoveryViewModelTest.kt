package com.example.glarmto

import com.example.glarmto.testsupport.TestTexts
import android.app.Application
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.testsupport.MainDispatcherRule
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.workout.MuscleRecovery
import com.example.glarmto.ui.workout.RecoveryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The dashboard's Muscle Recovery card is driven by this ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RecoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var repository: GlarmToRepository
    private val hour = 60L * 60 * 1000
    private val placeholder = "Analyzing your history…"

    @Before
    fun setup() {
        dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser")
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
    }

    private var nextId = 1

    private fun sets(exercise: String, count: Int, hoursAgo: Long = 0) {
        repeat(count) {
            dao.insertedWorkouts.add(
                WorkoutEntity(
                    id = nextId++, exerciseName = exercise, weight = 50.0, reps = 5,
                    dateInMillis = System.currentTimeMillis() - hoursAgo * hour, username = "testuser"
                )
            )
        }
    }

    private fun <T> await(block: suspend () -> T): T = runBlocking { withTimeout(10_000) { block() } }

    private fun RecoveryViewModel.settled(): Pair<List<MuscleRecovery>, String> = await {
        recoveryStatus.first { it.isNotEmpty() }
        val text = smartRecommendation.first { it != placeholder }
        recoveryStatus.value to text
    }

    private fun List<MuscleRecovery>.of(muscle: MuscleGroup) = first { it.muscleGroup == muscle }.recoveryPercentage

    @Test
    fun `no training means every muscle group is fully recovered`() {
        val (status, text) = RecoveryViewModel(repository, TestTexts.english).settled()

        assertEquals(MuscleGroup.values().toList(), status.map { it.muscleGroup })
        status.forEach { assertEquals(1f, it.recoveryPercentage, 0.0001f) }
        assertTrue(text.contains("fully recovered"))
    }

    @Test
    fun `a few recent sets lower only the trained muscle`() {
        sets("Bench Press", 2)

        val (status, _) = RecoveryViewModel(repository, TestTexts.english).settled()

        assertEquals(0.7f, status.of(MuscleGroup.Chest), 0.01f)
        assertEquals(1f, status.of(MuscleGroup.Legs), 0.0001f)
        assertEquals(1f, status.of(MuscleGroup.Back), 0.0001f)
    }

    @Test
    fun `an exhausted muscle is named and a recovered one is suggested`() {
        sets("Bench Press", 5) // 75% damage -> about 25% recovered

        val (status, text) = RecoveryViewModel(repository, TestTexts.english).settled()

        assertTrue(status.of(MuscleGroup.Chest) < 0.5f)
        assertTrue("text was: $text", text.contains("Chest are exhausted"))
        val recovered = MuscleGroup.values().filter { it != MuscleGroup.Chest }.map { it.name }
        assertTrue("text was: $text", recovered.any { text.contains("Focus on $it today") })
    }

    @Test
    fun `when every muscle is worn out it recommends a rest day`() {
        listOf("Bench Press", "Deadlift", "Squat", "Overhead Press", "Bicep Curl", "Plank").forEach { sets(it, 7) }

        val (status, text) = RecoveryViewModel(repository, TestTexts.english).settled()

        status.forEach { assertTrue("${it.muscleGroup} at ${it.recoveryPercentage}", it.recoveryPercentage < 0.5f) }
        assertTrue("text was: $text", text.contains("rest day"))
    }

    @Test
    fun `sets older than 48 hours have fully healed even inside the 72 hour window`() {
        sets("Bench Press", 6, hoursAgo = 60)

        val (status, text) = RecoveryViewModel(repository, TestTexts.english).settled()

        assertEquals(1f, status.of(MuscleGroup.Chest), 0.0001f)
        assertTrue(text.contains("fully recovered"))
    }

    @Test
    fun `sets older than 72 hours are not even fetched`() {
        sets("Bench Press", 6, hoursAgo = 100)

        val (status, _) = RecoveryViewModel(repository, TestTexts.english).settled()

        assertEquals(1f, status.of(MuscleGroup.Chest), 0.0001f)
    }

    @Test
    fun `old sets do not add to fresh ones - the window bug`() {
        sets("Bench Press", 5, hoursAgo = 60)
        sets("Bench Press", 1, hoursAgo = 1)

        val (status, _) = RecoveryViewModel(repository, TestTexts.english).settled()

        // Only the one recent set counts: about 0.15 damage, 1 hour recovered.
        assertEquals(1f - (0.15f - 1f / 48f), status.of(MuscleGroup.Chest), 0.01f)
    }

    @Test
    fun `fetching again picks up sets logged since`() {
        val vm = RecoveryViewModel(repository, TestTexts.english)
        val (before, _) = vm.settled()
        assertEquals(1f, before.of(MuscleGroup.Chest), 0.0001f)

        sets("Bench Press", 4)
        vm.fetchAndCalculateRecovery()

        val updated = await { vm.recoveryStatus.first { it.of(MuscleGroup.Chest) < 0.9f } }
        assertEquals(0.4f, updated.of(MuscleGroup.Chest), 0.02f)
    }

    @Test
    fun `unknown exercises do not affect any muscle`() {
        sets("Zumba", 10)

        val (status, _) = RecoveryViewModel(repository, TestTexts.english).settled()

        status.forEach { assertEquals(1f, it.recoveryPercentage, 0.0001f) }
    }

    @Test
    fun `exercise names are matched ignoring case`() {
        sets("bench press", 3)

        val (status, _) = RecoveryViewModel(repository, TestTexts.english).settled()

        assertTrue(status.of(MuscleGroup.Chest) < 1f)
    }

    @Test
    fun `other users' sets are ignored`() {
        dao.insertedWorkouts.add(
            WorkoutEntity(id = 99, exerciseName = "Bench Press", weight = 50.0, reps = 5,
                dateInMillis = System.currentTimeMillis(), username = "someone-else")
        )

        val (status, _) = RecoveryViewModel(repository, TestTexts.english).settled()

        assertEquals(1f, status.of(MuscleGroup.Chest), 0.0001f)
    }
}
