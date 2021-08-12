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

import android.content.Context
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samples.appinstaller.AppRepository
import com.samples.appinstaller.SessionStatusReceiver
import com.samples.appinstaller.store.AppPackage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

@HiltWorker
class InstallWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AppRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val PACKAGE_NAME_KEY = "package_name"
        const val FAKE_DOWNLOADING_DELAY = 3000L
    }

    private val TAG = this.javaClass.simpleName
    private lateinit var app: AppPackage

    override suspend fun doWork(): Result = coroutineScope {
        val packageName =
            inputData.getString(PACKAGE_NAME_KEY) ?: return@coroutineScope Result.failure()
        app = repository.getAppByName(packageName) ?: return@coroutineScope Result.failure()

        try {
            Log.d(TAG, "Installing: ${app.name}")
            val session =
                repository.createInstallSession(app.name) ?: return@coroutineScope Result.failure()
            writeApkToSession(session)

            Log.d(TAG, "Sleeping to simulate download latency")
            delay(FAKE_DOWNLOADING_DELAY)

            Log.d(TAG, "Committing session for $packageName")
            val pendingIntent =
                SessionStatusReceiver.getStatusPendingIntent(applicationContext, packageName)
            session.commit(pendingIntent.intentSender)
        } catch (e: IOException) {
            e.printStackTrace()
            return@coroutineScope Result.failure()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return@coroutineScope Result.failure()
        }

        Result.success()
    }

    /**
     * Save APK in the provided session
     */
    private suspend fun writeApkToSession(session: PackageInstaller.Session) {
        Log.d(TAG, "Writing APK to session...")

        withContext(Dispatchers.IO) {
            session.openWrite("package", 0, -1).use { destination ->
                applicationContext.assets.open("${app.name}.apk").copyTo(destination)
            }
        }
    }
}
