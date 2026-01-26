package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
@OptIn(ExperimentalMaterial3Api::class)
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

    val isPortionValid = portionType == PORTION_TYPE_GRAMS ||
        (customPortionGrams.toDoubleOrNull()?.let { it > 0 } == true)
    val canSave = name.trim().isNotBlank() && isPortionValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Recipe",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val customPortionValue = customPortionGrams.toDoubleOrNull()
                            viewModel.createBundleAndReturnId(
                                name = name,
                                description = description,
                                portionType = portionType,
                                customPortionGrams = customPortionValue
                            ) { bundleId ->
                                navController.navigate("bundle/$bundleId?edit=true") {
                                    popUpTo(Screen.CreateBundle.route) { inclusive = true }
                                }
                            }
                        },
                        enabled = canSave
                    ) {
                        Text("Save")
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
                style = MaterialTheme.typography.titleMedium
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isPortionValid) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Enter a gram value greater than zero.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
