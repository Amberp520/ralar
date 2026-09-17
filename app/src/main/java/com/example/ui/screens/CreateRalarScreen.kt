package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.config.RalarConfig
import com.example.data.model.RalarType
import com.example.ui.AppScreen
import com.example.ui.RalarViewModel
import com.example.ui.theme.*

@Composable
fun CreateRalarScreen(
  viewModel: RalarViewModel,
  modifier: Modifier = Modifier
) {
  var selectedType by remember { mutableStateOf(RalarType.EVENT) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var capacityInput by remember { mutableStateOf("100") }
  var paymentDestination by remember {
    mutableStateOf(RalarConfig.DEFAULT_ASSET_ISSUER)
  }

  // Type specific fields
  var ticketPriceInput by remember { mutableStateOf("25.00") }
  var registrationFeeInput by remember { mutableStateOf("10.00") }
  var prizePoolInput by remember { mutableStateOf("500.00") }
  var targetAmountInput by remember { mutableStateOf("1000.00") }

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(RalarPaper)
      .padding(16.dp)
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      IconButton(
        onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
        modifier = Modifier.size(36.dp)
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RalarInk)
      }
      Text(
        text = "Create New Ralar",
        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )
    }

    Text(
      text = "Configure settlement parameters on Stellar testnet. Amounts are fixed in 7-decimal minor units.",
      style = Typography.bodySmall.copy(color = RalarMuted)
    )

    // Type Selector (Segmented tabs)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(4.dp))
        .background(RalarSurface)
        .border(1.dp, RalarLine, RoundedCornerShape(4.dp))
        .padding(4.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      RalarType.values().forEach { type ->
        val isSelected = selectedType == type
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) RalarInk else RalarSurface)
            .clickable { selectedType = type }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = type.name,
            style = Typography.labelSmall.copy(
              color = if (isSelected) RalarPaper else RalarInk,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          )
        }
      }
    }

    // Title
    OutlinedTextField(
      value = title,
      onValueChange = { title = it },
      label = { Text("Title", style = Typography.labelSmall) },
      placeholder = { Text("e.g. Stellar Builder Meetup / Hacker Sprint", style = Typography.bodySmall) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("create_title_input"),
      shape = RoundedCornerShape(4.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RalarInk,
        unfocusedBorderColor = RalarLine
      )
    )

    // Description
    OutlinedTextField(
      value = description,
      onValueChange = { description = it },
      label = { Text("Description & Schedule", style = Typography.labelSmall) },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
      shape = RoundedCornerShape(4.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RalarInk,
        unfocusedBorderColor = RalarLine
      )
    )

    // Location / Venue
    OutlinedTextField(
      value = location,
      onValueChange = { location = it },
      label = { Text("Location / Virtual Link", style = Typography.labelSmall) },
      placeholder = { Text("San Francisco, CA or Online Discord", style = Typography.bodySmall) },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(4.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RalarInk,
        unfocusedBorderColor = RalarLine
      )
    )

    // Payment Destination (Stellar public key)
    OutlinedTextField(
      value = paymentDestination,
      onValueChange = { paymentDestination = it },
      label = { Text("Payment Destination (Stellar Address)", style = Typography.labelSmall) },
      modifier = Modifier.fillMaxWidth(),
      textStyle = FinancialSmall,
      shape = RoundedCornerShape(4.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RalarInk,
        unfocusedBorderColor = RalarLine
      )
    )

    // Conditional Fields based on RalarType
    when (selectedType) {
      RalarType.EVENT -> {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = ticketPriceInput,
            onValueChange = { ticketPriceInput = it },
            label = { Text("Ticket Price (USDC)", style = Typography.labelSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            textStyle = FinancialBody,
            shape = RoundedCornerShape(4.dp)
          )
          OutlinedTextField(
            value = capacityInput,
            onValueChange = { capacityInput = it },
            label = { Text("Capacity (Seats)", style = Typography.labelSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            textStyle = FinancialBody,
            shape = RoundedCornerShape(4.dp)
          )
        }
      }

      RalarType.HACKATHON -> {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = prizePoolInput,
            onValueChange = { prizePoolInput = it },
            label = { Text("Prize Pool (USDC)", style = Typography.labelSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            textStyle = FinancialBody,
            shape = RoundedCornerShape(4.dp)
          )
          OutlinedTextField(
            value = registrationFeeInput,
            onValueChange = { registrationFeeInput = it },
            label = { Text("Reg Fee (USDC)", style = Typography.labelSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            textStyle = FinancialBody,
            shape = RoundedCornerShape(4.dp)
          )
        }
        OutlinedTextField(
          value = capacityInput,
          onValueChange = { capacityInput = it },
          label = { Text("Hacker Capacity", style = Typography.labelSmall) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          textStyle = FinancialBody,
          shape = RoundedCornerShape(4.dp)
        )
      }

      RalarType.CAUSE -> {
        OutlinedTextField(
          value = targetAmountInput,
          onValueChange = { targetAmountInput = it },
          label = { Text("Fundraising Target (USDC)", style = Typography.labelSmall) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
          textStyle = FinancialBody,
          shape = RoundedCornerShape(4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Create Action Button
    Button(
      onClick = {
        val cap = capacityInput.toIntOrNull()
        viewModel.createRalar(
          type = selectedType,
          title = title,
          description = description,
          startsAt = System.currentTimeMillis() + 86400000L,
          endsAt = System.currentTimeMillis() + 86400000L * 3,
          location = location.takeIf { it.isNotBlank() },
          capacity = cap,
          targetAmountHuman = if (selectedType == RalarType.CAUSE) targetAmountInput else null,
          ticketPriceHuman = if (selectedType == RalarType.EVENT) ticketPriceInput else null,
          registrationFeeHuman = if (selectedType == RalarType.HACKATHON) registrationFeeInput else null,
          prizePoolHuman = if (selectedType == RalarType.HACKATHON) prizePoolInput else null,
          paymentDestination = paymentDestination
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
        .testTag("create_submit_button")
    ) {
      Text("Create Ralar & Settle on Stellar", style = Typography.labelMedium)
    }
  }
}
