package com.example

import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.security.AccessCheckResult
import com.example.data.security.AccessControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAccessControlTest {

  private fun verifiedCustomer() = UserProfile(
    userId = "cust_123",
    fullName = "Ayesha Khan",
    email = "ayesha@example.com",
    phoneNumber = "+923001234567",
    role = UserRole.CUSTOMER.name,
    city = "Islamabad"
  )

  private fun verifiedShopkeeper() = UserProfile(
    userId = "shop_456",
    fullName = "Bilal Ahmed",
    businessName = "Bilal Karahi",
    email = "bilal@example.com",
    phoneNumber = "+923007654321",
    role = UserRole.SHOPKEEPER.name,
    city = "Lahore"
  )

  @Test
  fun unauthenticatedUserIsDeniedAdminAndCustomerPanels() {
    val adminCheck = AccessControl.evaluateAdminAccess(
      isAuthenticated = false,
      email = null,
      emailVerified = false,
      profile = null,
      isProfileLoading = false
    )
    val customerCheck = AccessControl.evaluateCustomerAccess(
      isAuthenticated = false,
      email = null,
      emailVerified = false,
      profile = null,
      isProfileLoading = false
    )

    assertTrue(adminCheck is AccessCheckResult.Denied)
    assertTrue((adminCheck as AccessCheckResult.Denied).requiresSignIn)
    assertTrue(customerCheck is AccessCheckResult.Denied)
    assertTrue((customerCheck as AccessCheckResult.Denied).requiresSignIn)
  }

  @Test
  fun verifiedCustomerCanAccessCustomerPanelButIsDeniedAdminPanel() {
    val customer = verifiedCustomer()
    val customerCheck = AccessControl.evaluateCustomerAccess(
      isAuthenticated = true,
      email = customer.email,
      emailVerified = true,
      profile = customer,
      isProfileLoading = false
    )
    val adminCheck = AccessControl.evaluateAdminAccess(
      isAuthenticated = true,
      email = customer.email,
      emailVerified = true,
      profile = customer,
      isProfileLoading = false
    )

    assertEquals(AccessCheckResult.Allowed, customerCheck)
    assertTrue(adminCheck is AccessCheckResult.Denied)
  }

  @Test
  fun shopkeeperIsDeniedCustomerPanelAndAdminPanel() {
    val shopkeeper = verifiedShopkeeper()
    val customerCheck = AccessControl.evaluateCustomerAccess(
      isAuthenticated = true,
      email = shopkeeper.email,
      emailVerified = true,
      profile = shopkeeper,
      isProfileLoading = false
    )
    val adminCheck = AccessControl.evaluateAdminAccess(
      isAuthenticated = true,
      email = shopkeeper.email,
      emailVerified = true,
      profile = shopkeeper,
      isProfileLoading = false
    )
    val shopkeeperCheck = AccessControl.evaluateShopkeeperAccess(
      isAuthenticated = true,
      email = shopkeeper.email,
      emailVerified = true,
      profile = shopkeeper,
      isProfileLoading = false
    )

    assertTrue(customerCheck is AccessCheckResult.Denied)
    assertTrue(adminCheck is AccessCheckResult.Denied)
    assertEquals(AccessCheckResult.Allowed, shopkeeperCheck)
  }

  @Test
  fun verifiedPrimaryAdminEmailIsAllowedAdminAndPanelOversight() {
    val adminCheck = AccessControl.evaluateAdminAccess(
      isAuthenticated = true,
      email = "Moaviah760@gmail.com",
      emailVerified = true,
      profile = null,
      isProfileLoading = false
    )
    val customerCheck = AccessControl.evaluateCustomerAccess(
      isAuthenticated = true,
      email = "moaviah760@gmail.com",
      emailVerified = true,
      profile = null,
      isProfileLoading = false
    )

    assertEquals(AccessCheckResult.Allowed, adminCheck)
    assertEquals(AccessCheckResult.Allowed, customerCheck)
  }

  @Test
  fun unverifiedAdminEmailIsDeniedAdminAccess() {
    val adminCheck = AccessControl.evaluateAdminAccess(
      isAuthenticated = true,
      email = "moaviah760@gmail.com",
      emailVerified = false,
      profile = null,
      isProfileLoading = false
    )

    assertTrue(adminCheck is AccessCheckResult.Denied)
    assertFalse(AccessControl.isAdminEmail("moaviah760@gmail.com", emailVerified = false))
  }

  @Test
  fun signedInUserWithoutProfileMustCompleteCustomerRegistrationBeforeCustomerPanel() {
    val customerCheck = AccessControl.evaluateCustomerAccess(
      isAuthenticated = true,
      email = "newuser@example.com",
      emailVerified = true,
      profile = null,
      isProfileLoading = false
    )

    assertTrue(customerCheck is AccessCheckResult.Denied)
    assertTrue((customerCheck as AccessCheckResult.Denied).requiresCustomerRegistration)
  }

  @Test
  fun nonAdminCannotSelfAssignAdminRole() {
    assertFalse(
      AccessControl.canAssignRole(
        requestedRole = UserRole.ADMIN.name,
        isAuthenticated = true,
        email = "attacker@example.com",
        emailVerified = true
      )
    )
    assertTrue(
      AccessControl.canAssignRole(
        requestedRole = UserRole.CUSTOMER.name,
        isAuthenticated = true,
        email = "customer@example.com",
        emailVerified = true
      )
    )
    assertTrue(
      AccessControl.canAssignRole(
        requestedRole = UserRole.ADMIN.name,
        isAuthenticated = true,
        email = "moaviah760@gmail.com",
        emailVerified = true
      )
    )
  }
}
