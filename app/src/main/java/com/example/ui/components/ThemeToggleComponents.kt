package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun ThemeQuickToggleButton(
  isDarkTheme: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  FilledTonalIconButton(
    onClick = onToggle,
    colors = IconButtonDefaults.filledTonalIconButtonColors(
      containerColor = if (isDarkTheme) {
        MealVerseSaffronYellow.copy(alpha = 0.18f)
      } else {
        MealVerseDeepTeal.copy(alpha = 0.10f)
      },
      contentColor = if (isDarkTheme) MealVerseSaffronYellow else MealVerseDeepTeal
    ),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag("theme_quick_toggle_btn")
  ) {
    Icon(
      imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
      contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode"
    )
  }
}

@Composable
fun ThemeModeSelectorCard(
  themeMode: ThemeMode,
  isDarkActive: Boolean,
  onSelectMode: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("theme_selector_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
              if (isDarkActive) MealVerseSaffronYellow.copy(alpha = 0.18f)
              else MealVerseDeepTeal.copy(alpha = 0.12f)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isDarkActive) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = null,
            tint = if (isDarkActive) MealVerseSaffronYellow else MealVerseDeepTeal,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "App Appearance",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = when (themeMode) {
              ThemeMode.LIGHT -> "Light Mode active"
              ThemeMode.DARK -> "Dark Mode active"
              ThemeMode.SYSTEM -> "Following device theme (${if (isDarkActive) "Dark" else "Light"})"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      ThemeModeSelectorRow(
        themeMode = themeMode,
        onSelectMode = onSelectMode
      )
    }
  }
}

@Composable
fun ThemeModeSelectorRow(
  themeMode: ThemeMode,
  onSelectMode: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    ThemeModeOptionChip(
      label = "Light",
      icon = Icons.Default.LightMode,
      selected = themeMode == ThemeMode.LIGHT,
      onClick = { onSelectMode(ThemeMode.LIGHT) },
      modifier = Modifier
        .weight(1f)
        .testTag("theme_mode_light")
    )
    ThemeModeOptionChip(
      label = "Dark",
      icon = Icons.Default.DarkMode,
      selected = themeMode == ThemeMode.DARK,
      onClick = { onSelectMode(ThemeMode.DARK) },
      modifier = Modifier
        .weight(1f)
        .testTag("theme_mode_dark")
    )
    ThemeModeOptionChip(
      label = "System",
      icon = Icons.Default.BrightnessAuto,
      selected = themeMode == ThemeMode.SYSTEM,
      onClick = { onSelectMode(ThemeMode.SYSTEM) },
      modifier = Modifier
        .weight(1f)
        .testTag("theme_mode_system")
    )
  }
}

@Composable
private fun ThemeModeOptionChip(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
  val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bg,
    modifier = modifier
      .height(48.dp)
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, borderColor, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = fg,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.Bold,
          color = fg
        )
      )
    }
  }
}
