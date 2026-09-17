package com.example.service.pollar

import com.example.core.config.RalarConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class PollarWallet(
  val address: String,
  val custody: String = "smart", // "internal" | "smart" | "external"
  val provider: String = "pollar",
  val existsOnStellar: Boolean = true
)

sealed class PollarFundResult {
  data class Success(val transactionHash: String?, val message: String) : PollarFundResult()
  data class AlreadyFunded(val message: String = "Wallet already funded on testnet") : PollarFundResult()
  data class Error(val code: String, val message: String) : PollarFundResult()
  data class ConfigurationRequired(val message: String) : PollarFundResult()
}

class PollarClient(
  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()
) {
  private val baseUrl = "https://server.api.pollar.xyz"

  /**
   * Request deferred wallet funding on Stellar testnet: POST /v1/wallets/fund
   * Requires backend POLLAR_SECRET_KEY.
   */
  suspend fun fundWallet(walletAddress: String): PollarFundResult = withContext(Dispatchers.IO) {
    val secretKey = RalarConfig.pollarSecretKey
    if (secretKey.isBlank() || secretKey.contains("...")) {
      return@withContext PollarFundResult.ConfigurationRequired(
        "POLLAR_SECRET_KEY is not configured in environment. Deferred funding requires server authorization."
      )
    }

    val payload = JSONObject().apply {
      put("address", walletAddress)
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = payload.toString().toRequestBody(mediaType)

    val request = Request.Builder()
      .url("$baseUrl/v1/wallets/fund")
      .addHeader("x-pollar-api-key", secretKey)
      .addHeader("Content-Type", "application/json")
      .post(body)
      .build()

    try {
      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      when (response.code) {
        200, 201 -> {
          val json = JSONObject(responseBody)
          val txHash = if (json.has("content")) {
            val content = json.optJSONObject("content")
            content?.optString("transactionHash", null)
          } else null
          PollarFundResult.Success(txHash, "Wallet funded successfully on Stellar testnet")
        }
        409 -> {
          // Per section 7: 409 indicating wallet already funded is treated as success
          PollarFundResult.AlreadyFunded()
        }
        else -> {
          PollarFundResult.Error(
            code = "HTTP_${response.code}",
            message = "Pollar server returned ${response.code}: $responseBody"
          )
        }
      }
    } catch (e: IOException) {
      PollarFundResult.Error("NETWORK_ERROR", e.localizedMessage ?: "Failed to connect to Pollar server")
    }
  }
}
