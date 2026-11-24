package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.security.AccessCheckResult
import com.example.ui.theme.MealVerseDeepTeal
import com.example.ui.theme.MealVerseWarmOrange

@Composable
fun VerifyingAccessScreen(
  message: String = "Verifying account permissions..."
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(24.dp)
      ) {
        CircularProgressIndicator(color = MealVerseDeepTeal)
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun AccessRestrictedScreen(
  denial: AccessCheckResult.Denied,
  onReturnWelcome: () -> Unit,
  onNavigateToCustomerRegister: (() -> Unit)? = null,
  onNavigateToShopkeeperRegister: (() -> Unit)? = null,
  onGoToAuthorizedDashboard: (() -> Unit)? = null,
  authorizedDashboardLabel: String? = null,
  onSignOut: (() -> Unit)? = null
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
          .widthIn(max = 480.dp)
          .fillMaxWidth()
          .testTag("access_restricted_card")
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (denial.title.contains("Admin", ignoreCase = true)) {
                Icons.Default.AdminPanelSettings
              } else {
                Icons.Default.Lock
              },
              contentDescription = "Access Restricted",
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.size(34.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = denial.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("access_restricted_title")
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = denial.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(22.dp))

          if (denial.requiresCustomerRegistration && onNavigateToCustomerRegister != null) {
            Button(
              onClick = onNavigateToCustomerRegister,
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("access_restricted_customer_reg_btn")
            ) {
              Icon(Icons.Default.PersonAdd, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Complete Customer Registration", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          if (denial.requiresShopkeeperRegistration && onNavigateToShopkeeperRegister != null) {
            Button(
              onClick = onNavigateToShopkeeperRegister,
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseWarmOrange),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("access_restricted_shopkeeper_reg_btn")
            ) {
              Icon(Icons.Default.Storefront, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Complete Shopkeeper Registration", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          if (onGoToAuthorizedDashboard != null && !authorizedDashboardLabel.isNullOrBlank()) {
            Button(
              onClick = onGoToAuthorizedDashboard,
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("access_restricted_go_dashboard_btn")
            ) {
              Text(authorizedDashboardLabel, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          OutlinedButton(
            onClick = onReturnWelcome,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("access_restricted_home_btn")
          ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Welcome & Sign In")
          }

          if (!denial.requiresSignIn && onSignOut != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
              onClick = onSignOut,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("access_restricted_signout_btn")
            ) {
              Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sign Out & Switch Account", color = MaterialTheme.colorScheme.error)
            }
          }
        }
      }
    }
  }
}
