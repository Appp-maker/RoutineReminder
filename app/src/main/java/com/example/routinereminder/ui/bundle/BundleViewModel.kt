package com.example.routinereminder.ui.bundle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.OpenFoodFactsApiClient
import com.example.routinereminder.data.entities.FoodBundle
import com.example.routinereminder.data.entities.FoodProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.routinereminder.data.entities.FoodBundleWithItems
import com.example.routinereminder.data.entities.RecipeIngredient
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@HiltViewModel
class BundleViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val openFoodFactsApiClient = OpenFoodFactsApiClient()

    private val _bundles = MutableStateFlow<List<FoodBundle>>(emptyList())
    val bundles: StateFlow<List<FoodBundle>> = _bundles

    private val _searchResults = MutableStateFlow<List<FoodProduct>>(emptyList())
    val searchResults: StateFlow<List<FoodProduct>> = _searchResults.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _barcodeError = MutableStateFlow<String?>(null)
    val barcodeError: StateFlow<String?> = _barcodeError.asStateFlow()

    private val _scannedFoodProduct = MutableStateFlow<FoodProduct?>(null)
    val scannedFoodProduct: StateFlow<FoodProduct?> = _scannedFoodProduct.asStateFlow()

    init {
        loadBundles()
    }
    fun loadBundles() {
        viewModelScope.launch {
            _bundles.value = database.foodBundleDao().getAllBundles()
        }
    }

    suspend fun getBundleOnce(bundleId: Long): FoodBundleWithItems {
        return database.foodBundleDao().getBundleWithItems(bundleId)
    }

    suspend fun getIngredientOnce(ingredientId: Long): RecipeIngredient? {
        return database.foodBundleDao().getIngredientById(ingredientId)
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _barcodeError.value = null
            try {
                val result = openFoodFactsApiClient.getFoodProduct(barcode)
                if (result == null) {
                    _barcodeError.value = "No nutrition data found for this barcode."
                }
                _scannedFoodProduct.value = result
            } catch (e: SocketTimeoutException) {
                _barcodeError.value = "Network timeout. Try again."
            } catch (e: IOException) {
                _barcodeError.value = "Network error. Check your connection."
            } catch (e: Exception) {
                _barcodeError.value = "Barcode lookup failed."
            }
        }
    }

    fun clearScannedProduct() {
        _scannedFoodProduct.value = null
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
            _searchResults.value = emptyList()

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

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _searchError.value = null
    }

    fun createBundle(
        name: String,
        description: String,
        portionType: String,
        customPortionGrams: Double?
    ) {
        viewModelScope.launch {
            database.foodBundleDao().insertBundle(
                FoodBundle(
                    name = name.trim(),
                    description = description.trim(),
                    portionType = portionType,
                    customPortionGrams = customPortionGrams
                )
            )
            loadBundles() // refresh list so UI updates immediately
        }
    }

    fun createBundleAndReturnId(
        name: String,
        description: String,
        portionType: String,
        customPortionGrams: Double?,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val bundleId = database.foodBundleDao().insertBundle(
                FoodBundle(
                    name = name.trim(),
                    description = description.trim(),
                    portionType = portionType,
                    customPortionGrams = customPortionGrams
                )
            )
            loadBundles()
            onCreated(bundleId)
        }
    }

    fun deleteBundle(bundle: FoodBundle) {
        viewModelScope.launch {
            database.foodBundleDao().deleteBundle(bundle)
        }
    }
    fun upsertIngredient(
        ingredientId: Long?,
        bundleId: Long,
        name: String,
        portionSizeG: Int,
        calories: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double,
        saturatedFatG: Double,
        addedSugarsG: Double,
        sodiumMg: Double
    ) {
        viewModelScope.launch {
            val ingredient = RecipeIngredient(
                id = ingredientId ?: 0,
                bundleId = bundleId,
                name = name.trim(),
                portionSizeG = portionSizeG,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                fiberG = fiberG,
                saturatedFatG = saturatedFatG,
                addedSugarsG = addedSugarsG,
                sodiumMg = sodiumMg
            )

            database.foodBundleDao().insertIngredients(listOf(ingredient))
        }
    }

    fun updateBundle(
        bundleId: Long,
        name: String,
        description: String,
        portionType: String,
        customPortionGrams: Double?,
        updateScope: BundleUpdateScope
    ) {
        viewModelScope.launch {
            database.foodBundleDao().updateBundle(
                id = bundleId,
                name = name,
                description = description,
                portionType = portionType,
                customPortionGrams = customPortionGrams
            )
            updateLoggedFoodsForBundle(
                bundleId = bundleId,
                bundleName = name.trim(),
                updateScope = updateScope
            )
            loadBundles()
        }
    }
    fun deleteIngredient(ingredientId: Long) {
        viewModelScope.launch {
            database.foodBundleDao().deleteIngredientById(ingredientId)
        }
    }

    private suspend fun buildBundleFoodProduct(bundleId: Long, bundleName: String): FoodProduct {
        val bundleWithItems = database.foodBundleDao().getBundleWithItems(bundleId)
        val totalGrams = bundleWithItems.items.sumOf { it.portionSizeG }.toDouble()

        if (totalGrams <= 0.0) {
            return FoodProduct(
                name = bundleName,
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

        val per100Factor = 100.0 / totalGrams
        return FoodProduct(
            name = bundleName,
            caloriesPer100g = bundleWithItems.items.sumOf { it.calories } * per100Factor,
            proteinPer100g = bundleWithItems.items.sumOf { it.proteinG } * per100Factor,
            carbsPer100g = bundleWithItems.items.sumOf { it.carbsG } * per100Factor,
            fatPer100g = bundleWithItems.items.sumOf { it.fatG } * per100Factor,
            fiberPer100g = bundleWithItems.items.sumOf { it.fiberG } * per100Factor,
            saturatedFatPer100g = bundleWithItems.items.sumOf { it.saturatedFatG } * per100Factor,
            addedSugarsPer100g = bundleWithItems.items.sumOf { it.addedSugarsG } * per100Factor,
            sodiumPer100g = (bundleWithItems.items.sumOf { it.sodiumMg } * per100Factor) / 1000.0
        )
    }

    private suspend fun updateLoggedFoodsForBundle(
        bundleId: Long,
        bundleName: String,
        updateScope: BundleUpdateScope
    ) {
        val loggedFoods = database.loggedFoodDao().getFoodsForBundle(bundleId)
        if (loggedFoods.isEmpty()) return

        val foodProduct = buildBundleFoodProduct(bundleId, bundleName)
        val today = LocalDate.now()

        loggedFoods.forEach { entry ->
            val shouldUpdate = when (updateScope) {
                BundleUpdateScope.ALL -> true
                BundleUpdateScope.FUTURE -> {
                    val entryDate = runCatching { LocalDate.parse(entry.date) }.getOrNull()
                    entryDate?.isAfter(today.minusDays(1)) ?: true
                }
            }

            if (shouldUpdate) {
                val portion = entry.portionSizeG
                val calories = (foodProduct.caloriesPer100g / 100.0) * portion
                val protein = (foodProduct.proteinPer100g / 100.0) * portion
                val carbs = (foodProduct.carbsPer100g / 100.0) * portion
                val fat = (foodProduct.fatPer100g / 100.0) * portion
                val fiber = (foodProduct.fiberPer100g / 100.0) * portion
                val saturatedFat = (foodProduct.saturatedFatPer100g / 100.0) * portion
                val addedSugars = (foodProduct.addedSugarsPer100g / 100.0) * portion
                val sodium = (foodProduct.sodiumPer100g * 1000.0 / 100.0) * portion

                database.loggedFoodDao().upsert(
                    entry.copy(
                        foodProduct = foodProduct,
                        calories = calories,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat,
                        fiberG = fiber,
                        saturatedFatG = saturatedFat,
                        addedSugarsG = addedSugars,
                        sodiumMg = sodium,
                        bundleName = bundleName
                    )
                )
            }
        }
    }
}

enum class BundleUpdateScope {
    ALL,
    FUTURE
}
