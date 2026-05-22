package com.android.security.samples.playintegrityapi.feature.game.domain

import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.Utils.generateSha256Hash
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameInitiateResponse
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException


class InitiateGameUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val integrityRepository: IntegrityRepository
) {
    suspend operator fun invoke(): GameResult<GameInitiateResponse> {
        val requestHash = generateSha256Hash(UUID.randomUUID().toString())
        integrityRepository.warmUp()
        val tokenResult = integrityRepository.requestIntegrityToken(requestHash)

        if (tokenResult.isFailure) return GameResult.Failure.IntegrityError("Failed to generate local token")

        return try {
            val response = gameRepository.initiateSession(tokenResult.getOrThrow().token())
            if (response.isSuccessful && response.body() != null) {
                GameResult.Success(response.body()!!)
            } else {
                GameResult.Failure.NetworkError("Server rejected initiation: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            GameResult.Failure.NetworkError(e.message ?: "Network error")
        }
    }
}