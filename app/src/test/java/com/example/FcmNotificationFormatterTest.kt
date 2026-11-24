package com.example

import com.example.data.model.OrderNotificationRecord
import com.example.data.model.OrderStatus
import com.example.data.model.UserProfile
import com.example.notifications.OrderNotificationFormatter
import org.junit.Assert.*
import org.junit.Test

class FcmNotificationFormatterTest {

  @Test
  fun `buildNotificationContent generates clear push copy for every Firestore order status`() {
    val statuses = listOf(
      OrderStatus.NEW,
      OrderStatus.ACCEPTED,
      OrderStatus.PREPARING,
      OrderStatus.READY,
      OrderStatus.ON_THE_WAY,
      OrderStatus.DELIVERED,
      OrderStatus.REJECTED,
      OrderStatus.CANCELLED
    )

    for (status in statuses) {
      val content = OrderNotificationFormatter.buildNotificationContent(
        orderId = "ord_123",
        storeName = "Savour Foods",
        rawStatus = status.name
      )
      assertEquals(status.name, content.statusKey)
      assertTrue("Title blank for ${status.name}", content.title.isNotBlank())
      assertTrue("Body blank for ${status.name}", content.body.isNotBlank())
      assertTrue("Body should mention store for ${status.name}", content.body.contains("Savour Foods"))
    }
  }

  @Test
  fun `buildNotificationContent handles blank store name and lowercase status`() {
    val content = OrderNotificationFormatter.buildNotificationContent(
      orderId = "ord_456",
      storeName = "",
      rawStatus = "  on_the_way "
    )
    assertEquals(OrderStatus.ON_THE_WAY.name, content.statusKey)
    assertEquals("Rider On the Way!", content.title)
    assertTrue(content.body.contains("MealVerse Restaurant"))
  }

  @Test
  fun `UserProfile and OrderNotificationRecord store FCM defaults cleanly`() {
    val profile = UserProfile(
      userId = "u1",
      fullName = "Ali Khan",
      fcmToken = "sample_fcm_token_abc",
      notificationsEnabled = true
    )
    assertEquals("sample_fcm_token_abc", profile.fcmToken)
    assertTrue(profile.notificationsEnabled)

    val record = OrderNotificationRecord(
      orderId = "ord_99",
      customerId = "u1",
      storeName = "Butt Karahi",
      status = OrderStatus.PREPARING.name,
      title = "Chef Is Cooking",
      body = "Preparing your order",
      fcmToken = profile.fcmToken
    )
    assertEquals("FCM", record.deliveryChannel)
    assertEquals("PENDING", record.dispatchStatus)
  }
}
