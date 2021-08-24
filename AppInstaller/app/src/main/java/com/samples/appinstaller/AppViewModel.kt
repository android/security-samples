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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samples.appinstaller.settings.SettingsRepository
import com.samples.appinstaller.store.AppPackage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    fun canInstallPackages() = repository.canInstallPackages()

    val apps = repository.apps
    val pendingInstallUserActionEvents = repository.pendingUserActionEvents
    val settings = settingsRepository.settings.data.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        AppSettings.getDefaultInstance()
    )

    private val _intentsToBeLaunched = MutableSharedFlow<Intent>()
    val intentsToBeLaunched: SharedFlow<Intent> = _intentsToBeLaunched

    init {
        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            repository.loadLibrary()
        }
    }

    fun setAutoUpdateSchedule(value: Int) {
        viewModelScope.launch {
            settingsRepository.setAutoUpdateSchedule(value)
        }
    }

    fun setUpdateAvailabilityPeriod(value: Int) {
        viewModelScope.launch {
            settingsRepository.setUpdateAvailabilityPeriod(value)
        }
    }

    fun notifyPendingInstalls() = repository.notifyPendingInstalls()

    fun getPendingUserAction() = repository.getPendingUserAction()
    fun redeliverPendingUserAction() = viewModelScope.launch {
        repository.redeliverPendingUserAction()
    }

    fun installApp(app: AppPackage) {
        repository.installApp(app.name)
    }

    fun cancelInstall(app: AppPackage) {
        viewModelScope.launch {
            repository.cancelInstall(app.name)
        }
    }

    fun uninstallApp(app: AppPackage) {
        repository.uninstallApp(app.name)
    }

    fun openApp(appPackage: AppPackage) {
        viewModelScope.launch {
            repository.getAppLaunchingIntent(appPackage.name)?.let { _intentsToBeLaunched.emit(it) }
        }
    }
}