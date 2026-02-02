package com.example.routinereminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.routinereminder.R
import com.example.routinereminder.ui.RoutineInsights

@Composable
fun RoutineInsightsCard(
    insights: RoutineInsights,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.routine_insights_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(
                    R.string.routine_insights_summary,
                    insights.totalCompleted,
                    insights.totalScheduled
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightMetric(
                    label = stringResource(R.string.routine_insights_today_completion),
                    value = "${insights.todayCompletionPercent}%",
                    modifier = Modifier.weight(1f)
                )
                InsightMetric(
                    label = stringResource(R.string.routine_insights_weekly_completion),
                    value = "${insights.weeklyCompletionPercent}%",
                    modifier = Modifier.weight(1f)
                )
                InsightMetric(
                    label = stringResource(R.string.routine_insights_average_daily_consistency),
                    value = "${insights.averageDailyConsistencyPercent}%",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightMetric(
                    label = stringResource(R.string.routine_insights_overall_completion),
                    value = "${insights.overallCompletionPercent}%",
                    modifier = Modifier.weight(1f)
                )
                InsightMetric(
                    label = stringResource(R.string.routine_insights_streak),
                    value = "${insights.currentStreakDays}d",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InsightMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
