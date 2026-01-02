package com.example.routinereminder.ui.bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.ui.CalorieTrackerViewModel
import com.example.routinereminder.ui.Screen

private const val TAG = "BundleIngredientPicker"

@Composable
fun BundleIngredientPickerScreen(
    navController: NavController,
    bundleId: Long
) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry(Screen.CalorieTracker.route)
    }

    val calorieVm: CalorieTrackerViewModel = hiltViewModel(parentEntry)

    Log.d(
        TAG,
        "Picker VM#${System.identityHashCode(calorieVm)} startAddingToBundle($bundleId)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Button(
            onClick = {
                calorieVm.startAddingToBundle(bundleId)
                navController.navigate(Screen.BarcodeScanner.route)
            },
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) { Text("Scan") }

        Button(
            onClick = {
                calorieVm.startAddingToBundle(bundleId)
                navController.navigate(Screen.CalorieTracker.route)
            }
        ) {
            Text("Search")
        }


        Button(
            onClick = {
                calorieVm.startAddingToBundle(bundleId)
                calorieVm.onFoodSelected(
                    FoodProduct(
                        name = "",
                        caloriesPer100g = 0.0,
                        proteinPer100g = 0.0,
                        carbsPer100g = 0.0,
                        fatPer100g = 0.0,
                        fiberPer100g = 0.0,
                        saturatedFatPer100g = 0.0,
                        addedSugarsPer100g = 0.0,
                        sodiumPer100g = 0.0
                    )
                )
                navController.navigate(Screen.CalorieTracker.route)
            }
        ) {
            Text("Custom")
        }

    }
}


