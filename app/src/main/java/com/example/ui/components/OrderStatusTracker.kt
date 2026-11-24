package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.customer.OrderDisplayUtils
import com.example.ui.theme.MealVerseDeepTeal
import com.example.ui.theme.MealVerseWarmOrange
import com.example.ui.theme.isMealVerseDarkTheme
import java.util.Locale

data class OrderStageInfo(
  val statusKey: String,
  val headline: String,
  val subtitle: String,
  val etaLabel: String,
  val stepIndex: Int,
  val progressFraction: Float,
  val isLive: Boolean,
  val isTerminalError: Boolean
)

data class OrderTrackingMilestone(
  val status: OrderStatus,
  val shortLabel: String,
  val icon: ImageVector
)

object OrderTrackingMapper {
  val MILESTONES = listOf(
    OrderTrackingMilestone(OrderStatus.NEW, "Placed", Icons.Default.Receipt),
    OrderTrackingMilestone(OrderStatus.ACCEPTED, "Accepted", Icons.Default.ThumbUp),
    OrderTrackingMilestone(OrderStatus.PREPARING, "Cooking", Icons.Default.Restaurant),
    OrderTrackingMilestone(OrderStatus.READY, "Ready", Icons.Default.Inventory2),
    OrderTrackingMilestone(OrderStatus.ON_THE_WAY, "On Way", Icons.Default.DeliveryDining),
    OrderTrackingMilestone(OrderStatus.DELIVERED, "Delivered", Icons.Default.CheckCircle)
  )

  fun mapStatus(rawStatus: String, storeName: String = "the restaurant"): OrderStageInfo {
    val normalized = rawStatus.trim().uppercase(Locale.US)
    val safeStore = storeName.ifBlank { "the restaurant" }

    return when (normalized) {
      OrderStatus.NEW.name -> OrderStageInfo(
        statusKey = OrderStatus.NEW.name,
        headline = "Order Sent to Kitchen",
        subtitle = "Waiting for $safeStore to accept your order",
        etaLabel = "~25–35 mins",
        stepIndex = 0,
        progressFraction = 0.14f,
        isLive = true,
        isTerminalError = false
      )
      OrderStatus.ACCEPTED.name -> OrderStageInfo(
        statusKey = OrderStatus.ACCEPTED.name,
        headline = "Order Accepted",
        subtitle = "$safeStore confirmed your order and queued it",
        etaLabel = "~20–30 mins",
        stepIndex = 1,
        progressFraction = 0.32f,
        isLive = true,
        isTerminalError = false
      )
      OrderStatus.PREPARING.name -> OrderStageInfo(
        statusKey = OrderStatus.PREPARING.name,
        headline = "Preparing Your Meal",
        subtitle = "Chefs at $safeStore are freshly cooking your food",
        etaLabel = "~15–20 mins",
        stepIndex = 2,
        progressFraction = 0.52f,
        isLive = true,
        isTerminalError = false
      )
      OrderStatus.READY.name -> OrderStageInfo(
        statusKey = OrderStatus.READY.name,
        headline = "Ready for Rider Pickup",
        subtitle = "Packed hot and waiting for rider dispatch",
        etaLabel = "~10–12 mins",
        stepIndex = 3,
        progressFraction = 0.70f,
        isLive = true,
        isTerminalError = false
      )
      OrderStatus.ON_THE_WAY.name -> OrderStageInfo(
        statusKey = OrderStatus.ON_THE_WAY.name,
        headline = "Rider Is On the Way",
        subtitle = "Your meal is heading to your doorstep right now",
        etaLabel = "Arriving soon",
        stepIndex = 4,
        progressFraction = 0.88f,
        isLive = true,
        isTerminalError = false
      )
      OrderStatus.DELIVERED.name -> OrderStageInfo(
        statusKey = OrderStatus.DELIVERED.name,
        headline = "Order Delivered",
        subtitle = "Delivered from $safeStore. Enjoy your meal!",
        etaLabel = "Delivered",
        stepIndex = 5,
        progressFraction = 1.0f,
        isLive = false,
        isTerminalError = false
      )
      OrderStatus.REJECTED.name -> OrderStageInfo(
        statusKey = OrderStatus.REJECTED.name,
        headline = "Order Declined",
        subtitle = "$safeStore was unable to fulfill this order",
        etaLabel = "Closed",
        stepIndex = -1,
        progressFraction = 0f,
        isLive = false,
        isTerminalError = true
      )
      OrderStatus.CANCELLED.name -> OrderStageInfo(
        statusKey = OrderStatus.CANCELLED.name,
        headline = "Order Cancelled",
        subtitle = "This order has been cancelled",
        etaLabel = "Cancelled",
        stepIndex = -1,
        progressFraction = 0f,
        isLive = false,
        isTerminalError = true
      )
      else -> OrderStageInfo(
        statusKey = OrderStatus.NEW.name,
        headline = "Order Status: ${OrderDisplayUtils.formatStatusLabel(rawStatus)}",
        subtitle = "Syncing live status from $safeStore",
        etaLabel = "Live",
        stepIndex = 0,
        progressFraction = 0.14f,
        isLive = true,
        isTerminalError = false
      )
    }
  }
}

