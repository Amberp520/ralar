package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Ralar strictly uses the curated financial ledger palette (Light mode only)
private val RalarColorScheme = lightColorScheme(
  primary = RalarInk,
  onPrimary = RalarPaper,
  primaryContainer = RalarSurface,
  onPrimaryContainer = RalarInk,
  secondary = RalarAccent,
  onSecondary = RalarPaper,
  secondaryContainer = RalarAccentSurface,
  onSecondaryContainer = RalarAccent,
  background = RalarPaper,
  onBackground = RalarInk,
  surface = RalarSurface,
  onSurface = RalarInk,
  surfaceVariant = RalarPaper,
  onSurfaceVariant = RalarMuted,
  outline = RalarLine,
  outlineVariant = RalarLine,
  error = RalarFail,
  onError = RalarSurface,
  errorContainer = RalarFailSurface,
  onErrorContainer = RalarFail
)

// Precision corner radius: maximum 6dp for cards, 4dp for inputs and chips
val RalarShapes = Shapes(
  extraSmall = RoundedCornerShape(4.dp),
  small = RoundedCornerShape(4.dp),
  medium = RoundedCornerShape(6.dp),
  large = RoundedCornerShape(6.dp),
  extraLarge = RoundedCornerShape(6.dp)
)

@Composable
fun RalarTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = RalarColorScheme,
    typography = Typography,
    shapes = RalarShapes,
    content = content
  )
}
