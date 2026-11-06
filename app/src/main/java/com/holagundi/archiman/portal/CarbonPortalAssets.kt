package com.holagundi.archiman.portal

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/** Exact public static-file allowlist. Practice data never enters this cache. */
object CarbonPortalAssets {
    val mime=mapOf("vendor.js" to "application/javascript; charset=utf-8","vendor.css" to "text/css; charset=utf-8",
        "ibm_plex_sans_regular.ttf" to "font/ttf","ibm_plex_sans_medium.ttf" to "font/ttf","ibm_plex_sans_semibold.ttf" to "font/ttf")
    private val cache=ConcurrentHashMap<String,ByteArray>()
    fun read(context:Context,name:String):ByteArray {
        require(name in mime)
        return cache.getOrPut(name){context.assets.open("carbon/$name").use{com.holagundi.archiman.domain.PasswordEnvelope.readBounded(it,4*1024*1024)}}
    }
}
