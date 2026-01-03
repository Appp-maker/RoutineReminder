package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.ui.Screen
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RecipeIngredientEditorScreen(
    navController: NavController,
    bundleId: Long,
    ingredientId: Long?,
    viewModel: BundleViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("0") }
    var protein by remember { mutableStateOf("0") }
    var carbs by remember { mutableStateOf("0") }
    var fat by remember { mutableStateOf("0") }
    var fiber by remember { mutableStateOf("0") }
    var saturatedFat by remember { mutableStateOf("0") }
    var addedSugars by remember { mutableStateOf("0") }
    var sodium by remember { mutableStateOf("0") }
    var entryMode by remember { mutableStateOf(IngredientEntryMode.CUSTOM) }
    var hasNonCustomSelection by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val searchResults by viewModel.searchResults.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val barcodeError by viewModel.barcodeError.collectAsState()
    val scannedFoodProduct by viewModel.scannedFoodProduct.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    fun applyFoodProduct(food: FoodProduct) {
        val gramsValue = food.servingSizeG?.roundToInt()?.takeIf { it > 0 } ?: 100
        name = food.name
        grams = gramsValue.toString()
        calories = food.caloriesPer100g.toDisplayString()
        protein = food.proteinPer100g.toDisplayString()
        carbs = food.carbsPer100g.toDisplayString()
        fat = food.fatPer100g.toDisplayString()
        fiber = food.fiberPer100g.toDisplayString()
        saturatedFat = food.saturatedFatPer100g.toDisplayString()
        addedSugars = food.addedSugarsPer100g.toDisplayString()
        sodium = (food.sodiumPer100g * 1000).toDisplayString()
        hasNonCustomSelection = entryMode != IngredientEntryMode.CUSTOM
    }

    LaunchedEffect(ingredientId) {
        ingredientId?.let { id ->
            val ingredient = viewModel.getIngredientOnce(id) ?: return@let
            name = ingredient.name
            grams = ingredient.portionSizeG.toString()
            val per100Factor = if (ingredient.portionSizeG > 0) {
                100.0 / ingredient.portionSizeG.toDouble()
            } else {
                1.0
            }
            calories = (ingredient.calories * per100Factor).toDisplayString()
            protein = (ingredient.proteinG * per100Factor).toDisplayString()
            carbs = (ingredient.carbsG * per100Factor).toDisplayString()
            fat = (ingredient.fatG * per100Factor).toDisplayString()
            fiber = (ingredient.fiberG * per100Factor).toDisplayString()
            saturatedFat = (ingredient.saturatedFatG * per100Factor).toDisplayString()
            addedSugars = (ingredient.addedSugarsG * per100Factor).toDisplayString()
            sodium = (ingredient.sodiumMg * per100Factor).toDisplayString()
        }
    }

    LaunchedEffect(scannedFoodProduct) {
        scannedFoodProduct?.let {
            applyFoodProduct(it)
            viewModel.clearScannedProduct()
        }
    }

    DisposableEffect(navController, lifecycleOwner) {
        val observer = Observer<String> { barcode ->
            barcode?.let {
                viewModel.onBarcodeScanned(it)
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("barcode")
            }
        }
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getLiveData<String>("barcode")?.observe(lifecycleOwner, observer)
        onDispose { handle?.getLiveData<String>("barcode")?.removeObserver(observer) }
    }

    val numericKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
    val decimalKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (ingredientId == null) "Add Ingredient" else "Edit Ingredient",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Nutrition values should be entered per 100 g of the ingredient.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IngredientModeButton(
                text = "Custom",
                selected = entryMode == IngredientEntryMode.CUSTOM,
                onClick = {
                    entryMode = IngredientEntryMode.CUSTOM
                    hasNonCustomSelection = false
                },
                modifier = Modifier.weight(1f)
            )
            IngredientModeButton(
                text = "Scan barcode",
                selected = entryMode == IngredientEntryMode.BARCODE,
                onClick = {
                    entryMode = IngredientEntryMode.BARCODE
                    hasNonCustomSelection = false
                },
                modifier = Modifier.weight(1f)
            )
            IngredientModeButton(
                text = "Search",
                selected = entryMode == IngredientEntryMode.SEARCH,
                onClick = {
                    entryMode = IngredientEntryMode.SEARCH
                    hasNonCustomSelection = false
                },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Source: Open Food Facts (ODbL v1.0)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (entryMode) {
            IngredientEntryMode.CUSTOM -> {
                Text(
                    text = "Enter ingredient details manually.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IngredientEntryMode.BARCODE -> {
                Text(
                    text = "Scan a barcode to pull nutrition data.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { navController.navigate(Screen.BarcodeScanner.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open barcode scanner")
                }
                barcodeError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            IngredientEntryMode.SEARCH -> {
                Text(
                    text = "Search for an ingredient and select the best match.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Open Food Facts") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.searchFood(searchQuery) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Search")
                }
                searchError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (searchResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchResults.forEach { product ->
                            FoodSearchResultCard(
                                product = product,
                                onSelect = {
                                    applyFoodProduct(product)
                                    hasNonCustomSelection = true
                                    searchQuery = product.name
                                    viewModel.clearSearchResults()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (entryMode == IngredientEntryMode.CUSTOM || hasNonCustomSelection) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ingredient name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = grams,
                onValueChange = { grams = it },
                label = { Text("Amount (g)") },
                keyboardOptions = numericKeyboard,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calories (kcal per 100 g)") },
                keyboardOptions = decimalKeyboard,
                enabled = entryMode == IngredientEntryMode.CUSTOM,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = protein,
                onValueChange = { protein = it },
                label = { Text("Protein (g per 100 g)") },
                keyboardOptions = decimalKeyboard,
                enabled = entryMode == IngredientEntryMode.CUSTOM,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = carbs,
                onValueChange = { carbs = it },
                label = { Text("Carbs (g per 100 g)") },
                keyboardOptions = decimalKeyboard,
                enabled = entryMode == IngredientEntryMode.CUSTOM,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fat,
                onValueChange = { fat = it },
                label = { Text("Fat (g per 100 g)") },
                keyboardOptions = decimalKeyboard,
                enabled = entryMode == IngredientEntryMode.CUSTOM,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fiber,
                onValueChange = { fiber = it },
                label = { Text("Fiber (g per 100 g)") },
                keyboardOptions = decimalKeyboard,
                enabled = entryMode == IngredientEntryMode.CUSTOM,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = saturatedFat,
                onValueChange = { saturatedFat = it },
                label = { Text("Saturated fat (g per 100 g)") },
                keyboardOptions = decimalKeyboard,
                enabled = entryMode == IngredientEntryMode.CUSTOM,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = addedSugars,
                onValueChange = { addedSugars = it },
                label = { Text("Added sugars (g per 100 g)") },
                keyboardOptions = decimalKeyboard,
                enabled = entryMode == IngredientEntryMode.CUSTOM,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = sodium,
                onValueChange = { sodium = it },
                label = { Text("Sodium (mg per 100 g)") },
                keyboardOptions = decimalKeyboard,
                enabled = entryMode == IngredientEntryMode.CUSTOM,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        val gramsValue = grams.toIntOrNull()
        val caloriesValue = calories.toDoubleOrNull()
        val proteinValue = protein.toDoubleOrNull()
        val carbsValue = carbs.toDoubleOrNull()
        val fatValue = fat.toDoubleOrNull()
        val fiberValue = fiber.toDoubleOrNull()
        val saturatedFatValue = saturatedFat.toDoubleOrNull()
        val addedSugarsValue = addedSugars.toDoubleOrNull()
        val sodiumValue = sodium.toDoubleOrNull()

        val canSave = name.isNotBlank()
            && gramsValue != null
            && gramsValue > 0
            && caloriesValue != null
            && proteinValue != null
            && carbsValue != null
            && fatValue != null
            && fiberValue != null
            && saturatedFatValue != null
            && addedSugarsValue != null
            && sodiumValue != null

        Button(
            enabled = canSave,
            onClick = {
                val gramsDouble = gramsValue?.toDouble() ?: 0.0
                val portionFactor = if (gramsDouble > 0.0) gramsDouble / 100.0 else 0.0
                viewModel.upsertIngredient(
                    ingredientId = ingredientId,
                    bundleId = bundleId,
                    name = name,
                    portionSizeG = gramsValue ?: 0,
                    calories = (caloriesValue ?: 0.0) * portionFactor,
                    proteinG = (proteinValue ?: 0.0) * portionFactor,
                    carbsG = (carbsValue ?: 0.0) * portionFactor,
                    fatG = (fatValue ?: 0.0) * portionFactor,
                    fiberG = (fiberValue ?: 0.0) * portionFactor,
                    saturatedFatG = (saturatedFatValue ?: 0.0) * portionFactor,
                    addedSugarsG = (addedSugarsValue ?: 0.0) * portionFactor,
                    sodiumMg = (sodiumValue ?: 0.0) * portionFactor
                )
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (ingredientId == null) "Save ingredient" else "Update ingredient")
        }
    }
}

private enum class IngredientEntryMode {
    CUSTOM,
    BARCODE,
    SEARCH
}

@Composable
private fun IngredientModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = !selected
    ) {
        Text(text)
    }
}

@Composable
private fun FoodSearchResultCard(
    product: FoodProduct,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(text = product.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Per 100 g: ${product.caloriesPer100g.toDisplayString()} kcal, " +
                "P ${product.proteinPer100g.toDisplayString()} g, " +
                "C ${product.carbsPer100g.toDisplayString()} g, " +
                "F ${product.fatPer100g.toDisplayString()} g",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Fiber ${product.fiberPer100g.toDisplayString()} g, " +
                "Sat fat ${product.saturatedFatPer100g.toDisplayString()} g, " +
                "Sugars ${product.addedSugarsPer100g.toDisplayString()} g, " +
                "Sodium ${(product.sodiumPer100g * 1000).toDisplayString()} mg",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use this ingredient")
        }
    }
}

private fun Double.toDisplayString(): String {
    val formatted = String.format(Locale.US, "%.2f", this)
    return if (formatted.endsWith(".00")) formatted.dropLast(3) else formatted
}
