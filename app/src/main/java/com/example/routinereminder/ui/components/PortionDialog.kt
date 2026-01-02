package com.example.routinereminder.ui.components
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.data.entities.LoggedFood
import com.example.routinereminder.ui.CalorieTrackerViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val Bg = Color(0xFF121212)
private val Track = Color(0xFF3A3A3A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFBDBDBD)
private val AccentBlue = Color(0xFF64B5F6)
private val DangerRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortionDialog(
    foodProduct: FoodProduct,
    onDismiss: () -> Unit,

    // Existing tracker behavior (unchanged)
    onConfirm: (
        portion: Double,
        foodProduct: FoodProduct,
        time: LocalTime,
        mealSlot: String,
        isOneTime: Boolean,
        repeatDays: Set<DayOfWeek>,
        repeatEveryWeeks: Int,
        startDate: LocalDate
    ) -> Unit,

    // NEW: optional bundle callback
    onAddToBundle: ((food: FoodProduct, portionG: Double) -> Unit)? = null,

    currentTotals: CalorieTrackerViewModel.DailyTotals,
    targets: CalorieTrackerViewModel.DailyTargets,
    existingLoggedFood: LoggedFood? = null,
    initialPortion: Double? = null
)
{
    // ==== Initialize state from existing entry if available ====
    val initialIsOneTime = existingLoggedFood?.isOneTime ?: true
    val initialRepeatDays = existingLoggedFood?.repeatOnDays ?: emptySet()
    val initialRepeatEveryWeeks = existingLoggedFood?.repeatEveryWeeks ?: 1
    val initialStartDate = existingLoggedFood?.startEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()

    var portionText by remember {
        mutableStateOf(
            (initialPortion ?: existingLoggedFood?.portionSizeG ?: foodProduct.servingSizeG)?.let {
                if (it == 0.0) "" else it.toString()
            } ?: ""
        )
    }

    var selectedMeal by remember { mutableStateOf("Breakfast") }
    var selectedTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var isOneTime by remember { mutableStateOf(initialIsOneTime) }
    var selectedDays by remember { mutableStateOf(initialRepeatDays) }
    var repeatEveryWeeks by remember { mutableStateOf(initialRepeatEveryWeeks) }
    var startDate by remember { mutableStateOf(initialStartDate) }
    var customName by remember { mutableStateOf(foodProduct.name) }
    var customCalories by remember { mutableStateOf(foodProduct.caloriesPer100g.toString()) }
    var customCarbs by remember { mutableStateOf(foodProduct.carbsPer100g.toString()) }
    var customProtein by remember { mutableStateOf(foodProduct.proteinPer100g.toString()) }
    var customFat by remember { mutableStateOf(foodProduct.fatPer100g.toString()) }
    var customSugar by remember { mutableStateOf(foodProduct.addedSugarsPer100g.toString()) }
    var customFiber by remember { mutableStateOf(foodProduct.fiberPer100g.toString()) }
    var customSodium by remember { mutableStateOf(foodProduct.sodiumPer100g.toString()) }

    val isCustom = foodProduct.name.isBlank()

    val mealTimeMap = mapOf(
        "Breakfast" to LocalTime.of(8, 0),
        "Mid-Morning" to LocalTime.of(10, 30),
        "Lunch" to LocalTime.of(13, 0),
        "Afternoon Snack" to LocalTime.of(16, 0),
        "Dinner" to LocalTime.of(19, 0),
        "Evening Snack" to LocalTime.of(21, 30)
    )
    LaunchedEffect(selectedMeal) {
        selectedTime = mealTimeMap[selectedMeal] ?: LocalTime.of(8, 0)
    }

    val portion = portionText.toDoubleOrNull() ?: 0.0

    // --- Nutrient helpers ---
    val base = if (isCustom) {
        foodProduct.copy(
            caloriesPer100g = customCalories.toDoubleOrNull() ?: 0.0,
            proteinPer100g = customProtein.toDoubleOrNull() ?: 0.0,
            carbsPer100g = customCarbs.toDoubleOrNull() ?: 0.0,
            fatPer100g = customFat.toDoubleOrNull() ?: 0.0,
            fiberPer100g = customFiber.toDoubleOrNull() ?: 0.0,
            addedSugarsPer100g = customSugar.toDoubleOrNull() ?: 0.0,
            sodiumPer100g = customSodium.toDoubleOrNull() ?: 0.0
        )
    } else foodProduct

    fun cals(g: Double) = base.caloriesPer100g / 100.0 * g
    fun protein(g: Double) = base.proteinPer100g / 100.0 * g
    fun carbs(g: Double) = base.carbsPer100g / 100.0 * g
    fun fat(g: Double) = base.fatPer100g / 100.0 * g
    fun fiber(g: Double) = base.fiberPer100g / 100.0 * g
    fun sugar(g: Double) = base.addedSugarsPer100g / 100.0 * g
    fun sodiumMg(g: Double) = base.sodiumPer100g / 100.0 * 1000 *g



    val oldPortion = existingLoggedFood?.portionSizeG ?: 0.0
    val deltaPortion = portion - oldPortion
    val dCalories = cals(deltaPortion)
    val dProtein = protein(deltaPortion)
    val dCarbs = carbs(deltaPortion)
    val dFat = fat(deltaPortion)
    val dFiber = fiber(deltaPortion)
    val dSugar = sugar(deltaPortion)
    val dSodium = sodiumMg(deltaPortion)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().background(Bg),
            color = Bg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (isCustom) {

                    // ---- NAME FIELD ----
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Custom Food Name (required)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )}
                else
                {
                        Text(
                            text = foodProduct.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                }

                // ===== Portion input =====
                Text("Enter portion size in grams", color = TextPrimary)
                OutlinedTextField(
                    value = portionText,
                    onValueChange = { portionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary)
                )


                    Spacer(Modifier.height(10.dp))

// ===== Overview (per portion vs daily targets) =====
// ---------- ROW 0: CALORIES ----------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            NutrientTile(
                                label = "Calories",
                                unit = "kcal",
                                current = cals(portion),
                                target = targets.calories,
                                delta = cals(portion),
                                customMode = true
                            )
                        }
                        if (isCustom) {
                        OutlinedTextField(
                            value = customCalories,
                            onValueChange = { customCalories = cleanInput(customCalories, it) },
                            label = { Text("kcal") },
                            modifier = Modifier
                                .width(80.dp)
                                .height(90.dp),
                            singleLine = true
                        )
                    }}

                    Spacer(Modifier.height(10.dp))
                if (isCustom) {
// ---------- ROW 1: CARBS + PROTEIN ----------
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            NutrientTile("Carbs", "g", carbs(portion), targets.carbsG, carbs(portion), customMode = true)
                        }
                        Column(Modifier.weight(1f)) {
                            NutrientTile("Protein", "g", protein(portion), targets.proteinG, protein(portion), customMode = true)
                        }
                    }

