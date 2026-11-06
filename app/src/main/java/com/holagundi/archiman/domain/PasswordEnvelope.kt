package com.holagundi.archiman.domain

import java.io.*
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned authenticated container. No password, filename or plaintext is stored outside the ciphertext. */
object PasswordEnvelope {
    private val magic = "ARCHISEC".toByteArray(Charsets.US_ASCII)
    private const val iterations = 600_000
    private const val headerSize = 8 + 1 + 4 + 16 + 12
    fun isEncrypted(bytes: ByteArray) = bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(magic)

    fun encrypt(plain: ByteArray, password: CharArray, limit: Int): ByteArray {
        require(password.size in 12..256) { "Use a password of 12–256 characters. Keep it safely; it cannot be recovered." }
        require(plain.size <= limit) { "File exceeds the protected-file size limit." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val header = ByteArrayOutputStream().also { stream -> DataOutputStream(stream).use { it.write(magic); it.writeByte(1); it.writeInt(iterations); it.write(salt); it.write(nonce) } }.toByteArray()
        return crypt(Cipher.ENCRYPT_MODE, plain, password, salt, nonce, header).let { header + it }
    }

    fun decrypt(envelope: ByteArray, password: CharArray, limit: Int): ByteArray {
        require(envelope.size in (headerSize + 16)..(limit + headerSize + 16)) { "Invalid or oversized protected file." }
        val input = DataInputStream(ByteArrayInputStream(envelope))
        val marker = ByteArray(8).also(input::readFully)
        require(marker.contentEquals(magic) && input.readUnsignedByte() == 1 && input.readInt() == iterations) { "Unsupported protected-file format." }
        require(password.size in 1..256) { "Enter the file password." }
        val salt = ByteArray(16).also(input::readFully); val nonce = ByteArray(12).also(input::readFully)
        return try { crypt(Cipher.DECRYPT_MODE, envelope.copyOfRange(headerSize, envelope.size), password, salt, nonce, envelope.copyOfRange(0, headerSize)) }
        catch (error: javax.crypto.AEADBadTagException) { throw IllegalArgumentException("Incorrect password or damaged protected file.") }
    }

    private fun crypt(mode: Int, bytes: ByteArray, password: CharArray, salt: ByteArray, nonce: ByteArray, header: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val key = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
        try {
            return Cipher.getInstance("AES/GCM/NoPadding").run { init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce)); updateAAD(header); doFinal(bytes) }
        } finally { key.fill(0) }
    }

    fun readBounded(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val count = input.read(buffer); if (count < 0) break; require(output.size().toLong() + count <= limit) { "File exceeds the size limit." }; output.write(buffer, 0, count) }
        return output.toByteArray()
    }
}
