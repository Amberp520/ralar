package com.example

import com.example.core.money.Money
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

class MoneyTest {

  @Test
  fun testToMinorWholeUnits() {
    val minor = Money.toMinor("25")
    assertEquals(BigInteger.valueOf(250_000_000L), minor)
  }

  @Test
  fun testToMinorFractionalUnits() {
    val minor1 = Money.toMinor("25.5")
    assertEquals(BigInteger.valueOf(255_000_000L), minor1)

    val minorSmallest = Money.toMinor("0.0000001")
    assertEquals(BigInteger.ONE, minorSmallest)

    val minorMaxDecimals = Money.toMinor("12.3456789")
    assertEquals(BigInteger.valueOf(123_456_789L), minorMaxDecimals)
  }

  @Test
  fun testToMinorThrowsOnExcessDecimals() {
    assertThrows(IllegalArgumentException::class.java) {
      Money.toMinor("1.00000001") // 8 decimal places
    }
  }

  @Test
  fun testToMinorThrowsOnNegative() {
    assertThrows(IllegalArgumentException::class.java) {
      Money.toMinor("-10.00")
    }
  }

  @Test
  fun testToStellarAmount() {
    val stellar = Money.toStellarAmount(BigInteger.valueOf(250_000_000L))
    assertEquals("25.0000000", stellar)

    val small = Money.toStellarAmount(BigInteger.ONE)
    assertEquals("0.0000001", small)
  }

  @Test
  fun testFormatDisplay() {
    val formatted = Money.formatDisplay(BigInteger.valueOf(250_000_000L), "USDC")
    assertEquals("25.00 USDC", formatted)

    val formattedFraction = Money.formatDisplay(BigInteger.valueOf(255_000_000L), "USDC")
    assertEquals("25.50 USDC", formattedFraction)
  }
}
