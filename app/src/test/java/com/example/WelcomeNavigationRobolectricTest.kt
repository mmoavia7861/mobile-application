package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.navigation.MealVerseRoutes
import com.example.ui.theme.MealVerseTheme
import com.example.ui.welcome.WelcomeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WelcomeNavigationRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `verify navigation route constants`() {
    assertEquals("welcome", MealVerseRoutes.WELCOME)
    assertEquals("customer_register", MealVerseRoutes.CUSTOMER_REGISTER)
    assertEquals("shopkeeper_register", MealVerseRoutes.SHOPKEEPER_REGISTER)
    assertEquals("customer_home", MealVerseRoutes.CUSTOMER_HOME)
    assertEquals("shopkeeper_dashboard", MealVerseRoutes.SHOPKEEPER_DASHBOARD)
  }

  @Test
  fun `welcome screen renders both customer and shopkeeper navigation paths`() {
    var customerNavigated = false
    var shopkeeperNavigated = false

    composeTestRule.setContent {
      MealVerseTheme {
        WelcomeScreen(
          onNavigateToCustomerRegister = { customerNavigated = true },
          onNavigateToShopkeeperRegister = { shopkeeperNavigated = true },
          onExistingUserSignedIn = {}
        )
      }
    }

    // Verify 3D animated food stage is displayed
    composeTestRule.onNodeWithTag("animated_3d_food_stage")
      .performScrollTo()
      .assertIsDisplayed()

    // Scroll to and verify both registration cards are displayed
    composeTestRule.onNodeWithTag("welcome_customer_card")
      .performScrollTo()
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag("welcome_shopkeeper_card")
      .performScrollTo()
      .assertIsDisplayed()

    // Verify Customer action button triggers customer navigation path
    composeTestRule.onNodeWithTag("welcome_customer_reg_button")
      .performScrollTo()
      .performClick()
    assertTrue("Expected customer navigation callback to be called", customerNavigated)

    // Verify Shopkeeper action button triggers shopkeeper navigation path
    composeTestRule.onNodeWithTag("welcome_shopkeeper_reg_button")
      .performScrollTo()
      .performClick()
    assertTrue("Expected shopkeeper navigation callback to be called", shopkeeperNavigated)
  }
}
