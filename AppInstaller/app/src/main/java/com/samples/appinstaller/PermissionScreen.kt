/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.appinstaller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController

@Composable
fun PermissionScreen(navController: NavController, viewModel: AppViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var installPermission by remember { mutableStateOf(viewModel.canInstallPackages) }

    val permissionCheckerObserver = remember {
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                installPermission = viewModel.canInstallPackages
            }
        }
    }

    LaunchedEffect(installPermission) {
        if (installPermission) {
            navController.navigate(Route.Store.id)
        }
    }

    DisposableEffect(lifecycle, permissionCheckerObserver) {
        lifecycle.addObserver(permissionCheckerObserver)
        onDispose { lifecycle.removeObserver(permissionCheckerObserver) }
    }

    var openPermissionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (openPermissionDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.authorization_dialog_title)) },
            text = { Text(stringResource(R.string.authorization_dialog_description)) },
            confirmButton = {
                TextButton(onClick = { openPermissionDialog = false; requestPermission(context) }) {
                    Text(stringResource(R.string.authorization_dialog_confirm_label))
                }
            },
            onDismissRequest = { openPermissionDialog = false },
            dismissButton = {
                TextButton(onClick = { openLearnMoreLink(context) }) {
                    Text(stringResource(R.string.authorization_dialog_learn_more_label))
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.permission_description),
                    style = MaterialTheme.typography.body2
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { openPermissionDialog = true }) {
                    Text(stringResource(R.string.request_permission_label))
                }
            }
        }
    )
}

private fun requestPermission(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
    }

    context.startActivity(intent)
}

private fun openLearnMoreLink(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data =
            Uri.parse("https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller")
    }

    context.startActivity(intent)
}
