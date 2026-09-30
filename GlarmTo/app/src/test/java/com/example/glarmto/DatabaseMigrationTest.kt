package com.example.glarmto

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.AppDatabase
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Opens a hand-built version 12 database with the real Room database class, so Room runs the real
 * migrations and then validates the result against the current entities. If the migration and the
 * entities ever disagree, opening the database throws and these tests fail.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DatabaseMigrationTest {

    private lateinit var context: Context
    private val dbName = "migration_test.db"
    private var db: AppDatabase? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        db?.close()
        context.deleteDatabase(dbName)
    }

    /** The schema as it was at version 12, before `xpAwarded` existed. */
    private fun createVersion12Database() {
        val file: File = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL(
            """CREATE TABLE `user_log` (`username` TEXT NOT NULL, `password` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
            `age` INTEGER NOT NULL, `isMale` INTEGER NOT NULL, `weight` REAL NOT NULL, `height` REAL NOT NULL,
            `dailyGoal` INTEGER NOT NULL, `profileSetup` INTEGER NOT NULL, `xp` INTEGER NOT NULL, `level` INTEGER NOT NULL,
            `defaultRestSeconds` INTEGER NOT NULL, `dailyXPEarned` INTEGER NOT NULL, `lastXPDate` INTEGER NOT NULL,
            `goal` TEXT NOT NULL, `workoutDays` INTEGER NOT NULL, `macroProteinPct` INTEGER NOT NULL,
            `macroCarbPct` INTEGER NOT NULL, `macroFatPct` INTEGER NOT NULL, `dailyWaterGoalMl` INTEGER NOT NULL,
            PRIMARY KEY(`username`))"""
        )
        raw.execSQL(
            """CREATE TABLE `workout_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exerciseName` TEXT NOT NULL,
            `weight` REAL NOT NULL, `reps` INTEGER NOT NULL, `dateInMillis` INTEGER NOT NULL, `username` TEXT NOT NULL,
            `sessionId` INTEGER, `rpe` INTEGER)"""
        )
        raw.execSQL(
            """CREATE TABLE `nutrition_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `foodName` TEXT NOT NULL,
            `calories` INTEGER NOT NULL, `dateInMillis` INTEGER NOT NULL, `username` TEXT NOT NULL)"""
        )
        raw.execSQL(
            """CREATE TABLE `workout_sessions` (`sessionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `startTimeInMillis` INTEGER NOT NULL, `endTimeInMillis` INTEGER, `durationSeconds` INTEGER NOT NULL,
            `dateInMillis` INTEGER NOT NULL, `username` TEXT NOT NULL, `sessionName` TEXT NOT NULL, `notes` TEXT NOT NULL,
            `exhaustionLevel` INTEGER NOT NULL, `satisfactionLevel` INTEGER NOT NULL)"""
        )
        raw.execSQL(
            """CREATE TABLE `routine_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL,
            `routineName` TEXT NOT NULL, `exercises` TEXT NOT NULL)"""
        )
        raw.execSQL(
            """CREATE TABLE `water_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL,
            `dateInMillis` INTEGER NOT NULL, `amountMl` INTEGER NOT NULL)"""
        )
        raw.execSQL(
            "INSERT INTO user_log VALUES ('gym', 'secret', 1, 25, 1, 70.0, 175.0, 2500, 1, 420, 4, 60, 30, 99, 'Maintain', 3, 30, 40, 30, 2000)"
        )
        raw.execSQL("INSERT INTO workout_log VALUES (1, 'Squat', 100.0, 5, 1000, 'gym', NULL, 8)")
        raw.execSQL("INSERT INTO workout_log VALUES (2, 'Bench Press', 60.0, 8, 2000, 'gym', 7, NULL)")
        raw.execSQL("INSERT INTO nutrition_log VALUES (1, 'Rice', 300, 1000, 'gym')")
        raw.execSQL("INSERT INTO water_log VALUES (1, 'gym', 1000, 250)")
        raw.version = 12
        raw.close()
    }

    private fun openWithRealMigrations(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { db = it }

    @Test
    fun `migrating 12 to 13 opens cleanly and keeps every existing row`() {
        createVersion12Database()

        val dao = openWithRealMigrations().glarmToDao()

        val workouts = dao.getAllWorkoutsForUser("gym")
        assertEquals(2, workouts.size)
        assertEquals("Squat", workouts[0].exerciseName)
        assertEquals(100.0, workouts[0].weight, 0.0)
        assertEquals(8, workouts[0].rpe)
        assertEquals(7, workouts[1].sessionId)
        assertEquals(1, dao.getAllNutritionForUser("gym").size)
        assertEquals(1, dao.getAllWaterForUser("gym").size)
    }

    @Test
    fun `existing sets and meals get the legacy xp defaults`() {
        createVersion12Database()

        val dao = openWithRealMigrations().glarmToDao()

        // Deleting these used to take back 10 per set and 5 per meal, so that is what they are credited with.
        assertEquals(10, dao.getWorkoutXp(1))
        assertEquals(10, dao.getWorkoutXp(2))
        assertEquals(5, dao.getNutritionXp(1))
    }

    @Test
    fun `new rows after migration start at zero xp and can be updated`() {
        createVersion12Database()
        val dao = openWithRealMigrations().glarmToDao()

        val workoutId = dao.insertWorkout(
            WorkoutEntity(exerciseName = "Row", weight = 50.0, reps = 10, dateInMillis = 3000, username = "gym")
        ).toInt()
        val mealId = dao.insertNutrition(
            NutritionEntity(foodName = "Egg", calories = 80, dateInMillis = 3000, username = "gym")
        ).toInt()

        assertEquals(0, dao.getWorkoutXp(workoutId))
        assertEquals(0, dao.getNutritionXp(mealId))

        dao.setWorkoutXp(workoutId, 7)
        dao.setNutritionXp(mealId, 3)

        assertEquals(7, dao.getWorkoutXp(workoutId))
        assertEquals(3, dao.getNutritionXp(mealId))
    }

    @Test
    fun `xp lookups for a missing row return null`() {
        createVersion12Database()
        val dao = openWithRealMigrations().glarmToDao()

        assertEquals(null, dao.getWorkoutXp(999))
        assertEquals(null, dao.getNutritionXp(999))
        assertEquals(0, dao.setWorkoutXp(999, 5))
    }

    @Test
    fun `the user row survives the migration untouched`() {
        createVersion12Database()
        val database = openWithRealMigrations()

        val cursor = database.openHelper.readableDatabase.query("SELECT username, password, xp, level FROM user_log")
        assertTrue(cursor.moveToFirst())
        assertEquals("gym", cursor.getString(0))
        assertEquals("secret", cursor.getString(1))
        assertEquals(420L, cursor.getLong(2))
        assertEquals(4, cursor.getInt(3))
        cursor.close()
    }

    @Test
    fun `the migration chain has one step per version up to 13`() {
        val migrations = AppDatabase.ALL_MIGRATIONS
        assertEquals(12, migrations.size)
        migrations.forEachIndexed { index, m ->
            assertEquals(index + 1, m.startVersion)
            assertEquals(index + 2, m.endVersion)
        }
    }
}
