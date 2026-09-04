package com.example.portal

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordDigest(val hash: String, val salt: String, val iterations: Int)

object PortalPasswordHasher {
    const val ITERATIONS = 600_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun hash(password: CharArray, random: SecureRandom = SecureRandom()): PasswordDigest {
        require(password.size in 10..128) { "Password must contain 10 to 128 characters." }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(password, salt, ITERATIONS)
        return PasswordDigest(Base64.encodeToString(hash, Base64.NO_WRAP), Base64.encodeToString(salt, Base64.NO_WRAP), ITERATIONS)
    }

    fun verify(password: CharArray, expectedHash: String, salt: String, iterations: Int): Boolean {
        if (password.size !in 1..128 || iterations < 100_000) {
            password.fill('\u0000')
            return false
        }
        val expected = runCatching { Base64.decode(expectedHash, Base64.DEFAULT) }.getOrNull()
        val decodedSalt = runCatching { Base64.decode(salt, Base64.DEFAULT) }.getOrNull()
        if (expected == null || decodedSalt == null) {
            password.fill('\u0000')
            return false
        }
        return MessageDigest.isEqual(expected, derive(password, decodedSalt, iterations))
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword(); password.fill('\u0000') }
    }
}

data class PortalPrincipal(val userId: Long, val username: String, val displayName: String, val role: String)
