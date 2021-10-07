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
import androidx.core.os.BuildCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.samples.appinstaller.store.LibraryRepository
import com.samples.appinstaller.workers.InstallWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import java.io.IOException
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageInstallerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    private val notificationRepository: NotificationRepository
) {
    private val packageManager: PackageManager
        get() = context.packageManager

    private val packageInstaller: PackageInstaller
        get() = context.packageManager.packageInstaller

    private val _pendingUserActions: Queue<Intent> = LinkedList()
    private val _pendingUserActionEvents = MutableSharedFlow<Intent>()
    val pendingUserActionEvents: SharedFlow<Intent> = _pendingUserActionEvents

    private fun addPendingUserAction(packageName: String, intent: Intent) {
        _pendingUserActions.add(intent)
        runBlocking {
            _pendingUserActionEvents.emit(intent)
        }
    }

    fun removePendingUserAction(packageName: String, intent: Intent) {
        _pendingUserActions.remove(intent)
    }

    /**
     * Check if the app is allowed to install apps
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            Settings.Global.getInt(null, Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1
        }
    }

    /**
     * Check if this package name is part of our store apps
     */
    fun isAppInstallable(packageName: String): Boolean {
        return library.containsApp(packageName)
    }

    /**
     * Get app launching intent (main intent with intent filter: android.intent.action.MAIN)
     */
    fun getAppLaunchingIntent(packageName: String) =
        packageManager.getLaunchIntentForPackage(packageName)

    /**
     * Uninstall this package name
     */
    fun uninstallApp(packageName: String) {
        /**
         * We request an uninstallation from [PackageInstaller] and provides a pending intent that
         * will be send back to our app via [com.samples.appinstaller.SessionStatusReceiver] after
         * the uninstallation has started
         */
        val statusIntent = Intent(context, SessionStatusReceiver::class.java).apply {
            action = SessionStatusReceiver.UNINSTALL_ACTION
            data = Uri.fromParts("package", packageName, null)
        }

        val statusPendingIntent =
            PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_MUTABLE)
        packageInstaller.uninstall(packageName, statusPendingIntent.intentSender)
    }

    /**
     * Cancel all pending & active installs or upgrades related to this package name
     */
    suspend fun cancelInstall(packageName: String) {
        /**
         * We cancel all pending and active [InstallWorker] jobs related to [packageName]
         */
        WorkManager.getInstance(context).cancelAllWorkByTag(packageName)
        /**
         * We abandon all [PackageInstaller.Session] related to [packageName]
         */
        abandonInstallSessions(packageName)

        // We update the library to change the app status
        library.cancelInstall(packageName)
    }

    /**
     * Initialize an install session for this package name
     */
    fun installApp(packageName: String) {
        // We update the library to set this app as being installed
        onInstalling(packageName)

        // We add this install job to our WorkManager queue
        val installWorkRequest = OneTimeWorkRequestBuilder<InstallWorker>()
            .addTag(packageName)
            .setInputData(workDataOf(InstallWorker.PACKAGE_NAME_KEY to packageName))
            .build()

        WorkManager.getInstance(context).enqueue(installWorkRequest)
    }

    /**
     * Generate an install session to be created and opened by the [PackageInstaller]
     */
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

            // We update the library to set this app as being installed
            onInstalling(packageName)

            session
        } catch (e: IOException) {
            logcat(LogPriority.ERROR) { "Exception when creating session ${e.asLog()}" }
            library.cancelInstall(packageName)
            null
        }
    }

    /**
     * Abandon all [PackageInstaller.Session] related to [packageName]
     */
    private suspend fun abandonInstallSessions(packageName: String) {
        withContext(Dispatchers.IO) {
            packageInstaller.allSessions
                .filter { session -> session.appPackageName == packageName }
                .forEach { session ->
                    try {
                        packageInstaller.abandonSession(session.sessionId)
                    } catch (e: SecurityException) {
                        /**
                         * Some install sessions may have been created by other apps using
                         * [PackageInstaller] and can't be abandoned. Also after uninstalling your
                         * own app, your sessions lose owners and can't be abandoned by yourself
                         * after a reinstall. The system cleans up automatically old sessions after
                         * a certain period
                         */
                    }
                }
        }
    }

    /**
     * Create a status [Intent] that will be used to call the [SessionStatusReceiver] once an
     * install session has been processed
     */
    private fun createStatusIntent(packageName: String): Intent {
        return Intent(context, SessionStatusReceiver::class.java).apply {
            action = SessionStatusReceiver.INSTALL_ACTION

            // For convenience & to ensure a unique intent per-package:
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_RECEIVER_FOREGROUND
        }
    }

    /**
     * Create a [PendingIntent] that will hold a status [Intent] created by [createStatusIntent] to
     * callback the [SessionStatusReceiver] once an install session has been processed
     */
    fun createStatusPendingIntent(packageName: String): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            createStatusIntent(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * We update the saved [PendingIntent] (only if there's one already) to make sure the system
     * caches only the most recent install operation that requires user action
     */
    private fun updateStatusPendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE
        )
    }

    /**
     * We request a [PendingIntent] related to [packageName] from the system only if there's one
     * already. In this case, it means our pending user action intent hasn't been completed by the
     * user yet, so we bring it back to the user to complete or cancel it
     */
    private fun getCachedStatusPendingIntent(packageName: String): PendingIntent? {
        return PendingIntent.getBroadcast(
            context,
            0,
            createStatusIntent(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_NO_CREATE
        )
    }

    /**
     * We save this status intent inside a pending one to be used for later when the user
     */
    private fun saveStatusPendingIntentForLater(statusIntent: Intent) {
        val packageName = statusIntent.data!!.schemeSpecificPart
        logcat { "saveStatusPendingIntentForLater $packageName" }

        // Don't save these extras which we want to receive future changes to.
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS)
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        statusIntent.putExtra(SessionStatusReceiver.EXTRA_REDELIVER, true)

        updateStatusPendingIntent(statusIntent)
    }

    /**
     * Once an install/uninstall operation is complete, we cancel any pending status intent related
     * to the given package name (to not trigger user action once the app is resumed
     */
    private fun cancelStatusPendingIntent(packageName: String) {
        logcat { "cancelStatusPendingIntent $packageName" }
        getCachedStatusPendingIntent(packageName)?.cancel()
    }

    fun getPendingUserAction(): Intent? {
        return _pendingUserActions.peek()
    }

    fun removePendingUserAction(): Intent? {
        return _pendingUserActions.remove()
    }

    suspend fun redeliverPendingUserActions() {
        withContext(Dispatchers.IO) {
            packageInstaller.mySessions
                .filter { sessionInfo ->
                    val packageName = sessionInfo.appPackageName ?: return@filter false
                    library.containsApp(packageName)
                }
                .sortedByDescending { sessionInfo -> sessionInfo.createdMillis }
                .distinctBy { sessionInfo -> sessionInfo.appPackageName }
                .mapNotNull { sessionInfo -> sessionInfo.appPackageName }
                .also { sessions -> logcat { "Redeliver pending user actions (${sessions.size} unique sessions found)" } }
                .forEach { packageName ->
                    getCachedStatusPendingIntent(packageName)?.let { pendingIntent ->
                        try {
                            pendingIntent.send(
                                context,
                                0,
                                Intent(SessionStatusReceiver.REDELIVER_ACTION)
                            )

                            logcat { "Redelivered status intent for $packageName" }
                        } catch (ignored: CanceledException) {
                            logcat(LogPriority.ERROR) { ignored.asLog() }
                        }
                    }
                }
        }
    }

    fun onInstallPendingUserAction(packageName: String, statusIntent: Intent) {
        logcat { "onInstallPendingUserAction $packageName" }
        // We update the library to set this app as being installed
        onInstalling(packageName)

        runBlocking {
            addPendingUserAction(packageName, statusIntent)
        }

        // We save the pending user action to deliver it later to the user if the action isn't
        // completed during this activity lifecycle
        saveStatusPendingIntentForLater(statusIntent)

        // TODO: Deal with notification
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            notifyPendingUserActions()
        }
    }

    fun onUninstallPendingUserAction(packageName: String, statusIntent: Intent) {
        logcat { "onUninstallPendingUserAction $packageName" }
        // We update the library to set this app as being uninstalled
        library.addUninstall(packageName)
        // We save the pending user action to deliver it to the user
        _pendingUserActions.add(statusIntent)

        runBlocking {
            _pendingUserActionEvents.emit(statusIntent)
        }
    }

    fun notifyPendingUserActions() {
        logcat { "pendingUserActions: ${_pendingUserActions.size}" }
        if (_pendingUserActions.size > 0) {
            notificationRepository.createInstallNotification()
        }
    }

    fun cancelInstallNotification() {
        notificationRepository.cancelInstallNotification()
    }

    fun requestUserActionIfNeeded() {
        runBlocking {
            _pendingUserActions.peek()?.let { intent ->
                _pendingUserActionEvents.emit(intent)
            }
        }
    }

    fun createSessionActionObserver(sessionId: Int): SessionActionObserver {
        return SessionActionObserver(packageInstaller, sessionId)
    }

    private fun onInstalling(packageName: String) {
        logcat { "onInstalling $packageName" }
        // We update the library to set this app as being installed
        library.addInstall(packageName)
    }

    fun onInstallSuccess(packageName: String) {
        logcat { "onInstallSuccess $packageName" }
        library.completeInstall(packageName)
        cancelStatusPendingIntent(packageName)
    }

    fun onInstallFailure(packageName: String) {
        logcat { "onInstallFailure $packageName" }
        library.cancelInstall(packageName)
        cancelStatusPendingIntent(packageName)
    }

    fun onUninstallSuccess(packageName: String) {
        logcat { "onUninstallSuccess $packageName" }
        library.completeUninstall(packageName)
        cancelStatusPendingIntent(packageName)
    }

    fun onUninstallFailure(packageName: String) {
        logcat { "onUninstallFailure $packageName" }
        library.cancelUninstall(packageName)
        cancelStatusPendingIntent(packageName)
    }
}
