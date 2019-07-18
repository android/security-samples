Android Key Attestation Sample
===================================

This sample illustrates how to use the [Bouncy Castle ASN.1][1] parser to extract information
from an Android attestation data structure to verify that a key pair has been
generated in an Android device.

This repository contains a [server](server/) sample that shows how to attest an Android certificate
outside the Android framework. This is the recommended best practise, as it is safer to check the
certificate's authenticity on a separate server that you trust.

For more details, see the documentation and the guide at
https://developer.android.com/training/articles/security-key-attestation.html .

[1]: https://www.bouncycastle.org/


Getting Started
---------------

See the [server](server/) sample for details.

Support
-------

- Stack Overflow: http://stackoverflow.com/questions/tagged/android

If you've found an error in this sample, please file an issue:
https://github.com/android/security

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub. Please see CONTRIBUTING.md for more details.