@Composable
fun LiveSyncPulseBadge(
  isLive: Boolean,
  isTerminalError: Boolean,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "firestoreSyncPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.45f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 0.3f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  val isDark = isMealVerseDarkTheme()
  val dotColor = when {
    isTerminalError -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
    isLive -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    else -> MaterialTheme.colorScheme.primary
  }
  val bgColor = when {
    isTerminalError -> if (isDark) Color(0xFF3E1818) else Color(0xFFFFEBEE)
    isLive -> if (isDark) Color(0xFF173620) else Color(0xFFE8F5E9)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
  }
  val label = when {
    isTerminalError -> "CLOSED"
    isLive -> "LIVE SYNC"
    else -> "COMPLETED"
  }

  Surface(
    shape = RoundedCornerShape(50),
    color = bgColor,
    modifier = modifier.testTag("live_firestore_sync_badge")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.size(10.dp),
        contentAlignment = Alignment.Center
      ) {
        if (isLive) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .scale(pulseScale)
              .alpha(pulseAlpha)
              .clip(CircleShape)
              .background(dotColor.copy(alpha = 0.45f))
          )
        }
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(dotColor)
        )
      }
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = dotColor
      )
    }
  }
}

/**
 * Full real-time Firestore order status indicator with animated progress bar,
 * pulsing current milestone icon, and 6-stage lifecycle tracker.
 */
