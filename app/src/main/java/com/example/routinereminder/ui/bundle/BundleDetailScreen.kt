package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.routinereminder.data.entities.FoodBundleWithItems
import com.example.routinereminder.ui.CalorieTrackerViewModel
import androidx.compose.ui.Alignment
import com.example.routinereminder.ui.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.routinereminder.data.entities.RecipeIngredient
import com.example.routinereminder.data.entities.PORTION_TYPE_CUSTOM
import com.example.routinereminder.data.entities.PORTION_TYPE_GRAMS
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
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
    var portionType by remember(data.bundle.id) {
        mutableStateOf(data.bundle.portionType)
    }
    var customPortionGrams by remember(data.bundle.id) {
        mutableStateOf(data.bundle.customPortionGrams?.toString().orEmpty())
    }
    val scope = rememberCoroutineScope()
    var showUpdateDialog by remember { mutableStateOf(false) }

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
    val isPortionValid = portionType == PORTION_TYPE_GRAMS ||
        (customPortionGrams.toDoubleOrNull()?.let { it > 0 } == true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data.bundle.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (showUpdateDialog) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    title = { Text("Update tracked recipes?") },
                    text = {
                        Text(
                            "Apply these recipe changes to all logged entries, " +
                                "or only those that are not in the past?"
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val customPortionValue = customPortionGrams.toDoubleOrNull()
                                viewModel.updateBundle(
                                    bundleId = data.bundle.id,
                                    name = name,
                                    description = description,
                                    portionType = portionType,
                                    customPortionGrams = customPortionValue,
                                    updateScope = BundleUpdateScope.ALL
                                )
                                isEditing = false
                                showUpdateDialog = false
                            }
                        ) {
                            Text("Update all")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                val customPortionValue = customPortionGrams.toDoubleOrNull()
                                viewModel.updateBundle(
                                    bundleId = data.bundle.id,
                                    name = name,
                                    description = description,
                                    portionType = portionType,
                                    customPortionGrams = customPortionValue,
                                    updateScope = BundleUpdateScope.FUTURE
                                )
                                isEditing = false
                                showUpdateDialog = false
                            }
                        ) {
                            Text("Update future only")
                        }
                    }
                )
            }
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
                            if (isPortionValid) {
                                showUpdateDialog = true
                            }
                        } else {
                            isEditing = true
                        }
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

                if (!isEditing) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (data.bundle.portionType == PORTION_TYPE_CUSTOM) {
                            val customAmount = data.bundle.customPortionGrams ?: 0.0
                            "Portion: 1 portion = ${customAmount.toInt()} g"
                        } else {
                            "Portion: grams"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            if (isEditing) {
                Spacer(Modifier.height(12.dp))
                Text("Portion definition", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = portionType == PORTION_TYPE_GRAMS,
                            onClick = { portionType = PORTION_TYPE_GRAMS }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Grams")
                    }
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = portionType == PORTION_TYPE_CUSTOM,
                            onClick = { portionType = PORTION_TYPE_CUSTOM }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Custom portion")
                    }
                }

                if (portionType == PORTION_TYPE_CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customPortionGrams,
                        onValueChange = { customPortionGrams = it },
                        label = { Text("Grams per portion") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isPortionValid) {
                        Text(
                            text = "Enter a gram value greater than zero.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                    calorieVm.selectBundle(bundleId)
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
                    enabled = isPortionValid,
                    onClick = {
                        showUpdateDialog = true
                    }
                ) {
                    Text("Save changes")
                }
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
