package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.example.routinereminder.ui.Screen

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

    Column(modifier = Modifier.padding(16.dp)) {

        Text("Create Bundle", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            enabled = name.isNotBlank(),
            onClick = {
                viewModel.createBundle(name, description)
                navController.popBackStack() // go back to list
            }
        ) {
            Text("Save")
        }
    }
}


