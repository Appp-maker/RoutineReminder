package com.example.routinereminder.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.R
import com.example.routinereminder.ui.components.SeriesColorOptions
import com.example.routinereminder.data.*
import com.example.routinereminder.ui.components.SeriesColorPicker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import java.time.DayOfWeek
import java.time.format.TextStyle

fun censorEmailForDisplay(email: String?): String? {
    if (email.isNullOrBlank()) return null

    val parts = email.split("@")
    if (parts.size != 2) return email   // fallback if it's not a real email

    val name = parts[0]
    val domain = parts[1]

    val censoredName = when {
        name.length >= 2 -> name.take(2) + "***"
        name.isNotEmpty() -> name.first() + "***"
        else -> "***"
    }

    return "$censoredName@$domain"
}


enum class StartTimeOption {
    CURRENT_TIME,
    SPECIFIC_TIME,
    NEXT_FULL_HOUR;

    companion object {
        fun fromName(name: String?): StartTimeOption {
            return entries.find { it.name == name } ?: NEXT_FULL_HOUR
        }
    }
}

enum class DriveBackupAction {
    BACKUP,
    RESTORE
}

enum class SettingsCategory {
    PROFILE,
    SYNC,
    DEFAULT_EVENTS,
    EVENT_SETS,
    MAP,
    APP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    from: String,
    initialCategory: SettingsCategory? = null,
    onDismiss: () -> Unit
)
 {
     val context = LocalContext.current
     val coroutineScope = rememberCoroutineScope()
     val enabledTabsState by viewModel.enabledTabs.collectAsState()
     val enabledTabs = enabledTabsState ?: AppTab.defaultTabs
     val routineEnabled = enabledTabs.contains(AppTab.Routine)
     val caloriesEnabled = enabledTabs.contains(AppTab.Calories)
     val fallbackCategory = when (from) {
         "routine" -> if (routineEnabled) SettingsCategory.DEFAULT_EVENTS else SettingsCategory.SYNC
         "calories" -> if (caloriesEnabled) SettingsCategory.PROFILE else SettingsCategory.SYNC
         "map" -> SettingsCategory.MAP
         else -> SettingsCategory.SYNC
     }

     var selectedCategory by remember(from, enabledTabs, initialCategory) {
         mutableStateOf(initialCategory ?: fallbackCategory)
     }


    val userSettings by viewModel.userSettings.collectAsState()
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var customCaloriesInput by remember { mutableStateOf("") }
    var customProteinInput by remember { mutableStateOf("") }
    var customCarbsInput by remember { mutableStateOf("") }
    var customFatInput by remember { mutableStateOf("") }
    var customFiberInput by remember { mutableStateOf("") }
    var customSaturatedFatInput by remember { mutableStateOf("") }
    var customAddedSugarsInput by remember { mutableStateOf("") }
    var customSodiumInput by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var selectedActivityLevel by remember { mutableStateOf(ActivityLevel.SEDENTARY) }
    var selectedCalorieGoal by remember { mutableStateOf(CalorieGoal.MAINTAIN) }


    val currentSyncInterval by viewModel.syncIntervalMinutes.collectAsState()
    val currentDefaultEventSettings by viewModel.defaultEventSettings.collectAsState()
    val googleCalendars by viewModel.googleCalendars.collectAsState()
    val currentOnCalendarDeleteAction by viewModel.onCalendarDeleteAction.collectAsState()
    val currentImportTargetCalendarId by viewModel.importTargetCalendarId.collectAsState()
    val currentOnAppDeleteImportedAction by viewModel.onAppDeleteImportedAction.collectAsState()
    val currentUseGoogleBackupMode by viewModel.useGoogleBackupMode.collectAsState()
    val currentImportTargetCalendarIdForBothMode by viewModel.importTargetCalendarIdForBothMode.collectAsState()
    val currentCalendarSyncAppToCalendarEnabled by viewModel.calendarSyncAppToCalendarEnabled.collectAsState()
    val currentCalendarSyncCalendarToAppEnabled by viewModel.calendarSyncCalendarToAppEnabled.collectAsState()
    val mapTrackingMode by viewModel.mapTrackingMode.collectAsState()
    val currentFoodConsumedTrackingEnabled by viewModel.foodConsumedTrackingEnabled.collectAsState()
    val currentRoutineInsightsEnabled by viewModel.routineInsightsEnabled.collectAsState()
    val eventSetNames by viewModel.eventSetNames.collectAsState()
    val eventSetColors by viewModel.eventSetColors.collectAsState()
    val recentCustomEventColors by viewModel.recentCustomEventColors.collectAsState()
    val appThemeColors by viewModel.appThemeColors.collectAsState()
    val eventIndicatorDisplayCondition by viewModel.eventIndicatorDisplayCondition.collectAsState()
    val eventBackgroundDisplayCondition by viewModel.eventBackgroundDisplayCondition.collectAsState()
    val eventTitleColorChoice by viewModel.eventTitleColorChoice.collectAsState()
    val eventTitleCustomColor by viewModel.eventTitleCustomColor.collectAsState()
    val defaultActiveSetsByWeekday by viewModel.defaultActiveSetsByWeekday.collectAsState()
    //val blockedCalendarImports by viewModel.blockedCalendarImportsForDisplay.collectAsState(initial = emptyList())

    val selectedGoogleAccountName by viewModel.selectedGoogleAccountName.collectAsState()
    val driveBackupUri by viewModel.driveBackupUri.collectAsState()

     val showAllEvents by viewModel.showAllEvents.collectAsState()
    val calendarEventCounts by viewModel.calendarEventCounts.collectAsState()
    var selectedTabs by remember(enabledTabs) { mutableStateOf(enabledTabs) }


    var syncHoursInputText by remember(currentSyncInterval) { mutableStateOf((currentSyncInterval / 60).toString()) }
    var syncMinutesInputText by remember(currentSyncInterval) { mutableStateOf((currentSyncInterval % 60).toString()) }
    var defaultEventHourState by remember { mutableStateOf(currentDefaultEventSettings.hour) }
    var defaultEventMinuteState by remember { mutableStateOf(currentDefaultEventSettings.minute) }
    var startTimeOption by remember { mutableStateOf(StartTimeOption.fromName(currentDefaultEventSettings.startTimeOptionName)) }
    var defaultDurationHoursText by remember { mutableStateOf(currentDefaultEventSettings.durationHours.toString()) }
    var defaultDurationMinutesText by remember { mutableStateOf(currentDefaultEventSettings.durationMinutes.toString()) }
    var isOneTimeChecked by remember { mutableStateOf(currentDefaultEventSettings.isOneTime) }
    var createCalendarEntryChecked by remember { mutableStateOf(currentDefaultEventSettings.createCalendarEntry) }
    var selectedCalendarIdForNewEvents by remember { mutableStateOf(currentDefaultEventSettings.targetCalendarId) }
    var systemNotificationChecked by remember { mutableStateOf(currentDefaultEventSettings.systemNotification) }
    var showDetailsInNotificationChecked by remember { mutableStateOf(currentDefaultEventSettings.showDetailsInNotification) }
    var selectedOnCalendarDeleteAction by remember(currentOnCalendarDeleteAction) { mutableStateOf(currentOnCalendarDeleteAction) }
    var selectedImportTargetCalendarId by remember(currentImportTargetCalendarId) { mutableStateOf(currentImportTargetCalendarId) }
    var selectedOnAppDeleteImportedAction by remember(currentOnAppDeleteImportedAction) { mutableStateOf(currentOnAppDeleteImportedAction) }
    var useGoogleBackupModeChecked by remember(currentUseGoogleBackupMode) { mutableStateOf(currentUseGoogleBackupMode) }
    var selectedImportTargetCalendarIdForBothMode by remember(currentImportTargetCalendarIdForBothMode) { mutableStateOf(currentImportTargetCalendarIdForBothMode) }
    var showAllEventsChecked by remember(showAllEvents) { mutableStateOf(showAllEvents) }
    var calendarSyncAppToCalendarEnabled by remember(currentCalendarSyncAppToCalendarEnabled) { mutableStateOf(currentCalendarSyncAppToCalendarEnabled) }
    var calendarSyncCalendarToAppEnabled by remember(currentCalendarSyncCalendarToAppEnabled) { mutableStateOf(currentCalendarSyncCalendarToAppEnabled) }
    var foodConsumedTrackingEnabledChecked by remember(currentFoodConsumedTrackingEnabled) { mutableStateOf(currentFoodConsumedTrackingEnabled) }
    var routineInsightsEnabledChecked by remember(currentRoutineInsightsEnabled) { mutableStateOf(currentRoutineInsightsEnabled) }
    val eventSetNameInputs = remember { mutableStateListOf<String>() }
    val eventSetColorInputs = remember { mutableStateListOf<Int>() }
    var primaryColorInput by remember { mutableStateOf(appThemeColors.primary) }
    var secondaryColorInput by remember { mutableStateOf(appThemeColors.secondary) }
    var defaultActiveSetSelections by remember { mutableStateOf<Map<DayOfWeek, Set<Int>>>(emptyMap()) }
    var indicatorDisplayConditionState by remember(eventIndicatorDisplayCondition) { mutableStateOf(eventIndicatorDisplayCondition) }
    var backgroundDisplayConditionState by remember(eventBackgroundDisplayCondition) { mutableStateOf(eventBackgroundDisplayCondition) }
    var eventTitleColorChoiceState by remember(eventTitleColorChoice) { mutableStateOf(eventTitleColorChoice) }
    var eventTitleCustomColorState by remember(eventTitleCustomColor) { mutableStateOf(eventTitleCustomColor) }


    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var justSavedSuccessfully by remember { mutableStateOf(false) }
    var showGoogleCalendarDialog by remember { mutableStateOf(false) }
    var calendarSelectionPurpose by remember { mutableStateOf<String?>(null) }
    var showBlockedImportsDialog by remember { mutableStateOf(false) }
    var showDataManagementDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var accountNameToManage by remember { mutableStateOf<String?>(null) }
    var showDrivePermissionDialog by remember { mutableStateOf(false) }
    var pendingDriveAction by remember { mutableStateOf<DriveBackupAction?>(null) }

    val localBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val success = viewModel.exportDataToUri(context, uri)
                val message = if (success) {
                    context.getString(R.string.settings_backup_export_success)
                } else {
                    context.getString(R.string.settings_backup_export_failure)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

    val localRestoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val success = viewModel.importDataFromUri(context, uri)
                val message = if (success) {
                    context.getString(R.string.settings_backup_import_success)
                } else {
                    context.getString(R.string.settings_backup_import_failure)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

    val driveBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.updateDriveBackupUri(uri.toString())
            coroutineScope.launch {
                val success = viewModel.backupToDrive(context, uri)
                val message = if (success) {
                    context.getString(R.string.settings_drive_backup_success)
                } else {
                    context.getString(R.string.settings_drive_backup_failure)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

    val driveRestoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.updateDriveBackupUri(uri.toString())
            coroutineScope.launch {
                val success = viewModel.restoreFromDrive(context, uri)
                val message = if (success) {
                    context.getString(R.string.settings_drive_restore_success)
                } else {
                    context.getString(R.string.settings_drive_restore_failure)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }


    LaunchedEffect(userSettings) {
        userSettings?.let {
            weightInput = it.weightKg.toString()
            heightInput = it.heightCm.toString()
            ageInput = it.age.toString()
            customCaloriesInput = it.customCaloriesTarget.takeIf { value -> value > 0.0 }?.toString() ?: ""
            customProteinInput = it.customProteinTargetG.takeIf { value -> value > 0.0 }?.toString() ?: ""
            customCarbsInput = it.customCarbsTargetG.takeIf { value -> value > 0.0 }?.toString() ?: ""
            customFatInput = it.customFatTargetG.takeIf { value -> value > 0.0 }?.toString() ?: ""
            customFiberInput = it.customFiberTargetG.takeIf { value -> value > 0.0 }?.toString() ?: ""
            customSaturatedFatInput = it.customSaturatedFatTargetG.takeIf { value -> value > 0.0 }?.toString() ?: ""
            customAddedSugarsInput = it.customAddedSugarsTargetG.takeIf { value -> value > 0.0 }?.toString() ?: ""
            customSodiumInput = it.customSodiumTargetMg.takeIf { value -> value > 0.0 }?.toString() ?: ""
            selectedGender = it.gender
            selectedActivityLevel = it.activityLevel
            selectedCalorieGoal = it.calorieGoal
        }
    }

    LaunchedEffect(
        currentDefaultEventSettings,
        currentOnCalendarDeleteAction,
        currentImportTargetCalendarId,
        currentOnAppDeleteImportedAction,
        currentUseGoogleBackupMode,
        currentImportTargetCalendarIdForBothMode,
        showAllEvents,
        currentCalendarSyncAppToCalendarEnabled,
        currentCalendarSyncCalendarToAppEnabled
    ) {
        defaultEventHourState = currentDefaultEventSettings.hour
        defaultEventMinuteState = currentDefaultEventSettings.minute
        startTimeOption = StartTimeOption.fromName(currentDefaultEventSettings.startTimeOptionName)
        defaultDurationHoursText = currentDefaultEventSettings.durationHours.toString()
        defaultDurationMinutesText = currentDefaultEventSettings.durationMinutes.toString()
        isOneTimeChecked = currentDefaultEventSettings.isOneTime
        createCalendarEntryChecked = currentDefaultEventSettings.createCalendarEntry
        selectedCalendarIdForNewEvents = currentDefaultEventSettings.targetCalendarId
        systemNotificationChecked = currentDefaultEventSettings.systemNotification
        showDetailsInNotificationChecked = if (currentDefaultEventSettings.systemNotification) currentDefaultEventSettings.showDetailsInNotification else false
        selectedOnCalendarDeleteAction = currentOnCalendarDeleteAction
        selectedImportTargetCalendarId = currentImportTargetCalendarId
        selectedOnAppDeleteImportedAction = currentOnAppDeleteImportedAction
        useGoogleBackupModeChecked = currentUseGoogleBackupMode
        selectedImportTargetCalendarIdForBothMode = currentImportTargetCalendarIdForBothMode
        showAllEventsChecked = showAllEvents
        calendarSyncAppToCalendarEnabled = currentCalendarSyncAppToCalendarEnabled
        calendarSyncCalendarToAppEnabled = currentCalendarSyncCalendarToAppEnabled
        justSavedSuccessfully = false
    }

    LaunchedEffect(eventSetNames) {
        eventSetNameInputs.clear()
        eventSetNameInputs.addAll(eventSetNames)
    }

    LaunchedEffect(eventSetColors) {
        eventSetColorInputs.clear()
        eventSetColorInputs.addAll(eventSetColors)
    }

    LaunchedEffect(appThemeColors) {
        primaryColorInput = appThemeColors.primary
        secondaryColorInput = appThemeColors.secondary
    }

    LaunchedEffect(defaultActiveSetsByWeekday) {
        defaultActiveSetSelections = defaultActiveSetsByWeekday
    }

    LaunchedEffect(Unit) {
        viewModel.showGoogleCalendarChooser.receiveAsFlow().collect {
            if (useGoogleBackupModeChecked && calendarSelectionPurpose != null) {
                Toast.makeText(context, context.getString(R.string.settings_toast_gcalendar_disabled_backup_mode), Toast.LENGTH_LONG).show()
                return@collect
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                showGoogleCalendarDialog = true
            } else {
                Toast.makeText(context, context.getString(R.string.settings_toast_calendar_permission_needed_select), Toast.LENGTH_LONG).show()
            }
        }
    }

    /*LaunchedEffect(showBlockedImportsDialog) {
        if (showBlockedImportsDialog) {
            viewModel.refreshBlockedCalendarImports()
        }
    }*/

    // Add this new LaunchedEffect
    LaunchedEffect(selectedGoogleAccountName, currentUseGoogleBackupMode) {
        if (currentUseGoogleBackupMode) { // If backup mode is ON
            // When selectedGoogleAccountName changes (or currentUseGoogleBackupMode changes while it's ON),
            // ensure local UI states that depend on backup mode are consistent.

            // If backup mode is on, any calendar selection that targets a Google Calendar
            // should revert to a local/default equivalent for UI consistency,
            // as Google Calendar interactions are disabled.
            if (selectedImportTargetCalendarId.startsWith("google") ||
                selectedImportTargetCalendarId == SettingsRepository.IMPORT_TARGET_CALENDAR_BOTH
            ) {
                selectedImportTargetCalendarId = SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL
            }
            if (selectedCalendarIdForNewEvents.startsWith("google")) {
                selectedCalendarIdForNewEvents = SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL
            }
            // For 'import from both', if backup mode is on, it should reset to its own default.
            if (selectedImportTargetCalendarIdForBothMode != SettingsRepository.DEFAULT_IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE) {
                selectedImportTargetCalendarIdForBothMode = SettingsRepository.DEFAULT_IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE
            }
            // createCalendarEntryChecked is handled by the 'enabled' state of its switch and save logic.
            // No need to force it false here, to preserve user's preference if backup mode is toggled off.
        }
    }

    val hasUnsavedChanges by remember(
        weightInput, heightInput, ageInput, customCaloriesInput, customProteinInput, customCarbsInput, customFatInput, customFiberInput,
        customSaturatedFatInput, customAddedSugarsInput, customSodiumInput, selectedGender, selectedActivityLevel, selectedCalorieGoal, userSettings,
        syncHoursInputText, syncMinutesInputText, currentSyncInterval,
        defaultEventHourState, defaultEventMinuteState, startTimeOption,
        defaultDurationHoursText, defaultDurationMinutesText,
        isOneTimeChecked, createCalendarEntryChecked, selectedCalendarIdForNewEvents,
        systemNotificationChecked, showDetailsInNotificationChecked,
        selectedOnCalendarDeleteAction, currentOnCalendarDeleteAction,
        selectedImportTargetCalendarId, currentImportTargetCalendarId,
        selectedOnAppDeleteImportedAction, currentOnAppDeleteImportedAction,
        useGoogleBackupModeChecked, currentUseGoogleBackupMode,
        selectedImportTargetCalendarIdForBothMode, currentImportTargetCalendarIdForBothMode,
        calendarSyncAppToCalendarEnabled, currentCalendarSyncAppToCalendarEnabled,
        calendarSyncCalendarToAppEnabled, currentCalendarSyncCalendarToAppEnabled,
        currentDefaultEventSettings,
        selectedGoogleAccountName, // Added to dependency list
        showAllEventsChecked, showAllEvents,
        selectedTabs, enabledTabs,
        routineInsightsEnabledChecked, currentRoutineInsightsEnabled
    ) {
        derivedStateOf {
            if (weightInput.toDoubleOrNull() != userSettings?.weightKg) return@derivedStateOf true
            if (heightInput.toDoubleOrNull() != userSettings?.heightCm) return@derivedStateOf true
            if (ageInput.toIntOrNull() != userSettings?.age) return@derivedStateOf true
            if ((customCaloriesInput.toDoubleOrNull() ?: 0.0) != userSettings?.customCaloriesTarget) return@derivedStateOf true
            if ((customProteinInput.toDoubleOrNull() ?: 0.0) != userSettings?.customProteinTargetG) return@derivedStateOf true
            if ((customCarbsInput.toDoubleOrNull() ?: 0.0) != userSettings?.customCarbsTargetG) return@derivedStateOf true
            if ((customFatInput.toDoubleOrNull() ?: 0.0) != userSettings?.customFatTargetG) return@derivedStateOf true
            if ((customFiberInput.toDoubleOrNull() ?: 0.0) != userSettings?.customFiberTargetG) return@derivedStateOf true
            if ((customSaturatedFatInput.toDoubleOrNull() ?: 0.0) != userSettings?.customSaturatedFatTargetG) return@derivedStateOf true
            if ((customAddedSugarsInput.toDoubleOrNull() ?: 0.0) != userSettings?.customAddedSugarsTargetG) return@derivedStateOf true
            if ((customSodiumInput.toDoubleOrNull() ?: 0.0) != userSettings?.customSodiumTargetMg) return@derivedStateOf true
            if (selectedGender != userSettings?.gender) return@derivedStateOf true
            if (selectedActivityLevel != userSettings?.activityLevel) return@derivedStateOf true
            if (selectedCalorieGoal != userSettings?.calorieGoal) return@derivedStateOf true
            val syncH = syncHoursInputText.toLongOrNull() ?: 0L
            val syncM = syncMinutesInputText.toLongOrNull() ?: 0L
            if ((syncH * 60 + syncM) != currentSyncInterval) return@derivedStateOf true
            if (startTimeOption.name != currentDefaultEventSettings.startTimeOptionName) return@derivedStateOf true
            if (startTimeOption == StartTimeOption.SPECIFIC_TIME && (defaultEventHourState != currentDefaultEventSettings.hour || defaultEventMinuteState != currentDefaultEventSettings.minute)) return@derivedStateOf true
            if ((defaultDurationHoursText.toIntOrNull() ?: currentDefaultEventSettings.durationHours) != currentDefaultEventSettings.durationHours) return@derivedStateOf true
            if ((defaultDurationMinutesText.toIntOrNull() ?: currentDefaultEventSettings.durationMinutes) != currentDefaultEventSettings.durationMinutes) return@derivedStateOf true
            if (isOneTimeChecked != currentDefaultEventSettings.isOneTime) return@derivedStateOf true
            if (createCalendarEntryChecked != currentDefaultEventSettings.createCalendarEntry) return@derivedStateOf true
            // Logic for selectedCalendarIdForNewEvents might need adjustment if it auto-changes with Google Account selection
            if (selectedCalendarIdForNewEvents != currentDefaultEventSettings.targetCalendarId) return@derivedStateOf true
            if (systemNotificationChecked != currentDefaultEventSettings.systemNotification) return@derivedStateOf true
            if ((if (systemNotificationChecked) showDetailsInNotificationChecked else false) != currentDefaultEventSettings.showDetailsInNotification) return@derivedStateOf true
            if (selectedOnCalendarDeleteAction != currentOnCalendarDeleteAction) return@derivedStateOf true
            // Logic for selectedImportTargetCalendarId might need adjustment
            if (selectedImportTargetCalendarId != currentImportTargetCalendarId) return@derivedStateOf true
            if (selectedOnAppDeleteImportedAction != currentOnAppDeleteImportedAction) return@derivedStateOf true
            if (useGoogleBackupModeChecked != currentUseGoogleBackupMode) return@derivedStateOf true
            // Logic for selectedImportTargetCalendarIdForBothMode might need adjustment
            if (selectedImportTargetCalendarIdForBothMode != currentImportTargetCalendarIdForBothMode) return@derivedStateOf true
            if (calendarSyncAppToCalendarEnabled != currentCalendarSyncAppToCalendarEnabled) return@derivedStateOf true
            if (calendarSyncCalendarToAppEnabled != currentCalendarSyncCalendarToAppEnabled) return@derivedStateOf true
            if (showAllEventsChecked != showAllEvents) return@derivedStateOf true
            if (foodConsumedTrackingEnabledChecked != currentFoodConsumedTrackingEnabled) return@derivedStateOf true
            if (routineInsightsEnabledChecked != currentRoutineInsightsEnabled) return@derivedStateOf true
            if (selectedTabs != enabledTabs) return@derivedStateOf true
            // We don't directly compare selectedGoogleAccountName to a stored value for unsaved changes,
            // as its change directly triggers other settings to change, which are covered above.
            false
        }
    }

    fun attemptDismiss() {
        if (justSavedSuccessfully) onDismiss()
        else if (hasUnsavedChanges) showUnsavedChangesDialog = true
        else onDismiss()
    }

    BackHandler(enabled = !showUnsavedChangesDialog && !showGoogleCalendarDialog && !showBlockedImportsDialog) { attemptDismiss() }

    if (showUnsavedChangesDialog) {
        UnsavedChangesAlertDialog(
            onDismiss = { showUnsavedChangesDialog = false },
            onDiscard = { showUnsavedChangesDialog = false; onDismiss() }
        )
    }

    val timePickerDialog = TimePickerDialog(context, { _, hour, min ->
        defaultEventHourState = hour; defaultEventMinuteState = min
        startTimeOption = StartTimeOption.SPECIFIC_TIME; justSavedSuccessfully = false
    }, defaultEventHourState, defaultEventMinuteState, true)

    val filteredGoogleCalendarsForDialog by remember(
        googleCalendars,
        calendarSelectionPurpose,
        selectedImportTargetCalendarIdForBothMode,
        selectedGoogleAccountName // Added dependency
    ) {
        derivedStateOf {
            val accountToFilterBy = selectedGoogleAccountName
            if (accountToFilterBy != null) {
                googleCalendars.filter { it.accountName == accountToFilterBy }
            } else {
                googleCalendars // Show all if no account selected, or original behavior
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = { IconButton(onClick = ::attemptDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_action_back)) } },
                actions = {
                    Button(onClick = {
                        val newSettings = UserSettings(
                            weightKg = weightInput.toDoubleOrNull() ?: 0.0,
                            heightCm = heightInput.toDoubleOrNull() ?: 0.0,
                            age = ageInput.toIntOrNull() ?: 0,
                            gender = selectedGender,
                            activityLevel = selectedActivityLevel,
                            customCaloriesTarget = customCaloriesInput.toDoubleOrNull() ?: 0.0,
                            customProteinTargetG = customProteinInput.toDoubleOrNull() ?: 0.0,
                            customCarbsTargetG = customCarbsInput.toDoubleOrNull() ?: 0.0,
                            customFatTargetG = customFatInput.toDoubleOrNull() ?: 0.0,
                            customFiberTargetG = customFiberInput.toDoubleOrNull() ?: 0.0,
                            customSaturatedFatTargetG = customSaturatedFatInput.toDoubleOrNull() ?: 0.0,
                            customAddedSugarsTargetG = customAddedSugarsInput.toDoubleOrNull() ?: 0.0,
                            customSodiumTargetMg = customSodiumInput.toDoubleOrNull() ?: 0.0,
                            calorieGoal = selectedCalorieGoal
                        )
                        viewModel.saveUserSettings(newSettings)


                        val syncH = syncHoursInputText.toLongOrNull() ?: 0L
                        val syncM = syncMinutesInputText.toLongOrNull() ?: 0L
                        val totalSyncMins = syncH * 60 + syncM
                        var allSavesOk = true

                        if (totalSyncMins in 1..(24 * 60 * 7)) viewModel.updateSyncInterval(totalSyncMins)
                        else {
                            allSavesOk = false; Toast.makeText(context, context.getString(R.string.settings_toast_invalid_sync_interval), Toast.LENGTH_LONG).show()
                        }

                        var savedHour = defaultEventHourState
                        var savedMinute = defaultEventMinuteState
                        if (startTimeOption != StartTimeOption.SPECIFIC_TIME) {
                            val cal = Calendar.getInstance()
                            if (startTimeOption == StartTimeOption.CURRENT_TIME) {
                                savedHour = cal.get(Calendar.HOUR_OF_DAY); savedMinute = cal.get(Calendar.MINUTE)
                            } else {
                                var currentHour = cal.get(Calendar.HOUR_OF_DAY); if (cal.get(Calendar.MINUTE) > 0) currentHour = (currentHour + 1) % 24; savedHour = currentHour; savedMinute = 0
                            }
                        }
                        // ViewModel now handles defaulting to IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY if account is selected
                        val currentTargetCalId = if (selectedGoogleAccountName != null && selectedCalendarIdForNewEvents.startsWith("google")) SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY else selectedCalendarIdForNewEvents

                        val newDefaults = DefaultEventSettings(
                            hour = savedHour, minute = savedMinute,
                            durationHours = defaultDurationHoursText.toIntOrNull() ?: SettingsRepository.DEFAULT_EVENT_DURATION_HOURS,
                            durationMinutes = defaultDurationMinutesText.toIntOrNull() ?: SettingsRepository.DEFAULT_EVENT_DURATION_MINUTES,
                            isOneTime = isOneTimeChecked,
                            createCalendarEntry = if (useGoogleBackupModeChecked) false else createCalendarEntryChecked,
                            systemNotification = systemNotificationChecked,
                            showDetailsInNotification = if (systemNotificationChecked) showDetailsInNotificationChecked else false,
                            startTimeOptionName = startTimeOption.name,
                            targetCalendarId = if (useGoogleBackupModeChecked && currentTargetCalId.startsWith("google")) SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL else currentTargetCalId
                        )
                        viewModel.saveDefaultEventSettings(newDefaults)
                        viewModel.updateOnCalendarDeleteAction(selectedOnCalendarDeleteAction)

                        // ViewModel now handles defaulting to IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY
                        val currentImportTarget = if (selectedGoogleAccountName != null && selectedImportTargetCalendarId.startsWith("google")) SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY else selectedImportTargetCalendarId
                        val currentImportTargetBoth = if (selectedGoogleAccountName != null && selectedImportTargetCalendarIdForBothMode.startsWith("google")) SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY else selectedImportTargetCalendarIdForBothMode

                        viewModel.updateImportTargetCalendarId(if (useGoogleBackupModeChecked) SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL else currentImportTarget)
                        viewModel.updateOnAppDeleteImportedAction(selectedOnAppDeleteImportedAction)
                        viewModel.updateUseGoogleBackupMode(useGoogleBackupModeChecked)
                        viewModel.updateImportTargetCalendarIdForBothMode(if (useGoogleBackupModeChecked) SettingsRepository.DEFAULT_IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE else currentImportTargetBoth)
                        viewModel.updateCalendarSyncAppToCalendar(calendarSyncAppToCalendarEnabled)
                        viewModel.updateCalendarSyncCalendarToApp(calendarSyncCalendarToAppEnabled)
                        viewModel.updateShowAllEvents(showAllEventsChecked)
                        viewModel.saveFoodConsumedTrackingEnabled(foodConsumedTrackingEnabledChecked)
                        viewModel.updateRoutineInsightsEnabled(routineInsightsEnabledChecked)
                        viewModel.saveEventSetNames(eventSetNameInputs.toList())
                        viewModel.saveEventSetColors(eventSetColorInputs.toList())
                        viewModel.saveAppThemeColors(
                            AppThemeColors(
                                primary = primaryColorInput,
                                secondary = secondaryColorInput,
                                tertiary = secondaryColorInput
                            )
                        )
                        viewModel.saveEventIndicatorDisplayCondition(indicatorDisplayConditionState)
                        viewModel.saveEventBackgroundDisplayCondition(backgroundDisplayConditionState)
                        viewModel.saveEventTitleColorChoice(eventTitleColorChoiceState)
                        viewModel.saveEventTitleCustomColor(eventTitleCustomColorState)
                        viewModel.saveDefaultActiveSetsByWeekday(defaultActiveSetSelections)
                        if (selectedTabs.isNotEmpty()) {
                            viewModel.saveEnabledTabs(selectedTabs)
                        } else {
                            allSavesOk = false
                            Toast.makeText(context, context.getString(R.string.settings_tabs_select_at_least_one), Toast.LENGTH_SHORT).show()
                        }


                        if (allSavesOk) {
                            justSavedSuccessfully = true
                            if (startTimeOption != StartTimeOption.SPECIFIC_TIME) {
                                defaultEventHourState = savedHour; defaultEventMinuteState = savedMinute
                            }
                            Toast.makeText(context, context.getString(R.string.settings_toast_settings_saved), Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.settings_action_save)) }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val includeMapTab = from == "map"
                val allowedCategories = when (from) {
                    "routine" -> listOfNotNull(
                        if (routineEnabled) SettingsCategory.DEFAULT_EVENTS else null,
                        if (routineEnabled) SettingsCategory.EVENT_SETS else null,
                        SettingsCategory.SYNC,
                        if (includeMapTab) SettingsCategory.MAP else null,
                        SettingsCategory.APP
                    )
                    "calories" -> listOfNotNull(
                        if (caloriesEnabled) SettingsCategory.PROFILE else null,
                        SettingsCategory.SYNC,
                        if (includeMapTab) SettingsCategory.MAP else null,
                        SettingsCategory.APP
                    )
                    "map" -> listOf(
                        SettingsCategory.SYNC,
                        SettingsCategory.MAP,
                        SettingsCategory.APP
                    )
                    else -> listOf(
                        SettingsCategory.SYNC,
                        SettingsCategory.APP
                    )
                }

// ⚠ Ensure invalid category gets corrected:
                LaunchedEffect(from, enabledTabs) {
                    if (!allowedCategories.contains(selectedCategory)) {
                        selectedCategory = allowedCategories.first()
                    }
                }

// ─────────────────────────────────────────
// REAL TAB ROW (only one row!)
// ─────────────────────────────────────────
                val selectedIndex = allowedCategories.indexOf(selectedCategory).coerceAtLeast(0)
                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    allowedCategories.forEach { category ->
                        val text = when (category) {
                            SettingsCategory.PROFILE -> "Profile"
                            SettingsCategory.DEFAULT_EVENTS -> stringResource(R.string.settings_category_default_events)
                            SettingsCategory.SYNC -> stringResource(R.string.settings_category_sync)
                            SettingsCategory.MAP -> stringResource(R.string.settings_category_map)
                            SettingsCategory.APP -> stringResource(R.string.settings_category_app)
                            SettingsCategory.EVENT_SETS -> stringResource(R.string.settings_event_sets_title)
                        }

                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            selectedContentColor = MaterialTheme.colorScheme.secondary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(
                                    text = text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }




            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                when (selectedCategory) {
                    SettingsCategory.PROFILE -> ProfileSettingsSection(
                        weight = weightInput,
                        onWeightChange = { weightInput = it },
                        height = heightInput,
                        onHeightChange = { heightInput = it },
                        age = ageInput,
                        onAgeChange = { ageInput = it },
                        customCalories = customCaloriesInput,
                        onCustomCaloriesChange = { customCaloriesInput = it },
                        customProtein = customProteinInput,
                        onCustomProteinChange = { customProteinInput = it },
                        customCarbs = customCarbsInput,
                        onCustomCarbsChange = { customCarbsInput = it },
                        customFat = customFatInput,
                        onCustomFatChange = { customFatInput = it },
                        customFiber = customFiberInput,
                        onCustomFiberChange = { customFiberInput = it },
                        customSaturatedFat = customSaturatedFatInput,
                        onCustomSaturatedFatChange = { customSaturatedFatInput = it },
                        customAddedSugars = customAddedSugarsInput,
                        onCustomAddedSugarsChange = { customAddedSugarsInput = it },
                        customSodium = customSodiumInput,
                        onCustomSodiumChange = { customSodiumInput = it },
                        gender = selectedGender,
                        onGenderChange = { selectedGender = it },
                        activityLevel = selectedActivityLevel,
                        onActivityLevelChange = { selectedActivityLevel = it },
                        calorieGoal = selectedCalorieGoal,
                        onCalorieGoalChange = { selectedCalorieGoal = it },
                        foodConsumedTrackingEnabled = foodConsumedTrackingEnabledChecked,
                        onFoodConsumedTrackingEnabledChange = { foodConsumedTrackingEnabledChecked = it; justSavedSuccessfully = false }
                    )
                    SettingsCategory.DEFAULT_EVENTS -> DefaultEventsSettingsSection(
                        startTimeOption = startTimeOption,
                        onStartTimeOptionChange = { startTimeOption = it; justSavedSuccessfully = false },
                        defaultEventHourState = defaultEventHourState,
                        defaultEventMinuteState = defaultEventMinuteState,
                        onSpecificTimeClick = { timePickerDialog.show() },
                        defaultDurationHoursText = defaultDurationHoursText,
                        onDefaultDurationHoursChange = { defaultDurationHoursText = it.filter(Char::isDigit); justSavedSuccessfully = false },
                        defaultDurationMinutesText = defaultDurationMinutesText,
                        onDefaultDurationMinutesChange = { defaultDurationMinutesText = it.filter(Char::isDigit); justSavedSuccessfully = false },
                        isOneTimeChecked = isOneTimeChecked,
                        onIsOneTimeChange = { isOneTimeChecked = it; justSavedSuccessfully = false },
                        createCalendarEntryChecked = createCalendarEntryChecked,
                        onCreateCalendarEntryChange = { createCalendarEntryChecked = it; justSavedSuccessfully = false },
                        useGoogleBackupModeChecked = useGoogleBackupModeChecked,
                        selectedCalendarIdForNewEvents = selectedCalendarIdForNewEvents,
                        onSelectedCalendarIdForNewEventsChange = { selectedCalendarIdForNewEvents = it },
                        onSelectGoogleCalendarForNewEvents = {
                            calendarSelectionPurpose = "app_event"
                            viewModel.onSelectGoogleCalendarOption()
                            justSavedSuccessfully = false
                        },
                        googleCalendars = googleCalendars,
                        systemNotificationChecked = systemNotificationChecked,
                        onSystemNotificationChange = { newValue -> systemNotificationChecked = newValue; if (!newValue) showDetailsInNotificationChecked = false; justSavedSuccessfully = false },
                        showDetailsInNotificationChecked = showDetailsInNotificationChecked,
                        onShowDetailsInNotificationChange = { showDetailsInNotificationChecked = it; justSavedSuccessfully = false },
                        selectedGoogleAccountName = selectedGoogleAccountName,
                        eventsEnabled = selectedTabs.contains(AppTab.Routine),
                        onEventsEnabledChange = { enabled ->
                            val updatedTabs = if (enabled) {
                                selectedTabs + AppTab.Routine
                            } else {
                                selectedTabs - AppTab.Routine
                            }
                            if (updatedTabs.isNotEmpty()) {
                                selectedTabs = updatedTabs
                                justSavedSuccessfully = false
                            } else {
                                Toast.makeText(context, context.getString(R.string.settings_tabs_select_at_least_one), Toast.LENGTH_SHORT).show()
                            }
                        },
                        eventIndicatorDisplayCondition = indicatorDisplayConditionState,
                        onEventIndicatorDisplayConditionChange = { condition ->
                            indicatorDisplayConditionState = condition
                            justSavedSuccessfully = false
                        },
                        eventBackgroundDisplayCondition = backgroundDisplayConditionState,
                        onEventBackgroundDisplayConditionChange = { condition ->
                            backgroundDisplayConditionState = condition
                            justSavedSuccessfully = false
                        },
                        eventTitleColorChoice = eventTitleColorChoiceState,
                        onEventTitleColorChoiceChange = { choice ->
                            eventTitleColorChoiceState = choice
                            justSavedSuccessfully = false
                        },
                        eventTitleCustomColor = eventTitleCustomColorState,
                        onEventTitleCustomColorChange = { color ->
                            eventTitleCustomColorState = color
                            justSavedSuccessfully = false
                        },
                        recentCustomEventColors = recentCustomEventColors
                    )
                    SettingsCategory.EVENT_SETS -> EventSetsSettingsSection(
                        eventSetNames = eventSetNameInputs,
                        eventSetColors = eventSetColorInputs,
                        defaultActiveSetsByWeekday = defaultActiveSetSelections,
                        onResetManualActiveSets = {
                            viewModel.resetManualActiveEventSets()
                            justSavedSuccessfully = false
                        },
                        onEventSetNameChange = { index, name ->
                            if (index in eventSetNameInputs.indices) {
                                eventSetNameInputs[index] = name
                                justSavedSuccessfully = false
                            }
                        },
                        onEventSetColorChange = { index, color ->
                            if (index in eventSetColorInputs.indices) {
                                eventSetColorInputs[index] = color
                                justSavedSuccessfully = false
                            }
                        },
                        onDefaultActiveSetsChange = { day, selections ->
                            defaultActiveSetSelections = defaultActiveSetSelections.toMutableMap().apply {
                                put(day, selections)
                            }
                            justSavedSuccessfully = false
                        }
                    )
                    SettingsCategory.SYNC -> {
                        DataSyncSettingsSection(
                            useGoogleBackupModeChecked = useGoogleBackupModeChecked,
                            onUseGoogleBackupModeChange = { newBackupModeState ->
                                useGoogleBackupModeChecked = newBackupModeState
                                if (newBackupModeState) {
                                    if (selectedImportTargetCalendarId.startsWith("google") ||
                                        selectedImportTargetCalendarId == SettingsRepository.IMPORT_TARGET_CALENDAR_BOTH
                                    ) {
                                        selectedImportTargetCalendarId = SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL
                                    }
                                    if (selectedCalendarIdForNewEvents.startsWith("google")) selectedCalendarIdForNewEvents = SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL
                                    selectedImportTargetCalendarIdForBothMode = SettingsRepository.DEFAULT_IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE
                                }
                                justSavedSuccessfully = false
                            },
                            selectedGoogleAccountName = selectedGoogleAccountName,
                            onSelectGoogleAccount = {
                                viewModel.viewModelScope.launch {
                                    viewModel.triggerGoogleAccountSelectionFlow.send(Unit)
                                }
                            }
                        )
                        BackupRestoreSection(
                            driveBackupUri = driveBackupUri,
                            onLocalBackup = { localBackupLauncher.launch("routine_backup.json") },
                            onLocalRestore = { localRestoreLauncher.launch(arrayOf("application/json")) },
                            onDriveBackup = {
                                pendingDriveAction = DriveBackupAction.BACKUP
                                showDrivePermissionDialog = true
                            },
                            onDriveRestore = {
                                pendingDriveAction = DriveBackupAction.RESTORE
                                showDrivePermissionDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CalendarSyncSettingsSection(
                            currentSyncInterval = currentSyncInterval,
                            syncHoursInputText = syncHoursInputText,
                            onSyncHoursChange = { syncHoursInputText = it.filter(Char::isDigit); justSavedSuccessfully = false },
                            syncMinutesInputText = syncMinutesInputText,
                            onSyncMinutesChange = { syncMinutesInputText = it.filter(Char::isDigit); justSavedSuccessfully = false },
                            useGoogleBackupModeChecked = useGoogleBackupModeChecked,
                            selectedImportTargetCalendarId = selectedImportTargetCalendarId,
                            onImportTargetCalendarIdChange = { selectedImportTargetCalendarId = it; justSavedSuccessfully = false },
                            onSelectGoogleCalendarForImport = {
                                calendarSelectionPurpose = "import_event"
                                viewModel.onSelectGoogleCalendarOption()
                                justSavedSuccessfully = false
                            },
                            selectedImportTargetCalendarIdForBothMode = selectedImportTargetCalendarIdForBothMode,
                            onSelectGoogleCalendarForBothModeImport = {
                                calendarSelectionPurpose = "import_event_both_mode"
                                viewModel.onSelectGoogleCalendarOption()
                                justSavedSuccessfully = false
                            },
                            calendarSyncAppToCalendarEnabled = calendarSyncAppToCalendarEnabled,
                            onCalendarSyncAppToCalendarEnabledChange = { calendarSyncAppToCalendarEnabled = it; justSavedSuccessfully = false },
                            calendarSyncCalendarToAppEnabled = calendarSyncCalendarToAppEnabled,
                            onCalendarSyncCalendarToAppEnabledChange = { calendarSyncCalendarToAppEnabled = it; justSavedSuccessfully = false },
                            selectedOnAppDeleteImportedAction = selectedOnAppDeleteImportedAction,
                            onOnAppDeleteImportedActionChange = { selectedOnAppDeleteImportedAction = it; justSavedSuccessfully = false },
                            onManageBlockedImportsClick = { showBlockedImportsDialog = true },
                            selectedOnCalendarDeleteAction = selectedOnCalendarDeleteAction,
                            onOnCalendarDeleteActionChange = { selectedOnCalendarDeleteAction = it; justSavedSuccessfully = false },
                            selectedGoogleAccountName = selectedGoogleAccountName,
                            showAllEvents = showAllEventsChecked,
                            onShowAllEventsChange = { showAllEventsChecked = it; justSavedSuccessfully = false }
                        )
                    }
                    SettingsCategory.MAP -> MapSettingsSection(
                        trackingMode = TrackingMode.fromValue(mapTrackingMode),
                        onTrackingModeChange = { mode ->
                            viewModel.saveMapTrackingMode(mode.value)
                            justSavedSuccessfully = false
                        }
                    )
                    SettingsCategory.APP -> AppSettingsSection(
                        context = context,
                        selectedTabs = selectedTabs,
                        onTabSelectionChange = { updatedTabs ->
                            if (updatedTabs.isNotEmpty()) {
                                selectedTabs = updatedTabs
                                justSavedSuccessfully = false
                            } else {
                                Toast.makeText(context, context.getString(R.string.settings_tabs_select_at_least_one), Toast.LENGTH_SHORT).show()
                            }
                        },
                        selectedGoogleAccountName = selectedGoogleAccountName,
                        onDataManagementClick = { source, account ->
                            accountNameToManage = account
                            showDataManagementDialog = source
                        },
                        calendarEventCounts = calendarEventCounts,
                        primaryColor = primaryColorInput,
                        secondaryColor = secondaryColorInput,
                        onPrimaryColorChange = { primaryColorInput = it; justSavedSuccessfully = false },
                        onSecondaryColorChange = { secondaryColorInput = it; justSavedSuccessfully = false },
                        routineInsightsEnabled = routineInsightsEnabledChecked,
                        onRoutineInsightsEnabledChange = { routineInsightsEnabledChecked = it; justSavedSuccessfully = false },
                        onBackupClick = { selectedCategory = SettingsCategory.SYNC }
                    )
                }
            }
        }

    if (showDrivePermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showDrivePermissionDialog = false
                pendingDriveAction = null
            },
            title = { Text(stringResource(R.string.settings_drive_permission_title)) },
            text = { Text(stringResource(R.string.settings_drive_permission_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val action = pendingDriveAction
                    showDrivePermissionDialog = false
                    pendingDriveAction = null
                    if (action == null) return@TextButton
                    val existingUri = driveBackupUri?.let(Uri::parse)
                    when (action) {
                        DriveBackupAction.BACKUP -> {
                            if (existingUri != null) {
                                coroutineScope.launch {
                                    val success = viewModel.backupToDrive(context, existingUri)
                                    val message = if (success) {
                                        context.getString(R.string.settings_drive_backup_success)
                                    } else {
                                        context.getString(R.string.settings_drive_backup_failure)
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                driveBackupLauncher.launch("routine_backup.json")
                            }
                        }
                        DriveBackupAction.RESTORE -> {
                            if (existingUri != null) {
                                coroutineScope.launch {
                                    val success = viewModel.restoreFromDrive(context, existingUri)
                                    val message = if (success) {
                                        context.getString(R.string.settings_drive_restore_success)
                                    } else {
                                        context.getString(R.string.settings_drive_restore_failure)
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                driveRestoreLauncher.launch(arrayOf("application/json"))
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.alert_action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDrivePermissionDialog = false
                    pendingDriveAction = null
                }) {
                    Text(stringResource(R.string.alert_action_cancel))
                }
            }
        )
    }

    if (showGoogleCalendarDialog && !useGoogleBackupModeChecked) {
        val calendarsToShow = filteredGoogleCalendarsForDialog // Use the state that filters by account
        if (calendarsToShow.isEmpty() && calendarSelectionPurpose != null && selectedGoogleAccountName != null) {
            // If an account is selected but its calendar list is empty (e.g. no primary or specific calendars after filtering)
            Toast.makeText(context, context.getString(R.string.settings_toast_no_gcalendars_found) + " for selected account.", Toast.LENGTH_LONG).show()
            showGoogleCalendarDialog = false
        } else if (calendarsToShow.isEmpty() && calendarSelectionPurpose != null && selectedGoogleAccountName == null) {
            Toast.makeText(context, context.getString(R.string.settings_toast_no_gcalendars_found) + ". Try selecting an account first or check permissions.", Toast.LENGTH_LONG).show()
            showGoogleCalendarDialog = false
        } else {
            GoogleCalendarDialog(
                googleCalendars = calendarsToShow,
                onDismiss = { showGoogleCalendarDialog = false; calendarSelectionPurpose = null },
                onCalendarSelected = { calendarIdToSet ->
                    if (calendarSelectionPurpose == "app_event") {
                        selectedCalendarIdForNewEvents = calendarIdToSet
                    } else if (calendarSelectionPurpose == "import_event") {
                        // Logic simplified: if a google account is selected, this effectively becomes 'selected account's primary'
                        // The actual ID might be 'google_primary' or a specific ID, VM handles resolution.
                        selectedImportTargetCalendarId = calendarIdToSet
                    } else if (calendarSelectionPurpose == "import_event_both_mode") {
                        selectedImportTargetCalendarIdForBothMode = calendarIdToSet
                    }
                    showGoogleCalendarDialog = false
                    justSavedSuccessfully = false
                    calendarSelectionPurpose = null
                },
                selectedGoogleAccountName = selectedGoogleAccountName // Pass to Dialog
            )
        }
    }

    /*if (showBlockedImportsDialog) {
        BlockedImportsDialog(
            blockedCalendarImports = blockedCalendarImports,
            onUnblockEvent = {
                viewModel.unblockCalendarEvent(it)
                Toast.makeText(context, context.getString(R.string.settings_toast_event_unblocked), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBlockedImportsDialog = false }
        )
    }*/

    if (showDataManagementDialog != null) {
        val source = showDataManagementDialog!!
        val count = if (source == "GOOGLE") calendarEventCounts.google[accountNameToManage] ?: 0 else calendarEventCounts.local
        AlertDialog(
            onDismissRequest = { showDataManagementDialog = null },
            title = { Text("Manage $source Data") },
            text = {
                Column {
                    Text("There are $count events from this source.")
                    Text("What would you like to do?")
                }
            },
            confirmButton = {
                Column {
                    Button(onClick = {
                        viewModel.unlinkCalendar(source, accountNameToManage)
                        showDataManagementDialog = null
                    }) {
                        Text("Unlink All Events")
                    }
                    Button(onClick = {
                        showDataManagementDialog = null
                        showDeleteConfirmationDialog = true
                    }) {
                        Text("Delete All Events")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataManagementDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        val source = showDataManagementDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("This will permanently delete all events from the $source calendar. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.unlinkCalendar(source, accountNameToManage)
                    showDeleteConfirmationDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileSettingsSection(
    weight: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    customCalories: String,
    onCustomCaloriesChange: (String) -> Unit,
    customProtein: String,
    onCustomProteinChange: (String) -> Unit,
    customCarbs: String,
    onCustomCarbsChange: (String) -> Unit,
    customFat: String,
    onCustomFatChange: (String) -> Unit,
    customFiber: String,
    onCustomFiberChange: (String) -> Unit,
    customSaturatedFat: String,
    onCustomSaturatedFatChange: (String) -> Unit,
    customAddedSugars: String,
    onCustomAddedSugarsChange: (String) -> Unit,
    customSodium: String,
    onCustomSodiumChange: (String) -> Unit,
    gender: Gender,
    onGenderChange: (Gender) -> Unit,
    activityLevel: ActivityLevel,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    calorieGoal: CalorieGoal,
    onCalorieGoalChange: (CalorieGoal) -> Unit,
    foodConsumedTrackingEnabled: Boolean,
    onFoodConsumedTrackingEnabledChange: (Boolean) -> Unit
) {
    val hasAllOverrides = listOf(
        customCalories,
        customProtein,
        customCarbs,
        customFat,
        customFiber,
        customSaturatedFat,
        customAddedSugars,
        customSodium
    ).all { value -> value.toDoubleOrNull()?.let { it > 0 } == true }
    val calculatedCalories = calculateCalorieTargetEstimate(
        weightKg = weight.toDoubleOrNull(),
        heightCm = height.toDoubleOrNull(),
        age = age.toIntOrNull(),
        gender = gender,
        activityLevel = activityLevel,
        calorieGoal = calorieGoal
    )
    val customCaloriesValue = customCalories.toDoubleOrNull()?.takeIf { it > 0 }
    val hasAnyOverrides = listOf(
        customCalories,
        customProtein,
        customCarbs,
        customFat,
        customFiber,
        customSaturatedFat,
        customAddedSugars,
        customSodium
    ).any { value -> value.toDoubleOrNull()?.let { it > 0 } == true }
    var showOverridesInfoDialog by remember { mutableStateOf(false) }
    var showOverridesSheet by remember { mutableStateOf(false) }
    val overridesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Column {
        Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp))
        OutlinedTextField(
            value = weight,
            onValueChange = onWeightChange,
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !hasAllOverrides
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = height,
            onValueChange = onHeightChange,
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !hasAllOverrides
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !hasAllOverrides
        )
        Spacer(modifier = Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text("Daily target overrides") },
            supportingContent = {
                Text("Replace calculated calorie and macro targets with custom values.")
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showOverridesInfoDialog = true }
        )
        if (hasAnyOverrides) {
            Text(
                "Custom targets are active.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (showOverridesInfoDialog) {
            AlertDialog(
                onDismissRequest = { showOverridesInfoDialog = false },
                title = { Text("Daily target overrides") },
                text = {
                    Text(
                        "Overrides replace your calculated calorie and macro targets with " +
                            "the custom values you enter. Use this when you want fixed targets " +
                            "instead of profile-based calculations."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showOverridesInfoDialog = false
                        showOverridesSheet = true
                    }) {
                        Text("Open overrides")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverridesInfoDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        if (showOverridesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showOverridesSheet = false },
                sheetState = overridesSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Daily calorie target overview", style = MaterialTheme.typography.titleMedium)
                    when {
                        customCaloriesValue != null && calculatedCalories != null -> {
                            Text(
                                text = "Calculated target: ${calculatedCalories.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Overwritten target: ${customCaloriesValue.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        customCaloriesValue != null -> {
                            Text(
                                text = "Overwritten target: ${customCaloriesValue.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        calculatedCalories != null -> {
                            Text(
                                text = "Calculated target: ${calculatedCalories.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        else -> {
                            Text(
                                text = "Complete your profile to see the calculated target.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customCalories,
                        onValueChange = onCustomCaloriesChange,
                        label = { Text("Overwrite daily calories (kcal)") },
                        supportingText = { Text("Leave blank to keep calculated calories.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customProtein,
                        onValueChange = onCustomProteinChange,
                        label = { Text("Overwrite protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customCarbs,
                        onValueChange = onCustomCarbsChange,
                        label = { Text("Overwrite carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customFat,
                        onValueChange = onCustomFatChange,
                        label = { Text("Overwrite fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customFiber,
                        onValueChange = onCustomFiberChange,
                        label = { Text("Overwrite fiber (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customSaturatedFat,
                        onValueChange = onCustomSaturatedFatChange,
                        label = { Text("Overwrite saturated fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAddedSugars,
                        onValueChange = onCustomAddedSugarsChange,
                        label = { Text("Overwrite added sugars (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customSodium,
                        onValueChange = onCustomSodiumChange,
                        label = { Text("Overwrite sodium (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gender", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.selectableGroup()) {
            Gender.values().forEach {
                Row(
                    Modifier
                        .weight(1f)
                        .selectable(
                            selected = (it == gender),
                            enabled = !hasAllOverrides,
                            onClick = { onGenderChange(it) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (it == gender), onClick = null, enabled = !hasAllOverrides)
                    Text(text = it.name.lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Activity Level", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            ActivityLevel.values().forEach {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (it == activityLevel),
                            enabled = !hasAllOverrides,
                            onClick = { onActivityLevelChange(it) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (it == activityLevel), onClick = null, enabled = !hasAllOverrides)
                    Text(text = it.name.lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Goal", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            CalorieGoal.values().forEach { goal ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (goal == calorieGoal),
                            enabled = !hasAllOverrides,
                            onClick = { onCalorieGoalChange(goal) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (goal == calorieGoal), onClick = null, enabled = !hasAllOverrides)
                    Text(
                        text = goal.name.lowercase(Locale.getDefault())
                            .replace('_', ' ')
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingSwitchItem(
            text = stringResource(R.string.settings_calorie_track_consumed),
            checked = foodConsumedTrackingEnabled,
            onCheckedChange = onFoodConsumedTrackingEnabledChange
        )
        Text(
            text = stringResource(R.string.settings_calorie_track_consumed_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

private fun calculateCalorieTargetEstimate(
    weightKg: Double?,
    heightCm: Double?,
    age: Int?,
    gender: Gender,
    activityLevel: ActivityLevel,
    calorieGoal: CalorieGoal
): Double? {
    if (weightKg == null || heightCm == null || age == null) return null
    if (weightKg <= 0 || heightCm <= 0 || age <= 0) return null
    val genderConstant = if (gender == Gender.MALE) 5 else -161
    val bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age) + genderConstant
    val activityFactor = when (activityLevel) {
        ActivityLevel.SEDENTARY -> 1.2
        ActivityLevel.LIGHT -> 1.375
        ActivityLevel.MODERATE -> 1.55
        ActivityLevel.ACTIVE -> 1.725
    }
    val goalAdjustment = when (calorieGoal) {
        CalorieGoal.MAINTAIN -> 0.0
        CalorieGoal.LOSE_WEIGHT -> -500.0
        CalorieGoal.GAIN_WEIGHT -> 300.0
    }
    return (bmr * activityFactor + goalAdjustment).coerceAtLeast(0.0)
}


@Composable
private fun SettingSwitchItem(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, modifier = Modifier.weight(1f), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun DataSyncSettingsSection(
    useGoogleBackupModeChecked: Boolean,
    onUseGoogleBackupModeChange: (Boolean) -> Unit,
    selectedGoogleAccountName: String?,
    onSelectGoogleAccount: () -> Unit
) {
    val rawEmail = selectedGoogleAccountName
    val displayEmail = rawEmail?.let { censorEmailForDisplay(it) }

    LaunchedEffect(rawEmail) {
        android.util.Log.d("SettingsScreen", "DataSyncSettingsSection rawEmail = $rawEmail")
    }

    Text(
        text = if (!rawEmail.isNullOrEmpty()) {
            stringResource(R.string.settings_selected_google_account, displayEmail ?: rawEmail)
        } else {
            stringResource(R.string.settings_no_google_account_selected)
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp)
    )





    OutlinedButton(
        onClick = onSelectGoogleAccount,
        enabled = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(if (selectedGoogleAccountName != null) stringResource(R.string.settings_change_google_account) else stringResource(R.string.settings_select_google_account))
    }
    Spacer(modifier = Modifier.height(8.dp))
    SettingSwitchItem(
        text = stringResource(R.string.settings_data_sync_enable_calendar_sync),
        checked = !useGoogleBackupModeChecked,
        onCheckedChange = { onUseGoogleBackupModeChange(!it) }
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun CalendarSyncSettingsSection(
    currentSyncInterval: Long,
    syncHoursInputText: String,
    onSyncHoursChange: (String) -> Unit,
    syncMinutesInputText: String,
    onSyncMinutesChange: (String) -> Unit,
    useGoogleBackupModeChecked: Boolean,
    selectedImportTargetCalendarId: String,
    onImportTargetCalendarIdChange: (String) -> Unit,
    onSelectGoogleCalendarForImport: () -> Unit,
    selectedImportTargetCalendarIdForBothMode: String,
    onSelectGoogleCalendarForBothModeImport: () -> Unit,
    calendarSyncAppToCalendarEnabled: Boolean,
    onCalendarSyncAppToCalendarEnabledChange: (Boolean) -> Unit,
    calendarSyncCalendarToAppEnabled: Boolean,
    onCalendarSyncCalendarToAppEnabledChange: (Boolean) -> Unit,
    selectedOnAppDeleteImportedAction: String,
    onOnAppDeleteImportedActionChange: (String) -> Unit,
    onManageBlockedImportsClick: () -> Unit,
    selectedOnCalendarDeleteAction: String,
    onOnCalendarDeleteActionChange: (String) -> Unit,
    selectedGoogleAccountName: String?, // Added parameter
    showAllEvents: Boolean,
    onShowAllEventsChange: (Boolean) -> Unit
) {
    Text(stringResource(R.string.settings_calendar_sync_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (useGoogleBackupModeChecked) 0.38f else MaterialTheme.colorScheme.onSurface.alpha))
    SettingSwitchItem(
        text = stringResource(R.string.settings_calendar_sync_direction_app_to_calendar),
        checked = calendarSyncAppToCalendarEnabled,
        onCheckedChange = onCalendarSyncAppToCalendarEnabledChange,
        enabled = !useGoogleBackupModeChecked
    )
    SettingSwitchItem(
        text = stringResource(R.string.settings_calendar_sync_direction_calendar_to_app),
        checked = calendarSyncCalendarToAppEnabled,
        onCheckedChange = onCalendarSyncCalendarToAppEnabledChange,
        enabled = !useGoogleBackupModeChecked
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_calendar_sync_background_sync_interval),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (useGoogleBackupModeChecked) 0.38f else MaterialTheme.colorScheme.onSurface.alpha)
        )
        OutlinedTextField(
            value = syncHoursInputText,
            onValueChange = onSyncHoursChange,
            label = { Text(stringResource(R.string.settings_label_hours_short)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(70.dp),
            enabled = !useGoogleBackupModeChecked
        )
        Text(
            ":",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (useGoogleBackupModeChecked) 0.38f else MaterialTheme.colorScheme.onSurface.alpha)
        )
        OutlinedTextField(
            value = syncMinutesInputText,
            onValueChange = onSyncMinutesChange,
            label = { Text(stringResource(R.string.settings_label_minutes_short)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(70.dp),
            enabled = !useGoogleBackupModeChecked
        )
    }
    Spacer(modifier = Modifier.height(16.dp)) // This spacer maintains separation to the next element
    val importEnabled = calendarSyncCalendarToAppEnabled && !useGoogleBackupModeChecked
    Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp).selectableGroup()) {
        Text(
            stringResource(R.string.settings_calendar_sync_import_from),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (importEnabled) MaterialTheme.colorScheme.onSurface.alpha else 0.38f)
        )
        val isGoogleSelected = selectedImportTargetCalendarId.startsWith("google")
        val isBothSelected = selectedImportTargetCalendarId == SettingsRepository.IMPORT_TARGET_CALENDAR_BOTH
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedImportTargetCalendarId == SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL,
                onClick = { onImportTargetCalendarIdChange(SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL) },
                enabled = importEnabled
            )
            Text(
                text = stringResource(R.string.settings_calendar_sync_import_from_local),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
                color = if (importEnabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
            )
        }
        // "Google Calendar" option
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isGoogleSelected,
                onClick = {
                    val fallback = if (selectedGoogleAccountName != null) {
                        SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY
                    } else {
                        SettingsRepository.IMPORT_TARGET_CALENDAR_PRIMARY
                    }
                    onImportTargetCalendarIdChange(fallback)
                },
                enabled = importEnabled
            )
            Text(
                text = stringResource(R.string.settings_default_events_select_gcal_primary),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
                color = if (importEnabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
            )
        }
        if (isGoogleSelected) {
            OutlinedButton(
                onClick = onSelectGoogleCalendarForImport,
                enabled = importEnabled,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp)
            ) {
                Text(stringResource(R.string.settings_calendar_sync_select_google_calendar))
            }
        }
        // "Both" option implicitly uses the selected Google Account's primary for the Google part
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isBothSelected,
                onClick = { onImportTargetCalendarIdChange(SettingsRepository.IMPORT_TARGET_CALENDAR_BOTH) },
                enabled = importEnabled
            )
            Text(
                text = stringResource(R.string.settings_calendar_sync_import_from_both),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
                color = if (importEnabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
            )
        }
        if (isBothSelected) {
            OutlinedButton(
                onClick = onSelectGoogleCalendarForBothModeImport,
                enabled = importEnabled,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp)
            ) {
                Text(stringResource(R.string.settings_calendar_sync_select_google_calendar))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_calendar_sync_on_app_delete_imported_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (importEnabled) MaterialTheme.colorScheme.onSurface.alpha else 0.38f)
        )
        val appDeleteOptions = listOf(
            SettingsRepository.ACTION_APP_DELETE_KEEPS_CALENDAR to stringResource(R.string.settings_calendar_sync_on_app_delete_keep),
            SettingsRepository.ACTION_APP_DELETE_DELETES_CALENDAR to stringResource(R.string.settings_calendar_sync_on_app_delete_delete),
            SettingsRepository.ACTION_APP_DELETE_ALWAYS_ASK to stringResource(R.string.settings_calendar_sync_on_app_delete_ask)
        )
        appDeleteOptions.forEach { (actionKey, displayText) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedOnAppDeleteImportedAction == actionKey,
                    onClick = { onOnAppDeleteImportedActionChange(actionKey) },
                    enabled = importEnabled
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                    color = if (importEnabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    SettingSwitchItem(
        text = "Show all events",
        checked = showAllEvents,
        onCheckedChange = onShowAllEventsChange,
        enabled = !useGoogleBackupModeChecked
    )
    Text(
        text = "Warning: This will show events from all calendars, even those you are not syncing from. This may cause unexpected behavior.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onManageBlockedImportsClick,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        enabled = importEnabled
    ) {
        Text(stringResource(R.string.settings_calendar_sync_manage_blocked_imports))
    }
    Spacer(modifier = Modifier.height(8.dp))
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.settings_calendar_sync_on_calendar_delete_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (useGoogleBackupModeChecked) 0.38f else MaterialTheme.colorScheme.onSurface.alpha))
    Column(Modifier.selectableGroup().padding(start = 16.dp, bottom = 8.dp)) {
        val deleteOptions = listOf(
            SettingsRepository.ACTION_KEEP_IN_APP to stringResource(R.string.settings_calendar_sync_on_calendar_delete_keep_in_app), // MOVED UP
            SettingsRepository.ACTION_DELETE_FROM_APP to stringResource(R.string.settings_calendar_sync_on_calendar_delete_delete_from_app) // MOVED DOWN
        )
        deleteOptions.forEach { (actionKey, displayText) ->
            Row(Modifier.fillMaxWidth().selectable(selected = selectedOnCalendarDeleteAction == actionKey, onClick = { onOnCalendarDeleteActionChange(actionKey) }, enabled = !useGoogleBackupModeChecked).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedOnCalendarDeleteAction == actionKey, onClick = null, enabled = !useGoogleBackupModeChecked)
                Text(text = displayText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp), color = if (!useGoogleBackupModeChecked) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f))
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun BackupRestoreSection(
    driveBackupUri: String?,
    onLocalBackup: () -> Unit,
    onLocalRestore: () -> Unit,
    onDriveBackup: () -> Unit,
    onDriveRestore: () -> Unit
) {
    Text(
        text = stringResource(R.string.settings_backup_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_backup_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.settings_backup_file_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(R.string.settings_backup_file_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedButton(onClick = onLocalBackup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_backup_export_action))
            }
            OutlinedButton(onClick = onLocalRestore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_backup_import_action))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_backup_drive_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = if (driveBackupUri.isNullOrBlank()) {
                    stringResource(R.string.settings_backup_drive_not_set)
                } else {
                    stringResource(R.string.settings_backup_drive_ready)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedButton(onClick = onDriveBackup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_drive_backup_action))
            }
            OutlinedButton(onClick = onDriveRestore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_drive_restore_action))
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun DefaultEventsSettingsSection(
    startTimeOption: StartTimeOption,
    onStartTimeOptionChange: (StartTimeOption) -> Unit,
    defaultEventHourState: Int,
    defaultEventMinuteState: Int,
    onSpecificTimeClick: () -> Unit,
    defaultDurationHoursText: String,
    onDefaultDurationHoursChange: (String) -> Unit,
    defaultDurationMinutesText: String,
    onDefaultDurationMinutesChange: (String) -> Unit,
    isOneTimeChecked: Boolean,
    onIsOneTimeChange: (Boolean) -> Unit,
    createCalendarEntryChecked: Boolean,
    onCreateCalendarEntryChange: (Boolean) -> Unit,
    useGoogleBackupModeChecked: Boolean,
    selectedCalendarIdForNewEvents: String,
    onSelectedCalendarIdForNewEventsChange: (String) -> Unit,
    onSelectGoogleCalendarForNewEvents: () -> Unit,
    googleCalendars: List<CalendarInfo>,
    systemNotificationChecked: Boolean,
    onSystemNotificationChange: (Boolean) -> Unit,
    showDetailsInNotificationChecked: Boolean,
    onShowDetailsInNotificationChange: (Boolean) -> Unit,
    selectedGoogleAccountName: String?,
    eventsEnabled: Boolean,
    onEventsEnabledChange: (Boolean) -> Unit,
    eventIndicatorDisplayCondition: EventColorDisplayCondition,
    onEventIndicatorDisplayConditionChange: (EventColorDisplayCondition) -> Unit,
    eventBackgroundDisplayCondition: EventColorDisplayCondition,
    onEventBackgroundDisplayConditionChange: (EventColorDisplayCondition) -> Unit,
    eventTitleColorChoice: EventTitleColorChoice,
    onEventTitleColorChoiceChange: (EventTitleColorChoice) -> Unit,
    eventTitleCustomColor: Int,
    onEventTitleCustomColorChange: (Int) -> Unit,
    recentCustomEventColors: List<Int>
) {
    Text(stringResource(R.string.settings_default_events_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp))
    SettingSwitchItem(
        text = stringResource(R.string.settings_events_enabled_title),
        checked = eventsEnabled,
        onCheckedChange = onEventsEnabledChange
    )
    Text(
        text = stringResource(R.string.settings_events_enabled_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Text(
        text = stringResource(R.string.settings_event_appearance_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    EventColorConditionDropdown(
        label = stringResource(R.string.settings_event_color_indicator_label),
        selectedCondition = eventIndicatorDisplayCondition,
        onConditionChange = onEventIndicatorDisplayConditionChange
    )
    Spacer(modifier = Modifier.height(8.dp))
    EventColorConditionDropdown(
        label = stringResource(R.string.settings_event_color_background_label),
        selectedCondition = eventBackgroundDisplayCondition,
        onConditionChange = onEventBackgroundDisplayConditionChange
    )
    Spacer(modifier = Modifier.height(8.dp))
    EventTitleColorDropdown(
        label = stringResource(R.string.settings_event_title_color_label),
        selectedChoice = eventTitleColorChoice,
        onChoiceChange = onEventTitleColorChoiceChange
    )
    if (eventTitleColorChoice == EventTitleColorChoice.CUSTOM) {
        Spacer(modifier = Modifier.height(8.dp))
        SeriesColorPicker(
            label = stringResource(R.string.settings_event_title_custom_color_label),
            selectedColor = Color(eventTitleCustomColor),
            onColorSelected = { onEventTitleCustomColorChange(it.toArgb()) },
            modifier = Modifier.padding(bottom = 8.dp),
            colorOptions = SeriesColorOptions,
            allowCustomColor = true,
            recentCustomColors = recentCustomEventColors
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(R.string.settings_default_events_start_time), style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        StartTimeOption.entries.forEach { option ->
            Row(
                Modifier.fillMaxWidth().selectable(selected = (option == startTimeOption), onClick = { onStartTimeOptionChange(option) }).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (option == startTimeOption), onClick = null)
                Text(
                    text = when (option) {
                        StartTimeOption.CURRENT_TIME -> stringResource(R.string.settings_start_time_option_current_time)
                        StartTimeOption.SPECIFIC_TIME -> stringResource(R.string.settings_start_time_option_specific_time)
                        StartTimeOption.NEXT_FULL_HOUR -> stringResource(R.string.settings_start_time_option_next_full_hour)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp).weight(1f) // Added weight
                )
                if (option == StartTimeOption.SPECIFIC_TIME && startTimeOption == StartTimeOption.SPECIFIC_TIME) {
                    Spacer(Modifier.width(8.dp)) // Add some space
                    Text(
                        String.format(Locale.getDefault(), "%02d:%02d", defaultEventHourState, defaultEventMinuteState),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = onSpecificTimeClick, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.settings_default_events_edit_specific_time))
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    //Text(stringResource(R.string.settings_default_events_duration), style = MaterialTheme.typography.titleMedium) // Removed this line
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp) // Removed Arrangement.SpaceBetween
    ) {
        Text(
            stringResource(R.string.settings_default_events_duration), // Moved Text here
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f) // Added weight
        )
        OutlinedTextField(
            value = defaultDurationHoursText,
            onValueChange = onDefaultDurationHoursChange,
            label = { Text(stringResource(R.string.settings_label_hours_short)) }, // Changed label
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(70.dp) // Changed modifier
        )
        Text(
            ":", // Added colon
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        OutlinedTextField(
            value = defaultDurationMinutesText,
            onValueChange = onDefaultDurationMinutesChange,
            label = { Text(stringResource(R.string.settings_label_minutes_short)) }, // Changed label
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(70.dp) // Changed modifier
        )
        // Spacer(modifier = Modifier.width(16.dp)) // Removed Spacer
    }
    Spacer(modifier = Modifier.height(8.dp))
    SettingSwitchItem(
        text = stringResource(R.string.settings_default_events_one_time_appointment),
        checked = !isOneTimeChecked,
        onCheckedChange = { isRepeating -> onIsOneTimeChange(!isRepeating) }
    )
    SettingSwitchItem(
        text = stringResource(R.string.settings_default_events_create_calendar_entry),
        checked = createCalendarEntryChecked,
        onCheckedChange = onCreateCalendarEntryChange,
        enabled = !useGoogleBackupModeChecked
    )
    if (createCalendarEntryChecked && !useGoogleBackupModeChecked) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp).selectableGroup()) {
            Text(stringResource(R.string.settings_default_events_target_calendar_new_events), style = MaterialTheme.typography.titleSmall)
            val calendarOptionsForNewEvents = mutableListOf<Pair<String, String>>()
            calendarOptionsForNewEvents.add(SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL to stringResource(R.string.settings_default_events_target_calendar_local_device))
            if (selectedGoogleAccountName != null) {
                calendarOptionsForNewEvents.add(SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY to stringResource(R.string.settings_default_events_select_gcal_primary))
            }

            calendarOptionsForNewEvents.forEach { (optionId, optionDisplayName) ->
                val isSelected = selectedCalendarIdForNewEvents == optionId
                Row(Modifier.fillMaxWidth().selectable(
                    selected = isSelected,
                    onClick = { onSelectedCalendarIdForNewEventsChange(optionId) }
                ).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = null)
                    Text(text = optionDisplayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(R.string.settings_default_events_notification_options_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    SettingSwitchItem(text = stringResource(R.string.settings_default_events_system_notification), checked = systemNotificationChecked, onCheckedChange = onSystemNotificationChange)
    SettingSwitchItem(text = stringResource(R.string.settings_default_events_show_details_notification), checked = showDetailsInNotificationChecked, enabled = systemNotificationChecked, onCheckedChange = onShowDetailsInNotificationChange)
    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventColorConditionDropdown(
    label: String,
    selectedCondition: EventColorDisplayCondition,
    onConditionChange: (EventColorDisplayCondition) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = eventConditionLabel(selectedCondition)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            FilledTonalButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.menuAnchor()
            ) {
                Text(selectedLabel)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                EventColorDisplayCondition.entries.forEach { condition ->
                    DropdownMenuItem(
                        text = { Text(eventConditionLabel(condition)) },
                        onClick = {
                            onConditionChange(condition)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTitleColorDropdown(
    label: String,
    selectedChoice: EventTitleColorChoice,
    onChoiceChange: (EventTitleColorChoice) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = eventTitleColorLabel(selectedChoice)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            FilledTonalButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.menuAnchor()
            ) {
                Text(selectedLabel)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                EventTitleColorChoice.entries.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(eventTitleColorLabel(choice)) },
                        onClick = {
                            onChoiceChange(choice)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun eventConditionLabel(condition: EventColorDisplayCondition): String {
    return stringResource(
        when (condition) {
            EventColorDisplayCondition.ALWAYS -> R.string.settings_event_color_condition_always
            EventColorDisplayCondition.DONE -> R.string.settings_event_color_condition_done
            EventColorDisplayCondition.PAST -> R.string.settings_event_color_condition_past
            EventColorDisplayCondition.DONE_OR_PAST -> R.string.settings_event_color_condition_done_or_past
            EventColorDisplayCondition.NEVER -> R.string.settings_event_color_condition_never
        }
    )
}

@Composable
private fun eventTitleColorLabel(choice: EventTitleColorChoice): String {
    return stringResource(
        when (choice) {
            EventTitleColorChoice.PRIMARY -> R.string.settings_event_title_color_primary
            EventTitleColorChoice.SECONDARY -> R.string.settings_event_title_color_secondary
            EventTitleColorChoice.EVENT_COLOR -> R.string.settings_event_title_color_event
            EventTitleColorChoice.CUSTOM -> R.string.settings_event_title_color_custom
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventSetsSettingsSection(
    eventSetNames: List<String>,
    eventSetColors: List<Int>,
    defaultActiveSetsByWeekday: Map<DayOfWeek, Set<Int>>,
    onResetManualActiveSets: () -> Unit,
    onEventSetNameChange: (Int, String) -> Unit,
    onEventSetColorChange: (Int, Int) -> Unit,
    onDefaultActiveSetsChange: (DayOfWeek, Set<Int>) -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    Text(stringResource(R.string.settings_event_sets_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
    Text(
        text = stringResource(R.string.settings_event_sets_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
    OutlinedButton(
        onClick = { showResetDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.settings_event_sets_reset_manual_action))
    }
    Text(
        text = stringResource(R.string.settings_event_sets_reset_manual_warning),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
    )

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_event_sets_reset_manual_title)) },
            text = { Text(stringResource(R.string.settings_event_sets_reset_manual_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetManualActiveSets()
                }) {
                    Text(stringResource(R.string.settings_event_sets_reset_manual_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.alert_action_cancel))
                }
            }
        )
    }

    eventSetNames.forEachIndexed { index, name ->
        val label = stringResource(R.string.settings_event_set_name_label, ('A' + index))
        OutlinedTextField(
            value = name,
            onValueChange = { onEventSetNameChange(index, it.take(24)) },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        val currentColor = eventSetColors.getOrNull(index) ?: DEFAULT_SERIES_COLOR_ARGB
        SeriesColorPicker(
            label = stringResource(R.string.settings_event_set_color_label, ('A' + index)),
            selectedColor = Color(currentColor),
            onColorSelected = { onEventSetColorChange(index, it.toArgb()) },
            modifier = Modifier.padding(bottom = 12.dp),
            colorOptions = SeriesColorOptions,
            allowCustomColor = false
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.settings_event_sets_defaults_title), style = MaterialTheme.typography.titleMedium)
    Text(
        text = stringResource(R.string.settings_event_sets_defaults_hint, SettingsRepository.MAX_ACTIVE_SETS_PER_DAY),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 8.dp)
    )

    DayOfWeek.values().forEach { day ->
        val selections = defaultActiveSetsByWeekday[day].orEmpty()
        Text(
            text = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(eventSetNames.size) { index ->
                val setId = index + 1
                val isSelected = selections.contains(setId)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = selections.toMutableSet()
                        if (isSelected) {
                            updated.remove(setId)
                            onDefaultActiveSetsChange(day, updated)
                        } else {
                            if (updated.size >= SettingsRepository.MAX_ACTIVE_SETS_PER_DAY) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_event_sets_limit_reached, SettingsRepository.MAX_ACTIVE_SETS_PER_DAY),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                updated.add(setId)
                                onDefaultActiveSetsChange(day, updated)
                            }
                        }
                    },
                    label = { Text(eventSetNames[index]) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSettingsSection(
    trackingMode: TrackingMode,
    onTrackingModeChange: (TrackingMode) -> Unit
) {
    var trackingMenuExpanded by remember { mutableStateOf(false) }

    Text(stringResource(R.string.settings_category_map), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Tracking mode", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = trackingMode.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        ExposedDropdownMenuBox(
            expanded = trackingMenuExpanded,
            onExpandedChange = { trackingMenuExpanded = !trackingMenuExpanded }
        ) {
            FilledTonalButton(
                onClick = { trackingMenuExpanded = !trackingMenuExpanded },
                modifier = Modifier.menuAnchor()
            ) {
                Text(trackingMode.label)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (trackingMenuExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            ExposedDropdownMenu(
                expanded = trackingMenuExpanded,
                onDismissRequest = { trackingMenuExpanded = false }
            ) {
                TrackingMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label) },
                        onClick = {
                            onTrackingModeChange(mode)
                            trackingMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun AppSettingsSection(
    context: Context,
    selectedTabs: Set<AppTab>,
    onTabSelectionChange: (Set<AppTab>) -> Unit,
    selectedGoogleAccountName: String?,
    onDataManagementClick: (String, String?) -> Unit,
    calendarEventCounts: CalendarEventCounts,
    primaryColor: Int,
    secondaryColor: Int,
    onPrimaryColorChange: (Int) -> Unit,
    onSecondaryColorChange: (Int) -> Unit,
    routineInsightsEnabled: Boolean,
    onRoutineInsightsEnabledChange: (Boolean) -> Unit,
    onBackupClick: () -> Unit
) {
    var backupSyncHoursInputText by remember { mutableStateOf("1") }
    var backupSyncMinutesInputText by remember { mutableStateOf("0") }
    val isGoogleAccountSelected = selectedGoogleAccountName != null
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var showAppearanceSection by remember { mutableStateOf(true) }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.settings_app_about_action)) },
            text = { Text(stringResource(R.string.settings_app_about_body)) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.alert_action_close))
                }
            }
        )
    }

    if (showCreditsDialog) {
        AlertDialog(
            onDismissRequest = { showCreditsDialog = false },
            title = { Text(stringResource(R.string.settings_app_credits_action)) },
            text = { Text(stringResource(R.string.settings_app_credits_body)) },
            confirmButton = {
                TextButton(onClick = { showCreditsDialog = false }) {
                    Text(stringResource(R.string.alert_action_close))
                }
            }
        )
    }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            title = { Text(stringResource(R.string.settings_app_legal_action)) },
            text = { Text(stringResource(R.string.settings_app_legal_body)) },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) {
                    Text(stringResource(R.string.alert_action_close))
                }
            }
        )
    }

    Text(stringResource(R.string.settings_app_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
    Text("General", style = MaterialTheme.typography.titleMedium)
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAppearanceSection = !showAppearanceSection },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showAppearanceSection) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = showAppearanceSection) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_app_theme_colors_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    SeriesColorPicker(
                        label = stringResource(R.string.settings_app_theme_primary_label),
                        selectedColor = Color(primaryColor),
                        onColorSelected = { onPrimaryColorChange(it.toArgb()) },
                        allowCustomColor = false
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SeriesColorPicker(
                        label = stringResource(R.string.settings_app_theme_secondary_label),
                        selectedColor = Color(secondaryColor),
                        onColorSelected = { onSecondaryColorChange(it.toArgb()) },
                        allowCustomColor = false
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_routine_insights_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.settings_routine_insights_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = routineInsightsEnabled,
                    onCheckedChange = onRoutineInsightsEnabledChange
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(R.string.settings_tabs_title), style = MaterialTheme.typography.titleMedium)
    Text(
        text = stringResource(R.string.settings_tabs_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
    AppTab.entries.forEach { tab ->
        val checked = selectedTabs.contains(tab)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val updatedTabs = if (checked) {
                        selectedTabs - tab
                    } else {
                        selectedTabs + tab
                    }
                    onTabSelectionChange(updatedTabs)
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { isChecked ->
                    val updatedTabs = if (isChecked) {
                        selectedTabs + tab
                    } else {
                        selectedTabs - tab
                    }
                    onTabSelectionChange(updatedTabs)
                }
            )
            Text(
                text = stringResource(tab.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
    Text(
        text = stringResource(R.string.settings_tabs_disabled_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Text(
        "Data Management",
        style = MaterialTheme.typography.titleMedium
    )
    OutlinedButton(
        onClick = { onDataManagementClick("LOCAL", null) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text("Manage Local Calendar Data (${calendarEventCounts.local} events)")
    }
    calendarEventCounts.google.forEach { (account, count) ->
        OutlinedButton(
            onClick = { onDataManagementClick("GOOGLE", account) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text("Manage $account Data ($count events)")
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(onClick = onBackupClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(stringResource(R.string.settings_app_import_export))
    }
    OutlinedButton(onClick = { Toast.makeText(context, context.getString(R.string.settings_toast_work_in_progress), Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(stringResource(R.string.settings_app_language)) }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_data_sync_backup_interval),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = if (isGoogleAccountSelected) MaterialTheme.colorScheme.onSurface.alpha else 0.38f)
        )
        OutlinedTextField(
            value = backupSyncHoursInputText,
            onValueChange = { backupSyncHoursInputText = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.settings_label_hours_short)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(70.dp),
            enabled = isGoogleAccountSelected
        )
        Text(
            ":",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isGoogleAccountSelected) MaterialTheme.colorScheme.onSurface.alpha else 0.38f)
        )
        OutlinedTextField(
            value = backupSyncMinutesInputText,
            onValueChange = { backupSyncMinutesInputText = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.settings_label_minutes_short)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(70.dp),
            enabled = isGoogleAccountSelected
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(R.string.settings_app_about_section_title), style = MaterialTheme.typography.titleMedium)
    OutlinedButton(
        onClick = { showAboutDialog = true },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(stringResource(R.string.settings_app_about_action))
    }
    OutlinedButton(
        onClick = { showCreditsDialog = true },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(stringResource(R.string.settings_app_credits_action))
    }
    OutlinedButton(
        onClick = { showLegalDialog = true },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(stringResource(R.string.settings_app_legal_action))
    }
}

@Composable
private fun UnsavedChangesAlertDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alert_unsaved_changes_title)) },
        text = { Text(stringResource(R.string.alert_unsaved_changes_message)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.alert_action_discard)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.alert_action_cancel)) } }
    )
}

@Composable
private fun GoogleCalendarDialog(
    googleCalendars: List<CalendarInfo>, // This list is now pre-filtered by account by the caller
    onDismiss: () -> Unit,
    onCalendarSelected: (String) -> Unit,
    selectedGoogleAccountName: String? // Added to provide context
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_gcal_choose_title)) },
        text = {
            if (googleCalendars.isEmpty()) {
                Text(if (selectedGoogleAccountName != null) stringResource(R.string.dialog_gcal_no_calendars_found) + " for account $selectedGoogleAccountName." else stringResource(R.string.dialog_gcal_no_calendars_found))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Row(Modifier.fillMaxWidth().clickable {
                            onCalendarSelected(if (selectedGoogleAccountName != null) SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY else SettingsRepository.IMPORT_TARGET_CALENDAR_PRIMARY)
                        }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dialog_gcal_primary) + (selectedGoogleAccountName?.let { " ($it)" } ?: ""))
                        }
                    }
                    items(googleCalendars) { calendarInfo -> // These are already filtered for the selected account if one is chosen
                        Row(Modifier.fillMaxWidth().clickable { onCalendarSelected("google_calendar_id_${calendarInfo.id}") }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dialog_gcal_item_format, calendarInfo.displayName, calendarInfo.accountName))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.alert_action_close)) } }
    )
}

/*@Composable
private fun BlockedImportsDialog(
    blockedCalendarImports: List<BlockedEventDisplayInfo>,
    onUnblockEvent: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_blocked_imports_title)) },
        text = {
            if (blockedCalendarImports.isEmpty()) {
                Text(stringResource(R.string.dialog_blocked_imports_empty))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(blockedCalendarImports) { blockedEvent ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${blockedEvent.displayTitle} (Type: ${blockedEvent.accountType ?: "Unknown"})",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onUnblockEvent(blockedEvent.calendarEventId) }) {
                                Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.dialog_blocked_imports_unlock_event_desc))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.alert_action_close)) } }
    )
}*/
