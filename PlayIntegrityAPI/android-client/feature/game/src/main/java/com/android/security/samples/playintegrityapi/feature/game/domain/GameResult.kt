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

package com.android.security.samples.playintegrityapi.feature.game.domain

import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken

sealed interface GameResult<out T> {
    data class Success<T>(val data: T) : GameResult<T>

    sealed interface Failure : GameResult<Nothing> {
        val message: String

        data class NetworkError(override val message: String) : Failure

        data class IntegrityError(
            override val message: String,
            val token: StandardIntegrityToken? = null
        ) : Failure
    }
}