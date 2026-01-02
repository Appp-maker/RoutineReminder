package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun AddBundleItemScreen(
    navController: NavController,
    bundleId: Long,
    viewModel: BundleViewModel = hiltViewModel()
) {
    var foodName by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {

        Text("Add Ingredient", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = foodName,
            onValueChange = { foodName = it },
            label = { Text("Food name") }
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = grams,
            onValueChange = { grams = it },
            label = { Text("Grams") }
        )

        Spacer(Modifier.height(16.dp))

        Button(
            enabled = foodName.isNotBlank() && grams.toIntOrNull() != null,
            onClick = {
                viewModel.addItemToBundle(
                    bundleId = bundleId,
                    foodName = foodName.trim(),
                    grams = grams.toInt()
                )
                navController.popBackStack()
            }
        ) {
            Text("Save")
        }
    }
}
