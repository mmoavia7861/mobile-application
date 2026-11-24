package com.example.notifications

import android.util.Log
import com.example.data.repository.MealVerseRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MealVerseMessagingService : FirebaseMessagingService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    OrderNotificationHelper.ensureNotificationChannel(applicationContext)
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d("MealVerseFCM", "Refreshed FCM registration token")
    serviceScope.launch {
      try {
        MealVerseRepository(applicationContext).saveFcmToken(token)
      } catch (e: Exception) {
        Log.w("MealVerseFCM", "Token refresh deferred until sign-in: ${e.message}")
      }
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    val data = remoteMessage.data
    val orderId = data["orderId"].orEmpty()
    val storeName = data["storeName"].orEmpty()
    val status = data["status"].orEmpty()
    val title = remoteMessage.notification?.title ?: data["title"]
    val body = remoteMessage.notification?.body ?: data["body"]

    if (orderId.isNotBlank() || status.isNotBlank() || !title.isNullOrBlank()) {
      OrderNotificationHelper.showOrderStatusNotification(
        context = applicationContext,
        orderId = orderId,
        storeName = storeName,
        status = status,
        overrideTitle = title,
        overrideBody = body
      )
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }
}
