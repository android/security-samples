/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.fido.example.fido2apiexample;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fido.fido2.api.common.Attachment;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria;
import com.google.android.gms.fido.fido2.api.common.EC2Algorithm;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.common.collect.FluentIterable;
import com.google.common.io.BaseEncoding;
import com.google.webauthn.gaedemo.fido2RequestHandler.Fido2RequestHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Service to communicate with WebAuthn demo server. */
public class GAEService {
    private static GAEService gaeService = null;
    private static final String TAG = "GAEService";

    private static final String KEY_REQUEST_CHALLENGE = "challenge";
    private static final String KEY_RP = "rp";
    private static final String KEY_RP_ID = "id";
    private static final String KEY_RP_NAME = "name";
    private static final String KEY_RP_ICON = "icon";
    private static final String KEY_USER = "user";
    private static final String KEY_USER_DISPLAY_NAME = "displayName";
    private static final String KEY_PARAMETERS = "pubKeyCredParams";
    private static final String KEY_PARAMETERS_TYPE = "type";
    private static final String KEY_TIMEOUT = "timeout";
    private static final String KEY_ATTACHMENT = "attachment";
    private static final String KEY_SESSION = "session";
    private static final String KEY_SESSION_ID = "id";
    private static final String KEY_RPID = "rpId";
    private static final String KEY_CLIENT_DATA_JSON = "clientDataJSON";
    private static final String KEY_ATTESTATION_OBJECT = "attestationObject";
    private static final String KEY_AUTHENTICATOR_DATA = "authenticatorData";
    private static final String KEY_CREDENTIAL_ID = "credentialId";
    private static final String KEY_SIGNATURE = "signature";

    private Fido2RequestHandler fido2Service;
    private final Context context;
    private final GoogleSignInAccount googleSignInAccount;
    private final Map<String, String> sessionIds;
    private List<Map<String, String>> securityTokens;

    public static GAEService getInstance(Context context, GoogleSignInAccount googleSignInAccount) {
        if (gaeService == null) {
            gaeService = new GAEService(context, googleSignInAccount);
        }
        return gaeService;
    }

    private GAEService(Context context, GoogleSignInAccount googleSignInAccount) {
        this.context = context;
        this.googleSignInAccount = googleSignInAccount;
        sessionIds = new HashMap<>();
        initFido2GAEService();
    }

    public PublicKeyCredentialCreationOptions getRegistrationRequest(List<String> excludedKeys) {
        try {
            if (fido2Service == null) {
                return null;
            }
            List<String> registerRequestContent =
                    fido2Service.getRegistrationRequest().execute().getItems();
            if (registerRequestContent == null || registerRequestContent.isEmpty()) {
                Log.i(TAG, "registerRequestContent is null or empty");
                return null;
            }
            for (String value : registerRequestContent) {
                Log.i(TAG, "registerRequestContent " + value);
            }
            // A sample register request:
            // {"rp":{"id":"webauthndemo.appspot.com","name":"webauthndemo.appspot.com"},
            //
            // "user":{"name":"littlecattest","displayName":"littlecattest","id":"bGl0dGxlY2F0dGVzdEBnbWFpbC5jb20="},
            //     "challenge":"Zys9NEvoE6KRhZtVMJZ3KKHg+spgXu2R0R7AQ2Mudlg=",
            //     "pubKeyCredParams":[{"type":"public-key","alg":-7},{"type":"public-key","alg":-35},
            //     {"type":"public-key","alg":-36},{"type":"public-key","alg":-40},
            //     {"type":"public-key","alg":-41},{"type":"public-key","alg":-42},
            //     {"type":"public-key","alg":-37},{"type":"public-key","alg":-38},
            //     {"type":"public-key","alg":-39}],
            //     "session":{"id":5634387206995968,
            //     "challenge":"Zys9NEvoE6KRhZtVMJZ3KKHg+spgXu2R0R7AQ2Mudlg=",
            //     "origin":"webauthndemo.appspot.com"}}*/

            JSONObject registerRequestJson = new JSONObject(registerRequestContent.get(0));
            PublicKeyCredentialCreationOptions.Builder builder =
                    new PublicKeyCredentialCreationOptions.Builder();

            // Parse challenge
            builder.setChallenge(
                    BaseEncoding.base64().decode(registerRequestJson.getString(KEY_REQUEST_CHALLENGE)));

            // Parse RP
            JSONObject rpJson = registerRequestJson.getJSONObject(KEY_RP);
            String rpId = rpJson.getString(KEY_RP_ID);
            String rpName = rpJson.getString(KEY_RP_NAME);
            String rpIcon = null;
            if (rpJson.has(KEY_RP_ICON)) {
                rpIcon = rpJson.getString(KEY_RP_ICON);
            }
            PublicKeyCredentialRpEntity entity = new PublicKeyCredentialRpEntity(rpId, rpName, rpIcon);
            builder.setRp(entity);

            // Parse user
            JSONObject userJson = registerRequestJson.getJSONObject(KEY_USER);
            String displayName = userJson.getString(KEY_USER_DISPLAY_NAME);
            PublicKeyCredentialUserEntity userEntity =
                    new PublicKeyCredentialUserEntity(
                            displayName.getBytes() /* id */,
                            displayName /* name */,
                            null /* icon */,
                            displayName);
            builder.setUser(userEntity);

            // Parse parameters
            List<PublicKeyCredentialParameters> parameters = new ArrayList<>();
            if (registerRequestJson.has(KEY_PARAMETERS)) {
                JSONArray params = registerRequestJson.getJSONArray(KEY_PARAMETERS);
                for (int i = 0; i < params.length(); i++) {
                    JSONObject param = params.getJSONObject(i);
                    String type = param.getString(KEY_PARAMETERS_TYPE);
                    // TODO: this is a hack, use KEY_PARAMETERS_ALGORITHM = "alg"
                    PublicKeyCredentialParameters parameter =
                            new PublicKeyCredentialParameters(type, EC2Algorithm.ES256.getAlgoValue());
                    parameters.add(parameter);
                }
            }
            builder.setParameters(parameters);

            // Parse timeout
            Double timeout = null;
            if (registerRequestJson.has(KEY_TIMEOUT)) {
                timeout = Double.valueOf(registerRequestJson.getLong(KEY_TIMEOUT));
            }
            builder.setTimeoutSeconds(timeout);

            // Parse exclude list
            List<PublicKeyCredentialDescriptor> descriptors =
                    FluentIterable.from(excludedKeys)
                            .transform(
                                    k ->
                                            new PublicKeyCredentialDescriptor(
                                                    PublicKeyCredentialType.PUBLIC_KEY.toString(),
                                                    BaseEncoding.base64Url().decode(k),
                                                    /* transports= */ null))
                            .toList();
            builder.setExcludeList(descriptors);

            AuthenticatorSelectionCriteria.Builder criteria =
                    new AuthenticatorSelectionCriteria.Builder();
            if (registerRequestJson.has(KEY_ATTACHMENT)) {
                criteria.setAttachment(
                        Attachment.fromString(registerRequestJson.getString(KEY_ATTACHMENT)));
            }
            builder.setAuthenticatorSelection(criteria.build());
            return builder.build();
        } catch (IOException
                | JSONException
                | Attachment.UnsupportedAttachmentException e) {
            Log.e(TAG, "Error extracting information from server's registration request", e);
        }
        return null;
    }

