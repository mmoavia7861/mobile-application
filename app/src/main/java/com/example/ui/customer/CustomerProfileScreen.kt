package com.example.ui.customer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.payment.VoucherCalculator
import com.example.data.repository.MealVerseRepository
import com.example.notifications.OrderNotificationHelper
import com.example.ui.components.Animated3DFoodThumbnail
import com.example.ui.components.RateOrderPromptButton
import com.example.ui.components.ReviewSubmissionDialog
import com.example.ui.components.SubmittedReviewSummaryCard
import com.example.ui.components.ThemeModeSelectorCard
import com.example.ui.components.VoucherTicketCard
import com.example.ui.components.formatOrderPaymentLabel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecentOrderFilter(val label: String) {
  ALL("All"),
  DELIVERED("Delivered"),
  ACTIVE("In Progress")
}

enum class RecentOrderSortOption(
  val label: String,
  val isDateSort: Boolean
) {
  DATE_NEWEST("Newest First", true),
  DATE_OLDEST("Oldest First", true),
  MERCHANT_AZ("Merchant A–Z", false),
  MERCHANT_ZA("Merchant Z–A", false)
}

object OrderDisplayUtils {
  fun formatOrderDate(epochSeconds: Long?): String {
    if (epochSeconds == null || epochSeconds <= 0L) return "Recent order"
    val formatter = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.US)
    return formatter.format(Date(epochSeconds * 1000L))
  }

  fun summarizeOrderItems(items: List<OrderItemRecord>): String {
    if (items.isEmpty()) return "Order items unavailable"
    val preview = items.take(2).joinToString(", ") { "${it.quantity}x ${it.productName}" }
    val remaining = items.size - 2
    return if (remaining > 0) "$preview +$remaining more" else preview
  }

  fun formatStatusLabel(status: String): String {
    return status.replace('_', ' ')
      .lowercase(Locale.US)
      .split(' ')
      .joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
      }
  }

  fun isCompletedOrPastStatus(status: String): Boolean {
    return status == OrderStatus.DELIVERED.name ||
      status == OrderStatus.CANCELLED.name ||
      status == OrderStatus.REJECTED.name
  }

  fun orderEpochMillis(order: Order): Long {
    val timestamp = order.createdAt ?: order.updatedAt ?: return 0L
    return (timestamp.seconds * 1000L) + (timestamp.nanoseconds / 1_000_000L)
  }

  fun extractDistinctMerchants(orders: List<Order>): List<String> {
    val seenLower = mutableSetOf<String>()
    val result = mutableListOf<String>()
    orders.forEach { order ->
      val trimmed = order.storeName.trim()
      if (trimmed.isNotEmpty()) {
        val key = trimmed.lowercase(Locale.US)
        if (seenLower.add(key)) {
          result.add(trimmed)
        }
      }
    }
    return result.sortedBy { it.lowercase(Locale.US) }
  }

  fun filterAndSortOrders(
    orders: List<Order>,
    statusFilter: RecentOrderFilter = RecentOrderFilter.ALL,
    merchantFilter: String? = null,
    sortOption: RecentOrderSortOption = RecentOrderSortOption.DATE_NEWEST
  ): List<Order> {
    val statusFiltered = when (statusFilter) {
      RecentOrderFilter.ALL -> orders
      RecentOrderFilter.DELIVERED -> orders.filter { it.status == OrderStatus.DELIVERED.name }
      RecentOrderFilter.ACTIVE -> orders.filter { !isCompletedOrPastStatus(it.status) }
    }

    val normalizedMerchant = merchantFilter?.trim()?.takeIf { it.isNotEmpty() }
    val fullyFiltered = if (normalizedMerchant == null) {
      statusFiltered
    } else {
      statusFiltered.filter {
        it.storeName.trim().equals(normalizedMerchant, ignoreCase = true)
      }
    }

    val comparator: Comparator<Order> = when (sortOption) {
      RecentOrderSortOption.DATE_NEWEST ->
        compareByDescending<Order> { orderEpochMillis(it) }
          .thenBy { it.storeName.trim().lowercase(Locale.US) }
          .thenBy { it.id }

      RecentOrderSortOption.DATE_OLDEST ->
        compareBy<Order> { orderEpochMillis(it) }
          .thenBy { it.storeName.trim().lowercase(Locale.US) }
          .thenBy { it.id }

      RecentOrderSortOption.MERCHANT_AZ ->
        compareBy<Order> {
          it.storeName.trim().ifBlank { "MealVerse Restaurant" }.lowercase(Locale.US)
        }
          .thenByDescending { orderEpochMillis(it) }
          .thenBy { it.id }

      RecentOrderSortOption.MERCHANT_ZA ->
        compareByDescending<Order> {
          it.storeName.trim().ifBlank { "MealVerse Restaurant" }.lowercase(Locale.US)
        }
          .thenByDescending { orderEpochMillis(it) }
          .thenBy { it.id }
    }

    return fullyFiltered.sortedWith(comparator)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
  currentProfile: UserProfile,
  currentUserId: String,
  repository: MealVerseRepository,
  onBack: () -> Unit,
  onViewOrderTracking: (String?) -> Unit,
  onExploreRestaurants: () -> Unit,
  onApplyVoucher: (String) -> Unit = {},
  selectedVoucherCode: String? = null,
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  isDarkTheme: Boolean = false,
  onSelectThemeMode: (ThemeMode) -> Unit = {},
  onSwitchRole: () -> Unit,
  onSignOut: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val effectiveUserId = currentUserId.ifBlank { currentProfile.userId }
  var retryTrigger by remember { mutableIntStateOf(0) }
  val ordersState by remember(effectiveUserId, retryTrigger) {
    repository.observeCustomerOrdersState(effectiveUserId)
  }.collectAsStateWithLifecycle(initialValue = RecentOrdersUiState.Loading)
  val customerReviews by remember(effectiveUserId) {
    repository.observeCustomerReviews(effectiveUserId)
  }.collectAsStateWithLifecycle(initialValue = emptyMap())
  val firestoreVouchers by remember(repository) {
    repository.observeActiveVouchers()
  }.collectAsStateWithLifecycle(initialValue = emptyList())
  val displayVouchers = remember(firestoreVouchers) {
    firestoreVouchers.ifEmpty { VoucherCalculator.DEFAULT_VOUCHERS }
  }

  var selectedFilter by remember { mutableStateOf(RecentOrderFilter.ALL) }
  var selectedSort by remember { mutableStateOf(RecentOrderSortOption.DATE_NEWEST) }
  var selectedMerchant by remember { mutableStateOf<String?>(null) }
  var showMerchantDropdown by remember { mutableStateOf(false) }
  var orderToReview by remember { mutableStateOf<Order?>(null) }
  var isSubmittingReview by remember { mutableStateOf(false) }
  var reviewErrorMessage by remember { mutableStateOf<String?>(null) }

  val allOrders = (ordersState as? RecentOrdersUiState.Success)?.orders ?: emptyList()
  val availableMerchants = remember(allOrders) {
    OrderDisplayUtils.extractDistinctMerchants(allOrders)
  }
  LaunchedEffect(availableMerchants) {
    if (selectedMerchant != null && availableMerchants.none { it.equals(selectedMerchant, ignoreCase = true) }) {
      selectedMerchant = null
    }
  }

  val deliveredOrders = allOrders.filter { it.status == OrderStatus.DELIVERED.name }
  val totalSpent = allOrders
    .filter { it.status != OrderStatus.CANCELLED.name && it.status != OrderStatus.REJECTED.name }
    .sumOf { it.totalAmount }

  val filteredOrders = remember(allOrders, selectedFilter, selectedMerchant, selectedSort) {
    OrderDisplayUtils.filterAndSortOrders(
      orders = allOrders,
      statusFilter = selectedFilter,
      merchantFilter = selectedMerchant,
      sortOption = selectedSort
    )
  }
  val hasCustomFilterOrSort =
    selectedFilter != RecentOrderFilter.ALL ||
      selectedMerchant != null ||
      selectedSort != RecentOrderSortOption.DATE_NEWEST

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "My Profile",
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("customer_profile_back_btn")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = onSwitchRole) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Mode")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 720.dp)
          .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. Customer Profile Header Card
        item {
          CustomerProfileHeaderCard(
            profile = currentProfile,
            totalOrders = allOrders.size,
            deliveredCount = deliveredOrders.size,
            totalSpent = totalSpent
          )
        }

        // 2. Delivery Info Card
        item {
          DeliveryDetailsCard(profile = currentProfile)
        }

        // 2b. FCM Push Notification Settings Card
        item {
          PushNotificationsStatusCard(
            initialEnabled = currentProfile.notificationsEnabled,
            repository = repository
          )
        }

        // 2b-ii. App Appearance (Light / Dark / System Mode)
        item {
          ThemeModeSelectorCard(
            themeMode = themeMode,
            isDarkActive = isDarkTheme,
            onSelectMode = onSelectThemeMode
          )
        }

        // 2c. Available Vouchers & Promo Codes
        item {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.ConfirmationNumber,
                  contentDescription = null,
                  tint = MealVerseWarmOrange,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "My Vouchers & Deals",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
              }
              Text(
                text = "${displayVouchers.size} active",
                style = MaterialTheme.typography.labelMedium,
                color = MealVerseDeepTeal,
                fontWeight = FontWeight.Bold
              )
            }
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.testTag("profile_vouchers_row")
            ) {
              items(displayVouchers, key = { it.code }) { voucher ->
                val isSelected = selectedVoucherCode.equals(voucher.code, ignoreCase = true)
                VoucherTicketCard(
                  voucher = voucher,
                  isApplied = isSelected,
                  actionLabel = if (isSelected) "Selected" else "Use Deal",
                  onClick = { onApplyVoucher(voucher.code) }
                )
              }
            }
          }
        }

        // 3. 'Recent Orders' Section Header + Filter Chips
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp)
              .testTag("recent_orders_section")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                  contentDescription = null,
                  tint = MealVerseWarmOrange,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Recent Orders",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                  modifier = Modifier.testTag("recent_orders_title")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  shape = CircleShape,
                  color = MealVerseDeepTeal.copy(alpha = 0.12f)
                ) {
                  Text(
                    text = "${allOrders.size}",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = MealVerseDeepTeal
                    ),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                  )
                }
              }

              TextButton(
                onClick = { onViewOrderTracking(null) },
                modifier = Modifier
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("view_all_orders_btn")
              ) {
                Text(
                  text = "Live Tracking",
                  fontWeight = FontWeight.Bold,
                  color = MealVerseDeepTeal
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  Icons.Default.ChevronRight,
                  contentDescription = null,
                  tint = MealVerseDeepTeal,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            if (allOrders.isNotEmpty()) {
              Spacer(modifier = Modifier.height(6.dp))
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(RecentOrderFilter.entries) { filter ->
                  val isSelected = selectedFilter == filter
                  FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MealVerseDeepTeal,
                      selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("recent_orders_status_filter_${filter.name.lowercase(Locale.US)}")
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                  .fillMaxWidth()
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp)
                  )
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = MealVerseDeepTeal,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Sort & Filter Past Orders",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                    if (hasCustomFilterOrSort) {
                      TextButton(
                        onClick = {
                          selectedFilter = RecentOrderFilter.ALL
                          selectedMerchant = null
                          selectedSort = RecentOrderSortOption.DATE_NEWEST
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                          .defaultMinSize(minHeight = 48.dp)
                          .testTag("recent_orders_reset_filters_btn")
                      ) {
                        Text(
                          text = "Reset",
                          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                          color = MealVerseWarmOrange
                        )
                      }
                    }
                  }

                  LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    item {
                      val isDateActive = selectedSort.isDateSort
                      val dateLabel = when (selectedSort) {
                        RecentOrderSortOption.DATE_NEWEST -> "Date: Newest"
                        RecentOrderSortOption.DATE_OLDEST -> "Date: Oldest"
                        else -> "Sort: Date"
                      }
                      FilterChip(
                        selected = isDateActive,
                        onClick = {
                          selectedSort = if (selectedSort == RecentOrderSortOption.DATE_NEWEST) {
                            RecentOrderSortOption.DATE_OLDEST
                          } else {
                            RecentOrderSortOption.DATE_NEWEST
                          }
                        },
                        leadingIcon = {
                          Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                          )
                        },
                        trailingIcon = if (isDateActive) {
                          {
                            Icon(
                              imageVector = if (selectedSort == RecentOrderSortOption.DATE_NEWEST) {
                                Icons.Default.ArrowDownward
                              } else {
                                Icons.Default.ArrowUpward
                              },
                              contentDescription = if (selectedSort == RecentOrderSortOption.DATE_NEWEST) {
                                "Sorted newest first, tap to reverse"
                              } else {
                                "Sorted oldest first, tap to reverse"
                              },
                              modifier = Modifier.size(15.dp)
                            )
                          }
                        } else null,
                        label = { Text(dateLabel, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MealVerseDeepTeal,
                          selectedLabelColor = Color.White,
                          selectedLeadingIconColor = Color.White,
                          selectedTrailingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("recent_orders_sort_date_chip")
                      )
                    }

                    item {
                      val isMerchantActive = !selectedSort.isDateSort
                      val merchantSortLabel = when (selectedSort) {
                        RecentOrderSortOption.MERCHANT_AZ -> "Merchant: A–Z"
                        RecentOrderSortOption.MERCHANT_ZA -> "Merchant: Z–A"
                        else -> "Sort: Merchant"
                      }
                      FilterChip(
                        selected = isMerchantActive,
                        onClick = {
                          selectedSort = if (selectedSort == RecentOrderSortOption.MERCHANT_AZ) {
                            RecentOrderSortOption.MERCHANT_ZA
                          } else {
                            RecentOrderSortOption.MERCHANT_AZ
                          }
                        },
                        leadingIcon = {
                          Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                          )
                        },
                        trailingIcon = if (isMerchantActive) {
                          {
                            Icon(
                              imageVector = if (selectedSort == RecentOrderSortOption.MERCHANT_AZ) {
                                Icons.Default.ArrowUpward
                              } else {
                                Icons.Default.ArrowDownward
                              },
                              contentDescription = if (selectedSort == RecentOrderSortOption.MERCHANT_AZ) {
                                "Sorted A to Z, tap to reverse"
                              } else {
                                "Sorted Z to A, tap to reverse"
                              },
                              modifier = Modifier.size(15.dp)
                            )
                          }
                        } else null,
                        label = { Text(merchantSortLabel, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MealVerseWarmOrange,
                          selectedLabelColor = Color.White,
                          selectedLeadingIconColor = Color.White,
                          selectedTrailingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("recent_orders_sort_merchant_chip")
                      )
                    }

                    if (availableMerchants.isNotEmpty()) {
                      item {
                        Box {
                          FilterChip(
                            selected = selectedMerchant != null,
                            onClick = { showMerchantDropdown = true },
                            leadingIcon = {
                              Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                              )
                            },
                            trailingIcon = {
                              Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choose merchant filter",
                                modifier = Modifier.size(18.dp)
                              )
                            },
                            label = {
                              Text(
                                text = selectedMerchant ?: "All Merchants",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                              )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                              selectedContainerColor = MealVerseDeepTeal.copy(alpha = 0.16f),
                              selectedLabelColor = MealVerseDeepTeal,
                              selectedLeadingIconColor = MealVerseDeepTeal,
                              selectedTrailingIconColor = MealVerseDeepTeal
                            ),
                            modifier = Modifier.testTag("recent_orders_merchant_filter_chip")
                          )

                          DropdownMenu(
                            expanded = showMerchantDropdown,
                            onDismissRequest = { showMerchantDropdown = false },
                            modifier = Modifier.testTag("recent_orders_merchant_dropdown")
                          ) {
                            DropdownMenuItem(
                              text = {
                                Text(
                                  text = "All Merchants (${allOrders.size})",
                                  fontWeight = if (selectedMerchant == null) FontWeight.Bold else FontWeight.Normal
                                )
                              },
                              leadingIcon = {
                                Icon(
                                  imageVector = if (selectedMerchant == null) Icons.Default.Check else Icons.Default.Storefront,
                                  contentDescription = null,
                                  tint = if (selectedMerchant == null) MealVerseDeepTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                  modifier = Modifier.size(18.dp)
                                )
                              },
                              onClick = {
                                selectedMerchant = null
                                showMerchantDropdown = false
                              }
                            )
                            HorizontalDivider()
                            availableMerchants.forEach { merchantName ->
                              val isChosen = selectedMerchant.equals(merchantName, ignoreCase = true)
                              DropdownMenuItem(
                                text = {
                                  Text(
                                    text = merchantName,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                  )
                                },
                                leadingIcon = {
                                  Icon(
                                    imageVector = if (isChosen) Icons.Default.Check else Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = if (isChosen) MealVerseDeepTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                  )
                                },
                                onClick = {
                                  selectedMerchant = merchantName
                                  showMerchantDropdown = false
                                }
                              )
                            }
                          }
                        }
                      }
                    }
                  }

                  Text(
                    text = buildString {
                      append("Showing ${filteredOrders.size} of ${allOrders.size} orders • ${selectedSort.label}")
                      if (selectedMerchant != null) {
                        append(" • $selectedMerchant")
                      }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("recent_orders_filter_summary_bar")
                  )
                }
              }
            }
          }
        }

        // 4. Recent Orders States (Loading / Error / Empty / List)
        when (val state = ordersState) {
          is RecentOrdersUiState.Loading -> {
            item {
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("recent_orders_loading")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  CircularProgressIndicator(
                    color = MealVerseDeepTeal,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Fetching your recent orders from Firestore...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          is RecentOrdersUiState.Error -> {
            item {
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("recent_orders_error")
              ) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(32.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Button(
                    onClick = { retryTrigger++ },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                  ) {
                    Text("Retry")
                  }
                }
              }
            }
          }

          is RecentOrdersUiState.Success -> {
            if (filteredOrders.isEmpty()) {
              item {
                RecentOrdersEmptyCard(
                  hasAnyOrders = allOrders.isNotEmpty(),
                  onExploreRestaurants = onExploreRestaurants,
                  onResetFilter = {
                    selectedFilter = RecentOrderFilter.ALL
                    selectedMerchant = null
                    selectedSort = RecentOrderSortOption.DATE_NEWEST
                  }
                )
              }
            } else {
              items(filteredOrders, key = { it.id.ifBlank { "${it.storeId}_${it.createdAt?.seconds}" } }) { order ->
                RecentOrderItemCard(
                  order = order,
                  submittedReview = customerReviews[order.id],
                  onRateOrder = {
                    reviewErrorMessage = null
                    orderToReview = order
                  },
                  onClick = { onViewOrderTracking(order.id) }
                )
              }
            }
          }
        }

        // 5. Account Actions Footer
        item {
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("profile_sign_out_btn")
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(16.dp))
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
            customerName = currentProfile.fullName,
            rating = rating,
            comment = comment
          )
          isSubmittingReview = false
          result.onSuccess {
            orderToReview = null
            snackbarHostState.showSnackbar("Thank you! Your review has been saved.")
          }.onFailure { err ->
            reviewErrorMessage = err.localizedMessage ?: "Failed to save review."
          }
        }
      }
    )
  }
}

