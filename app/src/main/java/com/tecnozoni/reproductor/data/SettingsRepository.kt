package com.tecnozoni.reproductor.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tecnozoni.reproductor.data.model.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Un único DataStore de preferencias para toda la app.
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Preferencias simples clave-valor (DataStore). Por ahora, el orden elegido. */
@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val sortKey = stringPreferencesKey("sort_order")

    /** Orden guardado; si no hay o es inválido, NAME por defecto. */
    val sortOrder: Flow<SortOrder> = context.settingsDataStore.data.map { prefs ->
        prefs[sortKey]
            ?.let { name -> runCatching { SortOrder.valueOf(name) }.getOrNull() }
            ?: SortOrder.NAME
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.settingsDataStore.edit { prefs -> prefs[sortKey] = order.name }
    }
}
