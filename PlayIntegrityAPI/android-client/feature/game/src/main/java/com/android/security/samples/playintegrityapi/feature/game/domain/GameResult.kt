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