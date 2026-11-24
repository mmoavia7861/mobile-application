package com.example.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.components.Interactive3DFoodStage
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
  onNavigateToCustomerRegister: () -> Unit,
  onNavigateToShopkeeperRegister: () -> Unit,
  onExistingUserSignedIn: () -> Unit,
  isAuthenticated: Boolean = false,
  signedInEmail: String = "",
  isAdminAuthorized: Boolean = false,
  onOpenAuthorizedDashboard: () -> Unit = {},
  onOpenAdminPanel: () -> Unit = {},
  onSignOut: () -> Unit = {},
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {}
) {
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showSignInModal by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MealVerseDeepTeal.copy(alpha = 0.08f),
              MaterialTheme.colorScheme.background,
              MealVerseWarmOrange.copy(alpha = 0.05f)
            )
          )
        )
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Branding Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    colors = listOf(MealVerseDeepTeal, MealVerseLightTeal)
                  )
                )
                .border(2.dp, MealVerseSaffronYellow.copy(alpha = 0.65f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_mealverse_logo),
                contentDescription = "MealVerse Logo",
                modifier = Modifier.size(42.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "MealVerse",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary
                )
              )
              Text(
                text = "“Har Zaiqa, Har Ghar”",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MealVerseWarmOrange
                )
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ThemeQuickToggleButton(
              isDarkTheme = isDarkTheme,
              onToggle = onToggleTheme,
              modifier = Modifier.testTag("welcome_theme_toggle_btn")
            )

            if (!isAuthenticated) {
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .clickable { showSignInModal = true }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                  )
                }
              }
            } else {
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .clickable { onOpenAuthorizedDashboard() }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive 3D Animated Food Stage
        Interactive3DFoodStage(
          compact = true,
          onDishSelected = { onNavigateToCustomerRegister() }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "How would you like to join?",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
          ),
          textAlign = TextAlign.Center
        )

        Text(
          text = "Choose your experience to explore fresh Pakistani meals or launch your digital restaurant.",
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorMessage ?: "",
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(10.dp),
              style = MaterialTheme.typography.bodySmall
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PATH 1: CUSTOMER REGISTRATION CARD
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = 1.5.dp,
              brush = Brush.linearGradient(
                colors = listOf(MealVerseDeepTeal.copy(alpha = 0.5f), MealVerseLightTeal.copy(alpha = 0.25f))
              ),
              shape = RoundedCornerShape(22.dp)
            )
            .testTag("welcome_customer_card")
        ) {
          Column(
            modifier = Modifier
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    MealVerseDeepTeal.copy(alpha = 0.05f),
                    Color.Transparent
                  )
                )
              )
              .padding(18.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(
                    Brush.linearGradient(
                      colors = listOf(MealVerseDeepTeal, MealVerseLightTeal)
                    )
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Fastfood,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Surface(
                  color = MealVerseDeepTeal.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "FOR FOOD LOVERS & FAMILIES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MealVerseDeepTeal,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                  )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = "Register as Customer",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MealVerseDeepTeal
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Order authentic Karahi, Dum Biryani, Zingers & Desi Nashta from verified local kitchens with live tracking.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              FeaturePill(text = "Fast Delivery", icon = Icons.Default.ElectricBolt, tint = MealVerseDeepTeal)
              FeaturePill(text = "Live Tracking", icon = Icons.Default.LocationOn, tint = MealVerseDeepTeal)
              FeaturePill(text = "Card & Wallets", icon = Icons.Default.Payments, tint = MealVerseDeepTeal)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = onNavigateToCustomerRegister,
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("welcome_customer_reg_button")
            ) {
              Text(
                text = "Get Started as Customer",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PATH 2: SHOPKEEPER REGISTRATION CARD
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = 1.5.dp,
              brush = Brush.linearGradient(
                colors = listOf(MealVerseWarmOrange.copy(alpha = 0.55f), MealVerseSaffronYellow.copy(alpha = 0.35f))
              ),
              shape = RoundedCornerShape(22.dp)
            )
            .testTag("welcome_shopkeeper_card")
        ) {
          Column(
            modifier = Modifier
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    MealVerseWarmOrange.copy(alpha = 0.05f),
                    Color.Transparent
                  )
                )
              )
              .padding(18.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(
                    Brush.linearGradient(
                      colors = listOf(MealVerseWarmOrange, Color(0xFFFF8800))
                    )
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Storefront,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Surface(
                  color = MealVerseWarmOrange.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "FOR RESTAURANTS, CAFES & HOME CHEFS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MealVerseWarmOrange,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                  )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = "Register as Shopkeeper",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MealVerseWarmOrange
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Open your digital storefront, showcase 3D menus, manage kitchen orders in real time, and get direct payouts.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              FeaturePill(text = "Free Setup", icon = Icons.Default.Store, tint = MealVerseWarmOrange)
              FeaturePill(text = "Menu Control", icon = Icons.Default.RestaurantMenu, tint = MealVerseWarmOrange)
              FeaturePill(text = "Direct Payouts", icon = Icons.Default.AccountBalanceWallet, tint = MealVerseWarmOrange)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = onNavigateToShopkeeperRegister,
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseWarmOrange),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("welcome_shopkeeper_reg_button")
            ) {
              Text(
                text = "Register My Shop / Kitchen",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isAuthenticated) {
          // Already Registered Section
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Already have an account? ",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Sign In",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MealVerseDeepTeal,
              modifier = Modifier
                .clickable { showSignInModal = true }
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .testTag("welcome_sign_in_link")
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MealVerseDeepTeal,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Customer & Admin panels require verified Google Sign-In",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          // Signed-in session summary with strictly authorized navigation
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("welcome_signed_in_card")
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.VerifiedUser,
                  contentDescription = null,
                  tint = MealVerseDeepTeal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Signed in securely",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = signedInEmail.ifBlank { "Verified Google Account" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Button(
                onClick = onOpenAuthorizedDashboard,
                colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("welcome_continue_dashboard_btn")
              ) {
                Text("Continue to My Panel", fontWeight = FontWeight.Bold)
              }

              if (isAdminAuthorized) {
                OutlinedButton(
                  onClick = onOpenAdminPanel,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("welcome_admin_panel_btn")
                ) {
                  Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MealVerseDeepTeal)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Open Admin Verification Panel", fontWeight = FontWeight.Bold)
                }
              }

              TextButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Sign Out", color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Quick Sign In Dialog
  if (showSignInModal) {
    AlertDialog(
      onDismissRequest = { showSignInModal = false },
      title = {
        Text(
          text = "Sign in to MealVerse",
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Sign in with your Google account. We'll automatically open your Customer dashboard or Shopkeeper portal based on your registered profile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          GoogleSignInButton(
            buttonText = "Sign in with Google",
            onAuthSuccess = {
              showSignInModal = false
              onExistingUserSignedIn()
            },
            onAuthError = { err ->
              errorMessage = err
              showSignInModal = false
            }
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showSignInModal = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun FeaturePill(
  text: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: Color
) {
  Surface(
    color = tint.copy(alpha = 0.09f),
    shape = RoundedCornerShape(8.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = tint
      )
    }
  }
}
