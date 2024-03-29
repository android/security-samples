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
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.android.example.filelocker.databinding.FragmentEditBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File

class EditFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    private lateinit var binding: FragmentEditBinding
    private val existingNote get() = navArgs<EditFragmentArgs>().value.note

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {
            // Encrypt and save the file when the user clicks the navigate up icon.
            toolbar.setNavigationOnClickListener {
                encryptFile()
                findNavController().navigateUp()
            }
            toolbar.inflateMenu(R.menu.toolbar_edit_menu)
            toolbar.setOnMenuItemClickListener(this@EditFragment)

            if (existingNote.title.isNotBlank()) {
                binding.titleEditText.setText(existingNote.title)
                binding.bodyEditText.setText(decryptNote(existingNote))
            }
        }

        // Encrypt and save the file when the user uses the system back button.
        requireActivity().onBackPressedDispatcher.addCallback(this, true) {
            encryptFile()
            findNavController().popBackStack()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_edit_done -> {
                // Encrypt and save the file when the user clicks the 'done' icon.
                encryptFile()
                findNavController().navigateUp()
                true
            }
            R.id.menu_edit_delete -> {
                deleteNote(existingNote)
                findNavController().navigateUp()
                true
            }
            else -> false
        }
    }

    /**
     * Encrypt a file using the title and body of this fragment's text fields.
     *
     * If an existing file is currently being edited, delete and replace it.
     */
    private fun encryptFile() {
        val title = binding.titleEditText.text.toString()
        val body = binding.bodyEditText.text.toString()

        if (title.isBlank()) return

        val newNote = Note(title, File(requireContext().filesDir, title.urlEncode()).toString())
        try {
            deleteNote(existingNote)
            val encryptedFile = getNote(newNote)
            encryptedFile.openFileOutput().use { output ->
                output.write(body.toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note ($newNote)", e)
            showSnackbar(R.string.error_unable_to_save_file)
        }
    }


    /**
     * Decrypt a note and returns the plain text String.
     */
    private fun decryptNote(note: Note): String {
        val encryptedFile = getNote(note)

        try {
            encryptedFile.openFileInput().use { input ->
                return String(input.readBytes(), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting note ($note)", e)
            showSnackbar(R.string.error_unable_to_decrypt)
            return ""
        }
    }

    /**
     * Delete a file from the directory.
     */
    private fun deleteNote(note: Note) {
        if (note.title.isBlank()) return
        val file = File(note.path)
        if (file.exists()) file.delete()
    }

    /**
     * Get an [EncryptedFile], used to encrypt and decrypt files using Jetpack Security
     */
    private fun getNote(note: Note): EncryptedFile {
        return EncryptedFile.Builder(
            File(note.path),
            requireContext(),
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    private fun showSnackbar(@StringRes messageRes: Int) {
        Snackbar.make(binding.coordinator, messageRes, Snackbar.LENGTH_LONG).show()
    }
}

private const val TAG = "EditFragment"