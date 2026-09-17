package com.example

import com.example.core.memo.MemoGenerator
import org.junit.Assert.*
import org.junit.Test

class MemoGeneratorTest {

  @Test
  fun testMemoPrefixAndLength() {
    val memo = MemoGenerator.generate()
    assertTrue(memo.startsWith("RLR-"))
    assertEquals(12, memo.length)
    assertTrue(MemoGenerator.isValid(memo))
  }

  @Test
  fun testMemoUniqueness() {
    val seen = mutableSetOf<String>()
    for (i in 0 until 1000) {
      val memo = MemoGenerator.generate()
      assertFalse("Collision detected for memo $memo", seen.contains(memo))
      seen.add(memo)
    }
  }

  @Test
  fun testInvalidMemos() {
    assertFalse(MemoGenerator.isValid("INVALID"))
    assertFalse(MemoGenerator.isValid("RLR-123"))
    assertFalse(MemoGenerator.isValid("rlr-3F9K2Q8M"))
  }
}
