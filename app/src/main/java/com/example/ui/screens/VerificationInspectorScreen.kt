package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
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
import com.example.service.settlement.VerificationResult
import com.example.ui.RalarViewModel
import com.example.ui.components.AddressOrHash
import com.example.ui.theme.*

@Composable
fun VerificationInspectorScreen(
  viewModel: RalarViewModel,
  modifier: Modifier = Modifier
) {
  var txHashInput by remember { mutableStateOf("") }
  var expectedDestInput by remember { mutableStateOf(RalarConfig.DEFAULT_ASSET_ISSUER) }
  var expectedAmountInput by remember { mutableStateOf("25.00") }
  var expectedAssetCode by remember { mutableStateOf(RalarConfig.DEFAULT_ASSET_CODE) }
  var expectedIssuer by remember { mutableStateOf(RalarConfig.DEFAULT_ASSET_ISSUER) }
  var expectedMemoInput by remember { mutableStateOf("RLR-") }

  val inspectionResult by viewModel.inspectionResult.collectAsState()
  val isInspecting by viewModel.isInspecting.collectAsState()
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(RalarPaper)
      .padding(16.dp)
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(
      text = "Stellar On-Chain Verification Inspector",
      style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )

    Text(
      text = "Independent audit engine verifying Stellar Testnet transactions against authoritative server expectations (Checks 1-9 & Fee-Bump traversal).",
      style = Typography.bodySmall.copy(color = RalarMuted)
    )

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
        OutlinedTextField(
          value = txHashInput,
          onValueChange = { txHashInput = it.trim() },
          label = { Text("Transaction Hash (64 hex characters)", style = Typography.labelSmall) },
          placeholder = { Text("e.g. 5f83b2...a4", style = Typography.bodySmall) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inspector_tx_hash_input"),
          textStyle = FinancialSmall,
          shape = RoundedCornerShape(4.dp)
        )

        OutlinedTextField(
          value = expectedDestInput,
          onValueChange = { expectedDestInput = it.trim() },
          label = { Text("Expected Destination (Stellar Address)", style = Typography.labelSmall) },
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
            value = expectedAmountInput,
            onValueChange = { expectedAmountInput = it.trim() },
            label = { Text("Expected Amount", style = Typography.labelSmall) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = FinancialBody,
            shape = RoundedCornerShape(4.dp)
          )

          OutlinedTextField(
            value = expectedAssetCode,
            onValueChange = { expectedAssetCode = it.trim() },
            label = { Text("Asset Code", style = Typography.labelSmall) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = FinancialBody,
            shape = RoundedCornerShape(4.dp)
          )
        }

        OutlinedTextField(
          value = expectedMemoInput,
          onValueChange = { expectedMemoInput = it.trim() },
          label = { Text("Expected Memo (RLR-XXXXXXXX)", style = Typography.labelSmall) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = FinancialBody,
          shape = RoundedCornerShape(4.dp)
        )

        Button(
          onClick = {
            viewModel.inspectTransaction(
              txHash = txHashInput,
              expectedDestination = expectedDestInput,
              expectedAmountHuman = expectedAmountInput,
              expectedAssetCode = expectedAssetCode,
              expectedAssetIssuer = expectedIssuer.takeIf { it.isNotBlank() },
              expectedMemoRef = expectedMemoInput
            )
          },
          enabled = !isInspecting && txHashInput.isNotBlank(),
          shape = RoundedCornerShape(4.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = RalarInk,
            contentColor = RalarPaper
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("inspect_verify_button")
        ) {
          if (isInspecting) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = RalarPaper,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text("Run Independent Verification", style = Typography.labelMedium)
        }
      }
    }

    // Results Display
    inspectionResult?.let { res ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(
            1.dp,
            if (res is VerificationResult.Success) RalarAccent else RalarFail,
            RoundedCornerShape(6.dp)
          ),
        colors = CardDefaults.cardColors(
          containerColor = if (res is VerificationResult.Success) RalarAccentSurface else RalarFailSurface
        ),
        shape = RoundedCornerShape(6.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          when (res) {
            is VerificationResult.Success -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Success",
                  tint = RalarAccent
                )
                Text(
                  text = "VERIFIED ON STELLAR TESTNET",
                  style = Typography.titleMedium.copy(color = RalarAccent, fontWeight = FontWeight.Bold)
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "All 9 security verification checks succeeded.\nInner envelope, destination, exact 7-decimal minor units, and cryptographic memo match.",
                style = Typography.bodySmall.copy(color = RalarInk)
              )
              Spacer(modifier = Modifier.height(8.dp))
              AddressOrHash(value = res.hash, label = "Tx Hash", isTxHash = true)
            }
            is VerificationResult.Failure -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Error,
                  contentDescription = "Failure",
                  tint = RalarFail
                )
                Text(
                  text = "VERIFICATION REJECTED: ${res.reason.name}",
                  style = Typography.titleMedium.copy(color = RalarFail, fontWeight = FontWeight.Bold)
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = res.detail,
                style = Typography.bodySmall.copy(color = RalarFail)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Strict Rule: A mismatch or unconfirmed transaction is NEVER marked PAID.",
                style = Typography.labelSmall.copy(color = RalarMuted)
              )
            }
          }
        }
      }
    }

    // Protocol Verification Matrix
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
      colors = CardDefaults.cardColors(containerColor = RalarSurface),
      shape = RoundedCornerShape(6.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "Required Stellar Settlement Checks",
          style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val checks = listOf(
          "1. Transaction exists on Stellar Testnet",
          "2. Transaction status is successful (successful == true)",
          "3. Payment operation detected inside envelope",
          "4. Payment destination matches authoritative destination",
          "5. Asset code matches configured asset (e.g. USDC)",
          "6. Asset issuer matches configured issuer account",
          "7. Amount matches authoritative 7-decimal minor units (10^7)",
          "8. Cryptographic memo is present",
          "9. Memo matches server-generated memoRef (RLR-XXXXXXXX)",
          "• Fee-Bump envelope unwrapping & inner transaction resolution"
        )
        checks.forEach { check ->
          Text(
            text = check,
            style = Typography.bodySmall.copy(fontSize = 12.sp, color = RalarMuted),
            modifier = Modifier.padding(vertical = 2.dp)
          )
        }
      }
    }
  }
}
