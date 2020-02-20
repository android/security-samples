package com.example.biometricloginsample

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
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
class LoginWithPasswordViewModel(application: Application) : AndroidViewModel(application){

    private val _loginForm = MutableLiveData<LoginWithPasswordFormState>()
    val loginWithPasswordFormState: LiveData<LoginWithPasswordFormState> = _loginForm

    fun onLoginDataChanged(username:String, password:String){
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginWithPasswordFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginWithPasswordFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginWithPasswordFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    fun login(username: String, password: String):Boolean {
        if(isUserNameValid(username) && isPasswordValid(password)){
            // send to your sever to login
            // your server would then asynchronously return a fakeToken, etc, which you would use
            // to get/set other data
            // you would normally store that fakeToken, etc, in a local database. But here we just
            // use an object class: SampleAppUser
            SampleAppUser.username=username
            SampleAppUser.fakeToken = "some random fakeToken"
            return true
        }
        return false
    }
}