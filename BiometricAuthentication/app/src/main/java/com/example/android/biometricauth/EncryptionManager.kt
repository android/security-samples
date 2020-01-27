package com.example.android.biometricauth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.KeyStoreException
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

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

interface EncryptionManager {
    /**
     * This method first gets/generates a Secretkey and then initializes the Cipher with the key.
     * [ENCRYPT_MODE][Cipher.ENCRYPT_MODE] is used.
     */
    fun getInitializedCipherForEncryption(keyName: String):Cipher

    /**
     * This method first gets/generates a Secretkey and then initializes the Cipher with the key.
     * [DECRYPT_MODE][Cipher.DECRYPT_MODE] is used.
     */
    fun getInitializedCipherForDecryption(keyName:String):Cipher

    /**
     * The Cipher created with [getInitializedCipherForEncryption] is used here
     */
    fun encryptData(plaintext:String,cipher:Cipher):String

    /**
     * The Cipher created with [getInitializedCipherForDecryption] is used here
     */
    fun decryptData(ciphertext:String,cipher:Cipher):String

    /* instance creator */
    companion object{
        fun create():EncryptionManager{
            return EncryptionManagerImpl()
        }
    }

}

private class EncryptionManagerImpl: EncryptionManager{

    val TAG = "EncryptionManager"
    val ANDROID_KEYSTORE = "AndroidKeyStore"

    override fun getInitializedCipherForEncryption(keyName: String): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey(keyName,true)
        cipher.init(Cipher.ENCRYPT_MODE,secretKey)
        return cipher
    }

    override fun getInitializedCipherForDecryption(keyName: String): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey(keyName,true)
        cipher.init(Cipher.DECRYPT_MODE,secretKey)
        return cipher
    }

    override fun encryptData(plaintext: String, cipher: Cipher): String {
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charset.defaultCharset()))
        return Arrays.toString(ciphertext)
    }

    override fun decryptData(ciphertext: String, cipher: Cipher): String {
        val plaintext = cipher.doFinal(ciphertext.toByteArray(Charset.defaultCharset()))
        return Arrays.toString(plaintext)
    }

    private fun getCipher():Cipher{
        val transformation = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/" +
                "${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        return Cipher.getInstance(transformation)
    }

    private fun getOrCreateSecretKey(keyName: String, requireAuthentication: Boolean):SecretKey {
        // If Secretkey was previously created for that keyName, then grab and return it.
        try{
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null) // Keystore must be loaded before it can be accessed
            return keyStore.getKey(keyName,null) as SecretKey
        }catch(e: KeyStoreException){
            // keyName didn't match a SecretKey. Do nothing. Except maybe log it.
            Log.d(TAG,"$keyName didn't match a SecretKey")
        }

        // if you reach here, then a new SecretKey must be generated for that keyName
        val paramsBuilder = KeyGenParameterSpec.Builder(keyName,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        paramsBuilder.apply {
            setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            setUserAuthenticationRequired(true)
        }

        val keyGenParams=paramsBuilder.build()
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE)
        keyGenerator.init(keyGenParams)
        return keyGenerator.generateKey()
    }



}