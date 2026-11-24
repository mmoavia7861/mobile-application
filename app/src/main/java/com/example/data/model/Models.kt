package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
  CUSTOMER,
  MERCHANT,
  SHOPKEEPER,
  RIDER,
  ADMIN
}

enum class VerificationStatus {
  PENDING,
  VERIFIED,
  REJECTED,
  SUSPENDED
}

enum class StoreStatus {
  PENDING,
  APPROVED,
  REJECTED,
  SUSPENDED
}

enum class OrderStatus {
  NEW,
  ACCEPTED,
  PREPARING,
  READY,
  ON_THE_WAY,
  DELIVERED,
  REJECTED,
  CANCELLED
}

enum class PaymentMethod {
  COD,
  EASYPAISA,
  JAZZCASH,
  CARD
}

enum class VoucherDiscountType {
  FLAT,
  PERCENTAGE
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val businessName: String = "",
  val email: String = "",
  val phoneNumber: String = "",
  val role: String = UserRole.CUSTOMER.name,
  val city: String = "Islamabad",
  val address: String = "",
  val landmark: String = "",
  val houseFlat: String = "",
  val streetSector: String = "",
  val merchantId: String = "",
  val category: String = "",
  val cnicNumber: String = "",
  val status: String = "Pending",
  val latitude: Double? = null,
  val longitude: Double? = null,
  val fcmToken: String = "",
  val notificationsEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Merchant(
  @DocumentId val id: String = "",
  val ownerUserId: String = "",
  val businessName: String = "",
  val ownerName: String = "",
  val contactEmail: String = "",
  val contactPhone: String = "",
  val cnicNumber: String = "",
  val category: String = "Pakistani Food",
  val ntnNumber: String = "",
  val bankAccountTitle: String = "",
  val bankName: String = "",
  val ibanNumber: String = "",
  val verificationStatus: String = "Pending",
  val status: String = "Pending",
  val commissionRate: Double = 15.0,
  val storeCount: Long = 1,
  val rejectionReason: String = "",
  val latitude: Double? = null,
  val longitude: Double? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Category(
  @DocumentId val id: String = "",
  val name: String = "",
  val slug: String = "",
  val description: String = "",
  val icon: String = "",
  val imageUrl: String = "",
  val displayOrder: Long = 0,
  val isActive: Boolean = true,
  val isFeatured: Boolean = false,
  val itemCount: Long = 0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Store(
  @DocumentId val id: String = "",
  val merchantId: String = "",
  val ownerId: String = "",
  val name: String = "",
  val slug: String = "",
  val category: String = "Pakistani Food",
  val categoryIds: List<String> = emptyList(),
  val description: String = "",
  val city: String = "Islamabad",
  val address: String = "",
  val phone: String = "",
  val openingTime: String = "11:00 AM",
  val closingTime: String = "11:00 PM",
  val minOrder: Double = 300.0,
  val deliveryFee: Double = 99.0,
  val prepTimeMinutes: Long = 25,
  val rating: Double = 4.8,
  val reviewCount: Long = 120,
  val status: String = "Pending",
  val isOpen: Boolean = true,
  val imageUrl: String = "",
  val bannerUrl: String = "",
  val cnicNumber: String = "",
  val bankAccountTitle: String = "",
  val rejectionReason: String = "",
  val latitude: Double? = null,
  val longitude: Double? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val name: String = "",
  val description: String = "",
  val price: Double = 0.0,
  val category: String = "Main Course",
  val isAvailable: Boolean = true,
  val imageUrl: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
  val product: Product,
  val quantity: Int,
  val specialInstructions: String = ""
)

@IgnoreExtraProperties
data class OrderItemRecord(
  val productId: String = "",
  val productName: String = "",
  val price: Double = 0.0,
  val quantity: Int = 1,
  val instructions: String = ""
)

@IgnoreExtraProperties
data class Order(
  @DocumentId val id: String = "",
  val customerId: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val storeId: String = "",
  val storeName: String = "",
  val storeOwnerId: String = "",
  val city: String = "Islamabad",
  val deliveryAddress: String = "",
  val items: List<OrderItemRecord> = emptyList(),
  val subtotal: Double = 0.0,
  val deliveryFee: Double = 99.0,
  val discountAmount: Double = 0.0,
  val voucherCode: String = "",
  val voucherTitle: String = "",
  val totalAmount: Double = 0.0,
  val paymentMethod: String = PaymentMethod.COD.name,
  val paymentStatus: String = "PENDING",
  val cardBrand: String = "",
  val cardLast4: String = "",
  val status: String = OrderStatus.NEW.name,
  val riderId: String = "",
  val riderName: String = "",
  val specialInstructions: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Voucher(
  @DocumentId val id: String = "",
  val code: String = "",
  val title: String = "",
  val description: String = "",
  val discountType: String = VoucherDiscountType.FLAT.name,
  val discountValue: Double = 0.0,
  val maxDiscount: Double = 0.0,
  val minOrderAmount: Double = 0.0,
  val city: String = "All",
  val paymentMethodFilter: String = "",
  @get:PropertyName("isActive")
  @PropertyName("isActive")
  val isActive: Boolean = true,
  val usageLimit: Long = 1000,
  val usedCount: Long = 0,
  val badgeText: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

val PAKISTAN_CITIES = listOf(
  "Islamabad",
  "Rawalpindi",
  "Lahore",
  "Karachi",
  "Faisalabad",
  "Multan",
  "Peshawar",
  "Gujranwala",
  "Sialkot",
  "Hyderabad",
  "Jhang",
  "Bahawalpur"
)

val PAKISTAN_CITY_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
  "Islamabad" to (33.6844 to 73.0479),
  "Rawalpindi" to (33.5651 to 73.0169),
  "Lahore" to (31.5204 to 74.3587),
  "Karachi" to (24.8607 to 67.0011),
  "Faisalabad" to (31.4504 to 73.1350),
  "Multan" to (30.1575 to 71.5249),
  "Peshawar" to (34.0151 to 71.5249),
  "Gujranwala" to (32.1877 to 74.1945),
  "Sialkot" to (32.4945 to 74.5229),
  "Hyderabad" to (25.3960 to 68.3578),
  "Jhang" to (31.2781 to 72.3317),
  "Bahawalpur" to (29.3544 to 71.6911)
)

val FOOD_CATEGORIES = listOf(
  "All",
  "Biryani",
  "Karahi",
  "BBQ",
  "Burgers",
  "Pizza",
  "Desi Nashta",
  "Shawarma",
  "Chai & Coffee",
  "Desserts",
  "Home Chef",
  "Grocery"
)

sealed interface RecentOrdersUiState {
  data object Loading : RecentOrdersUiState
  data class Success(val orders: List<Order>) : RecentOrdersUiState
  data class Error(val message: String) : RecentOrdersUiState
}

@IgnoreExtraProperties
data class OrderNotificationRecord(
  @DocumentId val id: String = "",
  val orderId: String = "",
  val customerId: String = "",
  val storeName: String = "",
  val status: String = "",
  val title: String = "",
  val body: String = "",
  val fcmToken: String = "",
  val deliveryChannel: String = "FCM",
  val dispatchStatus: String = "PENDING",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Review(
  @DocumentId val id: String = "",
  val orderId: String = "",
  val storeId: String = "",
  val storeName: String = "",
  val customerId: String = "",
  val customerName: String = "",
  val rating: Int = 5,
  val comment: String = "",
  val orderItemsSummary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface ReviewsUiState {
  data object Loading : ReviewsUiState
  data class Success(val reviews: List<Review>) : ReviewsUiState
  data class Error(val message: String) : ReviewsUiState
}


