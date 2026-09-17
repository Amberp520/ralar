package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.RalarViewModel
import com.example.ui.theme.*

@Composable
fun OverviewScreen(
  viewModel: RalarViewModel,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(RalarPaper)
      .padding(16.dp)
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Editorial Header
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
      colors = CardDefaults.cardColors(containerColor = RalarSurface),
      shape = RoundedCornerShape(6.dp)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "RALAR PROTOCOL SPECIFICATION",
          style = Typography.labelSmall.copy(color = RalarMuted, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Create it.\nCollect it.\nSettle it.",
          style = Typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp, color = RalarInk)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "A modern financial ledger for real-world events, participation, and causes on Stellar through the Pollar SDK.",
          style = Typography.bodyMedium.copy(color = RalarMuted)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = { viewModel.navigateTo(AppScreen.CreateRalar) },
          shape = RoundedCornerShape(4.dp),
          colors = ButtonDefaults.buttonColors(containerColor = RalarInk, contentColor = RalarPaper)
        ) {
          Text("Launch Ralar", style = Typography.labelMedium)
        }
      }
    }

    // 4 Non-Negotiables
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, RalarLine, RoundedCornerShape(6.dp)),
      colors = CardDefaults.cardColors(containerColor = RalarSurface),
      shape = RoundedCornerShape(6.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "THE FOUR NON-NEGOTIABLES",
          style = Typography.labelSmall.copy(color = RalarMuted, fontWeight = FontWeight.Bold)
        )

        PrincipleItem(
          num = "01",
          title = "Browser is the Signing Device, Not Authority",
          desc = "The browser wallet signs transactions. It never determines amounts, destinations, or payment outcomes."
        )

        PrincipleItem(
          num = "02",
          title = "Server Holds Absolute Financial Authority",
          desc = "Server calculates and derives all amounts, generates unique cryptographic memos (RLR-XXXXXXXX), and controls capacity."
        )

        PrincipleItem(
          num = "03",
          title = "Exact 7-Decimal Stellar Minor Units",
          desc = "All money is stored as BigInt minor units (amount × 10^7). Floating-point conversions are strictly prohibited."
        )

        PrincipleItem(
          num = "04",
          title = "Independent Stellar On-Chain Verification",
          desc = "No transaction is ever marked PAID based on client reporting. Server independently verifies existence, destination, asset issuer, and memo."
        )
      }
    }

    // Supported Models
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
          text = "THREE PURPOSE-BUILT SETTLEMENT MODELS",
          style = Typography.labelSmall.copy(color = RalarMuted, fontWeight = FontWeight.Bold)
        )

        ModelRow(
          name = "EVENT",
          desc = "Fixed ticket pricing in USDC, atomic capacity enforcement, and verifiable admission receipts."
        )

        ModelRow(
          name = "HACKATHON",
          desc = "Participant registration, locked prize pool, and cryptographically verified organizer prize payouts."
        )

        ModelRow(
          name = "CAUSE",
          desc = "Open donor funding with transparent on-chain progress toward target settlement goals."
        )
      }
    }
  }
}

@Composable
private fun PrincipleItem(num: String, title: String, desc: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = num,
      style = FinancialBody.copy(color = RalarAccent, fontWeight = FontWeight.Bold)
    )
    Column {
      Text(
        text = title,
        style = Typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RalarInk)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = desc,
        style = Typography.bodySmall.copy(color = RalarMuted)
      )
    }
  }
}

@Composable
private fun ModelRow(name: String, desc: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(4.dp))
      .background(RalarPaper)
      .border(1.dp, RalarLine, RoundedCornerShape(4.dp))
      .padding(10.dp)
  ) {
    Text(
      text = name,
      style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = RalarInk)
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = desc,
      style = Typography.bodySmall.copy(color = RalarMuted)
    )
  }
}
