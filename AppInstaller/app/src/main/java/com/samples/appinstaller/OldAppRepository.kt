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
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionInfo
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class OldAppRepository(private val context: Context) {
    private val packageManager: PackageManager
        get() = context.packageManager

    private val packageInstaller: PackageInstaller
        get() = context.packageManager.packageInstaller

    fun openApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            ContextCompat.startActivity(context, it, null)
        }
    }

    suspend fun isAppInstalled(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            Settings.Global.getInt(null, Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1
        }
    }

    private suspend fun getInstallSessionsByPackage(packageName: String): List<SessionInfo> {
        return withContext(Dispatchers.IO) {
            return@withContext packageInstaller.mySessions.filter {
                // In some cases the originatingUid attached to a sessionInfo is -1 even though
                // the installerPackageName is the current app. Android loses the originatingUid
                // when the installer itself is uninstalled during a commit session
                it.originatingUid == context.applicationInfo.uid &&
                        it.appPackageName == packageName
            }
        }
    }

    suspend fun getCurrentInstallSession(packageName: String): SessionInfo? {
        return getInstallSessionsByPackage(packageName)
            // We filter to only get sessions that haven't been closed
            .filter { it.isActive }
            // We're checking if there's at least one exisiting session
            .maxByOrNull { it.updatedMillis }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun createInstallSession(appName: String, appPackage: String) =
        withContext(Dispatchers.IO) {
            val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
                setAppLabel(appName)
                setAppPackageName(appPackage)
            }

            if (BuildCompat.isAtLeastS()) {
                params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.setInstallReason(PackageManager.INSTALL_REASON_USER)
            }

            return@withContext packageInstaller.createSession(params)
        }

    suspend fun getSessionInfo(sessionId: Int) = withContext(Dispatchers.IO) {
        return@withContext packageInstaller.getSessionInfo(sessionId)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun writeAndCommitSession(
        sessionId: Int,
        apkInputStream: InputStream,
        isUpgrade: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val session = packageInstaller.openSession(sessionId)

            session.openWrite("package", 0, -1).use { destination ->
                apkInputStream.copyTo(destination)
            }

            val statusIntent = Intent()
//            val statusIntent = if (isUpgrade) {
//                Intent(UPGRADE_INTENT_NAME).apply {
//                    setPackage(context.packageName)
//                }
//            } else {
//                Intent(INSTALL_INTENT_NAME).apply {
//                    setPackage(context.packageName)
//                }
//            }

            val statusPendingIntent = PendingIntent.getBroadcast(context, 0, statusIntent, 0)
            session.commit(statusPendingIntent.intentSender)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun getInstallerPackageName(packageName: String): String? {
        return withContext(Dispatchers.IO) {
            return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                packageManager.getInstallerPackageName(packageName)
            }
        }
    }

    suspend fun getInstalledPackageMap(): Map<String, PackageInfo> {
        val appInstallerPackage = context.packageName

        return withContext(Dispatchers.IO) {
            return@withContext packageManager.getInstalledPackages(0)
                .mapNotNull {
                    if (getInstallerPackageName(it.packageName) == appInstallerPackage) {
                        it.packageName to it
                    } else {
                        null
                    }
                }
                .toMap()
        }
    }

    suspend fun getInstalledPackages(): List<PackageInfo> {
        return withContext(Dispatchers.IO) {
            return@withContext packageManager.getInstalledPackages(0)
        }
    }
}
