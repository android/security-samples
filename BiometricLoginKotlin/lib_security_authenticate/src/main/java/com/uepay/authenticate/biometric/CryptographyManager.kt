package com.uepay.authenticate.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.nio.charset.Charset
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Handles encryption and decryption
 */
interface CryptographyManager {

    fun getInitializedCipherForEncryption(keyName: String): CipherWrapper

    fun getInitializedCipherForDecryption(keyName: String, initializationVector: ByteArray): Cipher

    /**
     * The Cipher created with [getInitializedCipherForEncryption] is used here
     */
    fun encryptData(plaintext: String, cipher: Cipher): CiphertextWrapper

    /**
     * The Cipher created with [getInitializedCipherForDecryption] is used here
     */
    fun decryptData(ciphertext: ByteArray, cipher: Cipher): String

    fun sign(keyName: String, text: String): String

    fun verify(keyName: String, srcData: String, signedData: String): Boolean

    fun persistCiphertextWrapperToSharedPrefs(
        ciphertextWrapper: CiphertextWrapper,
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    )

    fun getCiphertextWrapperFromSharedPrefs(
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    ): CiphertextWrapper?

}

fun CryptographyManager(): CryptographyManager = CryptographyManagerImpl()

/**
 * To get an instance of this private CryptographyManagerImpl class, use the top-level function
 * fun CryptographyManager(): CryptographyManager = CryptographyManagerImpl()
 */
private class CryptographyManagerImpl : CryptographyManager {

    private val KEY_SIZE = 1024 //256
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA
    private val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_ECB
    private val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_RSA_OAEP

    private val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private val SIGNATURE_ALGORITEM = "SHA256withRSA/PSS"

    override fun getInitializedCipherForEncryption(keyName: String): CipherWrapper {
        val cipher = getCipher("AndroidKeyStoreBCWorkaround")
        val keyPair = getOrCreateKeyPair(keyName)
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)
        return CipherWrapper(cipher, keyPair.public.encoded)
    }

    override fun getInitializedCipherForDecryption(
        keyName: String,
        initializationVector: ByteArray
    ): Cipher {
        val cipher = getCipher("")
        val keyPair = getOrCreateKeyPair(keyName)
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
        return cipher
    }

    override fun encryptData(plaintext: String, cipher: Cipher): CiphertextWrapper {
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charset.forName("UTF-8")))
        return CiphertextWrapper(ciphertext, cipher.iv ?: byteArrayOf(0))
    }

    override fun decryptData(ciphertext: ByteArray, cipher: Cipher): String {
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charset.forName("UTF-8"))
    }

    override fun sign(keyName: String, text: String): String {
        val keyPair = getOrCreateKeyPair(keyName)
        val signature = Signature.getInstance(SIGNATURE_ALGORITEM)
        signature.initSign(keyPair.private)
        signature.update(text.toByteArray())
        return Base64.encodeToString(signature.sign(), Base64.DEFAULT)
    }

    override fun verify(keyName: String, srcData: String, signedData: String): Boolean {
        val keyPair = getOrCreateKeyPair(keyName)
        val signature = Signature.getInstance(SIGNATURE_ALGORITEM)
        signature.initVerify(keyPair.public)
        signature.update(srcData.toByteArray())
        return signature.verify(Base64.decode(signedData, Base64.DEFAULT))
    }

    private fun getCipher(provider: String): Cipher {
        return if (provider.isEmpty())
            Cipher.getInstance(TRANSFORMATION)
        else
            Cipher.getInstance(TRANSFORMATION, "AndroidKeyStoreBCWorkaround")
    }

    private fun getOrCreateSecretKey(keyName: String): SecretKey {
        // If Secretkey was previously created for that keyName, then grab and return it.
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        keyStore.getKey(keyName, null)?.let { return it as SecretKey }

        // if you reach here, then a new SecretKey must be generated for that keyName
        val paramsBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramsBuilder.apply {
            setBlockModes(ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(ENCRYPTION_PADDING)
            setKeySize(KEY_SIZE)
            setUserAuthenticationRequired(true)
        }

        val keyGenParams = paramsBuilder.build()
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(keyGenParams)
        return keyGenerator.generateKey()
    }

    private fun getOrCreateKeyPair(keyName: String): KeyPair {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        keyStore.getKey(keyName, null)?.let {
            val privateKey = it as PrivateKey
            val publicKey = keyStore.getCertificate(keyName).publicKey
            return KeyPair(publicKey, privateKey)
        }

        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(
            ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE
        )
        val keyProperties =
            KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_SIGN
        val builder = KeyGenParameterSpec.Builder(keyName, keyProperties)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
            .setUserAuthenticationRequired(false)
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setKeySize(KEY_SIZE)
        keyPairGenerator.initialize(builder.build())
        return keyPairGenerator.generateKeyPair()
    }

    override fun persistCiphertextWrapperToSharedPrefs(
        ciphertextWrapper: CiphertextWrapper,
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    ) {
        val json = Gson().toJson(ciphertextWrapper)
        context.getSharedPreferences(filename, mode).edit().putString(prefKey, json).apply()
    }

    override fun getCiphertextWrapperFromSharedPrefs(
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    ): CiphertextWrapper? {
        val json = context.getSharedPreferences(filename, mode).getString(prefKey, null)
        return Gson().fromJson(json, CiphertextWrapper::class.java)
    }
}