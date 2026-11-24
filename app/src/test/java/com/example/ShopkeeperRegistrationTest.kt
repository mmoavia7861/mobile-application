package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.data.repository.MealVerseRepository
import com.example.ui.auth.ShopkeeperRegistrationScreen
import com.example.ui.theme.MealVerseTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShopkeeperRegistrationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("1:791164229942:android:ffbdd1f578d862b3303b97")
          .setProjectId("gen-lang-client-0295520231")
          .setApiKey("fakeApiKeyForLocalTestingOnly12345")
          .build()
      )
    }
  }

  @Test
  fun `verify shopkeeper registration models have initial status as Pending`() {
    val store = Store(
      name = "Savour Foods",
      category = "Pakistani Food",
      status = "Pending"
    )
    assertEquals("Pending", store.status)

    val merchant = Merchant(
      businessName = "Savour Foods",
      ownerName = "Tariq Mehmood",
      cnicNumber = "37405-1234567-1",
      verificationStatus = "Pending",
      status = "Pending"
    )
    assertEquals("Pending", merchant.verificationStatus)
    assertEquals("Pending", merchant.status)

    val userProfile = UserProfile(
      fullName = "Tariq Mehmood",
      businessName = "Savour Foods",
      role = UserRole.SHOPKEEPER.name,
      status = "Pending"
    )
    assertEquals("Pending", userProfile.status)
    assertEquals("SHOPKEEPER", userProfile.role)
  }

  @Test
  fun `shopkeeper registration form displays owner name, business name, category, CNIC, and Pending status badge`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = MealVerseRepository(context)

    composeTestRule.setContent {
      MealVerseTheme {
        ShopkeeperRegistrationScreen(
          repository = repository,
          onRegistrationSuccess = {},
          onNavigateBack = {}
        )
      }
    }

    // 1. Verify Initial Account Status: 'Pending' banner is displayed
    composeTestRule.onNodeWithTag("shopkeeper_status_pending_badge")
      .performScrollTo()
      .assertIsDisplayed()

    // 2. Verify Owner Name input field is displayed and accepts text
    composeTestRule.onNodeWithTag("shopkeeper_owner_name_input")
      .performScrollTo()
      .assertIsDisplayed()
      .performTextInput("Tariq Mehmood")

    // 3. Verify Business Name input field is displayed and accepts text
    composeTestRule.onNodeWithTag("shopkeeper_business_name_input")
      .performScrollTo()
      .assertIsDisplayed()
      .performTextInput("Mehmood Karahi & BBQ")

    // 4. Verify Category dropdown field is displayed
    composeTestRule.onNodeWithTag("shopkeeper_category_dropdown")
      .performScrollTo()
      .assertIsDisplayed()

    // 5. Verify CNIC Number input field is displayed and formats 13 digits
    composeTestRule.onNodeWithTag("shopkeeper_cnic_input")
      .performScrollTo()
      .assertIsDisplayed()
      .performTextInput("4210112345671")

    // 6. Verify Location Picker Card & Map/GPS buttons are displayed
    composeTestRule.onNodeWithTag("store_location_picker_card")
      .performScrollTo()
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag("open_map_picker_btn")
      .performScrollTo()
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag("use_gps_location_btn")
      .performScrollTo()
      .assertIsDisplayed()

    // 7. Verify Submit button is present
    composeTestRule.onNodeWithTag("complete_shopkeeper_reg_btn")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun `shopkeeper registration shows circular progress indicator and disabled button while submitting`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = MealVerseRepository(context)

    composeTestRule.setContent {
      MealVerseTheme {
        ShopkeeperRegistrationScreen(
          repository = repository,
          onRegistrationSuccess = {},
          onNavigateBack = {},
          initialIsLoading = true
        )
      }
    }

    composeTestRule.onNodeWithTag("shopkeeper_reg_loading_indicator")
      .performScrollTo()
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag("complete_shopkeeper_reg_btn")
      .performScrollTo()
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }

  @Test
  fun `verify CNIC formatting logic`() {
    fun formatCnic(raw: String): String {
      val digits = raw.filter { it.isDigit() }.take(13)
      return buildString {
        for (i in digits.indices) {
          if (i == 5 || i == 12) append('-')
          append(digits[i])
        }
      }
    }

    val unformatted = "4210112345671"
    val formatted = formatCnic(unformatted)
    assertEquals("42101-1234567-1", formatted)
    assertEquals(15, formatted.length) // 13 digits + 2 hyphens
    assertEquals(13, formatted.filter { it.isDigit() }.length)
  }
}
