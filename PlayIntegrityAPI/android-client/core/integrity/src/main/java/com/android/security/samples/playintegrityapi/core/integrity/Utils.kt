package com.android.security.samples.playintegrityapi.core.integrity

import android.util.Base64
import java.security.MessageDigest

object Utils {
    /**
     * Computes an SHA-256 hash of the input string and encodes it as a
     * Base64 URL-safe, unpadded string.
     */
    fun generateSha256Hash(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }

}