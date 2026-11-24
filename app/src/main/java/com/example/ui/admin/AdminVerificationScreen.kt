package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Store
import com.example.data.model.StoreStatus
import com.example.data.repository.MealVerseRepository
import com.example.ui.components.Animated3DFoodThumbnail
import com.example.ui.components.MapGeoUtils
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.customer.get3DFoodGraphic
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationScreen(
  repository: MealVerseRepository,
  adminEmail: String = "",
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
  onSwitchRole: () -> Unit = {},
  onSignOut: () -> Unit = {},
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val allStores by repository.observeAllStoresForAdmin().collectAsState(initial = emptyList())

  var filterStatus by remember { mutableStateOf<StoreStatus?>(null) }
  var storeToReject by remember { mutableStateOf<Store?>(null) }
  var rejectReason by remember { mutableStateOf("") }
  var actionError by remember { mutableStateOf<String?>(null) }

  val filteredStores = allStores.filter {
    filterStatus == null || it.status.equals(filterStatus?.name, ignoreCase = true)
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text("Admin Verification Panel", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("admin_back_btn")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          ThemeQuickToggleButton(
            isDarkTheme = isDarkTheme,
            onToggle = onToggleTheme,
            modifier = Modifier.testTag("admin_theme_toggle_btn")
          )
          IconButton(
            onClick = onSwitchRole,
            modifier = Modifier.testTag("admin_account_menu_btn")
          ) {
            Icon(Icons.Default.ManageAccounts, contentDescription = "Admin Account & Panels")
          }
          IconButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("admin_sign_out_btn")
          ) {
            Icon(
              Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = MaterialTheme.colorScheme.error
            )
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
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Verified Admin Security Header
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isDarkTheme) Color(0xFF103235) else MealVerseDeepTeal.copy(alpha = 0.09f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_verified_session_banner")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Verified Administrator",
                tint = if (isDarkTheme) Color(0xFF80CBC4) else MealVerseDeepTeal,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Protected Administrator Session",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (isDarkTheme) Color(0xFFB2DFDB) else MealVerseDeepTeal
                )
                Text(
                  text = if (adminEmail.isNotBlank()) "Verified Admin: $adminEmail" else "Role-gated store verification controls",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        if (actionError != null) {
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = actionError.orEmpty(),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
              )
            }
          }
        }

        // Filter Chips
        item {
          val pendingCount = allStores.count { it.status.equals(StoreStatus.PENDING.name, ignoreCase = true) }
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            FilterChip(
              selected = filterStatus == null,
              onClick = { filterStatus = null },
              label = { Text("All (${allStores.size})") }
            )
            FilterChip(
              selected = filterStatus == StoreStatus.PENDING,
              onClick = { filterStatus = StoreStatus.PENDING },
              label = { Text("Pending ($pendingCount)") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MealVerseWarmOrange,
                selectedLabelColor = Color.White
              )
            )
            FilterChip(
              selected = filterStatus == StoreStatus.APPROVED,
              onClick = { filterStatus = StoreStatus.APPROVED },
              label = { Text("Approved") }
            )
          }
        }

        if (filteredStores.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
            ) {
              Box(
                modifier = Modifier
                  .padding(24.dp)
                  .fillMaxWidth(),
                contentAlignment = Alignment.Center
              ) {
                Text("No merchant stores found in this category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        } else {
          items(filteredStores, key = { it.id }) { store ->
            val isApproved = store.status.equals(StoreStatus.APPROVED.name, ignoreCase = true)
            val isPending = store.status.equals(StoreStatus.PENDING.name, ignoreCase = true)
            val isRejected = store.status.equals(StoreStatus.REJECTED.name, ignoreCase = true)

            Card(
              shape = RoundedCornerShape(18.dp),
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
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Animated3DFoodThumbnail(
                      imageRes = get3DFoodGraphic(store.name, store.category),
                      contentDescription = store.name,
                      size = 48.dp,
                      cornerRadius = 12.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(store.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                      Text("${store.category} • ${store.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                      isApproved && isDarkTheme -> Color(0xFF173620)
                      isApproved -> Color(0xFFE8F5E9)
                      isPending && isDarkTheme -> Color(0xFF3A2D12)
                      isPending -> Color(0xFFFFF8E1)
                      isDarkTheme -> Color(0xFF3E1818)
                      else -> Color(0xFFFFEBEE)
                    }
                  ) {
                    Text(
                      text = store.status,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when {
                          isApproved -> if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
                          isPending -> if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFFE65100)
                          else -> if (isDarkTheme) Color(0xFFEF9A9A) else Color(0xFFC62828)
                        }
                      )
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Address: ${store.address}", style = MaterialTheme.typography.bodySmall)
                if (store.latitude != null && store.longitude != null) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = MealVerseDeepTeal,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Delivery Pin: ${MapGeoUtils.formatCoordinates(store.latitude, store.longitude)}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                      color = MealVerseDeepTeal
                    )
                  }
                }
                Text("Phone: ${store.phone}", style = MaterialTheme.typography.bodySmall)
                if (store.cnicNumber.isNotBlank()) {
                  Text("CNIC: ${store.cnicNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                if (store.bankAccountTitle.isNotBlank()) {
                  Text("Account: ${store.bankAccountTitle}", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Admin Action Buttons
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  if (!isApproved) {
                    Button(
                      onClick = {
                        actionError = null
                        scope.launch {
                          try {
                            repository.updateStoreStatus(store.id, StoreStatus.APPROVED)
                          } catch (e: Exception) {
                            actionError = e.localizedMessage ?: "Unable to approve store."
                          }
                        }
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Approve Store")
                    }
                  }

                  if (!isRejected) {
                    OutlinedButton(
                      onClick = {
                        storeToReject = store
                        rejectReason = "Missing documentation or unverified address."
                      },
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Reject", color = MaterialTheme.colorScheme.error)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Reject Reason Dialog
  if (storeToReject != null) {
    val currentToReject = storeToReject!!
    AlertDialog(
      onDismissRequest = { storeToReject = null },
      title = { Text("Reject Store Verification", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Provide the reason for rejecting ${currentToReject.name}:", style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = rejectReason,
            onValueChange = { rejectReason = it },
            label = { Text("Rejection Reason") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            actionError = null
            scope.launch {
              try {
                repository.updateStoreStatus(currentToReject.id, StoreStatus.REJECTED, rejectReason)
                storeToReject = null
              } catch (e: Exception) {
                actionError = e.localizedMessage ?: "Unable to reject store."
                storeToReject = null
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Confirm Rejection")
        }
      },
      dismissButton = {
        TextButton(onClick = { storeToReject = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
