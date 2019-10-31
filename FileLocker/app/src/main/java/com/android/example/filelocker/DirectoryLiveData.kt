/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.filelocker

import android.os.FileObserver
import androidx.lifecycle.LiveData
import java.io.File

/**
 * A LiveData which observes and emits values when the list of files in [observationDir] changes.
 */
class DirectoryLiveData(
    private val observationDir: File
) : LiveData<List<FileEntity>>() {

    @Suppress("deprecation")
    private val observer = object : FileObserver(observationDir.path) {
        override fun onEvent(event: Int, path: String?) {
            dispatchFilesChanged()
        }
    }

    private fun dispatchFilesChanged() {
        postValue(observationDir.listFiles()?.map { FileEntity(it.name.urlDecode(), it.path) }
            ?: emptyList())
    }

    override fun onActive() {
        super.onActive()
        dispatchFilesChanged()
        observer.startWatching()
    }

    override fun onInactive() {
        super.onInactive()
        observer.stopWatching()
    }
}