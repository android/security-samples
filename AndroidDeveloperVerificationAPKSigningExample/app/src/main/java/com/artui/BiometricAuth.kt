package com.artui

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFail: () -> Unit = {}
    ) {

        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFail()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    onFail()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear cofre")
            .setSubtitle("Use biometria ou senha do dispositivo")
            .setAllowedAuthenticators(
                BiometricPrompt.AUTHENTICATOR_BIOMETRIC_STRONG or
                BiometricPrompt.AUTHENTICATOR_DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(info)
    }
}