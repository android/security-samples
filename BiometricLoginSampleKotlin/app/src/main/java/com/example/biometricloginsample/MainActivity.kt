package com.example.biometricloginsample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
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

    private lateinit var loginWithPasswordViewModel:LoginWithPasswordViewModel
    private lateinit var successView:AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val usernameView = findViewById<AppCompatEditText>(R.id.username)
        val passwordView = findViewById<AppCompatEditText>(R.id.password)
        val loginButton = findViewById<AppCompatButton>(R.id.login)
        successView = findViewById<AppCompatTextView>(R.id.success)

        loginWithPasswordManager(usernameView,passwordView,loginButton)

        val useBiometricsView = findViewById<AppCompatTextView>(R.id.use_biometrics)
        useBiometricsView.setOnClickListener{
            val isBiometricsAuthorized = getSharedPreferences(BIOMETRIC_PREFS,MODE_PRIVATE)
                .getBoolean(BIOMETRICS_ENABLED,false)
            if(isBiometricsAuthorized){
                showBiometricPrompt()
            }else{
                startActivity(Intent(this, EnableBiometricLogin::class.java))
            }
        }

    }

    // BIOMETRICS SECTION

    private fun showBiometricPrompt() {


    }

    // USERNAME + PASSWORD SECTION

    private fun loginWithPasswordManager(
        usernameView: AppCompatEditText,
        passwordView: AppCompatEditText,
        loginButton: AppCompatButton
    ) {
        loginWithPasswordViewModel = ViewModelProviders.of(this).get(LoginWithPasswordViewModel::class.java)
        loginWithPasswordViewModel.loginWithPasswordFormState.observe(this, Observer{
            val loginState = it ?: return@Observer

            loginButton.isEnabled = loginState.isDataValid
            loginState.usernameError?.let { usernameView.error = getString(it)}
            loginState.passwordError?.let { passwordView.error = getString(it) }
        })

        usernameView.afterTextChanged {
            loginWithPasswordViewModel.onLoginDataChanged(
                usernameView.text.toString(),
                passwordView.text.toString())
        }

        passwordView.apply{
            afterTextChanged{
                loginWithPasswordViewModel.onLoginDataChanged(
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
        loginButton.setOnClickListener {
            loginWithPassword(usernameView.text.toString(),passwordView.text.toString())
        }

        Log.d(TAG,"Username ${SampleAppUser.username}; fake token ${SampleAppUser.fakeToken}")
    }

    private fun loginWithPassword(username:String,password:String){
        val succeeded = loginWithPasswordViewModel.login(username,password)
        if(succeeded){
            successView.text = "You successfully signed up using password as: user " +
                    "${SampleAppUser.username} with fake token ${SampleAppUser.fakeToken}"
        }
    }
}

//Extension functions

fun AppCompatEditText.afterTextChanged(task:() -> Unit){
    this.addTextChangedListener(object: TextWatcher {
        override fun afterTextChanged(editable: Editable?){
            task()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    })
}
