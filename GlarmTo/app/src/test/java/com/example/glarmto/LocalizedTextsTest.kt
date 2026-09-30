package com.example.glarmto

import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.AiGeneratorContext
import com.example.glarmto.data.util.AppTexts
import com.example.glarmto.data.util.Equipment
import com.example.glarmto.data.util.ExerciseDef
import com.example.glarmto.data.util.ExerciseHistoryStats
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.data.util.WorkoutGenerator
import com.example.glarmto.testsupport.MainDispatcherRule
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.testsupport.TestTexts
import com.example.glarmto.ui.workout.RecoveryViewModel
import com.example.glarmto.ui.workout.WorkoutViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.app.Application
import kotlin.random.Random

/**
 * Text produced outside the screens (the AI workout, recovery advice, weight suggestions) comes out in
 * whichever language it is asked for, with the numbers in the right places.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LocalizedTextsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val en = TestTexts.english
    private val th = TestTexts.thai
    private val thaiChar = Regex("[฀-๿]")
    private val hour = 60L * 60 * 1000

    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser")
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
    }

    private fun generate(
        texts: AppTexts,
        focus: List<MuscleGroup> = listOf(MuscleGroup.Chest, MuscleGroup.Back),
        context: AiGeneratorContext = AiGeneratorContext()
    ) = WorkoutGenerator.generateWorkout(
        texts = texts,
        availableTimeMins = 45,
        equipmentConstraints = listOf(Equipment.Barbell, Equipment.Dumbbell),
        focusMuscles = focus,
        context = context,
        now = 1_000_000_000L,
        random = Random(7)
    )

    // ---------------- AI workout: title ----------------

    @Test
    fun `workout title in English`() {
        assertEquals("AI Chest & Back Blast", generate(en).title)
    }

    @Test
    fun `workout title in Thai uses Thai muscle names`() {
        assertEquals("โปรแกรม AI: อก & หลัง", generate(th).title)
    }

    @Test
    fun `a full body workout is named in both languages`() {
        assertEquals("AI Full Body Blast", generate(en, focus = emptyList()).title)
        assertEquals("โปรแกรม AI: ทั้งตัว", generate(th, focus = emptyList()).title)
    }

    @Test
    fun `the same seed gives the same exercises in both languages - only the words change`() {
        assertEquals(generate(en).exercises.map { it.name }, generate(th).exercises.map { it.name })
        assertEquals(generate(en).exercises.map { it.targetSets }, generate(th).exercises.map { it.targetSets })
    }

    // ---------------- AI workout: warnings and insights ----------------

    private val tiredChest = AiGeneratorContext(muscleRecovery = mapOf(MuscleGroup.Chest to 0.3f))

    @Test
    fun `low recovery warning in English names the muscle and the percentage`() {
        val warnings = generate(en, focus = listOf(MuscleGroup.Chest), context = tiredChest).warnings

        assertEquals(
            listOf("⚠️ Chest is only 30% recovered — sets trimmed, consider going lighter today."),
            warnings
        )
    }

    @Test
    fun `low recovery warning in Thai`() {
        val warnings = generate(th, focus = listOf(MuscleGroup.Chest), context = tiredChest).warnings

        assertEquals(listOf("⚠️ อก ฟื้นตัวเพียง 30% — ลดจำนวนเซตลงแล้ว ลองเบาลงหน่อยวันนี้"), warnings)
    }

    @Test
    fun `no warning when the muscle is recovered`() {
        val recovered = AiGeneratorContext(muscleRecovery = mapOf(MuscleGroup.Chest to 0.9f))

        assertTrue(generate(en, focus = listOf(MuscleGroup.Chest), context = recovered).warnings.isEmpty())
        assertTrue(generate(th, focus = listOf(MuscleGroup.Chest), context = recovered).warnings.isEmpty())
    }

    @Test
    fun `the session length insight carries the number in both languages`() {
        val learned = AiGeneratorContext(avgSetsPerExercise = 5.0)

        val english = generate(en, context = learned).insights.single { "tuned" in it }
        val thai = generate(th, context = learned).insights.single { thaiChar.containsMatchIn(it) && "5.0" in it }

        assertEquals("💡 Sets/exercise tuned to your usual session length (you typically do ~5.0 sets/exercise).", english)
        assertEquals("💡 ปรับจำนวนเซตต่อท่าให้เข้ากับความยาวเซสชันปกติของคุณ (ปกติคุณทำประมาณ 5.0 เซต/ท่า)", thai)
    }

    @Test
    fun `Thai output leaves no English sentence behind`() {
        val workout = generate(
            th, focus = listOf(MuscleGroup.Chest),
            context = AiGeneratorContext(muscleRecovery = mapOf(MuscleGroup.Chest to 0.2f), avgSetsPerExercise = 5.0)
        )

        (listOf(workout.title) + workout.warnings + workout.insights).forEach {
            assertTrue("no Thai in: $it", thaiChar.containsMatchIn(it))
            assertFalse("English left in: $it", it.contains("recovered") || it.contains("sets trimmed") || it.contains("Blast"))
        }
    }

    // ---------------- weight notes ----------------

    private val bench = ExerciseDef("Bench Press", MuscleGroup.Chest, Equipment.Barbell)
    private val pushups = ExerciseDef("Pushups", MuscleGroup.Chest, Equipment.Bodyweight)

    private fun history(weight: Double, reps: Int, recent: List<Pair<Double, Int>> = emptyList()) =
        ExerciseHistoryStats(lastWeight = weight, lastReps = reps, maxWeight = weight, lastPerformedMillis = 0L, recentSets = recent)

    private fun note(texts: AppTexts, def: ExerciseDef, reps: String, h: ExerciseHistoryStats?) =
        WorkoutGenerator.progressiveOverload(def, reps, h, texts).second

    @Test
    fun `first time note`() {
        assertEquals("First time on this one — start light and find your weight.", note(en, bench, "5-8", null))
        assertEquals("ครั้งแรกสำหรับท่านี้ — เริ่มจากเบาๆ แล้วหาน้ำหนักที่เหมาะกับคุณ", note(th, bench, "5-8", null))
    }

    @Test
    fun `bodyweight exercises have no first time note`() {
        assertEquals(null, note(en, pushups, "15-20", null))
        assertEquals(null, note(th, pushups, "15-20", null))
    }

    @Test
    fun `bodyweight note carries the reps`() {
        assertEquals("Last time: 12 reps — try to beat it.", note(en, pushups, "15-20", history(0.0, 12)))
        assertEquals("ครั้งที่แล้วทำได้ 12 ครั้ง — ลองทำให้ได้มากกว่านี้", note(th, pushups, "15-20", history(0.0, 12)))
    }

    @Test
    fun `bump note carries the increase and the old numbers`() {
        assertEquals("+2.5kg from last time 💪 (was 100.0kg x 8)", note(en, bench, "5-8", history(100.0, 8)))
        assertEquals("+2.5kg จากครั้งก่อน 💪 (เดิม 100.0kg x 8)", note(th, bench, "5-8", history(100.0, 8)))
    }

    @Test
    fun `repeat note carries reps and weight`() {
        assertEquals("Same as last time — beat 6 reps at 80.0kg.", note(en, bench, "5-8", history(80.0, 6)))
        assertEquals("เท่าครั้งที่แล้ว — ลองทำให้เกิน 6 ครั้งที่ 80.0kg", note(th, bench, "5-8", history(80.0, 6)))
    }

    @Test
    fun `deload note carries the new weight`() {
        val stalled = history(100.0, 5, recent = listOf(100.0 to 5, 100.0 to 5, 100.0 to 5))

        assertEquals(
            "Progress has stalled the last few sessions — deload to 90.0kg to reset and chase reps.",
            note(en, bench, "5-8", stalled)
        )
        assertEquals("ความก้าวหน้าหยุดนิ่งมาหลายเซสชัน — ลดเหลือ 90.0kg เพื่อเริ่มใหม่และเพิ่มจำนวนครั้ง", note(th, bench, "5-8", stalled))
    }

    @Test
    fun `the weight numbers are identical in both languages`() {
        val digits = Regex("[0-9]+(\\.[0-9]+)?")
        val h = history(57.5, 8)

        assertEquals(
            digits.findAll(note(en, bench, "5-8", h)!!).map { it.value }.toList(),
            digits.findAll(note(th, bench, "5-8", h)!!).map { it.value }.toList()
        )
    }

    // ---------------- recovery advice ----------------

    private var nextId = 1

    private fun sets(exercise: String, count: Int) = repeat(count) {
        dao.insertedWorkouts.add(
            WorkoutEntity(
                id = nextId++, exerciseName = exercise, weight = 50.0, reps = 5,
                dateInMillis = System.currentTimeMillis() - hour, username = "testuser"
            )
        )
    }

    private fun advice(texts: AppTexts): String = runBlocking {
        withTimeout(10_000) {
            val vm = RecoveryViewModel(repository, texts)
            vm.recoveryStatus.first { it.isNotEmpty() }
            vm.smartRecommendation.first { !it.contains("Analyzing") && !it.contains("กำลังวิเคราะห์") }
        }
    }

    // The first message is read before the background work finishes, using a repository that answers slowly.
    private val slowRepository = GlarmToRepository(object : RecordingFakeGlarmToDao() {
        override fun getWorkoutsBetween(username: String, start: Long, end: Long): List<WorkoutEntity> {
            Thread.sleep(800)
            return emptyList()
        }
    }, StaticFakeSessionManager())

    @Test
    fun `the waiting message is English when the texts are English`() {
        assertEquals("Analyzing your history…", RecoveryViewModel(slowRepository, en).smartRecommendation.value)
    }

    @Test
    fun `the waiting message is Thai when the texts are Thai`() {
        assertEquals("กำลังวิเคราะห์ประวัติของคุณ…", RecoveryViewModel(slowRepository, th).smartRecommendation.value)
    }

    @Test
    fun `fully recovered advice`() {
        assertEquals("You are fully recovered! Go crush any workout today 💪", advice(en))
        assertEquals("คุณฟื้นตัวเต็มที่แล้ว! วันนี้ลุยได้ทุกท่าเลย 💪", advice(th))
    }

    @Test
    fun `exhausted muscle advice names the tired muscle and a fresh one`() {
        sets("Bench Press", 6)

        val english = advice(en)
        val thai = advice(th)

        assertTrue(english, english.startsWith("Your Chest are exhausted. Focus on "))
        assertTrue(thai, thai.startsWith("อก ของคุณล้ามาก วันนี้เน้น "))
        val fresh = listOf("Back", "Legs", "Shoulders", "Arms", "Core")
        assertTrue(english, fresh.any { english.endsWith("Focus on $it today!") })
        assertTrue(thai, thai.contains("วันนี้เน้น ") && thai.endsWith("!"))
    }

    @Test
    fun `rest day advice when everything is worn out`() {
        listOf("Bench Press", "Deadlift", "Squat", "Overhead Press", "Bicep Curl", "Plank").forEach { sets(it, 7) }

        assertEquals("You've been working hard! Everything needs a rest. Take a rest day 🧘‍♂️", advice(en))
        assertEquals("คุณซ้อมหนักมาก! ทุกส่วนต้องพัก พักผ่อนสักวันนะ 🧘‍♂️", advice(th))
    }

    // ---------------- weight suggestions in the workout screen ----------------

    private fun suggestion(texts: AppTexts, weight: Double, reps: Int, rpe: Int?): String? = runBlocking {
        dao.insertedWorkouts.clear()
        dao.insertedWorkouts.add(
            WorkoutEntity(id = 1, exerciseName = "Squat", weight = weight, reps = reps, dateInMillis = 1L, username = "testuser", rpe = rpe)
        )
        val vm = WorkoutViewModel(repository, texts)
        vm.fetchSmartSuggestion("Squat")
        withTimeout(10_000) { vm.smartSuggestion.first { it != null } }
    }

    @Test
    fun `easy set suggests a heavier weight`() {
        assertEquals("Suggestion: Try 62.5kg (Last time: 60.0kg, RPE 6)", suggestion(en, 60.0, 8, 6))
        assertEquals("คำแนะนำ: ลอง 62.5kg (ครั้งก่อน: 60.0kg, RPE 6)", suggestion(th, 60.0, 8, 6))
    }

    @Test
    fun `hard set suggests more reps at the same weight`() {
        assertEquals("Suggestion: Target 60.0kg for 9 reps (Last time: RPE 8)", suggestion(en, 60.0, 8, 8))
        assertEquals("คำแนะนำ: เป้าหมาย 60.0kg x 9 ครั้ง (ครั้งก่อน: RPE 8)", suggestion(th, 60.0, 8, 8))
    }

    @Test
    fun `maximal set suggests staying or dropping`() {
        assertEquals("Suggestion: Stay at 60.0kg or drop to 57.5kg (Last time: RPE 10)", suggestion(en, 60.0, 5, 10))
        assertEquals("คำแนะนำ: คงที่ 60.0kg หรือลดเหลือ 57.5kg (ครั้งก่อน: RPE 10)", suggestion(th, 60.0, 5, 10))
    }

    @Test
    fun `a set without RPE suggests beating the last one`() {
        assertEquals("Suggestion: Last time you did 60.0kg x 8 reps. Try to beat it!", suggestion(en, 60.0, 8, null))
        assertEquals("คำแนะนำ: ครั้งก่อนคุณทำ 60.0kg x 8 ครั้ง ลองทำให้ดีกว่าเดิม!", suggestion(th, 60.0, 8, null))
    }

    @Test
    fun `the drop weight never goes below zero`() {
        assertEquals("Suggestion: Stay at 1.0kg or drop to 0.0kg (Last time: RPE 10)", suggestion(en, 1.0, 5, 10))
    }

    @Test
    fun `the text is fetched when needed so a language switch shows up on the next suggestion`() {
        var current: AppTexts = en
        val switching = AppTexts { id, args -> current.get(id, *args) }

        val first = suggestion(switching, 60.0, 8, 6)
        current = th
        val second = suggestion(switching, 60.0, 8, 6)

        assertTrue(first!!.startsWith("Suggestion:"))
        assertTrue(second!!.startsWith("คำแนะนำ:"))
        assertNotNull(second)
    }
}
