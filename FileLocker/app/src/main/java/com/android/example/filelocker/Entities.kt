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

import androidx.recyclerview.widget.DiffUtil
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * A data object which represents a locally encrypted file.
 */
data class FileEntity(
    val title: String,
    val path: String
)

object FileEntityDiff : DiffUtil.ItemCallback<FileEntity>() {
    override fun areItemsTheSame(oldItem: FileEntity, newItem: FileEntity) = oldItem == newItem
    override fun areContentsTheSame(oldItem: FileEntity, newItem: FileEntity) = oldItem == newItem
}

/**
 * Extension method to decode a URL encoded a string.
 */
fun String.urlDecode():String = URLDecoder.decode(this, "UTF-8")

/**
 * Extension method to URL encode a string.
 */
fun String.urlEncode():String = URLEncoder.encode(this, "UTF-8")