// ---------- CARBS + PROTEIN INPUTS ----------
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customCarbs,
                            onValueChange = { customCarbs = cleanInput(customCarbs, it) },
                            label = { Text("g") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customProtein,
                            onValueChange = { customProtein = cleanInput(customProtein, it) },
                            label = { Text("g") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(10.dp))

// ---------- ROW 2: FAT + FIBER ----------
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            NutrientTile("Fat", "g", fat(portion), targets.fatG, fat(portion), customMode = true)
                        }
                        Column(Modifier.weight(1f)) {
                            NutrientTile("Fiber", "g", fiber(portion), targets.fiberG, fiber(portion), customMode = true)
                        }
                    }

// ---------- FAT + FIBER INPUTS ----------
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customFat,
                            onValueChange = { customFat = cleanInput(customFat, it) },
                            label = { Text("g") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customFiber,
                            onValueChange = { customFiber = cleanInput(customFiber, it) },
                            label = { Text("g") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(16.dp))

// ---------- ROW 3: SUGAR + SODIUM ----------
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            NutrientTile("Sugar", "g", sugar(portion), targets.addedSugarsG, sugar(portion), lowerIsBetter = true, customMode = true)
                        }
                        Column(Modifier.weight(1f)) {
                            NutrientTile("Sodium", "mg", sodiumMg(portion), targets.sodiumMg, sodiumMg(portion), lowerIsBetter = true, customMode = true)
                        }
                    }

// ---------- SUGAR + SODIUM INPUTS ----------
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customSugar,
                            onValueChange = { customSugar = cleanInput(customSugar, it) },
                            label = { Text("g") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customSodium,
                            onValueChange = { customSodium = cleanInput(customSodium, it) },
                            label = { Text("mg") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                }






                if (!isCustom) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            NutrientTile("Carbs", "g", currentTotals.carbsG, targets.carbsG, dCarbs)
                            Spacer(Modifier.height(8.dp))
                            NutrientTile("Fiber", "g", currentTotals.fiberG, targets.fiberG, dFiber)
                            Spacer(Modifier.height(8.dp))
                            NutrientTile("Sodium", "mg", currentTotals.sodiumMg, targets.sodiumMg, dSodium, lowerIsBetter = true)
                        }

                        Column(Modifier.weight(1f)) {
                            NutrientTile("Protein", "g", currentTotals.proteinG, targets.proteinG, dProtein)
                            Spacer(Modifier.height(8.dp))
                            NutrientTile("Sugar", "g", currentTotals.addedSugarsG, targets.addedSugarsG, dSugar, lowerIsBetter = true)
                            Spacer(Modifier.height(8.dp))
                            NutrientTile("Fat", "g", currentTotals.fatG, targets.fatG, dFat)
                        }
                    }
                } else {
                    // custom foods: NO large tiles here, because they were shown above in compact form
                }

