package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigInteger

@Entity(
  tableName = "users",
  indices = [Index(value = ["pollarUserId"], unique = true)]
)
data class UserEntity(
  @PrimaryKey val id: String,
  val pollarUserId: String,
  val displayName: String?,
  val email: String?,
  val walletAddress: String?,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "ralars",
  indices = [
    Index(value = ["slug"], unique = true),
    Index(value = ["ownerId", "status"])
  ]
)
data class RalarEntity(
  @PrimaryKey val id: String,
  val ownerId: String,
  val type: RalarType,
  val title: String,
  val slug: String,
  val description: String,
  val status: RalarStatus = RalarStatus.DRAFT,
  val startsAt: Long? = null,
  val endsAt: Long? = null,
  val location: String? = null,
  val capacity: Int? = null,
  val seatsTaken: Int = 0,
  val targetAmountMinor: BigInteger? = null,
  val ticketPriceMinor: BigInteger? = null,
  val registrationFeeMinor: BigInteger? = null,
  val prizePoolMinor: BigInteger? = null,
  val assetCode: String = "USDC",
  val assetIssuer: String? = null,
  val paymentDestination: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "participations",
  indices = [
    Index(value = ["ralarId", "userId"], unique = true),
    Index(value = ["ralarId"])
  ]
)
data class ParticipationEntity(
  @PrimaryKey val id: String,
  val ralarId: String,
  val userId: String,
  val status: ParticipationStatus = ParticipationStatus.REGISTERED,
  val ticketType: String? = null,
  val amountDueMinor: BigInteger = BigInteger.ZERO,
  val isWinner: Boolean = false
)

@Entity(
  tableName = "payments",
  indices = [
    Index(value = ["memoRef"], unique = true),
    Index(value = ["ralarId", "idempotencyKey"], unique = true),
    Index(value = ["providerTransactionId"], unique = true),
    Index(value = ["ralarId", "status"])
  ]
)
data class PaymentEntity(
  @PrimaryKey val id: String,
  val ralarId: String,
  val participationId: String? = null,
  val payerUserId: String? = null,
  val kind: PaymentKind,
  val amountMinor: BigInteger,
  val assetCode: String,
  val assetIssuer: String? = null,
  val destination: String,
  val memoRef: String,
  val status: PaymentStatus = PaymentStatus.PENDING,
  val idempotencyKey: String,
  val provider: String = "pollar",
  val providerTransactionId: String? = null,
  val failureCode: String? = null,
  val failureMessage: String? = null,
  val verifiedAt: Long? = null,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "payouts",
  indices = [
    Index(value = ["memoRef"], unique = true),
    Index(value = ["ralarId", "prizeKey"], unique = true),
    Index(value = ["providerTransactionId"], unique = true),
    Index(value = ["ralarId", "status"])
  ]
)
data class PayoutEntity(
  @PrimaryKey val id: String,
  val ralarId: String,
  val recipientUserId: String? = null,
  val recipientAddress: String,
  val amountMinor: BigInteger,
  val assetCode: String,
  val assetIssuer: String? = null,
  val memoRef: String,
  val prizeKey: String,
  val status: PayoutStatus = PayoutStatus.AUTHORIZED,
  val idempotencyKey: String,
  val providerTransactionId: String? = null,
  val failureCode: String? = null,
  val verifiedAt: Long? = null,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "audit_logs",
  indices = [
    Index(value = ["ralarId"]),
    Index(value = ["actorUserId"])
  ]
)
data class AuditLogEntity(
  @PrimaryKey val id: String,
  val actorUserId: String? = null,
  val ralarId: String? = null,
  val action: String,
  val resourceType: String,
  val resourceId: String,
  val metadataJson: String = "{}",
  val createdAt: Long = System.currentTimeMillis()
)
