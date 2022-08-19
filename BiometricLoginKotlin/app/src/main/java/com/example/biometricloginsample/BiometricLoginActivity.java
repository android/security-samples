package com.example.biometricloginsample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.uepay.authenticate.biometric.CryptographyManager;
import com.uepay.authenticate.biometric.CryptographyManagerKt;

/**
 * Created by zsg on 2022/8/19.
 * Desc: Java代碼調用示例
 * <p>
 * Copyright (c) 2022 UePay.mo All rights reserved.
 */
public class BiometricLoginActivity extends AppCompatActivity {

    private CryptographyManager cryptographyManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cryptographyManager = CryptographyManagerKt.CryptographyManager();
        cryptographyManager.getInitializedCipherForEncryption("secretKeyName");
    }
}
