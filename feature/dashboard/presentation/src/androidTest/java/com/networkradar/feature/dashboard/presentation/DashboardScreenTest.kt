package com.networkradar.feature.dashboard.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.networkradar.core.designsystem.NetworkRadarTheme
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardFoundationText_isDisplayed() {
        composeTestRule.setContent {
            NetworkRadarTheme {
                DashboardScreen(
                    state = DashboardState(),
                    onAction = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Dashboard Foundation").assertIsDisplayed()
    }
}
