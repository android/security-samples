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

import android.content.Context
import android.content.pm.PackageManager
import com.samples.appinstaller.R
import com.samples.appinstaller.SessionStatusReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This list contains the apps available in our store. In a production app, this list would be
 * fetched from a remote server as it may be updated and not be static like here
 */
private val internalStoreAppsList = listOf(
    AppPackage(
        packageName = "com.acme.spaceshooter",
        label = "Space Shooter",
        company = "ACME Inc.",
        icon = R.drawable.ic_app_spaceshooter_foreground
    ),
    AppPackage(
        packageName = "com.champollion.pockettranslator",
        label = "Pocket Translator",
        company = "Champollion SA",
        icon = R.drawable.ic_app_pockettranslator_foreground,
    ),
    AppPackage(
        packageName = "com.echolabs.citymaker",
        label = "City Maker",
        company = "Echo Labs Ltd",
        icon = R.drawable.ic_app_citymaker_foreground,
    ),
    AppPackage(
        packageName = "com.paca.nicekart",
        label = "Nice Kart",
        company = "PACA SARL",
        icon = R.drawable.ic_app_nicekart_foreground,
    ),
)

/**
 * This repository contains the list of the store apps and their status on the current device
 * (installed, uninstalled, etc.) and acts as the source of truth for the UI. In a production app,
 * this list would be saved in a database (using Jetpack Room) as it may be too big to be kept on
 * memory
 */
@Singleton
class LibraryRepository @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        val storeApps = internalStoreAppsList
    }

    private val packageManager: PackageManager
        get() = context.packageManager

    private val _apps = MutableStateFlow(storeApps)
    val apps: StateFlow<List<AppPackage>> = _apps

    /**
     * Set containing list of package names being installed or upgraded
     */
    private val appsBeingInstalled = mutableSetOf<String>()

    /**
     * Find app by package name in the store apps list
     */
    private fun findApp(packageName: String): AppPackage? {
        return _apps.value.find { it.packageName == packageName }
    }

    /**
     * Find app by package name in the store apps list
     */
    fun containsApp(packageName: String): Boolean {
        return findApp(packageName) != null
    }

    /**
     * We load the library by listing our store apps list and updating their status if they're
     * installed or being installed (uninstalled by default if we can't find them on the device)
     */
    suspend fun loadLibrary() {
        withContext(Dispatchers.IO) {
            val installedPackages = packageManager.getInstalledPackages(0)

            val storeApps = storeApps
            val installedStoreApps = installedPackages.mapNotNull { installedPackage ->
                findApp(installedPackage.packageName)?.copy(
                    status = AppStatus.INSTALLED,
                    updatedAt = installedPackage.lastUpdateTime
                )
            }

            /**
             * We combine our store apps list and the installed apps (filtered from apps not being
             * in our store) with the apps being installed or upgraded to get the most up-to-date
             * library state
             */
            _apps.value = (installedStoreApps + storeApps).distinctBy { it.packageName }.map { app ->
                if (appsBeingInstalled.contains(app.packageName)) {
                    app.copy(
                        status = if (app.status == AppStatus.INSTALLED) {
                            AppStatus.UPGRADING
                        } else {
                            AppStatus.INSTALLING
                        }
                    )
                } else {
                    app
                }
            }.sortedBy {
                it.label
            }
        }
    }

    private fun setAppState(packageName: String, transform: (app: AppPackage) -> AppPackage) {
        _apps.value = _apps.value.map { app ->
            if (app.packageName == packageName) transform(app) else app
        }.sortedBy {
            it.label
        }
    }

    /**
     * We update our library to mark an app as being installed or upgraded
     */
    fun addInstall(packageName: String) {
        appsBeingInstalled.add(packageName)
        logcat { "$packageName is being installed or upgraded" }

        setAppState(packageName) { app ->
            app.copy(
                status = if (app.status == AppStatus.INSTALLED) {
                    AppStatus.UPGRADING
                } else {
                    AppStatus.INSTALLING
                }
            )
        }
    }

    /**
     * We update our library to mark an app as installed
     */
    fun completeInstall(packageName: String) {
        appsBeingInstalled.remove(packageName)
        logcat { "$packageName has been installed or upgraded" }

        setAppState(packageName) { app ->
            app.copy(status = AppStatus.INSTALLED, updatedAt = System.currentTimeMillis())
        }
    }

    /**
     * We update our library to mark an app as being uninstalled
     */
    fun addUninstall(packageName: String) {
        appsBeingInstalled.remove(packageName)
        logcat { "$packageName is being uninstalled" }

        setAppState(packageName) { app ->
            app.copy(status = AppStatus.UNINSTALLING)
        }
    }

    /**
     * We update our library to mark an app uninstalled
     */
    fun completeUninstall(packageName: String) {
        appsBeingInstalled.remove(packageName)
        logcat { "$packageName has been uninstalled" }

        setAppState(packageName) { app ->
            app.copy(status = AppStatus.UNINSTALLED, updatedAt = -1)
        }
    }

    /**
     * We update our library to revert the state of an app due to a install/upgrade cancellation
     */
    fun cancelInstall(packageName: String) {
        appsBeingInstalled.remove(packageName)
        logcat { "$packageName install been cancelled" }

        /**
         * We update the library to set this app as [AppStatus.UNINSTALLED] if it's a
         * [SessionStatusReceiver.INSTALL_ACTION] session otherwise [AppStatus.INSTALLED] if it's a
         * [SessionStatusReceiver.UPGRADE_ACTION]
         */
        setAppState(packageName) { app ->
            app.copy(status = if (app.updatedAt > -1) AppStatus.INSTALLED else AppStatus.UNINSTALLED)
        }
    }

    /**
     * We update our library to revert the state of an app due to a uninstall failure/cancellation
     */
    fun cancelUninstall(packageName: String) {
        appsBeingInstalled.remove(packageName)
        logcat { "$packageName uninstall been cancelled" }

        setAppState(packageName) { app ->
            app.copy(status = AppStatus.INSTALLED)
        }
    }
}
