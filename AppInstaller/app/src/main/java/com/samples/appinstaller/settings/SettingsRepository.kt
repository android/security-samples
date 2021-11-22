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
package com.samples.appinstaller.settings

import android.content.Context
import com.samples.appinstaller.AppSettings
import com.samples.appinstaller.store.PackageName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val PENDING_USER_ACTIONS = listOf(
            AppSettings.PackageActionType.PENDING_USER_INSTALLING,
            AppSettings.PackageActionType.PENDING_USER_UPGRADING,
            AppSettings.PackageActionType.PENDING_USER_UNINSTALLING,
        )
    }

    val appSettings = context.appSettings

    suspend fun setAutoUpdateSchedule(value: Int) {
        context.appSettings.updateData { currentSettings ->
            currentSettings.toBuilder().setAutoUpdateScheduleValue(value).build()
        }
    }

    suspend fun setUpdateAvailabilityPeriod(value: Int) {
        context.appSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setUpdateAvailabilityPeriodValue(value)
                .build()
        }
    }

    suspend fun removePackageAction(packageName: PackageName) {
        context.appSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .removePackageActions(packageName)
                .build()
        }
    }

    fun getPendingUserActions(): Flow<Map<String, AppSettings.PackageAction>> {
        return context.appSettings.data.map { settings ->
            settings.packageActionsMap.filterValues { action ->
                PENDING_USER_ACTIONS.contains(action.packageActionType)
            }
        }
    }

    fun hasPendingUserActions(): Boolean {
        return runBlocking {
            return@runBlocking context.appSettings.data.first().packageActionsMap.isNotEmpty()
        }
    }
}
