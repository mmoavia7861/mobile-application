package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.Review
import com.example.data.reviews.ReviewValidator
import com.example.data.reviews.StoreRatingSummary
import com.example.ui.customer.OrderDisplayUtils
import com.example.ui.customer.get3DFoodGraphic
import com.example.ui.theme.MealVerseDeepTeal
import com.example.ui.theme.MealVerseSaffronYellow
import com.example.ui.theme.MealVerseWarmOrange
import com.example.ui.theme.isMealVerseDarkTheme

@Composable
fun StarRatingSelector(
  rating: Int,
  onRatingSelected: (Int) -> Unit,
  enabled: Boolean = true,
  starSize: Dp = 36.dp
) {
  val starGold = MealVerseSaffronYellow

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      for (star in 1..ReviewValidator.MAX_RATING) {
        val selected = star <= rating
        val scale by animateFloatAsState(
          targetValue = if (star == rating) 1.18f else 1.0f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
          ),
          label = "starScale$star"
        )
        IconButton(
          onClick = { if (enabled) onRatingSelected(star) },
          enabled = enabled,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .scale(scale)
            .testTag("review_star_$star")
        ) {
          Icon(
            imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = "$star Star${if (star > 1) "s" else ""}",
            tint = if (selected) starGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(starSize)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Surface(
      shape = RoundedCornerShape(50),
      color = starGold.copy(alpha = 0.16f)
    ) {
      Text(
        text = "${ReviewValidator.ratingLabel(rating)} ($rating/5)",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = if (isMealVerseDarkTheme()) starGold else Color(0xFFB26A00),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
      )
    }
  }
}

@Composable
fun StarRatingDisplayRow(
  rating: Int,
  starSize: Dp = 16.dp
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (star in 1..ReviewValidator.MAX_RATING) {
      val filled = star <= rating
      Icon(
        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
        contentDescription = null,
        tint = if (filled) MealVerseSaffronYellow else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.size(starSize)
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewSubmissionDialog(
  order: Order,
  existingReview: Review? = null,
  isSubmitting: Boolean,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onSubmit: (rating: Int, comment: String) -> Unit
) {
  var rating by remember(order.id, existingReview) {
    mutableIntStateOf(existingReview?.rating?.coerceIn(1, 5) ?: 5)
  }
  var comment by remember(order.id, existingReview) {
    mutableStateOf(existingReview?.comment.orEmpty())
  }
  var localError by remember(errorMessage) { mutableStateOf(errorMessage) }

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Animated3DFoodThumbnail(
          imageRes = get3DFoodGraphic(order.storeName, order.items.firstOrNull()?.productName ?: ""),
          contentDescription = order.storeName,
          size = 44.dp,
          cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = if (existingReview == null) "Rate Your Meal" else "Update Your Review",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
          )
          Text(
            text = order.storeName.ifBlank { "MealVerse Restaurant" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "How was your order (${OrderDisplayUtils.summarizeOrderItems(order.items)})?",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        StarRatingSelector(
          rating = rating,
          onRatingSelected = {
            rating = it
            localError = null
          },
          enabled = !isSubmitting
        )

        // Quick-tap feedback tags
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          ReviewValidator.QUICK_FEEDBACK_TAGS.forEach { tag ->
            val selected = comment.contains(tag, ignoreCase = true)
            FilterChip(
              selected = selected,
              enabled = !isSubmitting,
              onClick = {
                comment = ReviewValidator.appendQuickTag(comment, tag)
                localError = null
              },
              label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MealVerseDeepTeal.copy(alpha = 0.16f),
                selectedLabelColor = MealVerseDeepTeal
              )
            )
          }
        }

        OutlinedTextField(
          value = comment,
          onValueChange = {
            if (it.length <= ReviewValidator.MAX_COMMENT_LENGTH) {
              comment = it
              localError = null
            }
          },
          enabled = !isSubmitting,
          label = { Text("Share your review comment *") },
          placeholder = { Text("Tell others about food quality, taste, and delivery...") },
          minLines = 3,
          maxLines = 4,
          shape = RoundedCornerShape(14.dp),
          supportingText = {
            Text("${comment.trim().length}/${ReviewValidator.MAX_COMMENT_LENGTH}")
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("review_comment_input")
        )

        val displayError = localError ?: errorMessage
        if (!displayError.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = displayError,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (comment.trim().length < ReviewValidator.MIN_COMMENT_LENGTH) {
            localError = "Please write a short comment (at least ${ReviewValidator.MIN_COMMENT_LENGTH} characters) or tap a quick tag."
            return@Button
          }
          onSubmit(rating, comment.trim())
        },
        enabled = !isSubmitting,
        colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("submit_review_btn")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Submitting...")
        } else {
          Text(if (existingReview == null) "Submit Review" else "Update Review", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isSubmitting,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun SubmittedReviewSummaryCard(
  review: Review,
  onEditReview: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = isMealVerseDarkTheme()
  val bgColor = if (isDark) Color(0xFF1F2B1D) else Color(0xFFFFFBF0)
  val borderColor = MealVerseSaffronYellow.copy(alpha = if (isDark) 0.4f else 0.55f)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, borderColor, RoundedCornerShape(14.dp))
      .testTag("submitted_review_card_${review.orderId}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          StarRatingDisplayRow(rating = review.rating, starSize = 16.dp)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${review.rating}.0 • ${ReviewValidator.ratingLabel(review.rating)}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onEditReview)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("edit_review_btn_${review.orderId}")
        ) {
          Icon(
            Icons.Default.Edit,
            contentDescription = "Edit review",
            tint = MealVerseDeepTeal,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Edit",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = MealVerseDeepTeal
            )
          )
        }
      }
      if (review.comment.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "“${review.comment}”",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun RateOrderPromptButton(
  orderId: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = MealVerseWarmOrange.copy(alpha = 0.14f),
      contentColor = MealVerseWarmOrange
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .testTag("rate_order_btn_$orderId")
  ) {
    Icon(
      imageVector = Icons.Default.RateReview,
      contentDescription = null,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "Rate & Review Order",
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun StoreReviewsSectionHeader(
  summary: StoreRatingSummary,
  customerReviewCount: Int
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("store_reviews_summary_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Customer Reviews",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (customerReviewCount > 0) {
            "$customerReviewCount verified post-delivery review${if (customerReviewCount > 1) "s" else ""}"
          } else {
            "Verified order ratings from MealVerse diners"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MealVerseSaffronYellow.copy(alpha = 0.18f)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MealVerseSaffronYellow,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${summary.averageRating}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = " (${summary.totalReviewCount})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun StoreReviewCard(
  review: Review
) {
  val initials = review.customerName
    .split(' ')
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }
    .ifBlank { "MC" }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("store_review_item_${review.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(MealVerseDeepTeal.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = initials,
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MealVerseDeepTeal
              )
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = review.customerName.ifBlank { "MealVerse Diner" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                Icons.Default.Verified,
                contentDescription = "Verified Order",
                tint = MealVerseDeepTeal,
                modifier = Modifier.size(14.dp)
              )
            }
            Text(
              text = OrderDisplayUtils.formatOrderDate(review.createdAt?.seconds),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        StarRatingDisplayRow(rating = review.rating, starSize = 15.dp)
      }

      if (review.comment.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = review.comment,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      if (review.orderItemsSummary.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
          Text(
            text = "Ordered: ${review.orderItemsSummary}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }
  }
}
