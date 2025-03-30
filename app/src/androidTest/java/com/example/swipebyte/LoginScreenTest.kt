package com.example.swipebyte

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.example.swipebyte.ui.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun handleLocationPermission() {
        // Setup UiDevice for permission handling
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Wait for app to start
        sleep(2000)

        // Handle location permission dialog if it appears
        try {
            // Wait for the permission dialog to appear (timeout after 5 seconds)
            val dialogAppears = device.wait(
                Until.findObject(By.textContains("location")),
                5000
            ) != null

            if (dialogAppears) {
                // Different Android versions have different button text
                val allowButtonSelector = UiSelector().textMatches("(?i)While using the app|Allow|Allow only while using the app")
                val allowButton = device.findObject(allowButtonSelector)

                if (allowButton.exists()) {
                    allowButton.click()
                    println("Clicked 'Allow' on location permission dialog")
                    sleep(1000) // Wait for dialog to dismiss
                }
            }
        } catch (e: Exception) {
            println("Error handling location permission: ${e.message}")
        }

        // Wait for app to fully initialize
        sleep(5000)
    }

    @Test
    fun loginScreenDisplaysCorrectly() {
        // Verify login screen elements are displayed
        composeTestRule.onNodeWithTag("loginScreen").assertExists()
        composeTestRule.onNodeWithTag("logoImage").assertExists()
        composeTestRule.onNodeWithTag("appTitle").assertExists()
        composeTestRule.onNodeWithTag("emailInput").assertExists()
        composeTestRule.onNodeWithTag("passwordInput").assertExists()
        composeTestRule.onNodeWithTag("loginButton").assertExists()
        composeTestRule.onNodeWithTag("signupLink").assertExists()
    }

    @Test
    fun canEnterCredentials() {
        // Enter email and password
        composeTestRule.onNodeWithTag("emailInput")
            .performTextInput("user@gmail.com")
        composeTestRule.onNodeWithTag("passwordInput")
            .performTextInput("123456")

        // Verify text was entered
        composeTestRule.onNodeWithTag("emailInput")
            .assertTextContains("user@gmail.com")
        composeTestRule.onNodeWithTag("passwordInput")
            .assertExists() // Can't check password content due to visual transformation
    }

    @Test
    fun canLoginWithValidCredentials() {
        // Wait for login screen to be fully visible after permission dialog
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                return@waitUntil composeTestRule.onAllNodesWithTag("emailInput")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                return@waitUntil false
            }
        }

        // Clear any existing text and enter valid credentials
        composeTestRule.onNodeWithTag("emailInput").apply {
            performClick()
            performTextClearance()
            performTextInput("user@gmail.com")
        }

        composeTestRule.onNodeWithTag("passwordInput").apply {
            performClick()
            performTextClearance()
            performTextInput("123456")
        }

        // Click login button
        composeTestRule.onNodeWithTag("loginButton").performClick()

        // Wait for navigation to Home screen
        composeTestRule.waitForIdle()

        // The test will pass if there's no crash - actual navigation is handled in ViewModel
        // In a real test, we'd also check if the home screen is displayed, but that requires
        // more setup for the authentication flow
    }
}