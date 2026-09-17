package com.example.core.config

data class ConfigStatus(
  val isPollarPublishableKeySet: Boolean,
  val isPollarSecretKeySet: Boolean,
  val isStellarRpcSet: Boolean,
  val isAssetConfigured: Boolean,
  val isReconcileSecretSet: Boolean
) {
  val isReadyForLiveSettlement: Boolean
    get() = isPollarPublishableKeySet && isStellarRpcSet && isAssetConfigured
}

object RalarConfig {
  // Testnet defaults and environment bindings
  const val DEFAULT_STELLAR_RPC = "https://soroban-testnet.stellar.org"
  const val DEFAULT_STELLAR_HORIZON = "https://horizon-testnet.stellar.org"
  const val DEFAULT_EXPLORER_BASE = "https://stellar.expert/explorer/testnet/tx"
  const val DEFAULT_ASSET_CODE = "USDC"
  const val DEFAULT_ASSET_ISSUER = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"

  var pollarPublishableKey: String = ""
  var pollarSecretKey: String = ""
  var stellarRpcUrl: String = DEFAULT_STELLAR_RPC
  var stellarHorizonUrl: String = DEFAULT_STELLAR_HORIZON
  var stellarExplorerBase: String = DEFAULT_EXPLORER_BASE
  var assetCode: String = DEFAULT_ASSET_CODE
  var assetIssuer: String = DEFAULT_ASSET_ISSUER
  var reconcileSecret: String = ""

  fun getStatus(): ConfigStatus {
    return ConfigStatus(
      isPollarPublishableKeySet = pollarPublishableKey.isNotBlank() && !pollarPublishableKey.contains("..."),
      isPollarSecretKeySet = pollarSecretKey.isNotBlank() && !pollarSecretKey.contains("..."),
      isStellarRpcSet = stellarRpcUrl.isNotBlank(),
      isAssetConfigured = assetCode.isNotBlank() && assetIssuer.isNotBlank(),
      isReconcileSecretSet = reconcileSecret.isNotBlank()
    )
  }

  fun getExplorerUrl(txHash: String): String {
    val cleanBase = stellarExplorerBase.trimEnd('/')
    return "$cleanBase/$txHash"
  }
}
