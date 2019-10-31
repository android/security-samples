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
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.android.example.filelocker.databinding.FragmentListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

private const val ENCRYPTED_PREFS_FILE_NAME = "default_prefs"
private const val ENCRYPTED_PREFS_PASSWORD_KEY = "key_prefs_password"

class ListFragment : Fragment(), FileAdapter.FileAdapterListener {


    private lateinit var binding: FragmentListBinding

    private val sharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            ENCRYPTED_PREFS_FILE_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            requireContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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

        val adapter = FileAdapter(this)

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

    override fun onFileClicked(file: FileEntity) {
        onEncryptedFileClicked(file)
    }

    private fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_list_add_item -> {
                findNavController().navigate(
                    ListFragmentDirections.actionListFragmentToEditFragment("")
                )
                true
            }
            R.id.menu_list_password -> {
                if (getPassword() == null) showSetPasswordDialog() else showResetPasswordDialog()
                true
            }
            else -> false
        }
    }

    private fun showSetPasswordDialog() {
        buildPasswordDialog(title = R.string.dialog_set_password_title) {
            setPassword(getPassword(), it)
        }.show()
    }

    private fun showResetPasswordDialog() {
        buildResetPasswordDialog { current, new ->
            setPassword(current, new)
        }.show()
    }

    private fun getPassword(): String? {
        return sharedPreferences.getString(
            ENCRYPTED_PREFS_PASSWORD_KEY,
            null
        )
    }

    private fun setPassword(current: String?, new: String?) {
        if (current != getPassword()) {
            showSnackbar(R.string.error_current_password_incorrect)
            return
        }

        if (new.isNullOrBlank()) {
            sharedPreferences.edit().putString(ENCRYPTED_PREFS_PASSWORD_KEY, null).apply()
            showSnackbar(R.string.message_password_cleared)
        } else {
            sharedPreferences.edit().putString(ENCRYPTED_PREFS_PASSWORD_KEY, new).apply()
            showSnackbar(R.string.message_password_set)
        }
    }

    private fun showSnackbar(@StringRes messageRes: Int) {
        Snackbar.make(binding.coordinator, messageRes, Snackbar.LENGTH_LONG).show()
    }

    private fun onEncryptedFileClicked(file: FileEntity) {
        if (getPassword() == null) {
            editFile(file)
        } else {
            buildPasswordDialog {
                if (it == getPassword()) {
                    editFile(file)
                } else {
                    showSnackbar(R.string.error_incorrect_password)
                }
            }.show()
        }
    }

    private fun editFile(file: FileEntity) {
        findNavController().navigate(
            ListFragmentDirections
                .actionListFragmentToEditFragment(file.title)
        )
    }

    private fun buildPasswordDialog(
        @StringRes title: Int = R.string.dialog_password_title,
        onPositiveClicked: (value: String) -> Unit
    ): AlertDialog {
        val view = View.inflate(requireContext(), R.layout.alert_dialog_password_layout, null)
        val editTextView: EditText = view.findViewById(R.id.password_input_edit_text)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.dialog_password_positive_button) { _, _ ->
                onPositiveClicked(editTextView.text.toString())
            }
            .setNegativeButton(R.string.dialog_password_negative_button) { _, _ -> }
            .create()
    }

    private fun buildResetPasswordDialog(
        @StringRes title: Int = R.string.dialog_new_password_title,
        onPositiveClicked: (current: String, new: String) -> Unit
    ): AlertDialog {
        val view = View.inflate(requireContext(), R.layout.alert_dialog_reset_password_layout, null)
        val passwordEditTextView: EditText = view.findViewById(R.id.password_input_edit_text)
        val newPasswordEditTextView: EditText = view.findViewById(R.id.new_password_input_edit_text)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.dialog_new_password_positive_button) { _, _ ->
                onPositiveClicked(
                    passwordEditTextView.text.toString(),
                    newPasswordEditTextView.text.toString()
                )
            }
            .setNegativeButton(R.string.dialog_new_password_negative_button) { _, _ -> }
            .create()
    }
}