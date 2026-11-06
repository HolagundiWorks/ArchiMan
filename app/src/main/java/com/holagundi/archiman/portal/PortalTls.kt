package com.holagundi.archiman.portal

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.File
import java.math.BigInteger
import java.net.InetAddress
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.X509ExtendedKeyManager
import javax.security.auth.x500.X500Principal

/** Device-local non-exportable identity. The browser must pair its fingerprint out of band. */
object PortalTls {
    private const val alias = "archiman.portal.tls.v2"
    private const val leafFile = "portal_tls_leaf.der"
    private const val leafIpFile = "portal_tls_leaf.ip"

    data class Identity(val socket: SSLServerSocket, val certificate: X509Certificate) {
        val fingerprint: String get() = MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString(":") { "%02X".format(it.toInt() and 255) }
    }

    @Synchronized
    fun create(address: String, storeDir: File): Identity {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(alias)) {
            KeyPairGenerator.getInstance("EC", "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                        .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                        // Conscrypt signs the already-hashed TLS transcript through NONEwithECDSA.
                        .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
                        .setCertificateSubject(X500Principal("CN=ArchiMan local portal"))
                        .build(),
                )
            }.generateKeyPair()
        }
        val key = store.getKey(alias, null) as? PrivateKey
            ?: error("Portal TLS private key is unavailable on this device.")
        val publicKey = (store.getCertificate(alias) as? X509Certificate)?.publicKey
            ?: error("Portal TLS key is missing a public certificate stub.")
        val certificate = leafCertificate(address, publicKey, key, storeDir)
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
        val allowed = setOf(
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
        )
        socket.enabledCipherSuites = socket.supportedCipherSuites.filter { it in allowed }.toTypedArray()
        return Identity(socket, certificate)
    }

    /**
     * Keep the private key in AndroidKeyStore, but persist the leaf certificate in app-private
     * storage. Rewriting AndroidKeyStore certificate entries fails on several OEM keystores
     * (notably Samsung StrongBox) when the Wi-Fi address changes.
     */
    private fun leafCertificate(address: String, publicKey: PublicKey, key: PrivateKey, storeDir: File): X509Certificate {
        storeDir.mkdirs()
        val certFile = File(storeDir, leafFile)
        val ipFile = File(storeDir, leafIpFile)
        if (certFile.isFile && ipFile.isFile && ipFile.readText() == address) {
            runCatching {
                CertificateFactory.getInstance("X.509").generateCertificate(certFile.inputStream()) as X509Certificate
            }.getOrNull()?.let { saved ->
                if (hasIpSan(saved, address)) return saved
            }
        }
        fun der(tag: Int, bytes: ByteArray): ByteArray {
            val length = if (bytes.size < 128) {
                byteArrayOf(bytes.size.toByte())
            } else {
                val encoded = BigInteger.valueOf(bytes.size.toLong()).toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
                byteArrayOf((128 + encoded.size).toByte()) + encoded
            }
            return byteArrayOf(tag.toByte()) + length + bytes
        }
        fun seq(vararg bytes: ByteArray) = der(0x30, bytes.fold(byteArrayOf()) { a, b -> a + b })
        val algorithm = seq(der(6, byteArrayOf(0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 4, 3, 2)))
        val name = X500Principal("CN=ArchiMan local portal").encoded
        // RFC 5280: UTCTime (0x17) for dates through 2049.
        val validity = seq(der(0x17, "260101000000Z".toByteArray()), der(0x17, "360101000000Z".toByteArray()))
        val san = seq(der(6, byteArrayOf(0x55, 0x1d, 0x11)), der(4, seq(der(0x87, InetAddress.getByName(address).address))))
        val basicConstraints = seq(
            der(6, byteArrayOf(0x55, 0x1d, 0x13)),
            der(1, byteArrayOf(0xff.toByte())),
            der(4, seq()),
        )
        val keyUsage = seq(
            der(6, byteArrayOf(0x55, 0x1d, 0x0f)),
            der(1, byteArrayOf(0xff.toByte())),
            der(4, der(3, byteArrayOf(0x07, 0x80.toByte()))),
        )
        val serial = BigInteger(1, MessageDigest.getInstance("SHA-256").digest(publicKey.encoded + address.toByteArray()).copyOf(16))
        val body = seq(
            der(0xa0, der(2, byteArrayOf(2))),
            der(2, serial.toByteArray()),
            algorithm,
            name,
            validity,
            name,
            publicKey.encoded,
            der(0xa3, seq(san, basicConstraints, keyUsage)),
        )
        val signature = signLeaf(key, body)
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(seq(body, algorithm, der(3, byteArrayOf(0) + signature)))) as X509Certificate
        cert.verify(publicKey)
        certFile.writeBytes(cert.encoded)
        ipFile.writeText(address)
        return cert
    }

    private fun signLeaf(key: PrivateKey, body: ByteArray): ByteArray {
        val engines = listOf(
            { Signature.getInstance("SHA256withECDSA", "AndroidKeyStore") },
            { Signature.getInstance("SHA256withECDSA") },
        )
        var last: Exception? = null
        for (engine in engines) {
            try {
                return engine().run {
                    initSign(key)
                    update(body)
                    sign()
                }
            } catch (error: Exception) {
                last = error
            }
        }
        throw last ?: IllegalStateException("Could not sign the portal certificate.")
    }

    private fun hasIpSan(certificate: X509Certificate, address: String): Boolean {
        return certificate.subjectAlternativeNames.orEmpty().any { entry ->
            entry.size >= 2 && entry[0] == 7 && entry[1]?.toString() == address
        }
    }
}
