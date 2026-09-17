package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.ConfigStatus
import com.example.core.config.RalarConfig
import com.example.core.money.Money
import com.example.data.db.RalarDatabase
import com.example.data.model.*
import com.example.service.payment.PaymentResult
import com.example.service.payment.PaymentService
import com.example.service.payout.PayoutResult
import com.example.service.payout.PayoutService
import com.example.service.pollar.PollarClient
import com.example.service.pollar.PollarFundResult
import com.example.service.pollar.PollarWallet
import com.example.service.ralar.RalarService
import com.example.service.ralar.RalarServiceResult
import com.example.service.reconciliation.ReconciliationReport
import com.example.service.reconciliation.ReconciliationService
import com.example.service.settlement.SettlementService
import com.example.service.settlement.VerificationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.UUID

sealed class AppScreen {
  object Dashboard : AppScreen()
  object CreateRalar : AppScreen()
  data class PublicRalar(val slug: String) : AppScreen()
  object VerificationInspector : AppScreen()
  object SystemConfig : AppScreen()
  object Overview : AppScreen()
}

data class UiNotification(
  val id: String = UUID.randomUUID().toString(),
  val message: String,
  val isError: Boolean = false
)

class RalarViewModel(application: Application) : AndroidViewModel(application) {
  private val db = RalarDatabase.getDatabase(application)
  private val ralarDao = db.ralarDao()
  private val userDao = db.userDao()
  private val participationDao = db.participationDao()
  private val paymentDao = db.paymentDao()
  private val payoutDao = db.payoutDao()
  private val auditLogDao = db.auditLogDao()

  private val settlementService = SettlementService()
  private val pollarClient = PollarClient()
  val paymentService = PaymentService(ralarDao, participationDao, paymentDao, auditLogDao, settlementService)
  val payoutService = PayoutService(ralarDao, participationDao, userDao, payoutDao, auditLogDao, settlementService)
  val ralarService = RalarService(ralarDao, auditLogDao)
  val reconciliationService = ReconciliationService(paymentDao, payoutDao, ralarDao, participationDao, auditLogDao, settlementService)

  // Current Screen
  private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  // Active Pollar User Wallet
  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

  private val _activeWallet = MutableStateFlow<PollarWallet?>(null)
  val activeWallet: StateFlow<PollarWallet?> = _activeWallet.asStateFlow()

  // Live Database Flows
  val ralars: StateFlow<List<RalarEntity>> = ralarDao.getAllRalarsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allPayments: StateFlow<List<PaymentEntity>> = paymentDao.getAllPayments()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allPayouts: StateFlow<List<PayoutEntity>> = payoutDao.getAllPayouts()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Selected Ralar for public receipt view
  private val _selectedRalar = MutableStateFlow<RalarEntity?>(null)
  val selectedRalar: StateFlow<RalarEntity?> = _selectedRalar.asStateFlow()

  private val _selectedRalarPayments = MutableStateFlow<List<PaymentEntity>>(emptyList())
  val selectedRalarPayments: StateFlow<List<PaymentEntity>> = _selectedRalarPayments.asStateFlow()

  // Verification Inspector state
  private val _inspectionResult = MutableStateFlow<VerificationResult?>(null)
  val inspectionResult: StateFlow<VerificationResult?> = _inspectionResult.asStateFlow()

  private val _isInspecting = MutableStateFlow(false)
  val isInspecting: StateFlow<Boolean> = _isInspecting.asStateFlow()

  // Reconciliation report
  private val _lastReconciliation = MutableStateFlow<ReconciliationReport?>(null)
  val lastReconciliation: StateFlow<ReconciliationReport?> = _lastReconciliation.asStateFlow()

  private val _isReconciling = MutableStateFlow(false)
  val isReconciling: StateFlow<Boolean> = _isReconciling.asStateFlow()

  // Notifications
  private val _notification = MutableStateFlow<UiNotification?>(null)
  val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

  // Config Status
  private val _configStatus = MutableStateFlow(RalarConfig.getStatus())
  val configStatus: StateFlow<ConfigStatus> = _configStatus.asStateFlow()

  init {
    initializeSession()
  }

  private fun initializeSession() {
    viewModelScope.launch {
      // Initialize organizer / default user profile
      val organizerId = "usr_organizer_001"
      var user = userDao.getUserById(organizerId)
      if (user == null) {
        user = UserEntity(
          id = organizerId,
          pollarUserId = "pol_user_ralar_organizer",
          displayName = "Ralar Organizer",
          email = "organizer@ralar.xyz",
          walletAddress = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        )
        userDao.insertOrUpdate(user)
      }
      _currentUser.value = user
      _activeWallet.value = PollarWallet(
        address = user.walletAddress ?: "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
        custody = "smart",
        provider = "pollar",
        existsOnStellar = true
      )
    }
  }

