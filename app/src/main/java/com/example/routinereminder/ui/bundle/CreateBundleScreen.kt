package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.example.routinereminder.data.entities.PORTION_TYPE_CUSTOM
import com.example.routinereminder.data.entities.PORTION_TYPE_GRAMS
import com.example.routinereminder.ui.Screen
import com.example.routinereminder.ui.components.RichTextEditor

@Composable
fun CreateBundleScreen(
    navController: NavController
) {
    // 🔑 Get the SAME ViewModel as BundleListScreen
    val parentEntry: NavBackStackEntry = remember(navController) {
        navController.getBackStackEntry(Screen.BundleList.route)
    }

    val viewModel: BundleViewModel = hiltViewModel(parentEntry)

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var portionType by remember { mutableStateOf(PORTION_TYPE_GRAMS) }
    var customPortionGrams by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {

        Text("Create Recipe", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        RichTextEditor(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            modifier = Modifier.fillMaxWidth(),
            outlined = true
        )

        Spacer(Modifier.height(12.dp))

        Text("Portion definition", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.weight(1f)) {
                RadioButton(
                    selected = portionType == PORTION_TYPE_GRAMS,
                    onClick = { portionType = PORTION_TYPE_GRAMS }
                )
                Spacer(Modifier.width(6.dp))
                Text("Grams")
            }
            Row(modifier = Modifier.weight(1f)) {
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
            Text(
                text = "This defines how many grams one portion represents.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        val customPortionValue = customPortionGrams.toDoubleOrNull()
        val canSavePortion = portionType == PORTION_TYPE_GRAMS ||
            (customPortionValue != null && customPortionValue > 0)

        Button(
            enabled = name.isNotBlank() && canSavePortion,
            onClick = {
                viewModel.createBundle(
                    name = name,
                    description = description,
                    portionType = portionType,
                    customPortionGrams = customPortionValue
                )
                navController.popBackStack() // go back to list
            }
        ) {
            Text("Save")
        }
    }
}
