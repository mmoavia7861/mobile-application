package com.example.ui.customer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.*
import com.example.data.payment.VoucherCalculator
import com.example.data.repository.MealVerseRepository
import com.example.notifications.OrderNotificationHelper
import com.example.ui.components.Animated3DFoodThumbnail
import com.example.ui.components.CompactRealTimeOrderBanner
import com.example.ui.components.Interactive3DFoodStage
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.components.VoucherTicketCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
  repository: MealVerseRepository,
  currentProfile: UserProfile,
  onSelectStore: (Store) -> Unit,
  onOpenAiAssistant: () -> Unit,
  onViewOrders: () -> Unit,
  onViewCart: () -> Unit,
  onViewProfile: () -> Unit = {},
  onApplyVoucher: (String) -> Unit = {},
  selectedVoucherCode: String? = null,
  cartItemCount: Int,
  onSwitchRole: () -> Unit,
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {}
) {
  var selectedCity by remember { mutableStateOf(currentProfile.city.ifBlank { "Islamabad" }) }
  var cityDropdownExpanded by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var selectedBudgetMax by remember { mutableStateOf<Double?>(null) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var hasNotificationPermission by remember {
    mutableStateOf(OrderNotificationHelper.hasNotificationPermission(context))
  }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasNotificationPermission = granted
    if (granted) {
      scope.launch {
        repository.setNotificationsEnabled(true)
        repository.syncFcmToken()
      }
    }
  }

  val stores by remember(selectedCity) {
    repository.observeApprovedStores(selectedCity)
  }.collectAsStateWithLifecycle(initialValue = emptyList())
  val customerOrders by remember(currentProfile.userId) {
    repository.observeCustomerOrders(currentProfile.userId)
  }.collectAsStateWithLifecycle(initialValue = emptyList())
  val activeOrder = customerOrders.firstOrNull {
    !OrderDisplayUtils.isCompletedOrPastStatus(it.status)
  }
  val firestoreVouchers by remember(repository) {
    repository.observeActiveVouchers()
  }.collectAsStateWithLifecycle(initialValue = emptyList())
  val displayVouchers = remember(firestoreVouchers) {
    firestoreVouchers.ifEmpty { VoucherCalculator.DEFAULT_VOUCHERS }
  }

  // Initial database seeding if empty
  LaunchedEffect(selectedCity) {
    repository.seedInitialDataIfNeeded(selectedCity)
  }

  val filteredStores = stores.filter { store ->
    val matchesSearch = searchQuery.isBlank() ||
      store.name.contains(searchQuery, ignoreCase = true) ||
      store.category.contains(searchQuery, ignoreCase = true) ||
      store.description.contains(searchQuery, ignoreCase = true)

    val matchesCategory = selectedCategory == "All" ||
      store.category.equals(selectedCategory, ignoreCase = true) ||
      store.category.contains(selectedCategory, ignoreCase = true)
    val matchesBudget = selectedBudgetMax == null || (store.minOrder + store.deliveryFee) <= selectedBudgetMax!!

    matchesSearch && matchesCategory && matchesBudget
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { cityDropdownExpanded = true }
                .padding(vertical = 2.dp)
            ) {
              Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MealVerseWarmOrange,
                modifier = Modifier.size(19.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = selectedCity,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
              )
              Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Select City",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
            Text(
              text = currentProfile.address.ifBlank { "Delivering across $selectedCity" },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            DropdownMenu(
              expanded = cityDropdownExpanded,
              onDismissRequest = { cityDropdownExpanded = false }
            ) {
              val allCities = listOf("All") + PAKISTAN_CITIES
              allCities.forEach { city ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = if (city == "All") "All Cities (Pakistan)" else city,
                      fontWeight = if (city == selectedCity) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  onClick = {
                    selectedCity = city
                    cityDropdownExpanded = false
                  }
                )
              }
            }
          }
        },
        actions = {
          ThemeQuickToggleButton(
            isDarkTheme = isDarkTheme,
            onToggle = onToggleTheme,
            modifier = Modifier.testTag("home_theme_toggle_btn")
          )
          FilledTonalIconButton(
            onClick = onOpenAiAssistant,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("ai_food_assistant_fab")
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Food Assistant")
          }
          IconButton(
            onClick = onViewProfile,
            modifier = Modifier.testTag("header_profile_btn")
          ) {
            Icon(
              Icons.Default.AccountCircle,
              contentDescription = "My Profile & Recent Orders",
              tint = MaterialTheme.colorScheme.primary
            )
          }
          IconButton(onClick = onSwitchRole) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Mode")
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
      ) {
        NavigationBarItem(
          selected = true,
          onClick = {},
          icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
          label = { Text("Explore", fontWeight = FontWeight.SemiBold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MealVerseDeepTeal,
            selectedTextColor = MealVerseDeepTeal,
            indicatorColor = MealVerseDeepTeal.copy(alpha = 0.12f)
          )
        )
        NavigationBarItem(
          selected = false,
          onClick = onViewOrders,
          icon = {
            BadgedBox(badge = {
              if (activeOrder != null) {
                Badge(containerColor = MealVerseWarmOrange)
              }
            }) {
              Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "My Orders")
            }
          },
          label = { Text("Orders") }
        )
        NavigationBarItem(
          selected = false,
          onClick = onViewCart,
          icon = {
            BadgedBox(badge = {
              if (cartItemCount > 0) {
                Badge(containerColor = MealVerseWarmOrange) { Text("$cartItemCount") }
              }
            }) {
              Icon(Icons.Default.ShoppingBag, contentDescription = "Cart")
            }
          },
          label = { Text("Cart") }
        )
        NavigationBarItem(
          selected = false,
          onClick = onOpenAiAssistant,
          icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Chef") },
          label = { Text("AI Chef", maxLines = 1) }
        )
        NavigationBarItem(
          selected = false,
          onClick = onViewProfile,
          icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
          label = { Text("Profile", maxLines = 1) },
          modifier = Modifier.testTag("nav_profile_tab")
        )
      }
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
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        // Real-Time Active Order Tracking Status Banner if present
        if (activeOrder != null) {
          item {
            CompactRealTimeOrderBanner(
              order = activeOrder,
              onClick = onViewOrders,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
          }
          if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MealVerseWarmOrange.copy(alpha = 0.12f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 2.dp)
                  .testTag("enable_push_alerts_banner")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MealVerseWarmOrange,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Enable push alerts for instant order status updates",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                  )
                  TextButton(
                    onClick = {
                      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                  ) {
                    Text("Allow", fontWeight = FontWeight.Bold, color = MealVerseWarmOrange)
                  }
                }
              }
            }
          }
        }

        // Search Bar
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Biryani, Karahi, Savour, Zinger...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MealVerseDeepTeal) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Clear")
                }
              }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedBorderColor = MealVerseDeepTeal,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp)
              .testTag("food_search_input"),
            singleLine = true
          )
        }

        // Interactive 3D Animated Food Stage Hero
        item {
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Interactive3DFoodStage(
              compact = false,
              onDishSelected = { dish ->
                selectedCategory = dish.categoryFilter
              }
            )
          }
        }

        // Food Categories Horizontal Scroll with 3D Animated Thumbnails
        item {
          Column(modifier = Modifier.padding(top = 16.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Popular Pakistani Cuisines",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
              )
              if (selectedCategory != "All") {
                Text(
                  text = "Show All",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MealVerseWarmOrange
                  ),
                  modifier = Modifier
                    .clickable { selectedCategory = "All" }
                    .padding(4.dp)
                )
              }
            }

            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              itemsIndexed(POPULAR_CUISINES) { index, cuisine ->
                val isSelected = selectedCategory == cuisine.name
                Card(
                  shape = RoundedCornerShape(18.dp),
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MealVerseDeepTeal else MaterialTheme.colorScheme.surface
                  ),
                  elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
                  modifier = Modifier
                    .width(108.dp)
                    .border(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color = if (isSelected) MealVerseSaffronYellow else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                      shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { selectedCategory = cuisine.name }
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Animated3DFoodThumbnail(
                      imageRes = cuisine.imageRes,
                      contentDescription = cuisine.name,
                      size = 58.dp,
                      cornerRadius = 16.dp,
                      phaseOffset = index * 0.7f
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = cuisine.name,
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                      ),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = cuisine.subtitle,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                      ),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }
        }

        // Budget Mode Filter Chips
        item {
          Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
              text = "Budget Friendly (PKR)",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val budgets = listOf(
                null to "All Prices",
                350.0 to "Under Rs. 350",
                500.0 to "Under Rs. 500",
                800.0 to "Under Rs. 800",
                1200.0 to "Under Rs. 1200"
              )
              items(budgets) { (amount, label) ->
                FilterChip(
                  selected = selectedBudgetMax == amount,
                  onClick = { selectedBudgetMax = amount },
                  label = { Text(label) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MealVerseWarmOrange,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }
        }

        // Vouchers & Card Deals Strip
        item {
          Column(modifier = Modifier.padding(top = 14.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.ConfirmationNumber,
                  contentDescription = null,
                  tint = MealVerseWarmOrange,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Vouchers & Card Deals",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
              }
              if (!selectedVoucherCode.isNullOrBlank()) {
                Text(
                  text = "$selectedVoucherCode ready for checkout",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                  )
                )
              }
            }
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.testTag("home_vouchers_row")
            ) {
              items(displayVouchers, key = { it.code }) { voucher ->
                val isSelected = selectedVoucherCode.equals(voucher.code, ignoreCase = true)
                VoucherTicketCard(
                  voucher = voucher,
                  isApplied = isSelected,
                  actionLabel = if (isSelected) "Ready" else if (cartItemCount > 0) "Use Now" else "Save",
                  onClick = { onApplyVoucher(voucher.code) }
                )
              }
            }
          }
        }

        // Restaurant List Header
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (selectedCity == "All") {
                "All Restaurants (${filteredStores.size})"
              } else {
                "Restaurants in $selectedCity (${filteredStores.size})"
              },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
          }
        }

        if (filteredStores.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            ) {
              Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  Icons.Default.SearchOff,
                  contentDescription = null,
                  tint = MealVerseWarmOrange,
                  modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "No restaurants found matching filters",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Try switching to 'All Cities' or resetting your category and budget filters.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = {
                      searchQuery = ""
                      selectedCategory = "All"
                      selectedBudgetMax = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal)
                  ) {
                    Text("Reset Filters")
                  }
                  if (selectedCity != "All") {
                    OutlinedButton(onClick = { selectedCity = "All" }) {
                      Text("View All Cities")
                    }
                  }
                }
              }
            }
          }
        } else {
          items(filteredStores, key = { it.id }) { store ->
            StoreCard(
              store = store,
              onClick = { onSelectStore(store) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun StoreCard(
  store: Store,
  onClick: () -> Unit
) {
  val imageRes = getStoreGraphic(store)
  val dish3DRes = get3DFoodGraphic(store.name, store.category)

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .clickable(onClick = onClick)
      .testTag("store_card_${store.id}")
  ) {
    Column {
      // Store banner with photographic graphics + floating 3D dish badge
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(152.dp)
      ) {
        Image(
          painter = painterResource(id = imageRes),
          contentDescription = store.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Gradient Scrim
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Black.copy(alpha = 0.12f),
                  Color.Black.copy(alpha = 0.86f)
                )
              )
            )
        )

        // Top-left Open badge
        Surface(
          shape = RoundedCornerShape(50),
          color = Color(0xFF1B5E20).copy(alpha = 0.88f),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(0xFF69F0AE))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "OPEN",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Top-right Rating Badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.Black.copy(alpha = 0.68f),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MealVerseSaffronYellow, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${store.rating}",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }

        // Bottom Title & Floating 3D Food Medallion
        Row(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = store.name,
                style = MaterialTheme.typography.titleLarge.copy(
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                Icons.Default.Verified,
                contentDescription = "Verified Merchant",
                tint = MealVerseSaffronYellow,
                modifier = Modifier.size(19.dp)
              )
            }
            Text(
              text = "${store.category} • ${store.city}",
              style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Animated3DFoodThumbnail(
            imageRes = dish3DRes,
            contentDescription = "${store.name} dish preview",
            size = 54.dp,
            cornerRadius = 14.dp
          )
        }
      }

      // Store Details Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MealVerseWarmOrange.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Timer, contentDescription = null, tint = MealVerseWarmOrange, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${store.prepTimeMinutes} mins",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MealVerseWarmOrange)
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Surface(
            color = MealVerseDeepTeal.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = MealVerseDeepTeal, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Rs. ${store.deliveryFee.toInt()}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MealVerseDeepTeal)
              )
            }
          }
        }

        Text(
          text = "Min: Rs. ${store.minOrder.toInt()}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
