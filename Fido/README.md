Android FIDO2 API Sample
===========================

A sample app showing how to register and authenticate with Public Key
Credentials using the FIDO2 API.

FIDO2 API is used for devices running Android N (API level 24) or newer.

Introduction
------------
[The Android FIDO2
API](https://developers.google.com/identity/fido/android/native-apps) provides a
[FIDO Alliance](https://fidoalliance.org/) certified implementation of a
[WebAuthn Client](https://www.w3.org/TR/webauthn/#webauthn-client) for Android.
The API supports the use of roaming authenticators such as BLE, NFC, and USB
security keys as well as platform authenticators, which allow users to
authenticate using their fingerprint or screenlock.

It is relying party's responsibility to manage registered keys. In the sample
app, the keys are managed by [WebAuthn demo
server](https://webauthndemo.appspot.com/) ([source
code](https://github.com/google/webauthndemo)), however, in production use
cases, the relying party should implement their own storage.

The FIDO2 API entry point is the
[`Fido2ApiClient`](https://developers.google.com/android/reference/com/google/android/gms/fido/fido2/Fido2ApiClient).

Code Example
------------

```
/* Get an instance of the API client. */
Fido2ApiClient fido2ApiClient = Fido.getFido2ApiClient(this /* calling activity */);
```

The `Fido2ApiClient` provides methods to allow your app to register new
credentials (registration) as well as authenticate using existing credentials
(signing)
```
Task<Fido2PendingIntent> fido2PendingIntent =
    fido2ApiClient.getRegisterIntent(
        publicKeyCredentialsCreationOptions);

Task<Fido2PendingIntent> fido2PendingIntent =
    fido2ApiClient.getSignIntent(
        publicKeyCredentialsRequestOptions);
```

Once the
[`Fido2PendingIntent`](https://developers.google.com/android/reference/com/google/android/gms/fido/fido2/Fido2PendingIntent)
is received, it can be launched using the callback:
```java
result.addOnSuccessListener(
     new OnSuccessListener<Fido2PendingIntent>() {
       @Override
       public void onSuccess(Fido2PendingIntent fido2PendingIntent) {
         if (fido2PendingIntent.hasPendingIntent()) {
           // Start a FIDO2 registration request.
           fido2PendingIntent.launchPendingIntent(this, REQUEST_CODE_REGISTER);
           // For a FIDO2 sign request.
           // fido2PendingIntent.launchPendingIntent(this, REQUEST_CODE_SIGN);
         }
       }
     });

 result.addOnFailureListener(
     new OnFailureListener() {
       @Override
       public void onFailure(Exception e) {
           // fail
       }
     });
```

The result is handled in `onActivityResult()`:
```
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (resultCode != RESULT_OK) {
    // Something went wrong
  }

  switch(requestCode) {
    case REQUEST_CODE_REGISTER:
      AuthenticatorAttestationResponse response =
        AuthenticatorAttestationResponse.deserializeFromBytes(
          data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA));
      // Do something useful
      break;
    case REQUEST_CODE_SIGN:
      AuthenticatorAssertionResponse response =
        AuthenticatorAssertionResponse.deserializeFromBytes(
          data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA));
      // Do something useful
      break;
    default:
      // Something went wrong
  }
}
```


Pre-requisites
--------------

- Android SDK 26
- Android Build Tools v25.0.3


Getting Started
---------------

To install the sample app on your Android device or emulator,
run `./gradlew :app:installRelease`. This will install the release
configuration, which uses the bundled keystore file to make the app
work with the demo server.

Support
-------

- [FIDO-Dev mailing list](https://groups.google.com/a/fidoalliance.org/forum/#!forum/fido-dev)

If you've found an error in this sample, please file an issue:
https://github.com/googlesamples/android-fido

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub. Please see CONTRIBUTING.md for more details.
