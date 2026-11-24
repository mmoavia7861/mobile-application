package com.example

import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Review
import com.example.data.reviews.ReviewValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostDeliveryReviewTest {

  private fun sampleOrder(status: String = OrderStatus.DELIVERED.name) = Order(
    id = "order_101",
    customerId = "cust_1",
    customerName = "Ayesha Khan",
    storeId = "store_1",
    storeName = "Savour Foods",
    status = status
  )

  @Test
  fun validDeliveredOrderReviewPassesValidation() {
    val result = ReviewValidator.validateReviewSubmission(
      order = sampleOrder(OrderStatus.DELIVERED.name),
      currentUserId = "cust_1",
      rating = 5,
      comment = "  Fresh and hot pulao kabab delivered quickly!  "
    )
    assertTrue(result.isValid)
    assertEquals("Fresh and hot pulao kabab delivered quickly!", result.sanitizedComment)
  }

  @Test
  fun nonDeliveredOrderCannotBeReviewed() {
    val preparingResult = ReviewValidator.validateReviewSubmission(
      order = sampleOrder(OrderStatus.PREPARING.name),
      currentUserId = "cust_1",
      rating = 4,
      comment = "Looks good so far"
    )
    val cancelledResult = ReviewValidator.validateReviewSubmission(
      order = sampleOrder(OrderStatus.CANCELLED.name),
      currentUserId = "cust_1",
      rating = 1,
      comment = "Order cancelled"
    )

    assertFalse(preparingResult.isValid)
    assertFalse(cancelledResult.isValid)
  }

  @Test
  fun ratingOutOfBoundsOrShortCommentIsRejected() {
    val badRating = ReviewValidator.validateReviewSubmission(
      order = sampleOrder(),
      currentUserId = "cust_1",
      rating = 6,
      comment = "Awesome food"
    )
    val shortComment = ReviewValidator.validateReviewSubmission(
      order = sampleOrder(),
      currentUserId = "cust_1",
      rating = 5,
      comment = "ok"
    )

    assertFalse(badRating.isValid)
    assertFalse(shortComment.isValid)
  }

  @Test
  fun anotherCustomerCannotReviewSomeoneElsesOrder() {
    val result = ReviewValidator.validateReviewSubmission(
      order = sampleOrder(),
      currentUserId = "other_user",
      rating = 2,
      comment = "Not my order"
    )
    assertFalse(result.isValid)
  }

  @Test
  fun computeStoreRatingSummaryAggregatesBaseAndNewReviewsAccurately() {
    val reviews = listOf(
      Review(id = "r1", orderId = "o1", storeId = "store_1", rating = 5, comment = "Great"),
      Review(id = "r2", orderId = "o2", storeId = "store_1", rating = 5, comment = "Awesome")
    )
    val summary = ReviewValidator.computeStoreRatingSummary(
      reviews = reviews,
      baseRating = 4.0,
      baseCount = 1L
    )
    // (4.0 * 1 + 5 + 5) / 3 = 14 / 3 = 4.666... -> 4.7
    assertEquals(3L, summary.totalReviewCount)
    assertEquals(4.7, summary.averageRating, 0.01)
  }

  @Test
  fun appendQuickTagAvoidsDuplicatesAndRespectsFormatting() {
    val first = ReviewValidator.appendQuickTag("", "Fresh & hot!")
    assertEquals("Fresh & hot!", first)
    val second = ReviewValidator.appendQuickTag(first, "Fast delivery")
    assertEquals("Fresh & hot! • Fast delivery", second)
    val duplicate = ReviewValidator.appendQuickTag(second, "Fresh & hot!")
    assertEquals(second, duplicate)
  }
}
