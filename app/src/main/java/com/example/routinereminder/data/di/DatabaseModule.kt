package com.example.routinereminder.data.di

import android.content.Context
import androidx.room.Room
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.MIGRATION_11_12
import com.example.routinereminder.data.MIGRATION_12_13
import com.example.routinereminder.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "routine_reminder_db"
        )
            .addMigrations(MIGRATION_11_12, MIGRATION_12_13)
            .fallbackToDestructiveMigration()
            .build()


    @Provides fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()
    @Provides fun provideScheduleDao(db: AppDatabase): ScheduleDao = db.scheduleDao()
    @Provides fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideLoggedFoodDao(db: AppDatabase): LoggedFoodDao = db.loggedFoodDao()
    @Provides fun provideCalorieEntryDao(db: AppDatabase): CalorieEntryDao = db.calorieEntryDao()
    @Provides fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()
    @Provides fun provideBlockedCalendarImportDao(db: AppDatabase): BlockedCalendarImportDao = db.blockedCalendarImportDao()
}
