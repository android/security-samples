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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.android.example.filelocker.databinding.FragmentListBinding

class ListFragment : Fragment(), NoteAdapter.NoteAdapterListener {

    private lateinit var binding: FragmentListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = NoteAdapter(this)

        binding.toolbar.inflateMenu(R.menu.toolbar_list_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onMenuItemClick(menuItem)
        }
        binding.recyclerView.adapter = adapter

        // Observe this app's files directory to be displayed as a list.
        DirectoryLiveData(requireContext().filesDir).observe(this@ListFragment) { newList ->
            adapter.submitList(newList)
        }
    }

    override fun onNoteClicked(note: Note) {
        findNavController().navigate(
            ListFragmentDirections
                .actionListFragmentToEditFragment(note.title)
        )
    }

    private fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_list_add_item -> {
                findNavController().navigate(
                    ListFragmentDirections.actionListFragmentToEditFragment("")
                )
                true
            }
            else -> false
        }
    }
}