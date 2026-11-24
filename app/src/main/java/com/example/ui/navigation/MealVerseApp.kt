package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.*
import com.example.data.repository.MealVerseRepository
import com.example.data.security.AccessCheckResult
import com.example.data.security.AccessControl
import com.example.notifications.OrderNotificationHelper
import com.example.ui.admin.AdminVerificationScreen
import com.example.ui.auth.CustomerRegistrationScreen
import com.example.ui.auth.ShopkeeperRegistrationScreen
import com.example.ui.components.AccessRestrictedScreen
import com.example.ui.components.ThemeModeSelectorRow
import com.example.ui.components.VerifyingAccessScreen
import com.example.ui.customer.*
import com.example.ui.shopkeeper.MenuManagementScreen
import com.example.ui.shopkeeper.ShopkeeperDashboardScreen
import com.example.ui.theme.*
import com.example.ui.welcome.WelcomeScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

object MealVerseRoutes {
  const val WELCOME = "welcome"
  const val CUSTOMER_REGISTER = "customer_register"
  const val SHOPKEEPER_REGISTER = "shopkeeper_register"
  const val CUSTOMER_HOME = "customer_home"
  const val STORE_DETAIL = "store_detail"
  const val CART_CHECKOUT = "cart_checkout"
  const val ORDER_TRACKING = "order_tracking"
  const val CUSTOMER_PROFILE = "customer_profile"
  const val SHOPKEEPER_DASHBOARD = "shopkeeper_dashboard"
  const val MENU_MANAGEMENT = "menu_management"
  const val ADMIN_PANEL = "admin_panel"
}

