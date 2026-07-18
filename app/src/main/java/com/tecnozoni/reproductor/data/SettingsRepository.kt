package com.tecnozoni.reproductor.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tecnozoni.reproductor.data.model.SortDirection
import com.tecnozoni.reproductor.data.model.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Un único DataStore de preferencias para toda la app.
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Preferencia de orden: criterio + dirección. */
data class SortPreference(val order: SortOrder, val direction: SortDirection)

/** Preferencias simples clave-valor (DataStore). */
@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val sortKey = stringPreferencesKey("sort_order")
    private val directionKey = stringPreferencesKey("sort_direction")

    /** Orden guardado; si no hay o es inválido, NAME ascendente por defecto. */
    val sortPreference: Flow<SortPreference> = context.settingsDataStore.data.map { prefs ->
        val order = prefs[sortKey]
            ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
            ?: SortOrder.NAME
        val direction = prefs[directionKey]
            ?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
            ?: SortDirection.ASC
        SortPreference(order, direction)
    }

    suspend fun setSort(order: SortOrder, direction: SortDirection) {
        context.settingsDataStore.edit { prefs ->
            prefs[sortKey] = order.name
            prefs[directionKey] = direction.name
        }
    }
}
