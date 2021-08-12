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
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.os.BuildCompat
import com.samples.appinstaller.store.AppPackage
import com.samples.appinstaller.store.AppStatus
import com.samples.appinstaller.store.SampleApps
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val TAG = this::class.java.simpleName
        const val EXTRA_RECEIVED_TIME = "extra_received_time"

    }
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

    private fun updateAppItem(packageName: String, status: AppStatus) {
        _apps.value = _apps.value.map { app ->
            if(app.name == packageName) app.copy(status = status) else app
        }
    }

    private val pendingInstalls = emptySet<String>()
    private val pendingInstallUserActions: Queue<Intent> = LinkedList()
    private val lastInstallEventTimestamps = mutableMapOf<String, Long>()

    fun recordInstallEvent(packageName: String) {
        lastInstallEventTimestamps[packageName] = System.currentTimeMillis()
    }

//    data class InstallEvent(val type: InstallEventType, val packageName: String)
//
//    enum class InstallEventType {
//        INSTALLING, INSTALL_SUCCESS, INSTALL_FAILURE, UNINSTALL_SUCCESS, UNINSTALL_FAILURE
//    }
//
//    private val _installEvents = MutableSharedFlow<InstallEvent>()
//    val installEvents: SharedFlow<InstallEvent> = _installEvents

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

            updateAppItem(packageName, AppStatus.INSTALLING)
//            _installEvents.emit(InstallEvent(InstallEventType.INSTALLING, packageName))
//            onPending(packageName)
            session
        } catch (e: IOException) {
            Log.e(TAG, "Exception when creating session", e)
            updateAppItem(packageName, AppStatus.UNINSTALLED)
//            _installEvents.emit(InstallEvent(InstallEventType.INSTALL_FAILURE, packageName))
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
                    } catch (e: SecurityException) { }
                }
        }
    }

    /**
     * Uninstall app
     */
    fun uninstallApp(packageName: String) {
        val statusIntent = Intent("UNINSTALL_INTENT_NAME").apply {
            setPackage(context.packageName)
        }

        val statusPendingIntent = PendingIntent.getBroadcast(context, 0, statusIntent, 0)
        context.packageManager.packageInstaller.uninstall(
            packageName,
            statusPendingIntent.intentSender
        )
    }

    fun cacheStatusPendingIntent(statusIntent: Intent) {
        val packageName = statusIntent.data?.schemeSpecificPart ?: return
        Log.d(TAG, "Caching status intent for $packageName")
        // Don't save these extras which we want to receive future changes to.
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS)
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        statusIntent.putExtra(
            SessionStatusReceiver.EXTRA_RECEIVED_TIME,
            SessionStatusReceiver.lastReceived[packageName]
        )
        SessionStatusReceiver.getStatusPendingIntent(
            context,
            statusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE
        )
    }
}