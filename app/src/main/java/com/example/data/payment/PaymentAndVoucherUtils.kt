package com.example.data.payment

import com.example.data.model.PaymentMethod
import com.example.data.model.Voucher
import com.example.data.model.VoucherDiscountType
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

enum class CardBrand(val displayName: String) {
  VISA("Visa"),
  MASTERCARD("Mastercard"),
  PAYPAK("PayPak"),
  UNIONPAY("UnionPay"),
  CARD("Debit/Credit Card")
}

data class CardValidationResult(
  val isValid: Boolean,
  val brand: CardBrand,
  val last4: String,
  val errorMessage: String? = null
)

data class VoucherValidationResult(
  val isValid: Boolean,
  val discountAmount: Double,
  val finalTotal: Double,
  val errorMessage: String? = null
)

object CardPaymentValidator {

  fun digitsOnly(input: String): String = input.filter { it.isDigit() }

  fun detectBrand(rawCardNumber: String): CardBrand {
    val digits = digitsOnly(rawCardNumber)
    if (digits.isEmpty()) return CardBrand.CARD
    if (digits.startsWith("4")) return CardBrand.VISA
    if (digits.startsWith("62")) return CardBrand.UNIONPAY
    if (digits.startsWith("58") || digits.startsWith("60")) return CardBrand.PAYPAK

    val twoDigit = digits.take(2).toIntOrNull()
    val fourDigit = digits.take(4).toIntOrNull()
    if ((twoDigit != null && twoDigit in 51..55) || (fourDigit != null && fourDigit in 2221..2720)) {
      return CardBrand.MASTERCARD
    }
    return CardBrand.CARD
  }

  fun formatCardNumber(rawInput: String): String {
    val digits = digitsOnly(rawInput).take(19)
    return digits.chunked(4).joinToString(" ")
  }

  fun formatExpiryInput(rawInput: String): String {
    val digits = digitsOnly(rawInput).take(4)
    return if (digits.length > 2) {
      "${digits.substring(0, 2)}/${digits.substring(2)}"
    } else {
      digits
    }
  }

  fun passesLuhn(rawCardNumber: String): Boolean {
    val digits = digitsOnly(rawCardNumber)
    if (digits.length !in 13..19) return false
    var sum = 0
    var alternate = false
    for (i in digits.length - 1 downTo 0) {
      var n = digits[i] - '0'
      if (alternate) {
        n *= 2
        if (n > 9) n -= 9
      }
      sum += n
      alternate = !alternate
    }
    return sum % 10 == 0
  }

  fun validateCard(
    cardNumber: String,
    cardHolderName: String,
    expiryMmYy: String,
    cvv: String,
    currentMonth: Int? = null,
    currentYearTwoDigits: Int? = null
  ): CardValidationResult {
    val cleanDigits = digitsOnly(cardNumber)
    val brand = detectBrand(cleanDigits)

    if (cardHolderName.trim().length < 2) {
      return CardValidationResult(false, brand, "", "Enter the cardholder name shown on your card.")
    }
    if (cleanDigits.length !in 13..19) {
      return CardValidationResult(false, brand, "", "Enter a valid 13–19 digit card number.")
    }
    if (!passesLuhn(cleanDigits)) {
      return CardValidationResult(false, brand, "", "Card number failed validation check. Please verify digits.")
    }

    val expiryParts = expiryMmYy.trim().split("/")
    val expMonth = expiryParts.getOrNull(0)?.toIntOrNull()
    val expYear = expiryParts.getOrNull(1)?.toIntOrNull()
    if (expiryParts.size != 2 || expMonth == null || expYear == null || expMonth !in 1..12 || expiryParts[1].length != 2) {
      return CardValidationResult(false, brand, "", "Enter expiry date in MM/YY format.")
    }

    val cal = Calendar.getInstance()
    val nowMonth = currentMonth ?: (cal.get(Calendar.MONTH) + 1)
    val nowYear = currentYearTwoDigits ?: (cal.get(Calendar.YEAR) % 100)
    if (expYear < nowYear || (expYear == nowYear && expMonth < nowMonth)) {
      return CardValidationResult(false, brand, "", "This card appears to be expired.")
    }

    val cleanCvv = digitsOnly(cvv)
    if (cleanCvv.length !in 3..4) {
      return CardValidationResult(false, brand, "", "Enter a valid 3 or 4 digit CVV security code.")
    }

    return CardValidationResult(
      isValid = true,
      brand = brand,
      last4 = cleanDigits.takeLast(4),
      errorMessage = null
    )
  }
}

object VoucherCalculator {

