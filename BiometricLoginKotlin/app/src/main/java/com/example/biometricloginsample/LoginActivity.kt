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
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.example.biometricloginsample.databinding.ActivityLoginBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uepay.authenticate.biometric.CryptographyManager
import com.uepay.authenticate.biometric.Fingerprint
import java.lang.reflect.Field
import java.lang.reflect.Method


/**
 * 1) after entering "valid" username and password, login button becomes enabled
 * 2) User clicks biometrics?
 *   - a) if no template exists, then ask user to register template
 *   - b) if template exists, ask user to confirm by entering username & password
 */
class LoginActivity : AppCompatActivity() {
    private val TAG = "LoginActivity"
    private lateinit var biometricPrompt: BiometricPrompt
    private val cryptographyManager = CryptographyManager()
    private val ciphertextWrapper
        get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            applicationContext,
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )
    private lateinit var binding: ActivityLoginBinding
    private val loginWithPasswordViewModel by viewModels<LoginWithPasswordViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.useBiometrics.setOnClickListener {
            if (ciphertextWrapper != null) {
                showBiometricPromptForDecryption()
            } else {
                startActivity(Intent(this, EnableBiometricLoginActivity::class.java))
            }
        }
        if (ciphertextWrapper == null) {
            setupForLoginWithPassword()
        }


    }

    /**
     * The logic is kept inside onResume instead of onCreate so that authorizing biometrics takes
     * immediate effect.
     */
    override fun onResume() {
        super.onResume()

        if (ciphertextWrapper != null) {
            if (SampleAppUser.fakeToken == null) {
                showBiometricPromptForDecryption()
            } else {
                // The user has already logged in, so proceed to the rest of the app
                // this is a todo for you, the developer
                updateApp(getString(R.string.already_signedin))
            }
        }
    }

    // BIOMETRICS SECTION

    private fun showBiometricPromptForDecryption() {
        ciphertextWrapper?.let { textWrapper ->
            val canAuthenticate =
                BiometricManager.from(applicationContext).canAuthenticate(BIOMETRIC_STRONG)
            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val secretKeyName = getString(R.string.secret_key_name)
                val cipher = cryptographyManager.getInitializedCipherForDecryption(
                    secretKeyName, textWrapper.initializationVector
                )
                biometricPrompt =
                    BiometricPromptUtils.createBiometricPrompt(
                        this,
                        ::decryptServerTokenFromStorage
                    )
                val promptInfo = BiometricPromptUtils.createPromptInfo(this)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }
        }
    }

    private fun decryptServerTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult) {
        ciphertextWrapper?.let { textWrapper ->

//            authResult.javaClass.declaredFields.forEach {
//                val field = (it as Field)
//                field.isAccessible = true
//                val value = field.get(authResult)
//                Log.i(TAG, "---> ${it.name}:${value}")
//            }

            authResult.cryptoObject?.cipher?.let { it ->

//                val fingerId = getFingerId(authResult)
//                Log.d(TAG, "fingerId: $fingerId")

//                val enrolledFingerprints = getEnrolledFingerprints(getFingerprintManager())
//                enrolledFingerprints.forEach { finger ->
//                    val fingerId = getFingerId(finger.javaClass)
//                    Log.d(TAG, "fingerId: $fingerId")
//                    finger.javaClass.fields.forEach {
//                        val field = (it as Field)
//                        field.isAccessible = true
//                        val value = field.get(finger)
//                        Log.i(TAG, "---> ${field.name}:${value}")
//                    }
//                    val field = finger.javaClass.getDeclaredField("mName")
//                    field.isAccessible = true
//                    val value = field.get(finger)
//                    Log.i(TAG, "3 ---> ${field.name}:${value}")
//                }
//                val typeToken: TypeToken<List<Fingerprint>> = object : TypeToken<List<Fingerprint>>() {}
//                val json = Gson().toJson(enrolledFingerprints)
//                Log.d(TAG, "json: $json")

//                val list = Gson().fromJson<List<Fingerprint>>(json, typeToken.type)
//                Log.d(TAG, "list: $list")

                var plaintext = cryptographyManager.decryptDataWithBlock(textWrapper.ciphertext, it)

                Log.d(TAG, "plaintext: $plaintext")

//                var plaintext =
//                    cryptographyManager.decryptData(textWrapper.ciphertext, it)

                SampleAppUser.fakeToken = plaintext
                // Now that you have the token, you can query server for everything else
                // the only reason we call this fakeToken is because we didn't really get it from
                // the server. In your case, you will have gotten it from the server the first time
                // and therefore, it's a real token.
//                plaintext = "biometricType=TouchID&serverRandom=5TPGNIJEWL&deviceModel=HMA-AL00&userId=526&deviceId=ffeffc3d-e1af-118e-6ebb-ecff7f5b7abf&deviceName=HUAWEI&timestamp=1662005396&cpk=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDMTYcjUKG88OllVx89ssaMoWaxoFpjkSgmIYFIDIMwBIZHEM8ALyWMQbAfxgpKydbEbKmMDzk0jxQt/e2S40Vh3TAoz/ULRJLF5Xb19ByOZCmQjiIsJDz0845ABk7LET0rzjSTPh6qPEdJBaIdqQNt3Hetqg83EViiA7zAsJgm0QIDAQAB&token=MTIzNDU2Nzg5LDUyNixudWxsLDE2NjE0MDg5NDk4NzksYW5kcm9pZA=="
//                updateApp(getString(R.string.already_signedin))
                updateApp("login success : $plaintext")
                Log.d(TAG, "plaintext: $plaintext")

                val secretKeyName = getString(R.string.secret_key_name)
                val signature = cryptographyManager.sign(secretKeyName, plaintext)
                Log.d(TAG, "signature: $signature")
                val verification = cryptographyManager.verify(secretKeyName, plaintext, signature)
                Log.d(TAG, "verify: $verification")

            }
        }
    }

    // USERNAME + PASSWORD SECTION

    private fun setupForLoginWithPassword() {
        loginWithPasswordViewModel.loginWithPasswordFormState.observe(this, Observer { formState ->
            val loginState = formState ?: return@Observer
            when (loginState) {
                is SuccessfulLoginFormState -> binding.login.isEnabled = loginState.isDataValid
                is FailedLoginFormState -> {
                    loginState.usernameError?.let { binding.username.error = getString(it) }
                    loginState.passwordError?.let { binding.password.error = getString(it) }
                }
            }
        })
        loginWithPasswordViewModel.loginResult.observe(this, Observer {
            val loginResult = it ?: return@Observer
            if (loginResult.success) {
                updateApp(
                    "You successfully signed up using password as: user " +
                            "${SampleAppUser.username} with fake token ${SampleAppUser.fakeToken}"
                )
            }
        })
        binding.username.doAfterTextChanged {
            loginWithPasswordViewModel.onLoginDataChanged(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
        }
        binding.password.doAfterTextChanged {
            loginWithPasswordViewModel.onLoginDataChanged(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
        }
        binding.password.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE ->
                    loginWithPasswordViewModel.login(
                        binding.username.text.toString(),
                        binding.password.text.toString()
                    )
            }
            false
        }
        binding.login.setOnClickListener {
            loginWithPasswordViewModel.login(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
        }
        Log.d(TAG, "Username ${SampleAppUser.username}; fake token ${SampleAppUser.fakeToken}")
    }

    private fun updateApp(successMsg: String) {
        binding.success.text = successMsg
    }

    private fun getFingerId(fingerprint: Class<Any>): Int {
        var fingerId = -1

        try {
//            val field: Field = fingerprint.getDeclaredField("mFingerprint")
//            field.isAccessible = true
//            val fingerPrint: Any = field.get(fingerprint)
            fingerprint.fields.forEach {
                val field = (it as Field)
                field.isAccessible = true
                val value = field.get(fingerprint)
                Log.i(TAG, "---> ${field}:${value}")
            }

//            val clazz = Class.forName("android.hardware.fingerprint.Fingerprint")
            return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                Log.i(TAG, "-------ANDROID Q-------")
                val supClass = fingerprint.superclass
                val getBiometricId: Method = supClass.getDeclaredMethod("getBiometricId")
                getBiometricId.isAccessible = true
                getBiometricId.invoke(fingerprint).let { it as Int }
            } else {
                Log.i(TAG, "------- ANDROID M-P-------")
                val getFingerId: Method = fingerprint.getDeclaredMethod("getFingerId")
                getFingerId.invoke(fingerprint).let { it as Int }
            }
            Log.d(TAG, "fingerId=$fingerId")
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
        return fingerId
    }

    private fun getEnrolledFingerprints(fm: FingerprintManager): List<Object> {
        try {
            return fm.let { it ->
                val method = it.javaClass.getDeclaredMethod("getEnrolledFingerprints")
                method.invoke(it).let { it as List<Object> }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return listOf()
    }

    private fun getFingerprintManager(): FingerprintManager {
        return getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
    }
}