package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.MealVerseRepository
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRegistrationScreen(
  repository: MealVerseRepository,
  onRegistrationSuccess: (UserProfile) -> Unit,
  onNavigateBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  var fullName by remember { mutableStateOf(currentUser?.displayName ?: "") }
  var phoneNumber by remember { mutableStateOf("+92 3") }
  var selectedCity by remember { mutableStateOf("Islamabad") }
  var deliveryAddress by remember { mutableStateOf("") }
  var houseFlat by remember { mutableStateOf("") }
  var landmark by remember { mutableStateOf("") }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var cityExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(currentUser) {
    if (currentUser != null && fullName.isBlank()) {
      fullName = currentUser?.displayName ?: ""
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Customer Registration",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("customer_reg_back_btn")
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
    containerColor = MaterialTheme.colorScheme.background
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp)
    ) {
      // Header Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MealVerseDeepTeal.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(MealVerseDeepTeal),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Fastfood,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(28.dp)
            )
          }
          Spacer(modifier = Modifier.width(16.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Food Lover Account",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium,
              color = MealVerseDeepTeal
            )
            Text(
              text = "Order from verified local restaurants, dhabas & home kitchens with real-time tracking.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Error message if any
      if (errorMessage != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
          )
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
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "1. Connect Google Account",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Sign in securely with Google to verify your account and safely save delivery addresses.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 8.dp)
            )
            GoogleSignInButton(
              buttonText = "Continue with Google",
              onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                fullName = currentUser?.displayName ?: ""
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
          shape = RoundedCornerShape(12.dp),
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
              tint = MealVerseDeepTeal,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Connected Google Account",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${currentUser?.displayName ?: "User"} (${currentUser?.email})",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Step 2: Personal & Delivery Information
      Text(
        text = "Delivery Details",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 12.dp)
      )

      OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text("Full Name *") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("customer_name_input"),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = phoneNumber,
        onValueChange = { phoneNumber = it },
        label = { Text("Mobile Contact (+92 3XX XXXXXXX) *") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("customer_phone_input"),
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
          label = { Text("Delivery City *") },
          leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
          modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .testTag("customer_city_dropdown")
        )
        ExposedDropdownMenu(
          expanded = cityExpanded,
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

      OutlinedTextField(
        value = deliveryAddress,
        onValueChange = { deliveryAddress = it },
        label = { Text("Delivery Address (Street / Sector / Area) *") },
        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("customer_address_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = houseFlat,
          onValueChange = { houseFlat = it },
          label = { Text("House / Flat #") },
          modifier = Modifier
            .weight(1f)
            .testTag("customer_house_input"),
          singleLine = true
        )
        Spacer(modifier = Modifier.width(10.dp))
        OutlinedTextField(
          value = landmark,
          onValueChange = { landmark = it },
          label = { Text("Landmark (Nearby)") },
          modifier = Modifier
            .weight(1.2f)
            .testTag("customer_landmark_input"),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = {
          if (currentUser == null) {
            errorMessage = "Please sign in with Google first to create your account."
            return@Button
          }
          if (fullName.isBlank()) {
            errorMessage = "Please enter your full name."
            return@Button
          }
          if (phoneNumber.length < 10) {
            errorMessage = "Please enter a valid Pakistani contact number."
            return@Button
          }
          if (deliveryAddress.isBlank()) {
            errorMessage = "Please enter your street / area delivery address."
            return@Button
          }

          scope.launch {
            isLoading = true
            try {
              val profile = UserProfile(
                userId = currentUser?.uid ?: "",
                fullName = fullName,
                email = currentUser?.email ?: "",
                phoneNumber = phoneNumber,
                role = UserRole.CUSTOMER.name,
                city = selectedCity,
                address = deliveryAddress,
                houseFlat = houseFlat,
                landmark = landmark
              )
              repository.saveUserProfile(profile)
              isLoading = false
              onRegistrationSuccess(profile)
            } catch (e: Exception) {
              isLoading = false
              errorMessage = "Failed to save customer profile: ${e.message}"
            }
          }
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("complete_customer_reg_btn")
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color.White,
            strokeWidth = 2.dp
          )
        } else {
          Icon(Icons.Default.Fastfood, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Complete Customer Registration",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
