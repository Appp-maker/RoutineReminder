package com.example.routinereminder.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.routinereminder.R
import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.data.exercisedb.ExerciseDbExercise
import com.example.routinereminder.data.workout.WorkoutPlan
import com.example.routinereminder.data.workout.WorkoutPlanExercise
import com.example.routinereminder.ui.components.EditItemDialog
import com.example.routinereminder.ui.components.SettingsIconButton
import kotlinx.coroutines.launch
import java.io.File
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    viewModel: WorkoutViewModel = hiltViewModel(),
    calorieTrackerViewModel: CalorieTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val defaultEventSettings by mainViewModel.defaultEventSettings.collectAsState()
    val useGoogleBackupMode by mainViewModel.useGoogleBackupMode.collectAsState()
    val eventSetNames by mainViewModel.eventSetNames.collectAsState()
    val enabledTabsState by mainViewModel.enabledTabs.collectAsState()
    val enabledTabs = enabledTabsState ?: AppTab.defaultTabs
    val caloriesEnabled = enabledTabs.contains(AppTab.Calories)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val selectPlanMessage = stringResource(R.string.workout_snackbar_select_plan)
    val exerciseListState = rememberLazyListState()

    var newPlanName by remember { mutableStateOf("") }
    var planMenuExpanded by remember { mutableStateOf(false) }
    var bodyPartMenuExpanded by remember { mutableStateOf(false) }
    var showNewPlanDialog by remember { mutableStateOf(false) }
    var showCustomExerciseDialog by remember { mutableStateOf(false) }
    var planToSchedule by remember { mutableStateOf<WorkoutPlan?>(null) }
    var planToDelete by remember { mutableStateOf<WorkoutPlan?>(null) }
    var previewExercise by remember { mutableStateOf<ExercisePreview?>(null) }
    var customExerciseName by remember { mutableStateOf("") }
    var customExerciseBodyPart by remember { mutableStateOf("") }
    var customExerciseTarget by remember { mutableStateOf("") }
    var customExerciseEquipment by remember { mutableStateOf("") }
    var customExerciseGifUrl by remember { mutableStateOf("") }
    var customExerciseVideoUrl by remember { mutableStateOf("") }
    var customExerciseInstructions by remember { mutableStateOf("") }
    var showCaloriesDialog by remember { mutableStateOf(false) }
    var workoutCaloriesInput by remember { mutableStateOf("") }

    val selectedPlan = uiState.plans.firstOrNull { it.id == uiState.selectedPlanId }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    if (planToDelete != null) {
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text(stringResource(R.string.workout_plan_delete_title)) },
            text = { Text(stringResource(R.string.workout_plan_delete_body, planToDelete?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    planToDelete?.let { viewModel.removePlan(it.id) }
                    planToDelete = null
                }) {
                    Text(stringResource(R.string.workout_plan_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text(stringResource(R.string.alert_action_cancel))
                }
            }
        )
    }
    if (showCaloriesDialog) {
        val caloriesValue = workoutCaloriesInput.toIntOrNull()
        val quickLogLabel = stringResource(R.string.calorie_tracker_quick_log_workout)
        AlertDialog(
            onDismissRequest = { showCaloriesDialog = false },
            title = { Text(text = stringResource(R.string.workout_calorie_adjustment_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.workout_calorie_adjustment_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextField(
                        value = workoutCaloriesInput,
                        onValueChange = { value ->
                            workoutCaloriesInput = value.filter { it.isDigit() }
                        },
                        label = { Text(stringResource(R.string.workout_calorie_adjustment_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.textFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = caloriesValue != null && caloriesValue > 0,
                    onClick = {
                        val calories = caloriesValue ?: return@TextButton
                        selectedPlan?.let { plan ->
                            if (plan.caloriesPerWorkout != calories) {
                                viewModel.updatePlanCalories(plan.id, calories)
                            }
                        }
                        calorieTrackerViewModel.logWorkoutCaloriesRequired(
                            calories = calories,
                            label = selectedPlan?.name ?: quickLogLabel
                        )
                        workoutCaloriesInput = ""
                        showCaloriesDialog = false
                    }
                ) {
                    Text(stringResource(R.string.workout_calorie_adjustment_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaloriesDialog = false }) {
                    Text(stringResource(R.string.alert_action_cancel))
                }
            }
        )
    }

    if (uiState.showRefreshPrompt) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRefreshPrompt,
            title = { Text(stringResource(R.string.workout_refresh_title)) },
            text = { Text(stringResource(R.string.workout_refresh_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::refreshExerciseDatabase) {
                    Text(stringResource(R.string.workout_refresh_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRefreshPrompt) {
                    Text(stringResource(R.string.workout_refresh_dismiss))
                }
            }
        )
    }

    if (uiState.showDownloadPrompt) {
        AlertDialog(
            onDismissRequest = viewModel::deferExerciseDbDownload,
            title = { Text(stringResource(R.string.workout_download_prompt_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.workout_download_prompt_body,
                        uiState.exerciseDbTotalDownloadSizeMb,
                        uiState.exerciseDbDownloadSizeMb,
                        uiState.exerciseDbGifDownloadSizeMb
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::acceptExerciseDbDownload) {
                    Text(stringResource(R.string.workout_download_prompt_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::deferExerciseDbDownload) {
                    Text(stringResource(R.string.workout_download_prompt_dismiss))
                }
            }
        )
    }

    if (uiState.showDownloadNotification && (uiState.isExerciseDbDownloading || uiState.isExerciseDbPaused)) {
        val totalCount = uiState.exerciseDbTotalCount
        val downloadedCount = uiState.exerciseDbDownloadedCount
        AlertDialog(
            onDismissRequest = viewModel::dismissDownloadNotification,
            title = { Text(stringResource(R.string.workout_download_notification_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            if (uiState.isExerciseDbPaused) {
                                R.string.workout_download_notification_paused_body
                            } else {
                                R.string.workout_download_notification_body
                            }
                        )
                    )
                    if (downloadedCount > 0) {
                        Text(
                            text = if (totalCount != null && totalCount > 0) {
                                stringResource(
                                    R.string.workout_download_progress_known,
                                    downloadedCount,
                                    totalCount
                                )
                            } else {
                                stringResource(
                                    R.string.workout_download_progress_unknown,
                                    downloadedCount
                                )
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (uiState.isExerciseDbPaused) {
                            viewModel.resumeExerciseDbDownload()
                        } else {
                            viewModel.pauseExerciseDbDownload()
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (uiState.isExerciseDbPaused) {
                                R.string.workout_download_resume_action
                            } else {
                                R.string.workout_download_pause_action
                            }
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDownloadNotification) {
                    Text(stringResource(R.string.workout_download_notification_dismiss))
                }
            }
        )
    }

    if (showNewPlanDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlanDialog = false },
            title = { Text(stringResource(R.string.workout_plan_new_title)) },
            text = {
                TextField(
                    value = newPlanName,
                    onValueChange = { newPlanName = it },
                    modifier = Modifier.heightIn(min = TextFieldDefaults.MinHeight),
                    label = { Text(stringResource(R.string.workout_plan_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val created = viewModel.createPlan(newPlanName)
                    if (created) {
                        newPlanName = ""
                        showNewPlanDialog = false
                    }
                }) {
                    Text(stringResource(R.string.workout_plan_create_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlanDialog = false }) {
                    Text(stringResource(R.string.alert_action_cancel))
                }
            }
        )
    }

    if (showCustomExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showCustomExerciseDialog = false },
            title = { Text(stringResource(R.string.workout_custom_exercise_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    TextField(
                        value = customExerciseName,
                        onValueChange = { customExerciseName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.workout_custom_exercise_name_label)) },
                        singleLine = true
                    )
                    TextField(
                        value = customExerciseBodyPart,
                        onValueChange = { customExerciseBodyPart = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.workout_custom_exercise_body_part_label)) },
                        singleLine = true
                    )
                    TextField(
                        value = customExerciseTarget,
                        onValueChange = { customExerciseTarget = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.workout_custom_exercise_target_label)) },
                        singleLine = true
                    )
                    TextField(
                        value = customExerciseEquipment,
                        onValueChange = { customExerciseEquipment = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.workout_custom_exercise_equipment_label)) },
                        singleLine = true
                    )
                    TextField(
                        value = customExerciseGifUrl,
                        onValueChange = { customExerciseGifUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.workout_custom_exercise_gif_label)) },
                        singleLine = true
                    )
                    TextField(
                        value = customExerciseVideoUrl,
                        onValueChange = { customExerciseVideoUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.workout_custom_exercise_video_label)) },
                        singleLine = true
                    )
                    TextField(
                        value = customExerciseInstructions,
                        onValueChange = { customExerciseInstructions = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.workout_custom_exercise_instructions_label)) }
                    )
                }
            },
            confirmButton = {
                val instructions = customExerciseInstructions
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                val selectedPlanId = selectedPlan?.id
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.addCustomExercise(
                            name = customExerciseName,
                            bodyPart = customExerciseBodyPart,
                            target = customExerciseTarget,
                            equipment = customExerciseEquipment,
                            gifUrl = customExerciseGifUrl,
                            videoUrl = customExerciseVideoUrl,
                            instructions = instructions,
                            addToPlanId = null
                        )
                        showCustomExerciseDialog = false
                    }) {
                        Text(stringResource(R.string.workout_custom_exercise_save_action))
                    }
                    if (selectedPlanId != null) {
                        TextButton(onClick = {
                            viewModel.addCustomExercise(
                                name = customExerciseName,
                                bodyPart = customExerciseBodyPart,
                                target = customExerciseTarget,
                                equipment = customExerciseEquipment,
                                gifUrl = customExerciseGifUrl,
                                videoUrl = customExerciseVideoUrl,
                                instructions = instructions,
                                addToPlanId = selectedPlanId
                            )
                            showCustomExerciseDialog = false
                        }) {
                            Text(stringResource(R.string.workout_custom_exercise_save_add_action))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomExerciseDialog = false }) {
                    Text(stringResource(R.string.alert_action_cancel))
                }
            }
        )
    }

    previewExercise?.let { preview ->
        ExercisePreviewDialog(
            preview = preview,
            onDismiss = { previewExercise = null }
        )
    }

    planToSchedule?.let { plan ->
        val defaultDurationMinutes = (defaultEventSettings.durationHours * 60) + defaultEventSettings.durationMinutes
        val notes = plan.exercises.takeIf { it.isNotEmpty() }?.joinToString("\n") { exercise ->
            val details = exerciseSettingsSummary(exercise)
            if (details.isNullOrBlank()) {
                "• ${exercise.name}"
            } else {
                "• ${exercise.name} ($details)"
            }
        }
        val draftItem = ScheduleItem(
            name = plan.name,
            notes = notes,
            hour = defaultEventSettings.hour,
            minute = defaultEventSettings.minute,
            durationMinutes = defaultDurationMinutes,
            isOneTime = defaultEventSettings.isOneTime,
            notifyEnabled = defaultEventSettings.systemNotification,
            showDetailsInNotification = defaultEventSettings.showDetailsInNotification,
            addToCalendarOnSave = defaultEventSettings.createCalendarEntry,
            targetCalendarSystem = defaultEventSettings.targetCalendarId
        )
        EditItemDialog(
            initialItem = draftItem,
            defaultEventSettings = defaultEventSettings,
            useGoogleBackupMode = useGoogleBackupMode,
            eventSetNames = eventSetNames,
            onDismissRequest = { planToSchedule = null },
            onSave = { item ->
                mainViewModel.upsertScheduleItem(item)
                planToSchedule = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            top = 0.dp,
            end = padding.calculateEndPadding(layoutDirection),
            bottom = padding.calculateBottomPadding()
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = exerciseListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp)
                    .padding(top = 0.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { planMenuExpanded = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(selectedPlan?.name ?: stringResource(R.string.workout_plan_select_placeholder))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Filled.ExpandMore, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = planMenuExpanded,
                            onDismissRequest = { planMenuExpanded = false }
                        ) {
                            if (uiState.plans.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workout_plan_empty_menu)) },
                                    onClick = { planMenuExpanded = false }
                                )
                            } else {
                                uiState.plans.forEach { plan ->
                                    DropdownMenuItem(
                                        text = { Text(plan.name) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                planMenuExpanded = false
                                                planToDelete = plan
                                            }) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.workout_plan_delete_action)
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectPlan(plan.id)
                                            planMenuExpanded = false
                                        }
                                    )
                                }
                            }
                            Divider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workout_plan_new_menu)) },
                                onClick = {
                                    planMenuExpanded = false
                                    newPlanName = ""
                                    showNewPlanDialog = true
                                }
                            )
                        }
                        SettingsIconButton(onClick = { navController.navigate("settings/workout") })
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.workout_plan_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selectedPlan == null) {
                        Text(
                            text = stringResource(R.string.workout_plan_select_prompt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        if (caloriesEnabled) {
                            TextField(
                                value = selectedPlan.caloriesPerWorkout?.toString().orEmpty(),
                                onValueChange = { value ->
                                    val cleaned = value.filter { it.isDigit() }
                                    val calories = cleaned.toIntOrNull()
                                    viewModel.updatePlanCalories(selectedPlan.id, calories)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = TextFieldDefaults.MinHeight),
                                label = { Text(stringResource(R.string.workout_plan_calories_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Text(
                                text = stringResource(R.string.workout_plan_calories_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (selectedPlan.exercises.isEmpty()) {
                            Text(
                                text = stringResource(R.string.workout_plan_empty_body),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                selectedPlan.exercises.forEach { exercise ->
                                    WorkoutPlanExerciseRow(
                                        exercise = exercise,
                                        onPreview = {
                                            previewExercise = ExercisePreview(
                                                title = exercise.name,
                                                subtitle = "${exercise.bodyPart} • ${exercise.target} • ${exercise.equipment}",
                                                gifFile = viewModel.getExerciseGifFile(exercise.id, exercise.gifUrl),
                                                gifUrl = exercise.gifUrl,
                                                imageUrls = exercise.imageUrls,
                                                videoUrl = exercise.videoUrl,
                                                instructions = exercise.instructions
                                            )
                                        },
                                        onRemove = { viewModel.removeExerciseFromPlan(selectedPlan.id, exercise.id) },
                                        autoEdit = uiState.autoEditExerciseId == exercise.id,
                                        onAutoEditConsumed = { viewModel.consumeAutoEditExercise(exercise.id) },
                                        onUpdate = { sets, repetitions, duration, rest, weight ->
                                            viewModel.updateExerciseSettings(
                                                planId = selectedPlan.id,
                                                exerciseId = exercise.id,
                                                sets = sets,
                                                repetitions = repetitions,
                                                durationMinutes = duration,
                                                restSeconds = rest,
                                                weight = weight
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { planToSchedule = selectedPlan },
                            enabled = selectedPlan != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.workout_plan_schedule_action))
                        }
                    }
                }
            }

            item {
                Divider()
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.workout_exercises_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!uiState.isExerciseDbReady) {
                        if (uiState.isExerciseDbPaused) {
                            Text(
                                text = stringResource(R.string.workout_download_paused_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.workout_download_notification_paused_body),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = viewModel::resumeExerciseDbDownload,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.workout_download_resume_action))
                            }
                        } else if (uiState.isExerciseDbDownloading) {
                            Text(
                                text = stringResource(R.string.workout_download_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.workout_download_body),
                                style = MaterialTheme.typography.bodySmall
                            )
                            val totalCount = uiState.exerciseDbTotalCount
                            val downloadedCount = uiState.exerciseDbDownloadedCount
                            if (totalCount != null && totalCount > 0) {
                                LinearProgressIndicator(
                                    progress = downloadedCount.toFloat() / totalCount.toFloat(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = stringResource(
                                        R.string.workout_download_progress_known,
                                        downloadedCount,
                                        totalCount
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(
                                    text = stringResource(
                                        R.string.workout_download_progress_unknown,
                                        downloadedCount
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.workout_download_pending_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.workout_download_prompt_body_short),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = viewModel::requestExerciseDbDownloadPrompt,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.workout_download_prompt_confirm))
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.workout_exercises_subtitle),
                            style = MaterialTheme.typography.bodySmall
                        )

                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = TextFieldDefaults.MinHeight),
                            label = { Text(stringResource(R.string.workout_search_label)) }
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { bodyPartMenuExpanded = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(uiState.selectedBodyPart ?: stringResource(R.string.workout_body_part_all))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Filled.ExpandMore, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = bodyPartMenuExpanded,
                                onDismissRequest = { bodyPartMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workout_body_part_all)) },
                                    onClick = {
                                        viewModel.selectBodyPart(null)
                                        bodyPartMenuExpanded = false
                                    }
                                )
                                uiState.bodyParts.forEach { bodyPart ->
                                    DropdownMenuItem(
                                        text = { Text(bodyPart) },
                                        onClick = {
                                            viewModel.selectBodyPart(bodyPart)
                                            bodyPartMenuExpanded = false
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                customExerciseName = ""
                                customExerciseBodyPart = uiState.selectedBodyPart.orEmpty()
                                customExerciseTarget = ""
                                customExerciseEquipment = ""
                                customExerciseGifUrl = ""
                                customExerciseVideoUrl = ""
                                customExerciseInstructions = ""
                                showCustomExerciseDialog = true
                            }) {
                                Text(stringResource(R.string.workout_custom_exercise_action))
                            }
                        }
                    }
                    if (uiState.isGifDownloading && uiState.gifTotalCount > 0) {
                        Text(
                            text = stringResource(R.string.workout_gif_download_title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = uiState.gifDownloadedCount.toFloat() / uiState.gifTotalCount.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(
                                R.string.workout_gif_download_progress,
                                uiState.gifDownloadedCount,
                                uiState.gifTotalCount
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (uiState.isExerciseDbReady) {
                if (uiState.isLoading) {
                    item {
                        Text(
                            text = stringResource(R.string.workout_loading_message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (uiState.exercises.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.workout_empty_results),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(uiState.exercises, key = { it.id }) { exercise ->
                        ExerciseSearchRow(
                            exercise = exercise,
                            onPreview = {
                                previewExercise = ExercisePreview(
                                    title = exercise.name,
                                    subtitle = "${exercise.bodyPart} • ${exercise.target} • ${exercise.equipment}",
                                    gifFile = viewModel.getExerciseGifFile(exercise.id, exercise.gifUrl),
                                    gifUrl = exercise.gifUrl,
                                    imageUrls = exercise.imageUrls,
                                    videoUrl = exercise.videoUrl,
                                    instructions = exercise.instructions
                                )
                            },
                            onAdd = {
                                val planId = selectedPlan?.id
                                if (planId == null) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message = selectPlanMessage)
                                    }
                                } else {
                                    viewModel.addExerciseToPlan(planId, exercise)
                                }
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.workout_exercisedb_attribution),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

        }
        if (uiState.isExerciseDbReady && uiState.exercises.isNotEmpty()) {
            val exerciseStartIndex = 4
            val letterIndexMap = remember(uiState.exercises) {
                buildMap {
                    uiState.exercises.forEachIndexed { index, exercise ->
                        val letter = exercise.name.firstOrNull()?.uppercaseChar() ?: return@forEachIndexed
                        if (!containsKey(letter)) {
                            put(letter, index)
                        }
                    }
                }
            }
            val currentLetter by remember(uiState.exercises, exerciseListState) {
                derivedStateOf {
                    val firstExerciseIndex = exerciseListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index >= exerciseStartIndex }
                        ?.index
                    val exerciseIndex = firstExerciseIndex?.minus(exerciseStartIndex) ?: return@derivedStateOf null
                    uiState.exercises.getOrNull(exerciseIndex)
                        ?.name
                        ?.firstOrNull()
                        ?.uppercaseChar()
                }
            }
            val alphabet = remember { ('A'..'Z').toList() }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentLetter != null) {
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = currentLetter.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    alphabet.forEach { letter ->
                        val enabled = letterIndexMap.containsKey(letter)
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier
                                .clickable(enabled = enabled) {
                                    letterIndexMap[letter]?.let { targetIndex ->
                                        coroutineScope.launch {
                                            exerciseListState.animateScrollToItem(
                                                index = exerciseStartIndex + targetIndex
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutPlanExerciseRow(
    exercise: WorkoutPlanExercise,
    onPreview: () -> Unit,
    onRemove: () -> Unit,
    autoEdit: Boolean,
    onAutoEditConsumed: () -> Unit,
    onUpdate: (Int?, Int?, Int?, Int?, Double?) -> Unit
) {
    var isEditing by remember(exercise.id) { mutableStateOf(false) }
    val summary = exerciseSettingsSummary(exercise)
    LaunchedEffect(autoEdit) {
        if (autoEdit) {
            isEditing = true
            onAutoEditConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPreview() }
            ) {
                Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${exercise.bodyPart} • ${exercise.target} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall
                )
                ExerciseGifUrlText(gifUrl = exercise.gifUrl)
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.workout_plan_remove_exercise))
            }
            IconButton(onClick = { isEditing = !isEditing }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.workout_plan_edit_exercise))
            }
        }
        if (isEditing) {
            val hasReps = (exercise.repetitions ?: 0) > 0
            val hasDuration = (exercise.durationMinutes ?: 0) > 0
            val setsCount = exercise.sets ?: 0
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = exercise.sets?.toString().orEmpty(),
                    onValueChange = { value ->
                        val cleaned = value.filter { it.isDigit() }
                        val parsed = cleaned.toIntOrNull()
                        onUpdate(parsed, exercise.repetitions, exercise.durationMinutes, exercise.restSeconds, exercise.weight)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = TextFieldDefaults.MinHeight),
                    label = {
                        Text(
                            text = stringResource(R.string.workout_exercise_sets_label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = exercise.repetitions?.toString().orEmpty(),
                    onValueChange = { value ->
                        val cleaned = value.filter { it.isDigit() }
                        val parsed = cleaned.toIntOrNull()
                        onUpdate(exercise.sets, parsed, exercise.durationMinutes, exercise.restSeconds, exercise.weight)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = TextFieldDefaults.MinHeight),
                    enabled = !hasDuration,
                    label = {
                        Text(
                            text = stringResource(R.string.workout_exercise_repetitions_label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = exercise.durationMinutes?.toString().orEmpty(),
                    onValueChange = { value ->
                        val cleaned = value.filter { it.isDigit() }
                        val parsed = cleaned.toIntOrNull()
                        onUpdate(exercise.sets, exercise.repetitions, parsed, exercise.restSeconds, exercise.weight)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = TextFieldDefaults.MinHeight),
                    enabled = !hasReps,
                    label = {
                        Text(
                            text = stringResource(R.string.workout_exercise_duration_label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = exercise.restSeconds?.toString().orEmpty(),
                    onValueChange = { value ->
                        val cleaned = value.filter { it.isDigit() }
                        val parsed = cleaned.toIntOrNull()
                        onUpdate(exercise.sets, exercise.repetitions, exercise.durationMinutes, parsed, exercise.weight)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = TextFieldDefaults.MinHeight),
                    enabled = setsCount > 1,
                    label = { Text(stringResource(R.string.workout_exercise_rest_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextField(
                value = exercise.weight?.toString().orEmpty(),
                onValueChange = { value ->
                    val cleaned = value.filterIndexed { index, char ->
                        char.isDigit() || (char == '.' && !value.take(index).contains('.'))
                    }
                    val parsed = cleaned.toDoubleOrNull()
                    onUpdate(exercise.sets, exercise.repetitions, exercise.durationMinutes, exercise.restSeconds, parsed)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TextFieldDefaults.MinHeight),
                label = { Text(stringResource(R.string.workout_exercise_weight_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

private fun exerciseSettingsSummary(exercise: WorkoutPlanExercise): String? {
    val setsCount = exercise.sets ?: 0
    val segments = buildList {
        exercise.repetitions?.let { add("$it Reps") }
        if (setsCount > 0) {
            add("$setsCount Sets")
        }
        exercise.weight?.takeIf { it > 0 }?.let { add("${formatWeight(it)} kg") }
        exercise.durationMinutes?.let { add("Duration $it seconds") }
        if (setsCount > 1) {
            exercise.restSeconds?.let { add("Rest between sets $it seconds") }
        }
    }
    return segments.takeIf { it.isNotEmpty() }?.joinToString(" - ")
}

private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        weight.toString()
    }
}

@Composable
private fun ExerciseSearchRow(
    exercise: ExerciseDbExercise,
    onPreview: () -> Unit,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPreview() }
            ) {
                Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${exercise.bodyPart} • ${exercise.target} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall
                )
                ExerciseGifUrlText(gifUrl = exercise.gifUrl)
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.workout_exercise_add))
            }
        }
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

private data class ExercisePreview(
    val title: String,
    val subtitle: String,
    val gifFile: File?,
    val gifUrl: String?,
    val imageUrls: List<String>,
    val videoUrl: String?,
    val instructions: List<String>
)

@Composable
private fun ExercisePreviewDialog(
    preview: ExercisePreview,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allImageUrls = remember(preview.gifUrl, preview.imageUrls) {
        preview.imageUrls.takeIf { it.isNotEmpty() } ?: deriveFallbackImageUrls(preview.gifUrl)
    }
    val model = preview.gifFile ?: preview.gifUrl ?: allImageUrls.firstOrNull()
    val supplementalImageUrls = remember(model, allImageUrls) {
        allImageUrls.filterNot { it == model }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(preview.title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(text = preview.subtitle, style = MaterialTheme.typography.bodySmall)
                if (model != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(model)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.workout_exercise_gif_preview),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Fit
                    )
                    supplementalImageUrls.forEach { imageUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.workout_exercise_gif_preview),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.workout_exercise_gif_unavailable),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (preview.instructions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.workout_exercise_instructions_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    preview.instructions.forEach { instruction ->
                        Text(text = instruction, style = MaterialTheme.typography.bodySmall)
                    }
                }
                preview.videoUrl?.let { videoUrl ->
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.workout_exercise_video_link))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.alert_action_ok))
            }
        }
    )
}

@Composable
private fun ExerciseGifUrlText(gifUrl: String?) {
    val text = if (!gifUrl.isNullOrBlank()) {
        stringResource(R.string.workout_exercise_gif_url, gifUrl)
    } else {
        stringResource(R.string.workout_exercise_gif_url_unavailable)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun deriveFallbackImageUrls(gifUrl: String?): List<String> {
    val url = gifUrl ?: return emptyList()
    return when {
        url.endsWith("/0.jpg") -> listOf(url, url.replace("/0.jpg", "/1.jpg"))
        url.endsWith("/1.jpg") -> listOf(url.replace("/1.jpg", "/0.jpg"), url)
        url.endsWith("/0.png") -> listOf(url, url.replace("/0.png", "/1.png"))
        url.endsWith("/1.png") -> listOf(url.replace("/1.png", "/0.png"), url)
        else -> emptyList()
    }
}
