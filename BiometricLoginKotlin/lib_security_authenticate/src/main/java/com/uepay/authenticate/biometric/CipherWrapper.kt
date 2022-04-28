package com.uepay.authenticate.biometric

import java.security.PublicKey
import javax.crypto.Cipher

/**
 * Created by zsg on 2022/4/28.
 * Desc:
 *
 * Copyright (c) 2022 UePay.mo All rights reserved.
 */
data class CipherWrapper(val cipher: Cipher, val publicKey: PublicKey)
