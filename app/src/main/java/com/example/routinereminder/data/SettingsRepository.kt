package com.example.routinereminder.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import com.example.routinereminder.location.TrackingService

class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val USER_WEIGHT_KG = doublePreferencesKey("user_weight_kg")
    val USER_HEIGHT_CM = doublePreferencesKey("user_height_cm")
    val USER_AGE = intPreferencesKey("user_age")
    val USER_GENDER = stringPreferencesKey("user_gender")
    val USER_ACTIVITY_LEVEL = stringPreferencesKey("user_activity_level")
    val USER_CUSTOM_CALORIES_TARGET = doublePreferencesKey("user_custom_calories_target")
    val USER_CUSTOM_PROTEIN_TARGET = doublePreferencesKey("user_custom_protein_target_g")
    val USER_CUSTOM_CARBS_TARGET = doublePreferencesKey("user_custom_carbs_target_g")
    val USER_CUSTOM_FAT_TARGET = doublePreferencesKey("user_custom_fat_target_g")
    val USER_CUSTOM_FIBER_TARGET = doublePreferencesKey("user_custom_fiber_target_g")
    val USER_CUSTOM_SATURATED_FAT_TARGET = doublePreferencesKey("user_custom_saturated_fat_target_g")
    val USER_CUSTOM_ADDED_SUGARS_TARGET = doublePreferencesKey("user_custom_added_sugars_target_g")
    val USER_CUSTOM_SODIUM_TARGET = doublePreferencesKey("user_custom_sodium_target_mg")
    val USER_CALORIE_GOAL = stringPreferencesKey("user_calorie_goal")

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
    val DEFAULT_EVENT_REMINDER_COUNT = intPreferencesKey("default_event_reminder_count")
    val DEFAULT_EVENT_REMINDER_INTERVAL_MINUTES = intPreferencesKey("default_event_reminder_interval_minutes")
    val START_TIME_OPTION_NAME = stringPreferencesKey("start_time_option_name")
    val TARGET_CALENDAR_ID = stringPreferencesKey("target_calendar_id")
    val ON_CALENDAR_DELETE_ACTION = stringPreferencesKey("on_calendar_delete_action")
    val IMPORT_TARGET_CALENDAR_ID = stringPreferencesKey("import_target_calendar_id")
    val ON_APP_DELETE_IMPORTED_ACTION = stringPreferencesKey("on_app_delete_imported_action")
    val USE_GOOGLE_BACKUP_MODE = booleanPreferencesKey("use_google_backup_mode")
    val IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE = stringPreferencesKey("import_target_calendar_id_for_both_mode")
    val SELECTED_GOOGLE_ACCOUNT_NAME = stringPreferencesKey("selected_google_account_name")
    val SHOW_ALL_EVENTS = booleanPreferencesKey("show_all_events")
    val CALENDAR_SYNC_APP_TO_CALENDAR = booleanPreferencesKey("calendar_sync_app_to_calendar")
    val CALENDAR_SYNC_CALENDAR_TO_APP = booleanPreferencesKey("calendar_sync_calendar_to_app")
    val ENABLED_TABS = stringSetPreferencesKey("enabled_tabs")
    val EXERCISE_DB_LAST_REFRESH = longPreferencesKey("exercise_db_last_refresh_epoch_ms")
    val EXERCISE_DB_LAST_PROMPT = longPreferencesKey("exercise_db_last_prompt_epoch_ms")
    val EXERCISE_DB_DOWNLOAD_ACCEPTED = booleanPreferencesKey("exercise_db_download_accepted")
    val EXERCISE_DB_CACHE_COMPLETE = booleanPreferencesKey("exercise_db_cache_complete")
    val EXERCISE_DB_CACHE_TOTAL = intPreferencesKey("exercise_db_cache_total")
    val MAP_TRACKING_MODE = stringPreferencesKey("map_tracking_mode")
    val FOOD_CONSUMED_TRACKING_ENABLED = booleanPreferencesKey("food_consumed_tracking_enabled")
    val EVENT_SET_NAMES = stringSetPreferencesKey("event_set_names")
    val EVENT_SET_COLORS = stringSetPreferencesKey("event_set_colors")
    val EVENT_SETS_ENABLED = booleanPreferencesKey("event_sets_enabled")
    val APP_PRIMARY_COLOR = intPreferencesKey("app_primary_color")
    val APP_SECONDARY_COLOR = intPreferencesKey("app_secondary_color")
    val APP_TERTIARY_COLOR = intPreferencesKey("app_tertiary_color")
    val RECENT_CUSTOM_EVENT_COLORS = stringPreferencesKey("recent_custom_event_colors")
    val EVENT_INDICATOR_DISPLAY_CONDITION = stringPreferencesKey("event_indicator_display_condition")
    val EVENT_BACKGROUND_DISPLAY_CONDITION = stringPreferencesKey("event_background_display_condition")
    val EVENT_BACKGROUND_TRANSPARENCY = stringPreferencesKey("event_background_transparency")
    val EVENT_TITLE_COLOR_CHOICE = stringPreferencesKey("event_title_color_choice")
    val EVENT_TITLE_CUSTOM_COLOR = intPreferencesKey("event_title_custom_color")
    val PAST_EVENT_TEXT_TREATMENT = stringPreferencesKey("past_event_text_treatment")
    val PAST_EVENT_TEXT_CUSTOM_COLOR = intPreferencesKey("past_event_text_custom_color")
    val PAST_EVENT_BACKGROUND_TREATMENT = stringPreferencesKey("past_event_background_treatment")
    val PAST_EVENT_BACKGROUND_CUSTOM_COLOR = intPreferencesKey("past_event_background_custom_color")
    val PAST_EVENT_BACKGROUND_TRANSPARENCY = stringPreferencesKey("past_event_background_transparency")
    val ROUTINE_INSIGHTS_ENABLED = booleanPreferencesKey("routine_insights_enabled")
    val EXAMPLE_DATA_SEEDED = booleanPreferencesKey("example_data_seeded")
    val DRIVE_BACKUP_URI = stringPreferencesKey("drive_backup_uri")
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")

    companion object {
        const val ACTION_KEEP_IN_APP = "KEEP_IN_APP"
        const val ACTION_DELETE_FROM_APP = "DELETE_FROM_APP"
        const val IMPORT_TARGET_CALENDAR_LOCAL = "local"
        const val IMPORT_TARGET_CALENDAR_PRIMARY = "google_primary"
        const val IMPORT_TARGET_CALENDAR_BOTH = "both"
        const val IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY = "google_selected_account_primary"
        const val ACTION_APP_DELETE_KEEPS_CALENDAR = "APP_DELETE_KEEPS_CALENDAR"
        const val ACTION_APP_DELETE_DELETES_CALENDAR = "APP_DELETE_DELETES_CALENDAR"
        const val ACTION_APP_DELETE_ALWAYS_ASK = "APP_DELETE_ALWAYS_ASK"
        const val DEFAULT_IMPORT_TARGET_CALENDAR_ID_FOR_BOTH_MODE = "google_primary"
        const val DEFAULT_EVENT_DURATION_HOURS = 1
        const val DEFAULT_EVENT_DURATION_MINUTES = 0
        const val MAX_EVENT_SETS = 20
        const val MAX_ACTIVE_SETS_PER_DAY = 3
    }

    suspend fun saveUserSettings(userSettings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[USER_WEIGHT_KG] = userSettings.weightKg
            preferences[USER_HEIGHT_CM] = userSettings.heightCm
            preferences[USER_AGE] = userSettings.age
            preferences[USER_GENDER] = userSettings.gender.name
            preferences[USER_ACTIVITY_LEVEL] = userSettings.activityLevel.name
            preferences[USER_CUSTOM_CALORIES_TARGET] = userSettings.customCaloriesTarget
            preferences[USER_CUSTOM_PROTEIN_TARGET] = userSettings.customProteinTargetG
            preferences[USER_CUSTOM_CARBS_TARGET] = userSettings.customCarbsTargetG
            preferences[USER_CUSTOM_FAT_TARGET] = userSettings.customFatTargetG
            preferences[USER_CUSTOM_FIBER_TARGET] = userSettings.customFiberTargetG
            preferences[USER_CUSTOM_SATURATED_FAT_TARGET] = userSettings.customSaturatedFatTargetG
            preferences[USER_CUSTOM_ADDED_SUGARS_TARGET] = userSettings.customAddedSugarsTargetG
            preferences[USER_CUSTOM_SODIUM_TARGET] = userSettings.customSodiumTargetMg
            preferences[USER_CALORIE_GOAL] = userSettings.calorieGoal.name
        }
    }

    fun getUserSettings(): Flow<UserSettings?> {
        return dataStore.data.map { preferences ->
            val weightKg = preferences[USER_WEIGHT_KG]
            val heightCm = preferences[USER_HEIGHT_CM]
            val age = preferences[USER_AGE]
            val gender = preferences[USER_GENDER]?.let { Gender.valueOf(it) }
            val activityLevel = preferences[USER_ACTIVITY_LEVEL]?.let { ActivityLevel.valueOf(it) }
            val customCaloriesTarget = preferences[USER_CUSTOM_CALORIES_TARGET] ?: 0.0
            val customProteinTarget = preferences[USER_CUSTOM_PROTEIN_TARGET] ?: 0.0
            val customCarbsTarget = preferences[USER_CUSTOM_CARBS_TARGET] ?: 0.0
            val customFatTarget = preferences[USER_CUSTOM_FAT_TARGET] ?: 0.0
            val customFiberTarget = preferences[USER_CUSTOM_FIBER_TARGET] ?: 0.0
            val customSaturatedFatTarget = preferences[USER_CUSTOM_SATURATED_FAT_TARGET] ?: 0.0
            val customAddedSugarsTarget = preferences[USER_CUSTOM_ADDED_SUGARS_TARGET] ?: 0.0
            val customSodiumTarget = preferences[USER_CUSTOM_SODIUM_TARGET] ?: 0.0
            val calorieGoal = preferences[USER_CALORIE_GOAL]?.let { CalorieGoal.valueOf(it) } ?: CalorieGoal.MAINTAIN

            if (weightKg != null && heightCm != null && age != null && gender != null && activityLevel != null) {
                UserSettings(
                    weightKg = weightKg,
                    heightCm = heightCm,
                    age = age,
                    gender = gender,
                    activityLevel = activityLevel,
                    customCaloriesTarget = customCaloriesTarget,
                    customProteinTargetG = customProteinTarget,
                    customCarbsTargetG = customCarbsTarget,
                    customFatTargetG = customFatTarget,
                    customFiberTargetG = customFiberTarget,
                    customSaturatedFatTargetG = customSaturatedFatTarget,
                    customAddedSugarsTargetG = customAddedSugarsTarget,
                    customSodiumTargetMg = customSodiumTarget,
                    calorieGoal = calorieGoal
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

    suspend fun saveFoodConsumedTrackingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FOOD_CONSUMED_TRACKING_ENABLED] = enabled
        }
    }

    fun getFoodConsumedTrackingEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[FOOD_CONSUMED_TRACKING_ENABLED] ?: false
        }.distinctUntilChanged()
    }

    suspend fun saveRoutineInsightsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ROUTINE_INSIGHTS_ENABLED] = enabled
        }
    }

    fun getRoutineInsightsEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[ROUTINE_INSIGHTS_ENABLED] ?: false
        }.distinctUntilChanged()
    }

    suspend fun saveDriveBackupUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(DRIVE_BACKUP_URI)
            } else {
                preferences[DRIVE_BACKUP_URI] = uri
            }
        }
    }

    fun getDriveBackupUri(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[DRIVE_BACKUP_URI]
        }.distinctUntilChanged()
    }

    suspend fun saveHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    fun getHasCompletedOnboarding(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] ?: false
        }.distinctUntilChanged()
    }

    fun getEventSetNames(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            val storedNames = preferences[EVENT_SET_NAMES].orEmpty()
            val nameMap = storedNames.mapNotNull { entry ->
                val parts = entry.split("|", limit = 2)
                val id = parts.firstOrNull()?.toIntOrNull()
                val name = parts.getOrNull(1)?.trim().orEmpty()
                if (id != null && name.isNotBlank()) {
                    id to name
                } else {
                    null
                }
            }.toMap()

            List(MAX_EVENT_SETS) { index ->
                nameMap[index + 1] ?: defaultEventSetName(index)
            }
        }
    }

    suspend fun saveEventSetNames(names: List<String>) {
        val normalizedNames = names.take(MAX_EVENT_SETS).mapIndexed { index, name ->
            val finalName = name.trim().ifBlank { defaultEventSetName(index) }
            "${index + 1}|$finalName"
        }.toSet()
        dataStore.edit { preferences ->
            preferences[EVENT_SET_NAMES] = normalizedNames
        }
    }

    fun getEventSetColors(): Flow<List<Int>> {
        return dataStore.data.map { preferences ->
            val storedColors = preferences[EVENT_SET_COLORS].orEmpty()
            val colorMap = storedColors.mapNotNull { entry ->
                val parts = entry.split("|", limit = 2)
                val id = parts.firstOrNull()?.toIntOrNull()
                val color = parts.getOrNull(1)?.toIntOrNull()
                if (id != null && color != null) {
                    id to color
                } else {
                    null
                }
            }.toMap()

            List(MAX_EVENT_SETS) { index ->
                colorMap[index + 1] ?: defaultEventSetColor(index)
            }
        }
    }

    suspend fun saveEventSetColors(colors: List<Int>) {
        val normalizedColors = colors.take(MAX_EVENT_SETS).mapIndexed { index, color ->
            "${index + 1}|$color"
        }.toSet()
        dataStore.edit { preferences ->
            preferences[EVENT_SET_COLORS] = normalizedColors
        }
    }

    suspend fun saveEventSetsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EVENT_SETS_ENABLED] = enabled
        }
    }

    fun getEventSetsEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[EVENT_SETS_ENABLED] ?: true
        }.distinctUntilChanged()
    }

    fun getRecentCustomEventColors(): Flow<List<Int>> {
        return dataStore.data.map { preferences ->
            preferences[RECENT_CUSTOM_EVENT_COLORS]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.filter { isCustomSeriesColor(it) }
                ?.distinct()
                ?.take(10)
                ?: emptyList()
        }
    }

    suspend fun saveRecentCustomEventColor(colorArgb: Int) {
        if (!isCustomSeriesColor(colorArgb)) return
        dataStore.edit { preferences ->
            val existing = preferences[RECENT_CUSTOM_EVENT_COLORS]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.filter { isCustomSeriesColor(it) }
                .orEmpty()
            val updated = listOf(colorArgb) + existing.filterNot { it == colorArgb }
            preferences[RECENT_CUSTOM_EVENT_COLORS] = updated.take(10).joinToString(",")
        }
    }

    fun getAppThemeColors(): Flow<AppThemeColors> {
        return dataStore.data.map { preferences ->
            AppThemeColors(
                primary = preferences[APP_PRIMARY_COLOR] ?: DEFAULT_PRIMARY_COLOR_ARGB,
                secondary = preferences[APP_SECONDARY_COLOR] ?: DEFAULT_SECONDARY_COLOR_ARGB,
                tertiary = preferences[APP_TERTIARY_COLOR] ?: DEFAULT_TERTIARY_COLOR_ARGB
            )
        }.distinctUntilChanged()
    }

    suspend fun saveAppThemeColors(colors: AppThemeColors) {
        dataStore.edit { preferences ->
            preferences[APP_PRIMARY_COLOR] = colors.primary
            preferences[APP_SECONDARY_COLOR] = colors.secondary
            preferences[APP_TERTIARY_COLOR] = colors.tertiary
        }
    }

    suspend fun saveEventIndicatorDisplayCondition(condition: EventColorDisplayCondition) {
        dataStore.edit { preferences ->
            preferences[EVENT_INDICATOR_DISPLAY_CONDITION] = condition.name
        }
    }

    fun getEventIndicatorDisplayCondition(): Flow<EventColorDisplayCondition> {
        return dataStore.data.map { preferences ->
            EventColorDisplayCondition.fromName(preferences[EVENT_INDICATOR_DISPLAY_CONDITION])
        }.distinctUntilChanged()
    }

    suspend fun saveEventBackgroundDisplayCondition(condition: EventColorDisplayCondition) {
        dataStore.edit { preferences ->
            preferences[EVENT_BACKGROUND_DISPLAY_CONDITION] = condition.name
        }
    }

    fun getEventBackgroundDisplayCondition(): Flow<EventColorDisplayCondition> {
        return dataStore.data.map { preferences ->
            EventColorDisplayCondition.fromName(
                preferences[EVENT_BACKGROUND_DISPLAY_CONDITION]
                    ?: EventColorDisplayCondition.NEVER.name
            )
        }.distinctUntilChanged()
    }

    suspend fun saveEventBackgroundTransparency(transparency: EventBackgroundTransparency) {
        dataStore.edit { preferences ->
            preferences[EVENT_BACKGROUND_TRANSPARENCY] = transparency.name
        }
    }

    fun getEventBackgroundTransparency(): Flow<EventBackgroundTransparency> {
        return dataStore.data.map { preferences ->
            EventBackgroundTransparency.fromName(preferences[EVENT_BACKGROUND_TRANSPARENCY])
        }.distinctUntilChanged()
    }

    suspend fun saveEventTitleColorChoice(choice: EventTitleColorChoice) {
        dataStore.edit { preferences ->
            preferences[EVENT_TITLE_COLOR_CHOICE] = choice.name
        }
    }

    fun getEventTitleColorChoice(): Flow<EventTitleColorChoice> {
        return dataStore.data.map { preferences ->
            EventTitleColorChoice.fromName(preferences[EVENT_TITLE_COLOR_CHOICE])
        }.distinctUntilChanged()
    }

    suspend fun saveEventTitleCustomColor(colorArgb: Int) {
        dataStore.edit { preferences ->
            preferences[EVENT_TITLE_CUSTOM_COLOR] = colorArgb
        }
    }

    fun getEventTitleCustomColor(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[EVENT_TITLE_CUSTOM_COLOR] ?: DEFAULT_PRIMARY_COLOR_ARGB
        }.distinctUntilChanged()
    }

    suspend fun savePastEventTextTreatment(treatment: PastEventColorTreatment) {
        dataStore.edit { preferences ->
            preferences[PAST_EVENT_TEXT_TREATMENT] = treatment.name
        }
    }

    fun getPastEventTextTreatment(): Flow<PastEventColorTreatment> {
        return dataStore.data.map { preferences ->
            PastEventColorTreatment.fromName(preferences[PAST_EVENT_TEXT_TREATMENT])
        }.distinctUntilChanged()
    }

    suspend fun savePastEventTextCustomColor(colorArgb: Int) {
        dataStore.edit { preferences ->
            preferences[PAST_EVENT_TEXT_CUSTOM_COLOR] = colorArgb
        }
    }

    fun getPastEventTextCustomColor(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[PAST_EVENT_TEXT_CUSTOM_COLOR] ?: DEFAULT_PRIMARY_COLOR_ARGB
        }.distinctUntilChanged()
    }

    suspend fun savePastEventBackgroundTreatment(treatment: PastEventColorTreatment) {
        dataStore.edit { preferences ->
            preferences[PAST_EVENT_BACKGROUND_TREATMENT] = treatment.name
        }
    }

    fun getPastEventBackgroundTreatment(): Flow<PastEventColorTreatment> {
        return dataStore.data.map { preferences ->
            PastEventColorTreatment.fromName(preferences[PAST_EVENT_BACKGROUND_TREATMENT])
        }.distinctUntilChanged()
    }

    suspend fun savePastEventBackgroundCustomColor(colorArgb: Int) {
        dataStore.edit { preferences ->
            preferences[PAST_EVENT_BACKGROUND_CUSTOM_COLOR] = colorArgb
        }
    }

    fun getPastEventBackgroundCustomColor(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[PAST_EVENT_BACKGROUND_CUSTOM_COLOR] ?: DEFAULT_PRIMARY_COLOR_ARGB
        }.distinctUntilChanged()
    }

    suspend fun savePastEventBackgroundTransparency(transparency: EventBackgroundTransparency) {
        dataStore.edit { preferences ->
            preferences[PAST_EVENT_BACKGROUND_TRANSPARENCY] = transparency.name
        }
    }

    fun getPastEventBackgroundTransparency(): Flow<EventBackgroundTransparency> {
        return dataStore.data.map { preferences ->
            EventBackgroundTransparency.fromName(preferences[PAST_EVENT_BACKGROUND_TRANSPARENCY])
        }.distinctUntilChanged()
    }

    fun getExampleDataSeeded(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[EXAMPLE_DATA_SEEDED] ?: false
        }.distinctUntilChanged()
    }

    suspend fun setExampleDataSeeded(seeded: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXAMPLE_DATA_SEEDED] = seeded
        }
    }

    fun getDefaultActiveSetsForWeekday(day: DayOfWeek): Flow<Set<Int>> {
        return dataStore.data.map { preferences ->
            preferences[defaultEventSetKey(day)].orEmpty()
                .mapNotNull { it.toIntOrNull() }
                .filter { it in 1..MAX_EVENT_SETS }
                .take(MAX_ACTIVE_SETS_PER_DAY)
                .toSet()
        }
    }

    fun getActiveSetsForDate(date: LocalDate): Flow<Set<Int>?> {
        return dataStore.data.map { preferences ->
            preferences[activeEventSetKey(date)]
                ?.mapNotNull { it.toIntOrNull() }
                ?.filter { it in 1..MAX_EVENT_SETS }
                ?.take(MAX_ACTIVE_SETS_PER_DAY)
                ?.toSet()
        }
    }

    fun getDefaultActiveSetsByWeekday(): Flow<Map<DayOfWeek, Set<Int>>> {
        val flows = DayOfWeek.values().map { day -> getDefaultActiveSetsForWeekday(day) }
        return combine(flows) { selections ->
            DayOfWeek.values().zip(selections.toList()).toMap()
        }
    }

    suspend fun saveDefaultActiveSetsForWeekday(day: DayOfWeek, setIds: Set<Int>) {
        val normalized = setIds.filter { it in 1..MAX_EVENT_SETS }
            .take(MAX_ACTIVE_SETS_PER_DAY)
            .map { it.toString() }
            .toSet()
        dataStore.edit { preferences ->
            preferences[defaultEventSetKey(day)] = normalized
        }
    }

    suspend fun saveActiveSetsForDate(date: LocalDate, setIds: Set<Int>) {
        val normalized = setIds.filter { it in 1..MAX_EVENT_SETS }
            .take(MAX_ACTIVE_SETS_PER_DAY)
            .map { it.toString() }
            .toSet()
        dataStore.edit { preferences ->
            preferences[activeEventSetKey(date)] = normalized
        }
    }

    suspend fun clearManualActiveEventSets() {
        dataStore.edit { preferences ->
            val keysToRemove = preferences.asMap().keys.filter { key ->
                key.name.startsWith("active_event_sets_")
            }
            keysToRemove.forEach { key ->
                preferences.remove(key)
            }
        }
    }

    suspend fun clearActiveSetsForDate(date: LocalDate) {
        dataStore.edit { preferences ->
            preferences.remove(activeEventSetKey(date))
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
            preferences[DEFAULT_EVENT_REMINDER_COUNT] = settings.reminderCount
            preferences[DEFAULT_EVENT_REMINDER_INTERVAL_MINUTES] = settings.reminderIntervalMinutes
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
                reminderCount = preferences[DEFAULT_EVENT_REMINDER_COUNT] ?: 0,
                reminderIntervalMinutes = preferences[DEFAULT_EVENT_REMINDER_INTERVAL_MINUTES] ?: 60,
                startTimeOptionName = preferences[START_TIME_OPTION_NAME] ?: "NEXT_FULL_HOUR",
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
            preferences[USE_GOOGLE_BACKUP_MODE] ?: false
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

    suspend fun saveCalendarSyncAppToCalendar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CALENDAR_SYNC_APP_TO_CALENDAR] = enabled
        }
    }

    fun getCalendarSyncAppToCalendar(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[CALENDAR_SYNC_APP_TO_CALENDAR] ?: true
        }
    }

    suspend fun saveCalendarSyncCalendarToApp(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CALENDAR_SYNC_CALENDAR_TO_APP] = enabled
        }
    }

    fun getCalendarSyncCalendarToApp(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[CALENDAR_SYNC_CALENDAR_TO_APP] ?: true
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

    private fun defaultEventSetName(index: Int): String {
        val letter = ('A' + index)
        return "Set $letter"
    }

    private fun defaultEventSetColor(index: Int): Int {
        return defaultSeriesColorForIndex(index)
    }

    private fun defaultEventSetKey(day: DayOfWeek): Preferences.Key<Set<String>> {
        return stringSetPreferencesKey("default_event_sets_${day.name.lowercase()}")
    }

    private fun activeEventSetKey(date: LocalDate): Preferences.Key<Set<String>> {
        return stringSetPreferencesKey("active_event_sets_${date}")
    }
}
