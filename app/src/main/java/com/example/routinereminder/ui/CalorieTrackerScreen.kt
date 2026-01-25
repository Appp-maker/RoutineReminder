package com.example.routinereminder.ui
import java.time.DayOfWeek
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.example.routinereminder.DateDisplayHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.routinereminder.ui.components.isNoEventFoodColor
import com.example.routinereminder.ui.components.resolveEventFoodColor
import androidx.compose.ui.unit.sp
import com.example.routinereminder.ui.theme.AppPalette
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.routinereminder.ui.Screen
import com.example.routinereminder.data.entities.FoodProduct
import com.example.routinereminder.data.entities.LoggedFood
import com.example.routinereminder.ui.components.PortionDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalConfiguration
import com.example.routinereminder.ui.components.ProductResultCard
import com.example.routinereminder.ui.components.NutritionPreview
import com.example.routinereminder.ui.components.SeriesColorDot
import com.example.routinereminder.ui.components.formatChecklistText
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.routinereminder.ui.components.FoodBundlePickerDialog
import java.time.LocalTime
import com.example.routinereminder.data.entities.PORTION_TYPE_CUSTOM
private const val TAG = "CalorieTrackerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieTrackerScreen(
    navController: NavController,
    startMode: String = "default",
    viewModel: CalorieTrackerViewModel = hiltViewModel()
) {
    val dailyTotals by viewModel.dailyTotals.collectAsState()
    val dailyTargets by viewModel.dailyTargets.collectAsState()
    val scannedFoodProduct by viewModel.scannedFoodProduct.collectAsState()
    val isProfileComplete by viewModel.isProfileComplete.collectAsState()
    val loggedFoods by viewModel.loggedFoods.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val foodConsumedTrackingEnabled by viewModel.foodConsumedTrackingEnabled.collectAsState()
    var selectedMealFilter by remember { mutableStateOf<String?>(null) }
    var activeMealSlot by remember { mutableStateOf<String?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
//    var modeConsumed by rememberSaveable { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<LoggedFood?>(null) }
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var totalHorizontalDrag by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var foodToDelete by remember { mutableStateOf<LoggedFood?>(null) }
    val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    var showBundlePicker by remember { mutableStateOf(false) }
    var showAllFoods by rememberSaveable { mutableStateOf(false) }
    val bundles by viewModel.foodBundles.collectAsState()
    val pendingBundleId by viewModel.pendingBundlePreview.collectAsState()
    val bundleForPortion by produceState<com.example.routinereminder.data.entities.FoodBundle?>(
        initialValue = null,
        key1 = pendingBundleId
    ) {
        value = pendingBundleId?.let { viewModel.getBundleById(it) }
    }






    LaunchedEffect(startMode) {
        when (startMode) {
            "default" -> {
            }
            "search" -> {
                showSearchDialog = true
            }
            "custom" -> {
                viewModel.onFoodSelected(
                    FoodProduct(
                        name = "",
                        caloriesPer100g = 0.0,
                        proteinPer100g = 0.0,
                        carbsPer100g = 0.0,
                        fatPer100g = 0.0,
                        fiberPer100g = 0.0,
                        saturatedFatPer100g = 0.0,
                        addedSugarsPer100g = 0.0,
                        sodiumPer100g = 0.0
                    )
                )
            }
        }
    }






    // Observe barcode result
    DisposableEffect(navController) {
        val observer = Observer<String> { barcode ->
            barcode?.let {
                viewModel.onBarcodeScanned(it)
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("barcode")
            }
        }
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getLiveData<String>("barcode")?.observe(lifecycleOwner, observer)
        onDispose { handle?.getLiveData<String>("barcode")?.removeObserver(observer) }
    }


    // Food search dialog
    if (showSearchDialog) {
        FoodSearchDialog(
            searchResults = searchResults,
            onDismiss = { showSearchDialog = false },
            onFoodSelected = {
                viewModel.onFoodSelected(it)
                showSearchDialog = false
            },
            onSearch = { viewModel.searchFood(it) }
        )
    }

    if (showBundlePicker) {
        FoodBundlePickerDialog(
            bundles = bundles,
            onPick = { bundle ->
                viewModel.selectBundle(bundle.id)
            },
            onDismiss = { showBundlePicker = false }
        )
    }


    // Portion dialog after scanning
    scannedFoodProduct?.let { food ->
        val isBundlePreview = pendingBundleId != null
        val portionDefinitionText = if (isBundlePreview) {
            if (bundleForPortion?.portionType == PORTION_TYPE_CUSTOM) {
                val portionGrams = bundleForPortion?.customPortionGrams ?: 0.0
                "Template portion: 1 portion = ${portionGrams.toInt()} g"
            } else {
                "Template portion: grams"
            }
        } else {
            null
        }
        val portionUnitLabel = if (isBundlePreview && bundleForPortion?.portionType == PORTION_TYPE_CUSTOM) {
            "portion(s)"
        } else {
            "g"
        }
        val portionMultiplier = if (isBundlePreview && bundleForPortion?.portionType == PORTION_TYPE_CUSTOM) {
            bundleForPortion?.customPortionGrams ?: 1.0
        } else {
            1.0
        }

        PortionDialog(
            foodProduct = food,
            onDismiss = { viewModel.clearScannedProduct() },
            // IMPORTANT: call addFood(...) with the scheduling values from the dialog
            onConfirm = { portion, finalFood, time, mealSlot, isOneTime, repeatDays, repeatEveryWeeks, anchorDate, colorArgb ->
                if (isBundlePreview) {
                    viewModel.addBundleToTracker(
                        bundleId = pendingBundleId!!,
                        mealSlot = mealSlot,
                        portionSizeG = portion,
                        isOneTime = isOneTime,
                        repeatDays = repeatDays,
                        repeatEveryWeeks = repeatEveryWeeks,
                        startDate = anchorDate,
                        colorArgb = colorArgb
                    )
                } else {
                    viewModel.addFood(
                        portion = portion,
                        foodProduct = finalFood,
                        time = time,
                        mealSlot = mealSlot,
                        isOneTime = isOneTime,
                        repeatDays = repeatDays,
                        repeatEveryWeeks = repeatEveryWeeks,
                        startDate = anchorDate,
                        colorArgb = colorArgb
                    )
                }

                viewModel.clearScannedProduct()
            },
            portionUnitLabel = portionUnitLabel,
            portionToGramsMultiplier = portionMultiplier,
            portionDefinitionText = portionDefinitionText,
            showSchedulingOptions = true,
            currentTotals = viewModel.dailyTotals.value ?: CalorieTrackerViewModel.DailyTotals(
                calories = 0.0,
                proteinG = 0.0,
                carbsG = 0.0,
                fatG = 0.0,
                fiberG = 0.0,
                saturatedFatG = 0.0,
                addedSugarsG = 0.0,
                sodiumMg = 0.0
            ),
            targets = viewModel.dailyTargets.value ?: CalorieTrackerViewModel.DailyTargets(
                calories = 0.0,
                proteinG = 0.0,
                carbsG = 0.0,
                fatG = 0.0,
                fiberG = 0.0,
                saturatedFatG = 0.0,
                addedSugarsG = 0.0,
                sodiumMg = 0.0
            )
        )
    }

// Portion dialog for editing logged food
    editingFood?.let { food ->
        PortionDialog(
            foodProduct = food.foodProduct,
            initialPortion = food.portionSizeG,
            existingLoggedFood = food,
            onDismiss = { editingFood = null },
            onConfirm = { portion, finalFood, time, mealSlot, isOneTime, repeatDays, repeatEveryWeeks, anchorDate, colorArgb ->
                if (isOneTime || repeatDays.isEmpty()) {
                    // Convert to one-time or update existing one-time
                    viewModel.updateLoggedFood(
                        loggedFood = food,
                        newPortionSizeG = portion,
                        mealSlot = mealSlot,
                        isOneTime = true,
                        repeatDays = emptySet(),
                        repeatEveryWeeks = 1,
                        startDate = anchorDate,
                        updatedFoodProduct = finalFood,
                        colorArgb = colorArgb
                    )
                } else {
                    // Remove old recurring pattern (if any)
                    viewModel.deleteRecurringEntries(food)

                    // Recreate full recurring schedule fresh
                    viewModel.addFood(
                        portion = portion,
                        foodProduct = finalFood,
                        mealSlot = mealSlot,
                        time = time,
                        isOneTime = false,
                        repeatDays = repeatDays,
                        repeatEveryWeeks = repeatEveryWeeks,
                        startDate = anchorDate,
                        colorArgb = colorArgb
                    )

                    // Optionally remove the original edited entry if it was one-time before
                    viewModel.deleteLoggedFood(food)
                }
                editingFood = null

            },
            currentTotals = viewModel.dailyTotals.value ?: CalorieTrackerViewModel.DailyTotals(
                calories = 0.0,
                proteinG = 0.0,
                carbsG = 0.0,
                fatG = 0.0,
                fiberG = 0.0,
                saturatedFatG = 0.0,
                addedSugarsG = 0.0,
                sodiumMg = 0.0
            ),
            targets = viewModel.dailyTargets.value ?: CalorieTrackerViewModel.DailyTargets(
                calories = 0.0,
                proteinG = 0.0,
                carbsG = 0.0,
                fatG = 0.0,
                fiberG = 0.0,
                saturatedFatG = 0.0,
                addedSugarsG = 0.0,
                sodiumMg = 0.0
            )
        )
    }

// 🔴 DELETE CONFIRMATION DIALOG (ADD HERE)
    if (showDeleteDialog && foodToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                foodToDelete = null
            },
            title = { Text("Delete food") },
            text = {
                Text(
                    "Do you want to remove this food only for today, or remove this and all future occurrences in the series? Past days will not be affected."
                )

            },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        // ONLY this day
                        viewModel.deleteLoggedFood(foodToDelete!!)
                        showDeleteDialog = false
                        foodToDelete = null
                    }) {
                        Text("Only this day")
                    }

                    TextButton(onClick = {
                        // ENTIRE series
                        viewModel.deleteRecurringEntries(foodToDelete!!)
                        showDeleteDialog = false
                        foodToDelete = null
                    }) {
                        Text("Entire series")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    foodToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    fun UnifiedDateHeader(
        currentDate: LocalDate,
        onPreviousDay: () -> Unit,
        onNextDay: () -> Unit,
        onDateTextClick: () -> Unit,
        onSettingsClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT ARROW – FIXED SIZE
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Day"
                )
            }

            // DATE – WEIGHTED CENTER
            Text(
                text = currentDate.format(formatter),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDateTextClick() },
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // RIGHT ARROW – FIXED SIZE
            IconButton(
                onClick = onNextDay,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Day"
                )
            }

            // SETTINGS – FIXED SIZE
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val newDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.setSelectedDate(newDate)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalHorizontalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalHorizontalDrag += dragAmount
                        },
                        onDragEnd = {
                            when {
                                totalHorizontalDrag > swipeThresholdPx -> viewModel.selectPreviousDay()
                                totalHorizontalDrag < -swipeThresholdPx -> viewModel.selectNextDay()
                            }
                        }
                    )
                }
                // 🔽 only keep bottom inset, no top padding
                .padding(
                    PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {   // OPEN Column content correctly

            if (!isProfileComplete) {
                MissingProfileScreen(navController)
                return@Column
            }

            // --- Overview section ---
            dailyTotals?.let { totals ->
                dailyTargets?.let { targets ->


                    // 1) Date row – EXACTLY like Routine tab

                    UnifiedDateHeader(
                        currentDate = selectedDate,
                        onPreviousDay = { viewModel.selectPreviousDay() },
                        onNextDay = { viewModel.selectNextDay() },
                        onDateTextClick = { showDatePicker = true },
                        onSettingsClick = { navController.navigate("settings/calories") },
                        modifier = Modifier.fillMaxWidth()
                    )


                    // 2) Compact row: circle on the left, buttons stacked on the right

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularNutrientIndicator(
                                label = "Calories",
                                currentValue = totals.calories,
                                targetValue = targets.calories,
                                modifier = Modifier.size(160.dp)   // a bit smaller to save space
                            )
                        }

                        // Scan / Search buttons
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = { navController.navigate(Screen.BarcodeScanner.route) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Scan")
                            }

                            Button(
                                onClick = { showSearchDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Search")

                            }
// CUSTOM BUTTON
                            Button(
                                onClick = {
                                    // Create an empty product the user can edit in PortionDialog
                                    viewModel.onFoodSelected(
                                        FoodProduct(
                                            name = "",
                                            caloriesPer100g = 0.0,
                                            proteinPer100g = 0.0,
                                            carbsPer100g = 0.0,
                                            fatPer100g = 0.0,
                                            fiberPer100g = 0.0,
                                            saturatedFatPer100g = 0.0,
                                            addedSugarsPer100g = 0.0,
                                            sodiumPer100g = 0.0
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Custom")
                            }
                            Button(
                                onClick = { navController.navigate(Screen.BundleList.route) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Recipe")
                            }




                        }


                    }

                    // 3) Nutrient bars directly under that row

                    Spacer(modifier = Modifier.height(4.dp))
                    NutrientRow(totals, targets)

                }
                // --- spacing before meal section ---
                Spacer(Modifier.height(4.dp))

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    SegmentedButton(
                        selected = !showAllFoods,
                        onClick = {
                            showAllFoods = false
                            activeMealSlot = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Group by meal")
                    }
                    SegmentedButton(
                        selected = showAllFoods,
                        onClick = {
                            showAllFoods = true
                            activeMealSlot = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("List all food")
                    }
                }

// --- content area (grid OR detail) fills all remaining height ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)          // ⭐ this whole area stretches from here down to the bottom
                ) {
                    if (showAllFoods) {
                        AllFoodsList(
                            foods = loggedFoods,
                            viewModel = viewModel,
                            showConsumedToggle = foodConsumedTrackingEnabled,
                            onFoodClick = { editingFood = it },
                            onDelete = {
                                if (it.isOneTime || it.repeatOnDays.isNullOrEmpty()) {
                                    viewModel.deleteLoggedFood(it)
                                } else {
                                    foodToDelete = it
                                    showDeleteDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (activeMealSlot == null) {

                        val mealSlots = listOf(
                            "Breakfast",
                            "Mid-Morning",
                            "Lunch",
                            "Afternoon Snack",
                            "Dinner",
                            "Evening Snack"
                        )

                        val config = LocalConfiguration.current
                        val isCompactHeight = config.screenHeightDp < 700
                        val verticalSpacing = if (isCompactHeight) 12.dp else 16.dp
                        val horizontalSpacing = if (isCompactHeight) 10.dp else 12.dp
                        val defaultSlotHeight = if (isCompactHeight) 64.dp else 72.dp
                        val minSlotHeight = 48.dp
                        val rows = (mealSlots.size + 1) / 2

                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val availableHeight = maxHeight -
                                (verticalSpacing * 2) -
                                (verticalSpacing * (rows - 1))
                            val maxSlotHeight = availableHeight / rows
                            val computedSlotHeight = minOf(defaultSlotHeight, maxSlotHeight)
                            val shouldScroll = computedSlotHeight < minSlotHeight
                            val slotHeight = if (shouldScroll) minSlotHeight else computedSlotHeight

                            val columnModifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalSpacing, vertical = verticalSpacing)
                                .then(
                                    if (shouldScroll) {
                                        Modifier.verticalScroll(rememberScrollState())
                                    } else {
                                        Modifier
                                    }
                                )

                            Column(
                                modifier = columnModifier,
                                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                mealSlots.chunked(2).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
                                    ) {
                                        row.forEach { meal ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(slotHeight)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = LocalIndication.current
                                                    ) {
                                                        activeMealSlot = meal
                                                    }
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    meal,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // --- meal slot detail fills full content area ---
                        MealSlotDetail(
                            slotName = activeMealSlot!!,
                            foods = loggedFoods.filter { it.mealSlot == activeMealSlot },
                            viewModel = viewModel,
                            showConsumedToggle = foodConsumedTrackingEnabled,
                            onFoodClick = { editingFood = it },
                            onDelete = {
                                if (it.isOneTime || it.repeatOnDays.isNullOrEmpty()) {
                                    viewModel.deleteLoggedFood(it)
                                } else {
                                    foodToDelete = it
                                    showDeleteDialog = true
                                }
                            },
                            onBackToGrid = { activeMealSlot = null },
                            modifier = Modifier.fillMaxSize()     // ⭐ use all Box space
                        )
                    }
                }

            }


        }
    }

}


@Composable
fun MealSlotDetail(
    slotName: String,
    foods: List<LoggedFood>,
    viewModel: CalorieTrackerViewModel,
    showConsumedToggle: Boolean,
    onFoodClick: (LoggedFood) -> Unit,
    onDelete: (LoggedFood) -> Unit,
    onBackToGrid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppPalette.SurfaceAlt)
            .padding(16.dp)
    )



    {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onBackToGrid() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppPalette.TextInverse
                )
            }

            Text(
                text = slotName,
                color = AppPalette.TextInverse,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }


        Spacer(Modifier.height(4.dp))


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // makes the scroll area take remaining space in the card
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            foods.forEach { food ->
                if (food.bundleId == null) {
                    // ----- NORMAL FOOD -----
                    FoodRowCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onFoodClick(food) }
                    ) {
                        if (!isNoEventFoodColor(food.colorArgb)) {
                            SeriesColorDot(
                                color = resolveEventFoodColor(food.colorArgb, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(food.foodProduct.name, color = AppPalette.TextInverse)
                            Text(
                                "${food.calories.toInt()} cal • ${food.portionSizeG.toInt()} g",
                                color = AppPalette.TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        if (showConsumedToggle) {
                            Checkbox(
                                checked = food.isConsumed,
                                onCheckedChange = { isChecked ->
                                    viewModel.setFoodConsumed(food, isChecked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.secondary,
                                    uncheckedColor = AppPalette.TextSecondary
                                )
                            )
                        }
                        IconButton(onClick = { onDelete(food) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AppPalette.Danger)
                        }
                    }
                } else {
                    // ----- RECIPE -----
                    ExpandableRecipeRow(
                        food = food,
                        viewModel = viewModel,
                        showConsumedToggle = showConsumedToggle,
                        onDeleteRecipe = { onDelete(food) }
                    )
                }
            }

        }

    }
}

@Composable
fun AllFoodsList(
    foods: List<LoggedFood>,
    viewModel: CalorieTrackerViewModel,
    showConsumedToggle: Boolean,
    onFoodClick: (LoggedFood) -> Unit,
    onDelete: (LoggedFood) -> Unit,
    modifier: Modifier = Modifier
) {
    val mealSlotOrder = listOf(
        "Breakfast",
        "Mid-Morning",
        "Lunch",
        "Afternoon Snack",
        "Dinner",
        "Evening Snack"
    )
    val groupedFoods = foods.groupBy { food ->
        val slot = food.mealSlot?.trim().orEmpty()
        if (slot.isBlank() || slot.equals("unspecified", ignoreCase = true)) {
            "Unassigned"
        } else {
            slot
        }
    }
    val orderedMealSlots = buildList {
        addAll(mealSlotOrder.filter { it in groupedFoods.keys })
        addAll(groupedFoods.keys.filter { it !in mealSlotOrder }.sorted())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppPalette.SurfaceAlt)
            .padding(16.dp)
    ) {
        if (foods.isEmpty()) {
            Text(
                text = "No foods logged for this day.",
                color = AppPalette.TextSecondary,
                fontSize = 13.sp
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            orderedMealSlots.forEachIndexed { index, mealSlot ->
                item {
                    Text(
                        text = mealSlot,
                        color = AppPalette.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(groupedFoods[mealSlot].orEmpty(), key = { it.id }) { food ->
                    if (food.bundleId == null) {
                        FoodRowCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onFoodClick(food) }
                        ) {
                            if (!isNoEventFoodColor(food.colorArgb)) {
                                SeriesColorDot(
                                    color = resolveEventFoodColor(food.colorArgb, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(food.foodProduct.name, color = AppPalette.TextInverse)
                                Text(
                                    "${food.calories.toInt()} cal • ${food.portionSizeG.toInt()} g",
                                    color = AppPalette.TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            if (showConsumedToggle) {
                                Checkbox(
                                    checked = food.isConsumed,
                                    onCheckedChange = { isChecked ->
                                        viewModel.setFoodConsumed(food, isChecked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.secondary,
                                        uncheckedColor = AppPalette.TextSecondary
                                    )
                                )
                            }
                            IconButton(onClick = { onDelete(food) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = AppPalette.Danger
                                )
                            }
                        }
                    } else {
                        ExpandableRecipeRow(
                            food = food,
                            viewModel = viewModel,
                            showConsumedToggle = showConsumedToggle,
                            onDeleteRecipe = { onDelete(food) }
                        )
                    }
                }
                if (index < orderedMealSlots.lastIndex) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FoodRowCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppPalette.Surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
@Composable
fun ExpandableRecipeRow(
    food: LoggedFood,
    viewModel: CalorieTrackerViewModel,
    showConsumedToggle: Boolean,
    onDeleteRecipe: () -> Unit
) {
    var showRecipeDialog by remember { mutableStateOf(false) }
    val bundleId = food.bundleId ?: return
    val bundleWithItems by produceState<com.example.routinereminder.data.entities.FoodBundleWithItems?>(
        initialValue = null,
        key1 = bundleId
    ) {
        value = viewModel.getBundleWithItems(bundleId)
    }
    val recipeName = bundleWithItems?.bundle?.name ?: food.foodProduct.name
    val recipeDescription = bundleWithItems?.bundle?.description.orEmpty()
    val recipeItems = bundleWithItems?.items.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppPalette.Surface)
            .clickable { showRecipeDialog = true }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isNoEventFoodColor(food.colorArgb)) {
                SeriesColorDot(
                    color = resolveEventFoodColor(food.colorArgb, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(recipeName, color = AppPalette.TextInverse, fontSize = 16.sp)
                Text(
                    "${food.calories.toInt()} cal • ${food.portionSizeG.toInt()} g",
                    color = AppPalette.TextMuted,
                    fontSize = 12.sp
                )
            }

            if (showConsumedToggle) {
                Checkbox(
                    checked = food.isConsumed,
                    onCheckedChange = { isChecked ->
                        viewModel.setFoodConsumed(food, isChecked)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.secondary,
                        uncheckedColor = AppPalette.TextSecondary
                    )
                )
            }
            IconButton(onClick = onDeleteRecipe) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = AppPalette.Danger)
            }
        }
    }

    if (showRecipeDialog) {
        Dialog(
            onDismissRequest = { showRecipeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppPalette.SurfaceDialog
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                recipeName,
                                color = AppPalette.TextInverse,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${food.calories.toInt()} cal • ${food.portionSizeG.toInt()} g",
                                color = AppPalette.TextMuted,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = { showRecipeDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = AppPalette.TextInverse)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (showConsumedToggle) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = food.isConsumed,
                                onCheckedChange = { isChecked ->
                                    viewModel.setFoodConsumed(food, isChecked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.secondary,
                                    uncheckedColor = AppPalette.TextSecondary
                                )
                            )
                            Text("Consumed", color = AppPalette.TextSecondary)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onDeleteRecipe) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = AppPalette.Danger)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (recipeDescription.isNotBlank()) {
                        Text(
                            formatChecklistText(recipeDescription),
                            color = AppPalette.TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    recipeItems.forEach { item ->
                        Text(
                            "• ${item.name} – ${item.portionSizeG.toInt()} g",
                            color = AppPalette.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NutrientRow(
    totals: CalorieTrackerViewModel.DailyTotals,
    targets: CalorieTrackerViewModel.DailyTargets
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NutrientProgress(
                name = "Carbs",
                value = totals.carbsG,
                target = targets.carbsG,
                modifier = Modifier.weight(1f)
            )
            NutrientProgress(
                name = "Protein",
                value = totals.proteinG,
                target = targets.proteinG,
                modifier = Modifier.weight(1f)
            )
            NutrientProgress(
                name = "Fat",
                value = totals.fatG,
                target = targets.fatG,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NutrientProgress(
                name = "Fiber",
                value = totals.fiberG,
                target = targets.fiberG,
                modifier = Modifier.weight(1f)
            )
            NutrientProgress(
                name = "Sugar",
                value = totals.addedSugarsG,
                target = targets.addedSugarsG,
                modifier = Modifier.weight(1f),
                lowerIsBetter = true
            )
            NutrientProgress(
                name = "Sodium",
                value = totals.sodiumMg,
                target = targets.sodiumMg,
                modifier = Modifier.weight(1f),
                lowerIsBetter = true
            )
        }
    }
}

@Composable
fun DateHeader(
    currentDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("E, d MMM")
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Day")
        }
        Text(
            currentDate.format(formatter),
            textAlign = TextAlign.Center,
            color = AppPalette.TextInverse
        )
        IconButton(onClick = onNextDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day")
        }
    }
}

@Composable
fun NutrientProgress(
    name: String,
    value: Double,
    target: Double,
    modifier: Modifier = Modifier,
    lowerIsBetter: Boolean = false
) {
    val progress = if (target > 0) (value / target).toFloat() else 0f
    val labelColor = MaterialTheme.colorScheme.onSurface
    val valueColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val color = when {
        lowerIsBetter -> if (progress > 1f) AppPalette.Danger else AppPalette.Success
        else -> if (progress <= 1f) AppPalette.Success else AppPalette.Danger
    }

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(name, color = labelColor)
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = trackColor
        )
        Text("${value.toInt()} / ${target.toInt()}", color = valueColor, fontSize = 11.sp)
    }
}


@Composable
fun LoggedFoodsList(
    loggedFoods: List<LoggedFood>,
    onDelete: (LoggedFood) -> Unit,
    onItemClick: (LoggedFood) -> Unit
) {
    LazyColumn {
        items(loggedFoods) { food ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(food) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isNoEventFoodColor(food.colorArgb)) {
                    SeriesColorDot(
                        color = resolveEventFoodColor(food.colorArgb, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(food.foodProduct.name, color = AppPalette.TextInverse)
                    Text(
                        "${food.calories.toInt()} cal, ${food.portionSizeG.toInt()}g",
                        color = AppPalette.TextMuted,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { onDelete(food) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AppPalette.Danger)
                }
            }
        }
    }
}

@Composable
fun CircularNutrientIndicator(
    label: String,
    currentValue: Double,
    targetValue: Double,
    modifier: Modifier = Modifier.size(100.dp)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val strokeWidth = 12.dp
        val sweepAngle = 315f
        val startAngle = -225f
        val progress = (currentValue / targetValue).coerceAtMost(1.5).toFloat()
        val indicatorTrackColor = MaterialTheme.colorScheme.surfaceVariant
        val indicatorProgressColor = MaterialTheme.colorScheme.secondary
        val primaryTextColor = MaterialTheme.colorScheme.onSurface
        val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = indicatorTrackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Butt)
            )
            drawArc(
                color = indicatorProgressColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle * progress,
                useCenter = false,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Butt)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(currentValue.toInt().toString(), color = primaryTextColor, fontSize = 26.sp)
            Text("/ ${targetValue.toInt()}", color = secondaryTextColor, fontSize = 12.sp)
            Text(label, color = primaryTextColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MissingProfileScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Please complete your profile in settings to use the calorie tracker.",
            color = AppPalette.TextInverse
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate("settings/calories") }) {
            Text("Go to Settings")
        }

    }
}
@Composable
fun FoodSearchDialog(
    searchResults: List<FoodProduct>,
    onDismiss: () -> Unit,
    onFoodSelected: (FoodProduct) -> Unit,
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AppPalette.TextPrimary,
        unfocusedTextColor = AppPalette.TextPrimary,
        disabledTextColor = AppPalette.TextMuted,
        focusedBorderColor = AppPalette.BorderStrong,
        unfocusedBorderColor = AppPalette.BorderSubtle,
        focusedLabelColor = AppPalette.TextSecondary,
        unfocusedLabelColor = AppPalette.TextSecondary,
        cursorColor = MaterialTheme.colorScheme.secondary
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppPalette.Surface
            )
            {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top bar: Title + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search Food",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                Spacer(Modifier.height(12.dp))

                // Search input
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Enter food name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(Modifier.height(10.dp))

                // Search button
                Button(
                    onClick = { onSearch(query.trim()) },
                    enabled = query.trim().length >= 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Search")
                }

                Spacer(Modifier.height(12.dp))

                if (searchResults.isEmpty() && query.length >= 3) {
                    Text(
                        text = "No search started or results found",
                        color = AppPalette.TextMuted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                // ✅ Results fill remaining space (no dead area)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(searchResults) { food ->
                        ProductResultCard(
                            title = food.name,
                            subtitle = null,
                            nutrition = NutritionPreview(
                                kcalPer100g = food.caloriesPer100g,
                                proteinPer100g = food.proteinPer100g,
                                carbsPer100g = food.carbsPer100g,
                                fatPer100g = food.fatPer100g,
                                fiberPer100g = food.fiberPer100g,
                                sugarPer100g = food.addedSugarsPer100g,
                                saltPer100g = food.sodiumPer100g
                            ),
                            onClick = { onFoodSelected(food) }
                        )
                    }
                }
            }
        }
    }
}