// NOTHING HERE. Custom food fields already shown above.



                Spacer(Modifier.height(20.dp))


// ===== Meal Slot Selection =====
                Text("Meal Slot", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(6.dp))

                var expandedMeal by remember { mutableStateOf(false) }
                val mealOptions = listOf("Breakfast", "Mid-Morning", "Lunch", "Afternoon Snack", "Dinner", "Evening Snack")

                Surface(
                    color = Track,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedMeal = true }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = selectedMeal,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                }

                DropdownMenu(
                    expanded = expandedMeal,
                    onDismissRequest = { expandedMeal = false }
                ) {
                    mealOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedMeal = option
                                expandedMeal = false
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ===== Scheduling =====
                Text("Scheduling Options", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isOneTime,
                        onCheckedChange = { isOneTime = it },
                        colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                    )
                    Text("One-time only", color = TextPrimary)
                }

                if (!isOneTime) {
                    Spacer(Modifier.height(8.dp))
                    Text("Repeat on:", color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DayOfWeek.values().forEach { day ->
                            val selected = selectedDays.contains(day)
                            Surface(
                                color = if (selected) AccentBlue else Track,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clickable {
                                        selectedDays = if (selected) selectedDays - day else selectedDays + day
                                    }
                            ) {
                                Text(
                                    text = day.name.take(3),
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }



                    Spacer(Modifier.height(12.dp))
                    Text("Every N weeks:", color = TextPrimary)
                    OutlinedTextField(
                        value = repeatEveryWeeks.toString(),
                        onValueChange = { repeatEveryWeeks = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary)
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Start anchor date: $startDate", color = AccentBlue)
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "Cancel",
                        color = TextSecondary,
                        modifier = Modifier.clickable { onDismiss() }.padding(10.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    val saveEnabled =
                        portion > 0.0 && (!isCustom || customName.isNotBlank())


                    Button(
                        onClick = {
                            if (!saveEnabled) return@Button  // Prevent saving without name

                            val usePortion = portion
                            if (usePortion > 0.0) {

                                val finalFood = if (isCustom) {
                                    foodProduct.copy(
                                        name = customName,
                                        caloriesPer100g = customCalories.toDoubleOrNull() ?: 0.0,
                                        carbsPer100g = customCarbs.toDoubleOrNull() ?: 0.0,
                                        proteinPer100g = customProtein.toDoubleOrNull() ?: 0.0,
                                        fatPer100g = customFat.toDoubleOrNull() ?: 0.0,
                                        addedSugarsPer100g = customSugar.toDoubleOrNull() ?: 0.0,
                                        fiberPer100g = customFiber.toDoubleOrNull() ?: 0.0,
                                        sodiumPer100g = customSodium.toDoubleOrNull() ?: 0.0
                                    )
                                } else foodProduct

                                if (onAddToBundle != null) {
                                    // Bundle mode: ignore scheduling completely
                                    onAddToBundle.invoke(finalFood, usePortion)
                                } else {
                                    // Tracker mode (existing behavior)
                                    onConfirm(
                                        usePortion,
                                        finalFood,
                                        selectedTime,
                                        selectedMeal,
                                        isOneTime,
                                        selectedDays,
                                        repeatEveryWeeks,
                                        startDate
                                    )
                                }

                                onDismiss()

                            }
                        },
                        enabled = saveEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(
                            when {
                                onAddToBundle != null -> "Add to recipe"
                                existingLoggedFood == null -> "Save"
                                else -> "Update"
                            },
                            color = Color.White
                        )

                    }


                }
                }
            }
        }
    }




/* ===== UI bits ===== */

@Composable
private fun CaloriesOverview(current: Double, target: Double, delta: Double) {
    val after = current + delta
    val progressAfter = if (target <= 0.0) 0f else min(after / target, 1.0).toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFF1C1C1C), shape = RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text("Calories", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        // Simple linear progress (keeps visuals consistent)
        LinearBar(progress = progressAfter, trackColor = Track, fillColor = AccentBlue)

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${current.round()} / ${target.round()}",
                color = TextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = (if (delta >= 0) "+${delta.round()}" else "${delta.round()}"),
                color = AccentBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NutrientTile(
    label: String,
    unit: String,
    current: Double,
    target: Double,
    delta: Double,
    lowerIsBetter: Boolean = false,
    customMode: Boolean = false
) {
    val after = current + delta
    val progressAfter = when {
        target <= 0.0 -> 0f
        else -> min(after / target, 1.0).toFloat()
    }
    val overTarget = (!lowerIsBetter && after > target) || (lowerIsBetter && after > target)
    val barColor = if (overTarget) DangerRed else AccentBlue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFF1C1C1C), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        LinearBar(progress = progressAfter, trackColor = Track, fillColor = barColor)
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!customMode) {
                // Default mode: "40 g / 306 g"
                Text(
                    text = String.format("%d %s / %d %s", current.round(), unit, target.round(), unit),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            } else {
                // Custom dialog mode:
                // LEFT  = +current (blue, no unit)
                // RIGHT = "/ target unit" (white)
                Text(
                    text = "+${current.round()}",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "/ ${target.round()} $unit",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End
                )
            }

            // Right-hand delta badge ONLY in normal mode
            if (!customMode) {
                Text(
                    text = (if (delta >= 0) "+${delta.round()}" else "${delta.round()}") + " " + unit,
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun NutrientPairRow(
    label1: String,
    unit1: String,
    current1: Double,
    target1: Double,
    delta1: Double,
    input1: String,
    onInput1: (String) -> Unit,

    label2: String,
    unit2: String,
    current2: Double,
    target2: Double,
    delta2: Double,
    input2: String,
    onInput2: (String) -> Unit,

    lowerIsBetter1: Boolean = false,
    lowerIsBetter2: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT TILE ---
        Column(Modifier.weight(1f)) {
            NutrientTile(
                label = label1,
                unit = unit1,
                current = current1,
                target = target1,
                delta = delta1,
                lowerIsBetter = lowerIsBetter1,
                customMode = true
            )
        }

        // --- LEFT INPUT ---
        OutlinedTextField(
            value = input1,
            onValueChange = { onInput1(it.filter(Char::isDigit)) },
            label = { Text(unit1) },
            modifier = Modifier
                .weight(0.6f)
                .height(56.dp),
            singleLine = true
        )

        // --- RIGHT TILE ---
        Column(Modifier.weight(1f)) {
            NutrientTile(
                label = label2,
                unit = unit2,
                current = current2,
                target = target2,
                delta = delta2,
                lowerIsBetter = lowerIsBetter2,
                customMode = true
            )
        }

        // --- RIGHT INPUT ---
        OutlinedTextField(
            value = input2,
            onValueChange = { onInput2(it.filter(Char::isDigit)) },
            label = { Text(unit2) },
            modifier = Modifier
                .weight(0.6f)
                .height(56.dp),
            singleLine = true
        )
    }
}
@Composable
private fun EditableNutrientRow(
    label: String,
    unit: String,
    current: Double,
    target: Double,
    delta: Double,
    textValue: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1C), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {

        // --- LABEL + VALUES ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextSecondary, fontSize = 13.sp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${current.round()} $unit / ${target.round()} $unit",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    (if (delta >= 0) "+${delta.round()}" else "${delta.round()}") + " $unit",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // --- INPUT FIELD ---
        OutlinedTextField(
            value = textValue,
            onValueChange = { onValueChange(it.filter(Char::isDigit)) },
            label = { Text(unit) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}


@Composable
private fun LinearBar(progress: Float, trackColor: Color, fillColor: Color) {
    // Custom lightweight bar to keep the dialog snappy
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color.Transparent, RoundedCornerShape(6.dp))
    ) {
        val w = size.width
        val h = size.height
        // track
        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = h / 2, y = h / 2)
        )
        // fill
        drawRoundRect(
            color = fillColor,
            size = androidx.compose.ui.geometry.Size(width = w * progress.coerceIn(0f, 1f), height = h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = h / 2, y = h / 2)
        )
    }
}

fun cleanInput(old: String, new: String): String {
    val digits = new.filter(Char::isDigit)

    // First typed character replaces "0.0" or "0"
    if (old == "0.0" || old == "0") {
        return digits.trimStart('0')
    }

    // Normal typing afterwards
    return digits
}

/* ===== tiny utils ===== */

private fun Double.round(): Int = this.roundToInt()
