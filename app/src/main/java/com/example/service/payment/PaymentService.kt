package com.example.service.payment

import com.example.core.memo.MemoGenerator
import com.example.core.money.Money
import com.example.data.dao.*
import com.example.data.model.*
import com.example.service.settlement.SettlementService
import com.example.service.settlement.VerificationResult
import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class PaymentAuthorizationResponse(
  val paymentId: String,
  val destination: String,
  val amountStellar: String,
  val amountMinor: BigInteger,
  val assetCode: String,
  val assetIssuer: String?,
  val memoRef: String,
  val kind: PaymentKind
)

sealed class PaymentResult {
  data class Authorized(val authorization: PaymentAuthorizationResponse) : PaymentResult()
  data class Paid(val payment: PaymentEntity, val txHash: String) : PaymentResult()
  data class Processing(val payment: PaymentEntity, val message: String) : PaymentResult()
  data class Error(val code: String, val message: String) : PaymentResult()
}

class PaymentService(
  private val ralarDao: RalarDao,
  private val participationDao: ParticipationDao,
  private val paymentDao: PaymentDao,
  private val auditLogDao: AuditLogDao,
  private val settlementService: SettlementService
) {

  // In-memory rate limiter: ~10 requests/minute/user (Section 17)
  private val rateLimiter = ConcurrentHashMap<String, MutableList<Long>>()

  private fun checkRateLimit(userId: String): Boolean {
    val now = System.currentTimeMillis()
    val windowStart = now - 60_000L
    val timestamps = rateLimiter.computeIfAbsent(userId) { mutableListOf() }

    synchronized(timestamps) {
      timestamps.removeAll { it < windowStart }
      if (timestamps.size >= 10) {
        return false
      }
      timestamps.add(now)
      return true
    }
  }

  /**
   * Authorize Payment (Section 9 of prompt)
   * The server derives all authoritative financial values.
   */
  suspend fun authorizePayment(
    payerUserId: String,
    ralarId: String,
    idempotencyKey: String,
    causeDonationHumanAmount: String? = null
  ): PaymentResult {
    if (!checkRateLimit(payerUserId)) {
      return PaymentResult.Error("RATE_LIMIT_EXCEEDED", "Rate limit exceeded: maximum 10 payment operations per minute")
    }

    val ralar = ralarDao.getRalarById(ralarId)
      ?: return PaymentResult.Error("RALAR_NOT_FOUND", "Ralar $ralarId not found")

    if (ralar.status != RalarStatus.PUBLISHED) {
      return PaymentResult.Error("RALAR_NOT_PUBLISHED", "Cannot make payment for an un-published Ralar")
    }

    // Check idempotency first
    val existing = paymentDao.getPaymentByIdempotencyKey(ralarId, idempotencyKey)
    if (existing != null) {
      return if (existing.status == PaymentStatus.PAID) {
        PaymentResult.Paid(existing, existing.providerTransactionId ?: "")
      } else {
        PaymentResult.Authorized(
          PaymentAuthorizationResponse(
            paymentId = existing.id,
            destination = existing.destination,
            amountStellar = Money.toStellarAmount(existing.amountMinor),
            amountMinor = existing.amountMinor,
            assetCode = existing.assetCode,
            assetIssuer = existing.assetIssuer,
            memoRef = existing.memoRef,
            kind = existing.kind
          )
        )
      }
    }

    // Determine amount and payment kind based on RalarType
    val (amountMinor, kind) = when (ralar.type) {
      RalarType.EVENT -> {
        val ticketPrice = ralar.ticketPriceMinor ?: BigInteger.ZERO
        Pair(ticketPrice, PaymentKind.TICKET)
      }
      RalarType.HACKATHON -> {
        val regFee = ralar.registrationFeeMinor ?: BigInteger.ZERO
        Pair(regFee, PaymentKind.REGISTRATION_FEE)
      }
      RalarType.CAUSE -> {
        if (causeDonationHumanAmount.isNullOrBlank()) {
          return PaymentResult.Error("INVALID_AMOUNT", "Donation amount required for CAUSE")
        }
        val donationMinor = try {
          Money.toMinor(causeDonationHumanAmount)
        } catch (e: Exception) {
          return PaymentResult.Error("INVALID_AMOUNT", e.message ?: "Invalid donation amount")
        }
        if (donationMinor <= BigInteger.ZERO) {
          return PaymentResult.Error("INVALID_AMOUNT", "Donation amount must be positive")
        }
        Pair(donationMinor, PaymentKind.DONATION)
      }
    }

    // Check capacity for EVENT and HACKATHON
    if (ralar.type != RalarType.CAUSE) {
      val affected = ralarDao.atomicIncrementSeatsTaken(ralarId)
      if (affected == 0) {
        return PaymentResult.Error("SOLD_OUT", "This Ralar has reached maximum capacity")
      }
    }

    // Ensure participation record exists
    var participation = participationDao.getParticipation(ralarId, payerUserId)
    if (participation == null) {
      val pId = UUID.randomUUID().toString()
      participation = ParticipationEntity(
        id = pId,
        ralarId = ralarId,
        userId = payerUserId,
        status = ParticipationStatus.REGISTERED,
        amountDueMinor = amountMinor
      )
      participationDao.insert(participation)
    }

    val memoRef = MemoGenerator.generate()
    val paymentId = UUID.randomUUID().toString()

    val payment = PaymentEntity(
      id = paymentId,
      ralarId = ralarId,
      participationId = participation.id,
      payerUserId = payerUserId,
      kind = kind,
      amountMinor = amountMinor,
      assetCode = ralar.assetCode,
      assetIssuer = ralar.assetIssuer,
      destination = ralar.paymentDestination,
      memoRef = memoRef,
      status = PaymentStatus.PENDING,
      idempotencyKey = idempotencyKey,
      provider = "pollar"
    )

    paymentDao.insert(payment)

    auditLogDao.insert(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        actorUserId = payerUserId,
        ralarId = ralarId,
        action = "PAYMENT_AUTHORIZED",
        resourceType = "PAYMENT",
        resourceId = paymentId,
        metadataJson = "{\"amountMinor\":\"$amountMinor\",\"memoRef\":\"$memoRef\",\"kind\":\"$kind\"}"
      )
    )

    return PaymentResult.Authorized(
      PaymentAuthorizationResponse(
        paymentId = paymentId,
        destination = ralar.paymentDestination,
        amountStellar = Money.toStellarAmount(amountMinor),
        amountMinor = amountMinor,
        assetCode = ralar.assetCode,
        assetIssuer = ralar.assetIssuer,
        memoRef = memoRef,
        kind = kind
      )
    )
  }

  /**
   * Submit transaction hash after client runTx('payment')
   * Server independently verifies against Stellar testnet.
   * Only transitions to PAID after successful verification.
   */
  suspend fun submitPaymentTransaction(
    paymentId: String,
    txHash: String
  ): PaymentResult {
    val payment = paymentDao.getPaymentById(paymentId)
      ?: return PaymentResult.Error("NOT_FOUND", "Payment record not found")

    if (payment.status == PaymentStatus.PAID) {
      return PaymentResult.Paid(payment, payment.providerTransactionId ?: txHash)
    }

    // Set status to PROCESSING
    val processing = payment.copy(
      status = PaymentStatus.PROCESSING,
      providerTransactionId = txHash
    )
    paymentDao.update(processing)

    auditLogDao.insert(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        actorUserId = payment.payerUserId,
        ralarId = payment.ralarId,
        action = "PAYMENT_SUBMITTED",
        resourceType = "PAYMENT",
        resourceId = paymentId,
        metadataJson = "{\"txHash\":\"$txHash\"}"
      )
    )

    // Independent on-chain verification
    val verifyResult = settlementService.verifyOnChain(
      txHash = txHash,
      expectedDestination = payment.destination,
      expectedAmountMinor = payment.amountMinor,
      expectedAssetCode = payment.assetCode,
      expectedAssetIssuer = payment.assetIssuer,
      expectedMemoRef = payment.memoRef
    )

    return when (verifyResult) {
      is VerificationResult.Success -> {
        val paid = processing.copy(
          status = PaymentStatus.PAID,
          verifiedAt = verifyResult.confirmedAt
        )
        paymentDao.update(paid)

        // Confirm participation
        payment.participationId?.let { pId ->
          payment.payerUserId?.let { uId ->
            val p = participationDao.getParticipation(payment.ralarId, uId)
            if (p != null) {
              participationDao.update(p.copy(status = ParticipationStatus.CONFIRMED))
            }
          }
        }

        auditLogDao.insert(
          AuditLogEntity(
            id = UUID.randomUUID().toString(),
            actorUserId = payment.payerUserId,
            ralarId = payment.ralarId,
            action = "PAYMENT_VERIFIED",
            resourceType = "PAYMENT",
            resourceId = paymentId,
            metadataJson = "{\"txHash\":\"$txHash\",\"verifiedAt\":${verifyResult.confirmedAt}}"
          )
        )

        PaymentResult.Paid(paid, txHash)
      }
      is VerificationResult.Failure -> {
        if (verifyResult.reason == VerificationResult.FailureReason.MISMATCH) {
          val failed = processing.copy(
            status = PaymentStatus.FAILED,
            failureCode = "MISMATCH: ${verifyResult.detail}"
          )
          paymentDao.update(failed)

          // Release reserved seat if failed
          ralarDao.atomicDecrementSeatsTaken(payment.ralarId)

          auditLogDao.insert(
            AuditLogEntity(
              id = UUID.randomUUID().toString(),
              actorUserId = payment.payerUserId,
              ralarId = payment.ralarId,
              action = "PAYMENT_FAILED",
              resourceType = "PAYMENT",
              resourceId = paymentId,
              metadataJson = "{\"reason\":\"${verifyResult.detail}\"}"
            )
          )

          PaymentResult.Error("PAYMENT_MISMATCH", verifyResult.detail)
        } else {
          // Keep PROCESSING for reconciliation
          PaymentResult.Processing(
            processing,
            "Confirming on Stellar: ${verifyResult.detail}"
          )
        }
      }
    }
  }
}
