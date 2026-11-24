package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.payment.CardPaymentValidator
import com.example.data.payment.VoucherCalculator
import com.example.data.repository.MealVerseRepository
import com.example.ui.components.Animated3DFoodThumbnail
import com.example.ui.components.CardPaymentDetailsSection
import com.example.ui.components.VoucherTicketCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartCheckoutScreen(
  cartItems: List<CartItem>,
  currentStore: Store?,
  currentProfile: UserProfile,
  repository: MealVerseRepository,
  preselectedVoucherCode: String? = null,
  onConsumePreselectedVoucher: () -> Unit = {},
  onUpdateQuantity: (Product, Int) -> Unit,
  onClearCart: () -> Unit,
  onOrderPlaced: (String) -> Unit,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var deliveryAddress by remember {
    mutableStateOf(
      currentProfile.address.ifBlank {
        "${currentProfile.houseFlat}, ${currentProfile.landmark}, ${currentProfile.city}".trim(',', ' ')
      }
    )
  }
  var specialInstructions by remember { mutableStateOf("") }
  var selectedPayment by remember { mutableStateOf(PaymentMethod.COD) }
  var mobileWalletNumber by remember { mutableStateOf(currentProfile.phoneNumber) }

  // Card payment fields (never persisted raw to Firestore; only brand + last4 are saved)
  var cardHolderName by remember { mutableStateOf(currentProfile.fullName) }
  var cardNumberDigits by remember { mutableStateOf("") }
  var cardExpiry by remember { mutableStateOf("") }
  var cardCvv by remember { mutableStateOf("") }
  var cardValidationError by remember { mutableStateOf<String?>(null) }

  // Voucher state
  val firestoreVouchers by remember(repository) {
    repository.observeActiveVouchers()
  }.collectAsStateWithLifecycle(initialValue = emptyList())

  LaunchedEffect(Unit) {
    repository.ensureVouchersSeeded()
  }

  val availableVouchers = remember(firestoreVouchers) {
    firestoreVouchers.ifEmpty { VoucherCalculator.DEFAULT_VOUCHERS }
  }
  var voucherInput by remember { mutableStateOf("") }
  var appliedVoucher by remember { mutableStateOf<Voucher?>(null) }
  var voucherFeedback by remember { mutableStateOf<String?>(null) }
  var isApplyingVoucher by remember { mutableStateOf(false) }

  var isSubmitting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val subtotal = cartItems.sumOf { it.product.price * it.quantity }
  val deliveryFee = currentStore?.deliveryFee ?: 99.0
  val platformFee = 20.0
  val storeCity = currentStore?.city ?: currentProfile.city

  fun applyVoucherObject(voucher: Voucher) {
    val result = VoucherCalculator.validateAndCalculate(
      voucher = voucher,
      subtotal = subtotal,
      deliveryFee = deliveryFee,
      platformFee = platformFee,
      paymentMethod = selectedPayment,
      storeCity = storeCity
    )
    if (result.isValid) {
      appliedVoucher = voucher
      voucherInput = voucher.code
      voucherFeedback = null
    } else {
      voucherFeedback = result.errorMessage
    }
  }

  // Apply preselected voucher forwarded from Home / Profile deals
  LaunchedEffect(preselectedVoucherCode, availableVouchers) {
    val code = preselectedVoucherCode?.let { VoucherCalculator.normalizeCode(it) } ?: ""
    if (code.isNotBlank() && cartItems.isNotEmpty()) {
      voucherInput = code
      val matched = availableVouchers.firstOrNull { it.code.equals(code, ignoreCase = true) }
        ?: repository.findVoucherByCode(code)
      if (matched != null) {
        if (matched.paymentMethodFilter.equals(PaymentMethod.CARD.name, ignoreCase = true)) {
          selectedPayment = PaymentMethod.CARD
        }
        val check = VoucherCalculator.validateAndCalculate(
          voucher = matched,
          subtotal = subtotal,
          deliveryFee = deliveryFee,
          platformFee = platformFee,
          paymentMethod = if (matched.paymentMethodFilter.equals(PaymentMethod.CARD.name, ignoreCase = true)) {
            PaymentMethod.CARD
          } else {
            selectedPayment
          },
          storeCity = storeCity
        )
        if (check.isValid) {
          appliedVoucher = matched
          voucherFeedback = null
        } else {
          voucherFeedback = check.errorMessage
        }
      }
      onConsumePreselectedVoucher()
    }
  }

  // Re-evaluate applied voucher whenever subtotal or payment method changes
  val activeVoucherResult = remember(appliedVoucher, subtotal, deliveryFee, selectedPayment, storeCity) {
    val current = appliedVoucher ?: return@remember null
    VoucherCalculator.validateAndCalculate(
      voucher = current,
      subtotal = subtotal,
      deliveryFee = deliveryFee,
      platformFee = platformFee,
      paymentMethod = selectedPayment,
      storeCity = storeCity
    )
  }

  LaunchedEffect(activeVoucherResult) {
    if (appliedVoucher != null && activeVoucherResult != null && !activeVoucherResult.isValid) {
      voucherFeedback = activeVoucherResult.errorMessage
      appliedVoucher = null
    }
  }

  val discountAmount = if (activeVoucherResult?.isValid == true) activeVoucherResult.discountAmount else 0.0
  val totalAmount = maxOf(0.0, subtotal - discountAmount) + deliveryFee + platformFee

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Checkout & Review", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack, enabled = !isSubmitting) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (cartItems.isNotEmpty()) {
            TextButton(onClick = onClearCart, enabled = !isSubmitting) {
              Text("Clear", color = MaterialTheme.colorScheme.error)
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      if (cartItems.isNotEmpty()) {
        Surface(
          shadowElevation = 10.dp,
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Total Amount to Pay", style = MaterialTheme.typography.titleMedium)
                if (discountAmount > 0.0) {
                  Text(
                    "Saved Rs. ${discountAmount.toInt()} with ${appliedVoucher?.code}",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color(0xFF2E7D32),
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }
              Text(
                "Rs. ${totalAmount.toInt()}",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = MealVerseDeepTeal
                )
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                errorMessage = null
                cardValidationError = null

                if (deliveryAddress.isBlank()) {
                  errorMessage = "Please enter delivery address."
                  return@Button
                }
                if (currentStore == null) {
                  errorMessage = "Store information missing."
                  return@Button
                }
                if ((selectedPayment == PaymentMethod.EASYPAISA || selectedPayment == PaymentMethod.JAZZCASH) &&
                  mobileWalletNumber.filter { it.isDigit() }.length < 10
                ) {
                  errorMessage = "Please enter a valid ${selectedPayment.name} mobile wallet number."
                  return@Button
                }

                var validatedCardBrand = ""
                var validatedCardLast4 = ""
                if (selectedPayment == PaymentMethod.CARD) {
                  val cardCheck = CardPaymentValidator.validateCard(
                    cardNumber = cardNumberDigits,
                    cardHolderName = cardHolderName,
                    expiryMmYy = cardExpiry,
                    cvv = cardCvv
                  )
                  if (!cardCheck.isValid) {
                    cardValidationError = cardCheck.errorMessage
                    errorMessage = cardCheck.errorMessage
                    return@Button
                  }
                  validatedCardBrand = cardCheck.brand.name
                  validatedCardLast4 = cardCheck.last4
                }

                scope.launch {
                  isSubmitting = true
                  try {
                    val orderItems = cartItems.map {
                      OrderItemRecord(
                        productId = it.product.id,
                        productName = it.product.name,
                        price = it.product.price,
                        quantity = it.quantity,
                        instructions = it.specialInstructions
                      )
                    }

                    val newOrder = Order(
                      customerId = currentProfile.userId,
                      customerName = currentProfile.fullName,
                      customerPhone = currentProfile.phoneNumber,
                      storeId = currentStore.id,
                      storeName = currentStore.name,
                      storeOwnerId = currentStore.ownerId,
                      city = currentStore.city,
                      deliveryAddress = deliveryAddress,
                      items = orderItems,
                      subtotal = subtotal,
                      deliveryFee = deliveryFee,
                      discountAmount = discountAmount,
                      voucherCode = if (discountAmount > 0.0) appliedVoucher?.code.orEmpty() else "",
                      voucherTitle = if (discountAmount > 0.0) appliedVoucher?.title.orEmpty() else "",
                      totalAmount = totalAmount,
                      paymentMethod = selectedPayment.name,
                      paymentStatus = if (selectedPayment == PaymentMethod.COD) "PENDING" else "SUCCESS",
                      cardBrand = validatedCardBrand,
                      cardLast4 = validatedCardLast4,
                      status = OrderStatus.NEW.name,
                      specialInstructions = specialInstructions
                    )

                    val orderId = repository.placeOrder(newOrder)
                    onClearCart()
                    isSubmitting = false
                    onOrderPlaced(orderId)
                  } catch (e: Exception) {
                    isSubmitting = false
                    errorMessage = e.localizedMessage ?: "Failed to place order"
                  }
                }
              },
              enabled = !isSubmitting,
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseWarmOrange),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_order_button")
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Placing Order...", fontWeight = FontWeight.Bold, color = Color.White)
              } else {
                Text(
                  "Place Order • Rs. ${totalAmount.toInt()}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
              }
            }
          }
        }
      }
    }
  ) { padding ->
    if (cartItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.RemoveShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MealVerseWarmOrange.copy(alpha = 0.7f)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            "Your cart is empty",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Explore verified restaurants and add your favorite Pakistani dishes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
          ) {
            Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Browse Restaurants", fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
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
            .padding(horizontal = 16.dp),
          contentPadding = PaddingValues(vertical = 12.dp)
        ) {
          if (errorMessage != null) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 12.dp)
              ) {
                Text(
                  text = errorMessage ?: "",
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.padding(12.dp)
                )
              }
            }
          }

          // Store Header
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Animated3DFoodThumbnail(
                  imageRes = get3DFoodGraphic(currentStore?.name ?: "", currentStore?.category ?: ""),
                  contentDescription = currentStore?.name ?: "Restaurant",
                  size = 48.dp,
                  cornerRadius = 12.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    currentStore?.name ?: "Restaurant",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                  )
                  Text(
                    "${currentStore?.category} • ${currentStore?.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          // Items Section
          item {
            Text(
              "Ordered Items",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(bottom = 8.dp)
            )
          }

          items(cartItems) { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, fontWeight = FontWeight.SemiBold)
                Text("Rs. ${item.product.price.toInt()} each", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.specialInstructions.isNotBlank()) {
                  Text("Note: ${item.specialInstructions}", style = MaterialTheme.typography.bodySmall, color = MealVerseWarmOrange)
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { onUpdateQuantity(item.product, item.quantity - 1) },
                  enabled = !isSubmitting,
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = MealVerseDeepTeal)
                }
                Text("${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(
                  onClick = { onUpdateQuantity(item.product, item.quantity + 1) },
                  enabled = !isSubmitting,
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.AddCircle, contentDescription = "Increase", tint = MealVerseDeepTeal)
                }
              }

              Spacer(modifier = Modifier.width(12.dp))
              Text(
                "Rs. ${(item.product.price * item.quantity).toInt()}",
                fontWeight = FontWeight.Bold,
                color = MealVerseDeepTeal
              )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
          }

          // Delivery Address & Instructions
          item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              "Delivery Address",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
              value = deliveryAddress,
              onValueChange = { deliveryAddress = it },
              enabled = !isSubmitting,
              label = { Text("Complete Address") },
              leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = specialInstructions,
              onValueChange = { specialInstructions = it },
              enabled = !isSubmitting,
              label = { Text("Delivery Instructions (Optional)") },
              placeholder = { Text("e.g. Ring bell twice, leave at gate") },
              leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            )
          }

          // Vouchers & Promo Code Section
          item {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
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
                  "Vouchers & Promo Code",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
              }
              if (appliedVoucher != null) {
                Text(
                  text = "Saved Rs. ${discountAmount.toInt()}",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32)
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = voucherInput,
                onValueChange = {
                  voucherInput = it.uppercase()
                  voucherFeedback = null
                },
                enabled = !isSubmitting && !isApplyingVoucher,
                placeholder = { Text("Enter code (e.g. MEAL100)") },
                leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MealVerseWarmOrange) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("voucher_code_input")
              )

              Button(
                onClick = {
                  val code = VoucherCalculator.normalizeCode(voucherInput)
                  if (code.isBlank()) {
                    voucherFeedback = "Enter a voucher code to apply."
                    return@Button
                  }
                  scope.launch {
                    isApplyingVoucher = true
                    val fromLocal = availableVouchers.firstOrNull { it.code.equals(code, ignoreCase = true) }
                    val found = fromLocal ?: repository.findVoucherByCode(code)
                    isApplyingVoucher = false
                    if (found == null) {
                      voucherFeedback = "Voucher \"$code\" was not found or has expired."
                    } else {
                      applyVoucherObject(found)
                    }
                  }
                },
                enabled = !isSubmitting && !isApplyingVoucher,
                colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .height(54.dp)
                  .testTag("apply_voucher_button")
              ) {
                if (isApplyingVoucher) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                  Text("Apply", fontWeight = FontWeight.Bold)
                }
              }
            }

            AnimatedVisibility(visible = appliedVoucher != null && discountAmount > 0.0) {
              val isDark = isMealVerseDarkTheme()
              val bannerBg = if (isDark) Color(0xFF173620) else Color(0xFFE8F5E9)
              val bannerPrimaryFg = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
              val bannerSecondaryFg = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = bannerBg,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp)
                  .testTag("applied_voucher_banner")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = bannerSecondaryFg, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${appliedVoucher?.code} applied • Rs. ${discountAmount.toInt()} off",
                      style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = bannerPrimaryFg
                      )
                    )
                    Text(
                      text = appliedVoucher?.title.orEmpty(),
                      style = MaterialTheme.typography.labelSmall,
                      color = bannerSecondaryFg
                    )
                  }
                  TextButton(
                    onClick = {
                      appliedVoucher = null
                      voucherFeedback = null
                    },
                    modifier = Modifier.testTag("remove_voucher_button")
                  ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            AnimatedVisibility(visible = !voucherFeedback.isNullOrBlank()) {
              Text(
                text = voucherFeedback.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Available MealVerse Vouchers (Tap to Apply)",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(availableVouchers, key = { it.code }) { voucher ->
                val isApplied = appliedVoucher?.code.equals(voucher.code, ignoreCase = true)
                VoucherTicketCard(
                  voucher = voucher,
                  isApplied = isApplied,
                  onClick = {
                    if (!isSubmitting) {
                      if (isApplied) {
                        appliedVoucher = null
                        voucherFeedback = null
                      } else {
                        if (voucher.paymentMethodFilter.equals(PaymentMethod.CARD.name, ignoreCase = true) &&
                          selectedPayment != PaymentMethod.CARD
                        ) {
                          selectedPayment = PaymentMethod.CARD
                          voucherFeedback = null
                        }
                        val targetMethod = if (voucher.paymentMethodFilter.equals(PaymentMethod.CARD.name, ignoreCase = true)) {
                          PaymentMethod.CARD
                        } else {
                          selectedPayment
                        }
                        val check = VoucherCalculator.validateAndCalculate(
                          voucher = voucher,
                          subtotal = subtotal,
                          deliveryFee = deliveryFee,
                          platformFee = platformFee,
                          paymentMethod = targetMethod,
                          storeCity = storeCity
                        )
                        if (check.isValid) {
                          appliedVoucher = voucher
                          voucherInput = voucher.code
                          voucherFeedback = null
                        } else {
                          voucherFeedback = check.errorMessage
                        }
                      }
                    }
                  }
                )
              }
            }
          }

          // Payment Method Section
          item {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              "Payment Method",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              PaymentOptionCard(
                title = "Cash on Delivery (COD)",
                subtitle = "Pay rider in cash upon food arrival",
                icon = Icons.Default.Payments,
                selected = selectedPayment == PaymentMethod.COD,
                onClick = { if (!isSubmitting) selectedPayment = PaymentMethod.COD },
                modifier = Modifier.testTag("payment_option_cod")
              )
              PaymentOptionCard(
                title = "Debit / Credit Card",
                subtitle = "Visa, Mastercard, PayPak & UnionPay • Instant Verification",
                icon = Icons.Default.CreditCard,
                badgeText = "NEW",
                selected = selectedPayment == PaymentMethod.CARD,
                onClick = { if (!isSubmitting) selectedPayment = PaymentMethod.CARD },
                modifier = Modifier.testTag("payment_option_card")
              )
              PaymentOptionCard(
                title = "Easypaisa Wallet",
                subtitle = "Secure direct payment via Pakistani mobile number",
                icon = Icons.Default.PhoneAndroid,
                selected = selectedPayment == PaymentMethod.EASYPAISA,
                onClick = { if (!isSubmitting) selectedPayment = PaymentMethod.EASYPAISA },
                modifier = Modifier.testTag("payment_option_easypaisa")
              )
              PaymentOptionCard(
                title = "JazzCash Wallet",
                subtitle = "Pay directly with your registered JazzCash account",
                icon = Icons.Default.AccountBalanceWallet,
                selected = selectedPayment == PaymentMethod.JAZZCASH,
                onClick = { if (!isSubmitting) selectedPayment = PaymentMethod.JAZZCASH },
                modifier = Modifier.testTag("payment_option_jazzcash")
              )
            }
          }

          // Card Payment Form when CARD selected
          if (selectedPayment == PaymentMethod.CARD) {
            item {
              Spacer(modifier = Modifier.height(12.dp))
              CardPaymentDetailsSection(
                cardHolderName = cardHolderName,
                onCardHolderChange = {
                  cardHolderName = it
                  cardValidationError = null
                },
                cardNumberDigits = cardNumberDigits,
                onCardNumberDigitsChange = {
                  cardNumberDigits = it
                  cardValidationError = null
                },
                expiryMmYy = cardExpiry,
                onExpiryChange = {
                  cardExpiry = it
                  cardValidationError = null
                },
                cvv = cardCvv,
                onCvvChange = {
                  cardCvv = it
                  cardValidationError = null
                },
                cardError = cardValidationError,
                enabled = !isSubmitting
              )
            }
          }

          // Mobile wallet number if Easypaisa / JazzCash selected
          if (selectedPayment == PaymentMethod.EASYPAISA || selectedPayment == PaymentMethod.JAZZCASH) {
            item {
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedTextField(
                value = mobileWalletNumber,
                onValueChange = { mobileWalletNumber = it },
                enabled = !isSubmitting,
                label = { Text("${selectedPayment.name} Mobile Number") },
                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }

          // Bill Summary Breakdown
          item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              "Bill Summary",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                  Text("Rs. ${subtotal.toInt()}", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Delivery Fee", style = MaterialTheme.typography.bodyMedium)
                  Text("Rs. ${deliveryFee.toInt()}", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Platform Service Fee", style = MaterialTheme.typography.bodyMedium)
                  Text("Rs. ${platformFee.toInt()}", fontWeight = FontWeight.SemiBold)
                }
                if (discountAmount > 0.0) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                      "Voucher (${appliedVoucher?.code})",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = Color(0xFF2E7D32)
                    )
                    Text(
                      "- Rs. ${discountAmount.toInt()}",
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF2E7D32)
                    )
                  }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Total (PKR)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                  Text(
                    "Rs. ${totalAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MealVerseDeepTeal)
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun PaymentOptionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  badgeText: String? = null,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (selected) MealVerseDeepTeal.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
    ),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(
        width = if (selected) 1.5.dp else 1.dp,
        color = if (selected) MealVerseDeepTeal else MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(14.dp)
      )
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      RadioButton(
        selected = selected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(selectedColor = MealVerseDeepTeal)
      )
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) MealVerseDeepTeal else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(title, fontWeight = FontWeight.Bold)
          if (!badgeText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MealVerseWarmOrange.copy(alpha = 0.16f)
            ) {
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = MealVerseWarmOrange
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
