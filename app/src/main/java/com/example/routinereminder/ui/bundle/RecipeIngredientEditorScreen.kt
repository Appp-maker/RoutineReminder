package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun RecipeIngredientEditorScreen(
    navController: NavController,
    bundleId: Long,
    ingredientId: Long?,
    viewModel: BundleViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var saturatedFat by remember { mutableStateOf("") }
    var addedSugars by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }

    LaunchedEffect(ingredientId) {
        ingredientId?.let { id ->
            val ingredient = viewModel.getIngredientOnce(id) ?: return@let
            name = ingredient.name
            grams = ingredient.portionSizeG.toString()
            calories = ingredient.calories.toString()
            protein = ingredient.proteinG.toString()
            carbs = ingredient.carbsG.toString()
            fat = ingredient.fatG.toString()
            fiber = ingredient.fiberG.toString()
            saturatedFat = ingredient.saturatedFatG.toString()
            addedSugars = ingredient.addedSugarsG.toString()
            sodium = ingredient.sodiumMg.toString()
        }
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
            text = "Nutrition values should match this ingredient's amount.",
            style = MaterialTheme.typography.bodySmall
        )

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
            label = { Text("Calories (kcal)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = protein,
            onValueChange = { protein = it },
            label = { Text("Protein (g)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = carbs,
            onValueChange = { carbs = it },
            label = { Text("Carbs (g)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fat,
            onValueChange = { fat = it },
            label = { Text("Fat (g)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fiber,
            onValueChange = { fiber = it },
            label = { Text("Fiber (g)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = saturatedFat,
            onValueChange = { saturatedFat = it },
            label = { Text("Saturated fat (g)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = addedSugars,
            onValueChange = { addedSugars = it },
            label = { Text("Added sugars (g)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = sodium,
            onValueChange = { sodium = it },
            label = { Text("Sodium (mg)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth()
        )

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
                viewModel.upsertIngredient(
                    ingredientId = ingredientId,
                    bundleId = bundleId,
                    name = name,
                    portionSizeG = gramsValue ?: 0,
                    calories = caloriesValue ?: 0.0,
                    proteinG = proteinValue ?: 0.0,
                    carbsG = carbsValue ?: 0.0,
                    fatG = fatValue ?: 0.0,
                    fiberG = fiberValue ?: 0.0,
                    saturatedFatG = saturatedFatValue ?: 0.0,
                    addedSugarsG = addedSugarsValue ?: 0.0,
                    sodiumMg = sodiumValue ?: 0.0
                )
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (ingredientId == null) "Save ingredient" else "Update ingredient")
        }
    }
}
