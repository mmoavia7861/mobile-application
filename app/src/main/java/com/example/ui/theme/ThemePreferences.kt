package com.example.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark")
}

object ThemeModeResolver {
  fun parseThemeMode(raw: String?): ThemeMode {
    if (raw.isNullOrBlank()) return ThemeMode.SYSTEM
    return ThemeMode.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
      ?: ThemeMode.SYSTEM
  }

  fun resolveDarkTheme(mode: ThemeMode, systemInDarkTheme: Boolean): Boolean {
    return when (mode) {
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
      ThemeMode.SYSTEM -> systemInDarkTheme
    }
  }

  fun toggleDarkLight(currentlyDark: Boolean): ThemeMode {
    return if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
  }
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "mealverse_theme_prefs")

class ThemePreferencesRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
  }

  val themeModeFlow: Flow<ThemeMode> = context.themeDataStore.data
    .catch { exception ->
      if (exception is IOException) {
        emit(emptyPreferences())
      } else {
        throw exception
      }
    }
    .map { prefs ->
      ThemeModeResolver.parseThemeMode(prefs[Keys.THEME_MODE])
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.themeDataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = mode.name
    }
  }
}