  val DEFAULT_VOUCHERS: List<Voucher> = listOf(
    Voucher(
      id = "MEAL100",
      code = "MEAL100",
      title = "Rs. 100 Off Feast",
      description = "Instant Rs. 100 savings on orders Rs. 400 or above.",
      discountType = VoucherDiscountType.FLAT.name,
      discountValue = 100.0,
      maxDiscount = 100.0,
      minOrderAmount = 400.0,
      city = "All",
      paymentMethodFilter = "",
      isActive = true,
      usageLimit = 5000,
      usedCount = 0,
      badgeText = "Rs. 100 OFF"
    ),
    Voucher(
      id = "ZAIQA20",
      code = "ZAIQA20",
      title = "20% Off Desi Classics",
      description = "Enjoy 20% off up to Rs. 250 on orders above Rs. 500.",
      discountType = VoucherDiscountType.PERCENTAGE.name,
      discountValue = 20.0,
      maxDiscount = 250.0,
      minOrderAmount = 500.0,
      city = "All",
      paymentMethodFilter = "",
      isActive = true,
      usageLimit = 5000,
      usedCount = 0,
      badgeText = "20% OFF"
    ),
    Voucher(
      id = "CARD120",
      code = "CARD120",
      title = "Card Special Rs. 120 Off",
      description = "Pay by Debit/Credit Card on Rs. 500+ orders and save Rs. 120.",
      discountType = VoucherDiscountType.FLAT.name,
      discountValue = 120.0,
      maxDiscount = 120.0,
      minOrderAmount = 500.0,
      city = "All",
      paymentMethodFilter = PaymentMethod.CARD.name,
      isActive = true,
      usageLimit = 5000,
      usedCount = 0,
      badgeText = "CARD DEAL"
    ),
    Voucher(
      id = "WELCOME150",
      code = "WELCOME150",
      title = "Welcome Bonus Rs. 150",
      description = "Save Rs. 150 on family feasts of Rs. 750 or more.",
      discountType = VoucherDiscountType.FLAT.name,
      discountValue = 150.0,
      maxDiscount = 150.0,
      minOrderAmount = 750.0,
      city = "All",
      paymentMethodFilter = "",
      isActive = true,
      usageLimit = 5000,
      usedCount = 0,
      badgeText = "Rs. 150 OFF"
    )
  )

  fun normalizeCode(rawCode: String): String = rawCode.trim().uppercase(Locale.US)

  fun validateAndCalculate(
    voucher: Voucher,
    subtotal: Double,
    deliveryFee: Double,
    platformFee: Double = 20.0,
    paymentMethod: PaymentMethod,
    storeCity: String = "All"
  ): VoucherValidationResult {
    val baseTotal = subtotal + deliveryFee + platformFee

    if (!voucher.isActive) {
      return VoucherValidationResult(false, 0.0, baseTotal, "Voucher ${voucher.code} is no longer active.")
    }
    if (voucher.usageLimit > 0 && voucher.usedCount >= voucher.usageLimit) {
      return VoucherValidationResult(false, 0.0, baseTotal, "Voucher ${voucher.code} has reached its redemption limit.")
    }
    if (voucher.city.isNotBlank() && !voucher.city.equals("All", ignoreCase = true) &&
      storeCity.isNotBlank() && !voucher.city.equals(storeCity, ignoreCase = true)
    ) {
      return VoucherValidationResult(false, 0.0, baseTotal, "Voucher ${voucher.code} is valid only in ${voucher.city}.")
    }
    if (subtotal < voucher.minOrderAmount) {
      val remaining = (voucher.minOrderAmount - subtotal).roundToInt()
      return VoucherValidationResult(
        false,
        0.0,
        baseTotal,
        "Add Rs. $remaining more to reach Rs. ${voucher.minOrderAmount.toInt()} minimum for ${voucher.code}."
      )
    }
    if (voucher.paymentMethodFilter.isNotBlank() &&
      !voucher.paymentMethodFilter.equals(paymentMethod.name, ignoreCase = true)
    ) {
      val reqLabel = if (voucher.paymentMethodFilter.equals(PaymentMethod.CARD.name, ignoreCase = true)) {
        "Debit/Credit Card"
      } else {
        voucher.paymentMethodFilter
      }
      return VoucherValidationResult(
        false,
        0.0,
        baseTotal,
        "Voucher ${voucher.code} requires $reqLabel payment."
      )
    }

    val rawDiscount = when (voucher.discountType.uppercase(Locale.US)) {
      VoucherDiscountType.PERCENTAGE.name -> {
        val pct = subtotal * (voucher.discountValue / 100.0)
        if (voucher.maxDiscount > 0.0) min(pct, voucher.maxDiscount) else pct
      }
      else -> voucher.discountValue
    }

    val clampedDiscount = min(maxOf(0.0, rawDiscount.roundToInt().toDouble()), subtotal)
    val finalTotal = maxOf(0.0, subtotal - clampedDiscount) + deliveryFee + platformFee

    return VoucherValidationResult(
      isValid = clampedDiscount > 0.0,
      discountAmount = clampedDiscount,
      finalTotal = finalTotal,
      errorMessage = if (clampedDiscount > 0.0) null else "Voucher cannot be applied to an empty order."
    )
  }
}
