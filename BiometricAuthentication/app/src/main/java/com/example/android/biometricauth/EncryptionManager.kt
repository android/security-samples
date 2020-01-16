package com.example.android.biometricauth

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
    fun getOrCreateKey(name:String, requireAuthentication:Boolean)


}

/**
 * Biometrics and Cryptography are not the same thing. In fact, they are orthogonal concepts,
 * completely independent of each other. Cryptography is the art of hiding messages in plain
 * sight. In cryptography, even if you intercept my message you cannot read it because you don't
 * have the secret key. Biometrics, on the other hand, is for verifying personal identity through
 * bodily measurements. In biometrics, only my fingerprint or face or iris can unlock my door.
 *
 * Nonetheless. While biometrics doesn't need cryptography, cryptography can be augmented with
 * biometrics. To understand this, let's dive a bit into how cryptography works on Android. And
 * then we will show how biometrics can be used for an added layer of security.
 *
 * At the core of cryptography is a [Cipher], an algorithm that can be used to perform
 * encryption and decryption on data. To apply a cipher in a meaningful way, you need a special
 * variable called a [SecreKey]. Only someone with the secret key can use the cipher to decrypt
 * your data. On Android, secret keys are usually kept in a secure system called the Android
 * Keystore. Different OEMs implement the Keystore differently. But the basic idea is to keep the
 * keys in a place where third-party apps cannot reach -- a secure memory location sometimes
 * referred to as the Trusted Execution Environment (TEE) or the Secure Element (SE) that is only
 * accessible to the framework.
 *
 * When you ask the Keystore to create a key for you, it never actually gives you the key. That's
because the [SecretKey] material is never allowed to leave the secure area. The actual process
goes like this:
 * 1. Your app asks the Keystore for a key
 * 2. The Keystore creates the key in the secure memory location
 * 3. The Keystore returns an ID to your app
 * 4. When your app wants to perform encryption, it aks the Keystore to do it.
 * 5. The keystore takes in the plaintext and the key ID, and returns the ciphertext (i.e.
 * encrypted data)
 * 6. When your app wants to perform decryption, the Keystore takes in the ciphertext and the key
 * ID and returns the plaintext (i.e. decrypted data)
 *
 *
 *
 *
 * to be meaningful, a cipher needs a
 * [SecretKey] that it uses to
 * obfuscate the data.
 *
 * At the technical level, a [Cipher] requires a [SecretKey] to perform encryption.
 * In fact, the Biometrics Library doesn't do anything
 * for cryptography.
 * This biometric library doesn't really have much to do with cryptography. The two con
 * Cipher does the encryption/decryption
 * Cipher needs a SecretKey
 * SecretKey lives in KeyStore and so we must load the KeyStore
 */
private class EncryptionManagerImpl: EncryptionManager{

}