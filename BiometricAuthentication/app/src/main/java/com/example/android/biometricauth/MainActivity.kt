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

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

/**
 * Main entry point for the sample, showing a backpack and "Purchase" button.
 * 1. App has an EditView for user to enter text to be encrypted; a button to encrypt; a button
 * to decrypt; and a TextView to view encrypted/decrypted text.
 *
 * 2. When user opens app the EditText is non-empty but the TextView is empty
 * 3. User clicks encrypt/decrypt to make text appear on TextView.
 * 4. Option to login with password still exists
 */
class MainActivity : AppCompatActivity() {

    private lateinit var textInputView: AppCompatEditText
    private lateinit var textOutputView: AppCompatTextView
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var readyToEncrypt: Boolean = false
    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var secretKeyName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cryptographyManager = CryptographyManager.create()
        secretKeyName = getString(R.string.secret_key_name)
        biometricPrompt = createBiometricPrompt()
        promptInfo = createPromptInfo()

        textInputView = findViewById(R.id.input_view)
        textOutputView = findViewById(R.id.output_view)
        findViewById<Button>(R.id.encrypt_button).setOnClickListener { authenticateToEncryptData() }
        findViewById<Button>(R.id.decrypt_button).setOnClickListener { authenticateToDecryptData() }
    }

    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d(TAG, "$errorCode :: $errString")
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    loginWithPassword() // Because negative button says use application password
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d(TAG, "Authentication failed for an unknown reason")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Authentication was successful")
                processData(result.cryptoObject)
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        return biometricPrompt
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.prompt_info_title))
                .setSubtitle(getString(R.string.prompt_info_subtitle))
                .setDescription(getString(R.string.prompt_info_description))
                .setConfirmationRequired(false)
                .setNegativeButtonText(getString(R.string.prompt_info_use_app_password))
                // .setDeviceCredentialAllowed(true) // Allow PIN/pattern/password authentication.
                // Also note that setDeviceCredentialAllowed and setNegativeButtonText are
                // incompatible so that if you uncomment one you must comment out the other
                .build()
        return promptInfo
    }

    private fun authenticateToEncryptData() {
        readyToEncrypt = true
        if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager
                        .BIOMETRIC_SUCCESS) {
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } else {
            loginWithPassword()
        }
    }

    private fun authenticateToDecryptData() {
        readyToEncrypt = false
        if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager
                        .BIOMETRIC_SUCCESS) {
            val cipher = cryptographyManager.getInitializedCipherForDecryption(secretKeyName)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } else {
            loginWithPassword()
        }
    }

    private fun loginWithPassword() {
        Log.d(TAG, "Use app password")
        // val fragment = FingerprintAuthenticationDialogFragment()
        // fragment.setCallback(this@MainActivity)
        // fragment.show(fragmentManager, DIALOG_FRAGMENT_TAG)
    }

    private fun processData(cryptoObject: BiometricPrompt.CryptoObject?) {
        val data = if (readyToEncrypt) {
            val text = textInputView.text.toString()
            cryptographyManager.encryptData(text, cryptoObject?.cipher!!)
        } else {
            val text = textOutputView.text.toString()
            cryptographyManager.decryptData(text, cryptoObject?.cipher!!)
        }
        textOutputView.text = data
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val DIALOG_FRAGMENT_TAG = "myFragment"
        private const val KEY_NAME = "key_not_invalidated"
        private const val SECRET_MESSAGE = "Very secret message"
        private const val TAG = "MainActivity"
    }
}
