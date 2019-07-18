
Android FingerprintDialog Sample (Kotlin)
=========================================

A sample that demonstrates to use registered fingerprints to authenticate the user in your app

Introduction
------------

This sample demonstrates how you can use registered fingerprints in your app to authenticate the
user before proceeding some actions such as purchasing an item.

First you need to create a symmetric key in the Android Key Store using [KeyGenerator][1]
which can be only be used after the user has authenticated with fingerprint and pass
a [KeyGenParameterSpec][2].

By setting [KeyGenParameterSpec.Builder.setUserAuthenticationRequired][3] to true, you can permit
the use of the key only after the user authenticate it including when authenticated with the user's
fingerprint.

Then start listening to a fingerprint on the fingerprint sensor by calling
[FingerprintManager.authenticate][4] with a [Cipher][5] initialized with the symmetric key created.
Or alternatively you can fall back to server-side verified password as an authenticator.

Once the fingerprint (or password) is verified, the
[FingerprintManager.AuthenticationCallback#onAuthenticationSucceeded()][6] callback is called.

[1]: https://developer.android.com/reference/javax/crypto/KeyGenerator.html
[2]: https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html
[3]: https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder.html#setUserAuthenticationRequired%28boolean%29
[4]: https://developer.android.com/reference/android/hardware/fingerprint/FingerprintManager.html#authenticate%28android.hardware.fingerprint.FingerprintManager.CryptoObject,%20android.os.CancellationSignal,%20int,%20android.hardware.fingerprint.FingerprintManager.AuthenticationCallback,%20android.os.Handler%29
[5]: https://developer.android.com/reference/javax/crypto/Cipher.html
[6]: https://developer.android.com/reference/android/hardware/fingerprint/FingerprintManager.AuthenticationCallback.html#onAuthenticationSucceeded%28android.hardware.fingerprint.FingerprintManager.AuthenticationResult%29

Pre-requisites
--------------

- Android SDK 27
- Android Support Repository

Screenshots
-------------

<img src="screenshots/1-purchase-screen.png" height="400" alt="Screenshot"/>
<img src="screenshots/2-fingerprint-dialog.png" height="400" alt="Screenshot"/>
<img src="screenshots/3-fingerprint-authenticated.png" height="400" alt="Screenshot"/>
<img src="screenshots/4-new-fingerprint-enrolled.png" height="400" alt="Screenshot"/>

Getting Started
---------------

This sample uses the Gradle build system. To build this project, use the
"gradlew build" command or use "Import Project" in Android Studio.

Support
-------

- Stack Overflow: http://stackoverflow.com/questions/tagged/android

If you've found an error in this sample, please file an issue:
https://github.com/android/security

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub. Please see CONTRIBUTING.md for more details.
