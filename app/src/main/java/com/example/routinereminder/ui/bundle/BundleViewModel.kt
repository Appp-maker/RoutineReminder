package com.example.routinereminder.ui.bundle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.entities.FoodBundle
import com.example.routinereminder.data.entities.FoodBundleItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.routinereminder.data.entities.FoodBundleWithItems
import com.example.routinereminder.data.entities.FoodProduct
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class BundleViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _bundles = MutableStateFlow<List<FoodBundle>>(emptyList())
    val bundles: StateFlow<List<FoodBundle>> = _bundles

    init {
        loadBundles()
    }
    fun loadBundles() {
        viewModelScope.launch {
            _bundles.value = database.foodBundleDao().getAllBundles()
        }
    }
    private val _pendingIngredient = MutableStateFlow<FoodProduct?>(null)
    val pendingIngredient = _pendingIngredient.asStateFlow()

    fun setPendingIngredient(food: FoodProduct) {
        _pendingIngredient.value = food
    }

    fun clearPendingIngredient() {
        _pendingIngredient.value = null
    }

    suspend fun getBundleOnce(bundleId: Long): FoodBundleWithItems {
        return database.foodBundleDao().getBundleWithItems(bundleId)
    }
    fun createBundle(
        name: String,
        description: String
    ) {
        viewModelScope.launch {
            database.foodBundleDao().insertBundle(
                FoodBundle(
                    name = name.trim(),
                    description = description.trim()
                )
            )
            loadBundles() // refresh list so UI updates immediately
        }
    }
    fun updateItemGrams(itemId: Long, grams: Int) {
        viewModelScope.launch {
            val dao = database.foodBundleDao()
            val item = dao.getItemById(itemId) ?: return@launch
            dao.insertItems(listOf(item.copy(portionSizeG = grams)))
        }
    }
    fun deleteBundle(bundle: FoodBundle) {
        viewModelScope.launch {
            database.foodBundleDao().deleteBundle(bundle)
        }
    }
    fun addFoodToBundle(
        bundleId: Long,
        food: FoodProduct,
        portionG: Double
    ) {
        viewModelScope.launch {
            val factor = portionG / 100.0

            database.foodBundleDao().insertItems(
                listOf(
                    FoodBundleItem(
                        bundleId = bundleId,
                        foodName = food.name,
                        portionSizeG = portionG.toInt(),
                        calories = food.caloriesPer100g * factor,
                        proteinG = food.proteinPer100g * factor,
                        carbsG = food.carbsPer100g * factor,
                        fatG = food.fatPer100g * factor,
                        fiberG = food.fiberPer100g * factor,
                        saturatedFatG = food.saturatedFatPer100g * factor,
                        addedSugarsG = food.addedSugarsPer100g * factor,
                        sodiumMg = food.sodiumPer100g * factor
                    )
                )
            )
        }
    }

    fun updateBundle(
        bundleId: Long,
        name: String,
        description: String
    ) {
        viewModelScope.launch {
            database.foodBundleDao().updateBundle(
                id = bundleId,
                name = name,
                description = description
            )
        }
    }
    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            database.foodBundleDao().deleteItemById(itemId)
        }
    }

//    fun addItemToBundle(
//        bundleId: Long,
//        foodName: String,
//        portionSizeG: Int,
//        caloriesPer100g: Double,
//        proteinPer100g: Double,
//        carbsPer100g: Double,
//        fatPer100g: Double,
//        fiberPer100g: Double,
//        saturatedFatPer100g: Double,
//        addedSugarsPer100g: Double,
//        sodiumPer100g: Double
//    ) {
//        viewModelScope.launch {
//            val factor = portionSizeG / 100.0
//
//            database.foodBundleDao().insertItems(
//                listOf(
//                    FoodBundleItem(
//                        bundleId = bundleId,
//                        foodName = foodName,
//                        portionSizeG = portionSizeG,
//                        calories = caloriesPer100g * factor,
//                        proteinG = proteinPer100g * factor,
//                        carbsG = carbsPer100g * factor,
//                        fatG = fatPer100g * factor,
//                        fiberG = fiberPer100g * factor,
//                        saturatedFatG = saturatedFatPer100g * factor,
//                        addedSugarsG = addedSugarsPer100g * factor,
//                        sodiumMg = sodiumPer100g * factor
//                    )
//                )
//            )
//        }
//    }
fun addItemToBundle(
    bundleId: Long,
    foodName: String,
    grams: Int
) {
    viewModelScope.launch {
        val item = FoodBundleItem(
            bundleId = bundleId,
            foodName = foodName,
            portionSizeG = grams,

            // temporary zero nutrition (can be filled later)
            calories = 0.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 0.0,
            fiberG = 0.0,
            saturatedFatG = 0.0,
            addedSugarsG = 0.0,
            sodiumMg = 0.0
        )

        database.foodBundleDao().insertItems(listOf(item))
    }
}


}