@Composable
private fun CustomerProfileHeaderCard(
  profile: UserProfile,
  totalOrders: Int,
  deliveredCount: Int,
  totalSpent: Double
) {
  val displayName = profile.fullName.ifBlank { "Valued Customer" }
  val initials = displayName
    .split(' ')
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }
    .ifBlank { "MV" }

  Card(
    shape = RoundedCornerShape(22.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("customer_profile_header_card")
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.linearGradient(
            colors = listOf(MealVerseDeepTeal, Color(0xFF003B46))
          )
        )
        .padding(18.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(MealVerseWarmOrange)
            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = initials,
            style = MaterialTheme.typography.titleLarge.copy(
              color = Color.White,
              fontWeight = FontWeight.ExtraBold
            )
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge.copy(
              color = Color.White,
              fontWeight = FontWeight.ExtraBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (profile.email.isNotBlank()) {
            Text(
              text = profile.email,
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.86f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.16f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.LocationCity,
                contentDescription = null,
                tint = MealVerseSaffronYellow,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "${profile.city.ifBlank { "Islamabad" }} • Food Lover",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(Color.White.copy(alpha = 0.12f))
          .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        ProfileStatItem(value = "$totalOrders", label = "Orders")
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.22f))
        )
        ProfileStatItem(value = "$deliveredCount", label = "Delivered")
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.22f))
        )
        ProfileStatItem(value = "Rs. ${totalSpent.toInt()}", label = "Total Spent")
      }
    }
  }
}

