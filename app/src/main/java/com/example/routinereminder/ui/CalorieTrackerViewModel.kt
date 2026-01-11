package com.example.routinereminder.ui

import java.time.DayOfWeek
import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.data.ActivityLevel
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.CalorieGoal
import com.example.routinereminder.data.Gender
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.data.entities.LoggedFood
import com.example.routinereminder.data.entities.FoodBundleWithItems
import com.example.routinereminder.data.entities.CalorieEntry
import com.example.routinereminder.data.OpenFoodFactsApiClient
import com.example.routinereminder.data.SettingsRepository
import com.example.routinereminder.data.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.routinereminder.data.entities.FoodBundle

@HiltViewModel
class CalorieTrackerViewModel @Inject constructor(
    application: Application,
    private val appDatabase: AppDatabase,
    private val settingsRepository: SettingsRepository

) : AndroidViewModel(application) {

    private val openFoodFactsApiClient = OpenFoodFactsApiClient()

    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()


    private val _dailyTotals = MutableStateFlow<DailyTotals?>(null)
    val dailyTotals: StateFlow<DailyTotals?> = _dailyTotals.asStateFlow()

    private val _dailyTargets = MutableStateFlow<DailyTargets?>(null)
    val dailyTargets: StateFlow<DailyTargets?> = _dailyTargets.asStateFlow()

    private val _scannedFoodProduct = MutableStateFlow<FoodProduct?>(null)
    val scannedFoodProduct: StateFlow<FoodProduct?> = _scannedFoodProduct.asStateFlow()

    private val _isProfileComplete = MutableStateFlow(false)
    val isProfileComplete: StateFlow<Boolean> = _isProfileComplete.asStateFlow()

    private val _loggedFoods = MutableStateFlow<List<LoggedFood>>(emptyList())
    val loggedFoods: StateFlow<List<LoggedFood>> = _loggedFoods.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FoodProduct>>(emptyList())
    val searchResults: StateFlow<List<FoodProduct>> = _searchResults.asStateFlow()
    private val _foodBundles = MutableStateFlow<List<FoodBundle>>(emptyList())
    val foodBundles: StateFlow<List<FoodBundle>> = _foodBundles.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    private val _pendingBundlePreview = MutableStateFlow<Long?>(null)
    val pendingBundlePreview = _pendingBundlePreview.asStateFlow()
    private val _foodConsumedTrackingEnabled = MutableStateFlow(false)
    val foodConsumedTrackingEnabled = _foodConsumedTrackingEnabled.asStateFlow()

    fun requestBundlePreview(bundleId: Long) {
        _pendingBundlePreview.value = bundleId
    }

    fun clearBundlePreview() {
        _pendingBundlePreview.value = null
    }


    init {
        viewModelScope.launch {
            settingsRepository.getUserSettings().collectLatest { settings ->
                _userSettings.value = settings
                _isProfileComplete.value = settings != null && settings.weightKg > 0 && settings.heightCm > 0 && settings.age > 0
                calculateDailyTargets()
                calculateDailyTotals()
            }
        }
        viewModelScope.launch {
            settingsRepository.getFoodConsumedTrackingEnabled().collectLatest { enabled ->
                _foodConsumedTrackingEnabled.value = enabled
                calculateDailyTotals()
            }
        }
        viewModelScope.launch {
            _foodBundles.value = appDatabase.foodBundleDao().getAllBundles()
        }
        viewModelScope.launch {
            selectedDate.collectLatest {
                _loggedFoods.value = appDatabase.loggedFoodDao().getFoodsForDate(date = it.toString())

                calculateDailyTotals()
                calculateDailyTargets()
            }
        }
    }

    fun selectBundle(bundleId: Long) {
        viewModelScope.launch {
            _pendingBundlePreview.value = bundleId
            val foodProduct = buildBundleAsFoodProduct(bundleId)
            _scannedFoodProduct.value = foodProduct
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectToday() {
        _selectedDate.value = LocalDate.now()
    }
    suspend fun buildBundleAsFoodProduct(bundleId: Long): FoodProduct {
        val bundleWithItems = appDatabase
            .foodBundleDao()
            .getBundleWithItems(bundleId)

        val totalGrams = bundleWithItems.items
            .sumOf { it.portionSizeG }
            .toDouble()

        if (totalGrams <= 0) {
            return FoodProduct(
                name = bundleWithItems.bundle.name,
                caloriesPer100g = 0.0,
                proteinPer100g = 0.0,
                carbsPer100g = 0.0,
                fatPer100g = 0.0,
                fiberPer100g = 0.0,
                saturatedFatPer100g = 0.0,
                addedSugarsPer100g = 0.0,
                sodiumPer100g = 0.0
            )
        }

        val factor = 100.0 / totalGrams

        return FoodProduct(
            name = bundleWithItems.bundle.name, // food name = bundle name

            caloriesPer100g =
                bundleWithItems.items.sumOf { it.calories } * factor,

            proteinPer100g =
                bundleWithItems.items.sumOf { it.proteinG } * factor,

            carbsPer100g =
                bundleWithItems.items.sumOf { it.carbsG } * factor,

            fatPer100g =
                bundleWithItems.items.sumOf { it.fatG } * factor,

            fiberPer100g =
                bundleWithItems.items.sumOf { it.fiberG } * factor,

            saturatedFatPer100g =
                bundleWithItems.items.sumOf { it.saturatedFatG } * factor,

            addedSugarsPer100g =
                bundleWithItems.items.sumOf { it.addedSugarsG } * factor,

            sodiumPer100g =
                (bundleWithItems.items.sumOf { it.sodiumMg } * factor) / 1000.0
        )
    }

    fun deleteRecurringEntries(food: LoggedFood) {
        viewModelScope.launch {
            if (!food.isOneTime && food.startEpochDay != null) {
                appDatabase.loggedFoodDao().deleteFoodSeriesFromDate(
                    startEpochDay = food.startEpochDay,
                    mealSlot = food.mealSlot,
                    foodName = food.foodProduct.name,
                    fromEpochDay = selectedDate.value.toEpochDay()
                )
            }

            refreshForSelectedDate()
        }
    }






    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _scannedFoodProduct.value = openFoodFactsApiClient.getFoodProduct(barcode)
        }
    }

    fun searchFood(query: String) {
        val cleanedQuery = query.trim()

        if (cleanedQuery.length < 3) {
            _searchResults.value = emptyList()
            _searchError.value = "Please enter at least 3 characters"
            return
        }

        viewModelScope.launch {
            _searchError.value = null
            _searchResults.value = emptyList() // clear previous results

            try {
                val results = withContext(Dispatchers.IO) {
                    openFoodFactsApiClient.searchFood(cleanedQuery)
                }

                _searchResults.value = results

                if (results.isEmpty()) {
                    _searchError.value = "No results found"
                }

            } catch (e: SocketTimeoutException) {
                _searchError.value = "Network timeout. Try again."
                _searchResults.value = emptyList()

            } catch (e: IOException) {
                _searchError.value = "Network error. Check your connection."
                _searchResults.value = emptyList()

            } catch (e: Exception) {
                _searchError.value = "Search failed"
                _searchResults.value = emptyList()
            }
        }
    }





    fun onFoodSelected(foodProduct: FoodProduct) {
        _scannedFoodProduct.value = foodProduct
    }

    fun clearScannedProduct() {
        _scannedFoodProduct.value = null
        clearBundlePreview()
    }


//    fun deleteLoggedFood(loggedFood: LoggedFood) {
//        viewModelScope.launch {
//            appDatabase.loggedFoodDao().delete(loggedFood)
//            calculateDailyTotals()
//            _loggedFoods.value = appDatabase.loggedFoodDao().getFoodsForDate(date = selectedDate.value.toString())
//        }
//    }

    fun deleteLoggedFood(food: LoggedFood) {
        viewModelScope.launch {
            appDatabase.loggedFoodDao().delete(food)
            refreshForSelectedDate()
        }
    }

    fun logCaloriesConsumed(
        calories: Int,
        label: String,
        date: LocalDate = LocalDate.now(),
        mealSlot: String = "unspecified"
    ) {
        if (calories <= 0) return
        viewModelScope.launch {
            val foodProduct = FoodProduct(
                name = label,
                caloriesPer100g = calories.toDouble(),
                proteinPer100g = 0.0,
                carbsPer100g = 0.0,
                fatPer100g = 0.0,
                fiberPer100g = 0.0,
                saturatedFatPer100g = 0.0,
                addedSugarsPer100g = 0.0,
                sodiumPer100g = 0.0
            )
            val loggedFood = LoggedFood(
                date = date.toString(),
                foodProduct = foodProduct,
                portionSizeG = 100.0,
                calories = calories.toDouble(),
                proteinG = 0.0,
                carbsG = 0.0,
                fatG = 0.0,
                fiberG = 0.0,
                saturatedFatG = 0.0,
                addedSugarsG = 0.0,
                sodiumMg = 0.0,
                isConsumed = false,
                mealSlot = mealSlot,
                isOneTime = true,
                dateEpochDay = date.toEpochDay()
            )
            appDatabase.loggedFoodDao().upsert(loggedFood)
            if (date == selectedDate.value) {
                refreshForSelectedDate()
            }
        }
    }

    fun logWorkoutCaloriesRequired(
        calories: Int,
        label: String,
        date: LocalDate = LocalDate.now()
    ) {
        if (calories <= 0) return
        viewModelScope.launch {
            val entry = CalorieEntry(
                dateEpochDay = date.toEpochDay(),
                calories = calories,
                label = label
            )
            appDatabase.calorieEntryDao().insert(entry)
            if (date == selectedDate.value) {
                calculateDailyTargets()
            }
        }
    }



    fun addCustomFood(
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        date: LocalDate
    ) {
        // Interpret inputs as “per 100 g” for now – this matches FoodProduct.
        // (If you enter totals for a 100 g portion, the behaviour is identical.)
        val product = FoodProduct(
            name = name,
            caloriesPer100g = calories.toDouble(),
            proteinPer100g = protein.toDouble(),
            carbsPer100g = carbs.toDouble(),
            fatPer100g = fat.toDouble(),
            fiberPer100g = 0.0,
            saturatedFatPer100g = 0.0,
            addedSugarsPer100g = 0.0,
            sodiumPer100g = 0.0
        )

        // Re-use the same flow as a scanned product:
        // the UI already shows PortionDialog whenever scannedFoodProduct != null
        _scannedFoodProduct.value = product
        // selectedDate is already used by addFood via PortionDialog’s startDate,
        // so we don’t need to use `date` directly here.
    }



    fun updateLoggedFood(
        loggedFood: LoggedFood,
        newPortionSizeG: Double,
        mealSlot: String,
        isOneTime: Boolean,
        repeatDays: Set<DayOfWeek>,
        repeatEveryWeeks: Int,
        startDate: LocalDate,
        updatedFoodProduct: FoodProduct,
        colorArgb: Int,
        updateAllFuture: Boolean = false
    ) {
        viewModelScope.launch {
            val dao = appDatabase.loggedFoodDao()
            val foodProduct = loggedFood.foodProduct

            val calories = (foodProduct.caloriesPer100g / 100.0) * newPortionSizeG
            val protein = (foodProduct.proteinPer100g / 100.0) * newPortionSizeG
            val carbs = (foodProduct.carbsPer100g / 100.0) * newPortionSizeG
            val fat = (foodProduct.fatPer100g / 100.0) * newPortionSizeG
            val fiber = (foodProduct.fiberPer100g / 100.0) * newPortionSizeG
            val saturatedFat = (foodProduct.saturatedFatPer100g / 100.0) * newPortionSizeG
            val addedSugars = (foodProduct.addedSugarsPer100g / 100.0) * newPortionSizeG
            val sodium = (foodProduct.sodiumPer100g / 100.0) * newPortionSizeG * 1000

            // Define updated recurrence fields
            val newRepeatDays = if (isOneTime) emptySet() else repeatDays
            val newRepeatEveryWeeks = if (isOneTime) 1 else repeatEveryWeeks
            val newStartEpochDay = if (isOneTime) null else startDate.toEpochDay()
            val newDateEpochDay = if (isOneTime) startDate.toEpochDay() else null

            if (updateAllFuture && !loggedFood.isOneTime && loggedFood.startEpochDay != null) {
                // Update all future entries in this recurrence pattern
                val allRecurring = dao.getAllRecurringFromStart(loggedFood.startEpochDay!!)
                for (entry in allRecurring) {
                    val updated = entry.copy(
                        portionSizeG = newPortionSizeG,
                        calories = calories,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat,
                        fiberG = fiber,
                        saturatedFatG = saturatedFat,
                        addedSugarsG = addedSugars,
                        sodiumMg = sodium,
                        isOneTime = isOneTime,
                        repeatOnDays = newRepeatDays,
                        repeatEveryWeeks = newRepeatEveryWeeks,
                        startEpochDay = newStartEpochDay,
                        dateEpochDay = newDateEpochDay,
                        colorArgb = colorArgb
                    )
                    dao.upsert(updated)
                }
            } else {
                // Update only this entry
                val updatedFood = loggedFood.copy(
                    portionSizeG = newPortionSizeG,
                    calories = calories,
                    proteinG = protein,
                    carbsG = carbs,
                    fatG = fat,
                    fiberG = fiber,
                    saturatedFatG = saturatedFat,
                    addedSugarsG = addedSugars,
                    sodiumMg = sodium,
                    mealSlot = mealSlot,
                    isOneTime = isOneTime,
                    repeatOnDays = newRepeatDays,
                    repeatEveryWeeks = newRepeatEveryWeeks,
                    startEpochDay = newStartEpochDay,
                    dateEpochDay = newDateEpochDay,
                    colorArgb = colorArgb
                )
                dao.upsert(updatedFood)
            }

            calculateDailyTotals()

        }
    }

    private suspend fun refreshForSelectedDate() {
        _loggedFoods.value =
            appDatabase.loggedFoodDao()
                .getFoodsForDate(selectedDate.value.toString())

        calculateDailyTotals()
        calculateDailyTargets()
    }

    fun addBundleToTracker(
        bundleId: Long,
        mealSlot: String,
        portionSizeG: Double,
        isOneTime: Boolean,
        repeatDays: Set<DayOfWeek>,
        repeatEveryWeeks: Int,
        startDate: LocalDate,
        colorArgb: Int
    ) {
        viewModelScope.launch {
            val bundleDao = appDatabase.foodBundleDao()
            val loggedFoodDao = appDatabase.loggedFoodDao()
            val bundleWithItems = bundleDao.getBundleWithItems(bundleId)
            if (portionSizeG <= 0.0) return@launch

            val foodProduct = buildBundleAsFoodProduct(bundleId)
            val caloriesPerGram = foodProduct.caloriesPer100g / 100.0
            val proteinPerGram = foodProduct.proteinPer100g / 100.0
            val carbsPerGram = foodProduct.carbsPer100g / 100.0
            val fatPerGram = foodProduct.fatPer100g / 100.0
            val fiberPerGram = foodProduct.fiberPer100g / 100.0
            val saturatedFatPerGram = foodProduct.saturatedFatPer100g / 100.0
            val addedSugarsPerGram = foodProduct.addedSugarsPer100g / 100.0
            val sodiumPerGram = foodProduct.sodiumPer100g / 100.0

            if (isOneTime || repeatDays.isEmpty()) {
                val loggedFood = LoggedFood(
                    date = startDate.toString(),
                    foodProduct = foodProduct,
                    portionSizeG = portionSizeG,
                    calories = caloriesPerGram * portionSizeG,
                    proteinG = proteinPerGram * portionSizeG,
                    carbsG = carbsPerGram * portionSizeG,
                    fatG = fatPerGram * portionSizeG,
                    fiberG = fiberPerGram * portionSizeG,
                    saturatedFatG = saturatedFatPerGram * portionSizeG,
                    addedSugarsG = addedSugarsPerGram * portionSizeG,
                    sodiumMg = sodiumPerGram * portionSizeG,
                    isConsumed = false,
                    mealSlot = mealSlot,
                    bundleName = bundleWithItems.bundle.name,
                    bundleId = bundleWithItems.bundle.id,
                    isOneTime = true,
                    dateEpochDay = startDate.toEpochDay(),
                    colorArgb = colorArgb
                )

                loggedFoodDao.upsert(loggedFood)
            } else {
                val maxWeeksToGenerate = 12
                for (weekOffset in 0 until maxWeeksToGenerate step repeatEveryWeeks) {
                    val weekStart = startDate.plusWeeks(weekOffset.toLong())

                    for (day in repeatDays) {
                        val dayOffset = (day.value - weekStart.dayOfWeek.value).let {
                            if (it < 0) it + 7 else it
                        }
                        val targetDate = weekStart.plusDays(dayOffset.toLong())

                        if (!targetDate.isBefore(startDate)) {
                            val recurringFood = LoggedFood(
                                date = targetDate.toString(),
                                dateEpochDay = targetDate.toEpochDay(),
                                foodProduct = foodProduct,
                                portionSizeG = portionSizeG,
                                calories = caloriesPerGram * portionSizeG,
                                proteinG = proteinPerGram * portionSizeG,
                                carbsG = carbsPerGram * portionSizeG,
                                fatG = fatPerGram * portionSizeG,
                                fiberG = fiberPerGram * portionSizeG,
                                saturatedFatG = saturatedFatPerGram * portionSizeG,
                                addedSugarsG = addedSugarsPerGram * portionSizeG,
                                sodiumMg = sodiumPerGram * portionSizeG,
                                isConsumed = false,
                                mealSlot = mealSlot,
                                bundleName = bundleWithItems.bundle.name,
                                bundleId = bundleWithItems.bundle.id,
                                isOneTime = false,
                                startEpochDay = startDate.toEpochDay(),
                                repeatOnDays = repeatDays,
                                repeatEveryWeeks = repeatEveryWeeks,
                                colorArgb = colorArgb
                            )

                            loggedFoodDao.upsert(recurringFood)
                        }
                    }
                }
            }

            refreshForSelectedDate()
        }
    }

    suspend fun getBundleById(bundleId: Long): FoodBundle? {
        return appDatabase.foodBundleDao().getBundleById(bundleId)
    }


    fun previewBundleAsFoodProduct(bundleId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val bundleWithItems = appDatabase.foodBundleDao().getBundleWithItems(bundleId)

            val totalGrams = bundleWithItems.items.sumOf { it.portionSizeG }.toDouble()
            if (totalGrams <= 0.0) return@launch

            val totalCalories = bundleWithItems.items.sumOf { it.calories }
            val totalProtein = bundleWithItems.items.sumOf { it.proteinG }
            val totalCarbs = bundleWithItems.items.sumOf { it.carbsG }
            val totalFat = bundleWithItems.items.sumOf { it.fatG }
            val totalFiber = bundleWithItems.items.sumOf { it.fiberG }
            val totalSatFat = bundleWithItems.items.sumOf { it.saturatedFatG }
            val totalSugar = bundleWithItems.items.sumOf { it.addedSugarsG }
            val totalSodiumMg = bundleWithItems.items.sumOf { it.sodiumMg }

            val per100Factor = 100.0 / totalGrams

            val synthetic = FoodProduct(
                name = bundleWithItems.bundle.name,
                caloriesPer100g = totalCalories * per100Factor,
                proteinPer100g = totalProtein * per100Factor,
                carbsPer100g = totalCarbs * per100Factor,
                fatPer100g = totalFat * per100Factor,
                fiberPer100g = totalFiber * per100Factor,
                saturatedFatPer100g = totalSatFat * per100Factor,
                addedSugarsPer100g = totalSugar * per100Factor,
                sodiumPer100g = (totalSodiumMg * per100Factor) / 1000.0 // mg -> g
            )

            withContext(Dispatchers.Main) {
                _scannedFoodProduct.value = synthetic
            }
        }
    }

    suspend fun getBundleWithItems(bundleId: Long): FoodBundleWithItems {
        return appDatabase.foodBundleDao().getBundleWithItems(bundleId)
    }

    fun logFood(foodProduct: FoodProduct, portionSizeG: Double, mealSlot: String) {

        viewModelScope.launch {
            val calories = (foodProduct.caloriesPer100g / 100.0) * portionSizeG
            val protein = (foodProduct.proteinPer100g / 100.0) * portionSizeG
            val carbs = (foodProduct.carbsPer100g / 100.0) * portionSizeG
            val fat = (foodProduct.fatPer100g / 100.0) * portionSizeG
            val fiber = (foodProduct.fiberPer100g / 100.0) * portionSizeG
            val saturatedFat = (foodProduct.saturatedFatPer100g / 100.0) * portionSizeG
            val addedSugars = (foodProduct.addedSugarsPer100g / 100.0) * portionSizeG
            val sodium = (foodProduct.sodiumPer100g / 100.0) * portionSizeG * 1000

            val loggedFood = LoggedFood(
                date = selectedDate.value.toString(),
                foodProduct = foodProduct,
                portionSizeG = portionSizeG,
                calories = calories,
                proteinG = protein,
                carbsG = carbs,
                fatG = fat,
                fiberG = fiber,
                saturatedFatG = saturatedFat,
                addedSugarsG = addedSugars,
                sodiumMg = sodium,
                isConsumed = false,
                mealSlot = mealSlot
            )
            appDatabase.loggedFoodDao().upsert(loggedFood)
            calculateDailyTotals()
            _loggedFoods.value = appDatabase.loggedFoodDao().getFoodsForDate(date = selectedDate.value.toString())
        }
    }

    private suspend fun calculateDailyTotals() {
        val loggedFoods = appDatabase.loggedFoodDao().getFoodsForDate(date = selectedDate.value.toString())
        val foodsForTotals = if (_foodConsumedTrackingEnabled.value) {
            loggedFoods.filter { it.isConsumed }
        } else {
            loggedFoods
        }
        val calories = foodsForTotals.sumOf { it.calories }
        val protein = foodsForTotals.sumOf { it.proteinG }
        val carbs = foodsForTotals.sumOf { it.carbsG }
        val fat = foodsForTotals.sumOf { it.fatG }
        val fiber = foodsForTotals.sumOf { it.fiberG }
        val saturatedFat = foodsForTotals.sumOf { it.saturatedFatG }
        val addedSugars = foodsForTotals.sumOf { it.addedSugarsG }
        val sodium = foodsForTotals.sumOf { it.sodiumMg }

        _dailyTotals.value = DailyTotals(
            calories = calories,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            fiberG = fiber,
            saturatedFatG = saturatedFat,
            addedSugarsG = addedSugars,
            sodiumMg = sodium
        )
    }
