package com.example.biometricloginsample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.uepay.authenticate.biometric.CryptographyManager;
import com.uepay.authenticate.biometric.CryptographyManagerKt;

/**
 * Desc: Java代碼調用示例
 */
public class BiometricLoginActivity extends AppCompatActivity {

    private CryptographyManager cryptographyManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cryptographyManager = CryptographyManagerKt.CryptographyManager();
        cryptographyManager.getInitializedCipherForEncryption("secretKeyName");
        cryptographyManager.sign("secretKeyName","hello");
    }
}
