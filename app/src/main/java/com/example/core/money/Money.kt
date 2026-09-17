package com.example.core.money

import java.math.BigInteger

/**
 * Ralar Authoritative Money Engine
 *
 * Mandate:
 * - Stellar uses exactly 7 decimal places.
 * - All authoritative values are stored as BigInteger minor units: human amount × 10^7.
 * - Float, Double, and floating-point conversions are strictly forbidden for financial calculations.
 */
object Money {
  val DECIMALS: Int = 7
  val SCALE: BigInteger = BigInteger.valueOf(10_000_000L) // 10^7
  val ZERO: BigInteger = BigInteger.ZERO

  /**
   * Converts a human decimal string (e.g. "25", "25.5", "0.0000001") into minor units.
   * Throws IllegalArgumentException on invalid format, negative amounts, or >7 decimal places.
   */
  fun toMinor(human: String): BigInteger {
    val trimmed = human.trim()
    if (trimmed.isEmpty()) throw IllegalArgumentException("Amount cannot be empty")
    if (trimmed.startsWith("-")) throw IllegalArgumentException("Amount cannot be negative")

    val parts = trimmed.split(".")
    if (parts.size > 2) throw IllegalArgumentException("Invalid decimal string: multiple decimal points")

    val integerPart = parts[0]
    if (integerPart.isNotEmpty() && !integerPart.all { it.isDigit() }) {
      throw IllegalArgumentException("Invalid characters in integer portion: $integerPart")
    }

    val wholeUnits = if (integerPart.isEmpty()) BigInteger.ZERO else BigInteger(integerPart)

    if (parts.size == 1) {
      return wholeUnits.multiply(SCALE)
    }

    val fractionalPart = parts[1]
    if (!fractionalPart.all { it.isDigit() }) {
      throw IllegalArgumentException("Invalid characters in fractional portion: $fractionalPart")
    }
    if (fractionalPart.length > DECIMALS) {
      throw IllegalArgumentException("Exceeds maximum precision: Stellar supports at most 7 decimal places")
    }

    // Pad with trailing zeros to exactly 7 decimal places
    val paddedFraction = fractionalPart.padEnd(DECIMALS, '0')
    val fractionUnits = BigInteger(paddedFraction)

    return wholeUnits.multiply(SCALE).add(fractionUnits)
  }

  /**
   * Converts minor units into exactly 7-decimal Stellar string format.
   * Example: 250000000L -> "25.0000000"
   */
  fun toStellarAmount(minor: BigInteger): String {
    if (minor < BigInteger.ZERO) throw IllegalArgumentException("Authoritative minor units cannot be negative")

    val quotient = minor.divide(SCALE)
    val remainder = minor.remainder(SCALE)

    val remainderStr = remainder.toString().padStart(DECIMALS, '0')
    return "$quotient.$remainderStr"
  }

  /**
   * Formats for human display.
   * Examples:
   * 250000000L, "USDC" -> "25.00 USDC"
   * 255000000L, "USDC" -> "25.50 USDC"
   * 1234567L, "USDC" -> "0.1234567 USDC"
   */
  fun formatDisplay(minor: BigInteger, code: String): String {
    val stellar = toStellarAmount(minor)
    val parts = stellar.split(".")
    val whole = parts[0]
    val fraction = parts[1]

    // Trim trailing zeros but keep at least 2 decimal places for financial readability
    var trimmedFraction = fraction.trimEnd('0')
    if (trimmedFraction.length < 2) {
      trimmedFraction = trimmedFraction.padEnd(2, '0')
    }

    return "$whole.$trimmedFraction $code".trim()
  }

  /**
   * Formats without the currency code.
   */
  fun formatNumber(minor: BigInteger): String {
    val stellar = toStellarAmount(minor)
    val parts = stellar.split(".")
    val whole = parts[0]
    val fraction = parts[1]

    var trimmedFraction = fraction.trimEnd('0')
    if (trimmedFraction.length < 2) {
      trimmedFraction = trimmedFraction.padEnd(2, '0')
    }
    return "$whole.$trimmedFraction"
  }
}
