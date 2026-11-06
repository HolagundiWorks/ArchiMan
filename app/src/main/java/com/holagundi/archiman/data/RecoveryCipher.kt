package com.holagundi.archiman.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Internal safety copies are device-bound; portable exports use a user password instead. */
object RecoveryCipher {
    private val marker = "ARCHIREC1".toByteArray(Charsets.US_ASCII)
    private const val alias = "archiman.recovery.aes.v1"
    fun protected(bytes: ByteArray) = bytes.size >= marker.size && bytes.copyOfRange(0,marker.size).contentEquals(marker)
    @Synchronized private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias,null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun encrypt(plain: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
        init(Cipher.ENCRYPT_MODE,key()); updateAAD(marker); marker + iv + doFinal(plain)
    }
    fun decrypt(bytes: ByteArray): ByteArray {
        require(protected(bytes) && bytes.size >= marker.size + 12 + 16) { "Invalid internal recovery copy." }
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(marker.size,marker.size+12)))
            updateAAD(marker); doFinal(bytes,marker.size+12,bytes.size-marker.size-12)
        }
    }
}
