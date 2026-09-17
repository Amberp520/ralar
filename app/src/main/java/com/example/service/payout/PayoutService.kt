/**
 * =========================================================================================
 * RALAR SECURITY ARCHITECTURE & FINANCIAL SETTLEMENT REASONING
 * =========================================================================================
 * 1. THE BROWSER / CLIENT IS THE SIGNING DEVICE, NEVER THE FINANCIAL AUTHORITY.
 *    The authenticated Pollar SDK session inside the client is strictly used to sign transactions
 *    using the user's custody key. The client has zero authority over financial values.
 *
 * 2. AUTHORITATIVE VALUES ORIGINATE EXCLUSIVELY FROM THE SERVER:
 *    - Destination wallet address is resolved server-side from the database.
 *    - Payout amount is calculated server-side in BigInt minor units (10^7 decimals).
 *    - Recipient user ID and eligibility are strictly validated server-side.
 *    - Memo reference (RLR-XXXXXXXX) is generated server-side and uniquely persisted.
 *
 * 3. INDEPENDENT ON-CHAIN VERIFICATION:
 *    The server never accepts a client's claim of "success". The client only reports
 *    a transaction hash. The server independently queries Stellar testnet (RPC / Horizon)
 *    to verify:
 *      a. Transaction exists and has status = SUCCESS.
 *      b. Payment operation matches destination, asset code, and asset issuer.
 *      c. Payment operation amount matches the authoritative minor units exactly.
 *      d. The on-chain memo matches the server-generated memoRef.
 *
 * 4. PAID STATUS IS ONLY WRITTEN AFTER INDEPENDENT VERIFICATION:
 *    Under no circumstances is a Payment or Payout marked PAID until verifiedAt is set
 *    by the independent on-chain verification engine.
 * =========================================================================================
 */

package com.example.service.payout

import com.example.core.memo.MemoGenerator
import com.example.core.money.Money
import com.example.data.dao.*
import com.example.data.model.*
import com.example.service.settlement.SettlementService
import com.example.service.settlement.VerificationResult
import java.math.BigInteger
import java.util.UUID

data class PayoutAuthorizationResponse(
  val payoutId: String,
  val destination: String,
  val amountStellar: String,
  val amountMinor: BigInteger,
  val assetCode: String,
  val assetIssuer: String?,
  val memoRef: String
)

sealed class PayoutResult {
  data class Authorized(val authorization: PayoutAuthorizationResponse) : PayoutResult()
  data class Paid(val payout: PayoutEntity, val txHash: String) : PayoutResult()
  data class Processing(val payout: PayoutEntity, val message: String) : PayoutResult()
  data class Error(val code: String, val message: String) : PayoutResult()
}

