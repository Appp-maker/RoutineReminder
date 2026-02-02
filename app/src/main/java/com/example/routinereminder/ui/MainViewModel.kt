package com.example.routinereminder.ui
import com.example.routinereminder.data.entities.ScheduleDone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.DefaultEventSettings
import com.example.routinereminder.data.Gender
import com.example.routinereminder.data.RunSessionRepository
import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.data.SettingsRepository
import com.example.routinereminder.data.UserSettings
import com.example.routinereminder.data.AppThemeColors
import com.example.routinereminder.data.DEFAULT_PRIMARY_COLOR_ARGB
import com.example.routinereminder.data.defaultSeriesColorForIndex
import com.example.routinereminder.data.EventBackgroundTransparency
import com.example.routinereminder.data.EventColorDisplayCondition
import com.example.routinereminder.data.EventTitleColorChoice
import com.example.routinereminder.data.PastEventColorTreatment
import com.example.routinereminder.data.model.ActiveRunState
import com.example.routinereminder.data.model.TrailPoint
import com.example.routinereminder.data.mappers.toEntity
import com.example.routinereminder.data.mappers.toItem
import com.example.routinereminder.location.TrackingService
import com.example.routinereminder.util.NotificationScheduler
import com.example.routinereminder.util.CalendarSyncManager
import com.example.routinereminder.util.CalendarMeta
import com.example.routinereminder.util.ExportImportUtil
import com.example.routinereminder.workers.ExerciseDbDownloadWorker
import com.example.routinereminder.sync.DriveAppDataProvider
import com.example.routinereminder.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt
import java.util.UUID
import org.maplibre.geojson.Point
import android.content.Context
import android.net.Uri

data class CalendarInfo(val id: String, val displayName: String, val accountName: String)

