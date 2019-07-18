/* Copyright 2019, The Android Open Source Project, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.attestation;


import static com.google.common.truth.Truth.assertThat;

import com.google.android.attestation.RootOfTrust.VerifiedBootState;
import com.google.common.truth.Truth;
import java.io.IOException;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for {@link RootOfTrust}.
 */
@RunWith(JUnit4.class)
public class RootOfTrustTest {

  // Generated from certificate with EC Algorithm and StrongBox Security Level
  private static final String ROOT_OF_TRUST =
      "MEoEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEACgECBCByjbEnTx8c8Vcd5DgLBIpVSsSjgOdvU1UI"
          + "NSkISpN4AQ==\n";

  private static final byte[] EXPECTED_VERIFIED_BOOT_KEY = Base64
      .decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
  private static final boolean EXPECTED_DEVICE_LOCKED = false;
  private static final VerifiedBootState EXPECTED_VERIFIED_BOOT_STATE =
      VerifiedBootState.UNVERIFIED;
  private static final byte[] EXPECTED_VERIFIED_BOOT_HASH = Base64
      .decode("co2xJ08fHPFXHeQ4CwSKVUrEo4Dnb1NVCDUpCEqTeAE=");


  @Test
  public void testCreateRootOfTrust() throws IOException {
    ASN1Sequence rootOfTrustSequence = getRootOfTrustSequence(ROOT_OF_TRUST);
    RootOfTrust rootOfTrust = RootOfTrust.createRootOfTrust(rootOfTrustSequence);

    assertThat(rootOfTrust).isNotNull();
    assertThat(rootOfTrust.verifiedBootKey).isEqualTo(EXPECTED_VERIFIED_BOOT_KEY);
    assertThat(rootOfTrust.deviceLocked).isEqualTo(EXPECTED_DEVICE_LOCKED);
    assertThat(rootOfTrust.verifiedBootState).isEqualTo(EXPECTED_VERIFIED_BOOT_STATE);
    assertThat(rootOfTrust.verifiedBootHash).isEqualTo(EXPECTED_VERIFIED_BOOT_HASH);
  }

  @Test
  public void testCreateEmptyRootOfTrust() {
    Truth.assertThat(RootOfTrust.createRootOfTrust(null)).isNull();
  }

  private ASN1Sequence getRootOfTrustSequence(String rootOfTrustB64) throws IOException {
    byte[] rootOfTrustBytes = Base64.decode(rootOfTrustB64);
    return (ASN1Sequence) ASN1Sequence.fromByteArray(rootOfTrustBytes);
  }
}