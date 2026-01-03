package com.example.routinereminder.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.routinereminder.data.model.ActiveRunState
import com.example.routinereminder.data.model.TrailPoint
import com.example.routinereminder.ui.JsonCodec
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunSessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val activeStateKey = stringPreferencesKey("active_run_state")
    private val gson = Gson()
    private val trailListType = object : TypeToken<List<TrailPoint>>() {}.type

    private fun trailKey(sessionId: String) = stringPreferencesKey("run_trail_$sessionId")

    val activeRunState: Flow<ActiveRunState?> = dataStore.data.map { preferences ->
        preferences[activeStateKey]?.let { stored ->
            JsonCodec.decode(stored, ActiveRunState::class.java)
        }
    }

    suspend fun saveActiveRunState(state: ActiveRunState?) {
        dataStore.edit { preferences ->
            if (state == null) {
                preferences.remove(activeStateKey)
            } else {
                preferences[activeStateKey] = JsonCodec.encode(state)
            }
        }
    }

    fun trailPoints(sessionId: String): Flow<List<TrailPoint>> = dataStore.data.map { preferences ->
        val stored = preferences[trailKey(sessionId)] ?: return@map emptyList()
        gson.fromJson<List<TrailPoint>>(stored, trailListType) ?: emptyList()
    }

    suspend fun saveTrailPoints(sessionId: String, points: List<TrailPoint>) {
        dataStore.edit { preferences ->
            preferences[trailKey(sessionId)] = gson.toJson(points, trailListType)
        }
    }

    suspend fun clearTrailPoints(sessionId: String) {
        dataStore.edit { preferences ->
            preferences.remove(trailKey(sessionId))
        }
    }
}
