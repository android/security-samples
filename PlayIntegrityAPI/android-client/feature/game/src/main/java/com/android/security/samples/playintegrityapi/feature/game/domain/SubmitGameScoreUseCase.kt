package com.android.security.samples.playintegrityapi.feature.game.domain

import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.Utils.generateSha256Hash
import com.android.security.samples.playintegrityapi.feature.game.data.remote.*
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepository
import com.squareup.moshi.Moshi
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class SubmitGameScoreUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val integrityRepository: IntegrityRepository,
    moshi: Moshi
) {
    private val requestAdapter = moshi.adapter(GameStopRequest::class.java)

    suspend operator fun invoke(request: GameStopRequest): GameResult<GameStopResponse> {
        val jsonPayload = requestAdapter.toJson(request)
        val requestHash = generateSha256Hash(jsonPayload)

        val tokenResult = integrityRepository.requestIntegrityToken(requestHash)
        if (tokenResult.isFailure) return GameResult.Failure.IntegrityError("Failed to sign final payload")

        val token = tokenResult.getOrThrow()

        return try {
            val response = gameRepository.stopSession(token.token(), request)
            if (response.isSuccessful && response.body() != null) {
                GameResult.Success(response.body()!!)
            } else {
                GameResult.Failure.IntegrityError(
                    message = "Server rejected score: ${response.errorBody()?.string()}",
                    token = token
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            GameResult.Failure.NetworkError("Failed to submit score")
        }
    }
}