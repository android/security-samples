package com.uepay.authenticate.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
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
     * The Cipher created with [getInitializedCipherForEncryption] is used here
     * for longer data above KeySize / 8 - 11
     */
    fun encryptDataWithBlock(plaintext: String, cipher: Cipher): CiphertextWrapper

    /**
     * The Cipher created with [getInitializedCipherForDecryption] is used here
     */
    fun decryptData(ciphertext: ByteArray, cipher: Cipher): String

    /**
     * The Cipher created with [getInitializedCipherForDecryption] is used here
     * for longer ciphertext above KeySize / 8
     */
    fun decryptDataWithBlock(ciphertext: ByteArray, cipher: Cipher): String

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
    private val RESERVE_BYTES = 11
    private val DECRYPT_BLOCK: Int = KEY_SIZE / 8
    private val ENCRYPT_BLOCK = DECRYPT_BLOCK - RESERVE_BYTES

    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA
    private val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_ECB
    private val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1

    private val TRANSFORMATION = "RSA/ECB/PKCS1Padding" // "RSA/None/PKCS1Padding" //""RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private val SIGNATURE_ALGORITEM = "SHA256withRSA" //"SHA256withRSA/PSS"

    override fun getInitializedCipherForEncryption(keyName: String): CipherWrapper {
        var cipher = getCipher()

        try {
            val key = getOrCreateSecretKey(keyName+"aes")
            Cipher.getInstance("AES/CBC/NoPadding","AndroidKeyStoreBCWorkaround").init(Cipher.ENCRYPT_MODE, key)
            Log.e("tag","SecretKey encrypt mode ---> ")
        } catch (e: InvalidKeyException){
            Log.e("tag","SecretKey Encryption InvalidKeyException ---> ")
            e.printStackTrace()
        }

        try {
            val keyPair = getOrCreateKeyPair(keyName)
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)
            return CipherWrapper(cipher, keyPair.public.encoded)
        } catch (e: InvalidKeyException){
            Log.e("tag","InvalidKeyException ---> ")
            e.printStackTrace()
            throw e
        }
        return CipherWrapper(cipher, byteArrayOf(0))
    }

    override fun getInitializedCipherForDecryption(keyName: String,initializationVector: ByteArray): Cipher {
        val cipher = getCipher()

        try {
            val key = getOrCreateSecretKey(keyName+"aes")
            Cipher.getInstance("AES/CBC/NoPadding","AndroidKeyStoreBCWorkaround").init(Cipher.DECRYPT_MODE, key)
            Log.e("tag","SecretKey decrypt mode ---> ")
        } catch (e: InvalidKeyException){
            Log.e("tag","SecretKey Decryption InvalidKeyException ---> ")
            e.printStackTrace()
        }

        try {
            val keyPair = getOrCreateKeyPair(keyName)
            cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
            return cipher
        } catch (e: InvalidKeyException){
            Log.e("tag","InvalidKeyException ---> ")
            e.printStackTrace()
            throw e
        }

        return cipher
    }

    override fun encryptData(plaintext: String, cipher: Cipher): CiphertextWrapper {
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charset.forName("UTF-8")))
        return CiphertextWrapper(ciphertext, cipher.iv ?: byteArrayOf(0))
    }

    override fun encryptDataWithBlock(plaintext: String, cipher: Cipher): CiphertextWrapper {
        val dataLength = plaintext.toByteArray().size
        var blockCount = (dataLength / ENCRYPT_BLOCK)
        if ((dataLength % ENCRYPT_BLOCK) != 0) {
            blockCount += 1;
        }

        val bos = ByteArrayOutputStream(blockCount * ENCRYPT_BLOCK)
        try {
            var offset = 0
            while (offset < plaintext.length) {
                var inputLen: Int = plaintext.length - offset
                if (inputLen > ENCRYPT_BLOCK) {
                    inputLen = ENCRYPT_BLOCK
                }
                val encryptedBlock =
                    cipher.doFinal(plaintext.toByteArray(Charset.forName("UTF-8")),offset,inputLen)
                bos.write(encryptedBlock)
                offset += ENCRYPT_BLOCK
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bos.close()
        }
        return CiphertextWrapper(bos.toByteArray(), cipher.iv ?: byteArrayOf(0))
    }

    override fun decryptData(ciphertext: ByteArray, cipher: Cipher): String {
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charset.forName("UTF-8"))
    }

    override fun decryptDataWithBlock(ciphertext: ByteArray, cipher: Cipher): String {
        var blockCount = (ciphertext.size / DECRYPT_BLOCK)
        if ((ciphertext.size % DECRYPT_BLOCK) != 0) {
            blockCount += 1
        }
        val bos = ByteArrayOutputStream(blockCount * DECRYPT_BLOCK)
        try {
            var offset = 0
            while (offset < ciphertext.size) {
                var inputLen: Int = ciphertext.size - offset
                if (inputLen > DECRYPT_BLOCK) {
                    inputLen = DECRYPT_BLOCK
                }
                val decryptedBlock = cipher.doFinal(ciphertext, offset, inputLen)
                bos.write(decryptedBlock)
                offset += DECRYPT_BLOCK
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bos.close()
        }
        return String(bos.toByteArray(), Charset.forName("UTF-8"))
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

    private fun getCipher(provider: String = "AndroidKeyStoreBCWorkaround"): Cipher {
        return Cipher.getInstance(TRANSFORMATION, provider)
    }

    private fun getOrCreateSecretKey(keyName: String): SecretKey {
        // If Secretkey was previously created for that keyName, then grab and return it.
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        keyStore.getKey(keyName, null)?.let {
            Log.e("tag","SecretKey create success ---> ")
            return it as SecretKey }

        // if you reach here, then a new SecretKey must be generated for that keyName
        val paramsBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramsBuilder.apply {
            setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
            setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                setInvalidatedByBiometricEnrollment(true)
            }
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
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
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