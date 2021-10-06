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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samples.appinstaller.PackageInstallerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import logcat.logcat
import java.io.IOException

@HiltWorker
class InstallWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val installer: PackageInstallerRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val PACKAGE_NAME_KEY = "package_name"
        const val FAKE_DOWNLOADING_DELAY = 3000L
    }

    override suspend fun doWork(): Result = coroutineScope {
        // We stop the install worker if we don't have the package name as argument
        val packageName = inputData.getString(PACKAGE_NAME_KEY)
            ?: return@coroutineScope Result.failure()

        // We verify if the provided package name is part of our store apps
        if (!installer.isAppInstallable(packageName)) return@coroutineScope Result.failure()

        try {
            logcat { "Installing: $packageName" }
            // We ask PackageInstaller to create an install sesssion
            val session = installer.createInstallSession(packageName)
                ?: return@coroutineScope Result.failure()

            // We simulate a delay as installers would probably download first the APK from a remote
            // server and write the APK bytes in the system afterwards
            logcat { "Sleeping to simulate download latency" }
            delay(FAKE_DOWNLOADING_DELAY)

            // We copy our apk bytes to the install session OutputStream opened via PackageInstaller
            writeApkToSession(packageName, session)

            /**
             * We commit our session to finalize it and give it a pending intent that will be send
             * back to our app via [com.samples.appinstaller.SessionStatusReceiver] after the
             * session has been processed
             */
            logcat { "Committing session for $packageName" }
            val pendingIntent = installer.createStatusPendingIntent(packageName)
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
    private suspend fun writeApkToSession(packageName: String, session: PackageInstaller.Session) {
        withContext(Dispatchers.IO) {
            // We open the apk from our assets folder and save it to the OutputStream provided by
            // PackageInstaller to install that app
            logcat { "Writing APK ($packageName) to session (${session.parentSessionId})..." }

            session.openWrite("package", 0, -1).use { destination ->
                applicationContext.assets.open("$packageName.apk").copyTo(destination)
            }
        }
    }
}
