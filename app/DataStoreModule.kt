package com.example.routinereminder.data

import android.content.Context
import androidx.room.Room
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "routine_reminder_db"
        )
            // Ensures schema updates don’t crash on version change
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCalorieEntryDao(db: AppDatabase): CalorieEntryDao = db.calorieEntryDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()

    @Provides
    fun provideLoggedFoodDao(db: AppDatabase): LoggedFoodDao = db.loggedFoodDao()

    @Provides
    fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()

    @Provides
    fun provideBlockedCalendarImportDao(db: AppDatabase): BlockedCalendarImportDao =
        db.blockedCalendarImportDao()
}