//    fun openSearchForBundle(bundleId: Long) {
//        _bundleTargetId.value = bundleId
//        showSearchDialog()
//    }
//
//    fun openCustomForBundle(bundleId: Long) {
//        _bundleTargetId.value = bundleId
//        onFoodSelected(
//            FoodProduct(
//                name = "",
//                caloriesPer100g = 0.0,
//                proteinPer100g = 0.0,
//                carbsPer100g = 0.0,
//                fatPer100g = 0.0,
//                fiberPer100g = 0.0,
//                saturatedFatPer100g = 0.0,
//                addedSugarsPer100g = 0.0,
//                sodiumPer100g = 0.0
//            )
//        )
//    }



    fun addFood(
        portion: Double,
        foodProduct: FoodProduct,
        time: java.time.LocalTime,
        mealSlot: String,
        isOneTime: Boolean,
        repeatDays: Set<java.time.DayOfWeek>,
        repeatEveryWeeks: Int,
        startDate: java.time.LocalDate,
        colorArgb: Int
    ) {
        viewModelScope.launch {
            val dao = appDatabase.loggedFoodDao()

            val caloriesPerGram = foodProduct.caloriesPer100g / 100.0
            val proteinPerGram = foodProduct.proteinPer100g / 100.0
            val carbsPerGram = foodProduct.carbsPer100g / 100.0
            val fatPerGram = foodProduct.fatPer100g / 100.0
            val fiberPerGram = foodProduct.fiberPer100g / 100.0
            val saturatedFatPerGram = foodProduct.saturatedFatPer100g / 100.0
            val addedSugarsPerGram = foodProduct.addedSugarsPer100g / 100.0
            val sodiumPerGram = foodProduct.sodiumPer100g / 100.0

            if (isOneTime || repeatDays.isEmpty()) {
                val loggedFood = LoggedFood(
                    date = startDate.toString(),
                    foodProduct = foodProduct,
                    portionSizeG = portion,
                    calories = caloriesPerGram * portion,
                    proteinG = proteinPerGram * portion,
                    carbsG = carbsPerGram * portion,
                    fatG = fatPerGram * portion,
                    fiberG = fiberPerGram * portion,
                    saturatedFatG = saturatedFatPerGram * portion,
                    addedSugarsG = addedSugarsPerGram * portion,
                    sodiumMg = sodiumPerGram * portion,
                    isConsumed = false,
                    mealSlot = mealSlot,
                    isOneTime = true,
                    dateEpochDay = startDate.toEpochDay(),
                    colorArgb = colorArgb
                )

                dao.upsert(loggedFood)
            } else {
                val maxWeeksToGenerate = 12 // limit to avoid infinite insertion

                for (weekOffset in 0 until maxWeeksToGenerate step repeatEveryWeeks) {
                    val weekStart = startDate.plusWeeks(weekOffset.toLong())

                    for (day in repeatDays) {
                        // calculate next valid occurrence of this weekday
                        val dayOffset = (day.value - weekStart.dayOfWeek.value).let {
                            if (it < 0) it + 7 else it
                        }
                        val targetDate = weekStart.plusDays(dayOffset.toLong())

                        if (!targetDate.isBefore(startDate)) {
                            val recurringFood = LoggedFood(
                                date = targetDate.toString(),
                                dateEpochDay = targetDate.toEpochDay(),
                                foodProduct = foodProduct,
                                portionSizeG = portion,
                                calories = caloriesPerGram * portion,
                                proteinG = proteinPerGram * portion,
                                carbsG = carbsPerGram * portion,
                                fatG = fatPerGram * portion,
                                fiberG = fiberPerGram * portion,
                                saturatedFatG = saturatedFatPerGram * portion,
                                addedSugarsG = addedSugarsPerGram * portion,
                                sodiumMg = sodiumPerGram * portion,
                                isConsumed = false,
                                mealSlot = mealSlot,
                                isOneTime = false,
                                startEpochDay = startDate.toEpochDay(),
                                repeatOnDays = repeatDays,
                                repeatEveryWeeks = repeatEveryWeeks,
                                colorArgb = colorArgb
                            )


                            dao.upsert(recurringFood)
                        }
                    }
                }
            }
            refreshForSelectedDate()

        }
    }
    private suspend fun calculateDailyTargets() {
        val userSettings = _userSettings.value
        if (userSettings == null || userSettings.weightKg <= 0 || userSettings.heightCm <= 0 || userSettings.age <= 0) {
            _dailyTargets.value = DailyTargets(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) // Default/empty state
            return
        }


        val baseCalories = if (userSettings.customCaloriesTarget > 0) {
            userSettings.customCaloriesTarget
        } else {
            val genderConstant = if (userSettings.gender == Gender.MALE) 5 else -161
            val bmr = (10 * userSettings.weightKg) + (6.25 * userSettings.heightCm) -
                (5 * userSettings.age) + genderConstant
            val activityFactor = when (userSettings.activityLevel) {
                ActivityLevel.SEDENTARY -> 1.2
                ActivityLevel.LIGHT -> 1.375
                ActivityLevel.MODERATE -> 1.55
                ActivityLevel.ACTIVE -> 1.725
            }
            val tdee = bmr * activityFactor
            val goalAdjustment = when (userSettings.calorieGoal) {
                CalorieGoal.MAINTAIN -> 0.0
                CalorieGoal.LOSE_WEIGHT -> -500.0
                CalorieGoal.GAIN_WEIGHT -> 300.0
            }
            (tdee + goalAdjustment).coerceAtLeast(0.0)
        }
        val workoutCalories = appDatabase
            .calorieEntryDao()
            .totalCaloriesForDate(selectedDate.value.toEpochDay())
        val calories = (baseCalories + workoutCalories).coerceAtLeast(0.0)
        val protein = 1.6 * userSettings.weightKg
        val carbs = (calories * 0.5) / 4
        val fat = (calories * 0.25) / 9
        val fiber = 14.0 * (calories / 1000)
        val saturatedFat = (calories * 0.1) / 9
        val addedSugars = (calories * 0.1) / 4
        val sodium = 2000.0

        _dailyTargets.value = DailyTargets(
            calories = calories,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            fiberG = fiber,
            saturatedFatG = saturatedFat,
            addedSugarsG = addedSugars,
            sodiumMg = sodium
        )
    }

    fun selectPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun selectNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun setFoodConsumed(loggedFood: LoggedFood, isConsumed: Boolean) {
        viewModelScope.launch {
            appDatabase.loggedFoodDao().upsert(loggedFood.copy(isConsumed = isConsumed))
            refreshForSelectedDate()
        }
    }

    data class DailyTotals(
        val calories: Double,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
        val fiberG: Double,
        val saturatedFatG: Double,
        val addedSugarsG: Double,
        val sodiumMg: Double
    )

    data class DailyTargets(
        val calories: Double,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
        val fiberG: Double,
        val saturatedFatG: Double,
        val addedSugarsG: Double,
        val sodiumMg: Double
    )
}
