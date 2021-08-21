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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.os.BuildCompat
import com.samples.appinstaller.store.AppPackage
import com.samples.appinstaller.store.AppStatus
import com.samples.appinstaller.store.SampleApps
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = this.javaClass.simpleName

    private val packageManager: PackageManager
        get() = context.packageManager

    private val packageInstaller: PackageInstaller
        get() = context.packageManager.packageInstaller

    private val _apps = MutableStateFlow(SampleApps)
    val apps: StateFlow<List<AppPackage>> = _apps

    fun getAppByName(name: String): AppPackage? {
        return apps.value.find { app -> app.name == name }
    }

    suspend fun loadLibrary() {
        withContext(Dispatchers.IO) {
            val installedPackages = packageManager.getInstalledPackages(0)

            _apps.value = SampleApps
                .map { app ->
                    val installedPackage = installedPackages.find { it.packageName == app.name }

                    if (installedPackage != null) {
                        app.copy(
                            status = AppStatus.INSTALLED,
                            updatedAt = installedPackage.lastUpdateTime
                        )
                    } else {
                        app
                    }
                }
                .map { app ->
                    if (appsBeingInstalled.contains(app.name)) {
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
                }
        }
    }

    private val appsBeingInstalled = mutableSetOf<String>()
    val pendingInstallUserActionEvents = MutableSharedFlow<Intent>()

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            Settings.Global.getInt(null, Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1
        }
    }

    fun createInstallSession(packageName: String): PackageInstaller.Session? {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
            .apply {
                setAppPackageName(packageName)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setInstallReason(PackageManager.INSTALL_REASON_USER)
                }

                if (BuildCompat.isAtLeastS()) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

        return try {
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            onInstalling(packageName)
            session
        } catch (e: IOException) {
            Log.e(TAG, "Exception when creating session", e)
            onInstallFailure(packageName)
            null
        }
    }

    suspend fun abandonInstallSessions(packageName: String) {
        withContext(Dispatchers.IO) {
            packageInstaller.allSessions
                .filter { session -> session.appPackageName == packageName }
                .forEach { session ->
                    try {
                        packageInstaller.abandonSession(session.sessionId)
                    } catch (e: SecurityException) {
                    }
                }
        }
    }

    fun uninstallApp(packageName: String) {
        val statusIntent = Intent("UNINSTALL_INTENT_NAME").apply {
            setPackage(context.packageName)
        }

        val statusPendingIntent =
            PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_MUTABLE)
        packageInstaller.uninstall(packageName, statusPendingIntent.intentSender)
    }

    fun getAppLaunchingIntent(packageName: String) = packageManager.getLaunchIntentForPackage(packageName)

    private fun getStatusIntent(packageName: String): Intent {
        val statusIntent = Intent(context, SessionStatusReceiver::class.java)
        // For convenience & to ensure a unique intent per-package:
        statusIntent.data = Uri.fromParts("package", packageName, null)
        statusIntent.flags = Intent.FLAG_RECEIVER_FOREGROUND
        return statusIntent
    }

    fun getStatusPendingIntent(packageName: String): PendingIntent {
        return getStatusPendingIntent(
            getStatusIntent(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getStatusPendingIntent(
        intent: Intent,
        flags: Int
    ): PendingIntent {
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE or flags)
    }

    private fun cacheStatusPendingIntent(statusIntent: Intent) {
        val packageName = statusIntent.data!!.schemeSpecificPart
        Log.d(TAG, "Caching status intent for $packageName")
        // Don't save these extras which we want to receive future changes to.
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS)
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        getStatusPendingIntent(
            statusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE
        )
    }

    fun onPendingUserAction(packageName: String, statusIntent: Intent) {
        cacheStatusPendingIntent(statusIntent)
        GlobalScope.launch {
            pendingInstallUserActionEvents.emit(statusIntent)
        }
    }

    fun onInstalling(packageName: String) {
        appsBeingInstalled.add(packageName)
        _apps.value = _apps.value.map { app ->
            if (app.name == packageName) app.copy(status = AppStatus.INSTALLING) else app
        }
    }

    fun onInstallSuccess(packageName: String) {
        appsBeingInstalled.remove(packageName)
        _apps.value = _apps.value.map { app ->
            if (app.name == packageName) {
                app.copy(status = AppStatus.INSTALLED, updatedAt = System.currentTimeMillis())
            } else app
        }
    }

    fun onInstallFailure(packageName: String) {
        appsBeingInstalled.remove(packageName)
        _apps.value = _apps.value.map { app ->
            if (app.name == packageName) {
                app.copy(status = if (app.updatedAt > -1) AppStatus.INSTALLED else AppStatus.UNINSTALLED)
            } else {
                app
            }
        }
    }
}