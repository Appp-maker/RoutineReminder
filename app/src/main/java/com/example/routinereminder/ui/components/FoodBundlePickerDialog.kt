package com.example.routinereminder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.routinereminder.data.entities.FoodBundle

@Composable
fun FoodBundlePickerDialog(
    bundles: List<FoodBundle>,
    onPick: (FoodBundle) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text("Add recipe", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                LazyColumn {
                    items(bundles) { bundle ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPick(bundle)
                                    onDismiss()
                                }
                                .padding(12.dp)
                        ) {
                            Text(bundle.name, style = MaterialTheme.typography.titleMedium)
                            if (bundle.description.isNotBlank()) {
                                Text(
                                    formatChecklistText(bundle.description),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
