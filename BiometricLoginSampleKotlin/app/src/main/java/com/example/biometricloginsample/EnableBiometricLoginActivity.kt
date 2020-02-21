/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.example.biometricloginsample

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

class EnableBiometricLoginActivity : AppCompatActivity() {

    private val TAG = "EnableBiometricLogin"
    private lateinit var loginViewModel: LoginWithPasswordViewModel
    private lateinit var cryptographyManager: CryptographyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enable_biometric_login)
        val usernameView = findViewById<AppCompatEditText>(R.id.username)
        val passwordView = findViewById<AppCompatEditText>(R.id.password)
        val authorizeButton = findViewById<AppCompatButton>(R.id.authorize)
        findViewById<AppCompatButton>(R.id.cancel).setOnClickListener { finish() }
        loginViewModel = ViewModelProviders.of(this).get(LoginWithPasswordViewModel::class.java)

        loginViewModel.loginWithPasswordFormState.observe(this, Observer {
            val loginState = it ?: return@Observer
            authorizeButton.isEnabled = loginState.isDataValid
            loginState.usernameError?.let { usernameView.error = getString(it) }
            loginState.passwordError?.let { passwordView.error = getString(it) }
        })

        usernameView.afterTextChanged {
            loginViewModel.onLoginDataChanged(
                usernameView.text.toString(),
                passwordView.text.toString()
            )
        }

        passwordView.apply {
            afterTextChanged {
                loginViewModel.onLoginDataChanged(
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
        authorizeButton.setOnClickListener {
            loginWithPassword(usernameView.text.toString(), passwordView.text.toString())
        }
    }

    private fun loginWithPassword(username: String, password: String) {
        val succeeded = loginViewModel.login(username, password)
        if (succeeded) {
            // you need to save the userToken you got from the server. That way,
            // next time the user authenticates with biometrics, you will have the token for
            // server calls. Use BiometricPrompt.CryptoObject to guard access to the token,
            // that way, the user really must authenticate to get access to the server userToken.

            showBiometricPromptForEncryption()
        }
    }

    private fun showBiometricPromptForEncryption() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = getString(R.string.secret_key_name)
            cryptographyManager = CryptographyManager()
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            val biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(this, ::encryptAndStoreServerToken)
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun encryptAndStoreServerToken(authResult: BiometricPrompt.AuthenticationResult){
        authResult?.cryptoObject?.cipher?.apply{
            SampleAppUser.fakeToken?.let { token ->
                Log.d(TAG,"The token from server is $token")
                val encryptedServerTokenWrapper = cryptographyManager.encryptData(token,this)
                cryptographyManager.persistCiphertextWrapperToSharedPrefs(encryptedServerTokenWrapper,
                    applicationContext,
                    SHARED_PREFS_FILENAME,
                    Context.MODE_PRIVATE,
                    CIPHERTEXT_WRAPPER
                    )
            }
        }
        finish()
    }
}
