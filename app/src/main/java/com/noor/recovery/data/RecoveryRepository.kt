package com.noor.recovery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "noor_prefs")

class RecoveryRepository(private val context: Context) {

    companion object {
        private val KEY_START_TIME   = longPreferencesKey("start_time")
        private val KEY_ADDICTION_ID = stringPreferencesKey("addiction_id")
    }

    val sessionFlow: Flow<RecoverySession?> = context.dataStore.data.map { prefs ->
        val startTime    = prefs[KEY_START_TIME]   ?: return@map null
        val addictionId  = prefs[KEY_ADDICTION_ID] ?: return@map null
        RecoverySession(addictionId, startTime)
    }

    suspend fun saveSession(session: RecoverySession) {
        context.dataStore.edit { prefs ->
            prefs[KEY_START_TIME]   = session.startTimeMs
            prefs[KEY_ADDICTION_ID] = session.addictionTypeId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_START_TIME)
            prefs.remove(KEY_ADDICTION_ID)
        }
    }
}
