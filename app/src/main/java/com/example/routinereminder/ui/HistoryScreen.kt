package com.example.routinereminder.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.routinereminder.data.model.SessionStats
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

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
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
                item {
                    AnalyticsSummary(sessions = sessions)
                    Spacer(modifier = Modifier.height(12.dp))
                }
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
private fun AnalyticsSummary(sessions: List<SessionStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.titleMedium
            )
            if (sessions.isEmpty()) {
                Text(
                    text = "No analytics yet. Complete a run to see weekly and monthly trends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val weeklySummaries = buildWeeklySummaries(sessions, maxWeeks = 4)
            val monthlySummaries = buildMonthlySummaries(sessions, maxMonths = 6)

            SummarySection(
                title = "Weekly (last ${weeklySummaries.size} weeks)",
                summaries = weeklySummaries
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummarySection(
                title = "Monthly (last ${monthlySummaries.size} months)",
                summaries = monthlySummaries
            )
        }
    }
}

@Composable
private fun SummarySection(title: String, summaries: List<PeriodSummary>) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    if (summaries.isEmpty()) {
        Text(
            text = "No data yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        summaries.forEach { summary ->
            Spacer(modifier = Modifier.height(6.dp))
            Column {
                Text(
                    text = summary.label,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = buildSummaryLine(summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun buildSummaryLine(summary: PeriodSummary): String {
    val distanceKm = summary.totalDistanceMeters / 1000.0
    val duration = formatDuration(summary.totalDurationSec)
    val pace = formatPace(summary.avgPaceSecPerKm)
    val calories = summary.totalCalories.toInt()
    return "${summary.sessionCount} sessions • ${distanceKm.format(2)} km • $duration • $pace min/km • $calories kcal"
}

private data class PeriodSummary(
    val label: String,
    val sessionCount: Int,
    val totalDistanceMeters: Double,
    val totalDurationSec: Long,
    val totalCalories: Double,
    val avgPaceSecPerKm: Long
)

private fun buildWeeklySummaries(
    sessions: List<SessionStats>,
    maxWeeks: Int
): List<PeriodSummary> {
    val zoneId = ZoneId.systemDefault()
    val weekFields = WeekFields.of(Locale.getDefault())
    val grouped = sessions.groupBy { session ->
        val date = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(session.startEpochMs),
            zoneId
        )
        date.with(weekFields.dayOfWeek(), 1)
    }
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    return grouped.toSortedMap(compareByDescending { it })
        .entries
        .take(maxWeeks)
        .map { (weekStart, weekSessions) ->
            val label = "Week of ${weekStart.format(formatter)}"
            buildSummary(label, weekSessions)
        }
}

private fun buildMonthlySummaries(
    sessions: List<SessionStats>,
    maxMonths: Int
): List<PeriodSummary> {
    val zoneId = ZoneId.systemDefault()
    val grouped = sessions.groupBy { session ->
        val date = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(session.startEpochMs),
            zoneId
        )
        date.withDayOfMonth(1)
    }
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    return grouped.toSortedMap(compareByDescending { it })
        .entries
        .take(maxMonths)
        .map { (monthStart, monthSessions) ->
            val label = monthStart.format(formatter)
            buildSummary(label, monthSessions)
        }
}

private fun buildSummary(label: String, sessions: List<SessionStats>): PeriodSummary {
    val totalDistance = sessions.sumOf { it.distanceMeters }
    val totalDuration = sessions.sumOf { it.durationSec }
    val totalCalories = sessions.sumOf { it.calories }
    val avgPace = if (totalDistance > 0.0) {
        ((totalDuration / (totalDistance / 1000.0)).toLong())
    } else {
        0L
    }
    return PeriodSummary(
        label = label,
        sessionCount = sessions.size,
        totalDistanceMeters = totalDistance,
        totalDurationSec = totalDuration,
        totalCalories = totalCalories,
        avgPaceSecPerKm = avgPace
    )
}

private fun formatDuration(durationSec: Long): String {
    val hours = durationSec / 3600
    val minutes = (durationSec % 3600) / 60
    val seconds = durationSec % 60
    return if (hours > 0) {
        "%dh %02dm".format(hours, minutes)
    } else {
        "%dm %02ds".format(minutes, seconds)
    }
}

private fun formatPace(paceSecPerKm: Long): String {
    if (paceSecPerKm <= 0L) return "--:--"
    val min = paceSecPerKm / 60
    val sec = paceSecPerKm % 60
    return "%d:%02d".format(min, sec)
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
                val splits = session.splitPaceSecPerKm.orEmpty()
                if (splits.isNotEmpty()) {
                    Text(
                        text = "Splits: ${formatSplits(splits)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Helper to format doubles nicely
private fun Double.format(digits: Int) = "%.${digits}f".format(this)

private fun formatSplits(splits: List<Long>): String {
    return splits.mapIndexed { index, paceSec ->
        "${index + 1}km ${formatSplitPace(paceSec)}"
    }.joinToString(", ")
}

private fun formatSplitPace(paceSec: Long): String {
    if (paceSec <= 0L) return "--:--"
    val min = paceSec / 60
    val sec = paceSec % 60
    return "%d:%02d".format(min, sec)
}