@Composable
fun MealVerseApp(
  notificationOrderId: String? = null,
  openTrackingFromNotification: Boolean = false,
  onNotificationHandled: () -> Unit = {},
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  isDarkTheme: Boolean = false,
  onThemeModeChange: (ThemeMode) -> Unit = {}
) {
  val context = LocalContext.current
  val repository = remember { MealVerseRepository(context) }
  val navController = rememberNavController()
  val scope = rememberCoroutineScope()
  val onToggleDarkLight = {
    onThemeModeChange(ThemeModeResolver.toggleDarkLight(isDarkTheme))
  }

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { fbAuth ->
      currentUser = fbAuth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  var userProfile by remember { mutableStateOf<UserProfile?>(null) }
  var isProfileLoading by remember { mutableStateOf(false) }
  var selectedStore by remember { mutableStateOf<Store?>(null) }
  var trackedOrderId by remember { mutableStateOf<String?>(null) }
  var showAccountDialog by remember { mutableStateOf(false) }
  var showAiAssistantDialog by remember { mutableStateOf(false) }

  // Cart State
  var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
  var cartStore by remember { mutableStateOf<Store?>(null) }
  var pendingVoucherCode by remember { mutableStateOf<String?>(null) }

  val isAuthenticated = currentUser != null && !currentUser?.uid.isNullOrBlank()
  val authEmail = currentUser?.email
  val isEmailVerified = currentUser?.isEmailVerified == true

  val isAdminAuthorized = remember(isAuthenticated, authEmail, isEmailVerified, userProfile) {
    AccessControl.isAdminAccessAllowed(
      isAuthenticated = isAuthenticated,
      email = authEmail,
      emailVerified = isEmailVerified,
      profile = userProfile
    )
  }

  val canAccessCustomer = remember(isAuthenticated, authEmail, isEmailVerified, userProfile) {
    AccessControl.canAccessCustomerPanel(
      isAuthenticated = isAuthenticated,
      email = authEmail,
      emailVerified = isEmailVerified,
      profile = userProfile
    )
  }

  val canAccessShopkeeper = remember(isAuthenticated, authEmail, isEmailVerified, userProfile) {
    AccessControl.canAccessShopkeeperPanel(
      isAuthenticated = isAuthenticated,
      email = authEmail,
      emailVerified = isEmailVerified,
      profile = userProfile
    )
  }

  fun performSignOut() {
    Firebase.auth.signOut()
    userProfile = null
    selectedStore = null
    trackedOrderId = null
    cartItems = emptyList()
    cartStore = null
    pendingVoucherCode = null
    showAccountDialog = false
    showAiAssistantDialog = false
    navController.navigate(MealVerseRoutes.WELCOME) {
      popUpTo(0) { inclusive = true }
    }
  }

  fun resolveAuthorizedHomeRoute(profile: UserProfile?, isAdmin: Boolean): String {
    return when {
      isAdmin -> MealVerseRoutes.ADMIN_PANEL
      profile?.role.equals(UserRole.SHOPKEEPER.name, ignoreCase = true) ||
        profile?.role.equals(UserRole.MERCHANT.name, ignoreCase = true) -> MealVerseRoutes.SHOPKEEPER_DASHBOARD
      profile?.role.equals(UserRole.CUSTOMER.name, ignoreCase = true) -> MealVerseRoutes.CUSTOMER_HOME
      else -> MealVerseRoutes.CUSTOMER_REGISTER
    }
  }

  // Load existing profile & sync FCM token when user is authenticated
  LaunchedEffect(currentUser?.uid) {
    val user = currentUser
    val uid = user?.uid
    if (uid.isNullOrBlank()) {
      userProfile = null
      isProfileLoading = false
      return@LaunchedEffect
    }
    isProfileLoading = true
    val prof = repository.getUserProfile(uid)
    userProfile = prof
    isProfileLoading = false

    val adminSession = AccessControl.isAdminAccessAllowed(
      isAuthenticated = true,
      email = user.email,
      emailVerified = user.isEmailVerified,
      profile = prof
    )
    if (adminSession) {
      repository.seedInitialDataIfNeeded(prof?.city ?: "Islamabad")
    }
    if (!openTrackingFromNotification && (prof != null || adminSession)) {
      val destination = resolveAuthorizedHomeRoute(prof, adminSession)
      navController.navigate(destination) {
        popUpTo(MealVerseRoutes.WELCOME) { inclusive = true }
      }
    }
    repository.syncFcmToken()
  }

  // Deep-link to Order Tracking when user taps an FCM push notification
  LaunchedEffect(openTrackingFromNotification, notificationOrderId, currentUser?.uid) {
    if (openTrackingFromNotification) {
      trackedOrderId = notificationOrderId
      navController.navigate(MealVerseRoutes.ORDER_TRACKING)
      onNotificationHandled()
    }
  }

  // Watch Firestore customer orders in real time to surface foreground status-change alerts
  LaunchedEffect(currentUser?.uid, canAccessCustomer) {
    val uid = currentUser?.uid ?: return@LaunchedEffect
    if (uid.isBlank() || !canAccessCustomer) return@LaunchedEffect
    val knownStatuses = mutableMapOf<String, String>()
    var initialized = false

    repository.observeCustomerOrders(uid).collect { orders ->
      if (!initialized) {
        orders.forEach { order ->
          if (order.id.isNotBlank()) knownStatuses[order.id] = order.status
        }
        initialized = true
      } else {
        orders.forEach { order ->
          if (order.id.isNotBlank()) {
            val prev = knownStatuses[order.id]
            if (prev != null && prev != order.status && userProfile?.notificationsEnabled != false) {
              OrderNotificationHelper.showOrderStatusNotification(
                context = context,
                orderId = order.id,
                storeName = order.storeName,
                status = order.status
              )
            }
            knownStatuses[order.id] = order.status
          }
        }
      }
    }
  }

  NavHost(
    navController = navController,
    startDestination = MealVerseRoutes.WELCOME
  ) {
    // 1. Initial Welcome Screen
    composable(MealVerseRoutes.WELCOME) {
      WelcomeScreen(
        onNavigateToCustomerRegister = {
          navController.navigate(MealVerseRoutes.CUSTOMER_REGISTER)
        },
        onNavigateToShopkeeperRegister = {
          navController.navigate(MealVerseRoutes.SHOPKEEPER_REGISTER)
        },
        onExistingUserSignedIn = {
          scope.launch {
            val user = Firebase.auth.currentUser
            val uid = user?.uid ?: ""
            isProfileLoading = true
            val prof = if (uid.isNotBlank()) repository.getUserProfile(uid) else null
            userProfile = prof
            isProfileLoading = false
            val adminSession = AccessControl.isAdminAccessAllowed(
              isAuthenticated = user != null,
              email = user?.email,
              emailVerified = user?.isEmailVerified == true,
              profile = prof
            )
            if (adminSession) {
              repository.seedInitialDataIfNeeded(prof?.city ?: "Islamabad")
            }
            val destination = resolveAuthorizedHomeRoute(prof, adminSession)
            navController.navigate(destination) {
              if (prof != null || adminSession) {
                popUpTo(MealVerseRoutes.WELCOME) { inclusive = true }
              }
            }
          }
        },
        isAuthenticated = isAuthenticated,
        signedInEmail = authEmail.orEmpty(),
        isAdminAuthorized = isAdminAuthorized,
        onOpenAuthorizedDashboard = {
          val destination = resolveAuthorizedHomeRoute(userProfile, isAdminAuthorized)
          navController.navigate(destination)
        },
        onOpenAdminPanel = {
          navController.navigate(MealVerseRoutes.ADMIN_PANEL)
        },
        onSignOut = { performSignOut() },
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleDarkLight
      )
    }

    // 2. Customer Registration Flow
    composable(MealVerseRoutes.CUSTOMER_REGISTER) {
      CustomerRegistrationScreen(
        repository = repository,
        onRegistrationSuccess = { profile ->
          userProfile = profile
          navController.navigate(MealVerseRoutes.CUSTOMER_HOME) {
            popUpTo(MealVerseRoutes.WELCOME) { inclusive = true }
          }
        },
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }

    // 3. Shopkeeper Registration Flow
    composable(MealVerseRoutes.SHOPKEEPER_REGISTER) {
      ShopkeeperRegistrationScreen(
        repository = repository,
        onRegistrationSuccess = { profile ->
          userProfile = profile
          navController.navigate(MealVerseRoutes.SHOPKEEPER_DASHBOARD) {
            popUpTo(MealVerseRoutes.WELCOME) { inclusive = true }
          }
        },
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }

    // 4. Customer Home Marketplace (Secured)
    composable(MealVerseRoutes.CUSTOMER_HOME) {
      ProtectedCustomerGate(
        isAuthenticated = isAuthenticated,
        email = authEmail,
        isEmailVerified = isEmailVerified,
        profile = userProfile,
        isProfileLoading = isProfileLoading,
        navController = navController,
        canAccessShopkeeper = canAccessShopkeeper,
        canAccessAdmin = isAdminAuthorized,
        onSignOut = { performSignOut() }
      ) {
        val profile = userProfile ?: UserProfile(
          userId = currentUser?.uid ?: "",
          fullName = currentUser?.displayName ?: "MealVerse Member",
          email = authEmail ?: "",
          role = if (isAdminAuthorized) UserRole.ADMIN.name else UserRole.CUSTOMER.name,
          city = "Islamabad"
        )
        CustomerHomeScreen(
          repository = repository,
          currentProfile = profile,
          onSelectStore = { store ->
            selectedStore = store
            navController.navigate(MealVerseRoutes.STORE_DETAIL)
          },
          onOpenAiAssistant = { showAiAssistantDialog = true },
          onViewOrders = {
            trackedOrderId = null
            navController.navigate(MealVerseRoutes.ORDER_TRACKING)
          },
          onViewCart = { navController.navigate(MealVerseRoutes.CART_CHECKOUT) },
          onViewProfile = { navController.navigate(MealVerseRoutes.CUSTOMER_PROFILE) },
          onApplyVoucher = { code ->
            pendingVoucherCode = code
            if (cartItems.isNotEmpty()) {
              navController.navigate(MealVerseRoutes.CART_CHECKOUT)
            }
          },
          selectedVoucherCode = pendingVoucherCode,
          cartItemCount = cartItems.sumOf { it.quantity },
          onSwitchRole = { showAccountDialog = true },
          isDarkTheme = isDarkTheme,
          onToggleTheme = onToggleDarkLight
        )
      }
    }

    // 5. Store Detail Screen (Secured)
    composable(MealVerseRoutes.STORE_DETAIL) {
      BackHandler { navController.popBackStack() }
      ProtectedCustomerGate(
        isAuthenticated = isAuthenticated,
        email = authEmail,
        isEmailVerified = isEmailVerified,
        profile = userProfile,
        isProfileLoading = isProfileLoading,
        navController = navController,
        canAccessShopkeeper = canAccessShopkeeper,
        canAccessAdmin = isAdminAuthorized,
        onSignOut = { performSignOut() }
      ) {
        val store = selectedStore ?: Store(name = "Restaurant")
        StoreDetailScreen(
          store = store,
          repository = repository,
          cartItems = cartItems,
          onAddToCart = { product, qty, notes ->
            if (cartStore != null && cartStore?.id != store.id) {
              cartItems = listOf(CartItem(product, qty, notes))
              cartStore = store
            } else {
              cartStore = store
              val existing = cartItems.find { it.product.id == product.id }
              cartItems = if (existing != null) {
                cartItems.map {
                  if (it.product.id == product.id) it.copy(quantity = it.quantity + qty) else it
                }
              } else {
                cartItems + CartItem(product, qty, notes)
              }
            }
          },
          onBack = { navController.popBackStack() },
          onViewCart = { navController.navigate(MealVerseRoutes.CART_CHECKOUT) }
        )
      }
    }

    // 6. Cart & Checkout Screen (Secured)
    composable(MealVerseRoutes.CART_CHECKOUT) {
      BackHandler { navController.popBackStack() }
      ProtectedCustomerGate(
        isAuthenticated = isAuthenticated,
        email = authEmail,
        isEmailVerified = isEmailVerified,
        profile = userProfile,
        isProfileLoading = isProfileLoading,
        navController = navController,
        canAccessShopkeeper = canAccessShopkeeper,
        canAccessAdmin = isAdminAuthorized,
        onSignOut = { performSignOut() }
      ) {
        val profile = userProfile ?: UserProfile(
          userId = currentUser?.uid ?: "",
          fullName = currentUser?.displayName ?: "MealVerse Member",
          email = authEmail ?: "",
          role = if (isAdminAuthorized) UserRole.ADMIN.name else UserRole.CUSTOMER.name,
          city = "Islamabad"
        )
        CartCheckoutScreen(
          cartItems = cartItems,
          currentStore = cartStore ?: selectedStore,
          currentProfile = profile,
          repository = repository,
          preselectedVoucherCode = pendingVoucherCode,
          onConsumePreselectedVoucher = { pendingVoucherCode = null },
          onUpdateQuantity = { product, newQty ->
            cartItems = if (newQty <= 0) {
              cartItems.filter { it.product.id != product.id }
            } else {
              cartItems.map {
                if (it.product.id == product.id) it.copy(quantity = newQty) else it
              }
            }
          },
          onClearCart = {
            cartItems = emptyList()
            cartStore = null
          },
          onOrderPlaced = { orderId ->
            trackedOrderId = orderId
            navController.navigate(MealVerseRoutes.ORDER_TRACKING) {
              popUpTo(MealVerseRoutes.CUSTOMER_HOME)
            }
          },
          onBack = { navController.popBackStack() }
        )
      }
    }

    // 7. Order Tracking Screen (Secured)
    composable(MealVerseRoutes.ORDER_TRACKING) {
      BackHandler { navController.popBackStack() }
      ProtectedCustomerGate(
        isAuthenticated = isAuthenticated,
        email = authEmail,
        isEmailVerified = isEmailVerified,
        profile = userProfile,
        isProfileLoading = isProfileLoading,
        navController = navController,
        canAccessShopkeeper = canAccessShopkeeper,
        canAccessAdmin = isAdminAuthorized,
        onSignOut = { performSignOut() }
      ) {
        val uid = currentUser?.uid ?: userProfile?.userId ?: ""
        OrderTrackingScreen(
          currentUserId = uid,
          repository = repository,
          highlightedOrderId = trackedOrderId,
          onBack = { navController.popBackStack() }
        )
      }
    }

    // 7b. Customer Profile & Recent Orders Screen (Secured)
    composable(MealVerseRoutes.CUSTOMER_PROFILE) {
      BackHandler { navController.popBackStack() }
      ProtectedCustomerGate(
        isAuthenticated = isAuthenticated,
        email = authEmail,
        isEmailVerified = isEmailVerified,
        profile = userProfile,
        isProfileLoading = isProfileLoading,
        navController = navController,
        canAccessShopkeeper = canAccessShopkeeper,
        canAccessAdmin = isAdminAuthorized,
        onSignOut = { performSignOut() }
      ) {
        val uid = currentUser?.uid ?: userProfile?.userId ?: ""
        val profile = userProfile ?: UserProfile(
          userId = uid,
          fullName = currentUser?.displayName ?: "MealVerse Member",
          email = authEmail ?: "",
          role = if (isAdminAuthorized) UserRole.ADMIN.name else UserRole.CUSTOMER.name,
          city = "Islamabad"
        )
        CustomerProfileScreen(
          currentProfile = profile,
          currentUserId = uid,
          repository = repository,
          onBack = { navController.popBackStack() },
          onViewOrderTracking = { selectedOrderId ->
            trackedOrderId = selectedOrderId
            navController.navigate(MealVerseRoutes.ORDER_TRACKING)
          },
          onExploreRestaurants = {
            navController.popBackStack(MealVerseRoutes.CUSTOMER_HOME, inclusive = false)
          },
          onApplyVoucher = { code ->
            pendingVoucherCode = code
            if (cartItems.isNotEmpty()) {
              navController.navigate(MealVerseRoutes.CART_CHECKOUT)
            } else {
              navController.popBackStack(MealVerseRoutes.CUSTOMER_HOME, inclusive = false)
            }
          },
          selectedVoucherCode = pendingVoucherCode,
          themeMode = themeMode,
          isDarkTheme = isDarkTheme,
          onSelectThemeMode = onThemeModeChange,
          onSwitchRole = { showAccountDialog = true },
          onSignOut = { performSignOut() }
        )
      }
    }

    // 8. Shopkeeper Dashboard Screen (Secured)
    composable(MealVerseRoutes.SHOPKEEPER_DASHBOARD) {
      val access = AccessControl.evaluateShopkeeperAccess(
        isAuthenticated = isAuthenticated,
        email = authEmail,
        emailVerified = isEmailVerified,
        profile = userProfile,
        isProfileLoading = isProfileLoading
      )
      when (access) {
        is AccessCheckResult.Loading -> VerifyingAccessScreen("Verifying merchant credentials...")
        is AccessCheckResult.Denied -> {
          AccessRestrictedScreen(
            denial = access,
            onReturnWelcome = {
              navController.navigate(MealVerseRoutes.WELCOME) {
                popUpTo(0) { inclusive = true }
              }
            },
            onNavigateToShopkeeperRegister = {
              navController.navigate(MealVerseRoutes.SHOPKEEPER_REGISTER)
            },
            onGoToAuthorizedDashboard = if (canAccessCustomer) {
              { navController.navigate(MealVerseRoutes.CUSTOMER_HOME) }
            } else null,
            authorizedDashboardLabel = if (canAccessCustomer) "Go to Customer Home" else null,
            onSignOut = { performSignOut() }
          )
        }
        is AccessCheckResult.Allowed -> {
          val profile = userProfile ?: UserProfile(
            userId = currentUser?.uid ?: "",
            email = authEmail ?: "",
            role = UserRole.SHOPKEEPER.name
          )
          ShopkeeperDashboardScreen(
            currentProfile = profile,
            repository = repository,
            onNavigateToMenu = { store ->
              selectedStore = store
              navController.navigate(MealVerseRoutes.MENU_MANAGEMENT)
            },
            onSwitchRole = { showAccountDialog = true },
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleDarkLight
          )
        }
      }
    }

    // 9. Menu Management Screen (Secured)
    composable(MealVerseRoutes.MENU_MANAGEMENT) {
      BackHandler { navController.popBackStack() }
      val access = AccessControl.evaluateShopkeeperAccess(
        isAuthenticated = isAuthenticated,
        email = authEmail,
        emailVerified = isEmailVerified,
        profile = userProfile,
        isProfileLoading = isProfileLoading
      )
      when (access) {
        is AccessCheckResult.Loading -> VerifyingAccessScreen("Verifying merchant permissions...")
        is AccessCheckResult.Denied -> {
          AccessRestrictedScreen(
            denial = access,
            onReturnWelcome = {
              navController.navigate(MealVerseRoutes.WELCOME) {
                popUpTo(0) { inclusive = true }
              }
            },
            onNavigateToShopkeeperRegister = {
              navController.navigate(MealVerseRoutes.SHOPKEEPER_REGISTER)
            },
            onSignOut = { performSignOut() }
          )
        }
        is AccessCheckResult.Allowed -> {
          val store = selectedStore ?: Store(name = "My Shop")
          MenuManagementScreen(
            store = store,
            repository = repository,
            onBack = { navController.popBackStack() }
          )
        }
      }
    }

    // 10. Admin Verification Panel (Secured)
    composable(MealVerseRoutes.ADMIN_PANEL) {
      BackHandler {
        if (!navController.popBackStack()) {
          navController.navigate(MealVerseRoutes.WELCOME) {
            popUpTo(0) { inclusive = true }
          }
        }
      }
      val adminAccess = AccessControl.evaluateAdminAccess(
        isAuthenticated = isAuthenticated,
        email = authEmail,
        emailVerified = isEmailVerified,
        profile = userProfile,
        isProfileLoading = isProfileLoading
      )
      when (adminAccess) {
        is AccessCheckResult.Loading -> VerifyingAccessScreen("Verifying administrator privileges...")
        is AccessCheckResult.Denied -> {
          AccessRestrictedScreen(
            denial = adminAccess,
            onReturnWelcome = {
              navController.navigate(MealVerseRoutes.WELCOME) {
                popUpTo(0) { inclusive = true }
              }
            },
            onGoToAuthorizedDashboard = when {
              canAccessCustomer -> ({ navController.navigate(MealVerseRoutes.CUSTOMER_HOME) })
              canAccessShopkeeper -> ({ navController.navigate(MealVerseRoutes.SHOPKEEPER_DASHBOARD) })
              else -> null
            },
            authorizedDashboardLabel = when {
              canAccessCustomer -> "Go to Customer Panel"
              canAccessShopkeeper -> "Go to Shopkeeper Portal"
              else -> null
            },
            onSignOut = { performSignOut() }
          )
        }
        is AccessCheckResult.Allowed -> {
          AdminVerificationScreen(
            repository = repository,
            adminEmail = authEmail.orEmpty(),
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleDarkLight,
            onSwitchRole = { showAccountDialog = true },
            onSignOut = { performSignOut() },
            onBack = {
              if (!navController.popBackStack()) {
                navController.navigate(MealVerseRoutes.WELCOME) {
                  popUpTo(0) { inclusive = true }
                }
              }
            }
          )
        }
      }
    }
  }

  // AI Assistant Modal Dialog (Available only when customer access is verified)
  if (showAiAssistantDialog && canAccessCustomer) {
    val aiStores by repository.observeApprovedStores(userProfile?.city ?: "Islamabad").collectAsState(initial = emptyList())
    AiFoodAssistantDialog(
      stores = aiStores,
      currentCity = userProfile?.city ?: "Islamabad",
      onDismiss = { showAiAssistantDialog = false }
    )
  }

  // Secured Account & Authorized Panel Switcher Dialog
  if (showAccountDialog) {
    val roleLabel = when {
      isAdminAuthorized -> "VERIFIED ADMIN"
      userProfile != null -> userProfile?.role.orEmpty()
      else -> "GUEST"
    }
    AlertDialog(
      onDismissRequest = { showAccountDialog = false },
      title = { Text("Account & Access", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.VerifiedUser,
                  contentDescription = null,
                  tint = MealVerseDeepTeal,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = roleLabel,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                  color = MealVerseDeepTeal
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = authEmail?.ifBlank { "Not signed in" } ?: "Not signed in",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          if (canAccessCustomer) {
            OutlinedButton(
              onClick = {
                navController.navigate(MealVerseRoutes.CUSTOMER_HOME)
                showAccountDialog = false
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("account_dialog_customer_btn")
            ) {
              Icon(Icons.Default.Fastfood, contentDescription = null, tint = MealVerseDeepTeal)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Customer Panel")
            }
          }

          if (canAccessShopkeeper) {
            OutlinedButton(
              onClick = {
                navController.navigate(MealVerseRoutes.SHOPKEEPER_DASHBOARD)
                showAccountDialog = false
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("account_dialog_shopkeeper_btn")
            ) {
              Icon(Icons.Default.Storefront, contentDescription = null, tint = MealVerseWarmOrange)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Shopkeeper Portal")
            }
          }

          if (isAdminAuthorized) {
            OutlinedButton(
              onClick = {
                navController.navigate(MealVerseRoutes.ADMIN_PANEL)
                showAccountDialog = false
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("account_dialog_admin_btn")
            ) {
              Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MealVerseDeepTeal)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Admin Verification Panel")
            }
          }

          OutlinedButton(
            onClick = {
              navController.navigate(MealVerseRoutes.WELCOME) {
                popUpTo(0) { inclusive = true }
              }
              showAccountDialog = false
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = MealVerseDeepTeal)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Welcome Screen")
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

          Text(
            text = "Appearance (Light / Dark)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          ThemeModeSelectorRow(
            themeMode = themeMode,
            onSelectMode = onThemeModeChange
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

          if (isAuthenticated) {
            TextButton(
              onClick = { performSignOut() },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("account_dialog_sign_out_btn")
            ) {
              Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sign Out", color = MaterialTheme.colorScheme.error)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAccountDialog = false }) {
          Text("Close")
        }
      }
    )
  }
}

@Composable
private fun ProtectedCustomerGate(
  isAuthenticated: Boolean,
  email: String?,
  isEmailVerified: Boolean,
  profile: UserProfile?,
  isProfileLoading: Boolean,
  navController: NavHostController,
  canAccessShopkeeper: Boolean,
  canAccessAdmin: Boolean,
  onSignOut: () -> Unit,
  content: @Composable () -> Unit
) {
  val access = AccessControl.evaluateCustomerAccess(
    isAuthenticated = isAuthenticated,
    email = email,
    emailVerified = isEmailVerified,
    profile = profile,
    isProfileLoading = isProfileLoading
  )
  when (access) {
    is AccessCheckResult.Loading -> VerifyingAccessScreen("Verifying customer session...")
    is AccessCheckResult.Denied -> {
      AccessRestrictedScreen(
        denial = access,
        onReturnWelcome = {
          navController.navigate(MealVerseRoutes.WELCOME) {
            popUpTo(0) { inclusive = true }
          }
        },
        onNavigateToCustomerRegister = if (access.requiresCustomerRegistration) {
          { navController.navigate(MealVerseRoutes.CUSTOMER_REGISTER) }
        } else null,
        onGoToAuthorizedDashboard = when {
          canAccessAdmin -> ({ navController.navigate(MealVerseRoutes.ADMIN_PANEL) })
          canAccessShopkeeper -> ({ navController.navigate(MealVerseRoutes.SHOPKEEPER_DASHBOARD) })
          else -> null
        },
        authorizedDashboardLabel = when {
          canAccessAdmin -> "Go to Admin Panel"
          canAccessShopkeeper -> "Go to Shopkeeper Portal"
          else -> null
        },
        onSignOut = onSignOut
      )
    }
    is AccessCheckResult.Allowed -> content()
  }
}
