package com.example

import com.example.data.model.Order
import com.example.data.model.PaymentMethod
import com.example.data.model.Voucher
import com.example.data.model.VoucherDiscountType
import com.example.data.payment.CardBrand
import com.example.data.payment.CardPaymentValidator
import com.example.data.payment.VoucherCalculator
import com.example.ui.components.formatOrderPaymentLabel
import org.junit.Assert.*
import org.junit.Test

class PaymentAndVoucherTest {

  @Test
  fun `card brand detection identifies Visa Mastercard PayPak and UnionPay`() {
    assertEquals(CardBrand.VISA, CardPaymentValidator.detectBrand("4111111111111111"))
    assertEquals(CardBrand.MASTERCARD, CardPaymentValidator.detectBrand("5500000000000004"))
    assertEquals(CardBrand.MASTERCARD, CardPaymentValidator.detectBrand("2221000000000009"))
    assertEquals(CardBrand.PAYPAK, CardPaymentValidator.detectBrand("6061000000000003"))
    assertEquals(CardBrand.UNIONPAY, CardPaymentValidator.detectBrand("6221260000000001"))
  }

  @Test
  fun `validateCard succeeds for valid Luhn card and extracts only last4`() {
    val result = CardPaymentValidator.validateCard(
      cardNumber = "4111 1111 1111 1111",
      cardHolderName = "Ali Raza",
      expiryMmYy = "12/29",
      cvv = "345",
      currentMonth = 11,
      currentYearTwoDigits = 26
    )
    assertTrue(result.isValid)
    assertEquals(CardBrand.VISA, result.brand)
    assertEquals("1111", result.last4)
    assertNull(result.errorMessage)
  }

  @Test
  fun `validateCard rejects invalid Luhn expired card and short CVV`() {
    val badLuhn = CardPaymentValidator.validateCard(
      cardNumber = "4111 1111 1111 1112",
      cardHolderName = "Ali Raza",
      expiryMmYy = "12/29",
      cvv = "345",
      currentMonth = 11,
      currentYearTwoDigits = 26
    )
    assertFalse(badLuhn.isValid)
    assertNotNull(badLuhn.errorMessage)

    val expired = CardPaymentValidator.validateCard(
      cardNumber = "4111111111111111",
      cardHolderName = "Ali Raza",
      expiryMmYy = "10/26",
      cvv = "345",
      currentMonth = 11,
      currentYearTwoDigits = 26
    )
    assertFalse(expired.isValid)
    assertTrue(expired.errorMessage!!.contains("expired", ignoreCase = true))

    val badCvv = CardPaymentValidator.validateCard(
      cardNumber = "4111111111111111",
      cardHolderName = "Ali Raza",
      expiryMmYy = "12/29",
      cvv = "12",
      currentMonth = 11,
      currentYearTwoDigits = 26
    )
    assertFalse(badCvv.isValid)
  }

  @Test
  fun `voucher calculator applies flat and capped percentage discounts accurately`() {
    val meal100 = VoucherCalculator.DEFAULT_VOUCHERS.first { it.code == "MEAL100" }
    val flatRes = VoucherCalculator.validateAndCalculate(
      voucher = meal100,
      subtotal = 600.0,
      deliveryFee = 80.0,
      platformFee = 20.0,
      paymentMethod = PaymentMethod.COD
    )
    assertTrue(flatRes.isValid)
    assertEquals(100.0, flatRes.discountAmount, 0.01)
    assertEquals(600.0, flatRes.finalTotal, 0.01)

    val zaiqa20 = VoucherCalculator.DEFAULT_VOUCHERS.first { it.code == "ZAIQA20" }
    val pctRes = VoucherCalculator.validateAndCalculate(
      voucher = zaiqa20,
      subtotal = 2000.0,
      deliveryFee = 100.0,
      platformFee = 20.0,
      paymentMethod = PaymentMethod.CARD
    )
    assertTrue(pctRes.isValid)
    assertEquals(250.0, pctRes.discountAmount, 0.01) // Capped at maxDiscount=250
    assertEquals(1870.0, pctRes.finalTotal, 0.01)
  }

  @Test
  fun `voucher calculator enforces minOrder paymentMethodFilter and active limits`() {
    val card120 = VoucherCalculator.DEFAULT_VOUCHERS.first { it.code == "CARD120" }

    val codAttempt = VoucherCalculator.validateAndCalculate(
      voucher = card120,
      subtotal = 700.0,
      deliveryFee = 80.0,
      platformFee = 20.0,
      paymentMethod = PaymentMethod.COD
    )
    assertFalse(codAttempt.isValid)
    assertTrue(codAttempt.errorMessage!!.contains("Card", ignoreCase = true))

    val cardAttempt = VoucherCalculator.validateAndCalculate(
      voucher = card120,
      subtotal = 700.0,
      deliveryFee = 80.0,
      platformFee = 20.0,
      paymentMethod = PaymentMethod.CARD
    )
    assertTrue(cardAttempt.isValid)
    assertEquals(120.0, cardAttempt.discountAmount, 0.01)

    val lowSubtotal = VoucherCalculator.validateAndCalculate(
      voucher = card120,
      subtotal = 350.0,
      deliveryFee = 80.0,
      platformFee = 20.0,
      paymentMethod = PaymentMethod.CARD
    )
    assertFalse(lowSubtotal.isValid)
    assertTrue(lowSubtotal.errorMessage!!.contains("minimum", ignoreCase = true))

    val exhausted = Voucher(
      code = "EXHAUST",
      title = "Old Deal",
      discountType = VoucherDiscountType.FLAT.name,
      discountValue = 50.0,
      minOrderAmount = 100.0,
      isActive = true,
      usageLimit = 10,
      usedCount = 10
    )
    assertFalse(
      VoucherCalculator.validateAndCalculate(
        voucher = exhausted,
        subtotal = 300.0,
        deliveryFee = 50.0,
        paymentMethod = PaymentMethod.COD
      ).isValid
    )
  }

  @Test
  fun `formatOrderPaymentLabel displays masked card brand and last4 safely`() {
    val cardOrder = Order(paymentMethod = PaymentMethod.CARD.name, cardBrand = "VISA", cardLast4 = "4242")
    assertEquals("VISA •••• 4242", formatOrderPaymentLabel(cardOrder))

    val codOrder = Order(paymentMethod = PaymentMethod.COD.name)
    assertEquals("COD", formatOrderPaymentLabel(codOrder))
  }
}
