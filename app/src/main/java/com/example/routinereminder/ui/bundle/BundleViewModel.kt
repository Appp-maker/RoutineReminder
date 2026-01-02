package com.example.routinereminder.ui.bundle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.entities.FoodBundle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.routinereminder.data.entities.FoodBundleWithItems
import com.example.routinereminder.data.entities.RecipeIngredient

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

    suspend fun getBundleOnce(bundleId: Long): FoodBundleWithItems {
        return database.foodBundleDao().getBundleWithItems(bundleId)
    }

    suspend fun getIngredientOnce(ingredientId: Long): RecipeIngredient? {
        return database.foodBundleDao().getIngredientById(ingredientId)
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
    fun deleteIngredient(ingredientId: Long) {
        viewModelScope.launch {
            database.foodBundleDao().deleteIngredientById(ingredientId)
        }
    }
}
