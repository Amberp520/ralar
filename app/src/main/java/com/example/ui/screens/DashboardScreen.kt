package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.money.Money
import com.example.data.model.*
import com.example.ui.AppScreen
import com.example.ui.RalarViewModel
import com.example.ui.components.AddressOrHash
import com.example.ui.components.StatusChip
import com.example.ui.components.TypeBadge
import com.example.ui.theme.*
import java.math.BigInteger

@Composable
fun DashboardScreen(
  viewModel: RalarViewModel,
  modifier: Modifier = Modifier
) {
  val ralars by viewModel.ralars.collectAsState()
  val payments by viewModel.allPayments.collectAsState()
  val payouts by viewModel.allPayouts.collectAsState()
  val isReconciling by viewModel.isReconciling.collectAsState()

  // STRICT FINANCIAL AUTHORITY: Only calculate confirmed money from PAID payments
  val confirmedPaidMinor = remember(payments) {
    payments.filter { it.status == PaymentStatus.PAID }
      .fold(BigInteger.ZERO) { acc, p -> acc.add(p.amountMinor) }
  }

  val processingCount = remember(payments) {
    payments.count { it.status == PaymentStatus.PROCESSING }
  }

  var showVerifyDialogForPayment by remember { mutableStateOf<PaymentEntity?>(null) }
  var manualTxHashInput by remember { mutableStateOf("") }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(RalarPaper)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Financial Control Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Financial Control Surface",
            style = Typography.titleLarge.copy(color = RalarInk)
          )
          Text(
            text = "Authoritative on-chain ledger",
            style = Typography.bodySmall.copy(color = RalarMuted)
          )
        }

        Button(
          onClick = { viewModel.runReconciliation() },
          enabled = !isReconciling,
          shape = RoundedCornerShape(4.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = RalarInk,
            contentColor = RalarPaper
          ),
          modifier = Modifier.testTag("reconcile_button")
        ) {
          if (isReconciling) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = RalarPaper,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
          } else {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text("Reconcile", style = Typography.labelMedium)
        }
      }
    }

    // Metric Summary Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Confirmed Inflows
        Card(
          modifier = Modifier
            .weight(1.2f)
            .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
          colors = CardDefaults.cardColors(containerColor = RalarSurface),
          shape = RoundedCornerShape(6.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "CONFIRMED INFLOWS",
              style = Typography.labelSmall.copy(color = RalarMuted)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = Money.formatDisplay(confirmedPaidMinor, "USDC"),
              style = FinancialMedium.copy(color = RalarAccent)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Stellar verified (PAID only)",
              style = Typography.bodySmall.copy(fontSize = 11.sp, color = RalarMuted)
            )
          }
        }

        // Active Ralars
        Card(
          modifier = Modifier
            .weight(0.8f)
            .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
          colors = CardDefaults.cardColors(containerColor = RalarSurface),
          shape = RoundedCornerShape(6.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "RALARS",
              style = Typography.labelSmall.copy(color = RalarMuted)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = ralars.size.toString(),
              style = FinancialMedium.copy(color = RalarInk)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${ralars.count { it.status == RalarStatus.PUBLISHED }} Published",
              style = Typography.bodySmall.copy(fontSize = 11.sp, color = RalarMuted)
            )
          }
        }

        // Pending Processing
        Card(
          modifier = Modifier
            .weight(0.8f)
            .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
          colors = CardDefaults.cardColors(containerColor = RalarSurface),
          shape = RoundedCornerShape(6.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "PROCESSING",
              style = Typography.labelSmall.copy(color = RalarMuted)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = processingCount.toString(),
              style = FinancialMedium.copy(
                color = if (processingCount > 0) RalarPending else RalarInk
              )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Awaiting block",
              style = Typography.bodySmall.copy(fontSize = 11.sp, color = RalarMuted)
            )
          }
        }
      }
    }

    // Ralars Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Managed Ralars",
          style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        TextButton(
          onClick = { viewModel.navigateTo(AppScreen.CreateRalar) }
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Ralar", style = Typography.labelMedium)
        }
      }
    }

    if (ralars.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RalarLine, RoundedCornerShape(6.dp))
            .background(RalarSurface)
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No Ralars created yet", style = Typography.bodyMedium.copy(color = RalarMuted))
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { viewModel.navigateTo(AppScreen.CreateRalar) },
              shape = RoundedCornerShape(4.dp),
              colors = ButtonDefaults.buttonColors(containerColor = RalarInk)
            ) {
              Text("Create Event, Hackathon, or Cause", style = Typography.labelMedium)
            }
          }
        }
      }
    } else {
      items(ralars) { ralar ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
          colors = CardDefaults.cardColors(containerColor = RalarSurface),
          shape = RoundedCornerShape(6.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                TypeBadge(ralar.type)
                Text(
                  text = ralar.title,
                  style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
              }
              StatusChip(ralar.status.name)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = ralar.description.take(120),
              style = Typography.bodySmall.copy(color = RalarMuted)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Financial metric
              when (ralar.type) {
                RalarType.EVENT -> {
                  Text(
                    text = "Ticket: ${Money.formatDisplay(ralar.ticketPriceMinor ?: BigInteger.ZERO, ralar.assetCode)} (${ralar.seatsTaken}/${ralar.capacity ?: "∞"} seats)",
                    style = FinancialSmall.copy(color = RalarInk)
                  )
                }
                RalarType.HACKATHON -> {
                  Text(
                    text = "Prize: ${Money.formatDisplay(ralar.prizePoolMinor ?: BigInteger.ZERO, ralar.assetCode)} (${ralar.seatsTaken} hackers)",
                    style = FinancialSmall.copy(color = RalarAccent)
                  )
                }
                RalarType.CAUSE -> {
                  Text(
                    text = "Target: ${Money.formatDisplay(ralar.targetAmountMinor ?: BigInteger.ZERO, ralar.assetCode)}",
                    style = FinancialSmall.copy(color = RalarInk)
                  )
                }
              }

              // Actions
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ralar.status == RalarStatus.DRAFT) {
                  OutlinedButton(
                    onClick = { viewModel.publishRalar(ralar.id) },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Text("Publish", style = Typography.labelSmall)
                  }
                } else if (ralar.status == RalarStatus.PUBLISHED) {
                  OutlinedButton(
                    onClick = { viewModel.closeRalar(ralar.id) },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Text("Close", style = Typography.labelSmall)
                  }
                }

                Button(
                  onClick = { viewModel.navigateTo(AppScreen.PublicRalar(ralar.slug)) },
                  shape = RoundedCornerShape(4.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = RalarInk),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Text("View Public", style = Typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }

    // Payments Ledger Section
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Payment Ledger (All Transactions)",
        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    }

    if (payments.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RalarLine, RoundedCornerShape(6.dp))
            .background(RalarSurface)
            .padding(20.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No payments recorded yet. Open a public Ralar to make a Stellar payment.",
            style = Typography.bodySmall.copy(color = RalarMuted)
          )
        }
      }
    } else {
      items(payments) { payment ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
          colors = CardDefaults.cardColors(containerColor = RalarSurface),
          shape = RoundedCornerShape(6.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = payment.kind.name,
                  style = Typography.labelSmall.copy(color = RalarMuted)
                )
                Text(
                  text = "•",
                  style = Typography.labelSmall.copy(color = RalarLine)
                )
                Text(
                  text = Money.formatDisplay(payment.amountMinor, payment.assetCode),
                  style = FinancialBody.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (payment.status == PaymentStatus.PAID) RalarAccent else RalarInk
                  )
                )
              }
              StatusChip(payment.status.name)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Memo & Destination
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Memo: ${payment.memoRef}",
                style = FinancialSmall.copy(fontWeight = FontWeight.SemiBold, color = RalarInk)
              )
              AddressOrHash(
                value = payment.destination,
                label = "To",
                modifier = Modifier
              )
            }

            // Tx Hash & Actions
            if (!payment.providerTransactionId.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                AddressOrHash(
                  value = payment.providerTransactionId,
                  label = "Tx",
                  isTxHash = true
                )

                if (payment.status == PaymentStatus.PROCESSING) {
                  TextButton(
                    onClick = {
                      viewModel.submitAndVerifyPayment(payment.id, payment.providerTransactionId)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("Re-check status", style = Typography.labelSmall.copy(color = RalarPending))
                  }
                }
              }
            } else if (payment.status == PaymentStatus.PROCESSING || payment.status == PaymentStatus.PENDING) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                OutlinedButton(
                  onClick = {
                    showVerifyDialogForPayment = payment
                    manualTxHashInput = ""
                  },
                  shape = RoundedCornerShape(4.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp)
                ) {
                  Text("Submit Tx Hash", style = Typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }

    // Payouts Ledger Section
    if (payouts.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Hackathon Prize Payouts",
          style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }

      items(payouts) { payout ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
          colors = CardDefaults.cardColors(containerColor = RalarSurface),
          shape = RoundedCornerShape(6.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = Money.formatDisplay(payout.amountMinor, payout.assetCode),
                style = FinancialBody.copy(fontWeight = FontWeight.Bold, color = RalarAccent)
              )
              StatusChip(payout.status.name)
            }

            Spacer(modifier = Modifier.height(4.dp))
            AddressOrHash(value = payout.recipientAddress, label = "Winner")
            Text(
              text = "Memo: ${payout.memoRef} (Key: ${payout.prizeKey})",
              style = FinancialSmall.copy(color = RalarMuted)
            )

            if (!payout.providerTransactionId.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              AddressOrHash(
                value = payout.providerTransactionId,
                label = "Tx",
                isTxHash = true
              )
            }
          }
        }
      }
    }
  }

  // Dialog to submit transaction hash for a payment
  if (showVerifyDialogForPayment != null) {
    val payment = showVerifyDialogForPayment!!
    AlertDialog(
      onDismissRequest = { showVerifyDialogForPayment = null },
      title = {
        Text("Submit Stellar Transaction Hash", style = Typography.titleMedium)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Memo: ${payment.memoRef}\nAmount: ${Money.formatDisplay(payment.amountMinor, payment.assetCode)}",
            style = FinancialSmall
          )
          OutlinedTextField(
            value = manualTxHashInput,
            onValueChange = { manualTxHashInput = it },
            label = { Text("64-character Tx Hash", style = Typography.labelSmall) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = FinancialSmall
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (manualTxHashInput.isNotBlank()) {
              viewModel.submitAndVerifyPayment(payment.id, manualTxHashInput.trim())
              showVerifyDialogForPayment = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = RalarInk),
          shape = RoundedCornerShape(4.dp)
        ) {
          Text("Verify on Stellar", style = Typography.labelSmall)
        }
      },
      dismissButton = {
        TextButton(onClick = { showVerifyDialogForPayment = null }) {
          Text("Cancel", style = Typography.labelSmall)
        }
      },
      shape = RoundedCornerShape(6.dp),
      containerColor = RalarSurface
    )
  }
}
