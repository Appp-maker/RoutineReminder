package com.example.routinereminder.ui
import com.example.routinereminder.data.entities.ScheduleDone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.DefaultEventSettings
import com.example.routinereminder.data.Gender
import com.example.routinereminder.data.RunSessionRepository
import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.data.SettingsRepository
import com.example.routinereminder.data.UserSettings
import com.example.routinereminder.data.model.ActiveRunState
import com.example.routinereminder.data.model.TrailPoint
import com.example.routinereminder.data.mappers.toEntity
import com.example.routinereminder.data.mappers.toItem
import com.example.routinereminder.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.UUID
import org.maplibre.geojson.Point
import android.content.Context

data class CalendarInfo(val id: String, val displayName: String, val accountName: String)

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

    private val _useGoogleBackupMode = MutableStateFlow(true)
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

    private val _enabledTabs = MutableStateFlow<Set<AppTab>?>(null)
    val enabledTabs: StateFlow<Set<AppTab>?> = _enabledTabs.asStateFlow()

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

    private var trailJob: Job? = null

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
            settingsRepository.getShowAllEvents().collectLatest { showAll ->
                _showAllEvents.value = showAll
            }
        }

        viewModelScope.launch {
            runSessionRepository.activeRunState.collectLatest { state ->
                _activeRunState.value = state
                trackTrailForSession(state?.sessionId)
            }
        }

        refreshScheduleItems()
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
            calories = 0.0
        )
        _activeRunState.value = state
        _trailPoints.value = emptyList()
        viewModelScope.launch {
            runSessionRepository.saveActiveRunState(state)
            runSessionRepository.saveTrailPoints(sessionId, emptyList())
        }
    }

    fun resumeRun() {
        val state = _activeRunState.value ?: return
        if (state.isRecording) return
        val updated = state.copy(isRecording = true)
        _activeRunState.value = updated
        viewModelScope.launch {
            runSessionRepository.saveActiveRunState(updated)
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

    fun discardRun() {
        val state = _activeRunState.value ?: return
        _activeRunState.value = null
        _trailPoints.value = emptyList()
        viewModelScope.launch {
            runSessionRepository.saveActiveRunState(null)
            runSessionRepository.clearTrailPoints(state.sessionId)
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

    fun saveEnabledTabs(tabs: Set<AppTab>) {
        viewModelScope.launch {
            settingsRepository.saveEnabledTabs(tabs.map { it.id }.toSet())
        }
    }

    fun onAppResumed() {
        refreshScheduleItems()

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
        refreshScheduleItems()
        refreshDoneStatesForDate(date.toEpochDay())
    }


    fun selectPreviousDay() {
        val newDate = _selectedDate.value.minusDays(1)
        _selectedDate.value = newDate
        refreshScheduleItems()
        refreshDoneStatesForDate(newDate.toEpochDay())
    }

    fun selectNextDay() {
        val newDate = _selectedDate.value.plusDays(1)
        _selectedDate.value = newDate
        refreshScheduleItems()
        refreshDoneStatesForDate(newDate.toEpochDay())
    }


    /**
     * Saves a schedule item:
     *  - If one-time or no repeat days -> insert once.
     *  - If recurring -> generate items only on selected weekdays,
     *    anchored at startEpochDay, and spaced by repeatEveryWeeks.
     */

    fun upsertScheduleItem(item: ScheduleItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete any duplicates with same name + time (same origin)
            val existing = scheduleDao.getAllOnce().filter {
                it.name == item.name &&
                        it.hour == item.hour &&
                        it.minute == item.minute &&
                        it.origin == "APP_CREATED"
            }
            for (dup in existing) {
                scheduleDao.delete(dup.id)
            }

            // Ensure start date is valid
            val startDate = item.startEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
            val epochDay = startDate.toEpochDay()

            // Insert once
            val entity = item.copy(
                origin = "APP_CREATED",
                startEpochDay = epochDay
            ).toEntity()

            scheduleDao.upsert(entity)

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
        }
    }



    fun unmarkScheduleItemDone(item: ScheduleItem, day: Long) {
        viewModelScope.launch {
            scheduleDoneDao.unmarkDone(item.id, day)

            if (item.notifyEnabled) {
                notificationScheduler.scheduleSingleOccurrence(item, day)
            }

            refreshDoneStatesForDate(day)
        }
    }






    fun deleteScheduleItem(item: ScheduleItem, deleteFromCalendar: Boolean) {
        viewModelScope.launch {
            scheduleDao.delete(item.id)
            if (deleteFromCalendar && item.calendarEventId != null) {
                // TODO: delete from external calendar if you store calendarEventId
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
                _selectedDate
            ) { entities: List<com.example.routinereminder.data.entities.Schedule>, date: LocalDate ->
                entities
                    .map { it.toItem() }
                    .filter { item -> item.occursOnDate(date) }
                    .distinctBy { item -> item.id }
                    .sortedBy { item -> item.hour * 60 + item.minute }
            }.collectLatest { filtered: List<ScheduleItem> ->
                withContext(Dispatchers.Main) {
                    _scheduleItems.value = filtered
                }
            }
        }
    }





    fun onSelectGoogleCalendarOption() {
        viewModelScope.launch {
            showGoogleCalendarChooser.send(Unit)
        }
    }
}

data class CalendarEventCounts(
    val local: Int = 0,
    val google: Map<String, Int> = emptyMap()
)
