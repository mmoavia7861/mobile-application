package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = MealVerseLightTeal,
  onPrimary = Color.White,
  primaryContainer = MealVerseDeepTeal,
  onPrimaryContainer = Color(0xFFA5F0E4),
  secondary = MealVerseWarmOrange,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF5A2506),
  onSecondaryContainer = Color(0xFFFFDBCF),
  tertiary = MealVerseSaffronYellow,
  onTertiary = Color(0xFF3E2E00),
  background = MealVerseDarkBackground,
  surface = MealVerseDarkSurface,
  surfaceVariant = MealVerseDarkSurfaceVariant,
  onBackground = Color(0xFFE7E9E9),
  onSurface = Color(0xFFE7E9E9),
  onSurfaceVariant = Color(0xFFC4C7C7),
  outlineVariant = Color(0xFF3C4141)
)

private val LightColorScheme = lightColorScheme(
  primary = MealVerseDeepTeal,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFCCE8E5),
  onPrimaryContainer = MealVerseDeepTealDark,
  secondary = MealVerseWarmOrange,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFFDBCF),
  onSecondaryContainer = MealVerseWarmOrangeDark,
  tertiary = MealVerseSaffronYellow,
  onTertiary = Color(0xFF3E2E00),
  background = MealVerseCreamBackground,
  surface = MealVerseSurface,
  surfaceVariant = MealVerseSurfaceVariant,
  onBackground = MealVerseTextPrimary,
  onSurface = MealVerseTextPrimary,
  onSurfaceVariant = MealVerseTextSecondary,
  outlineVariant = Color(0xFFDAD4CC)
)

@Composable
fun isMealVerseDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun MealVerseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep MealVerse's branded colors consistent
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Retain alias for template compatibility
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) = MealVerseTheme(darkTheme, dynamicColor, content)
