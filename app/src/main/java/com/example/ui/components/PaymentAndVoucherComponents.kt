package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.PaymentMethod
import com.example.data.model.Voucher
import com.example.data.payment.CardBrand
import com.example.data.payment.CardPaymentValidator
import com.example.ui.theme.*

fun formatOrderPaymentLabel(order: Order): String {
  return if (order.paymentMethod == PaymentMethod.CARD.name && order.cardLast4.isNotBlank()) {
    val brand = order.cardBrand.ifBlank { "CARD" }
    "$brand •••• ${order.cardLast4}"
  } else {
    order.paymentMethod
  }
}

@Composable
fun VoucherTicketCard(
  voucher: Voucher,
  isApplied: Boolean = false,
  actionLabel: String = if (isApplied) "Applied" else "Apply",
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = isMealVerseDarkTheme()
  val isCardExclusive = voucher.paymentMethodFilter.equals(PaymentMethod.CARD.name, ignoreCase = true)
  val greenAccent = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
  val accentColor = when {
    isApplied -> greenAccent
    isCardExclusive -> MaterialTheme.colorScheme.primary
    else -> MealVerseWarmOrange
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = when {
        isApplied && isDark -> Color(0xFF183320)
        isApplied -> Color(0xFFE8F5E9)
        else -> MaterialTheme.colorScheme.surface
      }
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isApplied) 3.dp else 2.dp),
    modifier = modifier
      .width(228.dp)
      .clip(RoundedCornerShape(16.dp))
      .border(
        width = if (isApplied) 1.5.dp else 1.dp,
        color = if (isApplied) greenAccent else accentColor.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp)
      )
      .clickable(onClick = onClick)
      .testTag("voucher_card_${voucher.code}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = accentColor.copy(alpha = 0.14f)
        ) {
          Text(
            text = voucher.badgeText.ifBlank { voucher.code },
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              color = accentColor
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Text(
            text = voucher.code,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = voucher.title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = voucher.description,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Min Rs. ${voucher.minOrderAmount.toInt()}",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
          shape = RoundedCornerShape(50),
          color = accentColor
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (isApplied) {
              Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
              text = actionLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }
        }
      }
    }
  }
}

@Composable
fun VirtualDebitCreditCardPreview(
  cardNumberDigits: String,
  cardHolderName: String,
  expiryMmYy: String,
  brand: CardBrand
) {
  val maskedPreview = if (cardNumberDigits.isBlank()) {
    "••••  ••••  ••••  ••••"
  } else {
    val padded = cardNumberDigits.take(16)
    val groups = padded.chunked(4)
    groups.mapIndexed { idx, grp ->
      if (idx < groups.lastIndex && padded.length > 8) "••••" else grp
    }.joinToString("  ")
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.linearGradient(
          colors = listOf(
            Color(0xFF0B3B3E),
            MealVerseDeepTeal,
            Color(0xFF1E5F63)
          )
        )
      )
      .padding(18.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Gold EMV chip graphic
        Box(
          modifier = Modifier
            .size(width = 38.dp, height = 26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
              Brush.linearGradient(
                listOf(MealVerseSaffronYellow, Color(0xFFD4A017))
              )
            )
        )
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color.White.copy(alpha = 0.16f)
        ) {
          Text(
            text = brand.displayName.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = maskedPreview,
        color = Color.White,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.4.sp
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column {
          Text(
            text = "CARD HOLDER",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = cardHolderName.trim().uppercase().ifBlank { "YOUR NAME" },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "EXPIRES",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = expiryMmYy.ifBlank { "MM/YY" },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}

@Composable
fun CardPaymentDetailsSection(
  cardHolderName: String,
  onCardHolderChange: (String) -> Unit,
  cardNumberDigits: String,
  onCardNumberDigitsChange: (String) -> Unit,
  expiryMmYy: String,
  onExpiryChange: (String) -> Unit,
  cvv: String,
  onCvvChange: (String) -> Unit,
  cardError: String?,
  enabled: Boolean
) {
  val detectedBrand = CardPaymentValidator.detectBrand(cardNumberDigits)

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, MealVerseDeepTeal.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
      .testTag("card_payment_form")
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      VirtualDebitCreditCardPreview(
        cardNumberDigits = cardNumberDigits,
        cardHolderName = cardHolderName,
        expiryMmYy = expiryMmYy,
        brand = detectedBrand
      )

      OutlinedTextField(
        value = cardHolderName,
        onValueChange = onCardHolderChange,
        enabled = enabled,
        label = { Text("Cardholder Name") },
        placeholder = { Text("e.g. Ali Raza") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_holder_input")
      )

      OutlinedTextField(
        value = CardPaymentValidator.formatCardNumber(cardNumberDigits),
        onValueChange = { raw ->
          onCardNumberDigitsChange(CardPaymentValidator.digitsOnly(raw).take(19))
        },
        enabled = enabled,
        label = { Text("Card Number") },
        placeholder = { Text("XXXX XXXX XXXX XXXX") },
        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
        trailingIcon = {
          if (cardNumberDigits.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MealVerseDeepTeal.copy(alpha = 0.12f),
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Text(
                text = detectedBrand.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = MealVerseDeepTeal
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_number_input")
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = expiryMmYy,
          onValueChange = { raw ->
            onExpiryChange(CardPaymentValidator.formatExpiryInput(raw))
          },
          enabled = enabled,
          label = { Text("Expiry (MM/YY)") },
          placeholder = { Text("08/28") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("card_expiry_input")
        )

        OutlinedTextField(
          value = cvv,
          onValueChange = { raw ->
            onCvvChange(CardPaymentValidator.digitsOnly(raw).take(4))
          },
          enabled = enabled,
          label = { Text("CVV") },
          placeholder = { Text("•••") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("card_cvv_input")
        )
      }

      AnimatedVisibility(visible = !cardError.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = cardError.orEmpty(),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp)
      ) {
        Icon(
          Icons.Default.Lock,
          contentDescription = null,
          tint = Color(0xFF2E7D32),
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Encrypted checkout • Only card brand & last 4 digits are saved",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
