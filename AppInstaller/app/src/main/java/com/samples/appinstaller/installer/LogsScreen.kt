package com.samples.appinstaller.installer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.samples.appinstaller.AppViewModel
import com.samples.appinstaller.R
import com.samples.appinstaller.Route
import com.samples.appinstaller.database.ActionLog
import com.samples.appinstaller.database.PackageAction

@Composable
fun LogsScreen(navController: NavController, viewModel: AppViewModel) {
    LaunchedEffect(viewModel.canInstallPackages) {
        if (!viewModel.canInstallPackages) {
            navController.navigate(Route.Permission.id)
        }
    }

    val logs = emptyList<ActionLog>()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.refresh_library))
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    icon = { Icon(Route.Store.icon, contentDescription = null) },
                    label = { Text(Route.Store.title) },
                    selected = true,
                    onClick = {
                        navController.navigate(Route.Store.id)
                    }
                )
                BottomNavigationItem(
                    icon = { Icon(Route.Logs.icon, contentDescription = null) },
                    label = { Text(Route.Settings.title) },
                    selected = true,
                    onClick = {}
                )
                BottomNavigationItem(
                    icon = { Icon(Route.Settings.icon, contentDescription = null) },
                    label = { Text(Route.Settings.title) },
                    selected = false,
                    onClick = {
                        navController.navigate(Route.Settings.id)
                    }
                )
            }
        },
        content = { innerPadding ->
            LazyColumn(Modifier.padding(innerPadding)) {
                items(logs) { log ->
                    ListItem(
                        text = { Text(log.packageName) },
                        secondaryText = { Text(log.toString()) }
                    )
                }
            }
        }
    )
}