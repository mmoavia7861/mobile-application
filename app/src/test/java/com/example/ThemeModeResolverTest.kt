package com.example

import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeModeResolver
import org.junit.Assert.*
import org.junit.Test

class ThemeModeResolverTest {

  @Test
  fun `resolveDarkTheme honors explicit light dark and system modes`() {
    assertFalse(ThemeModeResolver.resolveDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = true))
    assertTrue(ThemeModeResolver.resolveDarkTheme(ThemeMode.DARK, systemInDarkTheme = false))
    assertTrue(ThemeModeResolver.resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = true))
    assertFalse(ThemeModeResolver.resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = false))
  }

  @Test
  fun `parseThemeMode safely falls back to SYSTEM on null or unknown input`() {
    assertEquals(ThemeMode.DARK, ThemeModeResolver.parseThemeMode("dark"))
    assertEquals(ThemeMode.LIGHT, ThemeModeResolver.parseThemeMode("LIGHT"))
    assertEquals(ThemeMode.SYSTEM, ThemeModeResolver.parseThemeMode(null))
    assertEquals(ThemeMode.SYSTEM, ThemeModeResolver.parseThemeMode("invalid_mode"))
  }

  @Test
  fun `toggleDarkLight switches cleanly between Light and Dark modes`() {
    assertEquals(ThemeMode.LIGHT, ThemeModeResolver.toggleDarkLight(currentlyDark = true))
    assertEquals(ThemeMode.DARK, ThemeModeResolver.toggleDarkLight(currentlyDark = false))
  }
}
