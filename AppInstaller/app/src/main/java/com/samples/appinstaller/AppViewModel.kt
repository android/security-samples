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
import com.samples.appinstaller.store.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val installer: PackageInstallerRepository,
    private val library: LibraryRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    fun canInstallPackages() = installer.canInstallPackages()

    val apps = library.apps
    val pendingUserActionEvents = installer.pendingUserActionEvents
    val appSettings = settings.appSettings.data.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        AppSettings.getDefaultInstance()
    )

    private var sessionActionObserver: SessionActionObserver? = null

    private val _appsToBeOpened = MutableSharedFlow<Intent>()
    val appsToBeOpened: SharedFlow<Intent> = _appsToBeOpened

    /**
     * We load the list of installed/uninstalled apps from our store when the viewmodel is
     * initialized
     */
    init {
        refreshLibrary()
    }

    /**
     * Refresh state of installed/uninstalled apps from our store
     */
    fun refreshLibrary() {
        viewModelScope.launch {
            library.loadLibrary()
        }
    }

    /**
     * Set auto update schedule setting (how often automatic updates should happen)
     */
    fun setAutoUpdateSchedule(value: Int) {
        viewModelScope.launch {
            settings.setAutoUpdateSchedule(value)
        }
    }

    /**
     * Set update availability period setting (how long should we consider an app updatable?).
     * Keep in mind this setting is designed specifically to simulate app updates as we don't have
     * real app updates. In production, this setting isn't useful as apps get updates as often their
     * developers ship them
     */
    fun setUpdateAvailabilityPeriod(value: Int) {
        viewModelScope.launch {
            settings.setUpdateAvailabilityPeriod(value)
        }
    }

    fun notifyPendingUserActions() = installer.notifyPendingUserActions()

    fun getPendingUserAction() = installer.getPendingUserAction()
    fun redeliverPendingUserActions() = viewModelScope.launch {
        installer.redeliverPendingUserActions()
    }

    /**
     * When we launch a user action intent, we monitor its related session ID for updates
     */
    fun initSessionActionObserver(sessionId: Int) {
        logcat { "initSessionActionObserver $sessionId" }
        sessionActionObserver = installer.createSessionActionObserver(sessionId)
    }

    /**
     * Mark user action complete and start cancellation timer. If no answer is received from
     * [android.content.pm.PackageInstaller], considers current user action as dismissed by user
     */
    fun markUserActionComplete() {
        logcat { "markUserActionComplete ${sessionActionObserver?.trackedSessionId}" }
        if (sessionActionObserver != null) {
            installer.removePendingUserAction()
            sessionActionObserver?.startCancellationTimeout()
            sessionActionObserver = null
        }
    }

    /**
     * Cancel active monitoring of user action
     */
    fun cancelActiveUserAction() {
        logcat { "cancelActiveUserAction ${sessionActionObserver?.trackedSessionId}" }
        sessionActionObserver?.stop()
        sessionActionObserver = null
    }

    /**
     * Trigger an app install
     */
    fun installApp(app: AppPackage) {
        // We initialize an install session
        installer.installApp(app.packageName)
    }

    /**
     * Cancel ongoing app install
     */
    fun cancelInstall(app: AppPackage) {
        viewModelScope.launch {
            // We cancel all running install sessions related to this app
            installer.cancelInstall(app.packageName)
        }
    }

    /**
     * Trigger an app uninstall
     */
    fun uninstallApp(app: AppPackage) {
        installer.uninstallApp(app.packageName)
    }

    /**
     * Get app launching intent and send it to [MainActivity] to be launched
     */
    fun openApp(appPackage: AppPackage) {
        viewModelScope.launch {
            installer.getAppLaunchingIntent(appPackage.packageName)
                ?.let { _appsToBeOpened.emit(it) }
        }
    }
}
