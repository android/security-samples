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
import static com.google.common.truth.Truth8.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for {@link AuthorizationList}.
 */
@RunWith(JUnit4.class)
public class AuthorizationListTest {

  // Generated from certificate with RSA Algorithm and StrongBox Security Level
  private static final String SW_ENFORCED_EXTENSTION_DATA =
      "MIIBzb+FPQgCBgFr9iKgzL+FRYIBuwSCAbcwggGzMYIBizAMBAdhbmRyb2lkAgEdMBkEFGNvbS5hbmRyb2lkLmtleWNo"
          + "YWluAgEdMBkEFGNvbS5hbmRyb2lkLnNldHRpbmdzAgEdMBkEFGNvbS5xdGkuZGlhZ3NlcnZpY2VzAgEdMBoEFW"
          + "NvbS5hbmRyb2lkLmR5bnN5c3RlbQIBHTAdBBhjb20uYW5kcm9pZC5pbnB1dGRldmljZXMCAR0wHwQaY29tLmFu"
          + "ZHJvaWQubG9jYWx0cmFuc3BvcnQCAR0wHwQaY29tLmFuZHJvaWQubG9jYXRpb24uZnVzZWQCAR0wHwQaY29tLm"
          + "FuZHJvaWQuc2VydmVyLnRlbGVjb20CAR0wIAQbY29tLmFuZHJvaWQud2FsbHBhcGVyYmFja3VwAgEdMCEEHGNv"
          + "bS5nb29nbGUuU1NSZXN0YXJ0RGV0ZWN0b3ICAR0wIgQdY29tLmdvb2dsZS5hbmRyb2lkLmhpZGRlbm1lbnUCAQ"
          + "EwIwQeY29tLmFuZHJvaWQucHJvdmlkZXJzLnNldHRpbmdzAgEdMSIEIDAao8sIETRQHEXxQiq8ZsJCJP1d7V/c"
          + "jxfmlxdv2Gaq";
  private static final String TEE_ENFORCED_EXTENSTION_DATA =
      "MIGwoQgxBgIBAgIBA6IDAgEBowQCAggApQUxAwIBBKYIMQYCAQMCAQW/gUgFAgMBAAG/g3cCBQC/hT4DAgEAv4VATDBK"
          + "BCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEBAAoBAgQgco2xJ08fHPFXHeQ4CwSKVUrEo4Dnb1"
          + "NVCDUpCEqTeAG/hUEDAgEAv4VCBQIDAxSzv4VOBgIEATQV8b+FTwYCBAE0Few=";

  // Some enum values, complete list can be found at:
  // https://source.android.com/security/keystore/tags
  private static final int PURPOSE_SIGN = 2;
  private static final int PURPOSE_VERIFY = 3;
  private static final int ALGORITHM_RSA = 1;
  private static final int DIGEST_SHA_2_256 = 4;
  private static final int PADDING_RSA_PSS = 3;
  private static final int PADDING_RSA_1_5_SIGN = 5;
  private static final int ORIGIN_GENERATED = 0;

  // Monday, 15 July 2019 14:56:32.972
  private static final Long EXPECTED_SW_CREATION_DATETIME = 1563202592972L;
  private static final byte[] EXPECTED_SW_ATTESTATION_APPLICATION_ID = Base64.decode(
      "MIIBszGCAYswDAQHYW5kcm9pZAIBHTAZBBRjb20uYW5kcm9pZC5rZXljaGFpbgIBHTAZBBRjb20uYW5kcm9pZC5zZXR0"
          + "aW5ncwIBHTAZBBRjb20ucXRpLmRpYWdzZXJ2aWNlcwIBHTAaBBVjb20uYW5kcm9pZC5keW5zeXN0ZW0CAR0wHQ"
          + "QYY29tLmFuZHJvaWQuaW5wdXRkZXZpY2VzAgEdMB8EGmNvbS5hbmRyb2lkLmxvY2FsdHJhbnNwb3J0AgEdMB8E"
          + "GmNvbS5hbmRyb2lkLmxvY2F0aW9uLmZ1c2VkAgEdMB8EGmNvbS5hbmRyb2lkLnNlcnZlci50ZWxlY29tAgEdMC"
          + "AEG2NvbS5hbmRyb2lkLndhbGxwYXBlcmJhY2t1cAIBHTAhBBxjb20uZ29vZ2xlLlNTUmVzdGFydERldGVjdG9y"
          + "AgEdMCIEHWNvbS5nb29nbGUuYW5kcm9pZC5oaWRkZW5tZW51AgEBMCMEHmNvbS5hbmRyb2lkLnByb3ZpZGVycy"
          + "5zZXR0aW5ncwIBHTEiBCAwGqPLCBE0UBxF8UIqvGbCQiT9Xe1f3I8X5pcXb9hmqg==");

