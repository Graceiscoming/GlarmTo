package com.example.glarmto.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.glarmto.data.local.dao.GlarmToDao
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.RoutineEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.local.entity.WorkoutSessionEntity

@Database(entities = [WorkoutEntity::class, NutritionEntity::class, UserEntity::class, RoutineEntity::class, WorkoutSessionEntity::class, WaterEntity::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun glarmToDao(): GlarmToDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create user_log table
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_log` (`username` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`username`))")
                // Insert default admin user
                db.execSQL("INSERT OR IGNORE INTO `user_log` (`username`, `createdAt`) VALUES ('admin', ${System.currentTimeMillis()})")
                
                // Add username column to workout_log and nutrition_log
                db.execSQL("ALTER TABLE workout_log ADD COLUMN username TEXT NOT NULL DEFAULT 'admin'")
                db.execSQL("ALTER TABLE nutrition_log ADD COLUMN username TEXT NOT NULL DEFAULT 'admin'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_log ADD COLUMN age INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_log ADD COLUMN isMale INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_log ADD COLUMN weight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_log ADD COLUMN height REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_log ADD COLUMN dailyGoal INTEGER NOT NULL DEFAULT 2500")
                db.execSQL("ALTER TABLE user_log ADD COLUMN profileSetup INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routine_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `routineName` TEXT NOT NULL, `exercises` TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_log ADD COLUMN xp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_log ADD COLUMN level INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_log ADD COLUMN defaultRestSeconds INTEGER NOT NULL DEFAULT 60")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_log ADD COLUMN dailyXPEarned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_log ADD COLUMN lastXPDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_log ADD COLUMN goal TEXT NOT NULL DEFAULT 'Maintain'")
                db.execSQL("ALTER TABLE user_log ADD COLUMN workoutDays INTEGER NOT NULL DEFAULT 3")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_sessions` (`sessionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTimeInMillis` INTEGER NOT NULL, `endTimeInMillis` INTEGER, `durationSeconds` INTEGER NOT NULL, `dateInMillis` INTEGER NOT NULL, `username` TEXT NOT NULL)")
                db.execSQL("ALTER TABLE workout_log ADD COLUMN sessionId INTEGER")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN sessionName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN exhaustionLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN satisfactionLevel INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_log ADD COLUMN macroProteinPct INTEGER NOT NULL DEFAULT 30")
                db.execSQL("ALTER TABLE user_log ADD COLUMN macroCarbPct INTEGER NOT NULL DEFAULT 40")
                db.execSQL("ALTER TABLE user_log ADD COLUMN macroFatPct INTEGER NOT NULL DEFAULT 30")
                db.execSQL("ALTER TABLE user_log ADD COLUMN dailyWaterGoalMl INTEGER NOT NULL DEFAULT 2000")
                db.execSQL("ALTER TABLE workout_log ADD COLUMN rpe INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `water_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `dateInMillis` INTEGER NOT NULL, `amountMl` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_log ADD COLUMN password TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glarmto_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
