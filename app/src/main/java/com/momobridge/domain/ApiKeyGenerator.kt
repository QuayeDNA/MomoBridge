package com.momobridge.domain

import java.security.SecureRandom

object ApiKeyGenerator {

    private const val PREFIX = "mb_"
    private const val HEX_CHARS = 32
    private val random = SecureRandom()

    fun generate(): String {
        val hex = ByteArray(HEX_CHARS).let { bytes ->
            random.nextBytes(bytes)
            bytes.joinToString("") { "%02x".format(it) }
        }
        return PREFIX + hex
    }
}