private data class ScheduleFilterState(
    val itemsForDate: List<ScheduleItem>,
    val availableSetIds: Set<Int>,
    val effectiveActiveSetIds: Set<Int>,
    val eventSetsEnabled: Boolean
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val runSessionRepository: RunSessionRepository
) : AndroidViewModel(application) {

    private val scheduleDao = database.scheduleDao()
    private val scheduleDoneDao = database.scheduleDoneDao()

    private val blockedCalendarEventDao = database.blockedCalendarImportDao()

    private val notificationScheduler = NotificationScheduler(application)

    private val _scheduleItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val scheduleItems: StateFlow<List<ScheduleItem>> = _scheduleItems.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _syncIntervalMinutes = MutableStateFlow(60L)
    val syncIntervalMinutes: StateFlow<Long> = _syncIntervalMinutes.asStateFlow()

    private val _defaultEventSettings = MutableStateFlow(
        DefaultEventSettings(0, 0, 0, 0, false, false, false, false, "", "")
    )
    val defaultEventSettings: StateFlow<DefaultEventSettings> = _defaultEventSettings.asStateFlow()

    private val _googleCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val googleCalendars: StateFlow<List<CalendarInfo>> = _googleCalendars.asStateFlow()

    private val _onCalendarDeleteAction = MutableStateFlow(SettingsRepository.ACTION_KEEP_IN_APP)
    val onCalendarDeleteAction: StateFlow<String> = _onCalendarDeleteAction.asStateFlow()

    private val _importTargetCalendarId =
        MutableStateFlow(SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL)
    val importTargetCalendarId: StateFlow<String> = _importTargetCalendarId.asStateFlow()

    private val _onAppDeleteImportedAction =
        MutableStateFlow(SettingsRepository.ACTION_APP_DELETE_KEEPS_CALENDAR)
    val onAppDeleteImportedAction: StateFlow<String> = _onAppDeleteImportedAction.asStateFlow()

    private val _useGoogleBackupMode = MutableStateFlow(false)
    val useGoogleBackupMode: StateFlow<Boolean> = _useGoogleBackupMode.asStateFlow()

    private val _importTargetCalendarIdForBothMode =
        MutableStateFlow(SettingsRepository.DEFAULT_IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE)
    val importTargetCalendarIdForBothMode: StateFlow<String> =
        _importTargetCalendarIdForBothMode.asStateFlow()

    private val _selectedGoogleAccountName = MutableStateFlow<String?>(null)
    val selectedGoogleAccountName: StateFlow<String?> = _selectedGoogleAccountName.asStateFlow()

    private val _showAllEvents = MutableStateFlow(false)
    val showAllEvents: StateFlow<Boolean> = _showAllEvents.asStateFlow()

    private val _calendarEventCounts = MutableStateFlow(CalendarEventCounts())
    val calendarEventCounts: StateFlow<CalendarEventCounts> = _calendarEventCounts.asStateFlow()

    private val _calendarSyncAppToCalendarEnabled = MutableStateFlow(true)
    val calendarSyncAppToCalendarEnabled: StateFlow<Boolean> =
        _calendarSyncAppToCalendarEnabled.asStateFlow()

    private val _calendarSyncCalendarToAppEnabled = MutableStateFlow(true)
    val calendarSyncCalendarToAppEnabled: StateFlow<Boolean> =
        _calendarSyncCalendarToAppEnabled.asStateFlow()

    private val _enabledTabs = MutableStateFlow<Set<AppTab>?>(null)
    val enabledTabs: StateFlow<Set<AppTab>?> = _enabledTabs.asStateFlow()

    private val _mapTrackingMode = MutableStateFlow(TrackingService.MODE_BALANCED)
    val mapTrackingMode: StateFlow<String> = _mapTrackingMode.asStateFlow()

    private val _foodConsumedTrackingEnabled = MutableStateFlow(false)
    val foodConsumedTrackingEnabled: StateFlow<Boolean> = _foodConsumedTrackingEnabled.asStateFlow()

    private val _routineInsightsEnabled = MutableStateFlow(false)
    val routineInsightsEnabled: StateFlow<Boolean> = _routineInsightsEnabled.asStateFlow()

    private val _routineInsights = MutableStateFlow<RoutineInsights?>(null)
    val routineInsights: StateFlow<RoutineInsights?> = _routineInsights.asStateFlow()

    private val _driveBackupUri = MutableStateFlow<String?>(null)
    val driveBackupUri: StateFlow<String?> = _driveBackupUri.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _eventSetNames = MutableStateFlow(
        List(SettingsRepository.MAX_EVENT_SETS) { index ->
            "Set ${('A' + index)}"
        }
    )
    val eventSetNames: StateFlow<List<String>> = _eventSetNames.asStateFlow()

    private val _eventSetColors = MutableStateFlow(
        List(SettingsRepository.MAX_EVENT_SETS) { index ->
            defaultSeriesColorForIndex(index)
        }
    )
    val eventSetColors: StateFlow<List<Int>> = _eventSetColors.asStateFlow()

    private val _eventSetsEnabled = MutableStateFlow(true)
    val eventSetsEnabled: StateFlow<Boolean> = _eventSetsEnabled.asStateFlow()

    private val _recentCustomEventColors = MutableStateFlow<List<Int>>(emptyList())
    val recentCustomEventColors: StateFlow<List<Int>> = _recentCustomEventColors.asStateFlow()

    private val _appThemeColors = MutableStateFlow(AppThemeColors.Default)
    val appThemeColors: StateFlow<AppThemeColors> = _appThemeColors.asStateFlow()

    private val _eventIndicatorDisplayCondition =
        MutableStateFlow(EventColorDisplayCondition.ALWAYS)
    val eventIndicatorDisplayCondition: StateFlow<EventColorDisplayCondition> =
        _eventIndicatorDisplayCondition.asStateFlow()

    private val _eventBackgroundDisplayCondition =
        MutableStateFlow(EventColorDisplayCondition.NEVER)
    val eventBackgroundDisplayCondition: StateFlow<EventColorDisplayCondition> =
        _eventBackgroundDisplayCondition.asStateFlow()

    private val _eventBackgroundTransparency =
        MutableStateFlow(EventBackgroundTransparency.PERCENT_20)
    val eventBackgroundTransparency: StateFlow<EventBackgroundTransparency> =
        _eventBackgroundTransparency.asStateFlow()

    private val _eventTitleColorChoice = MutableStateFlow(EventTitleColorChoice.PRIMARY)
    val eventTitleColorChoice: StateFlow<EventTitleColorChoice> =
        _eventTitleColorChoice.asStateFlow()

    private val _eventTitleCustomColor = MutableStateFlow(DEFAULT_PRIMARY_COLOR_ARGB)
    val eventTitleCustomColor: StateFlow<Int> = _eventTitleCustomColor.asStateFlow()

    private val _pastEventTextTreatment = MutableStateFlow(PastEventColorTreatment.GREYED_OUT)
    val pastEventTextTreatment: StateFlow<PastEventColorTreatment> =
        _pastEventTextTreatment.asStateFlow()

    private val _pastEventTextCustomColor = MutableStateFlow(DEFAULT_PRIMARY_COLOR_ARGB)
    val pastEventTextCustomColor: StateFlow<Int> = _pastEventTextCustomColor.asStateFlow()

    private val _pastEventBackgroundTreatment = MutableStateFlow(PastEventColorTreatment.GREYED_OUT)
    val pastEventBackgroundTreatment: StateFlow<PastEventColorTreatment> =
        _pastEventBackgroundTreatment.asStateFlow()

    private val _pastEventBackgroundCustomColor = MutableStateFlow(DEFAULT_PRIMARY_COLOR_ARGB)
    val pastEventBackgroundCustomColor: StateFlow<Int> =
        _pastEventBackgroundCustomColor.asStateFlow()

    private val _pastEventBackgroundTransparency =
        MutableStateFlow(EventBackgroundTransparency.PERCENT_20)
    val pastEventBackgroundTransparency: StateFlow<EventBackgroundTransparency> =
        _pastEventBackgroundTransparency.asStateFlow()

    private val _availableSetIds = MutableStateFlow<Set<Int>>(emptySet())
    val availableSetIds: StateFlow<Set<Int>> = _availableSetIds.asStateFlow()

    private val _activeSetIds = MutableStateFlow<Set<Int>>(emptySet())
    val activeSetIds: StateFlow<Set<Int>> = _activeSetIds.asStateFlow()
    private val _hasManualActiveSetsForDate = MutableStateFlow(false)
    val hasManualActiveSetsForDate: StateFlow<Boolean> = _hasManualActiveSetsForDate.asStateFlow()

    private val _defaultActiveSetsByWeekday = MutableStateFlow<Map<DayOfWeek, Set<Int>>>(emptyMap())
    val defaultActiveSetsByWeekday: StateFlow<Map<DayOfWeek, Set<Int>>> = _defaultActiveSetsByWeekday.asStateFlow()

    val showGoogleCalendarChooser = Channel<Unit>()

    private val _syncStatus = Channel<String>()
    val syncStatus = _syncStatus.receiveAsFlow()

    val requestCalendarPermission = Channel<Unit>()
    val triggerGoogleAccountSelectionFlow = Channel<Unit>()
    val showExactAlarmPermissionDialog = Channel<Unit>()

    // Example; adjust according to your DB/model

    var userWeightKg: Double? = null
    var userHeightCm: Double? = null
    var userAge: Int? = null
    var userGender: String? = null


    private var syncJob: Job? = null
    private var isSyncing = false

    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()

    private val _activeRunState = MutableStateFlow<ActiveRunState?>(null)
    val activeRunState: StateFlow<ActiveRunState?> = _activeRunState.asStateFlow()

    private val _trailPoints = MutableStateFlow<List<Point>>(emptyList())
    val trailPoints: StateFlow<List<Point>> = _trailPoints.asStateFlow()

    private val _splitDurations = MutableStateFlow<List<Long>>(emptyList())
    val splitDurations: StateFlow<List<Long>> = _splitDurations.asStateFlow()

    private var trailJob: Job? = null
    private var splitJob: Job? = null

    private val exportImportUtil = ExportImportUtil(database)
    private val syncManager = SyncManager(application, database)

    init {
        viewModelScope.launch {
            settingsRepository.getUserSettings().collectLatest {
                _userSettings.value = it
            }
        }
        viewModelScope.launch {
            settingsRepository.getSyncInterval().collectLatest { interval ->
                _syncIntervalMinutes.value = interval
            }
        }
        viewModelScope.launch {
            settingsRepository.getDefaultEventSettings().collectLatest { settings ->
                _defaultEventSettings.value = settings
            }
        }
        viewModelScope.launch {
            settingsRepository.getOnCalendarDeleteAction().collectLatest { action ->
                _onCalendarDeleteAction.value = action
            }
        }
        viewModelScope.launch {
            settingsRepository.getImportTargetCalendarId().collectLatest { calendarId ->
                _importTargetCalendarId.value = calendarId
            }
        }
        viewModelScope.launch {
            settingsRepository.getOnAppDeleteImportedAction().collectLatest { action ->
                _onAppDeleteImportedAction.value = action
            }
        }
        viewModelScope.launch {
            settingsRepository.getUseGoogleBackupMode().collectLatest { useBackup ->
                _useGoogleBackupMode.value = useBackup
                if (useBackup) {
                    updateImportTargetCalendarId(SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL)
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.getImportTargetCalendarIdForBothMode().collectLatest { calendarId ->
                _importTargetCalendarIdForBothMode.value = calendarId
            }
        }
        viewModelScope.launch {
            settingsRepository.getSelectedGoogleAccountName().collectLatest { accountName ->
                android.util.Log.d(
                    "MainViewModel",
                    "Collector got accountName from DataStore: $accountName"
                )
                _selectedGoogleAccountName.value = accountName
            }
        }

        viewModelScope.launch {
            settingsRepository.getEnabledTabs().collectLatest { tabIds ->
                _enabledTabs.value = tabIds?.let { AppTab.fromIds(it) }
            }
        }


        viewModelScope.launch {
            settingsRepository.getShowAllEvents().collectLatest { showAll ->
                _showAllEvents.value = showAll
            }
        }

        viewModelScope.launch {
            settingsRepository.getCalendarSyncAppToCalendar().collectLatest { enabled ->
                _calendarSyncAppToCalendarEnabled.value = enabled
            }
        }

        viewModelScope.launch {
            settingsRepository.getCalendarSyncCalendarToApp().collectLatest { enabled ->
                _calendarSyncCalendarToAppEnabled.value = enabled
            }
        }

        viewModelScope.launch {
            settingsRepository.getMapTrackingMode().collectLatest { mode ->
                _mapTrackingMode.value = mode
            }
        }
        viewModelScope.launch {
            settingsRepository.getFoodConsumedTrackingEnabled().collectLatest { enabled ->
                _foodConsumedTrackingEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            settingsRepository.getRoutineInsightsEnabled().collectLatest { enabled ->
                _routineInsightsEnabled.value = enabled
                refreshRoutineInsights()
            }
        }
        viewModelScope.launch {
            settingsRepository.getDriveBackupUri().collectLatest { uri ->
                _driveBackupUri.value = uri
            }
        }
        viewModelScope.launch {
            settingsRepository.getHasCompletedOnboarding().collectLatest { completed ->
                _hasCompletedOnboarding.value = completed
            }
        }
        viewModelScope.launch {
            settingsRepository.getEventSetNames().collectLatest { names ->
                _eventSetNames.value = names
            }
        }
        viewModelScope.launch {
            settingsRepository.getEventSetColors().collectLatest { colors ->
                _eventSetColors.value = colors
            }
        }
        viewModelScope.launch {
            settingsRepository.getEventSetsEnabled().collectLatest { enabled ->
                _eventSetsEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            settingsRepository.getRecentCustomEventColors().collectLatest { colors ->
                _recentCustomEventColors.value = colors
            }
        }

        viewModelScope.launch {
            settingsRepository.getAppThemeColors().collectLatest { colors ->
                _appThemeColors.value = colors
            }
        }
        viewModelScope.launch {
            settingsRepository.getEventIndicatorDisplayCondition().collectLatest { condition ->
                _eventIndicatorDisplayCondition.value = condition
            }
        }
        viewModelScope.launch {
            settingsRepository.getEventBackgroundDisplayCondition().collectLatest { condition ->
                _eventBackgroundDisplayCondition.value = condition
            }
        }
        viewModelScope.launch {
            settingsRepository.getEventBackgroundTransparency().collectLatest { transparency ->
                _eventBackgroundTransparency.value = transparency
            }
        }
        viewModelScope.launch {
            settingsRepository.getEventTitleColorChoice().collectLatest { choice ->
                _eventTitleColorChoice.value = choice
            }
        }
        viewModelScope.launch {
            settingsRepository.getEventTitleCustomColor().collectLatest { color ->
                _eventTitleCustomColor.value = color
            }
        }
        viewModelScope.launch {
            settingsRepository.getPastEventTextTreatment().collectLatest { treatment ->
                _pastEventTextTreatment.value = treatment
            }
        }
        viewModelScope.launch {
            settingsRepository.getPastEventTextCustomColor().collectLatest { color ->
                _pastEventTextCustomColor.value = color
            }
        }
        viewModelScope.launch {
            settingsRepository.getPastEventBackgroundTreatment().collectLatest { treatment ->
                _pastEventBackgroundTreatment.value = treatment
            }
        }
        viewModelScope.launch {
            settingsRepository.getPastEventBackgroundCustomColor().collectLatest { color ->
                _pastEventBackgroundCustomColor.value = color
            }
        }
        viewModelScope.launch {
            settingsRepository.getPastEventBackgroundTransparency().collectLatest { transparency ->
                _pastEventBackgroundTransparency.value = transparency
            }
        }
        viewModelScope.launch {
            settingsRepository.getDefaultActiveSetsByWeekday().collectLatest { defaults ->
                _defaultActiveSetsByWeekday.value = defaults
            }
        }

        viewModelScope.launch {
            runSessionRepository.activeRunState.collectLatest { state ->
                _activeRunState.value = state
                trackTrailForSession(state?.sessionId)
                trackSplitsForSession(state?.sessionId)
            }
        }

        applyDefaultActiveSetsForDate(_selectedDate.value)
        refreshScheduleItems()
        refreshRoutineInsights()
    }

    private fun trackTrailForSession(sessionId: String?) {
        trailJob?.cancel()
        if (sessionId == null) {
            _trailPoints.value = emptyList()
            return
        }
        trailJob = viewModelScope.launch {
            runSessionRepository.trailPoints(sessionId)
                .distinctUntilChanged()
                .collectLatest { points ->
                    _trailPoints.value = points.map { Point.fromLngLat(it.lng, it.lat) }
                }
        }
    }

    private fun trackSplitsForSession(sessionId: String?) {
        splitJob?.cancel()
        if (sessionId == null) {
            _splitDurations.value = emptyList()
            return
        }
        splitJob = viewModelScope.launch {
            runSessionRepository.splitDurations(sessionId)
                .distinctUntilChanged()
                .collectLatest { splits ->
                    _splitDurations.value = splits
                }
        }
    }

    fun startRun(activity: String, trackingMode: String) {
        val sessionId = UUID.randomUUID().toString()
        val startEpochMs = System.currentTimeMillis()
        val state = ActiveRunState(
            sessionId = sessionId,
            activity = activity,
            isRecording = true,
            trackingMode = trackingMode,
            startEpochMs = startEpochMs,
            distanceMeters = 0.0,
            durationSec = 0L,
            calories = 0.0,
            splitDurationsSec = emptyList()
        )
        _activeRunState.value = state
        _trailPoints.value = emptyList()
        _splitDurations.value = emptyList()
        viewModelScope.launch {
            runSessionRepository.saveActiveRunState(state)
            runSessionRepository.saveTrailPoints(sessionId, emptyList())
            runSessionRepository.saveSplitDurations(sessionId, emptyList())
        }
    }

    fun stopRun() {
        val state = _activeRunState.value ?: return
        val updated = state.copy(isRecording = false)
        _activeRunState.value = updated
        viewModelScope.launch {
            runSessionRepository.saveActiveRunState(updated)
        }
    }

    fun updateTrackingMode(trackingMode: String) {
        updateActiveRunState { it.copy(trackingMode = trackingMode) }
    }

    fun updateRunStats(distanceMeters: Double, durationSec: Long, calories: Double) {
        updateActiveRunState {
            it.copy(distanceMeters = distanceMeters, durationSec = durationSec, calories = calories)
        }
    }

    fun setTrailPoints(points: List<Point>) {
        val state = _activeRunState.value ?: return
        _trailPoints.value = points
        val stored = points.map { TrailPoint(lat = it.latitude(), lng = it.longitude()) }
        viewModelScope.launch {
            runSessionRepository.saveTrailPoints(state.sessionId, stored)
        }
    }

    fun setSplitDurations(splits: List<Long>) {
        val state = _activeRunState.value ?: return
        _splitDurations.value = splits
        updateActiveRunState { it.copy(splitDurationsSec = splits) }
        viewModelScope.launch {
            runSessionRepository.saveSplitDurations(state.sessionId, splits)
        }
    }

    fun discardRun() {
        val state = _activeRunState.value ?: return
        _activeRunState.value = null
        _trailPoints.value = emptyList()
        _splitDurations.value = emptyList()
        viewModelScope.launch {
            runSessionRepository.saveActiveRunState(null)
            runSessionRepository.clearTrailPoints(state.sessionId)
            runSessionRepository.clearSplitDurations(state.sessionId)
        }
    }

    private fun updateActiveRunState(transform: (ActiveRunState) -> ActiveRunState) {
        val current = _activeRunState.value ?: return
        val updated = transform(current)
        _activeRunState.value = updated
        viewModelScope.launch {
            runSessionRepository.saveActiveRunState(updated)
        }
    }

    fun saveUserSettings(userSettings: UserSettings) {
        viewModelScope.launch {
            settingsRepository.saveUserSettings(userSettings)
        }
    }

    fun saveMapTrackingMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.saveMapTrackingMode(mode)
        }
    }

    fun saveFoodConsumedTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveFoodConsumedTrackingEnabled(enabled)
        }
    }

    fun saveEnabledTabs(tabs: Set<AppTab>) {
        viewModelScope.launch {
            settingsRepository.saveEnabledTabs(tabs.map { it.id }.toSet())
            _enabledTabs.value = tabs
            if (!tabs.contains(AppTab.Workout)) {
                WorkManager.getInstance(getApplication())
                    .cancelUniqueWork(ExerciseDbDownloadWorker.UNIQUE_WORK_NAME)
            }
        }
    }

    fun onAppResumed() {
        refreshScheduleItems()
        viewModelScope.launch(Dispatchers.IO) {
            syncCalendarsIfNeeded()
        }
    }


    fun onAppPaused() {
        // This is now handled by the SyncWorker
    }

    fun currentUserWeightKgOrNull(): Double? = userSettings.value?.weightKg

    fun currentUserHeightCmOrNull(): Double? = userSettings.value?.heightCm

    fun currentUserAgeOrNull(): Int? = userSettings.value?.age

    fun currentUserGenderOrNull(): String? = userSettings.value?.gender?.let { gender ->
        when (gender) {
            Gender.MALE -> "Male"
            Gender.FEMALE -> "Female"
        }
    }

    fun promptUserForWeightOnce(context: Context) {
        // Hook up your own dialog flow here if needed.
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        applyDefaultActiveSetsForDate(date)
        refreshScheduleItems()
        refreshDoneStatesForDate(date.toEpochDay())
        refreshRoutineInsights()
    }


    fun selectPreviousDay() {
        val newDate = _selectedDate.value.minusDays(1)
        _selectedDate.value = newDate
        applyDefaultActiveSetsForDate(newDate)
        refreshScheduleItems()
        refreshDoneStatesForDate(newDate.toEpochDay())
        refreshRoutineInsights()
    }

    fun selectNextDay() {
        val newDate = _selectedDate.value.plusDays(1)
        _selectedDate.value = newDate
        applyDefaultActiveSetsForDate(newDate)
        refreshScheduleItems()
        refreshDoneStatesForDate(newDate.toEpochDay())
        refreshRoutineInsights()
    }

    fun toggleActiveSet(setId: Int): Boolean {
        if (!_eventSetsEnabled.value) return false
        if (setId !in _availableSetIds.value) return false
        val current = _activeSetIds.value.toMutableSet()
        val updated = if (setId in current) {
            current.remove(setId)
            current
        } else {
            if (current.size >= SettingsRepository.MAX_ACTIVE_SETS_PER_DAY) {
                return false
            }
            current.add(setId)
            current
        }
        _activeSetIds.value = updated
        _hasManualActiveSetsForDate.value = true
        persistActiveSetsForDate()
        return true
    }

    fun saveEventSetNames(names: List<String>) {
        viewModelScope.launch {
            settingsRepository.saveEventSetNames(names)
        }
    }

    fun saveEventSetColors(colors: List<Int>) {
        viewModelScope.launch {
            settingsRepository.saveEventSetColors(colors)
        }
    }

    fun updateEventSetsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveEventSetsEnabled(enabled)
        }
    }

    fun saveRecentCustomEventColor(colorArgb: Int) {
        viewModelScope.launch {
            settingsRepository.saveRecentCustomEventColor(colorArgb)
        }
    }

    fun saveAppThemeColors(colors: AppThemeColors) {
        viewModelScope.launch {
            settingsRepository.saveAppThemeColors(colors)
        }
    }

    fun saveEventIndicatorDisplayCondition(condition: EventColorDisplayCondition) {
        viewModelScope.launch {
            settingsRepository.saveEventIndicatorDisplayCondition(condition)
        }
    }

    fun saveEventBackgroundDisplayCondition(condition: EventColorDisplayCondition) {
        viewModelScope.launch {
            settingsRepository.saveEventBackgroundDisplayCondition(condition)
        }
    }

    fun saveEventBackgroundTransparency(transparency: EventBackgroundTransparency) {
        viewModelScope.launch {
            settingsRepository.saveEventBackgroundTransparency(transparency)
        }
    }

    fun saveEventTitleColorChoice(choice: EventTitleColorChoice) {
        viewModelScope.launch {
            settingsRepository.saveEventTitleColorChoice(choice)
        }
    }

    fun saveEventTitleCustomColor(colorArgb: Int) {
        viewModelScope.launch {
            settingsRepository.saveEventTitleCustomColor(colorArgb)
        }
    }

    fun savePastEventTextTreatment(treatment: PastEventColorTreatment) {
        viewModelScope.launch {
            settingsRepository.savePastEventTextTreatment(treatment)
        }
    }

    fun savePastEventTextCustomColor(colorArgb: Int) {
        viewModelScope.launch {
            settingsRepository.savePastEventTextCustomColor(colorArgb)
        }
    }

    fun savePastEventBackgroundTreatment(treatment: PastEventColorTreatment) {
        viewModelScope.launch {
            settingsRepository.savePastEventBackgroundTreatment(treatment)
        }
    }

    fun savePastEventBackgroundCustomColor(colorArgb: Int) {
        viewModelScope.launch {
            settingsRepository.savePastEventBackgroundCustomColor(colorArgb)
        }
    }

    fun savePastEventBackgroundTransparency(transparency: EventBackgroundTransparency) {
        viewModelScope.launch {
            settingsRepository.savePastEventBackgroundTransparency(transparency)
        }
    }

    fun saveDefaultActiveSetsByWeekday(selections: Map<DayOfWeek, Set<Int>>) {
        viewModelScope.launch {
            selections.forEach { (day, sets) ->
                settingsRepository.saveDefaultActiveSetsForWeekday(day, sets)
            }
        }
    }

    fun resetManualActiveEventSets() {
        viewModelScope.launch {
            settingsRepository.clearManualActiveEventSets()
            applyDefaultActiveSetsForDate(_selectedDate.value)
        }
    }

    fun resetManualActiveEventSetsForDate() {
        val date = _selectedDate.value
        viewModelScope.launch {
            settingsRepository.clearActiveSetsForDate(date)
            applyDefaultActiveSetsForDate(date)
        }
    }

    private fun applyDefaultActiveSetsForDate(date: LocalDate) {
        viewModelScope.launch {
            val saved = settingsRepository.getActiveSetsForDate(date).first()
            _hasManualActiveSetsForDate.value = saved != null
            _activeSetIds.value = saved
                ?: settingsRepository.getDefaultActiveSetsForWeekday(date.dayOfWeek).first()
        }
    }

    private fun persistActiveSetsForDate() {
        val date = _selectedDate.value
        val activeSets = _activeSetIds.value
        viewModelScope.launch {
            settingsRepository.saveActiveSetsForDate(date, activeSets)
        }
    }


    /**
     * Saves a schedule item:
     *  - If one-time or no repeat days -> insert once.
     *  - If recurring -> generate items only on selected weekdays,
     *    anchored at startEpochDay, and spaced by repeatEveryWeeks.
     */

    fun upsertScheduleItem(item: ScheduleItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete exact duplicates with same name/time and matching recurrence details.
            val existing = scheduleDao.getAllOnce().filter {
                it.name == item.name &&
                    it.hour == item.hour &&
                    it.minute == item.minute &&
                    it.origin == "APP_CREATED" &&
                    it.id != item.id
            }
            val duplicates = existing.filter { candidate ->
                if (item.isOneTime) {
                    candidate.isOneTime && candidate.dateEpochDay == item.dateEpochDay
                } else {
                    !candidate.isOneTime &&
                        candidate.startEpochDay == item.startEpochDay &&
                        candidate.repeatEveryWeeks == item.repeatEveryWeeks &&
                        candidate.repeatOnDays == item.repeatOnDays
                }
            }
            for (dup in duplicates) {
                scheduleDao.delete(dup.id)
            }

            // Ensure start date is valid
            val startDate = item.startEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
            val epochDay = startDate.toEpochDay()

            // Insert once
            val shouldSyncToCalendar = shouldSyncAppToCalendar() &&
                (item.addToCalendarOnSave || item.calendarEventId != null)
            val context = getApplication<Application>()
            val calendars = if (shouldSyncToCalendar && CalendarSyncManager.hasCalendarPermissions(context)) {
                CalendarSyncManager.queryCalendars(context)
            } else {
                emptyList()
            }
            val targetCalendarSystem = item.targetCalendarSystem ?: _defaultEventSettings.value.targetCalendarId
            val targetCalendarId = if (shouldSyncToCalendar && calendars.isNotEmpty()) {
                CalendarSyncManager.resolveTargetCalendarId(calendars, targetCalendarSystem, _selectedGoogleAccountName.value)
            } else {
                null
            }
            val matchingEventId = if (shouldSyncToCalendar && targetCalendarId != null && item.calendarEventId == null) {
                CalendarSyncManager.findMatchingEventId(context, targetCalendarId, item)
            } else {
                null
            }
            val calendarEventId = if (shouldSyncToCalendar && targetCalendarId != null && item.calendarEventId == null) {
                matchingEventId ?: CalendarSyncManager.upsertEvent(context, item, targetCalendarId)
            } else {
                item.calendarEventId
            }
            val resolvedOrigin = if (calendarEventId != null && item.origin == "APP_CREATED") {
                if (targetCalendarId != null && CalendarSyncManager.isGoogleCalendar(targetCalendarId, calendars)) {
                    "APP_CREATED_GOOGLE"
                } else {
                    "APP_CREATED_LOCAL"
                }
            } else {
                item.origin
            }
            val entity = item.copy(
                startEpochDay = epochDay,
                calendarEventId = calendarEventId,
                origin = resolvedOrigin,
                targetCalendarSystem = targetCalendarSystem
            ).toEntity()

            scheduleDao.upsert(entity)

            if (shouldSyncToCalendar && item.calendarEventId != null && targetCalendarId != null) {
                CalendarSyncManager.upsertEvent(
                    context,
                    item.copy(calendarEventId = item.calendarEventId),
                    targetCalendarId
                )
            }

            if (item.notifyEnabled) {
                notificationScheduler.scheduleSingleOccurrence(item, epochDay)
            }



            withContext(Dispatchers.Main) {
                refreshScheduleItems()
            }
        }
    }





    suspend fun isDoneForDate(id: Long, day: Long): Boolean {
        return scheduleDoneDao.isDone(id, day) > 0
    }

    val doneItemsForDay = MutableStateFlow<Map<Pair<Long, Long>, Boolean>>(emptyMap())


    fun refreshDoneStatesForDate(epochDay: Long) {
        viewModelScope.launch {
            val ids = scheduleDoneDao.getDoneStatesForDay(epochDay)

            // keep previous entries for other days
            val newMap = doneItemsForDay.value.toMutableMap()

            // overwrite only entries for this day
            // (scheduleId, epochDay) -> true
            ids.forEach { scheduleId ->
                newMap[scheduleId to epochDay] = true
            }

            // also remove entries for this day that are no longer done
            newMap.keys
                .filter { it.second == epochDay && it.first !in ids }
                .forEach { newMap.remove(it) }

            doneItemsForDay.value = newMap
        }
    }



    fun markScheduleItemDone(item: ScheduleItem, day: Long) {
        viewModelScope.launch {
            scheduleDoneDao.markDone(item.id, day)

            if (item.notifyEnabled) {
                notificationScheduler.cancelSingleOccurrence(item, day)
            }


            refreshDoneStatesForDate(day)
            refreshRoutineInsights()
        }
    }



    fun unmarkScheduleItemDone(item: ScheduleItem, day: Long) {
        viewModelScope.launch {
            scheduleDoneDao.unmarkDone(item.id, day)

            if (item.notifyEnabled) {
                notificationScheduler.scheduleSingleOccurrence(item, day)
            }

            refreshDoneStatesForDate(day)
            refreshRoutineInsights()
        }
    }






    fun deleteScheduleItem(item: ScheduleItem, deleteFromCalendar: Boolean) {
        viewModelScope.launch {
            scheduleDao.delete(item.id)
            if (deleteFromCalendar && item.calendarEventId != null) {
                CalendarSyncManager.deleteEvent(getApplication(), item.calendarEventId)
            }
            refreshScheduleItems()
        }
    }

    suspend fun getScheduleItemForEditing(itemId: Long): ScheduleItem? {
        return withContext(Dispatchers.IO) {
            scheduleDao.getItemById(itemId)?.toItem()
        }
    }

    fun updateSyncInterval(minutes: Long) {
        viewModelScope.launch {
            settingsRepository.saveSyncInterval(minutes)
        }
    }

    fun updateRoutineInsightsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveRoutineInsightsEnabled(enabled)
        }
    }

    fun saveDefaultEventSettings(settings: DefaultEventSettings) {
        viewModelScope.launch {
            settingsRepository.saveDefaultEventSettings(settings)
        }
    }

    fun updateOnCalendarDeleteAction(action: String) {
        viewModelScope.launch {
            settingsRepository.saveOnCalendarDeleteAction(action)
        }
    }

    fun updateImportTargetCalendarId(calendarId: String) {
        viewModelScope.launch {
            settingsRepository.saveImportTargetCalendarId(calendarId)
        }
    }

    fun updateOnAppDeleteImportedAction(action: String) {
        viewModelScope.launch {
            settingsRepository.saveOnAppDeleteImportedAction(action)
        }
    }

    fun updateUseGoogleBackupMode(useBackup: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveUseGoogleBackupMode(useBackup)
        }
    }

    fun updateImportTargetCalendarIdForBothMode(calendarId: String) {
        viewModelScope.launch {
            settingsRepository.saveImportTargetCalendarIdForBothMode(calendarId)
        }
    }

    fun updateCalendarSyncAppToCalendar(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveCalendarSyncAppToCalendar(enabled)
        }
    }

    fun updateCalendarSyncCalendarToApp(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveCalendarSyncCalendarToApp(enabled)
        }
    }

    fun unblockCalendarEvent(calendarEventId: Long) {
        viewModelScope.launch {
            blockedCalendarEventDao.removeBlockedImport(calendarEventId)
        }
    }

    fun updateSelectedGoogleAccountName(accountName: String?) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "updateSelectedGoogleAccountName called with: $accountName")

            settingsRepository.saveSelectedGoogleAccountName(accountName)
            _selectedGoogleAccountName.value = accountName
            android.util.Log.d("MainViewModel", "StateFlow updated: ${_selectedGoogleAccountName.value}")
        }
    }

    fun updateDriveBackupUri(uri: String?) {
        viewModelScope.launch {
            settingsRepository.saveDriveBackupUri(uri)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.saveHasCompletedOnboarding(true)
        }
    }

    suspend fun exportDataToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                exportImportUtil.exportAllDataToStream(stream)
            } ?: return@runCatching false
            true
        }.getOrElse { false }
    }

    suspend fun importDataFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                exportImportUtil.importAllDataFromStream(stream)
            } ?: return@runCatching false
        }.getOrElse { false }
    }

    suspend fun backupToDrive(context: Context, uri: Uri): Boolean {
        syncManager.setCloudProvider(DriveAppDataProvider(context, uri))
        return syncManager.performBackup()
    }

    suspend fun restoreFromDrive(context: Context, uri: Uri): Boolean {
        syncManager.setCloudProvider(DriveAppDataProvider(context, uri))
        return syncManager.performRestore()
    }



    fun updateShowAllEvents(showAll: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveShowAllEvents(showAll)
        }
    }

    fun unlinkCalendar(source: String, accountName: String?) {
        viewModelScope.launch {
            // adapt if you support unlinking external calendars
            refreshScheduleItems()
        }
    }

    /**
     * Load items from DB and expose only those that occur on the currently selected date.
     * This is the crucial fix: previously we exposed ALL items, making them appear every day.
     */
    private fun refreshScheduleItems() {
        // Cancel any previous collector to avoid duplication
        syncJob?.cancel()

        // Needed import: import kotlinx.coroutines.flow.combine
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.flow.combine(
                scheduleDao.observeAll(),
                _selectedDate,
                _activeSetIds,
                _eventSetsEnabled
            ) { entities: List<com.example.routinereminder.data.entities.Schedule>, date: LocalDate, activeSetIds: Set<Int>, eventSetsEnabled: Boolean ->
                val itemsForDate = entities
                    .map { it.toItem() }
                    .filter { item -> item.occursOnDate(date) }
                    .distinctBy { item -> item.id }
                val availableSetIds = itemsForDate.mapNotNull { it.setId }.toSet()
                val effectiveActiveSetIds = if (eventSetsEnabled) {
                    activeSetIds.intersect(availableSetIds)
                } else {
                    availableSetIds
                }

                ScheduleFilterState(
                    itemsForDate = itemsForDate,
                    availableSetIds = availableSetIds,
                    effectiveActiveSetIds = effectiveActiveSetIds,
                    eventSetsEnabled = eventSetsEnabled
                )
            }.collectLatest { (itemsForDate, availableSetIds, effectiveActiveSetIds, eventSetsEnabled) ->
                if (_availableSetIds.value != availableSetIds) {
                    _availableSetIds.value = availableSetIds
                }
                if (eventSetsEnabled && _activeSetIds.value != effectiveActiveSetIds) {
                    _activeSetIds.value = effectiveActiveSetIds
                    if (_hasManualActiveSetsForDate.value) {
                        settingsRepository.saveActiveSetsForDate(_selectedDate.value, effectiveActiveSetIds)
                    }
                }

                updateNotificationsForSetSelection(itemsForDate, effectiveActiveSetIds, _selectedDate.value)

                val filtered = itemsForDate
                    .filter { item -> item.setId == null || item.setId in effectiveActiveSetIds }
                    .sortedWith(
                        compareBy<ScheduleItem> { it.durationMinutes }
                            .thenBy { item -> item.hour * 60 + item.minute }
                    )

                withContext(Dispatchers.Main) {
                    _scheduleItems.value = filtered
                }
                refreshRoutineInsights()
            }
        }
    }

    private fun refreshRoutineInsights() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_routineInsightsEnabled.value) {
                _routineInsights.value = null
                return@launch
            }
            val items = scheduleDao.getAllOnce().map { it.toItem() }
            val insights = calculateRoutineInsights(items, _selectedDate.value)
            withContext(Dispatchers.Main) {
                _routineInsights.value = insights
            }
        }
    }

    private suspend fun calculateRoutineInsights(
        items: List<ScheduleItem>,
        endDate: LocalDate
    ): RoutineInsights {
        val weeklyDays = (0..6).map { endDate.minusDays(it.toLong()) }
        var totalScheduled = 0
        var totalCompleted = 0
        val dailyRatios = mutableListOf<Double>()

        weeklyDays.forEach { day ->
            val itemsForDay = items.filter { it.occursOnDate(day) }
            if (itemsForDay.isEmpty()) return@forEach
            val doneIds = scheduleDoneDao.getDoneStatesForDay(day.toEpochDay()).toSet()
            val doneCount = itemsForDay.count { it.id in doneIds }
            totalScheduled += itemsForDay.size
            totalCompleted += doneCount
            dailyRatios.add(doneCount.toDouble() / itemsForDay.size.toDouble())
        }

        val weeklyAdherencePercent = if (totalScheduled > 0) {
            ((totalCompleted.toDouble() / totalScheduled.toDouble()) * 100).roundToInt()
        } else {
            0
        }
        val consistencyScorePercent = if (dailyRatios.isNotEmpty()) {
            (dailyRatios.average() * 100).roundToInt()
        } else {
            0
        }

        var streakDays = 0
        var dayOffset = 0L
        while (true) {
            val day = endDate.minusDays(dayOffset)
            val itemsForDay = items.filter { it.occursOnDate(day) }
            if (itemsForDay.isEmpty()) break
            val doneIds = scheduleDoneDao.getDoneStatesForDay(day.toEpochDay()).toSet()
            val doneCount = itemsForDay.count { it.id in doneIds }
            if (doneCount == itemsForDay.size) {
                streakDays += 1
                dayOffset += 1
            } else {
                break
            }
        }

        return RoutineInsights(
            weeklyAdherencePercent = weeklyAdherencePercent,
            consistencyScorePercent = consistencyScorePercent,
            currentStreakDays = streakDays,
            totalScheduled = totalScheduled,
            totalCompleted = totalCompleted
        )
    }

    private suspend fun updateNotificationsForSetSelection(
        itemsForDate: List<ScheduleItem>,
        activeSetIds: Set<Int>,
        date: LocalDate
    ) {
        val epochDay = date.toEpochDay()
        val doneIds = scheduleDoneDao.getDoneStatesForDay(epochDay).toSet()
        itemsForDate.forEach { item ->
            if (!item.notifyEnabled) return@forEach
            val isActive = item.setId == null || item.setId in activeSetIds
            val isDone = item.id in doneIds
            if (isActive && !isDone) {
                notificationScheduler.scheduleSingleOccurrence(item, epochDay)
            } else {
                notificationScheduler.cancelSingleOccurrence(item, epochDay)
            }
        }
    }





    fun onSelectGoogleCalendarOption() {
        viewModelScope.launch {
            showGoogleCalendarChooser.send(Unit)
        }
    }

    private suspend fun syncCalendarsIfNeeded() {
        if (isSyncing) return
        if (_useGoogleBackupMode.value) return
        if (!_calendarSyncAppToCalendarEnabled.value && !_calendarSyncCalendarToAppEnabled.value) return
        val context = getApplication<Application>()
        if (!CalendarSyncManager.hasCalendarPermissions(context)) {
            requestCalendarPermission.send(Unit)
            return
        }
        isSyncing = true
        try {
            val calendars = CalendarSyncManager.queryCalendars(context)
            if (_calendarSyncCalendarToAppEnabled.value) {
                syncFromCalendarToApp(context, calendars)
            }
            if (_calendarSyncAppToCalendarEnabled.value) {
                syncFromAppToCalendar(context, calendars)
            }
        } finally {
            isSyncing = false
        }
    }

    private suspend fun syncFromCalendarToApp(context: Context, calendars: List<CalendarMeta>) {
        val importTarget = _importTargetCalendarId.value
        val importTargetBoth = _importTargetCalendarIdForBothMode.value
        val calendarIds = CalendarSyncManager.resolveImportCalendarIds(
            calendars,
            importTarget,
            importTargetBoth,
            _selectedGoogleAccountName.value
        )
        if (calendarIds.isEmpty()) return
        val nowMillis = System.currentTimeMillis()
        val rangeStartMillis = nowMillis - (30L * 24 * 60 * 60 * 1000)
        val rangeEndMillis = nowMillis + (365L * 24 * 60 * 60 * 1000)
        val events = CalendarSyncManager.queryEvents(context, calendarIds, rangeStartMillis, rangeEndMillis)
        val calendarMetaMap = calendars.associateBy { it.id }
        val scheduleItems = scheduleDao.getAllOnce().map { it.toItem() }.toMutableList()

        events.forEach { event ->
            val existing = scheduleItems.firstOrNull { it.calendarEventId == event.id }
            val eventItem = CalendarSyncManager.eventToScheduleItem(
                event = event,
                calendarMetaMap = calendarMetaMap,
                existingId = existing?.id ?: 0,
                existingOrigin = existing?.origin,
                existingTargetCalendarSystem = existing?.targetCalendarSystem,
                existingColorArgb = existing?.colorArgb
            )
            if (existing != null) {
                if (!isSameContent(existing, eventItem)) {
                    scheduleDao.upsert(eventItem.toEntity())
                    scheduleItems.remove(existing)
                    scheduleItems.add(eventItem)
                }
            } else {
                val matching = scheduleItems.firstOrNull { candidate ->
                    candidate.calendarEventId == null && isSameContent(candidate, eventItem)
                }
                if (matching != null) {
                    val linkedOrigin = if (matching.origin == "APP_CREATED") {
                        if (CalendarSyncManager.isGoogleCalendar(event.calendarId, calendars)) {
                            "APP_CREATED_GOOGLE"
                        } else {
                            "APP_CREATED_LOCAL"
                        }
                    } else {
                        matching.origin
                    }
                    val linked = matching.copy(
                        calendarEventId = event.id,
                        origin = linkedOrigin,
                        targetCalendarSystem = eventItem.targetCalendarSystem
                    )
                    scheduleDao.upsert(linked.toEntity())
                    scheduleItems.remove(matching)
                    scheduleItems.add(linked)
                } else {
                    scheduleDao.upsert(eventItem.toEntity())
                    scheduleItems.add(eventItem)
                }
            }
        }

        val deleteAction = _onCalendarDeleteAction.value
        scheduleItems.filter { it.calendarEventId != null }.forEach { item ->
            val eventId = item.calendarEventId ?: return@forEach
            if (!CalendarSyncManager.eventExists(context, eventId)) {
                if (deleteAction == SettingsRepository.ACTION_DELETE_FROM_APP) {
                    scheduleDao.delete(item.id)
                }
            }
        }
    }

    private suspend fun syncFromAppToCalendar(context: Context, calendars: List<CalendarMeta>) {
        val scheduleItems = scheduleDao.getAllOnce().map { it.toItem() }
        scheduleItems.forEach { item ->
            if (!item.addToCalendarOnSave && item.calendarEventId == null) return@forEach
            val targetSystem = item.targetCalendarSystem ?: _defaultEventSettings.value.targetCalendarId
            val calendarId = CalendarSyncManager.resolveTargetCalendarId(
                calendars,
                targetSystem,
                _selectedGoogleAccountName.value
            ) ?: return@forEach
            if (item.calendarEventId == null) {
                val existingEventId = CalendarSyncManager.findMatchingEventId(context, calendarId, item)
                val createdEventId = existingEventId ?: CalendarSyncManager.upsertEvent(
                    context,
                    item,
                    calendarId
                )
                if (createdEventId != null) {
                    val origin = if (item.origin == "APP_CREATED") {
                        if (CalendarSyncManager.isGoogleCalendar(calendarId, calendars)) {
                            "APP_CREATED_GOOGLE"
                        } else {
                            "APP_CREATED_LOCAL"
                        }
                    } else {
                        item.origin
                    }
                    val updated = item.copy(
                        calendarEventId = createdEventId,
                        origin = origin,
                        targetCalendarSystem = targetSystem
                    )
                    scheduleDao.upsert(updated.toEntity())
                }
            } else {
                CalendarSyncManager.upsertEvent(context, item, calendarId)
            }
        }
    }

    private fun shouldSyncAppToCalendar(): Boolean {
        return !_useGoogleBackupMode.value && _calendarSyncAppToCalendarEnabled.value
    }

    private fun isSameContent(left: ScheduleItem, right: ScheduleItem): Boolean {
        return left.name == right.name &&
            left.notes == right.notes &&
            left.hour == right.hour &&
            left.minute == right.minute &&
            left.durationMinutes == right.durationMinutes &&
            left.isOneTime == right.isOneTime &&
            left.dateEpochDay == right.dateEpochDay &&
            left.startEpochDay == right.startEpochDay &&
            left.repeatOnDays == right.repeatOnDays &&
            left.repeatEveryWeeks == right.repeatEveryWeeks &&
            left.colorArgb == right.colorArgb
    }
}

data class CalendarEventCounts(
    val local: Int = 0,
    val google: Map<String, Int> = emptyMap()
)