class PayoutService(
  private val ralarDao: RalarDao,
  private val participationDao: ParticipationDao,
  private val userDao: UserDao,
  private val payoutDao: PayoutDao,
  private val auditLogDao: AuditLogDao,
  private val settlementService: SettlementService
) {

  /**
   * Authorize Prize Payout (Section 10 of prompt)
   * 1. Authenticates organizer
   * 2. Verifies ownership
   * 3. Verifies Ralar type is HACKATHON
   * 4. Verifies Ralar status is PUBLISHED or CLOSED
   * 5. Resolves winner wallet address from database
   * 6. Calculates prize amount from database
   * 7. Generates a unique memo
   * 8. Creates the Payout record
   * 9. Returns payout parameters for client signing
   */
  suspend fun authorizePayout(
    organizerUserId: String,
    ralarId: String,
    winnerUserId: String,
    idempotencyKey: String
  ): PayoutResult {
    // 1 & 2. Verify Ralar and ownership
    val ralar = ralarDao.getRalarById(ralarId)
      ?: return PayoutResult.Error("RALAR_NOT_FOUND", "Ralar $ralarId does not exist")

    if (ralar.ownerId != organizerUserId) {
      return PayoutResult.Error("UNAUTHORIZED", "User is not the owner of this Ralar")
    }

    // 3. Verify type is HACKATHON
    if (ralar.type != RalarType.HACKATHON) {
      return PayoutResult.Error("INVALID_TYPE", "Payouts can only be authorized for HACKATHON Ralars")
    }

    // 4. Verify Ralar is PUBLISHED or CLOSED
    if (ralar.status == RalarStatus.DRAFT) {
      return PayoutResult.Error("INVALID_STATUS", "Cannot authorize payout for a DRAFT hackathon")
    }

    // 5. Verify winner participation
    val participation = participationDao.getParticipation(ralarId, winnerUserId)
      ?: return PayoutResult.Error("WINNER_NOT_FOUND", "User $winnerUserId is not a registered participant")

    val winnerUser = userDao.getUserById(winnerUserId)
      ?: return PayoutResult.Error("WINNER_USER_NOT_FOUND", "Participant profile not found")

    val winnerWallet = winnerUser.walletAddress
    if (winnerWallet.isNullOrBlank()) {
      return PayoutResult.Error("MISSING_WALLET", "Winner has no registered Stellar wallet address")
    }

    // 6. Calculate prize amount from database
    val prizeAmountMinor = ralar.prizePoolMinor ?: BigInteger.ZERO
    if (prizeAmountMinor <= BigInteger.ZERO) {
      return PayoutResult.Error("NO_PRIZE_POOL", "Hackathon has no prize pool configured")
    }

    // 7. Check idempotency / unique prize key
    val prizeKey = "hackathon_prize_${ralarId}_${winnerUserId}"
    val existing = payoutDao.getPayoutByPrizeKey(ralarId, prizeKey)
    if (existing != null) {
      return if (existing.status == PayoutStatus.PAID) {
        PayoutResult.Paid(existing, existing.providerTransactionId ?: "")
      } else {
        PayoutResult.Authorized(
          PayoutAuthorizationResponse(
            payoutId = existing.id,
            destination = existing.recipientAddress,
            amountStellar = Money.toStellarAmount(existing.amountMinor),
            amountMinor = existing.amountMinor,
            assetCode = existing.assetCode,
            assetIssuer = existing.assetIssuer,
            memoRef = existing.memoRef
          )
        )
      }
    }

    // 8. Generate unique memoRef and record
    val memoRef = MemoGenerator.generate()
    val payoutId = UUID.randomUUID().toString()

    val payout = PayoutEntity(
      id = payoutId,
      ralarId = ralarId,
      recipientUserId = winnerUserId,
      recipientAddress = winnerWallet,
      amountMinor = prizeAmountMinor,
      assetCode = ralar.assetCode,
      assetIssuer = ralar.assetIssuer,
      memoRef = memoRef,
      prizeKey = prizeKey,
      status = PayoutStatus.AUTHORIZED,
      idempotencyKey = idempotencyKey
    )

    payoutDao.insert(payout)
    participationDao.setWinner(ralarId, winnerUserId, true)

    auditLogDao.insert(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        actorUserId = organizerUserId,
        ralarId = ralarId,
        action = "PAYOUT_AUTHORIZED",
        resourceType = "PAYOUT",
        resourceId = payoutId,
        metadataJson = "{\"winnerUserId\":\"$winnerUserId\",\"amountMinor\":\"$prizeAmountMinor\",\"memoRef\":\"$memoRef\"}"
      )
    )

    return PayoutResult.Authorized(
      PayoutAuthorizationResponse(
        payoutId = payoutId,
        destination = winnerWallet,
        amountStellar = Money.toStellarAmount(prizeAmountMinor),
        amountMinor = prizeAmountMinor,
        assetCode = ralar.assetCode,
        assetIssuer = ralar.assetIssuer,
        memoRef = memoRef
      )
    )
  }

  /**
   * Submit transaction hash after client runTx('payment')
   * Server independently verifies against Stellar testnet.
   * Only transitions to PAID after successful verification.
   */
  suspend fun submitPayoutTransaction(
    payoutId: String,
    txHash: String
  ): PayoutResult {
    val payout = payoutDao.getPayoutById(payoutId)
      ?: return PayoutResult.Error("NOT_FOUND", "Payout record not found")

    if (payout.status == PayoutStatus.PAID) {
      return PayoutResult.Paid(payout, payout.providerTransactionId ?: txHash)
    }

    // Transition to PROCESSING
    val processing = payout.copy(
      status = PayoutStatus.PROCESSING,
      providerTransactionId = txHash
    )
    payoutDao.update(processing)

    auditLogDao.insert(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        actorUserId = payout.recipientUserId,
        ralarId = payout.ralarId,
        action = "PAYOUT_SUBMITTED",
        resourceType = "PAYOUT",
        resourceId = payoutId,
        metadataJson = "{\"txHash\":\"$txHash\"}"
      )
    )

    // Independent Stellar verification
    val verifyResult = settlementService.verifyOnChain(
      txHash = txHash,
      expectedDestination = payout.recipientAddress,
      expectedAmountMinor = payout.amountMinor,
      expectedAssetCode = payout.assetCode,
      expectedAssetIssuer = payout.assetIssuer,
      expectedMemoRef = payout.memoRef
    )

    return when (verifyResult) {
      is VerificationResult.Success -> {
        val paid = processing.copy(
          status = PayoutStatus.PAID,
          verifiedAt = verifyResult.confirmedAt
        )
        payoutDao.update(paid)

        auditLogDao.insert(
          AuditLogEntity(
            id = UUID.randomUUID().toString(),
            actorUserId = payout.recipientUserId,
            ralarId = payout.ralarId,
            action = "PAYOUT_VERIFIED",
            resourceType = "PAYOUT",
            resourceId = payoutId,
            metadataJson = "{\"txHash\":\"$txHash\",\"verifiedAt\":${verifyResult.confirmedAt}}"
          )
        )

        PayoutResult.Paid(paid, txHash)
      }
      is VerificationResult.Failure -> {
        if (verifyResult.reason == VerificationResult.FailureReason.MISMATCH) {
          val failed = processing.copy(
            status = PayoutStatus.FAILED,
            failureCode = "MISMATCH: ${verifyResult.detail}"
          )
          payoutDao.update(failed)
          PayoutResult.Error("PAYOUT_MISMATCH", verifyResult.detail)
        } else {
          // Keep PROCESSING for reconciliation
          PayoutResult.Processing(
            processing,
            "Confirming on Stellar: ${verifyResult.detail}"
          )
        }
      }
    }
  }
}
