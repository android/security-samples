package com.artui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SenhaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // bloqueia a tela até autenticar
        BiometricAuth.authenticate(this,
            onSuccess = {
                setContentView(R.layout.activity_senha)
            },
            onFail = {
                finish()
            }
        )
    }
}