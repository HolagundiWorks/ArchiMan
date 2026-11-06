package com.holagundi.archiman.domain

import org.junit.Assert.*
import org.junit.Test

class PasswordEnvelopeTest {
    private val password = "sample strong passphrase".toCharArray()
    @Test fun roundTripAndRandomization() {
        val plain = "Private practice information".toByteArray()
        val first = PasswordEnvelope.encrypt(plain,password,1024)
        val second = PasswordEnvelope.encrypt(plain,password,1024)
        assertFalse(first.contentEquals(second)); assertTrue(PasswordEnvelope.isEncrypted(first))
        assertArrayEquals(plain,PasswordEnvelope.decrypt(first,password,1024))
        assertFalse(first.toString(Charsets.ISO_8859_1).contains("Private practice"))
    }
    @Test fun rejectsWrongPasswordAndTampering() {
        val bytes = PasswordEnvelope.encrypt("confidential".toByteArray(),password,1024)
        assertTrue(runCatching { PasswordEnvelope.decrypt(bytes,"wrong password".toCharArray(),1024) }.isFailure)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertTrue(runCatching { PasswordEnvelope.decrypt(bytes,password,1024) }.isFailure)
    }
    @Test fun rejectsHeaderWorkFactorBomb() {
        val bytes = PasswordEnvelope.encrypt(byteArrayOf(1),password,1024)
        bytes[9] = 127
        assertTrue(runCatching { PasswordEnvelope.decrypt(bytes,password,1024) }.isFailure)
    }
    @Test fun rejectsWeakPasswordAndOversize() {
        assertTrue(runCatching { PasswordEnvelope.encrypt(byteArrayOf(1),"short".toCharArray(),1024) }.isFailure)
        assertTrue(runCatching { PasswordEnvelope.encrypt(ByteArray(20),password,10) }.isFailure)
        assertTrue(runCatching { PasswordEnvelope.readBounded(ByteArray(20).inputStream(),10) }.isFailure)
        assertTrue(runCatching { PasswordEnvelope.decrypt(ByteArray(200),password,100) }.isFailure)
    }
    @Test fun rejectsTruncationAndUnknownVersion() {
        val bytes = PasswordEnvelope.encrypt(byteArrayOf(1),password,1024)
        assertTrue(runCatching { PasswordEnvelope.decrypt(bytes.copyOf(12),password,1024) }.isFailure)
        bytes[8] = 2
        assertTrue(runCatching { PasswordEnvelope.decrypt(bytes,password,1024) }.isFailure)
    }
}