    public String getRegisterResponseFromServer(AuthenticatorAttestationResponse response) {
        Log.d(TAG, "getRegisterResponseFromServer");
        try {
            if (fido2Service == null) {
                return null;
            }
            JSONObject responseJson = new JSONObject();
            String clientDataJSON = new String(response.getClientDataJSON(), "UTF-8");
            String attestationObject = BaseEncoding.base64().encode(response.getAttestationObject());
            responseJson.put(KEY_CLIENT_DATA_JSON, clientDataJSON);
            responseJson.put(KEY_ATTESTATION_OBJECT, attestationObject);

            List<String> registerResponseContent =
                    fido2Service.processRegistrationResponse(responseJson.toString()).execute().getItems();
            if (registerResponseContent == null || registerResponseContent.isEmpty()) {
                Log.i(TAG, "registerResponseContent is null or empty");
            } else {
                Log.i(TAG, "registerResponseContent " + registerResponseContent.get(0));
                JSONObject credential = new JSONObject(registerResponseContent.get(0));
                // return string value of the registered credential
                return credential.toString();
            }

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error processing registration response", e);
        }
        return null;
    }

    public PublicKeyCredentialRequestOptions getSignRequest(List<String> allowedKeys) {
        Log.d(TAG, "getSignRequest");
        try {
            if (fido2Service == null) {
                return null;
            }
            List<String> signRequestContent = fido2Service.getSignRequest().execute().getItems();
            if (signRequestContent == null || signRequestContent.isEmpty()) {
                Log.i(TAG, "signRequestContent is empty");
                return null;
            }
            for (String signRequest : signRequestContent) {
                Log.i(TAG, "signRequestContent " + signRequest);
            }
            JSONObject signRequestJson = new JSONObject(signRequestContent.get(0));
            PublicKeyCredentialRequestOptions.Builder builder =
                    new PublicKeyCredentialRequestOptions.Builder();
            // signRequestContent {"challenge":"AmlL6aQKTMd24MmfZtrvBGP/oKb8+zpXRcB7bfUHrPk=",
            // "rpId":"https://webauthdemo.appspot.com",
            // "allowList":[{"type":"public-key",
            // "id":"lmKQSq81f+gLQ49jeBQNFD/3TU7R2gGFWin+zNzpDrFeWUTTkEZ7nfmIC5OWXarRNqLxImA0hE7UVOI3eeVZZg=="}],
            // "session":{"id":5704837555552256,
            // "challenge":"AmlL6aQKTMd24MmfZtrvBGP/oKb8+zpXRcB7bfUHrPk=",
            // "origin":"https://webauthdemo.appspot.com"}}

            // Parse challenge
            builder.setChallenge(
                    BaseEncoding.base64().decode(signRequestJson.getString(KEY_REQUEST_CHALLENGE)));

            // Parse timeout
            if (signRequestJson.has(KEY_TIMEOUT)) {
                Double timeout = Double.valueOf(signRequestJson.getLong(KEY_TIMEOUT));
                builder.setTimeoutSeconds(timeout);
            }

            // Parse rpId
            String rpId = signRequestJson.getString(KEY_RPID);
            builder.setRpId(rpId);

            // Parse session id
            JSONObject session = signRequestJson.getJSONObject(KEY_SESSION);
            String sessionId = String.valueOf(session.getLong(KEY_SESSION_ID));

            // Parse allow list
            List<PublicKeyCredentialDescriptor> descriptors = new ArrayList<>();
            for (String allowedKey : allowedKeys) {
                sessionIds.put(allowedKey, sessionId);
                PublicKeyCredentialDescriptor publicKeyCredentialDescriptor =
                        new PublicKeyCredentialDescriptor(
                                PublicKeyCredentialType.PUBLIC_KEY.toString(),
                                BaseEncoding.base64Url().decode(allowedKey),
                                /* transports= */ null);
                descriptors.add(publicKeyCredentialDescriptor);
            }
            builder.setAllowList(descriptors);

            return builder.build();

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error processing sign request from server", e);
        }
        return null;
    }

