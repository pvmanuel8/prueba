package com.example.myapplication.data.repository


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.model.CompressionQuality
import com.example.myapplication.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository para gestionar preferencias del usuario
 */
class PreferencesRepository(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = Constants.PREFS_NAME
    )

    // Keys de preferencias
    private object PreferencesKeys {
        val COMPRESSION_QUALITY = stringPreferencesKey(Constants.PREF_COMPRESSION_QUALITY)
        val AUTO_SAVE = booleanPreferencesKey(Constants.PREF_AUTO_SAVE)
        val SHOW_HISTOGRAM = booleanPreferencesKey(Constants.PREF_SHOW_HISTOGRAM)
        val DARK_MODE = booleanPreferencesKey(Constants.PREF_DARK_MODE)
        val TILE_SIZE = intPreferencesKey("tile_size")
        val MAX_CONCURRENT_TASKS = intPreferencesKey("max_concurrent_tasks")
    }

    /**
     * Calidad de compresión
     */
    val compressionQuality: Flow<CompressionQuality> = context.dataStore.data.map { prefs ->
        val qualityString = prefs[PreferencesKeys.COMPRESSION_QUALITY]
            ?: CompressionQuality.HIGH.name

        try {
            CompressionQuality.valueOf(qualityString)
        } catch (e: Exception) {
            CompressionQuality.HIGH
        }
    }

    suspend fun setCompressionQuality(quality: CompressionQuality) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.COMPRESSION_QUALITY] = quality.name
        }
    }

    /**
     * Auto-guardado
     */
    val autoSave: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTO_SAVE] ?: false
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_SAVE] = enabled
        }
    }

    /**
     * Mostrar histograma
     */
    val showHistogram: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SHOW_HISTOGRAM] ?: true
    }

    suspend fun setShowHistogram(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHOW_HISTOGRAM] = enabled
        }
    }

    /**
     * Modo oscuro
     */
    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    /**
     * Tamaño de tiles para procesamiento paralelo
     */
    val tileSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TILE_SIZE] ?: Constants.TILE_SIZE
    }

    suspend fun setTileSize(size: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TILE_SIZE] = size.coerceIn(128, 512)
        }
    }

    /**
     * Máximo de tareas concurrentes
     */
    val maxConcurrentTasks: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.MAX_CONCURRENT_TASKS] ?: Constants.MAX_CONCURRENT_TASKS
    }

    suspend fun setMaxConcurrentTasks(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MAX_CONCURRENT_TASKS] = count.coerceIn(1, 8)
        }
    }

    /**
     * Obtiene todas las preferencias como un objeto
     */
    val allPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            compressionQuality = try {
                CompressionQuality.valueOf(
                    prefs[PreferencesKeys.COMPRESSION_QUALITY] ?: CompressionQuality.HIGH.name
                )
            } catch (e: Exception) {
                CompressionQuality.HIGH
            },
            autoSave = prefs[PreferencesKeys.AUTO_SAVE] ?: false,
            showHistogram = prefs[PreferencesKeys.SHOW_HISTOGRAM] ?: true,
            darkMode = prefs[PreferencesKeys.DARK_MODE] ?: false,
            tileSize = prefs[PreferencesKeys.TILE_SIZE] ?: Constants.TILE_SIZE,
            maxConcurrentTasks = prefs[PreferencesKeys.MAX_CONCURRENT_TASKS]
                ?: Constants.MAX_CONCURRENT_TASKS
        )
    }

    /**
     * Resetea todas las preferencias a valores por defecto
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

/**
 * Objeto de preferencias del usuario
 */
data class UserPreferences(
    val compressionQuality: CompressionQuality = CompressionQuality.HIGH,
    val autoSave: Boolean = false,
    val showHistogram: Boolean = true,
    val darkMode: Boolean = false,
    val tileSize: Int = Constants.TILE_SIZE,
    val maxConcurrentTasks: Int = Constants.MAX_CONCURRENT_TASKS
)