package com.example.routinereminder.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.example.routinereminder.location.TrackingService

class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val USER_WEIGHT_KG = doublePreferencesKey("user_weight_kg")
    val USER_HEIGHT_CM = doublePreferencesKey("user_height_cm")
    val USER_AGE = intPreferencesKey("user_age")
    val USER_GENDER = stringPreferencesKey("user_gender")
    val USER_ACTIVITY_LEVEL = stringPreferencesKey("user_activity_level")

    val SYNC_INTERVAL_MINUTES = longPreferencesKey("sync_interval_minutes")
    val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
    val DEFAULT_EVENT_HOUR = intPreferencesKey("default_event_hour")
    val DEFAULT_EVENT_MINUTE = intPreferencesKey("default_event_minute")
    val DEFAULT_EVENT_DURATION_HOURS_KEY = intPreferencesKey("default_event_duration_hours")
    val DEFAULT_EVENT_DURATION_MINUTES_KEY = intPreferencesKey("default_event_duration_minutes")
    val DEFAULT_EVENT_IS_ONE_TIME = booleanPreferencesKey("default_event_is_one_time")
    val DEFAULT_EVENT_CREATE_CALENDAR_ENTRY = booleanPreferencesKey("default_event_create_calendar_entry")
    val DEFAULT_EVENT_SYSTEM_NOTIFICATION = booleanPreferencesKey("default_event_system_notification")
    val DEFAULT_EVENT_SHOW_DETAILS_IN_NOTIFICATION = booleanPreferencesKey("default_event_show_details_in_notification")
    val START_TIME_OPTION_NAME = stringPreferencesKey("start_time_option_name")
    val TARGET_CALENDAR_ID = stringPreferencesKey("target_calendar_id")
    val ON_CALENDAR_DELETE_ACTION = stringPreferencesKey("on_calendar_delete_action")
    val IMPORT_TARGET_CALENDAR_ID = stringPreferencesKey("import_target_calendar_id")
    val ON_APP_DELETE_IMPORTED_ACTION = stringPreferencesKey("on_app_delete_imported_action")
    val USE_GOOGLE_BACKUP_MODE = booleanPreferencesKey("use_google_backup_mode")
    val IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE = stringPreferencesKey("import_target_calendar_id_for_both_mode")
    val SELECTED_GOOGLE_ACCOUNT_NAME = stringPreferencesKey("selected_google_account_name")
    val SHOW_ALL_EVENTS = booleanPreferencesKey("show_all_events")
    val ENABLED_TABS = stringSetPreferencesKey("enabled_tabs")
    val EXERCISE_DB_LAST_REFRESH = longPreferencesKey("exercise_db_last_refresh_epoch_ms")
    val EXERCISE_DB_LAST_PROMPT = longPreferencesKey("exercise_db_last_prompt_epoch_ms")
    val EXERCISE_DB_CACHE_COMPLETE = booleanPreferencesKey("exercise_db_cache_complete")
    val EXERCISE_DB_CACHE_TOTAL = intPreferencesKey("exercise_db_cache_total")
    val MAP_TRACKING_MODE = stringPreferencesKey("map_tracking_mode")

    companion object {
        const val ACTION_KEEP_IN_APP = "KEEP_IN_APP"
        const val ACTION_DELETE_FROM_APP = "DELETE_FROM_APP"
        const val IMPORT_TARGET_CALENDAR_LOCAL = "local"
        const val IMPORT_TARGET_CALENDAR_PRIMARY = "google_primary"
        const val IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY = "google_selected_account_primary"
        const val ACTION_APP_DELETE_KEEPS_CALENDAR = "APP_DELETE_KEEPS_CALENDAR"
        const val ACTION_APP_DELETE_DELETES_CALENDAR = "APP_DELETE_DELETES_CALENDAR"
        const val ACTION_APP_DELETE_ALWAYS_ASK = "APP_DELETE_ALWAYS_ASK"
        const val DEFAULT_IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE = "google_primary"
        const val DEFAULT_EVENT_DURATION_HOURS = 1
        const val DEFAULT_EVENT_DURATION_MINUTES = 0
    }

    suspend fun saveUserSettings(userSettings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[USER_WEIGHT_KG] = userSettings.weightKg
            preferences[USER_HEIGHT_CM] = userSettings.heightCm
            preferences[USER_AGE] = userSettings.age
            preferences[USER_GENDER] = userSettings.gender.name
            preferences[USER_ACTIVITY_LEVEL] = userSettings.activityLevel.name
        }
    }

    fun getUserSettings(): Flow<UserSettings?> {
        return dataStore.data.map { preferences ->
            val weightKg = preferences[USER_WEIGHT_KG]
            val heightCm = preferences[USER_HEIGHT_CM]
            val age = preferences[USER_AGE]
            val gender = preferences[USER_GENDER]?.let { Gender.valueOf(it) }
            val activityLevel = preferences[USER_ACTIVITY_LEVEL]?.let { ActivityLevel.valueOf(it) }

            if (weightKg != null && heightCm != null && age != null && gender != null && activityLevel != null) {
                UserSettings(
                    weightKg = weightKg,
                    heightCm = heightCm,
                    age = age,
                    gender = gender,
                    activityLevel = activityLevel
                )
            } else {
                null
            }
        }
    }

    suspend fun saveSyncInterval(minutes: Long) {
        dataStore.edit { preferences ->
            preferences[SYNC_INTERVAL_MINUTES] = minutes
        }
    }

    fun getSyncInterval(): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[SYNC_INTERVAL_MINUTES] ?: 60L
        }
    }

    suspend fun saveCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CLOUD_SYNC_ENABLED] = enabled
        }
    }

    fun getCloudSyncEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[CLOUD_SYNC_ENABLED] ?: false
        }
    }

    suspend fun saveMapTrackingMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[MAP_TRACKING_MODE] = mode
        }
    }

    fun getMapTrackingMode(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[MAP_TRACKING_MODE] ?: TrackingService.MODE_BALANCED
        }
    }

    suspend fun saveDefaultEventSettings(settings: DefaultEventSettings) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_EVENT_HOUR] = settings.hour
            preferences[DEFAULT_EVENT_MINUTE] = settings.minute
            preferences[DEFAULT_EVENT_DURATION_HOURS_KEY] = settings.durationHours
            preferences[DEFAULT_EVENT_DURATION_MINUTES_KEY] = settings.durationMinutes
            preferences[DEFAULT_EVENT_IS_ONE_TIME] = settings.isOneTime
            preferences[DEFAULT_EVENT_CREATE_CALENDAR_ENTRY] = settings.createCalendarEntry
            preferences[DEFAULT_EVENT_SYSTEM_NOTIFICATION] = settings.systemNotification
            preferences[DEFAULT_EVENT_SHOW_DETAILS_IN_NOTIFICATION] = settings.showDetailsInNotification
            preferences[START_TIME_OPTION_NAME] = settings.startTimeOptionName
            preferences[TARGET_CALENDAR_ID] = settings.targetCalendarId
        }
    }

    fun getDefaultEventSettings(): Flow<DefaultEventSettings> {
        return dataStore.data.map { preferences ->
            DefaultEventSettings(
                hour = preferences[DEFAULT_EVENT_HOUR] ?: 8,
                minute = preferences[DEFAULT_EVENT_MINUTE] ?: 0,
                durationHours = preferences[DEFAULT_EVENT_DURATION_HOURS_KEY] ?: DEFAULT_EVENT_DURATION_HOURS,
                durationMinutes = preferences[DEFAULT_EVENT_DURATION_MINUTES_KEY] ?: DEFAULT_EVENT_DURATION_MINUTES,
                isOneTime = preferences[DEFAULT_EVENT_IS_ONE_TIME] ?: false,
                createCalendarEntry = preferences[DEFAULT_EVENT_CREATE_CALENDAR_ENTRY] ?: true,
                systemNotification = preferences[DEFAULT_EVENT_SYSTEM_NOTIFICATION] ?: true,
                showDetailsInNotification = preferences[DEFAULT_EVENT_SHOW_DETAILS_IN_NOTIFICATION] ?: true,
                startTimeOptionName = preferences[START_TIME_OPTION_NAME] ?: "SPECIFIC_TIME",
                targetCalendarId = preferences[TARGET_CALENDAR_ID] ?: IMPORT_TARGET_CALENDAR_LOCAL
            )
        }
    }

    suspend fun saveOnCalendarDeleteAction(action: String) {
        dataStore.edit { preferences ->
            preferences[ON_CALENDAR_DELETE_ACTION] = action
        }
    }

    fun getOnCalendarDeleteAction(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[ON_CALENDAR_DELETE_ACTION] ?: ACTION_KEEP_IN_APP
        }
    }

    suspend fun saveImportTargetCalendarId(calendarId: String) {
        dataStore.edit { preferences ->
            preferences[IMPORT_TARGET_CALENDAR_ID] = calendarId
        }
    }

    fun getImportTargetCalendarId(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[IMPORT_TARGET_CALENDAR_ID] ?: IMPORT_TARGET_CALENDAR_LOCAL
        }
    }

    suspend fun saveOnAppDeleteImportedAction(action: String) {
        dataStore.edit { preferences ->
            preferences[ON_APP_DELETE_IMPORTED_ACTION] = action
        }
    }

    fun getOnAppDeleteImportedAction(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[ON_APP_DELETE_IMPORTED_ACTION] ?: ACTION_APP_DELETE_KEEPS_CALENDAR
        }
    }

    suspend fun saveUseGoogleBackupMode(useBackup: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_GOOGLE_BACKUP_MODE] = useBackup
        }
    }

    fun getUseGoogleBackupMode(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[USE_GOOGLE_BACKUP_MODE] ?: true
        }
    }

    suspend fun saveImportTargetCalendarIdForBothMode(calendarId: String) {
        dataStore.edit { preferences ->
            preferences[IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE] = calendarId
        }
    }

    fun getImportTargetCalendarIdForBothMode(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE] ?: DEFAULT_IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE
        }
    }

    suspend fun saveSelectedGoogleAccountName(accountName: String?) {
        dataStore.edit { preferences ->
            if (accountName == null) {
                preferences.remove(SELECTED_GOOGLE_ACCOUNT_NAME)
            } else {
                preferences[SELECTED_GOOGLE_ACCOUNT_NAME] = accountName
            }
        }
    }

    fun getSelectedGoogleAccountName(): Flow<String?> =
        dataStore.data.map { prefs -> prefs[SELECTED_GOOGLE_ACCOUNT_NAME] }.distinctUntilChanged()


    suspend fun saveShowAllEvents(showAll: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_ALL_EVENTS] = showAll
        }
    }

    suspend fun saveExerciseDbDownloadAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXERCISE_DB_DOWNLOAD_ACCEPTED] = accepted
        }
    }

    fun getExerciseDbDownloadAccepted(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[EXERCISE_DB_DOWNLOAD_ACCEPTED] ?: false
        }
    }

    fun getShowAllEvents(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[SHOW_ALL_EVENTS] ?: false
        }
    }

    suspend fun saveEnabledTabs(tabIds: Set<String>) {
        dataStore.edit { preferences ->
            preferences[ENABLED_TABS] = tabIds
        }
    }

    fun getEnabledTabs(): Flow<Set<String>?> {
        return dataStore.data.map { preferences ->
            preferences[ENABLED_TABS]
        }
    }

    suspend fun saveExerciseDbLastRefresh(epochMillis: Long) {
        dataStore.edit { preferences ->
            preferences[EXERCISE_DB_LAST_REFRESH] = epochMillis
        }
    }

    fun getExerciseDbLastRefresh(): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[EXERCISE_DB_LAST_REFRESH] ?: 0L
        }
    }

    suspend fun saveExerciseDbLastPrompt(epochMillis: Long) {
        dataStore.edit { preferences ->
            preferences[EXERCISE_DB_LAST_PROMPT] = epochMillis
        }
    }

    fun getExerciseDbLastPrompt(): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[EXERCISE_DB_LAST_PROMPT] ?: 0L
        }
    }

    suspend fun saveExerciseDbCacheComplete(isComplete: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXERCISE_DB_CACHE_COMPLETE] = isComplete
        }
    }

    fun getExerciseDbCacheComplete(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[EXERCISE_DB_CACHE_COMPLETE] ?: false
        }
    }

    suspend fun saveExerciseDbCacheTotal(total: Int?) {
        dataStore.edit { preferences ->
            if (total == null) {
                preferences.remove(EXERCISE_DB_CACHE_TOTAL)
            } else {
                preferences[EXERCISE_DB_CACHE_TOTAL] = total
            }
        }
    }

    fun getExerciseDbCacheTotal(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[EXERCISE_DB_CACHE_TOTAL]
        }
    }
}
