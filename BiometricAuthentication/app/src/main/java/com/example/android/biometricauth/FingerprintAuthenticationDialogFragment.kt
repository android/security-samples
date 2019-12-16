/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.example.android.biometricauth

import android.app.DialogFragment
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.biometric.BiometricPrompt

/**
 * A dialog that lets users sign in with password.
 */
class FingerprintAuthenticationDialogFragment : DialogFragment(),
        TextView.OnEditorActionListener {

    private lateinit var passwordEditText: EditText
    private lateinit var useFingerprintFutureCheckBox: CheckBox

    private lateinit var callback: Callback
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        dialog.setTitle(getString(R.string.sign_in))
        return inflater.inflate(R.layout.fingerprint_dialog_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cancelButton: Button = view.findViewById(R.id.cancel_button)
        cancelButton.setText(R.string.cancel)
        cancelButton.setOnClickListener { dismiss() }

        passwordEditText = view.findViewById(R.id.password)
        val secondDialogButton: Button = view.findViewById(R.id.second_dialog_button)
        useFingerprintFutureCheckBox = view.findViewById(R.id.use_fingerprint_in_future_check)

        secondDialogButton.setText(R.string.ok)

        passwordEditText.setOnEditorActionListener(this)
        secondDialogButton.setOnClickListener {
            verifyPassword()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    /**
     * Checks whether the current entered password is correct, and dismisses the dialog and
     * informs the activity about the result.
     */
    private fun verifyPassword() {
        if (!checkPassword(passwordEditText.text.toString())) {
            return
        }
        if (useFingerprintFutureCheckBox.isChecked) {
            sharedPreferences.edit()
                    .putBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                            useFingerprintFutureCheckBox.isChecked)
                    .apply()
            // Re-create the key so that fingerprints including new ones are validated.
            callback.createKey(DEFAULT_KEY_NAME)
        }
        passwordEditText.setText("")
        callback.onPurchased(withBiometrics = false)
        dismiss()
    }

    /**
     * Checks if the given password is valid. Assume that the password is always correct.
     * In a real world situation, the password needs to be verified via the server.
     *
     * @param password The password String
     *
     * @return true if `password` is correct, false otherwise
     */
    private fun checkPassword(password: String) = password.isNotEmpty()


    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        return if (actionId == EditorInfo.IME_ACTION_GO) {
            verifyPassword(); true
        } else false
    }

    interface Callback {
        fun onPurchased(withBiometrics: Boolean, crypto: BiometricPrompt.CryptoObject? = null)
        fun createKey(keyName: String, invalidatedByBiometricEnrollment: Boolean = true)
    }
}
