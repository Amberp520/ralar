package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.money.Money
import com.example.data.model.PaymentKind
import com.example.data.model.PaymentStatus
import com.example.data.model.RalarStatus
import com.example.data.model.RalarType
import com.example.ui.AppScreen
import com.example.ui.RalarViewModel
import com.example.ui.components.AddressOrHash
import com.example.ui.components.StatusChip
import com.example.ui.components.TypeBadge
import com.example.ui.theme.*
import java.math.BigInteger

@Composable
fun PublicRalarScreen(
  slug: String,
  viewModel: RalarViewModel,
  modifier: Modifier = Modifier
) {
  val ralar by viewModel.selectedRalar.collectAsState()
  val payments by viewModel.selectedRalarPayments.collectAsState()
  val activeWallet by viewModel.activeWallet.collectAsState()
  val clipboard = LocalClipboardManager.current

  var donationInput by remember { mutableStateOf("25.00") }
  var txHashToSubmit by remember { mutableStateOf("") }
  var lastGeneratedMemo by remember { mutableStateOf<String?>(null) }

  val confirmedRaisedMinor = remember(payments) {
    payments.filter { it.status == PaymentStatus.PAID }
      .fold(BigInteger.ZERO) { acc, p -> acc.add(p.amountMinor) }
  }

  if (ralar == null) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(RalarPaper)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = RalarInk)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Loading public receipt...", style = Typography.bodyMedium)
      }
    }
    return
  }

  val currentRalar = ralar!!
  val isSoldOut = currentRalar.capacity != null && currentRalar.seatsTaken >= currentRalar.capacity!!

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(RalarPaper)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Navigation & Technical Banner
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RalarInk)
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TypeBadge(currentRalar.type)
          StatusChip(currentRalar.status.name)
        }
      }
    }

    // Public Receipt Card (Modern Editorial Layout)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = RalarSurface),
        shape = RoundedCornerShape(6.dp)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "/r/${currentRalar.slug}",
            style = FinancialSmall.copy(color = RalarMuted)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = currentRalar.title,
            style = Typography.displayMedium.copy(fontSize = 26.sp, color = RalarInk)
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = currentRalar.description,
            style = Typography.bodyMedium.copy(color = RalarMuted)
          )

          if (!currentRalar.location.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Venue: ${currentRalar.location}",
                style = Typography.labelSmall.copy(color = RalarInk)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Divider(color = RalarLine, thickness = 1.dp)
          Spacer(modifier = Modifier.height(14.dp))

          // Financial Terms Section
          when (currentRalar.type) {
            RalarType.EVENT -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("TICKET PRICE", style = Typography.labelSmall.copy(color = RalarMuted))
                  Text(
                    text = Money.formatDisplay(currentRalar.ticketPriceMinor ?: BigInteger.ZERO, currentRalar.assetCode),
                    style = FinancialMedium.copy(color = RalarInk)
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("CAPACITY", style = Typography.labelSmall.copy(color = RalarMuted))
                  Text(
                    text = "${currentRalar.seatsTaken} / ${currentRalar.capacity ?: "∞"} Seats",
                    style = FinancialMedium.copy(
                      color = if (isSoldOut) RalarFail else RalarInk
                    )
                  )
                }
              }
            }

            RalarType.HACKATHON -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("PRIZE POOL", style = Typography.labelSmall.copy(color = RalarMuted))
                  Text(
                    text = Money.formatDisplay(currentRalar.prizePoolMinor ?: BigInteger.ZERO, currentRalar.assetCode),
                    style = FinancialMedium.copy(color = RalarAccent)
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("REGISTRATION FEE", style = Typography.labelSmall.copy(color = RalarMuted))
                  Text(
                    text = Money.formatDisplay(currentRalar.registrationFeeMinor ?: BigInteger.ZERO, currentRalar.assetCode),
                    style = FinancialMedium.copy(color = RalarInk)
                  )
                }
              }
            }

            RalarType.CAUSE -> {
              Column {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("CONFIRMED RAISED", style = Typography.labelSmall.copy(color = RalarMuted))
                  Text("TARGET", style = Typography.labelSmall.copy(color = RalarMuted))
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = Money.formatDisplay(confirmedRaisedMinor, currentRalar.assetCode),
                    style = FinancialMedium.copy(color = RalarAccent)
                  )
                  Text(
                    text = Money.formatDisplay(currentRalar.targetAmountMinor ?: BigInteger.ZERO, currentRalar.assetCode),
                    style = FinancialMedium.copy(color = RalarInk)
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Progress Bar
                val target = currentRalar.targetAmountMinor ?: BigInteger.ONE
                val progress = if (target > BigInteger.ZERO) {
                  (confirmedRaisedMinor.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                  progress = { progress },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                  color = RalarAccent,
                  trackColor = RalarLine
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          AddressOrHash(
            value = currentRalar.paymentDestination,
            label = "Settlement Destination"
          )
        }
      }
    }

    // Pollar Wallet Status Box
    item {
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
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(RalarAccent)
              )
              Text(
                text = "POLLAR WALLET",
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold)
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(RalarPaper)
                .border(1.dp, RalarLine, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "Custody: ${activeWallet?.custody ?: "smart"}",
                style = Typography.labelSmall.copy(fontSize = 10.sp, color = RalarMuted)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          AddressOrHash(
            value = activeWallet?.address ?: "Not connected",
            label = "Signing Address"
          )

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(
              onClick = { viewModel.requestWalletFunding() },
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(32.dp)
            ) {
              Text("Fund Testnet Wallet", style = Typography.labelSmall)
            }
          }
        }
      }
    }

    // Payment Execution Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = RalarSurface),
        shape = RoundedCornerShape(6.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Execute Payment",
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Server authorizes parameters. Browser wallet signs transaction.",
            style = Typography.bodySmall.copy(color = RalarMuted)
          )

          Spacer(modifier = Modifier.height(12.dp))

          if (currentRalar.type == RalarType.CAUSE) {
            OutlinedTextField(
              value = donationInput,
              onValueChange = { donationInput = it },
              label = { Text("Donation Amount (${currentRalar.assetCode})", style = Typography.labelSmall) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.fillMaxWidth(),
              textStyle = FinancialBody,
              shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
          }

          if (isSoldOut && currentRalar.type != RalarType.CAUSE) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(RalarFailSurface)
                .border(1.dp, RalarFail.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "CAPACITY REACHED — SOLD OUT",
                style = Typography.labelMedium.copy(color = RalarFail, fontWeight = FontWeight.Bold)
              )
            }
          } else {
            Button(
              onClick = {
                viewModel.executePayment(
                  ralarId = currentRalar.id,
                  causeDonationHumanAmount = if (currentRalar.type == RalarType.CAUSE) donationInput else null
                )
              },
              shape = RoundedCornerShape(4.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = RalarInk,
                contentColor = RalarPaper
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("execute_payment_button")
            ) {
              Text(
                text = when (currentRalar.type) {
                  RalarType.EVENT -> "Authorize Ticket (${Money.formatDisplay(currentRalar.ticketPriceMinor ?: BigInteger.ZERO, currentRalar.assetCode)})"
                  RalarType.HACKATHON -> "Authorize Registration (${Money.formatDisplay(currentRalar.registrationFeeMinor ?: BigInteger.ZERO, currentRalar.assetCode)})"
                  RalarType.CAUSE -> "Authorize Donation ($donationInput ${currentRalar.assetCode})"
                },
                style = Typography.labelMedium
              )
            }
          }
        }
      }
    }

    // Payments for this Ralar
    item {
      Text(
        text = "Receipts & Confirmed Ledger",
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
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No payments recorded for this Ralar yet.",
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
              Text(
                text = Money.formatDisplay(payment.amountMinor, payment.assetCode),
                style = FinancialBody.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (payment.status == PaymentStatus.PAID) RalarAccent else RalarInk
                )
              )
              StatusChip(payment.status.name)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Memo: ${payment.memoRef}",
              style = FinancialSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            if (!payment.providerTransactionId.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              AddressOrHash(
                value = payment.providerTransactionId,
                label = "Tx",
                isTxHash = true
              )
            }
          }
        }
      }
    }
  }
}
