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
import android.content.pm.PackageInstaller
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.samples.appinstaller.store.PackageName
import com.samples.appinstaller.ui.theme.AppInstallerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import logcat.logcat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Clean before committing
        lifecycleScope.launch {
//            viewModel.trigger(this@MainActivity)
        }

        /**
         * We remove the notification if it exists as user actions will be shown inside the app
         */
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.cancelInstallNotification()
            }
        }

        /**
         * We collect pending user action intents from [AppViewModel.pendingUserActionEvents] flow
         * once the activity is resumed and cancel the collect once it leaves this state
         */
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.pendingUserActionEvents.collect(::onPendingUserActions)
            }
        }

        /**
         * We collect launching app intents from [AppViewModel.appsToBeOpened] flow once the
         * activity is resumed and cancel the collect once it leaves this state
         */
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.appsToBeOpened.collect(::startActivity)
            }
        }

        // We display the app UI
        setContent {
            AppInstallerTheme {
                Router(viewModel)
            }
        }
    }

    /**
     * This method is called before [onResume] when a new intent is received.
     * When our app is re-launched from home screen or notification, because our launch mode is
     * singleTask, any other activity in our task will be finished.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.cancelActiveUserAction()
    }

    /**
     * When [onResume] is called, we track any active user action and update our library
     */
    override fun onResume() {
        super.onResume()

        viewModel.markUserActionComplete()
    }

    /**
     * We show a notification if there are still pending user actions when the app gets stopped
     */
    override fun onStop() {
        viewModel.notifyPendingUserActions()
        super.onStop()
    }

    /**
     * Before launching the intent containing the user confirmation dialog for install/uninstall
     * operation, we start monitoring with a [SessionActionObserver] any changes from the
     * [PackageInstaller]. As there's no way to listen to the dismiss of the dialog (which is
     * different from user cancelling it), we keep track of that specific session and start a timer
     * inside [onResume] with [AppViewModel.markUserActionComplete].
     * Within a second, the [PackageInstaller] should have sent a success/failure intent to
     * [SessionStatusReceiver] otherwise it means the user has dismissed the dialog and we cancel
     * the operation and update the UI
     */
    private fun onPendingUserAction(statusIntent: Intent) {
        logcat { "onPendingUserAction: $statusIntent" }

        viewModel.getPendingUserAction()?.let { intent ->
            val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
            viewModel.initSessionActionObserver(sessionId)

            startActivity(intent.getParcelableExtra(Intent.EXTRA_INTENT))
        }
    }

    private fun onPendingUserActions(pendingUserActions: Map<PackageName, AppSettings.PackageAction>) {
        logcat { "onPendingUserActions: $packageName" }

        pendingUserActions.values.minByOrNull { it.creationTime }?.let { action ->
            viewModel.getPendingIntent(action.packageName)?.let { pendingIntent ->
                val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
                viewModel.initSessionActionObserver(packageName, sessionId)

                startActivity(pendingIntent)
            }
        }
    }
}
