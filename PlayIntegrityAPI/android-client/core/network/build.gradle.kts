// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.android.security.samples.playintegrityapi.core.network"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt()) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "environment"
    productFlavors {
        // SECURITY WARNING: DEVELOPMENT ONLY
        // The BASE_URL definitions below use unencrypted HTTP (cleartext) to connect to the local Node.js server.
        // This is strictly for local development to avoid the friction of configuring local SSL/TLS certificates.
        // Cleartext permissions for these specific local addresses are explicitly granted in the app's
        // network_security_config.xml.
        //
        // This sample itself is not meant to be deployed as-is. When building your own production application,
        // you should avoid cleartext permissions and ensure to use secure HTTPS (https://).

        create("emulator") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/\"")
        }
        create("physical") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://localhost:3000/\"")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    api(libs.retrofit)
    api(libs.retrofit.converter.moshi)
    api(libs.moshi.kotlin)
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}