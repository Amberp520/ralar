package com.example.service.settlement

import com.example.core.config.RalarConfig
import com.example.core.money.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigInteger
import java.util.concurrent.TimeUnit

sealed class VerificationResult {
  data class Success(
    val hash: String,
    val confirmedAt: Long,
    val innerHash: String? = null
  ) : VerificationResult()

  data class Failure(
    val reason: FailureReason,
    val detail: String
  ) : VerificationResult()

  enum class FailureReason {
    NOT_FOUND,
    FAILED_ONCHAIN,
    MISMATCH,
    CONFIGURATION_REQUIRED
  }
}

class SettlementService(
  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()
) {

  /**
   * Independently verifies a transaction on Stellar testnet against authoritative database parameters.
   *
   * Checks strictly enforced (Section 12):
   * 1. transaction exists
   * 2. transaction succeeded
   * 3. correct payment operation exists
   * 4. destination matches exactly
   * 5. asset code matches
   * 6. asset issuer matches (if credit asset)
   * 7. amount matches after conversion to minor units (10^7)
   * 8. memo is present
   * 9. memo exactly matches the server-generated memoRef
   * Handles fee-bump transactions by following inner transaction hash.
   */
  suspend fun verifyOnChain(
    txHash: String,
    expectedDestination: String,
    expectedAmountMinor: BigInteger,
    expectedAssetCode: String,
    expectedAssetIssuer: String?,
    expectedMemoRef: String
  ): VerificationResult = withContext(Dispatchers.IO) {
    val cleanHash = txHash.trim()
    if (cleanHash.length != 64) {
      return@withContext VerificationResult.Failure(
        VerificationResult.FailureReason.MISMATCH,
        "Invalid transaction hash format: must be 64 hex characters"
      )
    }

    val horizonUrl = RalarConfig.stellarHorizonUrl.trimEnd('/')

    try {
      // 1. Fetch transaction record from Horizon
      val txRequest = Request.Builder()
        .url("$horizonUrl/transactions/$cleanHash")
        .get()
        .build()

      val txResponse = okHttpClient.newCall(txRequest).execute()
      if (txResponse.code == 404) {
        return@withContext VerificationResult.Failure(
          VerificationResult.FailureReason.NOT_FOUND,
          "Transaction not found on Stellar testnet"
        )
      }

      if (!txResponse.isSuccessful) {
        return@withContext VerificationResult.Failure(
          VerificationResult.FailureReason.FAILED_ONCHAIN,
          "Stellar testnet error: HTTP ${txResponse.code}"
        )
      }

      val txJson = JSONObject(txResponse.body?.string() ?: "{}")

      // 2. Check if transaction succeeded
      val isSuccessful = txJson.optBoolean("successful", false)
      if (!isSuccessful) {
        return@withContext VerificationResult.Failure(
          VerificationResult.FailureReason.FAILED_ONCHAIN,
          "Transaction exists on-chain but status is FAILED"
        )
      }

      // Handle fee-bump transaction: if inner_transaction_hash is present
      val innerHash = txJson.optString("inner_transaction_hash", null)
      val effectiveHash = if (!innerHash.isNullOrBlank()) innerHash else cleanHash

      // 8 & 9. Check memo matches server memoRef
      val memoOnChain = txJson.optString("memo", "")
      if (memoOnChain.isBlank()) {
        return@withContext VerificationResult.Failure(
          VerificationResult.FailureReason.MISMATCH,
          "Transaction has no memo; expected $expectedMemoRef"
        )
      }
      if (memoOnChain != expectedMemoRef) {
        return@withContext VerificationResult.Failure(
          VerificationResult.FailureReason.MISMATCH,
          "Memo mismatch: on-chain '$memoOnChain' != expected '$expectedMemoRef'"
        )
      }

      // 3. Inspect operations for the matching payment operation
      val opsRequest = Request.Builder()
        .url("$horizonUrl/transactions/$effectiveHash/operations")
        .get()
        .build()

      val opsResponse = okHttpClient.newCall(opsRequest).execute()
      if (!opsResponse.isSuccessful) {
        return@withContext VerificationResult.Failure(
          VerificationResult.FailureReason.FAILED_ONCHAIN,
          "Failed to retrieve operations for transaction: HTTP ${opsResponse.code}"
        )
      }

      val opsJson = JSONObject(opsResponse.body?.string() ?: "{}")
      val embedded = opsJson.optJSONObject("_embedded")
      val records = embedded?.optJSONArray("records")

      if (records == null || records.length() == 0) {
        return@withContext VerificationResult.Failure(
          VerificationResult.FailureReason.MISMATCH,
          "No operations found inside transaction"
        )
      }

      var paymentFound = false
      var failureDetail = "No matching payment operation found"

      for (i in 0 until records.length()) {
        val op = records.getJSONObject(i)
        val type = op.optString("type")

        if (type == "payment") {
          val to = op.optString("to")
          val amountStr = op.optString("amount")
          val assetType = op.optString("asset_type")
          val assetCode = op.optString("asset_code", if (assetType == "native") "XLM" else "")
          val assetIssuer = op.optString("asset_issuer", null)

          // Check destination
          if (to != expectedDestination) {
            failureDetail = "Destination mismatch: on-chain '$to' != expected '$expectedDestination'"
            continue
          }

          // Check asset code
          if (!assetCode.equals(expectedAssetCode, ignoreCase = true)) {
            failureDetail = "Asset code mismatch: on-chain '$assetCode' != expected '$expectedAssetCode'"
            continue
          }

          // Check asset issuer if non-native credit asset
          if (assetType != "native" && expectedAssetIssuer != null && assetIssuer != expectedAssetIssuer) {
            failureDetail = "Asset issuer mismatch: on-chain '$assetIssuer' != expected '$expectedAssetIssuer'"
            continue
          }

          // Check exact amount in minor units
          val minorAmountOnChain = try {
            Money.toMinor(amountStr)
          } catch (e: Exception) {
            failureDetail = "Could not parse on-chain amount: $amountStr"
            continue
          }

          if (minorAmountOnChain != expectedAmountMinor) {
            val expectedDisplay = Money.toStellarAmount(expectedAmountMinor)
            failureDetail = "Amount mismatch: on-chain $amountStr != expected $expectedDisplay"
            continue
          }

          paymentFound = true
          break
        }
      }

      if (!paymentFound) {
        return@withContext VerificationResult.Failure(
          VerificationResult.FailureReason.MISMATCH,
          failureDetail
        )
      }

      // Success
      val createdAtStr = txJson.optString("created_at")
      val confirmedAt = System.currentTimeMillis()

      VerificationResult.Success(
        hash = cleanHash,
        confirmedAt = confirmedAt,
        innerHash = innerHash
      )

    } catch (e: Exception) {
      VerificationResult.Failure(
        VerificationResult.FailureReason.NOT_FOUND,
        "Settlement lookup failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
      )
    }
  }
}
