package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.routinereminder.data.entities.FoodBundleWithItems
import com.example.routinereminder.ui.CalorieTrackerViewModel
import androidx.compose.ui.Alignment
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.ui.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import com.example.routinereminder.data.entities.RecipeIngredient
import kotlinx.coroutines.launch


@Composable
fun BundleDetailScreen(
    navController: NavController,
    bundleId: Long,
    viewModel: BundleViewModel = hiltViewModel()
) {
    var bundleWithItems by remember { mutableStateOf<FoodBundleWithItems?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(navController, bundleId) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val activeBundleId = entry.arguments?.getString("bundleId")?.toLongOrNull()
                ?: entry.arguments?.getString("id")?.toLongOrNull()
            if (activeBundleId == bundleId) {
                bundleWithItems = viewModel.getBundleOnce(bundleId)
            }
        }
    }

    val data = bundleWithItems ?: return
    var name by remember(data.bundle.id) {
        mutableStateOf(data.bundle.name)
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var description by remember(data.bundle.id) {
        mutableStateOf(data.bundle.description)
    }
    val scope = rememberCoroutineScope()

    val calorieEntry = remember(navController) {
        navController.getBackStackEntry(Screen.CalorieTracker.route)
    }
    val calorieVm: CalorieTrackerViewModel = hiltViewModel(calorieEntry)

    // ---- totals ----
    val totalGrams = data.items.sumOf { it.portionSizeG.toDouble() }.coerceAtLeast(1.0)
    val totalCalories = data.items.sumOf { it.calories }
    val totalProtein = data.items.sumOf { it.proteinG }
    val totalCarbs = data.items.sumOf { it.carbsG }
    val totalFat = data.items.sumOf { it.fatG }
    val totalFiber = data.items.sumOf { it.fiberG }
    val totalSaturatedFat = data.items.sumOf { it.saturatedFatG }
    val totalAddedSugars = data.items.sumOf { it.addedSugarsG }
    val totalSodium = data.items.sumOf { it.sodiumMg }

    val combinedProduct = FoodProduct(
        name = data.bundle.name,
        caloriesPer100g = totalCalories / totalGrams * 100.0,
        proteinPer100g = totalProtein / totalGrams * 100.0,
        carbsPer100g = totalCarbs / totalGrams * 100.0,
        fatPer100g = totalFat / totalGrams * 100.0,
        fiberPer100g = totalFiber / totalGrams * 100.0,
        saturatedFatPer100g = totalSaturatedFat / totalGrams * 100.0,
        addedSugarsPer100g = totalAddedSugars / totalGrams * 100.0,
        sodiumPer100g = (totalSodium / totalGrams * 100.0) / 1000.0
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete recipe") },
                text = {
                    Text(
                        "Are you sure you want to delete this recipe? " +
                                "This will remove all its ingredients and cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBundle(data.bundle)
                            showDeleteConfirm = false
                            navController.popBackStack()
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Recipe name") },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = data.bundle.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                IconButton(onClick = {
                    if (isEditing) {
                        // SAVE when leaving edit mode
                        viewModel.updateBundle(
                            bundleId = data.bundle.id,
                            name = name,
                            description = description
                        )
                    }
                    isEditing = !isEditing
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = "Edit recipe"
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Recipe description") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (data.bundle.description.isNotBlank()) {
                Text(
                    text = data.bundle.description,
                    color = Color.Gray
                )
            }
        }



//        IconButton(onClick = { isEditing = !isEditing }) {
//                Icon(
//                    imageVector = if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
//                    contentDescription = "Edit bundle"
//                )
//            }
//        }


        Spacer(Modifier.height(8.dp))

        Text(
            text = "Ingredients",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        if (data.items.isEmpty()) {
            Text(
                text = "No ingredients yet.",
                color = Color.Gray
            )
        } else {
            data.items.forEach { item ->
                if (isEditing) {
                    EditableIngredientRow(
                        item = item,
                        onEdit = {
                            navController.navigate(
                                "bundle/$bundleId/ingredient?ingredientId=${item.id}"
                            )
                        },
                        onDelete = {
                            viewModel.deleteIngredient(item.id)
                            scope.launch {
                                bundleWithItems = viewModel.getBundleOnce(bundleId)
                            }
                        }
                    )
                } else {
                    IngredientSummaryRow(item)
                }
            }
        }
    if (isEditing) {
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete recipe")
        }

    }

        if (isEditing) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    navController.navigate("bundle/$bundleId/ingredient")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add ingredient")
            }
        }

        Spacer(Modifier.weight(1f))

        if (!isEditing) {
            Button(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = {
                    calorieVm.onFoodSelected(combinedProduct)
                    navController.popBackStack(Screen.CalorieTracker.route, false)
                }
            ) {
                Text("Add to tracker")
            }
        }

        if (isEditing) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // Cancel → reload from DB
                        scope.launch {
                            bundleWithItems = viewModel.getBundleOnce(bundleId)
                        }
                        isEditing = false
                    }
                ) {
                    Text("Cancel")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.updateBundle(
                            bundleId = data.bundle.id,
                            name = name,
                            description = description
                        )
                        isEditing = false
                    }
                ) {
                    Text("Save changes")
                }
            }
        }


    }
}

@Composable
fun IngredientSummaryRow(item: RecipeIngredient) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(text = "• ${item.name} (${item.portionSizeG} g)")
        Text(
            text = "Calories: ${item.calories.toInt()} kcal • " +
                "Protein: ${item.proteinG} g • " +
                "Carbs: ${item.carbsG} g • " +
                "Fat: ${item.fatG} g",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun EditableIngredientRow(
    item: RecipeIngredient,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.name,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.OpenInNew, contentDescription = "Edit ingredient")
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}
