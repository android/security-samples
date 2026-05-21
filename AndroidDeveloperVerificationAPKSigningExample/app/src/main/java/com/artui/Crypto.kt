package com.artui

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object Crypto {

    private val key = "1234567890123456"

    fun enc(text: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(), "AES"))
        return Base64.encodeToString(cipher.doFinal(text.toByteArray()), Base64.DEFAULT)
    }

    fun dec(text: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(), "AES"))
        return String(cipher.doFinal(Base64.decode(text, Base64.DEFAULT)))
    }
}