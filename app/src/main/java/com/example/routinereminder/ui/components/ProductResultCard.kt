package com.example.routinereminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


/**
 * Lightweight nutrition model for preview cards
 * (used by both Scan + Search → same UI)
 */
data class NutritionPreview(
    val kcalPer100g: Double? = null,
    val proteinPer100g: Double? = null,
    val carbsPer100g: Double? = null,
    val fatPer100g: Double? = null,
    val fiberPer100g: Double? = null,
    val sugarPer100g: Double? = null,
    val saltPer100g: Double? = null,
)

@Composable
fun ProductResultCard(
    title: String,
    subtitle: String?,
    nutrition: NutritionPreview?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1C), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        // Product name
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Brand / subtitle
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0B0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
// --- Row 1: Calories ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            NutrientChip("kcal", nutrition?.kcalPer100g)
        }

        Spacer(modifier = Modifier.height(10.dp))

// --- Row 2: Carbs / Protein / Fat ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NutrientChip("C", nutrition?.carbsPer100g)
            NutrientChip("P", nutrition?.proteinPer100g)
            NutrientChip("F", nutrition?.fatPer100g)
        }

        Spacer(modifier = Modifier.height(8.dp))

// --- Row 3: Fiber / Sugar / Sodium ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NutrientChip("Fi", nutrition?.fiberPer100g)
            NutrientChip("Sugar", nutrition?.sugarPer100g)
            NutrientChip(
                "Na",
                nutrition?.saltPer100g   // already converted to g earlier
            )
        }




        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "per 100 g",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8A8A8A)
        )
    }
}

@Composable
private fun NutrientChip(label: String, value: Double?) {
    val displayValue = value?.let { formatOneDecimal(it) } ?: "-"

    Box(
        modifier = Modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$label $displayValue",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFEAEAEA)
        )
    }
}

private fun formatOneDecimal(value: Double): String {
    return String.format("%.1f", value)
}
