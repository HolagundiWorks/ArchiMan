package com.holagundi.archiman.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Seals and opens small secrets. The app uses the Android Keystore ([KeystoreBox]); the tests use a stand-in. */
interface SecretBox {
    fun seal(plain: ByteArray): ByteArray
    fun open(sealed: ByteArray): ByteArray
}

/** AES-GCM with a non-exportable key held in the Android Keystore. The sealed form is the 12-byte IV followed by the ciphertext. */
class KeystoreBox(private val alias: String = "mynahub_cloud_session") : SecretBox {
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    override fun seal(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return c.iv + c.doFinal(plain)
    }

    override fun open(sealed: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed.copyOfRange(0, 12))) }
        return c.doFinal(sealed.copyOfRange(12, sealed.size))
    }
}

/** Where the key-value pairs live. The app uses a private SharedPreferences file; the tests use a map. */
interface KeyValues {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

class PrefsKeyValues(context: Context, name: String = "cloud_session") : KeyValues {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    override fun get(key: String) = prefs.getString(key, null)
    override fun put(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
}

/**
 * The cloud sign-in kept on this phone: the session cookies sealed with the Keystore, plus the account's id and email in the clear.
 * The app works fully without it (local sign-in does not depend on it); losing it only means signing in to the cloud again.
 */
class CloudSessionStore(private val values: KeyValues, private val box: SecretBox) {
    fun save(session: CloudSession) {
        values.put("user_id", session.userId)
        values.put("email", session.email)
        values.put("sealed", Base64.getEncoder().encodeToString(box.seal(session.cookies.toByteArray(Charsets.UTF_8))))
    }

    /** The stored session, or null when none exists or it cannot be opened (a restored device, a cleared key). */
    fun load(): CloudSession? {
        val sealed = values.get("sealed") ?: return null
        return try {
            CloudSession(values.get("user_id").orEmpty(), values.get("email").orEmpty(), String(box.open(Base64.getDecoder().decode(sealed)), Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    fun email(): String? = values.get("email")

    fun clear() {
        listOf("user_id", "email", "sealed").forEach(values::remove)
    }
}