@Composable
private fun ProfileStatItem(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(
        color = Color.White,
        fontWeight = FontWeight.ExtraBold
      )
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.8f)
    )
  }
}

@Composable
private fun DeliveryDetailsCard(profile: UserProfile) {
  val formattedAddress = buildList {
    if (profile.houseFlat.isNotBlank()) add(profile.houseFlat)
    if (profile.address.isNotBlank()) add(profile.address)
    if (profile.landmark.isNotBlank()) add("Near ${profile.landmark}")
  }.joinToString(", ").ifBlank { "No saved delivery address yet" }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Saved Delivery Details",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
      )
      Row(verticalAlignment = Alignment.Top) {
        Icon(
          Icons.Default.Home,
          contentDescription = null,
          tint = MealVerseDeepTeal,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = formattedAddress,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      if (profile.phoneNumber.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Phone,
            contentDescription = null,
            tint = MealVerseWarmOrange,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = profile.phoneNumber,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
fun RecentOrderItemCard(
  order: Order,
  submittedReview: Review? = null,
  onRateOrder: () -> Unit = {},
  onClick: () -> Unit
) {
  val isDark = isMealVerseDarkTheme()
  val isDelivered = order.status == OrderStatus.DELIVERED.name
  val isCancelled = order.status == OrderStatus.CANCELLED.name || order.status == OrderStatus.REJECTED.name

  val statusBg = when {
    isDelivered -> if (isDark) Color(0xFF173620) else Color(0xFFE8F5E9)
    isCancelled -> if (isDark) Color(0xFF3E1818) else Color(0xFFFFEBEE)
    else -> MealVerseWarmOrange.copy(alpha = 0.16f)
  }
  val statusFg = when {
    isDelivered -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    isCancelled -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
    else -> MealVerseWarmOrange
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("recent_order_card_${order.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Animated3DFoodThumbnail(
          imageRes = get3DFoodGraphic(order.storeName, order.items.firstOrNull()?.productName ?: ""),
          contentDescription = order.storeName,
          size = 52.dp,
          cornerRadius = 14.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = order.storeName.ifBlank { "MealVerse Restaurant" },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = OrderDisplayUtils.formatOrderDate(order.createdAt?.seconds),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
          if (!isDelivered && !isCancelled) {
            com.example.ui.components.LiveSyncPulseBadge(
              isLive = true,
              isTerminalError = false
            )
          }
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusBg
          ) {
            Text(
              text = OrderDisplayUtils.formatStatusLabel(order.status),
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = statusFg
              )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = OrderDisplayUtils.summarizeOrderItems(order.items),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      if (order.deliveryAddress.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = order.deliveryAddress,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MealVerseDeepTeal.copy(alpha = 0.1f)
          ) {
            Text(
              text = formatOrderPaymentLabel(order),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MealVerseDeepTeal
              ),
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }
          if (order.discountAmount > 0.0 && order.voucherCode.isNotBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFE8F5E9)
            ) {
              Text(
                text = "${order.voucherCode} -Rs.${order.discountAmount.toInt()}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF2E7D32)
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${order.items.sumOf { it.quantity }} items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Rs. ${order.totalAmount.toInt()}",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              color = MealVerseDeepTeal
            )
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            Icons.Default.ChevronRight,
            contentDescription = "View order details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

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

@Composable
private fun RecentOrdersEmptyCard(
  hasAnyOrders: Boolean,
  onExploreRestaurants: () -> Unit,
  onResetFilter: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("recent_orders_empty_state")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(MealVerseWarmOrange.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
          contentDescription = null,
          tint = MealVerseWarmOrange,
          modifier = Modifier.size(32.dp)
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = if (hasAnyOrders) "No orders in this filter" else "No recent orders yet",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = if (hasAnyOrders) {
          "Reset your status or merchant filters to view your complete order history."
        } else {
          "Once you place an order from a local restaurant, your order history will appear here."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = if (hasAnyOrders) onResetFilter else onExploreRestaurants,
        colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) {
        Text(
          text = if (hasAnyOrders) "Reset Filters" else "Explore Restaurants",
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun PushNotificationsStatusCard(
  initialEnabled: Boolean,
  repository: MealVerseRepository
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var hasPermission by remember {
    mutableStateOf(OrderNotificationHelper.hasNotificationPermission(context))
  }
  var notificationsEnabled by remember(initialEnabled) { mutableStateOf(initialEnabled) }
  var fcmSynced by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
    if (granted) {
      notificationsEnabled = true
      scope.launch {
        repository.setNotificationsEnabled(true)
        fcmSynced = repository.syncFcmToken() != null
      }
    }
  }

  val isActive = notificationsEnabled && hasPermission

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("profile_notifications_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(
            if (isActive) Color(0xFF2E7D32).copy(alpha = 0.14f)
            else MealVerseWarmOrange.copy(alpha = 0.14f)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
          contentDescription = null,
          tint = if (isActive) Color(0xFF2E7D32) else MealVerseWarmOrange,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "FCM Order Push Alerts",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = when {
            isActive && fcmSynced -> "Active • FCM device token synced with Firestore"
            isActive -> "Active • Receive push alerts on order status changes"
            !hasPermission -> "Permission needed on Android 13+ for order push alerts"
            else -> "Paused • Enable to get instant order status updates"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Switch(
        checked = isActive,
        onCheckedChange = { checked ->
          if (checked && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            notificationsEnabled = checked
            scope.launch {
              repository.setNotificationsEnabled(checked)
              if (checked) {
                fcmSynced = repository.syncFcmToken() != null
              }
            }
          }
        },
        modifier = Modifier.testTag("notifications_toggle_switch")
      )
    }
  }
}

