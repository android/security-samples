package com.example.biometricloginsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

class EnableBiometricLogin : AppCompatActivity() {

    private lateinit var loginViewModel:LoginWithPasswordViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enable_biometric_login)
        val usernameView = findViewById<AppCompatEditText>(R.id.username)
        val passwordView = findViewById<AppCompatEditText>(R.id.password)
        val authorizeButton = findViewById<AppCompatButton>(R.id.authorize)
        findViewById<AppCompatButton>(R.id.cancel).setOnClickListener { finish() }
        loginViewModel = ViewModelProviders.of(this).get(LoginWithPasswordViewModel::class.java)

        loginViewModel.loginWithPasswordFormState.observe(this, Observer {
            val loginState = it?:return@Observer
            authorizeButton.isEnabled = loginState.isDataValid
            loginState.usernameError?.let{usernameView.error=getString(it)}
            loginState.passwordError?.let{passwordView.error=getString(it)}
        })

        usernameView.afterTextChanged {
            loginViewModel.onLoginDataChanged(
                usernameView.text.toString(),
                passwordView.text.toString())
        }

        passwordView.apply{
            afterTextChanged{
                loginViewModel.onLoginDataChanged(
                    usernameView.text.toString(),
                    passwordView.text.toString())
            }

            setOnEditorActionListener{_,actionId,_ ->
                when(actionId){
                    EditorInfo.IME_ACTION_DONE ->
                        loginWithPassword(usernameView.text.toString(),passwordView.text.toString())
                }
                false
            }
        }
        authorizeButton.setOnClickListener {
            loginWithPassword(usernameView.text.toString(),passwordView.text.toString())
        }
    }

    private fun loginWithPassword(username:String,password:String){
        val succeeded = loginViewModel.login(username,password)
        if(succeeded){
            getSharedPreferences(BIOMETRIC_PREFS,MODE_PRIVATE).edit()
                .putBoolean(BIOMETRICS_ENABLED,succeeded)
            Toast.makeText(this,"Biometric login enabled", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
