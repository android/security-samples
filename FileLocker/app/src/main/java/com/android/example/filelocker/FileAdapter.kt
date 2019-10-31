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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.example.filelocker.databinding.FileItemLayoutBinding

/**
 * A simple list adapter which displays a list of [FileEntity]s.
 */
class FileAdapter(
    private val listener: FileAdapterListener
) : ListAdapter<FileEntity, FileAdapter.FilesObjViewHolder>(FileEntityDiff) {

    interface FileAdapterListener {
        fun onFileClicked(file: FileEntity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilesObjViewHolder {
        return FilesObjViewHolder(
            FileItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            listener
        )
    }

    override fun onBindViewHolder(holder: FilesObjViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FilesObjViewHolder(
        private val binding: FileItemLayoutBinding,
        private val listener: FileAdapterListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: FileEntity) {
            binding.apply {
                fileEntity = file
                handler = listener
                executePendingBindings()
            }
        }
    }

}