    public String getSignResponseFromServer(AuthenticatorAssertionResponse response) {
        Log.d(TAG, "getSignResponseFromServer");
        try {
            if (fido2Service == null) {
                return null;
            }
            JSONObject responseJson = new JSONObject();
            String clientDataJSON = new String(response.getClientDataJSON(), "UTF-8");
            String authenticatorData = BaseEncoding.base64().encode(response.getAuthenticatorData());
            String credentialId = BaseEncoding.base64Url().encode(response.getKeyHandle());
            String signature = BaseEncoding.base64().encode(response.getSignature());
            responseJson.put(KEY_CLIENT_DATA_JSON, clientDataJSON);
            responseJson.put(KEY_AUTHENTICATOR_DATA, authenticatorData);
            responseJson.put(KEY_CREDENTIAL_ID, credentialId);
            responseJson.put(KEY_SIGNATURE, signature);

            // insert sessionId for the authenticated credential ID into result data in JSON format,
            // and pass it back to server.
            String sessionId = sessionIds.get(BaseEncoding.base64Url().encode(response.getKeyHandle()));
            responseJson.put(KEY_SESSION_ID, sessionId);

            List<String> signResponseContent =
                    fido2Service.processSignResponse(responseJson.toString()).execute().getItems();
            if (signResponseContent == null || signResponseContent.isEmpty()) {
                Log.i(TAG, "signResponseContent is null or empty");
            } else {
                Log.i(TAG, "signResponseContent " + signResponseContent.get(0));
                JSONObject credential = new JSONObject(signResponseContent.get(0));
                // return string value of the authenticated credential
                return credential.toString();
            }

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error processing sign response", e);
        }

        return null;
    }

    public List<Map<String, String>> getAllSecurityTokens() {
        Log.d(TAG, "getAllSecurityKeyTokens is called");
        try {
            if (fido2Service == null) {
                return new ArrayList<>();
            }
            List<String> securityKeyResponse = fido2Service.getAllSecurityKeys().execute().getItems();
            if (securityKeyResponse == null || securityKeyResponse.isEmpty()) {
                Log.i(TAG, "securityKeyResponse is null or empty");
                return null;
            }
            Log.i(TAG, "securityKeyResponse " + securityKeyResponse.get(0));
            securityTokens = new ArrayList<>();
            JSONArray tokenJsonArray = new JSONArray(securityKeyResponse.get(0));
            for (int i = 0; i < tokenJsonArray.length(); i++) {
                Map<String, String> tokenContent = new HashMap<>();

                JSONObject tokenJson = tokenJsonArray.getJSONObject(i);
                Iterator<String> keys = tokenJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    tokenContent.put(key, tokenJson.getString(key));
                }
                securityTokens.add(tokenContent);
            }
            return securityTokens;

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error getting all security key tokens", e);
        }
        return null;
    }

    public String removeSecurityKey(String publicKey) {
        try {
            fido2Service.removeSecurityKey(publicKey);
            return publicKey;
        } catch (IOException e) {
            Log.e(TAG, "Error removing security key.", e);
            return null;
        }
    }

    private void initFido2GAEService() {
        if (fido2Service != null) {
            return;
        }
        if (googleSignInAccount == null) {
            return;
        }
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingAudience(
                        context, "server:client_id:" + Constants.SERVER_CLIENT_ID);
        credential.setSelectedAccountName(googleSignInAccount.getEmail());
        Log.d(TAG, "credential account name " + credential.getSelectedAccountName());

        Fido2RequestHandler.Builder builder =
                new Fido2RequestHandler.Builder(
                        AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), credential)
                        .setGoogleClientRequestInitializer(
                                new GoogleClientRequestInitializer() {
                                    @Override
                                    public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest)
                                            throws IOException {
                                        abstractGoogleClientRequest.setDisableGZipContent(true);
                                    }
                                });
        fido2Service = builder.build();
    }
}
