package com.example.routinereminder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.ui.components.PortionDialog

@Composable
fun FoodBundleCreateScreen(
    onPickFood: () -> Unit,
    selectedFood: FoodProduct?,
    onClearSelectedFood: () -> Unit,
    vm: FoodBundleViewModel = hiltViewModel()
) {
    var bundleName by remember { mutableStateOf("") }
    var bundleDescription by remember { mutableStateOf("") }

    val previewItems = remember { mutableStateListOf<PreviewBundleItem>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Create Recipe", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = bundleName,
            onValueChange = { bundleName = it },
            label = { Text("Recipe name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = bundleDescription,
            onValueChange = { bundleDescription = it },
            label = { Text("Recipe description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalButton(onClick = onPickFood) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add food")
            }

            Button(
                enabled = bundleName.isNotBlank() && previewItems.isNotEmpty(),
                onClick = {
                    vm.saveBundle(
                        name = bundleName.trim(),
                        description = bundleDescription.trim()
                    )
                    bundleName = ""
                    bundleDescription = ""
                    previewItems.clear()
                }
            ) {
                Text("Save recipe")
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Ingredients", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(previewItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("${item.portionG} g")
                    }
                }
            }
        }
    }

    // ---------- Portion dialog in BUNDLE MODE ----------
    if (selectedFood != null) {
        PortionDialog(
            foodProduct = selectedFood,
            onDismiss = onClearSelectedFood,
            onConfirm = { _, _, _, _, _, _, _, _ -> },
            onAddToBundle = { food, portion ->
                vm.addFoodToBundle(food, portion)
                previewItems.add(
                    PreviewBundleItem(
                        name = food.name,
                        portionG = portion
                    )
                )
            },
            currentTotals = CalorieTrackerViewModel.DailyTotals(
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
            ),
            targets = CalorieTrackerViewModel.DailyTargets(
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
            )
        )
    }
}

data class PreviewBundleItem(
    val name: String,
    val portionG: Double
)
