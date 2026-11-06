package com.holagundi.archiman

import com.holagundi.archiman.portal.PortalServer
import com.holagundi.archiman.portal.PortalHttp
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*

object PinnedPortalClient {
    fun socket(server: PortalServer): SSLSocket {
        val expected = server.state.value.fingerprint
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = error("Not a client certificate verifier")
            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
                val actual = MessageDigest.getInstance("SHA-256").digest(chain.single().encoded).joinToString(":") { "%02X".format(it.toInt() and 255) }
                check(actual == expected) { "Certificate fingerprint mismatch" }; chain.single().checkValidity(); chain.single().verify(chain.single().publicKey)
            }
        }
        val tls = SSLContext.getInstance("TLS").apply { init(null,arrayOf(trust),null) }
        return (tls.socketFactory.createSocket("127.0.0.1",PortalHttp.PORT) as SSLSocket).apply {
            soTimeout = 10_000
            sslParameters = sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
            startHandshake()
            check(session.protocol in listOf("TLSv1.2","TLSv1.3"))
        }
    }
}
