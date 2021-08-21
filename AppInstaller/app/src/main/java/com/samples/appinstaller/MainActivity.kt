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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samples.appinstaller.settings.SettingsScreen
import com.samples.appinstaller.store.StoreScreen
import com.samples.appinstaller.ui.theme.AppInstallerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = MainActivity::class.java.simpleName
    private val viewModel: AppViewModel by viewModels()
    private var pendingInstallsJob: Job? = null

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppInstallerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("App Installer") }
                        )
                    },
                    content = { innerPadding -> AppContainer(viewModel, innerPadding) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pendingInstallsJob = lifecycleScope.launch {
            viewModel.pendingInstallUserActionEvents.collect(::onPendingUserAction)
        }
    }

    override fun onPause() {
        pendingInstallsJob?.cancel()
        super.onPause()
    }

    private fun onPendingUserAction(statusIntent: Intent) {
        Log.d(TAG, "MainActivity onPendingUserAction")
        startActivity(statusIntent.getParcelableExtra(Intent.EXTRA_INTENT))
    }
}

sealed class NavigationItem(val route: String, val icon: ImageVector, val title: String) {
    object Store : NavigationItem("store", Icons.Filled.Home, "Demo Store")
    object Settings : NavigationItem("settings", Icons.Filled.Settings, "Settings")
}

@ExperimentalMaterialApi
@Composable
fun AppContainer(viewModel: AppViewModel, innerPadding: PaddingValues) {
    val navController = rememberNavController()

    val items = listOf(NavigationItem.Store, NavigationItem.Settings)
    var selectedItem by remember { mutableStateOf(0) }

    Column(modifier = Modifier.padding(innerPadding)) {
        NavHost(
            modifier = Modifier.weight(1f),
            navController = navController,
            startDestination = NavigationItem.Store.route
        ) {
            composable(NavigationItem.Store.route) { StoreScreen(viewModel) }
            composable(NavigationItem.Settings.route) { SettingsScreen(viewModel) }
        }

        BottomNavigation {
            items.forEachIndexed { index, item ->
                BottomNavigationItem(
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.title) },
                    selected = selectedItem == index,
                    onClick = {
                        selectedItem = index
                        navController.navigate(item.route)
                    }
                )
            }
        }
    }
}