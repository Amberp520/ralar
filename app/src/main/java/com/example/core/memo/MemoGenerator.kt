package com.example.core.memo

import java.security.SecureRandom

/**
 * Server-side unique cryptographic memo generator for Ralar on Stellar.
 * Pattern: "RLR-" followed by 8 uppercase Base32 characters.
 * Example: RLR-3F9K2Q8M
 */
object MemoGenerator {
  private const val PREFIX = "RLR-"
  // Crockford's / RFC 4648 Base32 alphabet (avoiding ambiguous letters)
  private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
  private val random = SecureRandom()

  fun generate(): String {
    val sb = StringBuilder(PREFIX)
    for (i in 0 until 8) {
      val index = random.nextInt(ALPHABET.length)
      sb.append(ALPHABET[index])
    }
    return sb.toString()
  }

  fun isValid(memo: String): Boolean {
    if (!memo.startsWith(PREFIX)) return false
    val suffix = memo.removePrefix(PREFIX)
    if (suffix.length != 8) return false
    return suffix.all { it in ALPHABET }
  }
}
