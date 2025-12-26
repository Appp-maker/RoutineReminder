package com.example.routinereminder.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.routinereminder.data.model.SessionStats
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<SessionStats>>(emptyList()) }

    LaunchedEffect(Unit) {
        sessions = SessionStore.loadAllSessions(context)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History") }
            )
        }
    ) { padding ->
        // ... rest of your content
    }



    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History") }
            )

        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No past sessions yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
            ) {
                items(sessions) { session ->
                    HistoryItem(session, context, navController) {
                        scope.launch {
                            SessionStore.deleteSession(context, session)
                            sessions = SessionStore.loadAllSessions(context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    session: SessionStats,
    context: Context,
    navController: NavController,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                navController.navigate("share_preview/${session.id}")
            },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.activity,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Date: ${SessionStore.formatTime(session.startEpochMs)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Distance: ${(session.distanceMeters / 1000).format(2)} km",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Pace: ${(session.avgPaceSecPerKm / 60)}:${(session.avgPaceSecPerKm % 60).toString().padStart(2, '0')} min/km",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        navController.navigate("share_preview/${session.id}")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Cyan
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

// Helper to format doubles nicely
private fun Double.format(digits: Int) = "%.${digits}f".format(this)
