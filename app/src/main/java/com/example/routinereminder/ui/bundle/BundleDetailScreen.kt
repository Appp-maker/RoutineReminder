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
import androidx.navigation.NavBackStackEntry
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.ui.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import com.example.routinereminder.data.entities.FoodBundleItem
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch


@Composable
fun BundleDetailScreen(
    navController: NavController,
    bundleId: Long,
    viewModel: BundleViewModel = hiltViewModel()
) {
    var bundleWithItems by remember { mutableStateOf<FoodBundleWithItems?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(bundleId) {
        bundleWithItems = viewModel.getBundleOnce(bundleId)
    }

    val data = bundleWithItems ?: return
    var name by remember(data.bundle.id) {
        mutableStateOf(data.bundle.name)
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var description by remember(data.bundle.id) {
        mutableStateOf(data.bundle.description)
    }
    var editedItems by remember(data.bundle.id) {
        mutableStateOf(
            data.items.associate { item ->
                item.id to item.portionSizeG.toString()
            }
        )
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
        sodiumPer100g = totalSodium / totalGrams * 100.0
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete bundle") },
                text = {
                    Text(
                        "Are you sure you want to delete this bundle? " +
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
                        label = { Text("Bundle name") },
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
                        contentDescription = "Edit bundle"
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
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

        data.items.forEach { item ->
            if (isEditing) {
                EditableBundleItemRow(
                    item = item,
                    onUpdate = { newGrams ->
                        viewModel.updateItemGrams(item.id, newGrams)
                        scope.launch {
                            bundleWithItems = viewModel.getBundleOnce(bundleId)
                        }
                    },
                    onDelete = {
                        viewModel.deleteItem(item.id)
                        scope.launch {
                            bundleWithItems = viewModel.getBundleOnce(bundleId)
                        }

                    }
                )
            } else {
                Text("• ${item.foodName} (${item.portionSizeG} g)")
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
            Text("Delete bundle")
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
                Text("Add ingredients")
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

                        editedItems.forEach { (id, grams) ->
                            grams.toIntOrNull()?.let {
                                viewModel.updateItemGrams(id, it)
                            }
                        }

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
fun EditableBundleItemRow(
    item: FoodBundleItem,
    grams: String,
    onGramsChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.foodName, modifier = Modifier.weight(1f))

        OutlinedTextField(
            value = grams,
            onValueChange = onGramsChange,
            modifier = Modifier.width(80.dp),
            singleLine = true
        )

        Text(" g")

        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun EditableBundleItemRow(
    item: FoodBundleItem,
    onUpdate: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var grams by remember { mutableStateOf(item.portionSizeG.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.foodName,
            modifier = Modifier.weight(1f)
        )

        OutlinedTextField(
            value = grams,
            onValueChange = { grams = it },
            modifier = Modifier.width(80.dp),
            singleLine = true
        )

        Text(" g")



        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}


