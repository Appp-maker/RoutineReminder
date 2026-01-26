package com.example.routinereminder.data

import android.content.Context
import com.example.routinereminder.data.entities.FoodBundle
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.data.entities.LoggedFood
import com.example.routinereminder.data.entities.PORTION_TYPE_GRAMS
import com.example.routinereminder.data.entities.RecipeIngredient
import com.example.routinereminder.data.entities.Schedule
import com.example.routinereminder.data.workout.WorkoutPlan
import com.example.routinereminder.data.workout.WorkoutPlanExercise
import com.example.routinereminder.data.workout.WorkoutPlanRepository
import com.example.routinereminder.ui.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import org.maplibre.geojson.Point

@Singleton
class ExampleDataSeeder @Inject constructor(
    private val appDatabase: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val workoutPlanRepository: WorkoutPlanRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun seedIfNeeded() {
        if (settingsRepository.getExampleDataSeeded().first()) {
            return
        }

        val hasScheduleData = appDatabase.scheduleDao().getAllOnce().isNotEmpty()
        val hasFoodData = appDatabase.loggedFoodDao().getAllOnce().isNotEmpty()
        val hasBundles = appDatabase.foodBundleDao().getAllBundles().isNotEmpty()
        val hasSessions = SessionStore.loadAllSessions(context).isNotEmpty()
        val hasWorkoutPlans = workoutPlanRepository.planState.first().plans.isNotEmpty()
        if (hasScheduleData || hasFoodData || hasBundles || hasSessions || hasWorkoutPlans) {
            settingsRepository.setExampleDataSeeded(true)
            return
        }

        val themeColors = settingsRepository.getAppThemeColors().first()
        seedSchedules(themeColors.primary, themeColors.secondary)
        seedFood(themeColors.primary, themeColors.secondary)
        seedWorkoutPlans()
        seedMapSession()
        settingsRepository.setExampleDataSeeded(true)
    }

    private suspend fun seedSchedules(primaryColor: Int, secondaryColor: Int) {
        val today = LocalDate.now()
        val scheduleDao = appDatabase.scheduleDao()

        scheduleDao.insert(
            Schedule(
                name = "Morning mobility",
                notes = "5–10 min stretch + breathing",
                hour = 7,
                minute = 30,
                durationMinutes = 15,
                startEpochDay = today.toEpochDay(),
                repeatOnDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                repeatEveryWeeks = 1,
                notifyEnabled = true,
                showDetailsInNotification = true,
                colorArgb = primaryColor
            )
        )

        scheduleDao.insert(
            Schedule(
                name = "Daily focus block",
                notes = "Pick one important task and go 25 minutes",
                hour = 14,
                minute = 0,
                durationMinutes = 25,
                startEpochDay = today.toEpochDay(),
                repeatOnDays = DayOfWeek.values().toSet(),
                repeatEveryWeeks = 1,
                notifyEnabled = true,
                showDetailsInNotification = true,
                colorArgb = secondaryColor
            )
        )

        scheduleDao.insert(
            Schedule(
                name = "Evening reflection",
                notes = "Log a win + set tomorrow's top 3",
                hour = 20,
                minute = 30,
                durationMinutes = 10,
                isOneTime = true,
                dateEpochDay = today.toEpochDay(),
                notifyEnabled = true,
                showDetailsInNotification = true,
                colorArgb = primaryColor
            )
        )
    }

    private suspend fun seedFood(primaryColor: Int, secondaryColor: Int) {
        val today = LocalDate.now()
        val bundleDao = appDatabase.foodBundleDao()
        val loggedFoodDao = appDatabase.loggedFoodDao()

        val bundleId = bundleDao.insertBundle(
            FoodBundle(
                name = "Power breakfast",
                description = "Greek yogurt + berries + granola",
                portionType = PORTION_TYPE_GRAMS
            )
        )

        bundleDao.insertIngredients(
            listOf(
                RecipeIngredient(
                    bundleId = bundleId,
                    name = "Greek yogurt",
                    portionSizeG = 150,
                    calories = 150.0,
                    proteinG = 15.0,
                    carbsG = 9.0,
                    fatG = 4.0,
                    fiberG = 0.0,
                    saturatedFatG = 2.5,
                    addedSugarsG = 0.0,
                    sodiumMg = 80.0
                ),
                RecipeIngredient(
                    bundleId = bundleId,
                    name = "Mixed berries",
                    portionSizeG = 100,
                    calories = 50.0,
                    proteinG = 1.0,
                    carbsG = 12.0,
                    fatG = 0.5,
                    fiberG = 4.0,
                    saturatedFatG = 0.0,
                    addedSugarsG = 0.0,
                    sodiumMg = 1.0
                ),
                RecipeIngredient(
                    bundleId = bundleId,
                    name = "Granola",
                    portionSizeG = 30,
                    calories = 140.0,
                    proteinG = 3.0,
                    carbsG = 21.0,
                    fatG = 5.0,
                    fiberG = 2.0,
                    saturatedFatG = 1.0,
                    addedSugarsG = 4.0,
                    sodiumMg = 40.0
                )
            )
        )

        loggedFoodDao.insert(
            LoggedFood(
                date = today.toString(),
                foodProduct = FoodProduct(
                    name = "Oatmeal bowl",
                    caloriesPer100g = 110.0,
                    proteinPer100g = 4.0,
                    carbsPer100g = 19.0,
                    fatPer100g = 2.0,
                    fiberPer100g = 3.0,
                    saturatedFatPer100g = 0.5,
                    addedSugarsPer100g = 1.0,
                    sodiumPer100g = 55.0
                ),
                portionSizeG = 180.0,
                calories = 198.0,
                proteinG = 7.2,
                carbsG = 34.2,
                fatG = 3.6,
                fiberG = 5.4,
                saturatedFatG = 0.9,
                addedSugarsG = 1.8,
                sodiumMg = 99.0,
                mealSlot = "Breakfast",
                colorArgb = primaryColor
            )
        )

        loggedFoodDao.insert(
            LoggedFood(
                date = today.toString(),
                foodProduct = FoodProduct(
                    name = "Chicken quinoa salad",
                    caloriesPer100g = 135.0,
                    proteinPer100g = 12.0,
                    carbsPer100g = 10.0,
                    fatPer100g = 4.5,
                    fiberPer100g = 2.0,
                    saturatedFatPer100g = 1.0,
                    addedSugarsPer100g = 0.5,
                    sodiumPer100g = 120.0
                ),
                portionSizeG = 250.0,
                calories = 337.5,
                proteinG = 30.0,
                carbsG = 25.0,
                fatG = 11.2,
                fiberG = 5.0,
                saturatedFatG = 2.5,
                addedSugarsG = 1.2,
                sodiumMg = 300.0,
                mealSlot = "Lunch",
                colorArgb = secondaryColor
            )
        )
    }

    private suspend fun seedWorkoutPlans() {
        val planId = UUID.randomUUID().toString()
        val exercises = listOf(
            WorkoutPlanExercise(
                id = UUID.randomUUID().toString(),
                name = "Bodyweight squats",
                bodyPart = "upper legs",
                target = "quads",
                equipment = "body weight",
                sets = 3,
                repetitions = 12,
                restSeconds = 60
            ),
            WorkoutPlanExercise(
                id = UUID.randomUUID().toString(),
                name = "Incline push-ups",
                bodyPart = "chest",
                target = "pectorals",
                equipment = "body weight",
                sets = 3,
                repetitions = 10,
                restSeconds = 60
            ),
            WorkoutPlanExercise(
                id = UUID.randomUUID().toString(),
                name = "Plank hold",
                bodyPart = "waist",
                target = "abs",
                equipment = "body weight",
                durationMinutes = 1,
                restSeconds = 45
            )
        )
        val plan = WorkoutPlan(
            id = planId,
            name = "Starter circuit",
            exercises = exercises
        )
        workoutPlanRepository.savePlans(listOf(plan), planId)
    }

    private suspend fun seedMapSession() {
        val startEpochMs = System.currentTimeMillis() - (45 * 60 * 1000)
        val endEpochMs = startEpochMs + (38 * 60 * 1000)
        val durationSec = (endEpochMs - startEpochMs) / 1000
        val distanceMeters = 5200.0
        val avgPace = (durationSec / (distanceMeters / 1000.0)).toLong()
        val trail = listOf(
            Point.fromLngLat(-122.084, 37.421),
            Point.fromLngLat(-122.082, 37.4225),
            Point.fromLngLat(-122.08, 37.424),
            Point.fromLngLat(-122.079, 37.4255),
            Point.fromLngLat(-122.078, 37.4268)
        )
        val session = com.example.routinereminder.data.model.SessionStats(
            id = UUID.randomUUID().toString(),
            activity = "Run/Walk",
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            durationSec = durationSec,
            distanceMeters = distanceMeters,
            calories = 330.0,
            avgPaceSecPerKm = avgPace,
            splitPaceSecPerKm = listOf(avgPace, avgPace + 10),
            polyline = trail
        )
        SessionStore.saveSession(context, session)
    }
}