  fun navigateTo(screen: AppScreen) {
    _currentScreen.value = screen
    if (screen is AppScreen.PublicRalar) {
      loadRalarBySlug(screen.slug)
    }
  }

  fun dismissNotification() {
    _notification.value = null
  }

  fun notify(message: String, isError: Boolean = false) {
    _notification.value = UiNotification(message = message, isError = isError)
  }

  fun loadRalarBySlug(slug: String) {
    viewModelScope.launch {
      val ralar = ralarDao.getRalarBySlug(slug)
      _selectedRalar.value = ralar
      if (ralar != null) {
        paymentDao.getPaymentsForRalar(ralar.id).collect {
          _selectedRalarPayments.value = it
        }
      }
    }
  }

  fun createRalar(
    type: RalarType,
    title: String,
    description: String,
    startsAt: Long?,
    endsAt: Long?,
    location: String?,
    capacity: Int?,
    targetAmountHuman: String?,
    ticketPriceHuman: String?,
    registrationFeeHuman: String?,
    prizePoolHuman: String?,
    paymentDestination: String
  ) {
    viewModelScope.launch {
      val ownerId = _currentUser.value?.id ?: "usr_organizer_001"
      val targetAmountMinor = targetAmountHuman?.takeIf { it.isNotBlank() }?.let { Money.toMinor(it) }
      val ticketPriceMinor = ticketPriceHuman?.takeIf { it.isNotBlank() }?.let { Money.toMinor(it) }
      val regFeeMinor = registrationFeeHuman?.takeIf { it.isNotBlank() }?.let { Money.toMinor(it) }
      val prizePoolMinor = prizePoolHuman?.takeIf { it.isNotBlank() }?.let { Money.toMinor(it) }

      val result = ralarService.createRalar(
        ownerId = ownerId,
        type = type,
        title = title,
        description = description,
        startsAt = startsAt,
        endsAt = endsAt,
        location = location,
        capacity = capacity,
        targetAmountMinor = targetAmountMinor,
        ticketPriceMinor = ticketPriceMinor,
        registrationFeeMinor = regFeeMinor,
        prizePoolMinor = prizePoolMinor,
        assetCode = RalarConfig.assetCode,
        assetIssuer = RalarConfig.assetIssuer,
        paymentDestination = paymentDestination
      )

      when (result) {
        is RalarServiceResult.Success -> {
          notify("Ralar created: ${result.data.title}")
          _currentScreen.value = AppScreen.Dashboard
        }
        is RalarServiceResult.Error -> {
          notify(result.message, isError = true)
        }
      }
    }
  }

  fun publishRalar(ralarId: String) {
    viewModelScope.launch {
      val ownerId = _currentUser.value?.id ?: "usr_organizer_001"
      val res = ralarService.publishRalar(ralarId, ownerId)
      when (res) {
        is RalarServiceResult.Success -> notify("Ralar published successfully")
        is RalarServiceResult.Error -> notify(res.message, isError = true)
      }
    }
  }

  fun closeRalar(ralarId: String) {
    viewModelScope.launch {
      val ownerId = _currentUser.value?.id ?: "usr_organizer_001"
      val res = ralarService.closeRalar(ralarId, ownerId)
      when (res) {
        is RalarServiceResult.Success -> notify("Ralar closed")
        is RalarServiceResult.Error -> notify(res.message, isError = true)
      }
    }
  }

  /**
   * Complete payment cycle:
   * 1. Server authorizes payment based on database values
   * 2. Client executes transaction signature
   * 3. Server receives hash and verifies against Stellar testnet
   */
  fun executePayment(
    ralarId: String,
    causeDonationHumanAmount: String? = null,
    onSuccess: (PaymentEntity) -> Unit = {}
  ) {
    viewModelScope.launch {
      val userId = _currentUser.value?.id ?: "usr_participant_001"
      val idempotencyKey = UUID.randomUUID().toString()

      // 1. Server authorization
      val authResult = paymentService.authorizePayment(
        payerUserId = userId,
        ralarId = ralarId,
        idempotencyKey = idempotencyKey,
        causeDonationHumanAmount = causeDonationHumanAmount
      )

      when (authResult) {
        is PaymentResult.Authorized -> {
          val auth = authResult.authorization
          notify("Payment authorized: ${auth.amountStellar} ${auth.assetCode} (Memo: ${auth.memoRef})")

          // In live testnet, the Pollar client would execute runTx('payment', ...)
          // For verification with Stellar testnet, submit the transaction
        }
        is PaymentResult.Paid -> {
          notify("Payment already confirmed on-chain")
          onSuccess(authResult.payment)
        }
        is PaymentResult.Processing -> {
          notify("Payment is currently confirming on Stellar")
        }
        is PaymentResult.Error -> {
          notify(authResult.message, isError = true)
        }
      }
    }
  }

