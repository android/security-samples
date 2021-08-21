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

package com.samples.appinstaller.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.samples.appinstaller.AppViewModel

@ExperimentalMaterialApi
@Composable
fun StoreScreen(viewModel: AppViewModel) {
    val apps by viewModel.apps.collectAsState()

    fun install(app: AppPackage) = viewModel.installApp(app)
    fun upgrade(app: AppPackage) = viewModel.installApp(app)
    fun uninstall(app: AppPackage) = viewModel.installApp(app)
    fun open(app: AppPackage) = viewModel.installApp(app)
    fun cancel(app: AppPackage) = viewModel.cancelInstall(app)

    LazyColumn {
        items(apps) { app ->
            AppItem(app, ::install, ::upgrade, ::uninstall, ::open, ::cancel)
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun AppItem(
    app: AppPackage,
    onInstall: (app: AppPackage) -> Unit,
    onUpgrade: (app: AppPackage) -> Unit,
    onUninstall: (app: AppPackage) -> Unit,
    onOpen: (app: AppPackage) -> Unit,
    onCancel: (app: AppPackage) -> Unit
) {
    ListItem(
        text = { Text(app.label) },
        overlineText = { Text(app.company) },
        secondaryText = {
            Column {
                Text("Installed 2 days ago")
                Spacer(Modifier.height(5.dp))

                Row(Modifier.fillMaxWidth()) {
                    when (app.status) {
                        AppStatus.UNINSTALLED -> {
                            Button(modifier = Modifier.weight(1f), onClick = { onInstall(app) }) {
                                Text("Install")
                            }
                        }
                        AppStatus.INSTALLING -> {
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), onClick = { onCancel(app) }) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), enabled = false, onClick = { }) {
                                Text("Installing")
                            }
                            Spacer(Modifier.width(5.dp))
                        }
                        AppStatus.INSTALLED -> {
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), onClick = { onUninstall(app) }) {
                                Text("Uninstall")
                            }
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), onClick = { onOpen(app) }) {
                                Text("Open")
                            }
                            Spacer(Modifier.width(5.dp))
                        }
                        AppStatus.UPGRADING -> {
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), onClick = { onCancel(app) }) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), enabled = false, onClick = { }) {
                                Text("Upgrading")
                            }
                            Spacer(Modifier.width(5.dp))
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
            }
        },
        icon = {
            Image(
                painter = painterResource(app.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3DDC84))
            )
        }

    )
    if (app.status == AppStatus.INSTALLING || app.status == AppStatus.UPGRADING) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    Divider()
}