package com.example.routinereminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.entities.FoodBundle
import com.example.routinereminder.data.entities.FoodBundleItem
import com.example.routinereminder.data.entities.FoodProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodBundleViewModel @Inject constructor(
    application: Application,
    private val appDatabase: AppDatabase
) : AndroidViewModel(application) {

    private val selectedFoods = mutableListOf<FoodBundleItem>()

    fun addFoodToBundle(food: FoodProduct, portionG: Double) {
        val portionInt = portionG.toInt()

        selectedFoods.add(
            FoodBundleItem(
                bundleId = 0, // temporary, set on save
                foodName = food.name,
                portionSizeG = portionInt,
                calories = (food.caloriesPer100g / 100.0) * portionInt,
                proteinG = (food.proteinPer100g / 100.0) * portionInt,
                carbsG = (food.carbsPer100g / 100.0) * portionInt,
                fatG = (food.fatPer100g / 100.0) * portionInt,
                fiberG = (food.fiberPer100g / 100.0) * portionInt,
                saturatedFatG = (food.saturatedFatPer100g / 100.0) * portionInt,
                addedSugarsG = (food.addedSugarsPer100g / 100.0) * portionInt,
                sodiumMg = (food.sodiumPer100g / 100.0) * portionInt * 1000
            )
        )
    }


    fun saveBundle(
        name: String,
        description: String
    ) {
        viewModelScope.launch {
            val bundleId = appDatabase.foodBundleDao()
                .insertBundle(
                    FoodBundle(
                        name = name,
                        description = description
                    )
                )

            val itemsWithBundleId = selectedFoods.map {
                it.copy(bundleId = bundleId)
            }

            appDatabase.foodBundleDao().insertItems(itemsWithBundleId)
            selectedFoods.clear()
        }
    }
}
