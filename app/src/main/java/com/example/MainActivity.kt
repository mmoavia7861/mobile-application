package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.OrderNotificationHelper
import com.example.ui.navigation.MealVerseApp
import com.example.ui.theme.MealVerseTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeModeResolver
import com.example.ui.theme.ThemePreferencesRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private var pendingOrderId by mutableStateOf<String?>(null)
  private var openOrderTracking by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    OrderNotificationHelper.ensureNotificationChannel(this)
    extractNotificationExtras(intent)

    setContent {
      val themePrefs = remember { ThemePreferencesRepository(applicationContext) }
      val themeMode by themePrefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
      val systemDark = isSystemInDarkTheme()
      val isDarkTheme = ThemeModeResolver.resolveDarkTheme(themeMode, systemDark)
      val scope = rememberCoroutineScope()

      MealVerseTheme(darkTheme = isDarkTheme) {
        MealVerseApp(
          notificationOrderId = pendingOrderId,
          openTrackingFromNotification = openOrderTracking,
          onNotificationHandled = {
            pendingOrderId = null
            openOrderTracking = false
          },
          themeMode = themeMode,
          isDarkTheme = isDarkTheme,
          onThemeModeChange = { newMode ->
            scope.launch {
              themePrefs.setThemeMode(newMode)
            }
          }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    extractNotificationExtras(intent)
  }

  private fun extractNotificationExtras(intent: Intent?) {
    if (intent == null) return
    val orderId = intent.getStringExtra(OrderNotificationHelper.EXTRA_ORDER_ID)
      ?: intent.getStringExtra("orderId")
    val shouldOpenTracking = intent.getBooleanExtra(OrderNotificationHelper.EXTRA_OPEN_ORDER_TRACKING, false) ||
      !orderId.isNullOrBlank()
    if (shouldOpenTracking) {
      pendingOrderId = orderId
      openOrderTracking = true
    }
  }
}
