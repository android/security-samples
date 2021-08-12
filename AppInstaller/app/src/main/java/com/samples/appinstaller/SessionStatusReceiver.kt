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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SessionStatusReceiver : BroadcastReceiver() {
    @Inject
    lateinit var repository: AppRepository

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val userActionIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        val packageName = intent.data?.schemeSpecificPart ?: return
//        val sessionManager = SessionManager.getInstance(context)

//        if (intent.action == ACTION_REDELIVER) {
//            // When handling a redelivered status intent, need to be careful to make sure it isn't
//            // stale - ie. we may have received a newer status since the redelivery was requested.
//
//            // The time this redelivered status was originally received (if ever).
//            val receivedTime = intent.getLongExtra(EXTRA_RECEIVED_TIME, -1)
//            // The time a status was last received for this package (if ever).
//            val packageLastReceived = lastReceived[packageName]
//            if (packageLastReceived != null && packageLastReceived > receivedTime) {
//                Log.d(TAG, "Ignoring stale redelivered status intent")
//            } else {
//                Log.i(
//                    TAG, String.format(
//                        "Redelivery of status intent for sessionId=%d, packageName=%s",
//                        sessionId, packageName
//                    )
//                )
//                sessionManager.onPending(packageName)
//                if (userActionIntent != null) {
//                    sessionManager.onPendingUserAction(packageName, intent)
//                }
//            }
//            return
//        }

        Log.d(
            TAG,
            "Received sessionId=$sessionId, packageName=$packageName, "
                    + "status=${statusToString(status)}, message=$message"
        )

//        lastReceived[packageName] = System.currentTimeMillis()
        repository.recordInstallEvent(packageName)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                cacheStatusPendingIntent(context, intent)
                sessionManager.onPendingUserAction(packageName, intent)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                cancelStatusPendingIntent(context, intent)
                sessionManager.onSuccess(packageName)
            }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                cancelStatusPendingIntent(context, intent)
                sessionManager.onFailure(packageName)
            }
            else -> Log.e(TAG, "Unhandled status: $status")
        }
    }

    private fun cacheStatusPendingIntent(context: Context, statusIntent: Intent) {
        val packageName = statusIntent.data?.schemeSpecificPart ?: return
        Log.d(TAG, "Caching status intent for $packageName")
        // Don't save these extras which we want to receive future changes to.
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS)
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        statusIntent.putExtra(
            EXTRA_RECEIVED_TIME,
            lastReceived[packageName]
        )
        getStatusPendingIntent(
            context,
            statusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE
        )
    }

    private fun cancelStatusPendingIntent(context: Context, statusIntent: Intent) {
        val packageName = statusIntent.data!!.schemeSpecificPart
        Log.i(
            TAG,
            "Cancelling status pending intent for $packageName"
        )
        val pendingIntent =
            getStatusPendingIntent(context, statusIntent, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            pendingIntent.cancel()
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

    companion object {
        private val TAG = SessionStatusReceiver::class.java.simpleName
        private const val ACTION_REDELIVER = "action_redeliver"
        private const val EXTRA_RECEIVED_TIME = "extra_received_time"
        private val lastReceived: MutableMap<String, Long> = HashMap()

        private fun getStatusIntent(context: Context, packageName: String): Intent {
            val statusIntent = Intent(context, SessionStatusReceiver::class.java)
            // For convenience & to ensure a unique intent per-package:
            statusIntent.data = Uri.fromParts("package", packageName, null)
            statusIntent.flags = Intent.FLAG_RECEIVER_FOREGROUND
            return statusIntent
        }

        fun redeliverStatusIntent(context: Context, packageName: String) {
            val pendingIntent = getStatusPendingIntent(
                context,
                getStatusIntent(context, packageName), PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                try {
                    pendingIntent.send(context, 0, Intent(ACTION_REDELIVER))
                    Log.d(
                        TAG,
                        "Redelivered status intent for $packageName"
                    )
                } catch (ignored: CanceledException) {
                }
            }
        }

        fun getStatusPendingIntent(context: Context, packageName: String): PendingIntent {
            return getStatusPendingIntent(
                context,
                getStatusIntent(context, packageName),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun getStatusPendingIntent(
            context: Context,
            intent: Intent,
            flags: Int
        ): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE or flags
            )
        }

        fun noteUserActionComplete(context: Context, statusIntent: Intent) {
            Log.d(
                TAG, "Noting user action complete for " + statusIntent.data!!
                    .schemeSpecificPart
            )
            // User has acted on the user action intent, so no need to cache it any more.
            statusIntent.removeExtra(Intent.EXTRA_INTENT)
            // Don't save these extras which we want to receive future changes to.
            statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS)
            statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            getStatusPendingIntent(
                context,
                statusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE
            )
        }
    }
}
