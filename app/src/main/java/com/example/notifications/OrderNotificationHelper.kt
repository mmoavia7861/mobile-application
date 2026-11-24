package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.OrderStatus
import java.util.Locale

data class OrderNotificationContent(
  val title: String,
  val body: String,
  val statusKey: String
)

object OrderNotificationFormatter {
  fun buildNotificationContent(
    orderId: String,
    storeName: String,
    rawStatus: String
  ): OrderNotificationContent {
    val normalized = rawStatus.trim().uppercase(Locale.US)
    val safeStore = storeName.ifBlank { "MealVerse Restaurant" }

    return when (normalized) {
      OrderStatus.NEW.name -> OrderNotificationContent(
        title = "Order Placed — $safeStore",
        body = "Your order has been sent to $safeStore for confirmation.",
        statusKey = OrderStatus.NEW.name
      )
      OrderStatus.ACCEPTED.name -> OrderNotificationContent(
        title = "Order Accepted — $safeStore",
        body = "$safeStore confirmed your order and added it to the kitchen queue.",
        statusKey = OrderStatus.ACCEPTED.name
      )
      OrderStatus.PREPARING.name -> OrderNotificationContent(
        title = "Chef Is Cooking — $safeStore",
        body = "Your meal from $safeStore is now being freshly prepared.",
        statusKey = OrderStatus.PREPARING.name
      )
      OrderStatus.READY.name -> OrderNotificationContent(
        title = "Ready for Pickup — $safeStore",
        body = "Your order from $safeStore is packed hot and waiting for rider dispatch.",
        statusKey = OrderStatus.READY.name
      )
      OrderStatus.ON_THE_WAY.name -> OrderNotificationContent(
        title = "Rider On the Way!",
        body = "Your food from $safeStore is out for delivery to your doorstep.",
        statusKey = OrderStatus.ON_THE_WAY.name
      )
      OrderStatus.DELIVERED.name -> OrderNotificationContent(
        title = "Order Delivered — Enjoy!",
        body = "Your order from $safeStore has been delivered. Har Zaiqa, Har Ghar!",
        statusKey = OrderStatus.DELIVERED.name
      )
      OrderStatus.REJECTED.name -> OrderNotificationContent(
        title = "Order Declined — $safeStore",
        body = "Unfortunately $safeStore could not fulfill this order.",
        statusKey = OrderStatus.REJECTED.name
      )
      OrderStatus.CANCELLED.name -> OrderNotificationContent(
        title = "Order Cancelled",
        body = "Your order from $safeStore has been cancelled.",
        statusKey = OrderStatus.CANCELLED.name
      )
      else -> OrderNotificationContent(
        title = "Order Update — $safeStore",
        body = "Your order status changed to ${normalized.replace('_', ' ').lowercase(Locale.US)}.",
        statusKey = normalized
      )
    }
  }
}

object OrderNotificationHelper {
  const val CHANNEL_ID = "mealverse_order_updates"
  const val CHANNEL_NAME = "Order Status Updates"
  const val CHANNEL_DESCRIPTION = "Real-time Firebase push alerts when your food order status changes"
  const val EXTRA_ORDER_ID = "extra_order_id"
  const val EXTRA_OPEN_ORDER_TRACKING = "extra_open_order_tracking"

  fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          CHANNEL_NAME,
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = CHANNEL_DESCRIPTION
          enableVibration(true)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun showOrderStatusNotification(
    context: Context,
    orderId: String,
    storeName: String,
    status: String,
    overrideTitle: String? = null,
    overrideBody: String? = null
  ) {
    ensureNotificationChannel(context)
    if (!hasNotificationPermission(context)) return

    val formatted = OrderNotificationFormatter.buildNotificationContent(orderId, storeName, status)
    val title = overrideTitle?.takeIf { it.isNotBlank() } ?: formatted.title
    val body = overrideBody?.takeIf { it.isNotBlank() } ?: formatted.body

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_OPEN_ORDER_TRACKING, true)
      if (orderId.isNotBlank()) {
        putExtra(EXTRA_ORDER_ID, orderId)
      }
    }

    val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    val notificationId = (orderId.ifBlank { storeName } + "_" + formatted.statusKey).hashCode()
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      intent,
      pendingIntentFlags
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Permission revoked concurrently on API 33+
    }
  }
}
