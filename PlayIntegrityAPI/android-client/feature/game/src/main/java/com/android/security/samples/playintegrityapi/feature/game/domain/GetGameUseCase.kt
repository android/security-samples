package com.android.security.samples.playintegrityapi.feature.game.domain

import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.Utils.generateSha256Hash
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStatusResponse
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class GetGameStatusUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val integrityRepository: IntegrityRepository
) {
    suspend operator fun invoke(): GameResult<GameStatusResponse> {
        val requestHash = generateSha256Hash("status_check_${System.currentTimeMillis()}")
        val tokenResult = integrityRepository.requestIntegrityToken(requestHash)

        if (tokenResult.isFailure) return GameResult.Failure.IntegrityError("Token failed")

        return try {
            val response = gameRepository.getStatus(tokenResult.getOrThrow().token())
            if (response.isSuccessful && response.body() != null) GameResult.Success(response.body()!!)
            else GameResult.Failure.NetworkError("Failed to fetch status")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            GameResult.Failure.NetworkError("Network error")
        }
    }
}
