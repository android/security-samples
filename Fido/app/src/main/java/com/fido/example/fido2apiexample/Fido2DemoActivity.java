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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.Fido2PendingIntent;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.io.BaseEncoding;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity that conducts registration and authentication operations against WebAuthn demo server.
 */
public class Fido2DemoActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
    private static final String TAG = "Fido2DemoActivity";
    private static final String KEY_KEY_HANDLE = "handle";
    private static final String KEY_CREDENTIAL = "credential";
    private static final String KEY_CREDENTIAL_ID = "id";

    private static final int RC_SIGN_IN = 9001;
    private static final int REQUEST_CODE_REGISTER = 0;
    private static final int REQUEST_CODE_SIGN = 1;
    private static final int GET_ACCOUNTS_PERMISSIONS_REQUEST_REGISTER = 0x11;
    private static final int GET_ACCOUNTS_PERMISSIONS_REQUEST_SIGN = 0x13;
    private static final int GET_ACCOUNTS_PERMISSIONS_ALL_TOKENS = 0x15;

    // Create a new ThreadPoolExecutor with 2 threads for each processor on the
    // device and a 60 second keep-alive time.
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR =
            new ThreadPoolExecutor(
                    NUM_CORES * 2, NUM_CORES * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());

    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private SecurityTokenAdapter adapter;
    private List<Map<String, String>> securityTokens;
    private SignInButton signInButton;

    private TextView userEmailTextView;
    private TextView displayNameTextView;
    private MenuItem operationMenuItem;
    private MenuItem signInMenuItem;
    private MenuItem signOutMenuItem;

    private GoogleApiClient googleApiClient;
    private GAEService gaeService;
    private GoogleSignInAccount googleSignInAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // START Google sign in API client
        // configure sign-in to request user info
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(Constants.SERVER_CLIENT_ID)
                        .build();

        // build client with access to Google Sign-In API and the options specified by gso
        googleApiClient =
                new GoogleApiClient.Builder(this)
                        .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                        .build();
        // END Google sign in API client

        // START prepare main layout
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = findViewById(R.id.progressBar);

        swipeRefreshLayout = findViewById(R.id.swipe_container);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        updateAndDisplayRegisteredKeys();
                    }
                });

        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter =
                new SecurityTokenAdapter(
                        new ArrayList<Map<String, String>>(), R.layout.row_token, Fido2DemoActivity.this);
        // END prepare main layout

        // START prepare drawer layout
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(
                        this,
                        drawer,
                        toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);
        View header = navigationView.getHeaderView(0);
        userEmailTextView = header.findViewById(R.id.userEmail);
        displayNameTextView = header.findViewById(R.id.displayName);
        Menu menu = navigationView.getMenu();
        operationMenuItem = menu.findItem(R.id.nav_fido2Operations);
        signInMenuItem = menu.findItem(R.id.nav_signin);
        signOutMenuItem = menu.findItem(R.id.nav_signout);
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setScopes(gso.getScopeArray());
        signInButton.setOnClickListener(this);
        // END prepare drawer layout

        // request SignIn or load registered tokens
        updateUI();
    }

    /** Show SignIn button to request user sign in or display all registered security tokens */
    private void updateUI() {
        // We check a boolean value in SharedPreferences to determine whether the user has been
        // signed in. This value is false by default. It would be set to true after signing in and
        // would be reset to false after user clicks "Sign out".
        // After the users clicks "Sign out", we couldn't use
        // GoogleSignInApi#silentSignIn(GoogleApiClient), because it silently signs in the user
        // again. Thus, we rely on this boolean value in SharedPreferences.
        if (!getAccountSignInStatus()) {
            displayAccountNotSignedIn();
            return;
        }

        OptionalPendingResult<GoogleSignInResult> pendingResult =
                Auth.GoogleSignInApi.silentSignIn(googleApiClient);
        if (pendingResult.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            GoogleSignInResult result = pendingResult.get();
            if (result.isSuccess()) {
                googleSignInAccount = result.getSignInAccount();
                displayAccountSignedIn(
                        result.getSignInAccount().getEmail(), result.getSignInAccount().getDisplayName());
            } else {
                displayAccountNotSignedIn();
            }
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            displayAccountNotSignedIn();

            pendingResult.setResultCallback(
                    new ResultCallback<GoogleSignInResult>() {
                        @Override
                        public void onResult(@NonNull GoogleSignInResult result) {
                            if (result.isSuccess()) {
                                googleSignInAccount = result.getSignInAccount();
                                displayAccountSignedIn(
                                        result.getSignInAccount().getEmail(),
                                        result.getSignInAccount().getDisplayName());
                            } else {
                                displayAccountNotSignedIn();
                            }
                        }
                    });
        }
    }

    private void displayAccountSignedIn(String email, String displayName) {
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        userEmailTextView.setText(email);
        displayNameTextView.setText(displayName);
        operationMenuItem.setVisible(true);
        signInMenuItem.setVisible(false);
        signOutMenuItem.setVisible(true);
        updateAndDisplayRegisteredKeys();
        signInButton.setVisibility(View.GONE);
    }

    private void displayAccountNotSignedIn() {
        signInButton.setVisibility(View.VISIBLE);
        userEmailTextView.setText("");
        displayNameTextView.setText("");
        operationMenuItem.setVisible(false);
        signInMenuItem.setVisible(true);
        signOutMenuItem.setVisible(false);
        swipeRefreshLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void displayRegisteredKeys() {
        adapter.clearSecurityTokens();
        adapter.addSecurityToken(securityTokens);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
        progressBar.setVisibility(View.GONE);
    }

    private void getRegisterRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "getRegisterRequest permission is granted");

            Task<PublicKeyCredentialCreationOptions> getRegisterRequestTask = asyncGetRegisterRequest();
            getRegisterRequestTask.addOnCompleteListener(
                    new OnCompleteListener<PublicKeyCredentialCreationOptions>() {
                        @Override
                        public void onComplete(@NonNull Task<PublicKeyCredentialCreationOptions> task) {
                            PublicKeyCredentialCreationOptions options = task.getResult();
                            if (options == null) {
                                Log.d(TAG, "Register request is null");
                                return;
                            }
                            sendRegisterRequestToClient(options);
                        }
                    });
        } else {
            Log.i(TAG, "getRegisterRequest permission is requested");
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.GET_ACCOUNTS},
                    GET_ACCOUNTS_PERMISSIONS_REQUEST_REGISTER);
        }
    }

    private void sendRegisterRequestToClient(PublicKeyCredentialCreationOptions options) {
        Fido2ApiClient fido2ApiClient = Fido.getFido2ApiClient(this.getApplicationContext());

        Task<Fido2PendingIntent> result = fido2ApiClient.getRegisterIntent(options);

        result.addOnSuccessListener(
                new OnSuccessListener<Fido2PendingIntent>() {
                    @Override
                    public void onSuccess(Fido2PendingIntent fido2PendingIntent) {
                        if (fido2PendingIntent.hasPendingIntent()) {
                            try {
                                fido2PendingIntent.launchPendingIntent(
                                        Fido2DemoActivity.this, REQUEST_CODE_REGISTER);
                                Log.i(TAG, "Register request is sent out");
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "Error launching pending intent for register request", e);
                            }
                        }
                    }
                });
    }

    private void updateRegisterResponseToServer(AuthenticatorAttestationResponse response) {
        Task<String> updateRegisterResponseToServerTask = asyncUpdateRegisterResponseToServer(response);
        updateRegisterResponseToServerTask.addOnCompleteListener(
                new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        String securityKeyToken = task.getResult();
                        if (securityKeyToken == null) {
                            Toast.makeText(
                                    Fido2DemoActivity.this,
                                    "security key registration failed",
                                    Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                        updateAndDisplayRegisteredKeys();
                        Log.i(
                                TAG,
                                "Update register response to server with securityKeyToken: " + securityKeyToken);
                    }
                });
    }

    private void getSignRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "getSignRequest permission is granted");

            Task<PublicKeyCredentialRequestOptions> getSignRequestTask = asyncGetSignRequest();
            getSignRequestTask.addOnCompleteListener(
                    new OnCompleteListener<PublicKeyCredentialRequestOptions>() {
                        @Override
                        public void onComplete(@NonNull Task<PublicKeyCredentialRequestOptions> task) {
                            PublicKeyCredentialRequestOptions options = task.getResult();
                            if (options == null) {
                                Log.i(TAG, "Sign request is null");
                                return;
                            }
                            sendSignRequestToClient(options);
                        }
                    });
        } else {
            Log.i(TAG, "getSignRequest permission is requested");
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.GET_ACCOUNTS},
                    GET_ACCOUNTS_PERMISSIONS_REQUEST_SIGN);
        }
    }

    private void sendSignRequestToClient(PublicKeyCredentialRequestOptions options) {
        Fido2ApiClient fido2ApiClient = Fido.getFido2ApiClient(this.getApplicationContext());

        Task<Fido2PendingIntent> result = fido2ApiClient.getSignIntent(options);

        result.addOnSuccessListener(
                new OnSuccessListener<Fido2PendingIntent>() {
                    @Override
                    public void onSuccess(Fido2PendingIntent fido2PendingIntent) {
                        if (fido2PendingIntent.hasPendingIntent()) {
                            try {
                                fido2PendingIntent.launchPendingIntent(Fido2DemoActivity.this, REQUEST_CODE_SIGN);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "Error launching pending intent for sign request", e);
                            }
                        }
                    }
                });
    }

    private void updateSignResponseToServer(AuthenticatorAssertionResponse response) {
        Task<String> updateSignResponseToServerTask = asyncUpdateSignResponseToServer(response);
        updateSignResponseToServerTask.addOnCompleteListener(
                new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        String signResult = task.getResult();
                        if (signResult == null) {
                            Toast.makeText(
                                    Fido2DemoActivity.this,
                                    "this security key has not been registered!",
                                    Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                        Log.i(TAG, "authenticated key's pub key is " + signResult);
                        highlightAuthenticatedToken(signResult);
                    }
                });
    }

    private void updateAndDisplayRegisteredKeys() {
        progressBar.setVisibility(View.VISIBLE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "updateAndDisplayRegisteredKeys permission is granted");
            Task<List<Map<String, String>>> refreshSecurityKeyTask = asyncRefreshSecurityKey();
            refreshSecurityKeyTask.addOnCompleteListener(
                    new OnCompleteListener<List<Map<String, String>>>() {
                        @Override
                        public void onComplete(@NonNull Task<List<Map<String, String>>> task) {
                            List<Map<String, String>> tokens = task.getResult();
                            if (tokens == null) {
                                swipeRefreshLayout.setRefreshing(false);
                                progressBar.setVisibility(View.GONE);
                                return;
                            }
                            securityTokens = tokens;
                            adapter.clearSecurityTokens();
                            adapter.addSecurityToken(securityTokens);
                            displayRegisteredKeys();
                        }
                    });
        } else {
            Log.i(TAG, "updateAndDisplayRegisteredKeys permission is requested");
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.GET_ACCOUNTS},
                    GET_ACCOUNTS_PERMISSIONS_ALL_TOKENS);
        }
    }

    public void removeTokenByIndexInList(int whichToken) {
    /* assume this operation can only happen within short time after
      updateAndDisplayRegisteredKeys, which has already checked permission
    */
        Task<String> removeSecurityKeyTask = asyncRemoveSecurityKey(whichToken);
        removeSecurityKeyTask.addOnCompleteListener(
                new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        updateAndDisplayRegisteredKeys();
                    }
                });
    }

    private Task<PublicKeyCredentialCreationOptions> asyncGetRegisterRequest() {
        return Tasks.call(
                THREAD_POOL_EXECUTOR,
                new Callable<PublicKeyCredentialCreationOptions>() {
                    @Override
                    public PublicKeyCredentialCreationOptions call() throws Exception {
                        gaeService = GAEService.getInstance(Fido2DemoActivity.this, googleSignInAccount);
                        return gaeService.getRegistrationRequest(
                                FluentIterable.from(adapter.getCheckedItems())
                                        .transform(i -> i.get(KEY_KEY_HANDLE))
                                        .filter(i -> !Strings.isNullOrEmpty(i))
                                        .toList());
                    }
                });
    }

    private Task<String> asyncUpdateRegisterResponseToServer(
            final AuthenticatorAttestationResponse response) {
        return Tasks.call(
                THREAD_POOL_EXECUTOR,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        gaeService = GAEService.getInstance(Fido2DemoActivity.this, googleSignInAccount);
                        return gaeService.getRegisterResponseFromServer(response);
                    }
                });
    }

    private Task<PublicKeyCredentialRequestOptions> asyncGetSignRequest() {
        return Tasks.call(
                THREAD_POOL_EXECUTOR,
                new Callable<PublicKeyCredentialRequestOptions>() {
                    @Override
                    public PublicKeyCredentialRequestOptions call() {
                        gaeService = GAEService.getInstance(Fido2DemoActivity.this, googleSignInAccount);
                        return gaeService.getSignRequest(
                                FluentIterable.from(adapter.getCheckedItems())
                                        .transform(i -> i.get(KEY_KEY_HANDLE))
                                        .filter(i -> !Strings.isNullOrEmpty(i))
                                        .toList());
                    }
                });
    }

    private Task<String> asyncUpdateSignResponseToServer(
            final AuthenticatorAssertionResponse response) {
        return Tasks.call(
                THREAD_POOL_EXECUTOR,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        gaeService = GAEService.getInstance(Fido2DemoActivity.this, googleSignInAccount);
                        return gaeService.getSignResponseFromServer(response);
                    }
                });
    }

    private void highlightAuthenticatedToken(String signResult) {
        String credentialId;
        try {
            JSONObject signResultJson = new JSONObject(signResult);
            JSONObject credentialJson = signResultJson.getJSONObject(KEY_CREDENTIAL);
            credentialId = credentialJson.getString(KEY_CREDENTIAL_ID);
        } catch (JSONException e) {
            Log.e(TAG, "Error extracting information from JSON sign result", e);
            return;
        }
        int whichToken = -1;
        Log.i(TAG, "Successfully authenticated credential Id: " + credentialId);
        for (int position = 0; position < securityTokens.size(); position++) {
            Map<String, String> tokenMap = securityTokens.get(position);
            Log.d(TAG, "token map at position " + position + " is " + tokenMap.toString());
            Log.i(
                    TAG,
                    "highlightAuthenticatedToken registered public_key: " + tokenMap.get(KEY_KEY_HANDLE));
            if (credentialId.equals(tokenMap.get(KEY_KEY_HANDLE))) {
                whichToken = position;
                break;
            }
        }
        if (whichToken >= 0) {
            Log.i(TAG, "highlightAuthenticatedToken whichToken: " + whichToken);
            View card =
                    recyclerView
                            .getLayoutManager()
                            .findViewByPosition(whichToken)
                            .findViewById(R.id.information);
            card.setPressed(true);
            card.setPressed(false);
        }
    }

    private Task<List<Map<String, String>>> asyncRefreshSecurityKey() {
        return Tasks.call(
                THREAD_POOL_EXECUTOR,
                new Callable<List<Map<String, String>>>() {
                    @Override
                    public List<Map<String, String>> call() {
                        gaeService = GAEService.getInstance(Fido2DemoActivity.this, googleSignInAccount);
                        return gaeService.getAllSecurityTokens();
                    }
                });
    }

    private Task<String> asyncRemoveSecurityKey(final int tokenPositionInList) {
        return Tasks.call(
                THREAD_POOL_EXECUTOR,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        gaeService = GAEService.getInstance(Fido2DemoActivity.this, googleSignInAccount);
                        return gaeService.removeSecurityKey(
                                securityTokens.get(tokenPositionInList).get(KEY_CREDENTIAL_ID));
                    }
                });
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(googleApiClient)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                clearAccountSignInStatus();
                                updateUI();
                                gaeService = null;
                            }
                        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (RC_SIGN_IN == requestCode) {
            GoogleSignInResult siginInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(siginInResult);
            return;
        }

        switch (resultCode) {
            case RESULT_OK:
                if (data.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
                    Log.d(TAG, "Received error response from Google Play Services FIDO2 API");
                    AuthenticatorErrorResponse response =
                            AuthenticatorErrorResponse.deserializeFromBytes(
                                    data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA));
                    Toast.makeText(
                            Fido2DemoActivity.this, "Operation failed\n" + response, Toast.LENGTH_SHORT)
                            .show();
                } else if (requestCode == REQUEST_CODE_REGISTER) {
                    Log.d(TAG, "Received register response from Google Play Services FIDO2 API");
                    AuthenticatorAttestationResponse response =
                            AuthenticatorAttestationResponse.deserializeFromBytes(
                                    data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA));
                    Toast.makeText(
                            Fido2DemoActivity.this,
                            "Registration key handle:\n"
                                    + BaseEncoding.base64().encode(response.getKeyHandle()),
                            Toast.LENGTH_SHORT)
                            .show();
                    updateRegisterResponseToServer(response);
                } else if (requestCode == REQUEST_CODE_SIGN) {
                    Log.d(TAG, "Received sign response from Google Play Services FIDO2 API");
                    AuthenticatorAssertionResponse response =
                            AuthenticatorAssertionResponse.deserializeFromBytes(
                                    data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA));
                    Toast.makeText(
                            Fido2DemoActivity.this,
                            "Sign key handle:\n" + BaseEncoding.base64().encode(response.getKeyHandle()),
                            Toast.LENGTH_SHORT)
                            .show();
                    updateSignResponseToServer(response);
                }
                break;

            case RESULT_CANCELED:
                Toast.makeText(Fido2DemoActivity.this, "Operation is cancelled", Toast.LENGTH_SHORT).show();
                break;

            default:
                Toast.makeText(
                        Fido2DemoActivity.this,
                        "Operation failed, with resultCode " + resultCode,
                        Toast.LENGTH_SHORT)
                        .show();
                break;
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        Log.d(TAG, "sign in result: " + result.getStatus().toString());
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            saveAccountSignInStatus();
            Log.d(TAG, "account email" + acct.getEmail());
            Log.d(TAG, "account displayName" + acct.getDisplayName());
            Log.d(TAG, "account id" + acct.getId());
            Log.d(TAG, "account idToken" + acct.getIdToken());
            Log.d(TAG, "account scopes" + acct.getGrantedScopes());
        } else {
            clearAccountSignInStatus();
        }
        updateUI();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case GET_ACCOUNTS_PERMISSIONS_REQUEST_REGISTER:
                Log.d(TAG, "onRequestPermissionsResult");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getRegisterRequest();
                }
                return;
            case GET_ACCOUNTS_PERMISSIONS_REQUEST_SIGN:
                getSignRequest();
                return;
            case GET_ACCOUNTS_PERMISSIONS_ALL_TOKENS:
                updateAndDisplayRegisteredKeys();
                return;
            default:
                // TODO: better error handling
                return;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            default:
                // TODO: better error handling
                break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_signin:
                signIn();
                break;
            case R.id.nav_signout:
                signOut();
                break;
            case R.id.nav_register:
                getRegisterRequest();
                break;
            case R.id.nav_auth:
                getSignRequest();
                break;
            case R.id.nav_github:
                Intent browser =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_location)));
                this.startActivity(browser);
                break;
            default:
                // TODO: better error handling
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void saveAccountSignInStatus() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Constants.PREF_SIGNED_IN_STATUS, true);
        Log.d(TAG, "Save account sign in status: true");
        editor.apply();
    }

    private void clearAccountSignInStatus() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Constants.PREF_SIGNED_IN_STATUS, false);
        Log.d(TAG, "Clear account sign in status");
        editor.apply();
    }

    private boolean getAccountSignInStatus() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        return settings.getBoolean(Constants.PREF_SIGNED_IN_STATUS, false);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
