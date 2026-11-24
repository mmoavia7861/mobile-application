package com.example

import com.example.data.model.Order
import com.example.data.model.OrderItemRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RecentOrdersUiState
import com.example.ui.customer.OrderDisplayUtils
import com.example.ui.customer.RecentOrderFilter
import com.example.ui.customer.RecentOrderSortOption
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class CustomerProfileOrdersTest {

  private fun sampleOrders(): List<Order> = listOf(
    Order(
      id = "ord_1",
      storeName = "Savour Foods",
      status = OrderStatus.DELIVERED.name,
      createdAt = Timestamp(1_764_000_100L, 0)
    ),
    Order(
      id = "ord_2",
      storeName = "cheezious",
      status = OrderStatus.ON_THE_WAY.name,
      createdAt = Timestamp(1_764_000_300L, 0)
    ),
    Order(
      id = "ord_3",
      storeName = "Al-Baik Express",
      status = OrderStatus.DELIVERED.name,
      createdAt = Timestamp(1_764_000_200L, 0)
    ),
    Order(
      id = "ord_4",
      storeName = "Savour Foods",
      status = OrderStatus.PREPARING.name,
      createdAt = Timestamp(1_764_000_400L, 0)
    )
  )

  @Test
  fun `summarizeOrderItems formats single and multiple items cleanly`() {
    val empty = OrderDisplayUtils.summarizeOrderItems(emptyList())
    assertEquals("Order items unavailable", empty)

    val twoItems = listOf(
      OrderItemRecord(productId = "p1", productName = "Chicken Biryani", price = 390.0, quantity = 2),
      OrderItemRecord(productId = "p2", productName = "Shami Kabab", price = 90.0, quantity = 1)
    )
    assertEquals("2x Chicken Biryani, 1x Shami Kabab", OrderDisplayUtils.summarizeOrderItems(twoItems))

    val fourItems = twoItems + listOf(
      OrderItemRecord(productId = "p3", productName = "Raita", price = 60.0, quantity = 1),
      OrderItemRecord(productId = "p4", productName = "Zarda", price = 180.0, quantity = 1)
    )
    assertEquals("2x Chicken Biryani, 1x Shami Kabab +2 more", OrderDisplayUtils.summarizeOrderItems(fourItems))
  }

  @Test
  fun `formatStatusLabel normalizes enum names for display`() {
    assertEquals("On The Way", OrderDisplayUtils.formatStatusLabel(OrderStatus.ON_THE_WAY.name))
    assertEquals("Delivered", OrderDisplayUtils.formatStatusLabel(OrderStatus.DELIVERED.name))
    assertEquals("Preparing", OrderDisplayUtils.formatStatusLabel(OrderStatus.PREPARING.name))
  }

  @Test
  fun `isCompletedOrPastStatus distinguishes finished from active orders`() {
    assertTrue(OrderDisplayUtils.isCompletedOrPastStatus(OrderStatus.DELIVERED.name))
    assertTrue(OrderDisplayUtils.isCompletedOrPastStatus(OrderStatus.CANCELLED.name))
    assertTrue(OrderDisplayUtils.isCompletedOrPastStatus(OrderStatus.REJECTED.name))

    assertFalse(OrderDisplayUtils.isCompletedOrPastStatus(OrderStatus.NEW.name))
    assertFalse(OrderDisplayUtils.isCompletedOrPastStatus(OrderStatus.PREPARING.name))
    assertFalse(OrderDisplayUtils.isCompletedOrPastStatus(OrderStatus.ON_THE_WAY.name))
  }

  @Test
  fun `formatOrderDate handles null and valid epoch seconds`() {
    assertEquals("Recent order", OrderDisplayUtils.formatOrderDate(null))
    assertEquals("Recent order", OrderDisplayUtils.formatOrderDate(0L))

    val formatted = OrderDisplayUtils.formatOrderDate(1764000000L)
    assertTrue(formatted.contains("2025") || formatted.contains("2026"))
    assertTrue(formatted.contains("•"))
  }

  @Test
  fun `RecentOrdersUiState holds orders cleanly`() {
    val sampleOrder = Order(
      id = "ord_101",
      storeName = "Savour Foods",
      totalAmount = 850.0,
      status = OrderStatus.DELIVERED.name
    )
    val state: RecentOrdersUiState = RecentOrdersUiState.Success(listOf(sampleOrder))
    assertTrue(state is RecentOrdersUiState.Success)
    assertEquals(1, (state as RecentOrdersUiState.Success).orders.size)
    assertEquals("Savour Foods", state.orders.first().storeName)
  }

  @Test
  fun `filterAndSortOrders sorts by newest and oldest date accurately`() {
    val orders = sampleOrders()

    val newestFirst = OrderDisplayUtils.filterAndSortOrders(
      orders = orders,
      sortOption = RecentOrderSortOption.DATE_NEWEST
    )
    assertEquals(listOf("ord_4", "ord_2", "ord_3", "ord_1"), newestFirst.map { it.id })

    val oldestFirst = OrderDisplayUtils.filterAndSortOrders(
      orders = orders,
      sortOption = RecentOrderSortOption.DATE_OLDEST
    )
    assertEquals(listOf("ord_1", "ord_3", "ord_2", "ord_4"), oldestFirst.map { it.id })
  }

  @Test
  fun `filterAndSortOrders sorts by merchant name A to Z and Z to A with newest tie-breaker`() {
    val orders = sampleOrders()

    val merchantAZ = OrderDisplayUtils.filterAndSortOrders(
      orders = orders,
      sortOption = RecentOrderSortOption.MERCHANT_AZ
    )
    // Al-Baik Express -> cheezious -> Savour Foods (ord_4 newer than ord_1)
    assertEquals(listOf("ord_3", "ord_2", "ord_4", "ord_1"), merchantAZ.map { it.id })

    val merchantZA = OrderDisplayUtils.filterAndSortOrders(
      orders = orders,
      sortOption = RecentOrderSortOption.MERCHANT_ZA
    )
    // Savour Foods (ord_4 newer than ord_1) -> cheezious -> Al-Baik Express
    assertEquals(listOf("ord_4", "ord_1", "ord_2", "ord_3"), merchantZA.map { it.id })
  }

  @Test
  fun `extractDistinctMerchants deduplicates case-insensitively and sorts alphabetically`() {
    val orders = sampleOrders() + Order(
      id = "ord_5",
      storeName = "  savour foods  ",
      status = OrderStatus.DELIVERED.name
    ) + Order(id = "ord_6", storeName = "   ")

    val merchants = OrderDisplayUtils.extractDistinctMerchants(orders)
    assertEquals(listOf("Al-Baik Express", "cheezious", "Savour Foods"), merchants)
  }

  @Test
  fun `filterAndSortOrders combines status filter, merchant filter, and sort option`() {
    val orders = sampleOrders()

    val savourDeliveredOldest = OrderDisplayUtils.filterAndSortOrders(
      orders = orders,
      statusFilter = RecentOrderFilter.DELIVERED,
      merchantFilter = "savour foods",
      sortOption = RecentOrderSortOption.DATE_OLDEST
    )
    assertEquals(listOf("ord_1"), savourDeliveredOldest.map { it.id })

    val activeOnlyMerchantAZ = OrderDisplayUtils.filterAndSortOrders(
      orders = orders,
      statusFilter = RecentOrderFilter.ACTIVE,
      merchantFilter = null,
      sortOption = RecentOrderSortOption.MERCHANT_AZ
    )
    assertEquals(listOf("ord_2", "ord_4"), activeOnlyMerchantAZ.map { it.id })
  }
}
