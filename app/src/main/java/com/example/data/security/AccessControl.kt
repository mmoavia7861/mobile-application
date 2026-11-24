package com.example.data.security

import com.example.data.model.UserProfile
import com.example.data.model.UserRole

sealed interface AccessCheckResult {
  data object Loading : AccessCheckResult
  data object Allowed : AccessCheckResult
  data class Denied(
    val title: String,
    val message: String,
    val requiresSignIn: Boolean = false,
    val requiresCustomerRegistration: Boolean = false,
    val requiresShopkeeperRegistration: Boolean = false,
    val currentRoleLabel: String? = null
  ) : AccessCheckResult
}

object AccessControl {
  val ADMIN_EMAILS: Set<String> = setOf("moaviah760@gmail.com")

  fun isAdminEmail(email: String?, emailVerified: Boolean): Boolean {
    if (!emailVerified || email.isNullOrBlank()) return false
    return email.trim().lowercase() in ADMIN_EMAILS
  }

  fun isAdminAccessAllowed(
    isAuthenticated: Boolean,
    email: String?,
    emailVerified: Boolean,
    profile: UserProfile?
  ): Boolean {
    if (!isAuthenticated) return false
    if (isAdminEmail(email, emailVerified)) return true
    return profile?.role.equals(UserRole.ADMIN.name, ignoreCase = true)
  }

  fun canAccessCustomerPanel(
    isAuthenticated: Boolean,
    email: String?,
    emailVerified: Boolean,
    profile: UserProfile?
  ): Boolean {
    if (!isAuthenticated) return false
    if (isAdminAccessAllowed(isAuthenticated, email, emailVerified, profile)) return true
    if (profile == null || profile.userId.isBlank()) return false
    return profile.role.equals(UserRole.CUSTOMER.name, ignoreCase = true)
  }

  fun canAccessShopkeeperPanel(
    isAuthenticated: Boolean,
    email: String?,
    emailVerified: Boolean,
    profile: UserProfile?
  ): Boolean {
    if (!isAuthenticated) return false
    if (isAdminAccessAllowed(isAuthenticated, email, emailVerified, profile)) return true
    if (profile == null || profile.userId.isBlank()) return false
    return profile.role.equals(UserRole.SHOPKEEPER.name, ignoreCase = true) ||
      profile.role.equals(UserRole.MERCHANT.name, ignoreCase = true)
  }

  fun evaluateAdminAccess(
    isAuthenticated: Boolean,
    email: String?,
    emailVerified: Boolean,
    profile: UserProfile?,
    isProfileLoading: Boolean
  ): AccessCheckResult {
    if (!isAuthenticated) {
      return AccessCheckResult.Denied(
        title = "Administrator Sign-In Required",
        message = "Sign in with an authorized administrator Google account to open the MealVerse Admin Verification Panel.",
        requiresSignIn = true
      )
    }
    if (isAdminEmail(email, emailVerified)) {
      return AccessCheckResult.Allowed
    }
    if (isProfileLoading) {
      return AccessCheckResult.Loading
    }
    if (profile?.role.equals(UserRole.ADMIN.name, ignoreCase = true)) {
      return AccessCheckResult.Allowed
    }
    return AccessCheckResult.Denied(
      title = "Admin Access Denied",
      message = "Your current account (${email ?: "signed-in user"}) does not have administrator permissions to verify stores or manage platform settings.",
      currentRoleLabel = profile?.role
    )
  }

  fun evaluateCustomerAccess(
    isAuthenticated: Boolean,
    email: String?,
    emailVerified: Boolean,
    profile: UserProfile?,
    isProfileLoading: Boolean
  ): AccessCheckResult {
    if (!isAuthenticated) {
      return AccessCheckResult.Denied(
        title = "Customer Sign-In Required",
        message = "Please sign in with Google and register a Customer profile before browsing restaurants or placing orders.",
        requiresSignIn = true
      )
    }
    if (isAdminEmail(email, emailVerified)) {
      return AccessCheckResult.Allowed
    }
    if (isProfileLoading) {
      return AccessCheckResult.Loading
    }
    if (profile == null || profile.userId.isBlank()) {
      return AccessCheckResult.Denied(
        title = "Customer Registration Required",
        message = "Your Google account is signed in, but you haven't completed Customer registration yet.",
        requiresCustomerRegistration = true
      )
    }
    if (canAccessCustomerPanel(isAuthenticated, email, emailVerified, profile)) {
      return AccessCheckResult.Allowed
    }
    return AccessCheckResult.Denied(
      title = "Customer Panel Restricted",
      message = "This account is registered under the ${profile.role} role. Switch to a Customer account to order food.",
      currentRoleLabel = profile.role
    )
  }

  fun evaluateShopkeeperAccess(
    isAuthenticated: Boolean,
    email: String?,
    emailVerified: Boolean,
    profile: UserProfile?,
    isProfileLoading: Boolean
  ): AccessCheckResult {
    if (!isAuthenticated) {
      return AccessCheckResult.Denied(
        title = "Merchant Sign-In Required",
        message = "Please sign in with Google and register your store to access the Shopkeeper Portal.",
        requiresSignIn = true
      )
    }
    if (isAdminEmail(email, emailVerified)) {
      return AccessCheckResult.Allowed
    }
    if (isProfileLoading) {
      return AccessCheckResult.Loading
    }
    if (profile == null || profile.userId.isBlank()) {
      return AccessCheckResult.Denied(
        title = "Shopkeeper Registration Required",
        message = "Complete merchant registration first to open the Shopkeeper Portal.",
        requiresShopkeeperRegistration = true
      )
    }
    if (canAccessShopkeeperPanel(isAuthenticated, email, emailVerified, profile)) {
      return AccessCheckResult.Allowed
    }
    return AccessCheckResult.Denied(
      title = "Shopkeeper Portal Restricted",
      message = "This account is registered as a ${profile.role}. Only verified merchants or admins can access store management.",
      currentRoleLabel = profile.role
    )
  }

  fun canAssignRole(
    requestedRole: String,
    isAuthenticated: Boolean,
    email: String?,
    emailVerified: Boolean
  ): Boolean {
    if (!isAuthenticated) return false
    val normalized = requestedRole.trim().uppercase()
    if (normalized in setOf(UserRole.CUSTOMER.name, UserRole.SHOPKEEPER.name, UserRole.MERCHANT.name)) {
      return true
    }
    if (normalized == UserRole.ADMIN.name) {
      return isAdminEmail(email, emailVerified)
    }
    return false
  }
}
