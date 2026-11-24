package com.example

import com.example.data.model.OrderStatus
import com.example.ui.components.OrderTrackingMapper
import org.junit.Assert.*
import org.junit.Test

class OrderTrackingStatusTest {

  @Test
  fun `active Firestore order statuses map to monotonically increasing progress`() {
    val activeStatuses = listOf(
      OrderStatus.NEW,
      OrderStatus.ACCEPTED,
      OrderStatus.PREPARING,
      OrderStatus.READY,
      OrderStatus.ON_THE_WAY,
      OrderStatus.DELIVERED
    )

    var prevProgress = 0f
    activeStatuses.forEachIndexed { expectedIdx, status ->
      val stage = OrderTrackingMapper.mapStatus(status.name, "Savour Foods")
      assertEquals(expectedIdx, stage.stepIndex)
      assertTrue(
        "Progress for ${status.name} (${stage.progressFraction}) should exceed previous ($prevProgress)",
        stage.progressFraction > prevProgress
      )
      assertFalse(stage.isTerminalError)
      assertEquals(status != OrderStatus.DELIVERED, stage.isLive)
      assertTrue(stage.headline.isNotBlank())
      assertTrue(stage.subtitle.isNotBlank())
      prevProgress = stage.progressFraction
    }
    assertEquals(1.0f, prevProgress, 0.001f)
  }

  @Test
  fun `rejected and cancelled Firestore statuses mark terminal error state`() {
    val rejected = OrderTrackingMapper.mapStatus(OrderStatus.REJECTED.name, "Butt Karahi")
    assertTrue(rejected.isTerminalError)
    assertFalse(rejected.isLive)
    assertEquals(-1, rejected.stepIndex)
    assertEquals(0f, rejected.progressFraction, 0.001f)
    assertTrue(rejected.subtitle.contains("Butt Karahi"))

    val cancelled = OrderTrackingMapper.mapStatus(OrderStatus.CANCELLED.name, "Butt Karahi")
    assertTrue(cancelled.isTerminalError)
    assertFalse(cancelled.isLive)
    assertEquals(-1, cancelled.stepIndex)
    assertEquals(0f, cancelled.progressFraction, 0.001f)
  }

  @Test
  fun `status mapper normalizes lowercase and whitespace safely`() {
    val stage = OrderTrackingMapper.mapStatus("  on_the_way ", "Student Biryani")
    assertEquals(OrderStatus.ON_THE_WAY.name, stage.statusKey)
    assertEquals(4, stage.stepIndex)
    assertTrue(stage.isLive)
    assertEquals("Rider Is On the Way", stage.headline)
  }

  @Test
  fun `milestones list covers all six delivery lifecycle steps`() {
    assertEquals(6, OrderTrackingMapper.MILESTONES.size)
    assertEquals(OrderStatus.NEW, OrderTrackingMapper.MILESTONES[0].status)
    assertEquals(OrderStatus.READY, OrderTrackingMapper.MILESTONES[3].status)
    assertEquals(OrderStatus.DELIVERED, OrderTrackingMapper.MILESTONES[5].status)
  }
}
