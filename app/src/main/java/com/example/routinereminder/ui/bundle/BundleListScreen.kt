package com.example.routinereminder.ui.bundle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.routinereminder.ui.Screen

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

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Bundles",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(12.dp))

        if (bundles.isEmpty()) {
            Text("No bundles yet", color = Color.Gray)
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
            Text("Create Bundle")
        }
    }
}
