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

package com.samples.appinstaller.workers
//
//import android.content.Context
//import android.content.Intent
//import androidx.core.app.NotificationCompat
//import androidx.work.CoroutineWorker
//import androidx.work.ForegroundInfo
//import androidx.work.WorkManager
//import androidx.work.WorkerParameters
//import androidx.work.workDataOf
//import com.samples.appinstaller.AppInstallerApplication
//import com.samples.appinstaller.R
//import com.samples.appinstaller.SyncEvent
//import com.samples.appinstaller.SyncEventType
//import com.samples.appinstaller.apps.InstallSession
//import com.samples.appinstaller.apps.SampleStoreDB
//import com.samples.appinstaller.store.SampleStoreDB
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.delay
//
//class OldInstallWorker(appContext: Context, workerParams: WorkerParameters) :
//    CoroutineWorker(appContext, workerParams) {
//
//    companion object {
//        const val WORKER_TAG = "install"
//        const val PACKAGE_ID_KEY = "package_id"
//        const val PACKAGE_NAME_KEY = "package_name"
//        const val PACKAGE_LABEL_KEY = "package_label"
//        const val SESSION_ID_KEY = "session_id"
//        const val CREATED_AT_KEY = "created_at"
//    }
//
//    private lateinit var appContext: AppInstallerApplication
//    private lateinit var packageName: String
//    private lateinit var packageLabel: String
//
//    override suspend fun doWork(): Result = coroutineScope {
//        appContext = applicationContext as AppInstallerApplication
//
//        val packageId = inputData.getInt(PACKAGE_ID_KEY, -1)
//        if (packageId == -1) {
//            return@coroutineScope Result.failure()
//        }
//
//        packageName =
//            inputData.getString(PACKAGE_NAME_KEY) ?: return@coroutineScope Result.failure()
//        packageLabel =
//            inputData.getString(PACKAGE_LABEL_KEY) ?: return@coroutineScope Result.failure()
//
//        // Verify the package name exists in our app database
//        if (!SampleStoreDB.containsKey(packageName)) {
//            return@coroutineScope Result.failure()
//        }
//
//        // We send a sync event to update our library UI
//        appContext.emitSyncEvent(SyncEvent(SyncEventType.INSTALLING, packageName))
//
//        // Set worker as a foreground service
//        setForeground(createForegroundInfo(packageId, packageLabel))
//
//        // Create install session and write APK to it if there's not an existing one
//        val installSession = createAndWriteInstallSession()
//
//        try {
//            // Commit session (throws an exception if session doesn't exist)
//            PackageInstallerUtils.commitSession(
//                context = appContext,
//                sessionId = installSession.sessionId,
//                intent = Intent(INSTALL_INTENT_NAME).apply {
//                    setPackage(appContext.packageName)
//                }
//            )
//
//            return@coroutineScope Result.success(
//                workDataOf(
//                    PACKAGE_NAME_KEY to installSession.packageName,
//                    SESSION_ID_KEY to installSession.sessionId,
//                    CREATED_AT_KEY to installSession.createdAt
//                )
//            )
//        } catch (e: Exception) {
//            return@coroutineScope Result.failure()
//        }
//    }
//
//    /**
//     * Creates an instance of ForegroundInfo which can be used to update the ongoing notification.
//     */
//    private fun createForegroundInfo(notificationId: Int, packageLabel: String): ForegroundInfo {
//        val id = WORKER_TAG
//        val title = appContext.getString(R.string.installing_notification_title, packageLabel)
//        val cancel = appContext.getString(R.string.installing_notification_cancel_label)
//
//        // This PendingIntent can be used to cancel the worker
//        val intent = WorkManager.getInstance(appContext).createCancelPendingIntent(getId())
//
//        // Create a Notification channel
//        WorkerUtils.createNotificationChannel(appContext)
//
//        val notification = NotificationCompat.Builder(appContext, id)
//            .setContentTitle(title)
//            .setTicker(title)
//            .setSmallIcon(R.mipmap.ic_launcher)
//            .setOngoing(true)
//            // Add the cancel action to the notification which can
//            // be used to cancel the worker
//            .addAction(android.R.drawable.ic_delete, cancel, intent)
//            .build()
//
//        return ForegroundInfo(notificationId, notification)
//    }
//
//    /**
//     * Create install session and write APK to it if there's not an existing one
//     */
//    private suspend fun createAndWriteInstallSession(): InstallSession {
//        // Get previous pendingIntent in case
//
//        // We fake a delay to show active work. This would be replaced by real APK download
//        delay(3000L)
//
//        val sessionId = PackageInstallerUtils.createInstallSession(
//            appContext,
//            packageName,
//            packageLabel
//        )
//
//        @Suppress("BlockingMethodInNonBlockingContext")
//        PackageInstallerUtils.writeSession(
//            context = appContext,
//            sessionId = sessionId,
//            apkInputStream = appContext.assets.open("$packageName.apk")
//        )
//
//        return InstallSession(packageName, sessionId)
//    }
//}
