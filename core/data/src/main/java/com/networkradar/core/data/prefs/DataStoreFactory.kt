package com.networkradar.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "network_radar_prefs")

class DataStoreFactory(private val context: Context) {
    fun create(): DataStore<Preferences> = context.dataStore
}
