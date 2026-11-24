package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.example.data.payment.VoucherCalculator
import com.example.data.reviews.ReviewValidator
import com.example.data.security.AccessControl
import com.example.notifications.OrderNotificationFormatter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await

class MealVerseRepository(customDb: FirebaseFirestore? = null) {

  constructor(context: Context) : this(
    try {
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      )
    } catch (e: Exception) {
      null
    }
  )

  private val db: FirebaseFirestore by lazy {
    customDb ?: try {
      FirebaseFirestore.getInstance()
    } catch (e: Exception) {
      throw IllegalStateException("FirebaseFirestore is not initialized: ${e.message}")
    }
  }

  private val auth get() = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before performing database operations.")
  }

  suspend fun isCurrentUserAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    if (AccessControl.isAdminEmail(user.email, user.isEmailVerified)) return true
    val profile = getUserProfile(user.uid)
    return AccessControl.isAdminAccessAllowed(
      isAuthenticated = true,
      email = user.email,
      emailVerified = user.isEmailVerified,
      profile = profile
    )
  }

  // --- User Profile ---
  suspend fun getUserProfile(userId: String): UserProfile? {
    return try {
      val doc = db.collection("users").document(userId).get().await()
      doc.toObject(UserProfile::class.java)
    } catch (e: Exception) {
      Log.e("MealVerseRepo", "Error getting user profile: ${e.message}", e)
      null
    }
  }

  suspend fun saveUserProfile(profile: UserProfile) {
    val uid = requireUserId()
    val user = auth.currentUser
    if (!AccessControl.canAssignRole(
        requestedRole = profile.role,
        isAuthenticated = user != null,
        email = user?.email,
        emailVerified = user?.isEmailVerified == true
      )
    ) {
      throw SecurityException("Unauthorized role assignment.")
    }
    val docRef = db.collection("users").document(uid)
    val data = mapOf(
      "userId" to uid,
      "fullName" to profile.fullName,
      "businessName" to profile.businessName,
      "email" to (auth.currentUser?.email ?: profile.email),
      "phoneNumber" to profile.phoneNumber,
      "role" to profile.role,
      "city" to profile.city,
      "address" to profile.address,
      "landmark" to profile.landmark,
      "houseFlat" to profile.houseFlat,
      "streetSector" to profile.streetSector,
      "merchantId" to profile.merchantId,
      "category" to profile.category,
      "cnicNumber" to profile.cnicNumber,
      "status" to profile.status.ifBlank { "Pending" },
      "latitude" to profile.latitude,
      "longitude" to profile.longitude,
      "fcmToken" to profile.fcmToken,
      "notificationsEnabled" to profile.notificationsEnabled,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(data).await()
  }

  suspend fun saveFcmToken(token: String) {
    val uid = auth.currentUser?.uid ?: return
    if (uid.isBlank() || token.isBlank()) return
    try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "fcmToken" to token,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Exception) {
      Log.w("MealVerseRepo", "Could not persist FCM token: ${e.message}")
    }
  }

  suspend fun syncFcmToken(): String? {
    if (auth.currentUser == null) return null
    return try {
      val token = Firebase.messaging.token.await()
      if (!token.isNullOrBlank()) {
        saveFcmToken(token)
      }
      token
    } catch (e: Exception) {
      Log.w("MealVerseRepo", "FCM token fetch skipped: ${e.message}")
      null
    }
  }

  suspend fun setNotificationsEnabled(enabled: Boolean) {
    val uid = auth.currentUser?.uid ?: return
    try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "notificationsEnabled" to enabled,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Exception) {
      Log.w("MealVerseRepo", "Could not update notification preference: ${e.message}")
    }
  }

  // --- Stores ---
  fun observeApprovedStores(city: String): Flow<List<Store>> {
    if (auth.currentUser == null) return flowOf(emptyList())
    return db.collection("stores")
      .whereEqualTo("status", StoreStatus.APPROVED.name)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Store::class.java)
          } catch (e: Exception) {
            null
          }
        }.filter {
          it.city.equals(city, ignoreCase = true) || city == "All"
        }
      }
      .catch { e ->
        Log.w("MealVerseRepo", "Stores observation notice: ${e.message}")
        emit(emptyList())
      }
  }

  fun observeAllStoresForAdmin(): Flow<List<Store>> {
    if (auth.currentUser == null) return flowOf(emptyList())
    return db.collection("stores")
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Store::class.java)
          } catch (e: Exception) {
            null
          }
        }
      }
      .catch { e ->
        Log.w("MealVerseRepo", "Admin stores observation notice: ${e.message}")
        emit(emptyList())
      }
  }

  suspend fun getStoreByOwner(ownerId: String): Store? {
    return try {
      val query = db.collection("stores").whereEqualTo("ownerId", ownerId).get().await()
      query.documents.firstOrNull()?.toObject(Store::class.java)
    } catch (e: Exception) {
      Log.e("MealVerseRepo", "Error getting store by owner: ${e.message}", e)
      null
    }
  }

  suspend fun createStore(store: Store): String {
    val uid = requireUserId()
    val docRef = db.collection("stores").document()
    val data = mapOf(
      "ownerId" to uid,
      "merchantId" to store.merchantId,
      "name" to store.name,
      "category" to store.category,
      "description" to store.description,
      "city" to store.city,
      "address" to store.address,
      "phone" to store.phone,
      "openingTime" to store.openingTime,
      "closingTime" to store.closingTime,
      "minOrder" to store.minOrder,
      "deliveryFee" to store.deliveryFee,
      "prepTimeMinutes" to store.prepTimeMinutes,
      "rating" to 5.0,
      "reviewCount" to 1L,
      "status" to "Pending",
      "isOpen" to true,
      "imageUrl" to store.imageUrl,
      "cnicNumber" to store.cnicNumber,
      "bankAccountTitle" to store.bankAccountTitle,
      "latitude" to store.latitude,
      "longitude" to store.longitude,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(data).await()
    return docRef.id
  }

  suspend fun updateStoreStatus(storeId: String, status: StoreStatus, rejectionReason: String = "") {
    if (!isCurrentUserAdmin()) {
      throw SecurityException("Administrator privileges are required to update store verification status.")
    }
    val docRef = db.collection("stores").document(storeId)
    docRef.update(
      mapOf(
        "status" to status.name,
        "rejectionReason" to rejectionReason,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
  }

  // --- Products ---
  fun observeProducts(storeId: String): Flow<List<Product>> =
    db.collection("stores").document(storeId).collection("products")
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Product::class.java)
          } catch (e: Exception) {
            null
          }
        }
      }
      .catch { e ->
        Log.w("MealVerseRepo", "Products observation notice: ${e.message}")
        emit(emptyList())
      }

  suspend fun addProduct(storeId: String, product: Product) {
    val docRef = db.collection("stores").document(storeId).collection("products").document()
    val data = mapOf(
      "storeId" to storeId,
      "name" to product.name,
      "description" to product.description,
      "price" to product.price,
      "category" to product.category,
      "isAvailable" to product.isAvailable,
      "imageUrl" to product.imageUrl,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(data).await()
  }

  suspend fun toggleProductAvailability(storeId: String, productId: String, isAvailable: Boolean) {
    db.collection("stores").document(storeId).collection("products").document(productId)
      .update(
        mapOf(
          "isAvailable" to isAvailable,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
  }

  // --- Orders ---
  suspend fun placeOrder(order: Order): String {
    val uid = requireUserId()
    val docRef = db.collection("orders").document()
    val itemsData = order.items.map {
      mapOf(
        "productId" to it.productId,
        "productName" to it.productName,
        "price" to it.price,
        "quantity" to it.quantity,
        "instructions" to it.instructions
      )
    }
    val data = mapOf(
      "customerId" to uid,
      "customerName" to order.customerName,
      "customerPhone" to order.customerPhone,
      "storeId" to order.storeId,
      "storeName" to order.storeName,
      "storeOwnerId" to order.storeOwnerId,
      "city" to order.city,
      "deliveryAddress" to order.deliveryAddress,
      "items" to itemsData,
      "subtotal" to order.subtotal,
      "deliveryFee" to order.deliveryFee,
      "discountAmount" to order.discountAmount,
      "voucherCode" to order.voucherCode,
      "voucherTitle" to order.voucherTitle,
      "totalAmount" to order.totalAmount,
      "paymentMethod" to order.paymentMethod,
      "paymentStatus" to order.paymentStatus,
      "cardBrand" to order.cardBrand,
      "cardLast4" to order.cardLast4.takeLast(4),
      "status" to OrderStatus.NEW.name,
      "riderId" to "",
      "riderName" to "",
      "specialInstructions" to order.specialInstructions,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(data).await()

    if (order.voucherCode.isNotBlank()) {
      incrementVoucherUsage(order.voucherCode)
    }

    return docRef.id
  }

  // --- Vouchers ---
  fun observeActiveVouchers(): Flow<List<Voucher>> {
    if (auth.currentUser == null) return flowOf(emptyList())
    return db.collection("vouchers")
      .snapshots(MetadataChanges.INCLUDE)
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Voucher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } catch (e: Exception) {
            null
          }
        }.filter {
          it.isActive && (it.usageLimit <= 0 || it.usedCount < it.usageLimit)
        }.sortedBy { it.minOrderAmount }
      }
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w("MealVerseRepo", "Vouchers observation notice: ${e.message}")
        emit(emptyList())
      }
  }

  suspend fun findVoucherByCode(rawCode: String): Voucher? {
    val normalized = VoucherCalculator.normalizeCode(rawCode)
    if (normalized.isBlank() || auth.currentUser == null) return null
    return try {
      val byId = db.collection("vouchers").document(normalized).get().await()
      if (byId.exists()) {
        byId.toObject(Voucher::class.java)
      } else {
        val query = db.collection("vouchers")
          .whereEqualTo("code", normalized)
          .limit(1)
          .get()
          .await()
        query.documents.firstOrNull()?.toObject(Voucher::class.java)
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.w("MealVerseRepo", "Voucher lookup notice: ${e.message}")
      null
    }
  }

  private suspend fun incrementVoucherUsage(rawCode: String) {
    val normalized = VoucherCalculator.normalizeCode(rawCode)
    if (normalized.isBlank()) return
    try {
      val docById = db.collection("vouchers").document(normalized)
      val snap = docById.get().await()
      val targetRef = if (snap.exists()) {
        docById
      } else {
        db.collection("vouchers").whereEqualTo("code", normalized).limit(1).get().await()
          .documents.firstOrNull()?.reference
      }
      targetRef?.update(
        mapOf(
          "usedCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )?.await()
    } catch (e: Exception) {
      Log.w("MealVerseRepo", "Voucher usage increment skipped: ${e.message}")
    }
  }

  suspend fun ensureVouchersSeeded() {
    try {
      if (!isCurrentUserAdmin()) return
      val existing = db.collection("vouchers").get().await()
      val existingCodes = existing.documents.mapNotNull { it.getString("code") }.toSet()
      for (voucher in VoucherCalculator.DEFAULT_VOUCHERS) {
        if (voucher.code in existingCodes) continue
        db.collection("vouchers").document(voucher.code).set(
          mapOf(
            "code" to voucher.code,
            "title" to voucher.title,
            "description" to voucher.description,
            "discountType" to voucher.discountType,
            "discountValue" to voucher.discountValue,
            "maxDiscount" to voucher.maxDiscount,
            "minOrderAmount" to voucher.minOrderAmount,
            "city" to voucher.city,
            "paymentMethodFilter" to voucher.paymentMethodFilter,
            "isActive" to voucher.isActive,
            "usageLimit" to voucher.usageLimit,
            "usedCount" to voucher.usedCount,
            "badgeText" to voucher.badgeText,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Exception) {
      Log.w("MealVerseRepo", "Voucher seed skipped: ${e.message}")
    }
  }

  fun observeCustomerOrders(customerId: String): Flow<List<Order>> {
    if (customerId.isBlank()) return flowOf(emptyList())
    return db.collection("orders")
      .whereEqualTo("customerId", customerId)
      .snapshots(MetadataChanges.INCLUDE)
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } catch (e: Exception) {
            null
          }
        }.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w("MealVerseRepo", "Customer orders observation notice: ${e.message}")
        emit(emptyList())
      }
  }

  fun observeCustomerOrdersState(customerId: String): Flow<RecentOrdersUiState> {
    if (customerId.isBlank()) {
      return flowOf(RecentOrdersUiState.Success(emptyList()))
    }
    return db.collection("orders")
      .whereEqualTo("customerId", customerId)
      .snapshots(MetadataChanges.INCLUDE)
      .map<_, RecentOrdersUiState> { snapshot ->
        val orders = snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } catch (e: Exception) {
            null
          }
        }.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        RecentOrdersUiState.Success(orders)
      }
      .onStart { emit(RecentOrdersUiState.Loading) }
      .catch { e ->
        if (e is CancellationException) throw e
        Log.e("MealVerseRepo", "Failed to fetch customer recent orders: ${e.message}", e)
        emit(RecentOrdersUiState.Error(e.localizedMessage ?: "Unable to fetch recent orders from Firestore."))
      }
  }

  fun observeOrderById(orderId: String): Flow<Order?> {
    if (orderId.isBlank()) return flowOf(null)
    return db.collection("orders")
      .document(orderId)
      .snapshots(MetadataChanges.INCLUDE)
      .map { snapshot ->
        try {
          if (snapshot.exists()) {
            snapshot.toObject(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        } catch (e: Exception) {
          null
        }
      }
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w("MealVerseRepo", "Order document observation notice: ${e.message}")
        emit(null)
      }
  }

  fun observeStoreOrders(storeId: String, storeOwnerId: String = ""): Flow<List<Order>> {
    val currentUid = auth.currentUser?.uid.orEmpty()
    val effectiveOwnerId = storeOwnerId.ifBlank { currentUid }
    if (currentUid.isBlank() || storeId.isBlank() || effectiveOwnerId.isBlank()) {
      return flowOf(emptyList())
    }
    return db.collection("orders")
      .whereEqualTo("storeOwnerId", effectiveOwnerId)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Order::class.java)
          } catch (e: Exception) {
            null
          }
        }.filter { it.storeId == storeId }
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        Log.w("MealVerseRepo", "Store orders observation notice: ${e.message}")
        emit(emptyList())
      }
  }

  suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
    val uid = requireUserId()
    val orderRef = db.collection("orders").document(orderId)
    orderRef
      .update(
        mapOf(
          "status" to newStatus.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

    // Enqueue FCM notification dispatch record; Cloud Function resolves customer FCM token server-side
    try {
      val orderSnap = orderRef.get().await()
      val order = orderSnap.toObject(Order::class.java)
      if (order != null && order.customerId.isNotBlank()) {
        val fcmToken = if (order.customerId == uid) {
          try {
            db.collection("users").document(uid).get().await().getString("fcmToken").orEmpty()
          } catch (_: Exception) {
            ""
          }
        } else {
          ""
        }
        val content = OrderNotificationFormatter.buildNotificationContent(
          orderId = orderId,
          storeName = order.storeName,
          rawStatus = newStatus.name
        )
        db.collection("notifications").document().set(
          mapOf(
            "orderId" to orderId,
            "customerId" to order.customerId,
            "storeName" to order.storeName,
            "status" to newStatus.name,
            "title" to content.title,
            "body" to content.body,
            "fcmToken" to fcmToken,
            "deliveryChannel" to "FCM",
            "dispatchStatus" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Exception) {
      Log.w("MealVerseRepo", "Notification dispatch record skipped: ${e.message}")
    }
  }

  // --- Reviews Collection ('Reviews') ---
  suspend fun submitOrderReview(
    order: Order,
    customerName: String,
    rating: Int,
    comment: String
  ): Result<Review> {
    return try {
      val uid = requireUserId()
      val validation = ReviewValidator.validateReviewSubmission(
        order = order,
        currentUserId = uid,
        rating = rating,
        comment = comment
      )
      if (!validation.isValid) {
        return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid review details."))
      }

      val resolvedName = customerName.trim()
        .ifBlank { order.customerName.trim() }
        .ifBlank { auth.currentUser?.displayName?.trim().orEmpty() }
        .ifBlank { "MealVerse Customer" }
        .take(100)

      val resolvedStoreName = order.storeName.trim()
        .ifBlank { "MealVerse Restaurant" }
        .take(120)

      val itemsSummary = order.items
        .take(3)
        .joinToString(", ") { "${it.quantity}x ${it.productName}" }
        .let { summary ->
          if (order.items.size > 3) "$summary +${order.items.size - 3} more" else summary
        }
        .take(300)

      val reviewRef = db.collection("Reviews").document(order.id)
      val existingSnap = reviewRef.get().await()

      if (existingSnap.exists()) {
        reviewRef.update(
          mapOf(
            "rating" to rating,
            "comment" to validation.sanitizedComment,
            "customerName" to resolvedName,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        reviewRef.set(
          mapOf(
            "orderId" to order.id,
            "storeId" to order.storeId,
            "storeName" to resolvedStoreName,
            "customerId" to uid,
            "customerName" to resolvedName,
            "rating" to rating,
            "comment" to validation.sanitizedComment,
            "orderItemsSummary" to itemsSummary,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      val saved = Review(
        id = order.id,
        orderId = order.id,
        storeId = order.storeId,
        storeName = resolvedStoreName,
        customerId = uid,
        customerName = resolvedName,
        rating = rating,
        comment = validation.sanitizedComment,
        orderItemsSummary = itemsSummary
      )
      Result.success(saved)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e("MealVerseRepo", "Error submitting order review: ${e.message}", e)
      Result.failure(e)
    }
  }

  fun observeCustomerReviews(customerId: String): Flow<Map<String, Review>> {
    if (auth.currentUser == null || customerId.isBlank()) {
      return flowOf(emptyMap())
    }
    return db.collection("Reviews")
      .whereEqualTo("customerId", customerId)
      .snapshots(MetadataChanges.INCLUDE)
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } catch (e: Exception) {
            null
          }
        }.filter { it.orderId.isNotBlank() }
          .associateBy { it.orderId }
      }
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w("MealVerseRepo", "Customer reviews observation notice: ${e.message}")
        emit(emptyMap())
      }
  }

  fun observeStoreReviews(storeId: String): Flow<List<Review>> {
    if (auth.currentUser == null || storeId.isBlank()) {
      return flowOf(emptyList())
    }
    return db.collection("Reviews")
      .whereEqualTo("storeId", storeId)
      .snapshots(MetadataChanges.INCLUDE)
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } catch (e: Exception) {
            null
          }
        }.sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w("MealVerseRepo", "Store reviews observation notice: ${e.message}")
        emit(emptyList())
      }
  }

  // --- Categories Collection ---
  fun observeCategories(): Flow<List<Category>> =
    db.collection("categories")
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            doc.toObject(Category::class.java)
          } catch (e: Exception) {
            null
          }
        }.sortedBy { it.displayOrder }
      }
      .catch { e ->
        Log.w("MealVerseRepo", "Categories observation notice: ${e.message}")
        emit(emptyList())
      }

  suspend fun createCategory(category: Category): String {
    val docRef = db.collection("categories").document()
    val data = mapOf(
      "name" to category.name,
      "slug" to category.slug,
      "description" to category.description,
      "icon" to category.icon,
      "imageUrl" to category.imageUrl,
      "displayOrder" to category.displayOrder,
      "isActive" to category.isActive,
      "isFeatured" to category.isFeatured,
      "itemCount" to category.itemCount,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(data).await()
    return docRef.id
  }

  // --- Merchants Collection ---
  suspend fun getMerchant(merchantId: String): Merchant? {
    return try {
      val doc = db.collection("merchants").document(merchantId).get().await()
      doc.toObject(Merchant::class.java)
    } catch (e: Exception) {
      Log.e("MealVerseRepo", "Error getting merchant: ${e.message}", e)
      null
    }
  }

  suspend fun registerMerchant(merchant: Merchant): String {
    val uid = requireUserId()
    val docRef = db.collection("merchants").document()
    val data = mapOf(
      "ownerUserId" to uid,
      "businessName" to merchant.businessName,
      "ownerName" to merchant.ownerName,
      "contactEmail" to (auth.currentUser?.email ?: merchant.contactEmail),
      "contactPhone" to merchant.contactPhone,
      "cnicNumber" to merchant.cnicNumber,
      "category" to merchant.category,
      "ntnNumber" to merchant.ntnNumber,
      "bankAccountTitle" to merchant.bankAccountTitle,
      "bankName" to merchant.bankName,
      "ibanNumber" to merchant.ibanNumber,
      "verificationStatus" to merchant.verificationStatus.ifBlank { "Pending" },
      "status" to merchant.status.ifBlank { "Pending" },
      "commissionRate" to merchant.commissionRate,
      "storeCount" to merchant.storeCount,
      "rejectionReason" to "",
      "latitude" to merchant.latitude,
      "longitude" to merchant.longitude,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(data).await()
    return docRef.id
  }

  /**
   * Registers a shopkeeper, setting the initial account status to 'Pending' in Firestore across
   * Users, Stores, and Merchants collections.
   */
  suspend fun registerShopkeeper(
    ownerName: String,
    businessName: String,
    category: String,
    cnicNumber: String,
    phone: String = "+92 300 0000000",
    city: String = "Islamabad",
    address: String = "",
    description: String = "",
    bankAccountTitle: String = "",
    latitude: Double? = null,
    longitude: Double? = null
  ): Triple<UserProfile, Store, Merchant> {
    val uid = requireUserId()
    val email = auth.currentUser?.email ?: ""

    // 1. Create Merchant record with initial status = 'Pending'
    val merchantDocRef = db.collection("merchants").document()
    val merchant = Merchant(
      id = merchantDocRef.id,
      ownerUserId = uid,
      businessName = businessName,
      ownerName = ownerName,
      contactEmail = email,
      contactPhone = phone,
      cnicNumber = cnicNumber,
      category = category,
      bankAccountTitle = bankAccountTitle,
      verificationStatus = "Pending",
      status = "Pending",
      latitude = latitude,
      longitude = longitude
    )
    val merchantData = mapOf(
      "ownerUserId" to uid,
      "businessName" to businessName,
      "ownerName" to ownerName,
      "contactEmail" to email,
      "contactPhone" to phone,
      "cnicNumber" to cnicNumber,
      "category" to category,
      "bankAccountTitle" to bankAccountTitle,
      "ntnNumber" to "",
      "bankName" to "",
      "ibanNumber" to "",
      "verificationStatus" to "Pending",
      "status" to "Pending",
      "commissionRate" to 15.0,
      "storeCount" to 1L,
      "rejectionReason" to "",
      "latitude" to latitude,
      "longitude" to longitude,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    merchantDocRef.set(merchantData).await()

    // 2. Create Store record with initial status = 'Pending'
    val storeDocRef = db.collection("stores").document()
    val storeDesc = description.ifBlank { "Authentic $category dishes and specialties in $city" }
    val store = Store(
      id = storeDocRef.id,
      merchantId = merchantDocRef.id,
      ownerId = uid,
      name = businessName,
      category = category,
      description = storeDesc,
      city = city,
      address = address,
      phone = phone,
      status = "Pending",
      cnicNumber = cnicNumber,
      bankAccountTitle = bankAccountTitle,
      isOpen = true,
      latitude = latitude,
      longitude = longitude
    )
    val storeData = mapOf(
      "ownerId" to uid,
      "merchantId" to merchantDocRef.id,
      "name" to businessName,
      "category" to category,
      "description" to storeDesc,
      "city" to city,
      "address" to address,
      "phone" to phone,
      "openingTime" to "11:00 AM",
      "closingTime" to "11:00 PM",
      "minOrder" to 300.0,
      "deliveryFee" to 99.0,
      "prepTimeMinutes" to 25L,
      "rating" to 5.0,
      "reviewCount" to 1L,
      "status" to "Pending",
      "isOpen" to true,
      "imageUrl" to "",
      "cnicNumber" to cnicNumber,
      "bankAccountTitle" to bankAccountTitle,
      "latitude" to latitude,
      "longitude" to longitude,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    storeDocRef.set(storeData).await()

    // 3. Save UserProfile with role = SHOPKEEPER and status = 'Pending'
    val profile = UserProfile(
      userId = uid,
      fullName = ownerName,
      businessName = businessName,
      email = email,
      phoneNumber = phone,
      role = UserRole.SHOPKEEPER.name,
      city = city,
      address = address,
      merchantId = merchantDocRef.id,
      category = category,
      cnicNumber = cnicNumber,
      status = "Pending",
      latitude = latitude,
      longitude = longitude
    )
    saveUserProfile(profile)

    return Triple(profile, store, merchant)
  }

  // --- Initial Seed for Pakistani Restaurants & Categories (Admin Only) ---
  suspend fun seedInitialDataIfNeeded(city: String) {
    try {
      val uid = auth.currentUser?.uid ?: return
      if (!isCurrentUserAdmin()) return

      // Seed Categories if empty
      val categoriesSnapshot = db.collection("categories").get().await()
      if (categoriesSnapshot.isEmpty) {
        val initialCategories = listOf(
          Category(name = "Biryani", slug = "biryani", description = "Karachi & Sindhi Dum Biryani", displayOrder = 1, isActive = true, isFeatured = true),
          Category(name = "Karahi & Handi", slug = "karahi-handi", description = "Desi Ghee & Shinwari Karahi", displayOrder = 2, isActive = true, isFeatured = true),
          Category(name = "BBQ & Grills", slug = "bbq-grills", description = "Sajji, Malai Boti, Seekh Kabab", displayOrder = 3, isActive = true, isFeatured = true),
          Category(name = "Burgers & Fast Food", slug = "burgers-fast-food", description = "Zingers, Loaded Fries & Pizzas", displayOrder = 4, isActive = true, isFeatured = true),
          Category(name = "Desi Nashta & Chai", slug = "desi-nashta", description = "Halwa Puri, Nihari & Karak Chai", displayOrder = 5, isActive = true, isFeatured = true),
          Category(name = "Mithai & Sweets", slug = "mithai-sweets", description = "Multani Sohan Halwa & Gulab Jamun", displayOrder = 6, isActive = true, isFeatured = true),
          Category(name = "Home Chef", slug = "home-chef", description = "Freshly cooked Ghar ka Khana", displayOrder = 7, isActive = true, isFeatured = true),
          Category(name = "Grocery & Mart", slug = "grocery-mart", description = "Daily essentials & pantry", displayOrder = 8, isActive = true, isFeatured = false)
        )
        for (cat in initialCategories) {
          val docRef = db.collection("categories").document()
          docRef.set(
            mapOf(
              "name" to cat.name,
              "slug" to cat.slug,
              "description" to cat.description,
              "icon" to cat.icon,
              "imageUrl" to cat.imageUrl,
              "displayOrder" to cat.displayOrder,
              "isActive" to cat.isActive,
              "isFeatured" to cat.isFeatured,
              "itemCount" to cat.itemCount,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
      }

      // Seed Vouchers if missing
      ensureVouchersSeeded()

      val existingSnapshot = db.collection("stores").get().await()
      val existingNames = existingSnapshot.documents.mapNotNull { it.getString("name") }.toSet()

      val seedOwnerId = uid
      val initialStores = listOf(
        Store(
          name = "Savour Foods",
          category = "Pakistani Food",
          description = "Famous Pulao Kabab, Crispy Chicken and Zarda. Islamabad's pride.",
          city = "Islamabad",
          address = "Blue Area, Jinnah Avenue, Islamabad",
          phone = "+9251111728687",
          minOrder = 300.0,
          deliveryFee = 80.0,
          prepTimeMinutes = 20,
          rating = 4.9,
          reviewCount = 4280,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Butt Karahi",
          category = "Karahi",
          description = "Authentic Desi Ghee Mutton & Chicken Karahi cooked fresh.",
          city = "Lahore",
          address = "Lakshmi Chowk, Lahore",
          phone = "+924237234567",
          minOrder = 800.0,
          deliveryFee = 120.0,
          prepTimeMinutes = 35,
          rating = 4.8,
          reviewCount = 2840,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Student Biryani",
          category = "Biryani",
          description = "Karachi's original spicy aromatic Dum Biryani with Aloo and Raita.",
          city = "Karachi",
          address = "Saddar, Karachi",
          phone = "+9221111111444",
          minOrder = 400.0,
          deliveryFee = 70.0,
          prepTimeMinutes = 20,
          rating = 4.7,
          reviewCount = 5100,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Cheezious",
          category = "Fast Food",
          description = "Crown Crust, Cheesy Tikka, Behari Rolls, and Crispy Fries.",
          city = "Islamabad",
          address = "F-7 Markaz, Islamabad",
          phone = "+9251111446699",
          minOrder = 500.0,
          deliveryFee = 100.0,
          prepTimeMinutes = 30,
          rating = 4.8,
          reviewCount = 3910,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Ammi's Home Kitchen",
          category = "Home Chef",
          description = "Hygienic, authentic Ghar ka Khana: Daal Chawal, Chicken Qorma, Roti.",
          city = "Islamabad",
          address = "Sector G-11/3, Islamabad",
          phone = "+923005551234",
          minOrder = 350.0,
          deliveryFee = 60.0,
          prepTimeMinutes = 25,
          rating = 4.9,
          reviewCount = 890,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Chai Shai & Paratha Spot",
          category = "Desi Nashta",
          description = "Kadak Karak Doodh Patti Chai, Aloo Paratha, Cheese Omelette.",
          city = "Rawalpindi",
          address = "Commercial Market, Satellite Town, Rawalpindi",
          phone = "+923334445555",
          minOrder = 200.0,
          deliveryFee = 50.0,
          prepTimeMinutes = 15,
          rating = 4.6,
          reviewCount = 1420,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Kolachi Restaurant",
          category = "BBQ",
          description = "Iconic Do Darya waterfront open-air dining. Sajji, Malai Boti & Seafood.",
          city = "Karachi",
          address = "Do Darya, Phase 8, DHA, Karachi",
          phone = "+9221111444222",
          minOrder = 900.0,
          deliveryFee = 140.0,
          prepTimeMinutes = 40,
          rating = 4.9,
          reviewCount = 6840,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Burns Road Food Corner",
          category = "Pakistani Food",
          description = "Historic Karachi street food: Shahi Haleem, Dahi Baray, Bun Kabab.",
          city = "Karachi",
          address = "Burns Road, Saddar, Karachi",
          phone = "+922132631122",
          minOrder = 250.0,
          deliveryFee = 60.0,
          prepTimeMinutes = 20,
          rating = 4.8,
          reviewCount = 3710,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Waris Nihari",
          category = "Pakistani Food",
          description = "Anarkali's slow-simmered Nalli Maghaz Beef Nihari with piping hot Kulchas.",
          city = "Lahore",
          address = "Abkari Road, New Anarkali, Lahore",
          phone = "+924237321155",
          minOrder = 500.0,
          deliveryFee = 90.0,
          prepTimeMinutes = 25,
          rating = 4.9,
          reviewCount = 4590,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Gawalmandi Desi Nashta",
          category = "Desi Nashta",
          description = "Historic Lahore breakfast: Halwa Puri, Murgh Chana, Khoya Paira Lassi.",
          city = "Lahore",
          address = "Gawalmandi Food Street, Lahore",
          phone = "+924237662244",
          minOrder = 250.0,
          deliveryFee = 60.0,
          prepTimeMinutes = 20,
          rating = 4.7,
          reviewCount = 2950,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Nisar Charsi Tikka",
          category = "Karahi",
          description = "Legendary Namak Mandi Shinwari Dumba Karahi, Lamb Chops & Tikka.",
          city = "Peshawar",
          address = "Namak Mandi, Peshawar",
          phone = "+92915277112",
          minOrder = 900.0,
          deliveryFee = 100.0,
          prepTimeMinutes = 35,
          rating = 4.9,
          reviewCount = 3890,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Peshawari Chapli Kabab House",
          category = "Pakistani Food",
          description = "Authentic soft spiced beef Chapli Kababs fried in pure tallow.",
          city = "Peshawar",
          address = "Qissa Khwani Bazaar, Peshawar",
          phone = "+92915288990",
          minOrder = 350.0,
          deliveryFee = 70.0,
          prepTimeMinutes = 20,
          rating = 4.8,
          reviewCount = 2140,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Rewari Sweets & Multani Sohan Halwa",
          category = "Sweets",
          description = "Pure Desi Ghee Multani Almond Sohan Halwa, Gulab Jamun, Samosa Chaat.",
          city = "Multan",
          address = "Hussain Agahi Bazaar, Multan",
          phone = "+92614543322",
          minOrder = 300.0,
          deliveryFee = 60.0,
          prepTimeMinutes = 15,
          rating = 4.9,
          reviewCount = 3450,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Al-Aziz Dahi Bhallay & Fast Food",
          category = "Fast Food",
          description = "Clock Tower's renowned sweet-sour Dahi Bhallay, Papdi Chaat & Burgers.",
          city = "Faisalabad",
          address = "Clock Tower Bazaar, Faisalabad",
          phone = "+92412614455",
          minOrder = 200.0,
          deliveryFee = 50.0,
          prepTimeMinutes = 15,
          rating = 4.7,
          reviewCount = 1880,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Shahbaz Tikka",
          category = "BBQ",
          description = "Famous Gujranwala tender Mutton Chops, Stuffed Tikka & Quail Karahi.",
          city = "Gujranwala",
          address = "GT Road, Gujranwala",
          phone = "+92553841122",
          minOrder = 600.0,
          deliveryFee = 90.0,
          prepTimeMinutes = 30,
          rating = 4.8,
          reviewCount = 2760,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        ),
        Store(
          name = "Bala Tikka & Karahi House",
          category = "Karahi",
          description = "Kartarpura Food Street's iconic chicken & beef seekh kababs & white karahi.",
          city = "Rawalpindi",
          address = "Kartarpura Food Street, Rawalpindi",
          phone = "+92515553311",
          minOrder = 450.0,
          deliveryFee = 70.0,
          prepTimeMinutes = 25,
          rating = 4.8,
          reviewCount = 2390,
          status = StoreStatus.APPROVED.name,
          isOpen = true
        )
      )

      for (store in initialStores) {
        if (store.name in existingNames) continue

        val docRef = db.collection("stores").document()
        val cityCoords = PAKISTAN_CITY_COORDINATES[store.city]
        val data = mapOf(
          "ownerId" to seedOwnerId,
          "name" to store.name,
          "category" to store.category,
          "description" to store.description,
          "city" to store.city,
          "address" to store.address,
          "phone" to store.phone,
          "openingTime" to store.openingTime,
          "closingTime" to store.closingTime,
          "minOrder" to store.minOrder,
          "deliveryFee" to store.deliveryFee,
          "prepTimeMinutes" to store.prepTimeMinutes,
          "rating" to store.rating,
          "reviewCount" to store.reviewCount,
          "status" to StoreStatus.APPROVED.name,
          "isOpen" to true,
          "latitude" to cityCoords?.first,
          "longitude" to cityCoords?.second,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()

        // Seed products
        val sampleProducts = when (store.name) {
          "Savour Foods" -> listOf(
            Product(name = "Single Chicken Pulao (with 1 Shami Kabab)", description = "Signature golden aromatic rice with tender chicken piece, 1 shami kabab & fresh salad", price = 420.0, category = "Main Course"),
            Product(name = "Double Chicken Pulao (with 2 Shami Kababs)", description = "Large portion with two shami kababs, double chicken & raita", price = 650.0, category = "Main Course"),
            Product(name = "Extra Shami Kabab", description = "Crispy beef shami kabab seasoned with secret spices", price = 90.0, category = "Sides"),
            Product(name = "Savour Special Zarda", description = "Traditional sweet rice with nuts and khoya", price = 180.0, category = "Desserts")
          )
          "Butt Karahi" -> listOf(
            Product(name = "Desi Ghee Chicken Karahi (Full)", description = "Fresh chicken prepared in pure Pakistani Desi Ghee with ginger and green chilies", price = 1850.0, category = "Karahi"),
            Product(name = "Desi Ghee Mutton Karahi (Half kg)", description = "Tender mutton pieces in thick rich tomato and desi butter gravy", price = 2200.0, category = "Karahi"),
            Product(name = "Khamiri Roti (Tandoori)", description = "Hot and fluffy tandoori khamiri roti", price = 40.0, category = "Breads"),
            Product(name = "Mint Raita & Salad Platter", description = "Freshly blended mint yogurt and crisp farm vegetables", price = 120.0, category = "Sides")
          )
          "Student Biryani" -> listOf(
            Product(name = "Chicken Dum Biryani (Regular)", description = "Authentic Karachi style basmati biryani with masala aloo and succulent chicken", price = 390.0, category = "Biryani"),
            Product(name = "Beef Nalli Biryani", description = "Rich aromatic spiced biryani cooked with slow-simmered beef shank bone marrow", price = 680.0, category = "Biryani"),
            Product(name = "Chicken Shami Burger", description = "Classic street-style bun kabab with egg, chutney and onion", price = 220.0, category = "Fast Food"),
            Product(name = "Gulab Jamun (2 Pcs)", description = "Hot melt-in-mouth milk dumplings in cardamom syrup", price = 150.0, category = "Desserts")
          )
          "Cheezious" -> listOf(
            Product(name = "Cheesy Tikka Pizza (Medium)", description = "Loaded with smoked tikka chunks, mozzarella cheese, onions and special sauce", price = 1290.0, category = "Pizza"),
            Product(name = "Crown Crust Pizza (Large)", description = "Special stuffed crown pockets filled with cheese and kabab bites", price = 1890.0, category = "Pizza"),
            Product(name = "Behari Chicken Roll (2 Pcs)", description = "Tender behari spiced chicken wrapped in golden paratha", price = 590.0, category = "Rolls"),
            Product(name = "Cheesy Fries Basket", description = "Crisp french fries smothered in cheddar sauce and jalapeños", price = 380.0, category = "Sides")
          )
          "Ammi's Home Kitchen" -> listOf(
            Product(name = "Daal Chawal & Achar Thali", description = "Comforting Yellow Moong Daal, fragrant basmati rice, homemade mango pickle and papad", price = 320.0, category = "Home Meals"),
            Product(name = "Desi Chicken Qorma with 2 Phulkas", description = "Home-style chicken qorma with soft handmade whole wheat rotis", price = 480.0, category = "Home Meals"),
            Product(name = "Aloo Qeema Matar", description = "Minced beef with soft potatoes and peas, cooked with ginger and cumin", price = 420.0, category = "Home Meals"),
            Product(name = "Kheer Bowl", description = "Slow-simmered rice pudding with cardamom, almonds and pistachios", price = 160.0, category = "Desserts")
          )
          "Kolachi Restaurant" -> listOf(
            Product(name = "Chicken Malai Boti (Skewer)", description = "Melt-in-mouth chicken cubes marinated in clotted cream, white pepper and cardamom", price = 620.0, category = "BBQ"),
            Product(name = "Balochi Sajji with Rice (Half)", description = "Tender whole salted roasted chicken served over aromatic basmati rice", price = 1100.0, category = "BBQ"),
            Product(name = "Grilled Prawns Skewer", description = "Arabian Sea fresh jumbo prawns grilled over charcoal with garlic herb glaze", price = 1450.0, category = "Seafood"),
            Product(name = "Roghani Naan", description = "Fluffy tandoori bread brushed with butter and sesame seeds", price = 70.0, category = "Breads")
          )
          "Burns Road Food Corner" -> listOf(
            Product(name = "Special Shahi Beef Haleem", description = "Slow-cooked shredded beef, wheat and lentils topped with golden onions, ginger and lemon", price = 350.0, category = "Main Course"),
            Product(name = "Burns Road Dahi Baray", description = "Soft lentil dumplings in sweet chilled yogurt with spicy chaat masala", price = 220.0, category = "Chaat"),
            Product(name = "Karachi Special Bun Kabab", description = "Pan-toasted bun with daal shami, fluffy egg lather, cucumber and imli chutney", price = 180.0, category = "Fast Food"),
            Product(name = "Rabri Falooda Glass", description = "Chilled condensed milk rabri with vermicelli, basil seeds and rose syrup", price = 280.0, category = "Desserts")
          )
          "Waris Nihari" -> listOf(
            Product(name = "Special Beef Maghaz Nalli Nihari", description = "Slow-cooked shank beef with bone marrow and brain, simmered overnight with 32 spices", price = 750.0, category = "Nihari"),
            Product(name = "Plain Beef Nihari (Quarter)", description = "Rich and thick velvety nihari with tender boneless beef chunk", price = 450.0, category = "Nihari"),
            Product(name = "Roghani Kulcha", description = "Crisp and soft sesame-seeded tandoori kulcha", price = 50.0, category = "Breads"),
            Product(name = "Ginger Lemon Green Chili Platter", description = "Freshly sliced slivered ginger, tart lemons and spicy green chilies", price = 30.0, category = "Sides")
          )
          "Gawalmandi Desi Nashta" -> listOf(
            Product(name = "Halwa Puri Chana Thali (2 Puris)", description = "Hot golden puffed puris served with sweet sooji halwa and spicy chickpeas curry", price = 260.0, category = "Breakfast"),
            Product(name = "Special Khoya Paira Sweet Lassi", description = "Rich creamy blended yogurt topped with thick cream layer and crushed peda", price = 220.0, category = "Beverages"),
            Product(name = "Murgh Chana Plate", description = "Tender chicken cooked with Lahori spiced chickpea gravy", price = 380.0, category = "Breakfast"),
            Product(name = "Methi Lachha Paratha", description = "Crispy multi-layered whole wheat paratha seasoned with dried fenugreek", price = 160.0, category = "Breakfast")
          )
          "Nisar Charsi Tikka" -> listOf(
            Product(name = "Shinwari Dumba Karahi (Half kg)", description = "Fresh lamb cooked in its own natural fat with fresh tomatoes, green chilies and salt only", price = 1650.0, category = "Karahi"),
            Product(name = "Charsi Lamb Tikka (Skewer)", description = "Charcoal-skewered salt-marinated lamb cubes with fat caps", price = 850.0, category = "BBQ"),
            Product(name = "Afghani Naan (Large)", description = "Crispy long oval tandoori bread with nigella seeds", price = 60.0, category = "Breads"),
            Product(name = "Green Tea Kehwa with Cardamom", description = "Aromatic traditional Peshawari green tea served with candied sugar", price = 70.0, category = "Beverages")
          )
          "Peshawari Chapli Kabab House" -> listOf(
            Product(name = "Traditional Peshawari Chapli Kabab (Large)", description = "Minced beef blended with crushed pomegranate seeds, coriander seeds, tomatoes and fried crisp", price = 320.0, category = "Main Course"),
            Product(name = "Special Egg Fried Chapli Kabab", description = "Chapli kabab topped with a fried egg cooked into the crust", price = 370.0, category = "Main Course"),
            Product(name = "Kandahari Naan", description = "Freshly baked flatbread", price = 50.0, category = "Breads"),
            Product(name = "Chutney & Kachumar Salad", description = "Mint dip with diced cucumber, tomatoes and onions", price = 60.0, category = "Sides")
          )
          "Rewari Sweets & Multani Sohan Halwa" -> listOf(
            Product(name = "Multani Royal Almond Sohan Halwa (500g)", description = "Traditional thick and brittle Desi Ghee Sohan Halwa loaded with whole almonds and pistachios", price = 750.0, category = "Sweets"),
            Product(name = "Gulab Jamun Gift Box (500g)", description = "Freshly made hot milk dumplings in saffron cardamom rose syrup", price = 580.0, category = "Sweets"),
            Product(name = "Crunchy Samosa Chaat Plate", description = "Crisp potato samosa mashed with spicy chickpeas, sweet chutney and yogurt", price = 190.0, category = "Chaat"),
            Product(name = "Pista Kulfi Falooda", description = "Traditional dense pistachio milk ice cream served with cold vermicelli", price = 240.0, category = "Desserts")
          )
          "Al-Aziz Dahi Bhallay & Fast Food" -> listOf(
            Product(name = "Special Faisalabadi Dahi Bhallay", description = "Spongy lentil bhallas soaked in spiced sweetened yogurt with crispy papdi", price = 220.0, category = "Chaat"),
            Product(name = "Crispy Zinger Burger Meal", description = "Extra-crunchy breaded chicken fillet with spicy mayo, cheddar cheese, fries and drink", price = 590.0, category = "Fast Food"),
            Product(name = "Spicy Chicken Shawarma Roll", description = "Thin pita bread wrapped with shaved marinated chicken and garlic sauce", price = 240.0, category = "Fast Food"),
            Product(name = "Loaded Cheesy Fries", description = "Crinkle-cut french fries topped with warm cheese sauce and jalapeño peppers", price = 350.0, category = "Sides")
          )
          "Shahbaz Tikka" -> listOf(
            Product(name = "Gujranwala Mutton Chops (6 Pcs)", description = "Tenderized mutton ribs marinated in Desi spices and grilled over live coals", price = 1350.0, category = "BBQ"),
            Product(name = "Chicken Seekh Kabab (4 Pcs)", description = "Juicy minced chicken skewers seasoned with crushed herbs and chilies", price = 520.0, category = "BBQ"),
            Product(name = "Batair (Quail) Karahi", description = "Whole quail birds cooked in spicy tomato and black pepper gravy", price = 890.0, category = "Karahi"),
            Product(name = "Sesame Roghani Naan", description = "Warm tandoori bread with melted butter and toasted sesame seeds", price = 60.0, category = "Breads")
          )
          "Bala Tikka & Karahi House" -> listOf(
            Product(name = "Kartarpura Beef Seekh Kababs (4 Pcs)", description = "Authentic smoky beef skewers served with mint chutney and onion rings", price = 480.0, category = "BBQ"),
            Product(name = "Creamy White Chicken Karahi (Half kg)", description = "Mild chicken karahi simmered with fresh cream, yogurt, white pepper and ginger", price = 980.0, category = "Karahi"),
            Product(name = "Garlic Butter Naan", description = "Tandoor-baked bread topped with roasted minced garlic and butter", price = 80.0, category = "Breads"),
            Product(name = "Zeera Raita", description = "Chilled savory yogurt whipped with roasted cumin seeds", price = 60.0, category = "Sides")
          )
          else -> listOf(
            Product(name = "Karak Doodh Patti Chai", description = "Rich full-cream milk tea boiled to perfection with cardamom", price = 120.0, category = "Beverages"),
            Product(name = "Aloo Cheese Paratha", description = "Crispy lachha paratha stuffed with spicy potato mash and melted cheese", price = 260.0, category = "Breakfast"),
            Product(name = "Desi Anda Shami Omelette", description = "Double egg fluffy omelette with onions, tomatoes and shami spices", price = 180.0, category = "Breakfast")
          )
        }

        for (prod in sampleProducts) {
          val prodRef = docRef.collection("products").document()
          prodRef.set(
            mapOf(
              "storeId" to docRef.id,
              "name" to prod.name,
              "description" to prod.description,
              "price" to prod.price,
              "category" to prod.category,
              "isAvailable" to true,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
      }
    } catch (e: Exception) {
      Log.w("MealVerseRepo", "Seed skipping or already seeded: ${e.message}")
    }
  }
}
