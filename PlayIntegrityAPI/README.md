# Play Integrity API: E2E Sample app

> [!NOTE] 
> Disclaimer: Non-Goals
>
> This project is designed for demonstration and educational purposes to provide
> a blueprint for technical integration. It is not the goal of this sample app
> to provide a production-ready anti-abuse strategy.
>
> While the sample demonstrates best practices for token handling and
> server-side verification, it is not a substitute for a comprehensive security
> audit. Developers should treat the Play Integrity API as one signal within a
> broader, multi-layered anti-abuse strategy tailored to their specific business
> risks.

# Setup

To run the Play Integrity API Canonical Sample end-to-end, you need to configure
a Google Cloud project, register your app in the Google Play Console, and set up
both the local Node.js server and the Android client.

### Prerequisites

*   [Node.js](https://nodejs.org/en) v18 or higher installed.
*   The latest version of [Android Studio](https://developer.android.com/studio)
    installed.
*   A Google Play Developer account.
*   A Google Cloud account.

## Step 1: Configure Google Cloud & Play Console

First, establish the connection between your Google Cloud project and your
Google Play app entry.

1.  Open the [Google Cloud Console](https://console.cloud.google.com/) and
    create a new project.
2.  Navigate to **APIs & Services** \> **Library**, search for the **Google Play
    Integrity API**, and click **Enable**.
3.  Open the [Google Play Console](https://play.google.com/console/) and create
    a new app entry.
    *   *Note: Choose your package name carefully. You will use this exact
        package name to configure both the Android client and the Node.js server
        later.*
4.  In the Play Console left navigation menu, select **Protected with Play**,
    and then click **Get Started** on the **Play Integrity API** card.
5.  Follow the on-screen instructions to link the Google Cloud project you
    created in step 1\.
6.  Enable the population of the following
    [optional verdicts](https://developer.android.com/google/play/integrity/verdicts#optional-device-labels)
    within the same section:
    *   `MEETS_STRONG_INTEGRITY`
    *   `MEETS_BASIC_INTEGRITY`
    *   Device attributes
    *   App access risk
    *   Play Protect

## Step 2: Generate Service Account Credentials

Your local server needs credentials to securely communicate with Google Cloud.

1.  In the Google Cloud Console, navigate to **IAM & Admin** \> **Service
    Accounts**.
2.  Click **Create Service Account**. (Default settings are fine; no special
    roles are required).
3.  Click on your newly created Service Account, navigate to the **Keys** tab,
    and select **Add Key** \> **Create new key**.
4.  Select **JSON** as the key type and click **Create** to download the
    credentials file to your machine.

## Step 3: Download the Project

Clone the repository containing the sample code to your local machine.

```shell
git clone https://github.com/android/security-samples.git
cd security-samples
```

## Step 4: Local Server Setup

Configure and run the Node.js backend.

1.  Navigate to the server directory: `cd PlayIntegrityAPI/node-server`
2.  Install the required dependencies: `npm install`
3.  Move the downloaded JSON credentials file from Step 2 into the root of the
    `node-server` directory.
4.  Rename the file to `google-credentials.json`
    *   *Note: This filename is listed in `.gitignore` to prevent accidental
        credential leaks*
5.  Create a file named `.env` in the root of the `node-server` directory and
    define the following variables:
    *   `PACKAGE_NAME="com.your.package.name"` \# Use the package name of the
        Play Console app entry created in Step 1
    *   `GOOGLE_CREDENTIALS_PATH="./google-credentials.json"`
6.  Start the server: `node app.js`

## Step 5: Android Client Setup

Configure the Android app to communicate with your local server and your
specific Google Cloud project.

1.  Open the `PlayIntegrityAPI/android-client` directory using Android Studio
2.  Open the `local.properties` file in the project root and add your Google
    Cloud project number:
    *   `GCP_PROJECT_NUMBER=1234567890`
3.  Open the app-level `build.gradle.kts` file and update the `applicationId` to
    match the package name of the app entry you created in the Play Console.
4.  Sync your project with Gradle files.
5.  Select the `physicalRelease` variant from the Build Variants tool window,
    opened via the Tool Window bar on the far left of the Android Studio
    interface
6.  [Generate a signed Android App Bundle (AAB)](https://developer.android.com/studio/publish/app-signing)
    using Android Studio.
7.  In the Google Play Console,
    [set up an internal testing track](https://support.google.com/googleplay/android-developer/answer/9845334)
    and upload your signed AAB as a new release.
8.  Once the release is processed, use the internal testing link provided in the
    Play Console to install the app onto your physical test device.
9.  To allow the app on your device to communicate with your local machine's
    backend server, connect the device via USB and set up ADB reverse port
    forwarding in your terminal (replace `<SERVER_PORT>` with your Node server
    port, e.g., 3000, and `<CLIENT_PORT>` with the port number that the app
    tries to access, e.g. 3000): `adb reverse tcp:<CLIENT_PORT>
    tcp:<SERVER_PORT>`
    *   Note: once the device is disconnected, you will need to run this command
        again the next time you need to test this flow

# Banking Micro App: Client

[This directory](android-client/feature/bank) contains the Android client
implementation for the Bank micro-app. It focuses on the practical mechanics of
integrating the Play Integrity API Standard Request flow, including token
preparation, payload hashing, and UI-level error handling (Remediation Dialogs).

### 1\. Token preparation (warm-up)

To ensure the high-value action (tapping "Transfer") executes with minimal
latency, the client asynchronously calls
`StandardIntegrityManager.prepareIntegrityToken()` method to pre-warm the token
provider when the user navigates to the Transaction portal.

*   Implementation: This logic is managed within the
    [BankViewModel.kt](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/ui/BankViewModel.kt)
    `init` block, which delegates to the
    [IntegrityRepositoryImpl.kt](android-client/core/integrity/src/main/java/com/android/security/samples/playintegrityapi/core/integrity/IntegrityRepository.kt)'s
    `warmUp()` method.

### 2\. Request hash generation

When the user initiates the transfer, the client serializes the transaction
details into a JSON string and computes its SHA-256 hash using utilities in the
`:core:common` module.

*   Implementation: See the
    [SubmitSecureTransferUseCase.kt](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/domain/SubmitSecureTransferUseCase.kt).
    The resulting Base64-encoded SHA-256 string is passed directly as the
    `requestHash` parameter into
    `StandardIntegrityTokenProvider.requestIntegrityToken()`.

### 3\. Network execution

Once the Play Integrity token is generated, it is passed down to the
`:core:network` layer.

*   **Implementation:** The
    [SubmitSecureTransferUseCase](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/domain/SubmitSecureTransferUseCase.kt)
    invokes the
    [BankRepository](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/data/repository/BankRepository.kt),
    placing the raw JSON transaction in the HTTP body and injecting the token
    into the `x-play-integrity-token` HTTP header for submission to the server.

### 4\. Handling remediation

A core feature of this client implementation is gracefully handling server-side
integrity rejections.

![Triggering the GET\_INTEGRITY Remediation Dialog](media/bank_app_remediation.gif)

***Figure 1\.** Triggering the GET\_INTEGRITY Remediation Dialog*

If the backend decides the device or app does not meet the required security
policy, it returns a 403 Forbidden with a structured JSON payload.

**Example Server Response:**

```json
{
  "status": "ERROR",
  "error_code": "INTEGRITY_REJECTED",
  "message": "Device does not meet the required security standards.",
  "remediation_code": 4
}
```

**Client response flow:**

1.  **Parsing:**
    [SubmitSecureTransferUseCase](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/domain/SubmitSecureTransferUseCase.kt)
    parses the 403 error and extracts the
    [remediationCode](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/domain/SubmitSecureTransferUseCase.kt).
2.  **UI State Update:** The
    [BankViewModel](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/ui/BankViewModel.kt)
    updates the
    [TransferUiState](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/ui/BankViewModel.kt)
    to
    [TransferUiState.Error.Integrity.Server](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/ui/BankViewModel.kt),
    making the remediation type code available to the UI.
3.  **UI Prompt:**
    [BankRoute](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/ui/BankScreen.kt)
    composable observes this state and presents an
    [AlertDialog](https://developer.android.com/develop/ui/compose/components/dialog)
    to the user.
4.  **Triggering the Dialog:** If the user chooses to resolve it,
    [BankViewModel.triggerRemediationDialog()](android-client/feature/bank/src/main/java/com/android/security/samples/playintegrityapi/feature/bank/ui/BankViewModel.kt)
    invokes the `standardIntegrityManager.showDialog()` via the
    [IntegrityRepository](android-client/core/integrity/src/main/java/com/android/security/samples/playintegrityapi/core/integrity/IntegrityRepository.kt)
    to display the
    [GET\_INTEGRITY](https://developer.android.com/google/play/integrity/remediation#get-integrity-dialog)
    dialog.
5.  **Resolution:** The client handles the result. If successful, the user can
    re-attempt the transfer.

--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

# Banking Micro App: Server

[This directory](node-server/src/features/bank) contains the backend logic for
the Bank micro-app. It demonstrates how to securely parse HTTP requests,
cryptographically validate Play Integrity tokens, and enforce business rules
using a dedicated Policy file.

## Architecture Overview

The feature is split into two layers to maintain clean architecture: the
Controller
([bank.controller.js](node-server/src/features/bank/bank.controller.js)) and the
Policy ([bank.policy.js](node-server/src/features/bank/bank.policy.js)).

### 1\. The Controller (bank.controller.js)

The Controller handles the cryptographic validation phase of the request. The
token decoding is modularized into middleware. Its execution flow is as follows:

1.  **Extract & Decode Middleware:** The
    [extractIntegrityToken](node-server/src/middleware/integrity.middleware.js)
    middleware retrieves the `x-play-integrity-token` from the HTTP headers and
    delegates decoding to the global
    [integrity.service.js](node-server/src/services/integrity.service.js).
2.  **Hash:** The controller computes a SHA-256 hash of the incoming JSON body
    ([serverRequestHash](node-server/src/features/bank/bank.controller.js)).
3.  **Binding (Tampering Protection):** The controller compares the
    `serverRequestHash` against the `requestHash` returned inside the decrypted
    token
    ([tokenPayload.requestDetails.requestHash](node-server/src/features/bank/bank.controller.js)).
    *   Rejection: If they do not match, it means the payload was altered in
        transit (Payload Hijacking / MitM). The controller instantly throws a
        `403 Forbidden` with `error_code: "REQUEST_TAMPERED"`.

### 2\. The Policy (bank.policy.js)

Once the token is proven mathematically valid and bound to the correct payload,
the Controller delegates business-logic enforcement to the Policy layer by
calling
[bankPolicy.evaluateTransferPolicy(tokenPayload)](node-server/src/features/bank/bank.controller.js).

The [BankPolicy](node-server/src/features/bank/bank.policy.js) class isolates
the specific rules required for the financial transaction. It inspects the JSON
verdicts and returns a boolean. For a transfer to succeed, the token must
satisfy all of the following:

*   **deviceRecognitionVerdict:** Must meet at least `MEETS_DEVICE_INTEGRITY`
*   **appRecognitionVerdict:** Must equal `PLAY_RECOGNIZED`.
*   **appLicensingVerdict:** Must equal `LICENSED`.
*   **requestPackageName:** Must match the expected package name on the server.

### 3\. Structured error formatting

If `evaluateTransferPolicy` returns false, the Controller translates the failure
into an actionable HTTP response for the Android client.

Rather than returning a generic 500 Internal Server Error, the API is designed
to return a 403 Forbidden containing the exact parameters the Android client
needs to trigger an in-app Remediation Dialog:

```json
{
  "status": "ERROR",
  "error_code": "INTEGRITY_REJECTED",
  "message": "Device does not meet the required security standards.",
  "remediation_code": 4
}
```

This payload ensures the client knows *why* the request failed and *how* to
utilize Play Integrity API to fix it.

--------------------------------------------------------------------------------

# Streaming Micro-App: Client

[This directory](android-client/feature/streaming) contains the Android client
implementation for the Streaming micro-app. It focuses on configuring Android’s
[ExoPlayer](https://developer.android.com/media/media3/exoplayer) to utilize
Play Integrity tokens when setting up a
[DASH playback](https://developer.android.com/media/media3/exoplayer/dash).

#### 1\. Token Preparation

Media playback requires near-instantaneous network requests to prevent
buffering. To achieve this, the client pre-warms the token provider via the
`IntegrityRepository.warmUp()` method during application’s `onCreate()` method
so that subsequent integrity token requests before video playback only incurs a
latency of a few hundred milliseconds.

In addition, if the integrity token generation fails, the client catches the
exception and intentionally proceeds with a null token rather than hard-failing.
This allows the backend to gracefully fall back to the lowest Restricted tier
(144p) instead of blocking the user.

#### 2\. Request Hash Generation (Content Binding)

Unlike the Bank micro-app where the payload is a complex transaction, the
streaming payload is simply the requested video context. The client manually
constructs a tight JSON string:

```json
{
  "action": "fetch_manifest",
  "contentId": "sample_video_01"
}
```

It then generates a SHA-256 hash of this string to produce the `requestHash`
which is passed to the Play Integrity API when requesting a token.

In a production environment, you should strengthen this binding further. Instead
of just hashing the action and content ID, consider including a non sensitive
user-specific identifier within the hashed data. This could be:

*   A server-side session token
*   The user's unique ID
*   A nonce tied to the user's current session.

By including a session-specific element, the JSON to be hashed might look like:

```json
{
  "action": "fetch_manifest",
  "contentId": "sample_video_01",
  "sessionId": "user_session_abc123"
}
```

Hashing this entire string ensures the integrity token is bound not only to the
content but also to that specific user session, making it more difficult for an
attacker to replay a token across different users or sessions. The backend must
then be able to reconstruct and verify this same hash based on the authenticated
user's session and the requested content.

#### 3\. ExoPlayer Network Injection

The client does not manually download the XML manifest. Instead, it natively
instructs [ExoPlayer](https://developer.android.com/media/media3/exoplayer) to
append the integrity token to its outbound HTTP headers.

The preparePlayerMediaSource() method instantiates a
[DefaultHttpDataSource.Factory()](https://developer.android.com/reference/androidx/media3/datasource/DefaultHttpDataSource.Factory).
We use
[setDefaultRequestProperties()](https://developer.android.com/reference/androidx/media3/datasource/DefaultHttpDataSource.Factory#setDefaultRequestProperties\(java.util.Map%3Cjava.lang.String,java.lang.String%3E\))
to inject the `X-Play-Integrity-Token header`. This factory is passed into the
[DashMediaSource](https://developer.android.com/reference/androidx/media3/exoplayer/dash/DashMediaSource),
ensuring the token is present when ExoPlayer requests the .mpd file over the
network.

#### 4\. Handling Dynamic Tiers & UI State

The Android client is completely agnostic to the quality tier it receives.
ExoPlayer automatically parses the dynamically filtered DASH manifest returned
by the Node.js server.

The `StreamingViewModel` attaches a
[Player.Listener](https://developer.android.com/reference/androidx/media3/common/Player.Listener)
to the player. When the manifest loads, `onTracksChanged()` scans the available
video tracks to find the maximum videoHeight the server authorized. It then maps
this height (e.g., \>= 1080, \>= 720\) to the
`StreamingUiState.activeTierIndex`, which instantly updates the Compose UI to
highlight the correct "Premium", "Standard", or "Restricted" card on the screen.

# Streaming Micro-App: Server

[This directory](node-server/src/features/streaming) contains the backend logic
for the Streaming micro-app. It demonstrates how to parse standard integrity
tokens, enforce tiered access policies, and dynamically modify DASH XML
manifests.

The backend is structured around three primary components: the Controller, the
Policy, and the Manifest Service.

1.  The Controller
    ([streaming.controller.js](node-server/src/features/streaming/streaming.controller.js)):
    The controller handles cryptographic validation and routing.
    *   **Token Decoding & Replay Protection:** The token is extracted and
        decoded via the `integrity.middleware`. Because we use Standard
        requests, Google's server
        [automatically detects](https://developer.android.com/google/play/integrity/standard#replay-protection)
        and rejects replayed tokens.
    *   **Content Binding Check:** The `#isContentBindingValid` method creates a
        mirror payload (`{ action: 'fetch_manifest', contentId: 'contentId'})`,
        computes its hash, and compares it against the `requestHash` inside the
        decoded token. If they mismatch, the controller falls back to the
        Restricted tier.
2.  The Policy
    ([streaming.policy.js](node-server/src/features/streaming/streaming.policy.js)):
    This file evaluates the trust level of the device to determine the maximum
    allowed video resolution. The `evaluateStreamQuality` function translates
    Play Integrity labels into business rules:
    *   1080p (Premium): Requires `MEETS_STRONG_INTEGRITY` and an Android SDK
        version \>= 33\.
    *   720p (High): Requires `MEETS_DEVICE_INTEGRITY` (SDK \>= 33\) or
        `MEETS_STRONG_INTEGRITY` (SDK \< 33).
    *   480p (Standard): Requires `MEETS_DEVICE_INTEGRITY` (SDK \< 33).
    *   240p (Basic): Requires `MEETS_BASIC_INTEGRITY`.
    *   144p (Restricted): The default fallback.
3.  The Manifest Service
    ([manifest.service.js](node-server/src/features/streaming/manifest.service.js)):
    Once the policy determines the maximum allowed resolution, the controller
    delegates to the `ManifestService`. The service parses the root XML manifest
    using the xml2js library. The `#filterVideoRepresentations` method locates
    the `AdaptationSet` for video and strips out any `<Representation>` nodes
    that exceed the permitted height limit dictated by the policy.

—--------------------------------------------------------------------------------------------------------------------------------------------------------

# Game Micro-App: Client

[This directory](android-client/feature/game) contains the Android client
implementation for the Game micro-app. It focuses on orchestrating a real-time
game session, handling continuous background Play Integrity token generation on
dynamic intervals, and utilizing several Play remediation dialogs.

#### 1\. Token preparation and session initialisation

When the user navigates to the Rhythm Pulse micro-app, the client automatically
pre-warms the token provider using `IntegrityRepository.warmUp()`. \
When the user taps Start Secure Session, the `InitiateGameUseCase` fetches an
initial Play Integrity token and calls `POST /api/v1/game/initiate`. The server
returns a unique `sessionId`, the game’s `targetTime`, and an array of
randomised check-in intervals (e.g. `[2.5, 5.12, 8.3]`).

#### 2\. TOCTOU defence

Once gameplay begins, the client must periodically prove its environment remains
secure.

*   An asynchronous coroutine monitors the intervals returned by the server.
*   When an interval is reached, the client applies a randomised padding offset
    (jitter) and requests a standard integrity token in the background via the
    `GenerateIntervalTokenUseCase`.
*   Content Binding the Interval: To cryptographically bind the background token
    to this exact time window, the client hashes a dynamic challenge string.
*   The resulting SHA-256 hash is passed as the requestHash parameter, and the
    token is stored in memory alongside its interval time.

#### 3\. Session stop and final submission

When the user taps **Stop**, the `SubmitGameScoreUseCase` prepares a final
payload containing the local `actualTime` elapsed and the array of collected
background `intervalTokens`. A final closing Play Integrity token is generated,
bound to the hash of this entire final JSON payload, and submitted to `POST
/api/v1/game/stop`.

#### 4\. Handling environment remediation

![Game sample showcasing the effect of an unknown app capturing
the screen during an active game session and Play remediation](media/game_app_remediation.gif)

***Figure 2\.** Game sample showcasing the effect of an unknown app capturing
the screen during an active game session and Play remediation*

If the server rejects a session due to an environmental policy failure, the
`GameViewModel.triggerRemediationDialog()`leverages Play’s user remediation
features to prompt the user to either close apps that maybe recording their
screen or controlling their device via the `CLOSE_ALL_ACCESS_RISK` dialog, or
uninstall malicious apps on their device by using the `GET_STRONG_INTEGRITY`
dialog.

# Game Micro-App: Server

[This directory](node-server/src/features/game) contains the backend
implementation for the Game sample. It showcases a stateful, secure verification
pattern designed to defeat TOCTOU (Time-of-Check to Time-of-Use) cheats, enforce
strict environment policies, and securely evaluate background Play Integrity API
attestations.

### Architecture overview

The feature is divided into the Controller
([game.controller.js](node-server/src/features/game/game.controller.js)) and the
Policy ([game.policy.js](node-server/src/features/game/game.policy.js)). The
entire architecture relies on Play Integrity API tokens to validate state at
multiple points in time in a session.

1.  The `GameController` manages active sessions, orchestrates integrity token
    verification to decide if a user’s final score would be added to a
    hypothetical leaderboard.
2.  The `GamePolicy` class acts as the core rules engine, evaluating the
    decrypted Play Integrity JSON payload, focusing heavily on interpreting the
    `environmentDetails` returned by the Integrity API:
    *   Play Protect: Evaluates `playProtectVerdict` to ensure it equals
        `NO_ISSUES`. If threats are found, this signals a compromised
        environment harbouring potentially malicious software.
    *   App access risk evaluation: It parses the `appsDetected` array from the
        `appAccessRiskVerdict`:
        *   If the array includes `UNKNOWN_CAPTURING`, `screenCaptureSafe` is
            flagged as false.
        *   If the array includes `UNKNOWN_OVERLAYS` or `UNKNOWN_CONTROLLING`,
            `accessibilitySafe` is flagged as false, preventing automated
            clickers or malicious overlays.
    *   Strict fallback: If `environmentDetails` or the `appAccessRiskVerdict`
        are missing entirely from the integrity token payload, the policy
        securely defaults `screenCaptureSafe` and `accessibilitySafe` to false.
        This ensures the system does not blindly approve unevaluated
        environments.

### API Endpoints

#### `POST /api/v1/game/initiate`

Initialises a secure gameplay session, triggering an initial evaluation and
returning the randomised check-in intervals.

**Request headers:** \
`x-play-integrity-token`: `<token>`

**Success response (`200 OK`):**

```json
{
  "status": "SUCCESS",
  "sessionId": "b6a0ff4d-0453-481b-8512-1df69614db5a",
  "targetTime": 15.34,
  "intervals": [2.45, 6.12, 10.89],
  "checklist": {
    "isSecure": true,
    "screenCaptureSafe": true,
    "accessibilitySafe": true,
    "playProtectSafe": true
  }
}
```

#### `POST /api/v1/game/status`

Queries the real-time security state of the device environment. Typically
triggered on demand or when the mobile app is resumed from a background state.

**Request headers:** \
`x-play-integrity-token`: `<real_time_environment_integrity_token>`

**Success response (`200 OK`):**

```json
{
  "status": "SUCCESS",
  "checklist": {
    "isSecure": true,
    "screenCaptureSafe": true,
    "accessibilitySafe": true,
    "playProtectSafe": true
  }
}
```

#### `POST /api/v1/game/stop`

Submits the final score payload along with all background Play Integrity tokens
for strict verification.

**Request headers:** \
`x-play-integrity-token`: `<final_closing_integrity_token>`

**Request payload:**

```json
{
  "sessionId": "b6a0ff4d-0453-481b-8512-1df69614db5a",
  "clientStartTime": 1727247472000,
  "actualTime": 16.54,
  "intervalTokens": [
    { "interval": 2.45, "token": "<your_token>" },
    { "interval": 6.12, "token": "<your_token>" },
    { "interval": 10.89, "token": "<your_token>" }
  ]
}
```

**Success response (`200 OK`):**

```json
{
  "status": "SUCCESS",
  "message": "Score verified."
}
```

**Tampered/Compromised response (`403 Forbidden`):**

```json
{
  "status": "ERROR",
  "error_code": "ENVIRONMENT_COMPROMISED",
  "message": "Cheat toggling detected: Environment compromised at interval 6.12s."
}
```

--------------------------------------------------------------------------------

# Testing Play Console Integrity Responses

This section guides you through using the Play Integrity API test responses
feature in the
[Google Play Console](https://developer.android.com/distribute/console) to
dynamically alter the streaming quality in the sample app. This assumes you have
already completed the full end-to-end setup as described in the root project
guide (i.e. app created in Play Console, Play Integrity API enabled, Google
Cloud project linked, Node.js server running, Android app buildable).

#### Prerequisites

*   A Google Play Developer account.
*   Your app is set up in the Play Console.
*   Play Integrity API is enabled for your app and linked to your Google Cloud
    project.
*   The sample Node.js backend server is running.
*   The Android client app is installed and runnable on a device or emulator,
    signed in with a Google account.

#### Steps to Test Different Integrity Verdicts

1.  **Navigate to Play Integrity API Settings:**
    *   Open the Google Play Console.
    *   Select your application.
    *   In the Play Console left navigation menu, select **Protected with
        Play**.
    *   On the **Protected with Play** page, locate the **Play Integrity API**
        row and click the **Manage** button.

![Navigating to Play Integrity API settings on Play Console](media/integrity_api_settings_navigation.png)

2.  **Configure Test Responses:**
    *   Scroll down to the Testing section.
    *   Click on Create new test.
    *   Give your test a descriptive name (e.g., "Device Unrecognized Test").
    *   Under Email lists, select or create an email list containing the Google
        account(s) used on your test device(s).
    *   Modify the Integrity verdicts to simulate different scenarios. For
        example:
        *   Premium Quality (Fully Trusted):
            *   `appRecognitionVerdict: PLAY_RECOGNIZED`
            *   `deviceRecognitionVerdict: [MEETS_DEVICE_INTEGRITY,
            MEETS_STRONG_INTEGRITY]`
            *   `appLicensingVerdict: LICENSED`
        *   Basic Quality (Basic Integrity):
            *   `appRecognitionVerdict: PLAY_RECOGNIZED`
            *   `deviceRecognitionVerdict: [MEETS_BASIC_INTEGRITY]`
            *   `appLicensingVerdict: LICENSED`
        *   Restricted Quality (No Device Integrity):
            *   `appRecognitionVerdict: UNEVALUATED`
            *   `deviceRecognitionVerdict: []` (Empty)
            *`appLicensingVerdict: UNEVALUATED`

![UI to configure a test response](media/integrity_api_test_response_configuration.png)

3.  **Save the test configurations:**
    *   Click Create test. You might need to click Save changes at the bottom of
        the page too. Propagation time varies depending on multiple factors, but
        changes should be reflected in about 1-2 hours at the longest.
4.  **Observe in the Android App:**
    *   Open the Streaming micro-app on your test device (ensuring it's logged
        in to one of the accounts from the email list in the test
        configuration).
    *   The app might show a quality level based on a previous integrity check.
    *   Click the "Refresh Integrity Check" button within the app. This action
        forces the app to request a new Play Integrity token and DASH manifest.
    *   Play Integrity API will return a token with the verdicts you configured
        in the Play Console test.
    *   The Node.js server will decode this test token and return a DASH
        manifest filtered according to the tier mapped to the received verdicts.
    *   Observe the UI: The highlighted tier card ("Premium", "Standard", or
        "Restricted") should update, and the video playback quality will adjust
        after ExoPlayer reloads the manifest.

#### Example Scenarios to Try:

*   **Simulate a Rooted/Compromised Device:** Set `deviceRecognitionVerdict` to
    be empty. The stream should degrade to the "Restricted" tier.
*   **Simulate an Unlicensed User:** Set `appLicensingVerdict` to `UNLICENSED`.
    The stream should also degrade to the "Restricted" tier.
*   **Simulate a Fully Trusted Device & Licensed User:** Ensure verdicts are
    `MEETS_STRONG_INTEGRITY`, `PLAY_RECOGNIZED`, and `LICENSED`. The stream
    should allow "Premium" quality.

By changing the test responses in the Play Console and using the "Refresh
Integrity Check" button, you can effectively test how the end-to-end integration
handles various Play Integrity API outcomes and confirm that the stream quality
adjusts dynamically as expected.

