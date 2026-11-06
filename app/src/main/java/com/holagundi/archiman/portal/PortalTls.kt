package com.holagundi.archiman.portal

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.net.InetAddress
import java.net.Socket
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*
import javax.security.auth.x500.X500Principal

/** Device-local non-exportable identity. The browser must pair its fingerprint out of band. */
object PortalTls {
    private const val alias = "archiman.portal.tls.v2"
    data class Identity(val socket: SSLServerSocket, val certificate: X509Certificate) {
        val fingerprint: String get() = MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString(":") { "%02X".format(it.toInt() and 255) }
    }

    @Synchronized fun create(address: String): Identity {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(alias)) KeyPairGenerator.getInstance("EC", "AndroidKeyStore").apply {
            initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                // Conscrypt signs the already-hashed TLS transcript through NONEwithECDSA.
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256).setCertificateSubject(X500Principal("CN=ArchiMan local portal")).build())
        }.generateKeyPair()
        val key = store.getKey(alias, null) as PrivateKey
        val publicKey = store.getCertificate(alias).publicKey
        val certificate = certificate(address, publicKey, key)
        val manager = object : X509ExtendedKeyManager() {
            override fun getClientAliases(type: String?, issuers: Array<out java.security.Principal>?) = null
            override fun chooseClientAlias(types: Array<out String>?, issuers: Array<out java.security.Principal>?, socket: Socket?) = null
            override fun getServerAliases(type: String?, issuers: Array<out java.security.Principal>?) = if (type == "EC") arrayOf(alias) else null
            override fun chooseServerAlias(type: String?, issuers: Array<out java.security.Principal>?, socket: Socket?) = if (type == "EC") alias else null
            override fun getCertificateChain(name: String?) = if (name == alias) arrayOf(certificate) else null
            override fun getPrivateKey(name: String?) = if (name == alias) key else null
        }
        val context = SSLContext.getInstance("TLS").apply { init(arrayOf(manager), null, SecureRandom()) }
        val socket = context.serverSocketFactory.createServerSocket() as SSLServerSocket
        socket.enabledProtocols = socket.supportedProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }.toTypedArray()
        val allowed = setOf("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256")
        socket.enabledCipherSuites = socket.supportedCipherSuites.filter { it in allowed }.toTypedArray()
        return Identity(socket, certificate)
    }

    // Minimal DER encoding for a self-signed EC leaf with an IP Subject Alternative Name.
    // The certificate is deterministic for the device identity and address, so its fingerprint
    // remains stable across portal restarts; it changes after app-data removal or address change.
    private fun certificate(address: String, publicKey: PublicKey, key: PrivateKey): X509Certificate {
        fun der(tag: Int, bytes: ByteArray): ByteArray {
            val length = if (bytes.size < 128) byteArrayOf(bytes.size.toByte()) else {
                val encoded = BigInteger.valueOf(bytes.size.toLong()).toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
                byteArrayOf((128 + encoded.size).toByte()) + encoded
            }
            return byteArrayOf(tag.toByte()) + length + bytes
        }
        fun seq(vararg bytes: ByteArray) = der(0x30, bytes.fold(byteArrayOf()) { a, b -> a + b })
        val algorithm = seq(der(6, byteArrayOf(0x2a,0x86.toByte(),0x48,0xce.toByte(),0x3d,4,3,2)))
        val name = X500Principal("CN=ArchiMan local portal").encoded
        val validity = seq(der(0x18,"20260101000000Z".toByteArray()),der(0x18,"20360101000000Z".toByteArray()))
        val san = seq(der(6,byteArrayOf(0x55,0x1d,0x11)),der(4,seq(der(0x87,InetAddress.getByName(address).address))))
        val serial = BigInteger(1,MessageDigest.getInstance("SHA-256").digest(publicKey.encoded + address.toByteArray()).copyOf(16))
        val body = seq(der(0xa0,der(2,byteArrayOf(2))),der(2,serial.toByteArray()),algorithm,name,validity,name,publicKey.encoded,der(0xa3,seq(san)))
        // ECDSA signatures are randomized. Persisting the certificate is handled below using
        // the keystore's certificate entry, without exporting the private key.
        val existing = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val saved = existing.getCertificate(alias) as X509Certificate
        if (saved.subjectAlternativeNames?.any { it[0] == 7 && it[1] == address } == true) return saved
        val signature = Signature.getInstance("SHA256withECDSA").run { initSign(key); update(body); sign() }
        val cert = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(seq(body,algorithm,der(3,byteArrayOf(0)+signature)))) as X509Certificate
        cert.verify(publicKey)
        existing.setKeyEntry(alias, key, null, arrayOf(cert))
        return cert
    }
}
