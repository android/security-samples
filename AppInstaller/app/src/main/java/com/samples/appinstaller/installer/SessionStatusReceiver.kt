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
package com.samples.appinstaller.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import dagger.hilt.android.AndroidEntryPoint
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject

@AndroidEntryPoint
class SessionStatusReceiver : BroadcastReceiver() {
    companion object {
        const val INSTALL_ACTION = "install_action"
        const val UPGRADE_ACTION = "upgrade_action"
        const val UNINSTALL_ACTION = "uninstall_action"
        const val REDELIVER_ACTION = "redeliver_action"

        const val EXTRA_REDELIVER = "extra_redelivered"
        const val EXTRA_CREATION_TIME = "extra_creation_time"
    }

    @Inject
    lateinit var installer: PackageInstallerRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val packageName = intent.data?.schemeSpecificPart ?: return
        val isRedelivered = intent.getBooleanExtra(EXTRA_REDELIVER, false)

        /**
         * Redelivered intents are cached intents that the user hasn't interacted yet with
         */
//        if (isRedelivered && status < 0) {
        if (isRedelivered) {
            logcat { "This is a redelivery for $packageName" }
//
//            if(installer.isPendingUserActionObsolete(packageName, creationTime)) {
//                logcat { "Redelivery is cancelled for $packageName" }
//            } else {
//                logcat { "Redelivery is done for $packageName" }
//                return installer.onInstallPendingUserAction(packageName, intent)
//            }
        }

        logcat {
            "Received sessionId=$sessionId, packageName=$packageName, status=${statusToString(status)}, message=$message"
        }

        when (status) {
            /**
             * When the system requires a user action to confirm or cancel an action
             */
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                when (action) {
                    INSTALL_ACTION -> {
                        if(installer.isSessionValid(packageName, sessionId)) {
                            installer.saveStatusPendingIntentForLater(intent)
                            installer.onInstallPendingUserAction(packageName, intent)
                        }
                    }
                    UNINSTALL_ACTION -> {
                        installer.onUninstallPendingUserAction(packageName, intent)
                    }
                    else -> logcat(LogPriority.ERROR) { "Unhandled status: $status" }
                }
            }
            /**
             * When the system successfully install or uninstall a package
             */
            PackageInstaller.STATUS_SUCCESS -> {
                when (action) {
                    INSTALL_ACTION -> {
                        installer.completeAction("onInstallSuccess", packageName)
                    }
                    UNINSTALL_ACTION -> {
                        installer.completeAction("onUninstallSuccess", packageName)
                    }
                    else -> logcat(LogPriority.ERROR) { "Unhandled status: $status" }
                }
            }
            /**
             * When the system fails to install or uninstall a package
             */
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                when (action) {
                    INSTALL_ACTION -> {
                        installer.completeAction("onInstallFailure", packageName)
                    }
                    UNINSTALL_ACTION -> {
                        installer.completeAction("onUninstallFailure", packageName)
                    }
                    else -> logcat(LogPriority.ERROR) { "Unhandled failure status: $status" }
                }
            }

            else -> logcat(LogPriority.ERROR) { "Unknown status: $status" }
        }
    }

    private fun statusToString(status: Int): String {
        return when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> "STATUS_PENDING_USER_ACTION"
            PackageInstaller.STATUS_SUCCESS -> "STATUS_SUCCESS"
            PackageInstaller.STATUS_FAILURE -> "STATUS_FAILURE"
            PackageInstaller.STATUS_FAILURE_ABORTED -> "STATUS_FAILURE_ABORTED"
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "STATUS_FAILURE_BLOCKED"
            PackageInstaller.STATUS_FAILURE_CONFLICT -> "STATUS_FAILURE_CONFLICT"
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "STATUS_FAILURE_INCOMPATIBLE"
            PackageInstaller.STATUS_FAILURE_INVALID -> "STATUS_FAILURE_INVALID"
            PackageInstaller.STATUS_FAILURE_STORAGE -> "STATUS_FAILURE_STORAGE"
            else -> "UNKNOWN_STATUS"
        }
    }
}