package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

        Text(
            text = "Create Recipe",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

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

        Text(
            text = "Portion definition",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = portionType == PORTION_TYPE_GRAMS,
                    onClick = { portionType = PORTION_TYPE_GRAMS }
                )
                Spacer(Modifier.width(6.dp))
                Text("Grams")
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
        val canCreate = name.isNotBlank() && canSavePortion

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Ingredients",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = canCreate,
                onClick = {
                    viewModel.createBundleAndReturnId(
                        name = name,
                        description = description,
                        portionType = portionType,
                        customPortionGrams = customPortionValue
                    ) { bundleId ->
                        navController.navigate(Screen.BundleDetail.route(bundleId, edit = true)) {
                            popUpTo(Screen.BundleList.route)
                        }
                    }
                }
            ) {
                Text("Add ingredient")
            }

            Button(
                modifier = Modifier.weight(1f),
                enabled = canCreate,
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
}
