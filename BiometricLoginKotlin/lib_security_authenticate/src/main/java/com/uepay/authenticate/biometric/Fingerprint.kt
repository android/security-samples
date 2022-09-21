package com.uepay.authenticate.biometric

import com.google.gson.annotations.SerializedName

/**
 * Created by zsg on 2022/9/22.
 * Desc:
 *
 * Copyright (c) 2022 UePay.mo All rights reserved.
 */
data class Fingerprint(
    @SerializedName("mBiometricId") var biometricId: Int,
    @SerializedName("mFingerId") var fingerId: Int,
    @SerializedName("mGroupId") var groupId: Int,
    @SerializedName("mDeviceId") var devicesId: Int,
    @SerializedName("mName") var name: String
)
