package com.example.routinereminder.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.routinereminder.data.dao.*
import com.example.routinereminder.data.entities.*
import com.example.routinereminder.data.entities.Meal
import com.example.routinereminder.data.entities.Schedule
import com.example.routinereminder.data.entities.Exercise
import com.example.routinereminder.data.entities.ExercisePlan
import com.example.routinereminder.data.entities.LoggedFood
import com.example.routinereminder.data.entities.CalorieEntry
import com.example.routinereminder.data.entities.Weight
import com.example.routinereminder.data.entities.WeightLog
import com.example.routinereminder.data.entities.BlockedCalendarImport
import com.example.routinereminder.data.entities.FoodBundle
import com.example.routinereminder.data.entities.RecipeIngredient
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.data.dao.FoodBundleDao

// --- NEW MIGRATION: adds isDone column to schedule table ---
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // Add the new isDone column to schedule
        database.execSQL(
            """
            ALTER TABLE schedule 
            ADD COLUMN isDone INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )

        // Create the missing schedule_done table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS schedule_done (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                scheduleId INTEGER NOT NULL,
                dateEpochDay INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            ALTER TABLE schedule
            ADD COLUMN reminderCount INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
        database.execSQL(
            """
            ALTER TABLE schedule
            ADD COLUMN reminderIntervalMinutes INTEGER NOT NULL DEFAULT 60
            """.trimIndent()
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS schedule_done_new (
                scheduleId INTEGER NOT NULL,
                dateEpochDay INTEGER NOT NULL,
                PRIMARY KEY(scheduleId, dateEpochDay)
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT OR IGNORE INTO schedule_done_new (scheduleId, dateEpochDay)
            SELECT DISTINCT scheduleId, dateEpochDay FROM schedule_done
            """.trimIndent()
        )
        database.execSQL("DROP TABLE schedule_done")
        database.execSQL("ALTER TABLE schedule_done_new RENAME TO schedule_done")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            ALTER TABLE schedule
            ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
    }
}

@Database(
    entities = [
        Meal::class,
        Schedule::class,
        Exercise::class,
        ExercisePlan::class,
        LoggedFood::class,
        CalorieEntry::class,
        Weight::class,
        WeightLog::class,
        BlockedCalendarImport::class,
        ScheduleDone::class,
        FoodBundle::class,
        RecipeIngredient::class

    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class, DayOfWeekSetConverter::class, LocalDateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun foodBundleDao(): FoodBundleDao

    abstract fun scheduleDoneDao(): ScheduleDoneDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exercisePlanDao(): ExercisePlanDao
    abstract fun loggedFoodDao(): LoggedFoodDao
    abstract fun calorieEntryDao(): CalorieEntryDao
    abstract fun weightDao(): WeightDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun blockedCalendarImportDao(): BlockedCalendarImportDao
}
