package com.example.routinereminder.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.R
import com.example.routinereminder.data.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*

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
            return entries.find { it.name == name } ?: SPECIFIC_TIME
        }
    }
}

enum class SettingsCategory {
    PROFILE,
    SYNC,
    DEFAULT_EVENTS,
    APP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    from: String,
    onDismiss: () -> Unit
)
 {
     val context = LocalContext.current
     val enabledTabsState by viewModel.enabledTabs.collectAsState()
     val enabledTabs = enabledTabsState ?: AppTab.defaultTabs
     val routineEnabled = enabledTabs.contains(AppTab.Routine)
     val caloriesEnabled = enabledTabs.contains(AppTab.Calories)
     val initialCategory = when (from) {
         "routine" -> if (routineEnabled) SettingsCategory.DEFAULT_EVENTS else SettingsCategory.SYNC
         "calories" -> if (caloriesEnabled) SettingsCategory.PROFILE else SettingsCategory.SYNC
         "map" -> SettingsCategory.APP
         else -> SettingsCategory.SYNC
     }

     var selectedCategory by remember(from, enabledTabs) { mutableStateOf(initialCategory) }


     val userSettings by viewModel.userSettings.collectAsState()
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var selectedActivityLevel by remember { mutableStateOf(ActivityLevel.SEDENTARY) }


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
    //val blockedCalendarImports by viewModel.blockedCalendarImportsForDisplay.collectAsState(initial = emptyList())

    val selectedGoogleAccountName by viewModel.selectedGoogleAccountName.collectAsState()
     Text(
         text = "DEBUG: selectedGoogleAccountName = ${selectedGoogleAccountName ?: "null"}",
         style = MaterialTheme.typography.bodySmall
     )

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


    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var justSavedSuccessfully by remember { mutableStateOf(false) }
    var showGoogleCalendarDialog by remember { mutableStateOf(false) }
    var calendarSelectionPurpose by remember { mutableStateOf<String?>(null) }
    var showBlockedImportsDialog by remember { mutableStateOf(false) }
    var showDataManagementDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var accountNameToManage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(userSettings) {
        userSettings?.let {
            weightInput = it.weightKg.toString()
            heightInput = it.heightCm.toString()
            ageInput = it.age.toString()
            selectedGender = it.gender
            selectedActivityLevel = it.activityLevel
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
        weightInput, heightInput, ageInput, selectedGender, selectedActivityLevel, userSettings,
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
        selectedTabs, enabledTabs
    ) {
        derivedStateOf {
            if (weightInput.toDoubleOrNull() != userSettings?.weightKg) return@derivedStateOf true
            if (heightInput.toDoubleOrNull() != userSettings?.heightCm) return@derivedStateOf true
            if (ageInput.toIntOrNull() != userSettings?.age) return@derivedStateOf true
            if (selectedGender != userSettings?.gender) return@derivedStateOf true
            if (selectedActivityLevel != userSettings?.activityLevel) return@derivedStateOf true
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = ::attemptDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_action_back)) } },
                actions = {
                    Button(onClick = {
                        val newSettings = UserSettings(
                            weightKg = weightInput.toDoubleOrNull() ?: 0.0,
                            heightCm = heightInput.toDoubleOrNull() ?: 0.0,
                            age = ageInput.toIntOrNull() ?: 0,
                            gender = selectedGender,
                            activityLevel = selectedActivityLevel
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val allowedCategories = when (from) {
                    "routine" -> listOfNotNull(
                        if (routineEnabled) SettingsCategory.DEFAULT_EVENTS else null,
                        SettingsCategory.SYNC,
                        SettingsCategory.APP
                    )
                    "calories" -> listOfNotNull(
                        if (caloriesEnabled) SettingsCategory.PROFILE else null,
                        SettingsCategory.SYNC,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    allowedCategories.forEach { category ->
                        TextButton(
                            onClick = { selectedCategory = category },
                            modifier = Modifier.weight(1f)
                        ) {
                            val text = when (category) {
                                SettingsCategory.PROFILE -> "Profile"
                                SettingsCategory.DEFAULT_EVENTS -> stringResource(R.string.settings_category_default_events)
                                SettingsCategory.SYNC -> stringResource(R.string.settings_category_sync)
                                SettingsCategory.APP -> stringResource(R.string.settings_category_app)
                            }

                            Text(
                                text,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                        gender = selectedGender,
                        onGenderChange = { selectedGender = it },
                        activityLevel = selectedActivityLevel,
                        onActivityLevelChange = { selectedActivityLevel = it }
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
                        selectedGoogleAccountName = selectedGoogleAccountName // Pass down
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
                            selectedGoogleAccountName = selectedGoogleAccountName // Pass down
                        )
                    }
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
                        showAllEvents = showAllEventsChecked,
                        onShowAllEventsChange = { showAllEventsChecked = it },
                        onDataManagementClick = { source, account ->
                            accountNameToManage = account
                            showDataManagementDialog = source
                        },
                        calendarEventCounts = calendarEventCounts,
                        trackingMode = TrackingMode.fromValue(mapTrackingMode),
                        onTrackingModeChange = { mode ->
                            viewModel.saveMapTrackingMode(mode.value)
                            justSavedSuccessfully = false
                        }
                    )
                }
            }
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
fun ProfileSettingsSection(
    weight: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    gender: Gender,
    onGenderChange: (Gender) -> Unit,
    activityLevel: ActivityLevel,
    onActivityLevelChange: (ActivityLevel) -> Unit
) {
    Column {
        Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp))
        OutlinedTextField(
            value = weight,
            onValueChange = onWeightChange,
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = height,
            onValueChange = onHeightChange,
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gender", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.selectableGroup()) {
            Gender.values().forEach {
                Row(
                    Modifier
                        .weight(1f)
                        .selectable(selected = (it == gender), onClick = { onGenderChange(it) })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (it == gender), onClick = null)
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
                        .selectable(selected = (it == activityLevel), onClick = { onActivityLevelChange(it) })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (it == activityLevel), onClick = null)
                    Text(text = it.name.lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
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
    selectedGoogleAccountName: String? // Added parameter
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
    selectedGoogleAccountName: String?
) {
    Text(stringResource(R.string.settings_default_events_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp))
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
    SettingSwitchItem(text = stringResource(R.string.settings_default_events_one_time_appointment), checked = isOneTimeChecked, onCheckedChange = onIsOneTimeChange)
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

@Composable
private fun AppSettingsSection(
    context: Context,
    selectedTabs: Set<AppTab>,
    onTabSelectionChange: (Set<AppTab>) -> Unit,
    selectedGoogleAccountName: String?,
    showAllEvents: Boolean,
    onShowAllEventsChange: (Boolean) -> Unit,
    onDataManagementClick: (String, String?) -> Unit,
    calendarEventCounts: CalendarEventCounts,
    trackingMode: TrackingMode,
    onTrackingModeChange: (TrackingMode) -> Unit
) {
    var backupSyncHoursInputText by remember { mutableStateOf("1") }
    var backupSyncMinutesInputText by remember { mutableStateOf("0") }
    val isGoogleAccountSelected = selectedGoogleAccountName != null
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var trackingMenuExpanded by remember { mutableStateOf(false) }

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
        TextButton(onClick = { trackingMenuExpanded = true }) {
            Text("Change")
        }
        DropdownMenu(
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
    OutlinedButton(onClick = { Toast.makeText(context, context.getString(R.string.settings_toast_work_in_progress), Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(stringResource(R.string.settings_app_import_export)) }
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isGoogleAccountSelected) MaterialTheme.colorScheme.onSurface.alpha else 0.38f)
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

    SettingSwitchItem(
        text = "Show all events",
        checked = showAllEvents,
        onCheckedChange = onShowAllEventsChange
    )
    Text(
        text = "Warning: This will show events from all calendars, even those you are not syncing from. This may cause unexpected behavior.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )

    Spacer(modifier = Modifier.height(16.dp))
    Text("Data Management", style = MaterialTheme.typography.titleMedium)
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
