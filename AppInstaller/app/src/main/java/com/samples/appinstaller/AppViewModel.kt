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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.samples.appinstaller.settings.appSettings
import com.samples.appinstaller.store.AppPackage
import com.samples.appinstaller.workers.InstallWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {
    val isPermissionGranted: Boolean
        get() = repository.canInstallPackages()

    val settings: LiveData<AppSettings> = context.appSettings.data.asLiveData()
    val apps: StateFlow<List<AppPackage>> = repository.apps

//    init {
//        viewModelScope.launch {
//            repository.installEvents
//                .collect { installEvent ->
//                    _apps.value = _apps.value.map { app ->
//                        if(app.name == installEvent.packageName) {
//                            when(installEvent.type) {
//                                AppRepository.InstallEventType.INSTALLING -> {
//                                    app.copy(status = AppStatus.INSTALLING)
//                                }
//                                AppRepository.InstallEventType.INSTALL_SUCCESS -> {
//                                    app.copy(status = AppStatus.INSTALLED)
//                                }
//                                AppRepository.InstallEventType.INSTALL_FAILURE -> {
//                                    app.copy(status = AppStatus.UNINSTALLED)
//                                }
//                                AppRepository.InstallEventType.UNINSTALL_SUCCESS -> {
//                                    app.copy(status = AppStatus.UNINSTALLED)
//                                }
//                                AppRepository.InstallEventType.UNINSTALL_FAILURE -> {
//                                    app.copy(status = AppStatus.INSTALLED)
//                                }
//                            }
//                        } else {
//                            app
//                        }
//                    }
//                }
//        }
//    }

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