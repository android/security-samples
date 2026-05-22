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

const HEADERS = {
    PLAY_INTEGRITY_TOKEN: 'x-play-integrity-token'
};

const INTEGRITY_VERDICTS = {
    DEVICE: {
        MEETS_DEVICE_INTEGRITY: 'MEETS_DEVICE_INTEGRITY',
        MEETS_VIRTUAL_INTEGRITY: 'MEETS_VIRTUAL_INTEGRITY',
        MEETS_STRONG_INTEGRITY: 'MEETS_STRONG_INTEGRITY',
        MEETS_BASIC_INTEGRITY: 'MEETS_BASIC_INTEGRITY'
    },
    APP: {
        PLAY_RECOGNIZED: 'PLAY_RECOGNIZED',
        UNRECOGNIZED_VERSION: 'UNRECOGNIZED_VERSION',
        UNEVALUATED: 'UNEVALUATED'
    },
    ACCOUNT: {
        LICENSED: 'LICENSED',
        UNLICENSED: 'UNLICENSED',
        UNEVALUATED: 'UNEVALUATED'
    },
    ENVIRONMENT: {
        PLAY_PROTECT: {
            NO_ISSUES: 'NO_ISSUES',
            NO_DATA: 'NO_DATA',
            POSSIBLE_RISK: 'POSSIBLE_RISK',
            MEDIUM_RISK: 'MEDIUM_RISK',
            HIGH_RISK: 'HIGH_RISK'
        },
        APP_ACCESS_RISK: {
            KNOWN_INSTALLED: 'KNOWN_INSTALLED',
            UNKNOWN_CAPTURING: 'UNKNOWN_CAPTURING',
            UNKNOWN_OVERLAYS: 'UNKNOWN_OVERLAYS',
            UNKNOWN_CONTROLLING: 'UNKNOWN_CONTROLLING',
            KNOWN_CONTROLLING: 'KNOWN_CONTROLLING',
            UNEVALUATED: 'UNEVALUATED'
        }
    }
};

module.exports = {
    HEADERS,
    INTEGRITY_VERDICTS
};