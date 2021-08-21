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
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.samples.appinstaller.settings.appSettings
import com.samples.appinstaller.store.AppPackage
import com.samples.appinstaller.workers.InstallWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {
    fun canInstallPackages() = repository.canInstallPackages()

    val settings: LiveData<AppSettings> = context.appSettings.data.asLiveData()
    val apps: StateFlow<List<AppPackage>> = repository.apps
    val pendingInstallUserActionEvents = repository.pendingInstallUserActionEvents

    /**
     * Install app by creating an install session and write the app's apk in it.
     */
    fun installApp(app: AppPackage) {
        val installWorkRequest = OneTimeWorkRequestBuilder<InstallWorker>()
            .addTag(app.name)
            .setInputData(workDataOf(InstallWorker.PACKAGE_NAME_KEY to app.name))
            .build()

        WorkManager.getInstance(context).enqueue(installWorkRequest)
    }

    fun cancelInstall(app: AppPackage) {
        WorkManager.getInstance(context).cancelAllWorkByTag(app.name)
        viewModelScope.launch {
            repository.abandonInstallSessions(app.name)
        }
    }
}