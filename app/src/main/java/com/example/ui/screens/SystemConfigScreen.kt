package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.RalarConfig
import com.example.ui.RalarViewModel
import com.example.ui.components.AddressOrHash
import com.example.ui.theme.*

@Composable
fun SystemConfigScreen(
  viewModel: RalarViewModel,
  modifier: Modifier = Modifier
) {
  val configStatus by viewModel.configStatus.collectAsState()
  val scrollState = rememberScrollState()

  var pollarPubKeyInput by remember { mutableStateOf(RalarConfig.pollarPublishableKey) }
  var pollarSecKeyInput by remember { mutableStateOf(RalarConfig.pollarSecretKey) }
  var rpcUrlInput by remember { mutableStateOf(RalarConfig.stellarRpcUrl) }
  var assetCodeInput by remember { mutableStateOf(RalarConfig.assetCode) }
  var assetIssuerInput by remember { mutableStateOf(RalarConfig.assetIssuer) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(RalarPaper)
      .padding(16.dp)
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(
      text = "System Configuration & Readiness",
      style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )

    Text(
      text = "Live status of environment bindings and Stellar Testnet connections. Missing keys indicate configuration requirements.",
      style = Typography.bodySmall.copy(color = RalarMuted)
    )

    // Configuration Checklist Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
      colors = CardDefaults.cardColors(containerColor = RalarSurface),
      shape = RoundedCornerShape(6.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "CREDENTIAL READINESS AUDIT",
          style = Typography.labelSmall.copy(color = RalarMuted, fontWeight = FontWeight.Bold)
        )

        ConfigRow(
          label = "POLLAR_PUBLISHABLE_KEY",
          isConfigured = configStatus.isPollarPublishableKeySet,
          detail = "Client SDK authentication"
        )
        ConfigRow(
          label = "POLLAR_SECRET_KEY",
          isConfigured = configStatus.isPollarSecretKeySet,
          detail = "Server-only deferred wallet funding"
        )
        ConfigRow(
          label = "STELLAR_RPC_URL",
          isConfigured = configStatus.isStellarRpcSet,
          detail = RalarConfig.stellarRpcUrl
        )
        ConfigRow(
          label = "SETTLEMENT ASSET & ISSUER",
          isConfigured = configStatus.isAssetConfigured,
          detail = "${RalarConfig.assetCode} on Stellar Testnet"
        )
      }
    }

    // Editable testnet connection settings
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
      colors = CardDefaults.cardColors(containerColor = RalarSurface),
      shape = RoundedCornerShape(6.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "ENVIRONMENT BINDINGS",
          style = Typography.labelSmall.copy(color = RalarMuted, fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
          value = pollarPubKeyInput,
          onValueChange = { pollarPubKeyInput = it },
          label = { Text("NEXT_PUBLIC_POLLAR_PUBLISHABLE_KEY", style = Typography.labelSmall) },
          placeholder = { Text("pub_testnet_...", style = Typography.bodySmall) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = FinancialSmall,
          shape = RoundedCornerShape(4.dp)
        )

        OutlinedTextField(
          value = pollarSecKeyInput,
          onValueChange = { pollarSecKeyInput = it },
          label = { Text("POLLAR_SECRET_KEY (Backend / Server Only)", style = Typography.labelSmall) },
          placeholder = { Text("sec_testnet_...", style = Typography.bodySmall) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = FinancialSmall,
          shape = RoundedCornerShape(4.dp)
        )

        OutlinedTextField(
          value = rpcUrlInput,
          onValueChange = { rpcUrlInput = it },
          label = { Text("STELLAR_RPC_URL", style = Typography.labelSmall) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = FinancialSmall,
          shape = RoundedCornerShape(4.dp)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = assetCodeInput,
            onValueChange = { assetCodeInput = it },
            label = { Text("Asset Code", style = Typography.labelSmall) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = FinancialBody,
            shape = RoundedCornerShape(4.dp)
          )

          OutlinedTextField(
            value = assetIssuerInput,
            onValueChange = { assetIssuerInput = it },
            label = { Text("Asset Issuer (G...)", style = Typography.labelSmall) },
            singleLine = true,
            modifier = Modifier.weight(2f),
            textStyle = FinancialSmall,
            shape = RoundedCornerShape(4.dp)
          )
        }

        Button(
          onClick = {
            RalarConfig.pollarPublishableKey = pollarPubKeyInput.trim()
            RalarConfig.pollarSecretKey = pollarSecKeyInput.trim()
            RalarConfig.stellarRpcUrl = rpcUrlInput.trim()
            RalarConfig.assetCode = assetCodeInput.trim()
            RalarConfig.assetIssuer = assetIssuerInput.trim()
            viewModel.refreshConfigStatus()
            viewModel.notify("Configuration updated")
          },
          shape = RoundedCornerShape(4.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = RalarInk,
            contentColor = RalarPaper
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("save_config_button")
        ) {
          Text("Save & Apply Configuration", style = Typography.labelMedium)
        }
      }
    }

    // AI Studio Secrets Panel Instructions
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
      colors = CardDefaults.cardColors(containerColor = RalarSurface),
      shape = RoundedCornerShape(6.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "How to Configure Live Keys in AI Studio",
          style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "1. Open the Secrets panel in AI Studio.\n" +
              "2. Set POLLAR_SECRET_KEY with your Pollar backend key (sec_testnet_...).\n" +
              "3. Set NEXT_PUBLIC_POLLAR_PUBLISHABLE_KEY with your publishable key (pub_testnet_...).\n" +
              "4. Ensure your Stellar testnet account has USDC trustline established.\n" +
              "5. Ralar strictly requires no mock or fake payments—all settlements are verified directly against Stellar RPC.",
          style = Typography.bodySmall.copy(color = RalarMuted, lineHeight = 20.sp)
        )
      }
    }
  }
}

@Composable
private fun ConfigRow(label: String, isConfigured: Boolean, detail: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
        contentDescription = null,
        tint = if (isConfigured) RalarAccent else RalarPending,
        modifier = Modifier.size(16.dp)
      )
      Column {
        Text(
          text = label,
          style = Typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = if (isConfigured) RalarInk else RalarPending
          )
        )
        Text(
          text = detail,
          style = Typography.bodySmall.copy(fontSize = 11.sp, color = RalarMuted)
        )
      }
    }

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(3.dp))
        .background(if (isConfigured) RalarAccentSurface else RalarPendingSurface)
        .border(1.dp, if (isConfigured) RalarAccent.copy(alpha = 0.3f) else RalarPending.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
        .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
      Text(
        text = if (isConfigured) "READY" else "PENDING",
        style = Typography.labelSmall.copy(
          fontSize = 10.sp,
          color = if (isConfigured) RalarAccent else RalarPending,
          fontWeight = FontWeight.Bold
        )
      )
    }
  }
}