@Composable
fun RealTimeOrderStatusIndicator(
  order: Order,
  modifier: Modifier = Modifier
) {
  val stage = OrderTrackingMapper.mapStatus(order.status, order.storeName)
  val animatedProgress by animateFloatAsState(
    targetValue = stage.progressFraction,
    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
    label = "orderProgress"
  )
  val accentColor by animateColorAsState(
    targetValue = when {
      stage.isTerminalError -> Color(0xFFC62828)
      stage.statusKey == OrderStatus.DELIVERED.name -> Color(0xFF2E7D32)
      stage.statusKey == OrderStatus.ON_THE_WAY.name -> MealVerseWarmOrange
      else -> MealVerseDeepTeal
    },
    animationSpec = tween(durationMillis = 400),
    label = "stageAccentColor"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "activeStepRing")
  val ringScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.18f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "activeRingScale"
  )

  val currentIcon = OrderTrackingMapper.MILESTONES
    .getOrNull(stage.stepIndex)?.icon
    ?: if (stage.isTerminalError) Icons.Default.Cancel else Icons.Default.Info

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(accentColor.copy(alpha = 0.06f))
      .border(1.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
      .padding(14.dp)
      .testTag("realtime_order_status_indicator_${order.id}")
  ) {
    // Top Status Headline + Live Sync Badge + ETA
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = currentIcon,
          contentDescription = stage.headline,
          tint = accentColor,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LiveSyncPulseBadge(
            isLive = stage.isLive,
            isTerminalError = stage.isTerminalError
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.12f)
          ) {
            Text(
              text = stage.etaLabel,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
          text = stage.headline,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            color = accentColor
          ),
          modifier = Modifier.testTag("order_live_status_headline")
        )
        Text(
          text = stage.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (!stage.isTerminalError) {
      Spacer(modifier = Modifier.height(14.dp))

      // Smooth Animated Linear Progress Bar
      LinearProgressIndicator(
        progress = { animatedProgress },
        color = accentColor,
        trackColor = accentColor.copy(alpha = 0.16f),
        modifier = Modifier
          .fillMaxWidth()
          .height(7.dp)
          .clip(RoundedCornerShape(50))
          .testTag("order_live_progress_bar")
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 6-Stage Real-Time Milestone Stepper
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        OrderTrackingMapper.MILESTONES.forEachIndexed { index, milestone ->
          val isCompleted = index < stage.stepIndex ||
            (stage.statusKey == OrderStatus.DELIVERED.name && index == stage.stepIndex)
          val isCurrent = index == stage.stepIndex && stage.isLive

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .weight(1f)
              .testTag("order_stage_node_${milestone.status.name}")
          ) {
            Box(
              modifier = Modifier.size(30.dp),
              contentAlignment = Alignment.Center
            ) {
              if (isCurrent) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.22f))
                )
              }
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      isCompleted || isCurrent -> accentColor
                      else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                  ),
                contentAlignment = Alignment.Center
              ) {
                if (isCompleted) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = milestone.shortLabel,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                  )
                } else if (isCurrent) {
                  Icon(
                    imageVector = milestone.icon,
                    contentDescription = milestone.shortLabel,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                  )
                } else {
                  Text(
                    text = "${index + 1}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = milestone.shortLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = if (isCompleted || isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCompleted || isCurrent) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
              ),
              textAlign = TextAlign.Center,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    val updatedSeconds = order.updatedAt?.seconds ?: order.createdAt?.seconds
    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Firestore status: ${order.status.replace('_', ' ')}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "Updated: ${OrderDisplayUtils.formatOrderDate(updatedSeconds)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

/**
 * Compact live status indicator banner shown on CustomerHomeScreen so customers
 * see real-time Firestore order updates without leaving the marketplace.
 */
@Composable
fun CompactRealTimeOrderBanner(
  order: Order,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val stage = OrderTrackingMapper.mapStatus(order.status, order.storeName)
  val animatedProgress by animateFloatAsState(
    targetValue = stage.progressFraction,
    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
    label = "bannerOrderProgress"
  )

  Card(
    shape = RoundedCornerShape(18.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("home_live_order_status_banner")
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.horizontalGradient(
            colors = listOf(Color(0xFF0A4D3C), MealVerseDeepTeal)
          )
        )
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = OrderTrackingMapper.MILESTONES.getOrNull(stage.stepIndex)?.icon
              ?: Icons.Default.DeliveryDining,
            contentDescription = stage.headline,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            LiveSyncPulseBadge(
              isLive = stage.isLive,
              isTerminalError = stage.isTerminalError
            )
            Text(
              text = stage.etaLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold
              )
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stage.headline,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )
          )
          Text(
            text = "${order.storeName} • Rs. ${order.totalAmount.toInt()} (${order.paymentMethod})",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          Icons.Default.ChevronRight,
          contentDescription = "Track live order",
          tint = Color.White
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      LinearProgressIndicator(
        progress = { animatedProgress },
        color = Color(0xFFFFB300),
        trackColor = Color.White.copy(alpha = 0.22f),
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(50))
      )
    }
  }
}
