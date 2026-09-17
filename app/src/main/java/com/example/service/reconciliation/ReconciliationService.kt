package com.example.service.reconciliation

import com.example.core.config.RalarConfig
import com.example.data.dao.*
import com.example.data.model.*
import com.example.service.settlement.SettlementService
import com.example.service.settlement.VerificationResult
import java.util.UUID

data class ReconciliationReport(
  val paymentsChecked: Int,
  val paymentsVerified: Int,
  val paymentsFailed: Int,
  val payoutsChecked: Int,
  val payoutsVerified: Int,
  val payoutsFailed: Int
)

class ReconciliationService(
  private val paymentDao: PaymentDao,
  private val payoutDao: PayoutDao,
  private val ralarDao: RalarDao,
  private val participationDao: ParticipationDao,
  private val auditLogDao: AuditLogDao,
  private val settlementService: SettlementService
) {

  /**
   * Reconciles all PROCESSING payments and payouts.
   * - Only considers records older than 15 seconds
   * - Verifies against Stellar testnet
   * - Marks verified transactions as PAID
   * - Leaves NOT_FOUND transactions PROCESSING
   * - After 10 minutes without a valid matching transaction, marks them FAILED with "NOT_FOUND_ONCHAIN"
   */
  suspend fun reconcile(bearerToken: String?): ReconciliationReport {
    val configuredSecret = RalarConfig.reconcileSecret
    if (configuredSecret.isNotBlank() && bearerToken != "Bearer $configuredSecret") {
      throw SecurityException("Invalid reconciliation bearer authorization token")
    }

    val now = System.currentTimeMillis()
    val minAgeMs = 15_000L // 15 seconds
    val maxAgeMs = 600_000L // 10 minutes

    var paymentsChecked = 0
    var paymentsVerified = 0
    var paymentsFailed = 0

    var payoutsChecked = 0
    var payoutsVerified = 0
    var payoutsFailed = 0

    // 1. Reconcile Payments
    val processingPayments = paymentDao.getProcessingPayments()
    for (payment in processingPayments) {
      val age = now - payment.createdAt
      if (age < minAgeMs) continue

      val txHash = payment.providerTransactionId
      if (txHash.isNullOrBlank()) {
        if (age > maxAgeMs) {
          paymentDao.update(
            payment.copy(
              status = PaymentStatus.FAILED,
              failureCode = "NOT_FOUND_ONCHAIN"
            )
          )
          ralarDao.atomicDecrementSeatsTaken(payment.ralarId)
          paymentsFailed++
        }
        continue
      }

      paymentsChecked++
      val result = settlementService.verifyOnChain(
        txHash = txHash,
        expectedDestination = payment.destination,
        expectedAmountMinor = payment.amountMinor,
        expectedAssetCode = payment.assetCode,
        expectedAssetIssuer = payment.assetIssuer,
        expectedMemoRef = payment.memoRef
      )

      when (result) {
        is VerificationResult.Success -> {
          paymentDao.update(
            payment.copy(
              status = PaymentStatus.PAID,
              verifiedAt = result.confirmedAt
            )
          )
          payment.payerUserId?.let { uId ->
            val p = participationDao.getParticipation(payment.ralarId, uId)
            if (p != null) {
              participationDao.update(p.copy(status = ParticipationStatus.CONFIRMED))
            }
          }
          auditLogDao.insert(
            AuditLogEntity(
              id = UUID.randomUUID().toString(),
              actorUserId = payment.payerUserId,
              ralarId = payment.ralarId,
              action = "RECONCILE_PAYMENT_VERIFIED",
              resourceType = "PAYMENT",
              resourceId = payment.id,
              metadataJson = "{\"txHash\":\"$txHash\"}"
            )
          )
          paymentsVerified++
        }
        is VerificationResult.Failure -> {
          if (result.reason == VerificationResult.FailureReason.MISMATCH || age > maxAgeMs) {
            paymentDao.update(
              payment.copy(
                status = PaymentStatus.FAILED,
                failureCode = if (result.reason == VerificationResult.FailureReason.MISMATCH) "MISMATCH" else "NOT_FOUND_ONCHAIN"
              )
            )
            ralarDao.atomicDecrementSeatsTaken(payment.ralarId)
            paymentsFailed++
          }
        }
      }
    }

    // 2. Reconcile Payouts
    val processingPayouts = payoutDao.getProcessingPayouts()
    for (payout in processingPayouts) {
      val age = now - payout.createdAt
      if (age < minAgeMs) continue

      val txHash = payout.providerTransactionId
      if (txHash.isNullOrBlank()) {
        if (age > maxAgeMs) {
          payoutDao.update(
            payout.copy(
              status = PayoutStatus.FAILED,
              failureCode = "NOT_FOUND_ONCHAIN"
            )
          )
          payoutsFailed++
        }
        continue
      }

      payoutsChecked++
      val result = settlementService.verifyOnChain(
        txHash = txHash,
        expectedDestination = payout.recipientAddress,
        expectedAmountMinor = payout.amountMinor,
        expectedAssetCode = payout.assetCode,
        expectedAssetIssuer = payout.assetIssuer,
        expectedMemoRef = payout.memoRef
      )

      when (result) {
        is VerificationResult.Success -> {
          payoutDao.update(
            payout.copy(
              status = PayoutStatus.PAID,
              verifiedAt = result.confirmedAt
            )
          )
          auditLogDao.insert(
            AuditLogEntity(
              id = UUID.randomUUID().toString(),
              actorUserId = payout.recipientUserId,
              ralarId = payout.ralarId,
              action = "RECONCILE_PAYOUT_VERIFIED",
              resourceType = "PAYOUT",
              resourceId = payout.id,
              metadataJson = "{\"txHash\":\"$txHash\"}"
            )
          )
          payoutsVerified++
        }
        is VerificationResult.Failure -> {
          if (result.reason == VerificationResult.FailureReason.MISMATCH || age > maxAgeMs) {
            payoutDao.update(
              payout.copy(
                status = PayoutStatus.FAILED,
                failureCode = if (result.reason == VerificationResult.FailureReason.MISMATCH) "MISMATCH" else "NOT_FOUND_ONCHAIN"
              )
            )
            payoutsFailed++
          }
        }
      }
    }

    return ReconciliationReport(
      paymentsChecked = paymentsChecked,
      paymentsVerified = paymentsVerified,
      paymentsFailed = paymentsFailed,
      payoutsChecked = payoutsChecked,
      payoutsVerified = payoutsVerified,
      payoutsFailed = payoutsFailed
    )
  }
}
