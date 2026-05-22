package com.android.security.samples.playintegrityapi.feature.bank.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.security.samples.playintegrityapi.feature.bank.R
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme

@Composable
fun BankRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transferState = uiState.transferState
    val activity = LocalContext.current as? Activity

    if (transferState is TransferUiState.Error.Integrity && transferState.isRemediable && transferState.remediationDialogTypeCode != null) {
        AlertDialog(
            onDismissRequest = {
                // User tapped outside the dialog
                viewModel.clearRemediationState()
            },
            title = {
                Text(text = stringResource(id = R.string.bank_dialog_security_failed_title))
            },
            text = {
                Text(text = stringResource(id = R.string.bank_dialog_security_failed_text))
            },
            confirmButton = {
                Button(
                    onClick = {
                        activity?.let { viewModel.triggerRemediationDialog(it) }
                    }
                ) {
                    Text(stringResource(id = R.string.bank_dialog_btn_fix))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.clearRemediationState() }
                ) {
                    Text(stringResource(id = R.string.bank_dialog_btn_cancel))
                }
            }
        )
    }

    BankScreen(
        accountNumber = uiState.accountNumber,
        onAccountNumberChange = viewModel::updateAccountNumber,
        amount = uiState.amount,
        onAmountChange = viewModel::updateAmount,
        transferState = transferState,
        isTransferEnabled = uiState.isTransferEnabled,
        onTransferClick = viewModel::transfer,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankScreen(
    accountNumber: String,
    onAccountNumberChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    transferState: TransferUiState,
    isTransferEnabled: Boolean,
    onTransferClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isLoading = transferState is TransferUiState.Loading

    Scaffold(
        topBar = {

            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.bank_top_bar_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(com.android.security.samples.playintegrityapi.core.ui.R.string.navigate_back_content_desc)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp)
                .imePadding()
        ) {
            // Top Section (Header + Form Fields)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.bank_header_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FormField(
                        label = stringResource(id = R.string.bank_label_account_number),
                        value = accountNumber,
                        placeholder = stringResource(id = R.string.bank_placeholder_account_number),
                        onValueChange = onAccountNumberChange,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    FormField(
                        label = stringResource(id = R.string.bank_label_amount),
                        value = amount,
                        placeholder = stringResource(id = R.string.bank_placeholder_amount),
                        onValueChange = onAmountChange,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onTransferClick()
                    },
                    enabled = isTransferEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = if (isLoading) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        },
                        disabledContentColor = if (isLoading) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.bank_btn_transfer),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Show inline error ONLY if it's a general network error
                // OR if it's an integrity error that CANNOT be remediated.
                val showInlineError = transferState is TransferUiState.Error &&
                        !(transferState is TransferUiState.Error.Integrity && transferState.isRemediable)

                AnimatedVisibility(
                    visible = showInlineError,
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        StatusMessage(
                            text = (transferState as? TransferUiState.Error)?.message
                                ?: stringResource(id = R.string.bank_status_error_service_unavailable),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                AnimatedVisibility(
                    visible = transferState is TransferUiState.Success,
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        StatusMessage(
                            text = stringResource(id = R.string.bank_status_success_transfer),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.outline
            )
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun StatusMessage(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BankScreenPreview() {
    PiaSampleTheme(dynamicColor = false) {
        BankScreen(
            accountNumber = "1234567890",
            onAccountNumberChange = {},
            amount = "50.00",
            onAmountChange = {},
            transferState = TransferUiState.Idle,
            isTransferEnabled = true,
            onTransferClick = {},
            onBackClick = {}
        )
    }
}