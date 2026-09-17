package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.RalarConfig
import com.example.data.model.PaymentStatus
import com.example.data.model.PayoutStatus
import com.example.data.model.RalarStatus
import com.example.data.model.RalarType
import com.example.ui.AppScreen
import com.example.ui.theme.*

@Composable
fun RalarHeader(
  currentScreen: AppScreen,
  onNavigate: (AppScreen) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(RalarPaper)
      .border(width = 1.dp, color = RalarLine)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onNavigate(AppScreen.Dashboard) }
      ) {
        // Geometric ledger glyph
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(RalarInk)
            .border(1.dp, RalarLine, RoundedCornerShape(3.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "R",
            style = Typography.labelMedium.copy(
              color = RalarPaper,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          )
        }

        Text(
          text = "RALAR",
          style = Typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        )
      }

      // Network indicator badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(RalarAccentSurface)
          .border(1.dp, RalarAccent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(RalarAccent)
        )
        Text(
          text = "STELLAR TESTNET",
          style = Typography.labelSmall.copy(
            color = RalarAccent,
            fontWeight = FontWeight.SemiBold
          )
        )
      }
    }
  }
}

@Composable
fun RalarNavigationTabs(
  currentScreen: AppScreen,
  onNavigate: (AppScreen) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(RalarPaper)
      .border(width = 1.dp, color = RalarLine)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceAround
  ) {
    val items = listOf(
      Triple(AppScreen.Dashboard, "Ledger", Icons.Default.Assessment),
      Triple(AppScreen.CreateRalar, "Create", Icons.Default.AddCircleOutline),
      Triple(AppScreen.VerificationInspector, "Inspect", Icons.Default.Search),
      Triple(AppScreen.SystemConfig, "Config", Icons.Default.Settings),
      Triple(AppScreen.Overview, "About", Icons.Default.Info)
    )

    items.forEach { (screen, label, icon) ->
      val isSelected = currentScreen.javaClass == screen.javaClass
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .clickable { onNavigate(screen) }
          .padding(horizontal = 8.dp, vertical = 6.dp)
          .testTag("nav_tab_${label.lowercase()}")
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (isSelected) RalarInk else RalarMuted,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = label,
          style = Typography.labelSmall.copy(
            color = if (isSelected) RalarInk else RalarMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        )
      }
    }
  }
}

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
  val (bgColor, textColor, borderColor) = when (status.uppercase()) {
    "PAID", "CONFIRMED", "PUBLISHED" -> Triple(RalarAccentSurface, RalarAccent, RalarAccent.copy(alpha = 0.4f))
    "PROCESSING", "REGISTERED" -> Triple(RalarPendingSurface, RalarPending, RalarPending.copy(alpha = 0.4f))
    "FAILED", "CANCELLED", "CLOSED" -> Triple(RalarFailSurface, RalarFail, RalarFail.copy(alpha = 0.4f))
    else -> Triple(RalarPaper, RalarMuted, RalarLine)
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(4.dp))
      .background(bgColor)
      .border(1.dp, borderColor, RoundedCornerShape(4.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = status.uppercase(),
      style = Typography.labelSmall.copy(
        color = textColor,
        fontWeight = FontWeight.SemiBold
      )
    )
  }
}

@Composable
fun TypeBadge(type: RalarType, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(4.dp))
      .background(RalarInk)
      .padding(horizontal = 8.dp, vertical = 2.dp)
  ) {
    Text(
      text = type.name,
      style = Typography.labelSmall.copy(
        color = RalarPaper,
        fontWeight = FontWeight.Bold
      )
    )
  }
}

@Composable
fun AddressOrHash(
  value: String,
  label: String? = null,
  isTxHash: Boolean = false,
  modifier: Modifier = Modifier
) {
  val clipboard = LocalClipboardManager.current
  val uriHandler = LocalUriHandler.current

  val displayValue = if (value.length > 16) {
    "${value.take(8)}...${value.takeLast(6)}"
  } else value

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier
  ) {
    if (label != null) {
      Text(
        text = "$label: ",
        style = Typography.labelSmall.copy(color = RalarMuted)
      )
    }

    Text(
      text = displayValue,
      style = FinancialSmall.copy(color = RalarInk),
      modifier = Modifier
        .clip(RoundedCornerShape(3.dp))
        .background(RalarPaper)
        .border(1.dp, RalarLine, RoundedCornerShape(3.dp))
        .clickable {
          clipboard.setText(AnnotatedString(value))
        }
        .padding(horizontal = 6.dp, vertical = 2.dp)
    )

    if (isTxHash && value.length == 64) {
      IconButton(
        onClick = {
          try {
            uriHandler.openUri(RalarConfig.getExplorerUrl(value))
          } catch (e: Exception) {}
        },
        modifier = Modifier.size(24.dp)
      ) {
        Icon(
          imageVector = Icons.Default.OpenInNew,
          contentDescription = "View on Stellar Expert Explorer",
          tint = RalarAccent,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
