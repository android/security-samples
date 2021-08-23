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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private val TAG = SessionStatusReceiver::class.java.simpleName

@AndroidEntryPoint
class SessionStatusReceiver : BroadcastReceiver() {
    companion object {
        const val INSTALL_ACTION = "install_action"
        const val UNINSTALL_ACTION = "uninstall_action"
    }

    @Inject
    lateinit var repository: AppRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val packageName = intent.data?.schemeSpecificPart ?: return

        Log.d(
            TAG,
            "Received sessionId=$sessionId, packageName=$packageName, "
                    + "status=${statusToString(status)}, message=$message"
        )

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                repository.onPendingUserAction(packageName, intent)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                when (action) {
                    INSTALL_ACTION -> repository.onInstallSuccess(packageName)
                    UNINSTALL_ACTION -> repository.onUninstallSuccess(packageName)
                    else -> Log.e(TAG, "Unhandled status: $status")
                }
            }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                when (action) {
                    INSTALL_ACTION -> repository.onInstallFailure(packageName)
                    UNINSTALL_ACTION -> repository.onInstallFailure(packageName)
                    else -> Log.e(TAG, "Unhandled status: $status")
                }
            }
            // TODO: Remove branch (too many logs) and add intent filter in manifest
            else -> Log.e(TAG, "Unhandled status: $status")
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
