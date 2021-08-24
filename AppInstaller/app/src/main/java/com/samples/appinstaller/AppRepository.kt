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
import android.app.PendingIntent.CanceledException
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.samples.appinstaller.store.AppPackage
import com.samples.appinstaller.store.AppStatus
import com.samples.appinstaller.store.StoreRepository
import com.samples.appinstaller.workers.InstallWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    private val TAG = this.javaClass.simpleName

    private val packageManager: PackageManager
        get() = context.packageManager

    private val packageInstaller: PackageInstaller
        get() = context.packageManager.packageInstaller

    private val _apps = MutableStateFlow(StoreRepository.apps)
    val apps: StateFlow<List<AppPackage>> = _apps

    fun getAppByName(name: String): AppPackage? {
        return apps.value.find { app -> app.name == name }
    }

    private fun updateAppState(packageName: String, transform: (app: AppPackage) -> AppPackage) {
        _apps.value = _apps.value.map { app ->
            if (app.name == packageName) transform(app) else app
        }
    }

    suspend fun loadLibrary() {
        withContext(Dispatchers.IO) {
            val installedPackages = packageManager.getInstalledPackages(0)

            _apps.value = StoreRepository.apps
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
    private val pendingUserActions: Queue<Intent> = LinkedList()
    private val _pendingUserActionEvents = MutableSharedFlow<Intent>()
    val pendingUserActionEvents: SharedFlow<Intent> = _pendingUserActionEvents

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            Settings.Global.getInt(null, Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1
        }
    }

    fun getAppLaunchingIntent(packageName: String) =
        packageManager.getLaunchIntentForPackage(packageName)

    fun uninstallApp(packageName: String) {
        val statusIntent = Intent(context, SessionStatusReceiver::class.java).apply {
            action = SessionStatusReceiver.UNINSTALL_ACTION
            data = Uri.fromParts("package", packageName, null)
        }

        val statusPendingIntent =
            PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_MUTABLE)
        packageInstaller.uninstall(packageName, statusPendingIntent.intentSender)
    }

    suspend fun cancelInstall(packageName: String) {
        appsBeingInstalled.remove(packageName)
        updateAppState(packageName) {
            it.copy(status = if (it.updatedAt > -1) AppStatus.INSTALLED else AppStatus.UNINSTALLED)
        }
        WorkManager.getInstance(context).cancelAllWorkByTag(packageName)
        abandonInstallSessions(packageName)
    }

    fun installApp(packageName: String) {
        val installWorkRequest = OneTimeWorkRequestBuilder<InstallWorker>()
            .addTag(packageName)
            .setInputData(workDataOf(InstallWorker.PACKAGE_NAME_KEY to packageName))
            .build()

        WorkManager.getInstance(context).enqueue(installWorkRequest)
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

    private suspend fun abandonInstallSessions(packageName: String) {
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

    private fun createStatusIntent(packageName: String): Intent {
        return Intent(context, SessionStatusReceiver::class.java).apply {
            action = SessionStatusReceiver.INSTALL_ACTION

            // For convenience & to ensure a unique intent per-package:
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_RECEIVER_FOREGROUND
        }
    }

    fun createStatusPendingIntent(packageName: String): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            createStatusIntent(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun updateStatusPendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE
        )
    }

    private fun getCachedStatusPendingIntent(packageName: String): PendingIntent? {
        return PendingIntent.getBroadcast(
            context,
            0,
            createStatusIntent(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_NO_CREATE
        )
    }

    private fun cacheStatusPendingIntent(statusIntent: Intent) {
        val packageName = statusIntent.data!!.schemeSpecificPart
        Log.d(TAG, "Caching status intent for $packageName")
        // Don't save these extras which we want to receive future changes to.
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS)
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        statusIntent.putExtra(SessionStatusReceiver.EXTRA_REDELIVER, true)

        updateStatusPendingIntent(statusIntent)
    }

    fun getPendingUserAction(): Intent? {
        return pendingUserActions.peek()
    }

    suspend fun redeliverPendingUserActions() {
        withContext(Dispatchers.IO) {
            packageInstaller.mySessions
                .filter {
                    val packageName = it.appPackageName ?: return@filter false
                    getAppByName(packageName) != null
                }
                .map { it.appPackageName!! }
                .distinct()
                .forEach { packageName ->
                    getCachedStatusPendingIntent(packageName)?.let { pendingIntent ->
                        try {
                            pendingIntent.send(
                                context,
                                0,
                                Intent(SessionStatusReceiver.REDELIVER_ACTION)
                            )
                            Log.d(TAG, "Redelivered status intent for $packageName")
                        } catch (ignored: CanceledException) {
                        }
                    }
                }
        }
    }

    fun onInstallPendingUserAction(packageName: String, statusIntent: Intent) {
        appsBeingInstalled.add(packageName)
        pendingUserActions.add(statusIntent)
        updateAppState(packageName) { it.copy(status = if (it.updatedAt > -1) AppStatus.UPGRADING else AppStatus.INSTALLING) }
        cacheStatusPendingIntent(statusIntent)

        GlobalScope.launch {
            _pendingUserActionEvents.emit(statusIntent)
        }

        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            notifyPendingInstalls()
        }
    }

    fun onUninstallPendingUserAction(packageName: String, statusIntent: Intent) {
        GlobalScope.launch {
            _pendingUserActionEvents.emit(statusIntent)
        }
    }

    fun notifyPendingInstalls() {
        println("pendingUserActions: ${pendingUserActions.size}")
        if (pendingUserActions.size > 0) {
            notificationRepository.createInstallNotification()
        }
    }

    private fun onInstalling(packageName: String) {
        appsBeingInstalled.add(packageName)
        updateAppState(packageName) { it.copy(status = AppStatus.INSTALLING) }
    }

    fun onInstallSuccess(packageName: String) {
        appsBeingInstalled.remove(packageName)
        updateAppState(packageName) {
            it.copy(
                status = AppStatus.INSTALLED,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun onInstallFailure(packageName: String) {
        appsBeingInstalled.remove(packageName)
        updateAppState(packageName) { it.copy(status = if (it.updatedAt > -1) AppStatus.INSTALLED else AppStatus.UNINSTALLED) }
    }

    fun onUninstallSuccess(packageName: String) {
        updateAppState(packageName) { it.copy(status = AppStatus.UNINSTALLED, updatedAt = -1) }
    }

    fun onUninstallFailure(packageName: String) {
        updateAppState(packageName) { it.copy(status = AppStatus.INSTALLED) }
    }
}