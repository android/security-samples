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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.lifecycleScope
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
    private var intentLaunchingJob: Job? = null

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppInstallerTheme {
                Router(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pendingInstallsJob = lifecycleScope.launch {
            viewModel.pendingInstallUserActionEvents.collect(::onPendingUserAction)
        }

        intentLaunchingJob = lifecycleScope.launch {
            viewModel.intentsToBeLaunched.collect(::startActivity)
        }
    }

    override fun onPause() {
        pendingInstallsJob?.cancel()
        intentLaunchingJob?.cancel()
        super.onPause()
    }

    private fun onPendingUserAction(statusIntent: Intent) {
        Log.d(TAG, "MainActivity onPendingUserAction")
        startActivity(statusIntent.getParcelableExtra(Intent.EXTRA_INTENT))
    }
}