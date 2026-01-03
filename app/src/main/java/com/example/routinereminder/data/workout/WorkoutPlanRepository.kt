package com.example.routinereminder.data.workout

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class WorkoutPlanState(
    val plans: List<WorkoutPlan>,
    val selectedPlanId: String?
)

@Singleton
class WorkoutPlanRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val plansKey = stringPreferencesKey("workout_plans")
    private val selectedPlanKey = stringPreferencesKey("workout_selected_plan")
    private val gson = Gson()
    private val planListType = object : TypeToken<List<WorkoutPlan>>() {}.type

    val planState: Flow<WorkoutPlanState> = dataStore.data.map { preferences ->
        val plansJson = preferences[plansKey]
        val plans = if (plansJson.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                gson.fromJson<List<WorkoutPlan>>(plansJson, planListType) ?: emptyList()
            } catch (_: JsonSyntaxException) {
                emptyList()
            }
        }
        val selectedPlanId = preferences[selectedPlanKey]?.takeIf { selected ->
            plans.any { it.id == selected }
        }
        WorkoutPlanState(plans = plans, selectedPlanId = selectedPlanId)
    }

    suspend fun savePlans(plans: List<WorkoutPlan>, selectedPlanId: String?) {
        dataStore.edit { preferences ->
            if (plans.isEmpty()) {
                preferences.remove(plansKey)
            } else {
                preferences[plansKey] = gson.toJson(plans, planListType)
            }
            if (selectedPlanId.isNullOrBlank()) {
                preferences.remove(selectedPlanKey)
            } else {
                preferences[selectedPlanKey] = selectedPlanId
            }
        }
    }
}
