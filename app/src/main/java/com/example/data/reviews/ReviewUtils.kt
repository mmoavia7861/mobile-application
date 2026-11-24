package com.example.data.reviews

import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Review

data class ReviewValidationResult(
  val isValid: Boolean,
  val sanitizedComment: String = "",
  val errorMessage: String? = null
)

data class StoreRatingSummary(
  val averageRating: Double,
  val totalReviewCount: Long
)

object ReviewValidator {
  const val MIN_RATING = 1
  const val MAX_RATING = 5
  const val MIN_COMMENT_LENGTH = 3
  const val MAX_COMMENT_LENGTH = 500

  val QUICK_FEEDBACK_TAGS = listOf(
    "Fresh & hot!",
    "Authentic taste",
    "Fast delivery",
    "Great packaging",
    "Generous portions"
  )

  fun validateReviewSubmission(
    order: Order,
    currentUserId: String,
    rating: Int,
    comment: String
  ): ReviewValidationResult {
    if (currentUserId.isBlank()) {
      return ReviewValidationResult(
        isValid = false,
        errorMessage = "Please sign in before submitting a review."
      )
    }
    if (order.id.isBlank() || order.storeId.isBlank()) {
      return ReviewValidationResult(
        isValid = false,
        errorMessage = "Order details are incomplete."
      )
    }
    if (order.customerId.isNotBlank() && order.customerId != currentUserId) {
      return ReviewValidationResult(
        isValid = false,
        errorMessage = "You can only review your own completed orders."
      )
    }
    if (!order.status.equals(OrderStatus.DELIVERED.name, ignoreCase = true)) {
      return ReviewValidationResult(
        isValid = false,
        errorMessage = "Reviews can only be submitted after your order has been delivered."
      )
    }
    if (rating !in MIN_RATING..MAX_RATING) {
      return ReviewValidationResult(
        isValid = false,
        errorMessage = "Please select a star rating between 1 and 5."
      )
    }

    val cleanedComment = comment.trim()
    if (cleanedComment.length < MIN_COMMENT_LENGTH) {
      return ReviewValidationResult(
        isValid = false,
        errorMessage = "Please share a short comment (at least $MIN_COMMENT_LENGTH characters)."
      )
    }
    if (cleanedComment.length > MAX_COMMENT_LENGTH) {
      return ReviewValidationResult(
        isValid = false,
        errorMessage = "Comment must be $MAX_COMMENT_LENGTH characters or fewer."
      )
    }

    return ReviewValidationResult(
      isValid = true,
      sanitizedComment = cleanedComment
    )
  }

  fun ratingLabel(rating: Int): String = when (rating) {
    1 -> "Poor"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Very Good"
    5 -> "Excellent!"
    else -> "Select Stars"
  }

  fun appendQuickTag(currentComment: String, tag: String): String {
    val trimmed = currentComment.trim()
    if (trimmed.contains(tag, ignoreCase = true)) return currentComment
    val combined = if (trimmed.isBlank()) tag else "$trimmed • $tag"
    return combined.take(MAX_COMMENT_LENGTH)
  }

  fun computeStoreRatingSummary(
    reviews: List<Review>,
    baseRating: Double = 4.8,
    baseCount: Long = 0L
  ): StoreRatingSummary {
    if (reviews.isEmpty()) {
      return StoreRatingSummary(
        averageRating = baseRating,
        totalReviewCount = baseCount
      )
    }
    val reviewsSum = reviews.sumOf { it.rating.coerceIn(MIN_RATING, MAX_RATING).toDouble() }
    val safeBaseCount = baseCount.coerceAtLeast(0L)
    val totalCount = safeBaseCount + reviews.size
    val weightedAverage = if (totalCount > 0) {
      ((baseRating * safeBaseCount) + reviewsSum) / totalCount
    } else {
      reviewsSum / reviews.size
    }
    val rounded = kotlin.math.round(weightedAverage * 10.0) / 10.0
    return StoreRatingSummary(
      averageRating = rounded,
      totalReviewCount = totalCount
    )
  }
}
