package com.example.ui.shopkeeper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.MealVerseRepository
import com.example.ui.components.MapGeoUtils
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopkeeperDashboardScreen(
  currentProfile: UserProfile,
  repository: MealVerseRepository,
  onNavigateToMenu: (Store) -> Unit,
  onSwitchRole: () -> Unit,
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {}
) {
  val scope = rememberCoroutineScope()
  var currentStore by remember { mutableStateOf<Store?>(null) }
  var isLoadingStore by remember { mutableStateOf(true) }

  // Fetch or observe store for current owner
  LaunchedEffect(currentProfile.userId) {
    isLoadingStore = true
    currentStore = repository.getStoreByOwner(currentProfile.userId)
    isLoadingStore = false
  }

  val storeId = currentStore?.id ?: ""
  val storeOwnerId = currentStore?.ownerId ?: ""
  val storeOrders by repository.observeStoreOrders(storeId, storeOwnerId).collectAsState(initial = emptyList())

  val completedOrders = storeOrders.filter { it.status == OrderStatus.DELIVERED.name }
  val grossSales = completedOrders.sumOf { it.totalAmount }
  val commission = grossSales * 0.15 // 15% platform commission
  val netPayout = grossSales - commission

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(currentStore?.name ?: "Merchant Portal", fontWeight = FontWeight.Bold)
            Text("Shopkeeper Dashboard • ${currentStore?.city ?: currentProfile.city}", style = MaterialTheme.typography.bodySmall)
          }
        },
        actions = {
          ThemeQuickToggleButton(
            isDarkTheme = isDarkTheme,
            onToggle = onToggleTheme,
            modifier = Modifier.testTag("shopkeeper_theme_toggle_btn")
          )
          IconButton(onClick = onSwitchRole) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Switch to Customer / Admin")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { padding ->
    if (isLoadingStore) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MealVerseWarmOrange)
      }
    } else if (currentStore == null) {
      Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(64.dp), tint = MealVerseWarmOrange)
          Spacer(modifier = Modifier.height(16.dp))
          Text("No registered store found", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
          Text("Switch to registration or customer mode.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(16.dp))
          Button(onClick = onSwitchRole, colors = ButtonDefaults.buttonColors(containerColor = MealVerseWarmOrange)) {
            Text("Switch Mode")
          }
        }
      }
    } else {
      val store = currentStore!!
      val isApproved = store.status.equals(StoreStatus.APPROVED.name, ignoreCase = true) || store.status.equals("Approved", ignoreCase = true)
      val isPending = store.status.equals(StoreStatus.PENDING.name, ignoreCase = true) || store.status.equals("Pending", ignoreCase = true)
      val isRejected = store.status.equals(StoreStatus.REJECTED.name, ignoreCase = true) || store.status.equals("Rejected", ignoreCase = true)

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Status Alert Banner
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = when {
                isApproved && isDarkTheme -> Color(0xFF173620)
                isApproved -> Color(0xFFE8F5E9)
                isPending && isDarkTheme -> Color(0xFF3A2D12)
                isPending -> Color(0xFFFFF8E1)
                isDarkTheme -> Color(0xFF3E1818)
                else -> Color(0xFFFFEBEE)
              }
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = when {
                  isApproved -> Icons.Default.CheckCircle
                  isPending -> Icons.Default.HourglassTop
                  else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when {
                  isApproved -> if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
                  isPending -> if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFFF57F17)
                  else -> if (isDarkTheme) Color(0xFFEF9A9A) else Color(0xFFC62828)
                },
                modifier = Modifier.size(28.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = when {
                    isApproved -> "Store Status: Approved & Active"
                    isPending -> "Verification Pending"
                    else -> "Verification Rejected"
                  },
                  fontWeight = FontWeight.Bold,
                  color = when {
                    isApproved -> if (isDarkTheme) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                    isPending -> if (isDarkTheme) Color(0xFFFFE082) else Color(0xFFE65100)
                    else -> if (isDarkTheme) Color(0xFFFFCDD2) else Color(0xFFB71C1C)
                  }
                )
                Text(
                  text = when {
                    isApproved -> "Your restaurant is live for customers across ${store.city}."
                    isPending -> "Our administrative team is verifying CNIC and food compliance. You can configure your menu in advance."
                    else -> "Reason: ${store.rejectionReason.ifBlank { "Document clarification needed." }}"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (store.latitude != null && store.longitude != null) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = MealVerseDeepTeal,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Delivery Pin: ${MapGeoUtils.formatCoordinates(store.latitude, store.longitude)}",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                      color = MealVerseDeepTeal
                    )
                  }
                }
              }
            }
          }
        }

        // Quick Actions (Manage Menu)
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
              onClick = { onNavigateToMenu(store) },
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp)
            ) {
              Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Manage Menu")
            }
          }
        }

        // Earnings & Sales Metrics
        item {
          Text("Financial Performance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Orders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${storeOrders.size}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MealVerseDeepTeal))
                Text("${completedOrders.size} delivered", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
              }
            }

            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text("Net Merchant Payout", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Rs. ${netPayout.toInt()}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MealVerseWarmOrange))
                Text("After 15% platform fee", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        // Orders Management Section
        item {
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Incoming Customer Orders (${storeOrders.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          }
        }

        if (storeOrders.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No orders received yet. Once approved, orders from customers will appear here in real time.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        } else {
          items(storeOrders) { order ->
            ShopkeeperOrderCard(
              order = order,
              onUpdateStatus = { newStatus ->
                scope.launch {
                  repository.updateOrderStatus(order.id, newStatus)
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun ShopkeeperOrderCard(
  order: Order,
  onUpdateStatus: (OrderStatus) -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Customer: ${order.customerName}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text("Phone: ${order.customerPhone}", style = MaterialTheme.typography.bodySmall)
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MealVerseWarmOrange.copy(alpha = 0.12f)
        ) {
          Text(
            text = order.status.replace('_', ' '),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MealVerseWarmOrange)
          )
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

      // Items
      order.items.forEach { item ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("${item.quantity}x ${item.productName}", style = MaterialTheme.typography.bodySmall)
          Text("Rs. ${(item.price * item.quantity).toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
      }

      if (order.specialInstructions.isNotBlank()) {
        Text("Customer Note: ${order.specialInstructions}", style = MaterialTheme.typography.bodySmall, color = MealVerseWarmOrange, modifier = Modifier.padding(top = 4.dp))
      }

      Spacer(modifier = Modifier.height(6.dp))
      if (order.discountAmount > 0.0 && order.voucherCode.isNotBlank()) {
        Text(
          text = "Voucher Applied: ${order.voucherCode} (-Rs. ${order.discountAmount.toInt()})",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color(0xFF2E7D32)
        )
        Spacer(modifier = Modifier.height(4.dp))
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          "Total: Rs. ${order.totalAmount.toInt()} (${com.example.ui.components.formatOrderPaymentLabel(order)})",
          fontWeight = FontWeight.Bold,
          color = MealVerseDeepTeal
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Status Transition Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        when (order.status) {
          OrderStatus.NEW.name -> {
            Button(
              onClick = { onUpdateStatus(OrderStatus.ACCEPTED) },
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
              modifier = Modifier.weight(1f)
            ) {
              Text("Accept Order")
            }
            OutlinedButton(
              onClick = { onUpdateStatus(OrderStatus.REJECTED) },
              modifier = Modifier.weight(1f)
            ) {
              Text("Reject", color = MaterialTheme.colorScheme.error)
            }
          }
          OrderStatus.ACCEPTED.name -> {
            Button(
              onClick = { onUpdateStatus(OrderStatus.PREPARING) },
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Start Preparing in Kitchen")
            }
          }
          OrderStatus.PREPARING.name -> {
            Button(
              onClick = { onUpdateStatus(OrderStatus.READY) },
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseWarmOrange),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Mark Ready for Pickup")
            }
          }
          OrderStatus.READY.name -> {
            Button(
              onClick = { onUpdateStatus(OrderStatus.ON_THE_WAY) },
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Handed to Rider (On the Way)")
            }
          }
          OrderStatus.ON_THE_WAY.name -> {
            Button(
              onClick = { onUpdateStatus(OrderStatus.DELIVERED) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Mark Delivered")
            }
          }
          else -> {
            Text("Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}
