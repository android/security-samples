package com.uepay.authenticate.biometric

/**
 * Created by zsg on 2022/4/28.
 * Desc:
 *
 * Copyright (c) 2022 UePay.mo All rights reserved.
 */
data class CiphertextWrapper(val ciphertext: ByteArray, val initializationVector: ByteArray)
