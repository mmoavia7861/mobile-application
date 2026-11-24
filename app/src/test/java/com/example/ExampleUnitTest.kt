package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testPakistaniCitiesList() {
    assertTrue(PAKISTAN_CITIES.contains("Islamabad"))
    assertTrue(PAKISTAN_CITIES.contains("Lahore"))
    assertTrue(PAKISTAN_CITIES.contains("Karachi"))
    assertTrue(PAKISTAN_CITIES.contains("Peshawar"))
    assertTrue(PAKISTAN_CITIES.contains("Rawalpindi"))
  }

  @Test
  fun testFoodCategories() {
    assertTrue(FOOD_CATEGORIES.contains("Biryani"))
    assertTrue(FOOD_CATEGORIES.contains("Karahi"))
    assertTrue(FOOD_CATEGORIES.contains("Desi Nashta"))
    assertTrue(FOOD_CATEGORIES.contains("Home Chef"))
  }

  @Test
  fun testOrderCalculation() {
    val biryani = Product(id = "p1", name = "Chicken Biryani", price = 390.0, category = "Biryani")
    val shami = Product(id = "p2", name = "Shami Kabab", price = 90.0, category = "Sides")

    val cart = listOf(
      CartItem(product = biryani, quantity = 2),
      CartItem(product = shami, quantity = 3)
    )

    val subtotal = cart.sumOf { it.product.price * it.quantity }
    val deliveryFee = 80.0
    val platformFee = 20.0
    val total = subtotal + deliveryFee + platformFee

    // 2 * 390 = 780; 3 * 90 = 270; subtotal = 1050; total = 1150
    assertEquals(1050.0, subtotal, 0.01)
    assertEquals(1150.0, total, 0.01)
  }

  @Test
  fun testOrderStatusTransitions() {
    val newStatus = OrderStatus.NEW
    val accepted = OrderStatus.ACCEPTED
    val preparing = OrderStatus.PREPARING
    val ready = OrderStatus.READY
    val onTheWay = OrderStatus.ON_THE_WAY
    val delivered = OrderStatus.DELIVERED

    assertNotNull(newStatus)
    assertNotNull(accepted)
    assertNotNull(preparing)
    assertNotNull(ready)
    assertNotNull(onTheWay)
    assertNotNull(delivered)
  }
}
