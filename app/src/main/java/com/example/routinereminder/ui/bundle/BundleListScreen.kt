package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.routinereminder.ui.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun BundleListScreen(
    navController: NavController,
    viewModel: BundleViewModel = hiltViewModel()
) {
    val bundles by viewModel.bundles.collectAsState()

    // 🔑 THIS WAS MISSING
    LaunchedEffect(Unit) {
        viewModel.loadBundles()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipes") },
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
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Spacer(Modifier.height(12.dp))

            if (bundles.isEmpty()) {
                Text("No recipes yet", color = Color.Gray)
            } else {
                LazyColumn {
                    items(bundles) { bundle ->
                        Text(
                            text = bundle.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("bundle/${bundle.id}")
                                }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(Screen.CreateBundle.route) }
            ) {
                Text("Create Recipe")
            }
        }
    }
}
