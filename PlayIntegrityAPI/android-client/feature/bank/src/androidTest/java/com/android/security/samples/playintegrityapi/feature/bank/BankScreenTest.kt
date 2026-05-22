package com.android.security.samples.playintegrityapi.feature.bank

import androidx.activity.ComponentActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.security.samples.playintegrityapi.feature.bank.ui.BankScreen
import com.android.security.samples.playintegrityapi.feature.bank.ui.TransferUiState
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme // Updated to match your core theme import
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BankScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun getString(id: Int) = context.getString(id)

    @Test
    fun bankScreen_displaysAllExpectedElements() {
        composeTestRule.setContent {
            PiaSampleTheme {
                BankScreen(
                    accountNumber = "",
                    onAccountNumberChange = {},
                    amount = "",
                    onAmountChange = {},
                    transferState = TransferUiState.Idle,
                    isTransferEnabled = false,
                    onTransferClick = {},
                    onBackClick = {}
                )
            }
        }

        // Verify Top Bar & Header
        composeTestRule.onNodeWithText(getString(R.string.bank_top_bar_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.bank_header_subtitle)).assertIsDisplayed()

        // Verify Input Field Labels
        composeTestRule.onNodeWithText(getString(R.string.bank_label_account_number))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.bank_label_amount)).assertIsDisplayed()

        // Verify CTA
        composeTestRule.onNodeWithText(getString(R.string.bank_btn_transfer)).assertIsDisplayed()
    }

    @Test
    fun bankScreen_clickingBackButton_triggersOnBackClick() {
        var backClicked = false

        composeTestRule.setContent {
            PiaSampleTheme {
                BankScreen(
                    accountNumber = "",
                    onAccountNumberChange = {},
                    amount = "",
                    onAmountChange = {},
                    transferState = TransferUiState.Idle,
                    isTransferEnabled = false,
                    onTransferClick = {},
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(getString(com.android.security.samples.playintegrityapi.core.ui.R.string.navigate_back_content_desc))
            .performClick()
        assertTrue(backClicked)
    }

    @Test
    fun bankScreen_clickingTransferButton_triggersOnTransferClick() {
        var transferClicked = false

        composeTestRule.setContent {
            PiaSampleTheme {
                BankScreen(
                    accountNumber = "1234567890",
                    onAccountNumberChange = {},
                    amount = "50.0",
                    onAmountChange = {},
                    transferState = TransferUiState.Idle,
                    isTransferEnabled = true,
                    onTransferClick = { transferClicked = true },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.bank_btn_transfer))
            .assertIsEnabled()
            .performClick()

        assertTrue(transferClicked)
    }

    @Test
    fun bankScreen_enteringText_triggersChangeCallbacks() {
        var expectedAccountNumber = ""
        var expectedAmount = ""

        composeTestRule.setContent {
            PiaSampleTheme {
                BankScreen(
                    accountNumber = "",
                    onAccountNumberChange = { expectedAccountNumber = it },
                    amount = "",
                    onAmountChange = { expectedAmount = it },
                    transferState = TransferUiState.Idle,
                    isTransferEnabled = false,
                    onTransferClick = {},
                    onBackClick = {}
                )
            }
        }

        // Type into the Account Number field
        composeTestRule.onNodeWithText(getString(R.string.bank_label_account_number))
            .performTextInput("1234567890")
        assertEquals("1234567890", expectedAccountNumber)

        // Type into the Amount field
        composeTestRule.onNodeWithText(getString(R.string.bank_label_amount))
            .performTextInput("50.00")
        assertEquals("50.00", expectedAmount)
    }

    @Test
    fun bankScreen_transferButtonIsDisabled_whenIsTransferEnabledIsFalse() {
        composeTestRule.setContent {
            PiaSampleTheme {
                BankScreen(
                    accountNumber = "",
                    onAccountNumberChange = {},
                    amount = "",
                    onAmountChange = {},
                    transferState = TransferUiState.Idle,
                    isTransferEnabled = false,
                    onTransferClick = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.bank_btn_transfer)).assertIsNotEnabled()
    }

    @Test
    fun bankScreen_loadingState_disablesInputsAndHidesTransferText() {
        composeTestRule.setContent {
            PiaSampleTheme {
                BankScreen(
                    accountNumber = "",
                    onAccountNumberChange = {},
                    amount = "",
                    onAmountChange = {},
                    transferState = TransferUiState.Loading,
                    isTransferEnabled = true,
                    onTransferClick = {},
                    onBackClick = {}
                )
            }
        }

        // Inputs should be disabled during loading
        composeTestRule.onNodeWithText(getString(R.string.bank_label_account_number))
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText(getString(R.string.bank_label_amount)).assertIsNotEnabled()

        // Transfer text is replaced by a CircularProgressIndicator
        composeTestRule.onNodeWithText(getString(R.string.bank_btn_transfer)).assertDoesNotExist()
    }

    @Test
    fun bankScreen_successState_showsSuccessMessage() {
        composeTestRule.setContent {
            PiaSampleTheme {
                BankScreen(
                    accountNumber = "1234567890",
                    onAccountNumberChange = {},
                    amount = "50.0",
                    onAmountChange = {},
                    transferState = TransferUiState.Success,
                    isTransferEnabled = true,
                    onTransferClick = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.bank_status_success_transfer))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.bank_status_error_service_unavailable))
            .assertDoesNotExist()
    }

    @Test
    fun bankScreen_errorState_showsErrorMessage() {
        val errorMessage = getString(R.string.bank_status_error_service_unavailable)
        composeTestRule.setContent {
            PiaSampleTheme {
                BankScreen(
                    accountNumber = "1234567890",
                    onAccountNumberChange = {},
                    amount = "50.0",
                    onAmountChange = {},
                    transferState = TransferUiState.Error.Network(errorMessage),
                    isTransferEnabled = true,
                    onTransferClick = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.bank_status_success_transfer))
            .assertDoesNotExist()
    }
}