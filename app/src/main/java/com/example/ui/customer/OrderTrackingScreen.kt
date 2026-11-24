package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.RecentOrdersUiState
import com.example.data.model.Review
import com.example.data.repository.MealVerseRepository
import com.example.ui.components.Animated3DFoodThumbnail
import com.example.ui.components.LiveSyncPulseBadge
import com.example.ui.components.RateOrderPromptButton
import com.example.ui.components.RealTimeOrderStatusIndicator
import com.example.ui.components.ReviewSubmissionDialog
import com.example.ui.components.SubmittedReviewSummaryCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
  currentUserId: String,
  repository: MealVerseRepository,
  highlightedOrderId: String? = null,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var retryTrigger by remember { mutableIntStateOf(0) }
  val ordersState by remember(currentUserId, retryTrigger) {
    repository.observeCustomerOrdersState(currentUserId)
  }.collectAsStateWithLifecycle(initialValue = RecentOrdersUiState.Loading)

  val customerReviews by remember(currentUserId) {
    repository.observeCustomerReviews(currentUserId)
  }.collectAsStateWithLifecycle(initialValue = emptyMap())

  var orderToReview by remember { mutableStateOf<Order?>(null) }
  var isSubmittingReview by remember { mutableStateOf(false) }
  var reviewErrorMessage by remember { mutableStateOf<String?>(null) }

  val highlightedOrder by remember(highlightedOrderId) {
    repository.observeOrderById(highlightedOrderId ?: "")
  }.collectAsStateWithLifecycle(initialValue = null)

  val mergedOrders = remember(ordersState, highlightedOrder, highlightedOrderId) {
    val list = (ordersState as? RecentOrdersUiState.Success)?.orders ?: emptyList()
    val withPinned = if (highlightedOrder != null && list.none { it.id == highlightedOrder?.id }) {
      listOf(highlightedOrder!!) + list
    } else if (highlightedOrder != null) {
      list.map { if (it.id == highlightedOrder?.id) highlightedOrder!! else it }
    } else {
      list
    }
    if (!highlightedOrderId.isNullOrBlank()) {
      withPinned.sortedByDescending { it.id == highlightedOrderId }
    } else {
      withPinned
    }
  }

  val hasAnyLiveOrder = mergedOrders.any {
    !OrderDisplayUtils.isCompletedOrPastStatus(it.status)
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Live Order Tracking", fontWeight = FontWeight.Bold)
            Text(
              text = "Real-time updates from Firestore",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("order_tracking_back_btn")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (mergedOrders.isNotEmpty()) {
            LiveSyncPulseBadge(
              isLive = hasAnyLiveOrder,
              isTerminalError = false,
              modifier = Modifier.padding(end = 12.dp)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { padding ->
    when (val state = ordersState) {
      is RecentOrdersUiState.Loading -> {
        if (mergedOrders.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding)
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = MealVerseDeepTeal)
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Syncing live order status from Firestore...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      is RecentOrdersUiState.Error -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.widthIn(max = 520.dp)
          ) {
            Column(
              modifier = Modifier.padding(22.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = { retryTrigger++ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
              ) {
                Text("Retry Live Sync")
              }
            }
          }
        }
      }

      is RecentOrdersUiState.Success -> {
        if (mergedOrders.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding)
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(68.dp),
                tint = MealVerseDeepTeal.copy(alpha = 0.65f)
              )
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                "No orders placed yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                "Explore meals in your city and track your order live from kitchen to doorstep.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(18.dp))
              Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
              ) {
                Text("Explore Restaurants", fontWeight = FontWeight.Bold)
              }
            }
          }
        } else {
          val liveOrders = mergedOrders.filter { !OrderDisplayUtils.isCompletedOrPastStatus(it.status) }
          val pastOrders = mergedOrders.filter { OrderDisplayUtils.isCompletedOrPastStatus(it.status) }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentAlignment = Alignment.TopCenter
          ) {
            LazyColumn(
              modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              if (liveOrders.isNotEmpty()) {
                item {
                  Text(
                    text = "Active Live Orders (${liveOrders.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                  )
                }
                items(liveOrders, key = { it.id.ifBlank { "${it.storeId}_${it.createdAt?.seconds}" } }) { order ->
                  OrderTrackingCard(
                    order = order,
                    isHighlighted = order.id == highlightedOrderId,
                    submittedReview = customerReviews[order.id],
                    onRateOrder = {
                      reviewErrorMessage = null
                      orderToReview = order
                    }
                  )
                }
              }

              if (pastOrders.isNotEmpty()) {
                item {
                  Text(
                    text = "Completed & Past Orders (${pastOrders.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(top = if (liveOrders.isNotEmpty()) 6.dp else 0.dp)
                  )
                }
                items(pastOrders, key = { it.id.ifBlank { "${it.storeId}_${it.createdAt?.seconds}" } }) { order ->
                  OrderTrackingCard(
                    order = order,
                    isHighlighted = order.id == highlightedOrderId,
                    submittedReview = customerReviews[order.id],
                    onRateOrder = {
                      reviewErrorMessage = null
                      orderToReview = order
                    }
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (orderToReview != null) {
    val targetOrder = orderToReview!!
    val existingReview = customerReviews[targetOrder.id]
    ReviewSubmissionDialog(
      order = targetOrder,
      existingReview = existingReview,
      isSubmitting = isSubmittingReview,
      errorMessage = reviewErrorMessage,
      onDismiss = {
        orderToReview = null
        reviewErrorMessage = null
      },
      onSubmit = { rating, comment ->
        scope.launch {
          isSubmittingReview = true
          reviewErrorMessage = null
          val result = repository.submitOrderReview(
            order = targetOrder,
            customerName = targetOrder.customerName,
            rating = rating,
            comment = comment
          )
          isSubmittingReview = false
          result.onSuccess {
            orderToReview = null
            snackbarHostState.showSnackbar("Thank you! Your review has been published.")
          }.onFailure { err ->
            reviewErrorMessage = err.localizedMessage ?: "Failed to submit review."
          }
        }
      }
    )
  }
}

@Composable
fun OrderTrackingCard(
  order: Order,
  isHighlighted: Boolean = false,
  submittedReview: Review? = null,
  onRateOrder: () -> Unit = {}
) {
  val isDelivered = order.status == OrderStatus.DELIVERED.name
  val isCancelled = order.status == OrderStatus.CANCELLED.name || order.status == OrderStatus.REJECTED.name

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 5.dp else 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .then(
        if (isHighlighted) {
          Modifier.border(1.5.dp, MealVerseWarmOrange.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
        } else {
          Modifier
        }
      )
      .testTag("order_tracking_card_${order.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Animated3DFoodThumbnail(
            imageRes = get3DFoodGraphic(order.storeName, order.items.firstOrNull()?.productName ?: ""),
            contentDescription = order.storeName,
            size = 48.dp,
            cornerRadius = 12.dp
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = order.storeName.ifBlank { "MealVerse Restaurant" },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "${order.city} • ${OrderDisplayUtils.formatOrderDate(order.createdAt?.seconds)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when {
            isDelivered -> Color(0xFFE8F5E9)
            isCancelled -> Color(0xFFFFEBEE)
            else -> MealVerseDeepTeal.copy(alpha = 0.12f)
          }
        ) {
          Text(
            text = OrderDisplayUtils.formatStatusLabel(order.status),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = when {
                isDelivered -> Color(0xFF2E7D32)
                isCancelled -> Color(0xFFC62828)
                else -> MealVerseDeepTeal
              }
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Real-Time Firestore Status Tracking Indicator
      RealTimeOrderStatusIndicator(order = order)

      HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

      // Items list
      Text("Items (${order.items.size}):", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
      order.items.forEach { item ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("${item.quantity}x ${item.productName}", style = MaterialTheme.typography.bodySmall)
          Text("Rs. ${(item.price * item.quantity).toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      if (order.discountAmount > 0.0 && order.voucherCode.isNotBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            "Voucher (${order.voucherCode})",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF2E7D32)
          )
          Text(
            "- Rs. ${order.discountAmount.toInt()}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2E7D32)
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          "Total (${com.example.ui.components.formatOrderPaymentLabel(order)})",
          fontWeight = FontWeight.Bold
        )
        Text("Rs. ${order.totalAmount.toInt()}", fontWeight = FontWeight.Bold, color = MealVerseDeepTeal)
      }

      Text(
        text = "Deliver to: ${order.deliveryAddress}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
      )

      if (isDelivered && order.id.isNotBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        if (submittedReview != null) {
          SubmittedReviewSummaryCard(
            review = submittedReview,
            onEditReview = onRateOrder
          )
        } else {
          RateOrderPromptButton(
            orderId = order.id,
            onClick = onRateOrder
          )
        }
      }
    }
  }
}
