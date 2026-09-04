package com.example.domain.drawing

import android.net.Uri

data class DrawingViewerCapabilities(
    val canRenderDwg: Boolean,
    val canReadLayers: Boolean,
    val canMeasure: Boolean,
    val canAnnotate: Boolean,
    val engineName: String
)

sealed interface DrawingOpenResult {
    data class Opened(val sessionId: String) : DrawingOpenResult
    data class Unsupported(val reason: String) : DrawingOpenResult
    data class Failed(val message: String) : DrawingOpenResult
}

/**
 * Vendor-neutral boundary for an offline native DWG renderer.
 * Project records, revisions and markups must never depend on a vendor SDK type.
 */
interface DrawingViewerEngine {
    val capabilities: DrawingViewerCapabilities
    suspend fun open(source: Uri): DrawingOpenResult
    suspend fun close()
}

object UnavailableDrawingViewerEngine : DrawingViewerEngine {
    override val capabilities = DrawingViewerCapabilities(
        canRenderDwg = false,
        canReadLayers = false,
        canMeasure = false,
        canAnnotate = false,
        engineName = "Not configured"
    )

    override suspend fun open(source: Uri): DrawingOpenResult = DrawingOpenResult.Unsupported(
        "A native DWG engine must be licensed and configured for this build."
    )

    override suspend fun close() = Unit
}
