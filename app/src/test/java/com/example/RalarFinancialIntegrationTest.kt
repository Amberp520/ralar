package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.money.Money
import com.example.data.db.RalarDatabase
import com.example.data.model.*
import com.example.service.payment.PaymentResult
import com.example.service.payment.PaymentService
import com.example.service.payout.PayoutResult
import com.example.service.payout.PayoutService
import com.example.service.ralar.RalarService
import com.example.service.ralar.RalarServiceResult
import com.example.service.settlement.SettlementService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigInteger
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RalarFinancialIntegrationTest {
  private lateinit var db: RalarDatabase
  private lateinit var ralarService: RalarService
  private lateinit var paymentService: PaymentService
  private lateinit var payoutService: PayoutService

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, RalarDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val settlementService = SettlementService()
    ralarService = RalarService(db.ralarDao(), db.auditLogDao())
    paymentService = PaymentService(
      db.ralarDao(),
      db.participationDao(),
      db.paymentDao(),
      db.auditLogDao(),
      settlementService
    )
    payoutService = PayoutService(
      db.ralarDao(),
      db.participationDao(),
      db.userDao(),
      db.payoutDao(),
      db.auditLogDao(),
      settlementService
    )
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun testCreateAndPublishEvent() = runBlocking {
    val ownerId = "usr_test_owner"
    val result = ralarService.createRalar(
      ownerId = ownerId,
      type = RalarType.EVENT,
      title = "Stellar DevCon 2026",
      description = "Annual developer conference",
      capacity = 2,
      ticketPriceMinor = Money.toMinor("25.00"),
      paymentDestination = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    )

    assertTrue(result is RalarServiceResult.Success)
    val ralar = (result as RalarServiceResult.Success).data
    assertEquals(RalarStatus.DRAFT, ralar.status)

    // Publish
    val pubResult = ralarService.publishRalar(ralar.id, ownerId)
    assertTrue(pubResult is RalarServiceResult.Success)
    val published = (pubResult as RalarServiceResult.Success).data
    assertEquals(RalarStatus.PUBLISHED, published.status)
  }

  @Test
  fun testCapacityEnforcementPreventsOverbooking() = runBlocking {
    val ownerId = "usr_test_owner"
    val ralarRes = ralarService.createRalar(
      ownerId = ownerId,
      type = RalarType.EVENT,
      title = "Exclusive Workshop",
      description = "Cap 2 seats",
      capacity = 2,
      ticketPriceMinor = Money.toMinor("10.00"),
      paymentDestination = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    )
    val ralar = (ralarRes as RalarServiceResult.Success).data
    ralarService.publishRalar(ralar.id, ownerId)

    // Authorize Seat 1
    val pay1 = paymentService.authorizePayment(
      payerUserId = "user_1",
      ralarId = ralar.id,
      idempotencyKey = "key_1"
    )
    assertTrue(pay1 is PaymentResult.Authorized)

    // Authorize Seat 2
    val pay2 = paymentService.authorizePayment(
      payerUserId = "user_2",
      ralarId = ralar.id,
      idempotencyKey = "key_2"
    )
    assertTrue(pay2 is PaymentResult.Authorized)

    // Attempt Seat 3 (Must fail with SOLD_OUT)
    val pay3 = paymentService.authorizePayment(
      payerUserId = "user_3",
      ralarId = ralar.id,
      idempotencyKey = "key_3"
    )
    assertTrue(pay3 is PaymentResult.Error)
    assertEquals("SOLD_OUT", (pay3 as PaymentResult.Error).code)
  }

  @Test
  fun testPayoutAuthorizationRequiresHackathonAndWinner() = runBlocking {
    val organizerId = "usr_org"
    val winnerId = "usr_winner"

    // Create winner user profile with wallet
    db.userDao().insertOrUpdate(
      UserEntity(
        id = winnerId,
        pollarUserId = "pol_winner",
        displayName = "Hackathon Winner",
        email = "winner@ralar.xyz",
        walletAddress = "GDGTPGHKFLG2M7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      )
    )

    // Create Hackathon
    val ralarRes = ralarService.createRalar(
      ownerId = organizerId,
      type = RalarType.HACKATHON,
      title = "Soroban Hackathon",
      description = "Build on Stellar",
      prizePoolMinor = Money.toMinor("500.00"),
      paymentDestination = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    )
    val ralar = (ralarRes as RalarServiceResult.Success).data

    // Register participant
    db.participationDao().insert(
      ParticipationEntity(
        id = UUID.randomUUID().toString(),
        ralarId = ralar.id,
        userId = winnerId,
        status = ParticipationStatus.CONFIRMED,
        amountDueMinor = BigInteger.ZERO
      )
    )

    // Cannot authorize on DRAFT
    val draftPayout = payoutService.authorizePayout(
      organizerUserId = organizerId,
      ralarId = ralar.id,
      winnerUserId = winnerId,
      idempotencyKey = "payout_key_1"
    )
    assertTrue(draftPayout is PayoutResult.Error)
    assertEquals("INVALID_STATUS", (draftPayout as PayoutResult.Error).code)

    // Publish hackathon
    ralarService.publishRalar(ralar.id, organizerId)

    // Authorize payout succeeds
    val authPayout = payoutService.authorizePayout(
      organizerUserId = organizerId,
      ralarId = ralar.id,
      winnerUserId = winnerId,
      idempotencyKey = "payout_key_1"
    )
    assertTrue(authPayout is PayoutResult.Authorized)
    val auth = (authPayout as PayoutResult.Authorized).authorization
    assertEquals("500.0000000", auth.amountStellar)
    assertTrue(auth.memoRef.startsWith("RLR-"))
  }
}
