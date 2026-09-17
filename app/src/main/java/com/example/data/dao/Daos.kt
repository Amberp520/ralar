package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
  @Query("SELECT * FROM users WHERE id = :id")
  suspend fun getUserById(id: String): UserEntity?

  @Query("SELECT * FROM users WHERE pollarUserId = :pollarUserId")
  suspend fun getUserByPollarId(pollarUserId: String): UserEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(user: UserEntity)
}

@Dao
interface RalarDao {
  @Query("SELECT * FROM ralars ORDER BY createdAt DESC")
  fun getAllRalarsFlow(): Flow<List<RalarEntity>>

  @Query("SELECT * FROM ralars WHERE ownerId = :ownerId ORDER BY createdAt DESC")
  fun getRalarsByOwnerFlow(ownerId: String): Flow<List<RalarEntity>>

  @Query("SELECT * FROM ralars WHERE id = :id")
  suspend fun getRalarById(id: String): RalarEntity?

  @Query("SELECT * FROM ralars WHERE id = :id")
  fun getRalarByIdFlow(id: String): Flow<RalarEntity?>

  @Query("SELECT * FROM ralars WHERE slug = :slug")
  suspend fun getRalarBySlug(slug: String): RalarEntity?

  @Query("SELECT * FROM ralars WHERE slug = :slug")
  fun getRalarBySlugFlow(slug: String): Flow<RalarEntity?>

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(ralar: RalarEntity)

  @Update
  suspend fun update(ralar: RalarEntity)

  // Atomic capacity increment (Section 16)
  // Returns number of affected rows (0 = capacity exhausted / sold out)
  @Query("UPDATE ralars SET seatsTaken = seatsTaken + 1 WHERE id = :id AND (capacity IS NULL OR seatsTaken < capacity)")
  suspend fun atomicIncrementSeatsTaken(id: String): Int

  @Query("UPDATE ralars SET seatsTaken = CASE WHEN seatsTaken > 0 THEN seatsTaken - 1 ELSE 0 END WHERE id = :id")
  suspend fun atomicDecrementSeatsTaken(id: String): Int
}

@Dao
interface ParticipationDao {
  @Query("SELECT * FROM participations WHERE ralarId = :ralarId")
  fun getParticipationsForRalar(ralarId: String): Flow<List<ParticipationEntity>>

  @Query("SELECT * FROM participations WHERE ralarId = :ralarId AND userId = :userId")
  suspend fun getParticipation(ralarId: String, userId: String): ParticipationEntity?

  @Query("SELECT * FROM participations WHERE ralarId = :ralarId AND isWinner = 1")
  suspend fun getWinnerForRalar(ralarId: String): ParticipationEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(participation: ParticipationEntity)

  @Update
  suspend fun update(participation: ParticipationEntity)

  @Query("UPDATE participations SET isWinner = :isWinner WHERE ralarId = :ralarId AND userId = :userId")
  suspend fun setWinner(ralarId: String, userId: String, isWinner: Boolean)
}

@Dao
interface PaymentDao {
  @Query("SELECT * FROM payments WHERE ralarId = :ralarId ORDER BY createdAt DESC")
  fun getPaymentsForRalar(ralarId: String): Flow<List<PaymentEntity>>

  @Query("SELECT * FROM payments ORDER BY createdAt DESC")
  fun getAllPayments(): Flow<List<PaymentEntity>>

  @Query("SELECT * FROM payments WHERE id = :id")
  suspend fun getPaymentById(id: String): PaymentEntity?

  @Query("SELECT * FROM payments WHERE memoRef = :memoRef")
  suspend fun getPaymentByMemo(memoRef: String): PaymentEntity?

  @Query("SELECT * FROM payments WHERE ralarId = :ralarId AND idempotencyKey = :idempotencyKey")
  suspend fun getPaymentByIdempotencyKey(ralarId: String, idempotencyKey: String): PaymentEntity?

  @Query("SELECT * FROM payments WHERE status = 'PROCESSING'")
  suspend fun getProcessingPayments(): List<PaymentEntity>

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(payment: PaymentEntity)

  @Update
  suspend fun update(payment: PaymentEntity)
}

@Dao
interface PayoutDao {
  @Query("SELECT * FROM payouts WHERE ralarId = :ralarId ORDER BY createdAt DESC")
  fun getPayoutsForRalar(ralarId: String): Flow<List<PayoutEntity>>

  @Query("SELECT * FROM payouts ORDER BY createdAt DESC")
  fun getAllPayouts(): Flow<List<PayoutEntity>>

  @Query("SELECT * FROM payouts WHERE id = :id")
  suspend fun getPayoutById(id: String): PayoutEntity?

  @Query("SELECT * FROM payouts WHERE memoRef = :memoRef")
  suspend fun getPayoutByMemo(memoRef: String): PayoutEntity?

  @Query("SELECT * FROM payouts WHERE ralarId = :ralarId AND prizeKey = :prizeKey")
  suspend fun getPayoutByPrizeKey(ralarId: String, prizeKey: String): PayoutEntity?

  @Query("SELECT * FROM payouts WHERE status = 'PROCESSING'")
  suspend fun getProcessingPayouts(): List<PayoutEntity>

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(payout: PayoutEntity)

  @Update
  suspend fun update(payout: PayoutEntity)
}

@Dao
interface AuditLogDao {
  @Query("SELECT * FROM audit_logs WHERE ralarId = :ralarId ORDER BY createdAt DESC")
  fun getLogsForRalar(ralarId: String): Flow<List<AuditLogEntity>>

  @Insert
  suspend fun insert(log: AuditLogEntity)
}
