package com.example.service.ralar

import com.example.data.dao.AuditLogDao
import com.example.data.dao.RalarDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.RalarEntity
import com.example.data.model.RalarStatus
import com.example.data.model.RalarType
import java.math.BigInteger
import java.util.Locale
import java.util.UUID

sealed class RalarServiceResult<out T> {
  data class Success<out T>(val data: T) : RalarServiceResult<T>()
  data class Error(val code: String, val message: String) : RalarServiceResult<Nothing>()
}

class RalarService(
  private val ralarDao: RalarDao,
  private val auditLogDao: AuditLogDao
) {

  private fun generateSlug(title: String): String {
    val clean = title.lowercase(Locale.ROOT)
      .replace(Regex("[^a-z0-9\\s-]"), "")
      .replace(Regex("\\s+"), "-")
      .take(40)
    val suffix = UUID.randomUUID().toString().take(6)
    return "$clean-$suffix"
  }

  suspend fun createRalar(
    ownerId: String,
    type: RalarType,
    title: String,
    description: String,
    startsAt: Long? = null,
    endsAt: Long? = null,
    location: String? = null,
    capacity: Int? = null,
    targetAmountMinor: BigInteger? = null,
    ticketPriceMinor: BigInteger? = null,
    registrationFeeMinor: BigInteger? = null,
    prizePoolMinor: BigInteger? = null,
    assetCode: String = "USDC",
    assetIssuer: String? = null,
    paymentDestination: String
  ): RalarServiceResult<RalarEntity> {
    if (title.isBlank()) {
      return RalarServiceResult.Error("INVALID_TITLE", "Title cannot be empty")
    }
    if (paymentDestination.isBlank()) {
      return RalarServiceResult.Error("INVALID_DESTINATION", "Payment destination address is required")
    }

    val id = UUID.randomUUID().toString()
    val slug = generateSlug(title)

    val entity = RalarEntity(
      id = id,
      ownerId = ownerId,
      type = type,
      title = title.trim(),
      slug = slug,
      description = description.trim(),
      status = RalarStatus.DRAFT,
      startsAt = startsAt,
      endsAt = endsAt,
      location = location?.trim(),
      capacity = capacity,
      seatsTaken = 0,
      targetAmountMinor = targetAmountMinor,
      ticketPriceMinor = ticketPriceMinor,
      registrationFeeMinor = registrationFeeMinor,
      prizePoolMinor = prizePoolMinor,
      assetCode = assetCode,
      assetIssuer = assetIssuer,
      paymentDestination = paymentDestination.trim()
    )

    ralarDao.insert(entity)

    auditLogDao.insert(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        actorUserId = ownerId,
        ralarId = id,
        action = "RALAR_CREATED",
        resourceType = "RALAR",
        resourceId = id,
        metadataJson = "{\"type\":\"$type\",\"title\":\"$title\"}"
      )
    )

    return RalarServiceResult.Success(entity)
  }

  suspend fun publishRalar(id: String, ownerId: String): RalarServiceResult<RalarEntity> {
    val ralar = ralarDao.getRalarById(id)
      ?: return RalarServiceResult.Error("NOT_FOUND", "Ralar not found")

    if (ralar.ownerId != ownerId) {
      return RalarServiceResult.Error("UNAUTHORIZED", "Only the owner can publish this Ralar")
    }

    if (ralar.status != RalarStatus.DRAFT) {
      return RalarServiceResult.Error("INVALID_STATE", "Only DRAFT Ralars can be published")
    }

    val updated = ralar.copy(status = RalarStatus.PUBLISHED, updatedAt = System.currentTimeMillis())
    ralarDao.update(updated)

    auditLogDao.insert(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        actorUserId = ownerId,
        ralarId = id,
        action = "RALAR_PUBLISHED",
        resourceType = "RALAR",
        resourceId = id
      )
    )

    return RalarServiceResult.Success(updated)
  }

  suspend fun closeRalar(id: String, ownerId: String): RalarServiceResult<RalarEntity> {
    val ralar = ralarDao.getRalarById(id)
      ?: return RalarServiceResult.Error("NOT_FOUND", "Ralar not found")

    if (ralar.ownerId != ownerId) {
      return RalarServiceResult.Error("UNAUTHORIZED", "Only the owner can close this Ralar")
    }

    val updated = ralar.copy(status = RalarStatus.CLOSED, updatedAt = System.currentTimeMillis())
    ralarDao.update(updated)

    auditLogDao.insert(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        actorUserId = ownerId,
        ralarId = id,
        action = "RALAR_CLOSED",
        resourceType = "RALAR",
        resourceId = id
      )
    )

    return RalarServiceResult.Success(updated)
  }
}
