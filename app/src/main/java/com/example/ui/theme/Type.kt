package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Ralar Typography scale: 13, 14, 16, 20, 28, 40sp. Headings letterSpacing -0.02em.
val Typography = Typography(
  displayLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 40.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.02).sp
  ),
  displayMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.02).sp
  ),
  titleLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.02).sp
  ),
  titleMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.01).sp
  ),
  bodyLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  bodySmall = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp
  ),
  labelLarge = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.05.sp
  ),
  labelMedium = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.04.sp
  ),
  labelSmall = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.06.sp
  )
)

// Monospace styles for financial amounts, addresses, hashes, and technical labels
val FinancialLarge = TextStyle(
  fontFamily = FontFamily.Monospace,
  fontWeight = FontWeight.Bold,
  fontSize = 28.sp,
  lineHeight = 32.sp
)

val FinancialMedium = TextStyle(
  fontFamily = FontFamily.Monospace,
  fontWeight = FontWeight.SemiBold,
  fontSize = 20.sp,
  lineHeight = 24.sp
)

val FinancialBody = TextStyle(
  fontFamily = FontFamily.Monospace,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp
)

val FinancialSmall = TextStyle(
  fontFamily = FontFamily.Monospace,
  fontWeight = FontWeight.Normal,
  fontSize = 12.sp,
  lineHeight = 16.sp
)
