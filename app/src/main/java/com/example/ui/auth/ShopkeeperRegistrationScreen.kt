package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.MealVerseRepository
import com.example.ui.components.FullScreenMapLocationPickerDialog
import com.example.ui.components.StoreLocationPickerCard
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopkeeperRegistrationScreen(
  repository: MealVerseRepository,
  onRegistrationSuccess: (UserProfile) -> Unit,
  onNavigateBack: () -> Unit,
  initialIsLoading: Boolean = false
) {
  val scope = rememberCoroutineScope()
  var currentUser by remember {
    mutableStateOf(try { Firebase.auth.currentUser } catch (e: Exception) { null })
  }

  // Required Fields
  var ownerName by remember { mutableStateOf(currentUser?.displayName ?: "") }
  var businessName by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("Pakistani Food") }
  var shopCnic by remember { mutableStateOf("") }

  // Additional Store Details
  var shopPhone by remember { mutableStateOf("+92 3") }
  var selectedCity by remember { mutableStateOf("Islamabad") }
  var shopAddress by remember { mutableStateOf("") }
  var shopDescription by remember { mutableStateOf("") }
  var bankAccount by remember { mutableStateOf("") }
  var pinnedLatitude by remember { mutableStateOf<Double?>(null) }
  var pinnedLongitude by remember { mutableStateOf<Double?>(null) }
  var showMapPicker by remember { mutableStateOf(false) }

  var isLoading by remember { mutableStateOf(initialIsLoading) }
  var hasAttemptedSubmit by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var cityExpanded by remember { mutableStateOf(false) }
  var categoryExpanded by remember { mutableStateOf(false) }

  val shopCategories = listOf(
    "Pakistani Food",
    "Biryani & Pulao",
    "Karahi & Handi",
    "Fast Food & Burgers",
    "BBQ & Grills",
    "Desi Nashta & Chai",
    "Home Kitchen / Homemade",
    "Bakery & Sweets",
    "Grocery & Mart"
  )

  LaunchedEffect(currentUser) {
    if (currentUser != null && ownerName.isBlank()) {
      ownerName = currentUser?.displayName ?: ""
    }
  }

  // Format CNIC as XXXXX-XXXXXXX-X (13 digits)
  fun formatCnicInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(13)
    return buildString {
      for (i in digits.indices) {
        if (i == 5 || i == 12) {
          append('-')
        }
        append(digits[i])
      }
    }
  }

  val cnicDigitsCount = shopCnic.filter { it.isDigit() }.length
  val ownerError = hasAttemptedSubmit && ownerName.isBlank()
  val businessError = hasAttemptedSubmit && businessName.isBlank()
  val cnicError = hasAttemptedSubmit && cnicDigitsCount != 13
  val addressError = hasAttemptedSubmit && shopAddress.isBlank()
  val coordinatesError = hasAttemptedSubmit && (pinnedLatitude == null || pinnedLongitude == null)

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Shopkeeper Registration",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            enabled = !isLoading,
            modifier = Modifier.testTag("shopkeeper_reg_back_btn")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp)
      ) {
        // Header Gradient Card
        Card(
          shape = RoundedCornerShape(20.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .background(
                Brush.horizontalGradient(
                  colors = listOf(MealVerseWarmOrange, Color(0xFFFF8800))
                )
              )
              .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "MealVerse Merchant Partner",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Launch your digital storefront & reach thousands of food lovers across Pakistan.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.92f)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3-Step Onboarding Strip
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          StepPill(step = "1", label = "Account", done = currentUser != null, modifier = Modifier.weight(1f))
          StepPill(
            step = "2",
            label = "Business & CNIC",
            done = ownerName.isNotBlank() && businessName.isNotBlank() && cnicDigitsCount == 13,
            modifier = Modifier.weight(1f)
          )
          StepPill(
            step = "3",
            label = "Location",
            done = shopAddress.isNotBlank() && pinnedLatitude != null && pinnedLongitude != null,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status 'Pending' Notice Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFCC80), RoundedCornerShape(14.dp))
            .testTag("shopkeeper_status_pending_badge")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.HourglassTop,
              contentDescription = null,
              tint = Color(0xFFF57F17),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Initial Account Status: 'Pending'",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFE65100)
              )
              Text(
                text = "Upon registration, your account status will be set to 'Pending' in Firestore pending verification before live fulfillment.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5D4037)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message if any
        if (errorMessage != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }

        // Step 1: Authentication Requirement
        if (currentUser == null) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "1. Connect Merchant Account",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Sign in to securely link your digital store, payout details, and daily orders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
              )
              GoogleSignInButton(
                buttonText = "Sign in with Google for Merchant",
                containerColor = MealVerseWarmOrange,
                onAuthSuccess = {
                  currentUser = Firebase.auth.currentUser
                  ownerName = currentUser?.displayName ?: ""
                  errorMessage = null
                },
                onAuthError = { err ->
                  errorMessage = err
                }
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else {
          // Authenticated badge
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MealVerseSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MealVerseWarmOrange,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Connected Merchant Account",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "${currentUser?.displayName ?: "Shopkeeper"} (${currentUser?.email})",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Step 2: Required Information Section
        Text(
          text = "Required Merchant Information",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Owner Name Field
        OutlinedTextField(
          value = ownerName,
          onValueChange = { ownerName = it },
          enabled = !isLoading,
          isError = ownerError,
          label = { Text("Owner Name *") },
          placeholder = { Text("e.g. Tariq Mehmood") },
          supportingText = if (ownerError) {
            { Text("Owner name is required") }
          } else null,
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shopkeeper_owner_name_input"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Business Name Field
        OutlinedTextField(
          value = businessName,
          onValueChange = { businessName = it },
          enabled = !isLoading,
          isError = businessError,
          label = { Text("Business Name *") },
          placeholder = { Text("e.g. Butt Karahi or Savour Foods") },
          supportingText = if (businessError) {
            { Text("Business name is required") }
          } else null,
          leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shopkeeper_business_name_input"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Category Field (Dropdown)
        ExposedDropdownMenuBox(
          expanded = categoryExpanded && !isLoading,
          onExpandedChange = { if (!isLoading) categoryExpanded = !categoryExpanded }
        ) {
          OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            enabled = !isLoading,
            label = { Text("Category *") },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .testTag("shopkeeper_category_dropdown")
          )
          ExposedDropdownMenu(
            expanded = categoryExpanded && !isLoading,
            onDismissRequest = { categoryExpanded = false }
          ) {
            shopCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat) },
                onClick = {
                  selectedCategory = cat
                  categoryExpanded = false
                }
              )
            }
          }
        }

        // Scrollable Quick Category Chips (fixes clipping on narrow screens!)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Quick Select Category:",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val quickChips = listOf(
            "Pakistani Food",
            "Biryani & Pulao",
            "Karahi & Handi",
            "Fast Food & Burgers",
            "BBQ & Grills",
            "Desi Nashta & Chai"
          )
          items(quickChips) { cat ->
            val isSelected = selectedCategory == cat
            FilterChip(
              selected = isSelected,
              enabled = !isLoading,
              onClick = { selectedCategory = cat },
              label = { Text(cat, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MealVerseWarmOrange,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. CNIC Number Field (Pakistani format: XXXXX-XXXXXXX-X)
        OutlinedTextField(
          value = shopCnic,
          onValueChange = { input ->
            shopCnic = formatCnicInput(input)
          },
          enabled = !isLoading,
          isError = cnicError,
          label = { Text("CNIC Number (13 Digits) *") },
          placeholder = { Text("42101-1234567-1") },
          supportingText = {
            Text(
              text = when {
                cnicError -> "Please enter all 13 digits of your Pakistani CNIC"
                cnicDigitsCount == 13 -> "Valid 13-digit CNIC format"
                else -> "$cnicDigitsCount/13 digits (Pakistani CNIC: XXXXX-XXXXXXX-X)"
              },
              color = when {
                cnicError -> MaterialTheme.colorScheme.error
                cnicDigitsCount == 13 -> Color(0xFF2E7D32)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          },
          leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shopkeeper_cnic_input"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Additional Business Details
        Text(
          text = "Location & Contact Information",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.padding(vertical = 8.dp)
        )

        // Business Phone
        OutlinedTextField(
          value = shopPhone,
          onValueChange = { shopPhone = it },
          enabled = !isLoading,
          label = { Text("Business Phone (+92 3XX XXXXXXX) *") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_phone_input"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // City Selector Dropdown
        ExposedDropdownMenuBox(
          expanded = cityExpanded && !isLoading,
          onExpandedChange = { if (!isLoading) cityExpanded = !cityExpanded }
        ) {
          OutlinedTextField(
            value = selectedCity,
            onValueChange = {},
            readOnly = true,
            enabled = !isLoading,
            label = { Text("City of Operation *") },
            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .testTag("shop_city_dropdown")
          )
          ExposedDropdownMenu(
            expanded = cityExpanded && !isLoading,
            onDismissRequest = { cityExpanded = false }
          ) {
            PAKISTAN_CITIES.forEach { city ->
              DropdownMenuItem(
                text = { Text(city) },
                onClick = {
                  selectedCity = city
                  cityExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Physical Address
        OutlinedTextField(
          value = shopAddress,
          onValueChange = { shopAddress = it },
          enabled = !isLoading,
          isError = addressError,
          label = { Text("Physical Shop Address (Shop #, Plaza / Road) *") },
          placeholder = { Text("e.g. Shop # 4, Main Commercial Market") },
          supportingText = if (addressError) {
            { Text("Physical shop address is required") }
          } else null,
          leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_address_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Map-Based Store Delivery Location Picker
        StoreLocationPickerCard(
          city = selectedCity,
          latitude = pinnedLatitude,
          longitude = pinnedLongitude,
          enabled = !isLoading,
          isError = coordinatesError,
          onOpenFullMap = { showMapPicker = true },
          onLocationCaptured = { lat, lon, resolvedAddress ->
            pinnedLatitude = lat
            pinnedLongitude = lon
            if (shopAddress.isBlank() && !resolvedAddress.isNullOrBlank()) {
              shopAddress = resolvedAddress
            }
          }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Settlement Account
        OutlinedTextField(
          value = bankAccount,
          onValueChange = { bankAccount = it },
          enabled = !isLoading,
          label = { Text("Settlement Account (JazzCash / Easypaisa / Bank Title)") },
          placeholder = { Text("e.g. Tariq Mehmood - JazzCash 03001234567") },
          leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_settlement_input"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Menu Highlights / Specialty
        OutlinedTextField(
          value = shopDescription,
          onValueChange = { shopDescription = it },
          enabled = !isLoading,
          label = { Text("Shop Specialties & Highlights") },
          placeholder = { Text("e.g. Special Beef Pulao, Desi Ghee Karahi & Fresh Tandoori Naan") },
          leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_desc_input")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Submit Button with explicit CircularProgressIndicator & Disabled State during Firestore upload
        Button(
          onClick = {
            hasAttemptedSubmit = true
            if (currentUser == null) {
              errorMessage = "Please connect with Google first."
              return@Button
            }
            if (ownerName.isBlank()) {
              errorMessage = "Please enter the owner name."
              return@Button
            }
            if (businessName.isBlank()) {
              errorMessage = "Please enter the business name."
              return@Button
            }
            if (selectedCategory.isBlank()) {
              errorMessage = "Please select a business category."
              return@Button
            }
            if (cnicDigitsCount != 13) {
              errorMessage = "Please enter a valid 13-digit CNIC number (e.g. 42101-1234567-1)."
              return@Button
            }
            if (shopAddress.isBlank()) {
              errorMessage = "Please enter the physical shop address."
              return@Button
            }
            if (pinnedLatitude == null || pinnedLongitude == null) {
              errorMessage = "Please pin your exact store delivery coordinates on the map."
              return@Button
            }

            errorMessage = null
            scope.launch {
              isLoading = true
              try {
                // Registers shopkeeper with initial account status explicitly set to 'Pending' in Firestore
                val (profile, _, _) = repository.registerShopkeeper(
                  ownerName = ownerName.trim(),
                  businessName = businessName.trim(),
                  category = selectedCategory.trim(),
                  cnicNumber = shopCnic.trim(),
                  phone = shopPhone.trim(),
                  city = selectedCity.trim(),
                  address = shopAddress.trim(),
                  description = shopDescription.trim(),
                  bankAccountTitle = bankAccount.trim(),
                  latitude = pinnedLatitude,
                  longitude = pinnedLongitude
                )

                isLoading = false
                onRegistrationSuccess(profile)
              } catch (e: Exception) {
                isLoading = false
                errorMessage = "Failed to submit merchant application: ${e.message}"
              }
            }
          },
          enabled = !isLoading,
          colors = ButtonDefaults.buttonColors(
            containerColor = MealVerseWarmOrange,
            disabledContainerColor = MealVerseWarmOrange.copy(alpha = 0.55f),
            disabledContentColor = Color.White.copy(alpha = 0.9f)
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("complete_shopkeeper_reg_btn")
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier
                .size(22.dp)
                .testTag("shopkeeper_reg_loading_indicator"),
              color = Color.White,
              strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Submitting to Firestore...",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = Color.White
            )
          } else {
            Icon(Icons.Default.Storefront, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Submit Registration (Pending Status)",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  if (showMapPicker) {
    FullScreenMapLocationPickerDialog(
      city = selectedCity,
      initialLatitude = pinnedLatitude,
      initialLongitude = pinnedLongitude,
      onDismiss = { showMapPicker = false },
      onConfirmLocation = { lat, lon, resolvedAddress ->
        pinnedLatitude = lat
        pinnedLongitude = lon
        if (shopAddress.isBlank() && !resolvedAddress.isNullOrBlank()) {
          shopAddress = resolvedAddress
        }
        showMapPicker = false
      }
    )
  }
}

@Composable
private fun StepPill(
  step: String,
  label: String,
  done: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (done) MealVerseWarmOrange.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(18.dp)
          .clip(CircleShape)
          .background(if (done) MealVerseWarmOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
      ) {
        if (done) {
          Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        } else {
          Text(step, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = if (done) FontWeight.Bold else FontWeight.Medium,
        color = if (done) MealVerseWarmOrange else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
    }
  }
}