  private static final Set<Integer> EXPECTED_TEE_PURPOSE = new HashSet<>(
      Arrays.asList(PURPOSE_SIGN, PURPOSE_VERIFY));
  private static final Integer EXPECTED_TEE_ALGORITHM = ALGORITHM_RSA;
  private static final Integer EXPECTED_TEE_KEY_SIZE = 2048;
  private static final Set<Integer> EXPECTED_TEE_DIGEST = new HashSet<>(
      Collections.singletonList(DIGEST_SHA_2_256));
  private static final Set<Integer> EXPECTED_TEE_PADDING = new HashSet<>(
      Arrays.asList(PADDING_RSA_PSS, PADDING_RSA_1_5_SIGN));
  private static final Long EXPECTED_TEE_RSA_PUBLIC_COMPONENT = 65537L;
  private static final Integer EXPECTED_TEE_ORIGIN = ORIGIN_GENERATED;
  private static final Integer EXPECTED_TEE_OS_VERSION = 0;
  private static final Integer EXPECTED_TEE_OS_PATCH_LEVEL = 201907;
  private static final Integer EXPECTED_TEE_VENDOR_PATCH_LEVEL = 20190705;
  private static final Integer EXPECTED_TEE_BOOT_PATCH_LEVEL = 20190700;

  @Test
  public void testCanParseAuthorizationListFromSwEnforced()
      throws IOException {
    AuthorizationList authorizationList = AuthorizationList.createAuthorizationList(
        getEncodableAuthorizationList(SW_ENFORCED_EXTENSTION_DATA));

    assertThat(authorizationList.creationDateTime).hasValue(EXPECTED_SW_CREATION_DATETIME);
    assertThat(authorizationList.rootOfTrust).isEmpty();
    assertThat(authorizationList.attestationApplicationId)
        .hasValue(EXPECTED_SW_ATTESTATION_APPLICATION_ID);
  }

  @Test
  public void testCanParseAuthorizationListFromTeeEnforced()
      throws IOException {
    AuthorizationList authorizationList = AuthorizationList.createAuthorizationList(
        getEncodableAuthorizationList(TEE_ENFORCED_EXTENSTION_DATA));

    assertThat(authorizationList.purpose).hasValue(EXPECTED_TEE_PURPOSE);
    assertThat(authorizationList.algorithm).hasValue(EXPECTED_TEE_ALGORITHM);
    assertThat(authorizationList.keySize).hasValue(EXPECTED_TEE_KEY_SIZE);
    assertThat(authorizationList.digest).hasValue(EXPECTED_TEE_DIGEST);
    assertThat(authorizationList.padding).hasValue(EXPECTED_TEE_PADDING);
    assertThat(authorizationList.rsaPublicExponent).hasValue(EXPECTED_TEE_RSA_PUBLIC_COMPONENT);
    assertThat(authorizationList.noAuthRequired).isTrue();
    assertThat(authorizationList.origin).hasValue(EXPECTED_TEE_ORIGIN);
    assertThat(authorizationList.rootOfTrust).isPresent();
    assertThat(authorizationList.osVersion).hasValue(EXPECTED_TEE_OS_VERSION);
    assertThat(authorizationList.osPatchLevel).hasValue(EXPECTED_TEE_OS_PATCH_LEVEL);
    assertThat(authorizationList.vendorPatchLevel).hasValue(EXPECTED_TEE_VENDOR_PATCH_LEVEL);
    assertThat(authorizationList.bootPatchLevel).hasValue(EXPECTED_TEE_BOOT_PATCH_LEVEL);
  }

  private ASN1Encodable[] getEncodableAuthorizationList(String extensionData) throws IOException {
    byte[] extensionDataBytes = Base64.decode(extensionData);
    return ((ASN1Sequence) ASN1Sequence.fromByteArray(extensionDataBytes)).toArray();
  }
}