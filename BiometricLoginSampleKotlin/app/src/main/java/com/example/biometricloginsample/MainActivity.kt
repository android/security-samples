/*
 * Copyright (C) 2020 Google Inc. All Rights Reserved.
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
 * limitations under the License.
 */
package com.example.biometricloginsample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders


/**
 * 1) after entering "valid" username and password, login button becomes enabled
 * 2) User clicks biometrics?
 *   - a) if no template exists, then ask user to register template
 *   - b) if template exists, ask user to confirm by entering username & password
 */
class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    private lateinit var loginWithPasswordViewModel: LoginWithPasswordViewModel

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var encryptedServerTokenWrapper: CiphertextWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate is called")

    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        cryptographyManager = CryptographyManager()
        val ciphertextWrapper = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            applicationContext,
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )

        if (ciphertextWrapper != null) {
            if (SampleAppUser.fakeToken == null) {
                encryptedServerTokenWrapper = ciphertextWrapper
                showBiometricPromptForDecryption()
            } else {
                // The user has already logged in, so proceed to the rest of the app
                // this is a todo for you, the developer
                updateApp(getString(R.string.already_signedin))
            }
        } else {
            loginWithPasswordManager()
        }

        val useBiometricsView = findViewById<AppCompatTextView>(R.id.use_biometrics)
        useBiometricsView.setOnClickListener {
            if (ciphertextWrapper != null) {
                showBiometricPromptForDecryption()
            } else {
                startActivity(Intent(this, EnableBiometricLoginActivity::class.java))
            }
        }
    }


    // BIOMETRICS SECTION

    private fun showBiometricPromptForDecryption() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = getString(R.string.secret_key_name)
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName, encryptedServerTokenWrapper.initializationVector
            )
            biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(this, ::decryptServerTokenFromStorage)
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun decryptServerTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult) {
        authResult?.cryptoObject?.cipher?.apply {
            val plaintext =
                cryptographyManager.decryptData(encryptedServerTokenWrapper.ciphertext, this)
            SampleAppUser.fakeToken = plaintext
            // now that you have the token, you can query server for everything else
            // the only reason we call this fakeToken is because we didn't really get it from
            // the server. In your case, you will have gotten it from the server the first time
            // and therefore, it's a real token.

            updateApp(getString(R.string.already_signedin))
        }
    }

    // USERNAME + PASSWORD SECTION

    private fun loginWithPasswordManager() {

        val usernameView = findViewById<AppCompatEditText>(R.id.username)
        val passwordView = findViewById<AppCompatEditText>(R.id.password)
        val loginButton = findViewById<AppCompatButton>(R.id.login)

        loginWithPasswordViewModel =
            ViewModelProviders.of(this).get(LoginWithPasswordViewModel::class.java)
        loginWithPasswordViewModel.loginWithPasswordFormState.observe(this, Observer {
            val loginState = it ?: return@Observer

            loginButton.isEnabled = loginState.isDataValid
            loginState.usernameError?.let { usernameView.error = getString(it) }
            loginState.passwordError?.let { passwordView.error = getString(it) }
        })

        usernameView.afterTextChanged {
            loginWithPasswordViewModel.onLoginDataChanged(
                usernameView.text.toString(),
                passwordView.text.toString()
            )
        }

        passwordView.apply {
            afterTextChanged {
                loginWithPasswordViewModel.onLoginDataChanged(
                    usernameView.text.toString(),
                    passwordView.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginWithPassword(
                            usernameView.text.toString(),
                            passwordView.text.toString()
                        )
                }
                false
            }
        }
        loginButton.setOnClickListener {
            loginWithPassword(usernameView.text.toString(), passwordView.text.toString())
        }

        Log.d(TAG, "Username ${SampleAppUser.username}; fake token ${SampleAppUser.fakeToken}")
    }

    private fun loginWithPassword(username: String, password: String) {
        val succeeded = loginWithPasswordViewModel.login(username, password)
        if (succeeded) {
            updateApp(
                "You successfully signed up using password as: user " +
                        "${SampleAppUser.username} with fake token ${SampleAppUser.fakeToken}"
            )
        }
    }

    private fun updateApp(s: String) {
        val successView = findViewById<AppCompatTextView>(R.id.success)
        successView.text = s
    }
}

//Extension functions

fun AppCompatEditText.afterTextChanged(task: () -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            task()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    })
}