  /**
   * Re-check / submit transaction hash for a payment
   */
  fun submitAndVerifyPayment(paymentId: String, txHash: String) {
    viewModelScope.launch {
      val result = paymentService.submitPaymentTransaction(paymentId, txHash)
      when (result) {
        is PaymentResult.Paid -> {
          notify("Payment verified on Stellar testnet! Status: PAID")
        }
        is PaymentResult.Processing -> {
          notify(result.message)
        }
        is PaymentResult.Error -> {
          notify(result.message, isError = true)
        }
        else -> {}
      }
    }
  }

  /**
   * Authorize and submit Prize Payout for Hackathon
   */
  fun executePayout(ralarId: String, winnerUserId: String) {
    viewModelScope.launch {
      val organizerId = _currentUser.value?.id ?: "usr_organizer_001"
      val idempotencyKey = UUID.randomUUID().toString()

      val authRes = payoutService.authorizePayout(
        organizerUserId = organizerId,
        ralarId = ralarId,
        winnerUserId = winnerUserId,
        idempotencyKey = idempotencyKey
      )

      when (authRes) {
        is PayoutResult.Authorized -> {
          val auth = authRes.authorization
          notify("Payout authorized: ${auth.amountStellar} ${auth.assetCode} to ${auth.destination.take(8)}...")
        }
        is PayoutResult.Paid -> {
          notify("Payout already verified on Stellar testnet")
        }
        is PayoutResult.Processing -> {
          notify("Payout is confirming on Stellar")
        }
        is PayoutResult.Error -> {
          notify(authRes.message, isError = true)
        }
      }
    }
  }

  fun submitAndVerifyPayout(payoutId: String, txHash: String) {
    viewModelScope.launch {
      val result = payoutService.submitPayoutTransaction(payoutId, txHash)
      when (result) {
        is PayoutResult.Paid -> {
          notify("Payout verified on Stellar testnet! Status: PAID")
        }
        is PayoutResult.Processing -> {
          notify(result.message)
        }
        is PayoutResult.Error -> {
          notify(result.message, isError = true)
        }
        else -> {}
      }
    }
  }

  /**
   * Independent verification inspector tool
   */
  fun inspectTransaction(
    txHash: String,
    expectedDestination: String,
    expectedAmountHuman: String,
    expectedAssetCode: String,
    expectedAssetIssuer: String?,
    expectedMemoRef: String
  ) {
    viewModelScope.launch {
      _isInspecting.value = true
      try {
        val amountMinor = Money.toMinor(expectedAmountHuman)
        val result = settlementService.verifyOnChain(
          txHash = txHash,
          expectedDestination = expectedDestination,
          expectedAmountMinor = amountMinor,
          expectedAssetCode = expectedAssetCode,
          expectedAssetIssuer = expectedAssetIssuer,
          expectedMemoRef = expectedMemoRef
        )
        _inspectionResult.value = result
      } catch (e: Exception) {
        _inspectionResult.value = VerificationResult.Failure(
          VerificationResult.FailureReason.MISMATCH,
          e.message ?: "Invalid inspection inputs"
        )
      } finally {
        _isInspecting.value = false
      }
    }
  }

  /**
   * Run manual or automated reconciliation
   */
  fun runReconciliation() {
    viewModelScope.launch {
      _isReconciling.value = true
      try {
        val token = if (RalarConfig.reconcileSecret.isNotBlank()) "Bearer ${RalarConfig.reconcileSecret}" else null
        val report = reconciliationService.reconcile(token)
        _lastReconciliation.value = report
        notify("Reconciliation complete: ${report.paymentsVerified} payments, ${report.payoutsVerified} payouts verified")
      } catch (e: Exception) {
        notify("Reconciliation error: ${e.message}", isError = true)
      } finally {
        _isReconciling.value = false
      }
    }
  }

  /**
   * Request deferred wallet funding via Pollar API
   */
  fun requestWalletFunding() {
    viewModelScope.launch {
      val wallet = _activeWallet.value
      if (wallet == null) {
        notify("No active wallet connected", isError = true)
        return@launch
      }
      val res = pollarClient.fundWallet(wallet.address)
      when (res) {
        is PollarFundResult.Success -> notify(res.message)
        is PollarFundResult.AlreadyFunded -> notify(res.message)
        is PollarFundResult.Error -> notify("Funding error: ${res.message}", isError = true)
        is PollarFundResult.ConfigurationRequired -> notify(res.message, isError = true)
      }
    }
  }

  fun updateWalletAddress(newAddress: String) {
    viewModelScope.launch {
      val current = _currentUser.value ?: return@launch
      val updated = current.copy(walletAddress = newAddress.trim())
      userDao.insertOrUpdate(updated)
      _currentUser.value = updated
      _activeWallet.value = _activeWallet.value?.copy(address = newAddress.trim())
      notify("Wallet updated: ${newAddress.take(8)}...")
    }
  }

  fun refreshConfigStatus() {
    _configStatus.value = RalarConfig.getStatus()
  }
}
