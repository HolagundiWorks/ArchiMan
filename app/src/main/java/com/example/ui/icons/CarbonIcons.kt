package com.example.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

/**
 * Official Carbon Design System 32 px icon geometry.
 * Source: @carbon/icons-react 11.87.0 (Apache-2.0).
 */
object CarbonIcons {
    private val cache = ConcurrentHashMap<String, ImageVector>()
    private val mirrored = setOf("ArrowLeft", "ArrowRight", "ArrowsHorizontal", "Undo")
    private val definitions = mapOf(
        "ArrowLeft" to listOf("M14 26 15.41 24.59 7.83 17 28 17 28 15 7.83 15 15.41 7.41 14 6 4 16 14 26z"),
        "ArrowRight" to listOf("M18 6 16.57 7.393 24.15 15 4 15 4 17 24.15 17 16.57 24.573 18 26 28 16 18 6z"),
        "Task" to listOf("M14 20.18 10.41 16.59 9 18 14 23 23 14 21.59 12.58 14 20.18z", "M25,5H22V4a2,2,0,0,0-2-2H12a2,2,0,0,0-2,2V5H7A2,2,0,0,0,5,7V28a2,2,0,0,0,2,2H25a2,2,0,0,0,2-2V7A2,2,0,0,0,25,5ZM12,4h8V8H12ZM25,28H7V7h3v3H22V7h3Z"),
        "Calendar" to listOf("M26,4h-4V2h-2v2h-8V2h-2v2H6C4.9,4,4,4.9,4,6v20c0,1.1,0.9,2,2,2h20c1.1,0,2-0.9,2-2V6C28,4.9,27.1,4,26,4z M26,26H6V12h20 V26z M26,10H6V6h4v2h2V6h8v2h2V6h4V10z"),
        "Book" to listOf("M19 10H26V12H19z", "M19 15H26V17H19z", "M19 20H26V22H19z", "M6 10H13V12H6z", "M6 15H13V17H6z", "M6 20H13V22H6z", "M28,5H4A2.002,2.002,0,0,0,2,7V25a2.002,2.002,0,0,0,2,2H28a2.002,2.002,0,0,0,2-2V7A2.002,2.002,0,0,0,28,5ZM4,7H15V25H4ZM17,25V7H28V25Z"),
        "Launch" to listOf("M26,28H6a2.0027,2.0027,0,0,1-2-2V6A2.0027,2.0027,0,0,1,6,4H16V6H6V26H26V16h2V26A2.0027,2.0027,0,0,1,26,28Z", "M20 2 20 4 26.586 4 18 12.586 19.414 14 28 5.414 28 12 30 12 30 2 20 2z"),
        "Rule" to listOf("M10 16H22V18H10z", "M10 10H22V12H10z", "M16,30,9.8242,26.7071A10.9815,10.9815,0,0,1,4,17V4A2.0022,2.0022,0,0,1,6,2H26a2.0022,2.0022,0,0,1,2,2V17a10.9815,10.9815,0,0,1-5.8242,9.7069ZM6,4V17a8.9852,8.9852,0,0,0,4.7656,7.9423L16,27.7333l5.2344-2.791A8.9852,8.9852,0,0,0,26,17V4Z"),
        "Undo" to listOf("M20,10H7.8149l3.5874-3.5859L10,5,4,11,10,17l1.4023-1.4146L7.8179,12H20a6,6,0,0,1,0,12H12v2h8a8,8,0,0,0,0-16Z"),
        "TreeViewAlt" to listOf("M23,9h6a2,2,0,0,0,2-2V3a2,2,0,0,0-2-2H23a2,2,0,0,0-2,2V4H11V3A2,2,0,0,0,9,1H3A2,2,0,0,0,1,3V7A2,2,0,0,0,3,9H9a2,2,0,0,0,2-2V6h4V26a2.0023,2.0023,0,0,0,2,2h4v1a2,2,0,0,0,2,2h6a2,2,0,0,0,2-2V25a2,2,0,0,0-2-2H23a2,2,0,0,0-2,2v1H17V17h4v1a2,2,0,0,0,2,2h6a2,2,0,0,0,2-2V14a2,2,0,0,0-2-2H23a2,2,0,0,0-2,2v1H17V6h4V7A2,2,0,0,0,23,9Zm0-6h6V7H23ZM9,7H3V3H9ZM23,25h6v4H23Zm0-11h6v4H23Z"),
        "Add" to listOf("M17 15 17 8 15 8 15 15 8 15 8 17 15 17 15 24 17 24 17 17 24 17 24 15z"),
        "Camera" to listOf("M29,26H3a1,1,0,0,1-1-1V8A1,1,0,0,1,3,7H9.46l1.71-2.55A1,1,0,0,1,12,4h8a1,1,0,0,1,.83.45L22.54,7H29a1,1,0,0,1,1,1V25A1,1,0,0,1,29,26ZM4,24H28V9H22a1,1,0,0,1-.83-.45L19.46,6H12.54L10.83,8.55A1,1,0,0,1,10,9H4Z", "M16,22a6,6,0,1,1,6-6A6,6,0,0,1,16,22Zm0-10a4,4,0,1,0,4,4A4,4,0,0,0,16,12Z"),
        "AddFilled" to listOf("M16,2A14.1725,14.1725,0,0,0,2,16,14.1725,14.1725,0,0,0,16,30,14.1725,14.1725,0,0,0,30,16,14.1725,14.1725,0,0,0,16,2Zm8,15H17v7H15V17H8V15h7V8h2v7h7Z"),
        "Link" to listOf("M29.25,6.76a6,6,0,0,0-8.5,0l1.42,1.42a4,4,0,1,1,5.67,5.67l-8,8a4,4,0,1,1-5.67-5.66l1.41-1.42-1.41-1.42-1.42,1.42a6,6,0,0,0,0,8.5A6,6,0,0,0,17,25a6,6,0,0,0,4.27-1.76l8-8A6,6,0,0,0,29.25,6.76Z", "M4.19,24.82a4,4,0,0,1,0-5.67l8-8a4,4,0,0,1,5.67,0A3.94,3.94,0,0,1,19,14a4,4,0,0,1-1.17,2.85L15.71,19l1.42,1.42,2.12-2.12a6,6,0,0,0-8.51-8.51l-8,8a6,6,0,0,0,0,8.51A6,6,0,0,0,7,28a6.07,6.07,0,0,0,4.28-1.76L9.86,24.82A4,4,0,0,1,4.19,24.82Z"),
        "Building" to listOf("M28,2H16a2.002,2.002,0,0,0-2,2V14H4a2.002,2.002,0,0,0-2,2V30H30V4A2.0023,2.0023,0,0,0,28,2ZM9,28V21h4v7Zm19,0H15V20a1,1,0,0,0-1-1H8a1,1,0,0,0-1,1v8H4V16H16V4H28Z", "M18 8H20V10H18z", "M24 8H26V10H24z", "M18 14H20V16H18z", "M24 14H26V16H24z", "M18 20H20V22H18z", "M24 20H26V22H24z"),
        "Construction" to listOf("M29.34,16.06a1.0007,1.0007,0,0,0-1.1084.3L24.46,20.8857l-5.4355-.9882-3.602-8.9512A3.014,3.014,0,0,0,12.6138,9h-4.06A3.0018,3.0018,0,0,0,7.01,9.4277L2,12.4336v6.4009l5,.9092V30H9V20.1074l3.5652.648L14,24.2V30h2V23.8l-1.0911-2.6182L22.99,22.6509,18.2319,28.36A1,1,0,0,0,19,30H29a1,1,0,0,0,1-1V17A1,1,0,0,0,29.34,16.06ZM4,17.1655V13.5664l3-1.8v5.9448Zm5,.9092V11h3.6138a1.0141,1.0141,0,0,1,.9453.6709l3.14,7.8037ZM28,28H21.1353L28,19.7617Z", "M12.5,8A3.5,3.5,0,1,1,16,4.5,3.5042,3.5042,0,0,1,12.5,8Zm0-5A1.5,1.5,0,1,0,14,4.5,1.5017,1.5017,0,0,0,12.5,3Z"),
        "Archive" to listOf("M14 19H18V21H14z", "M6,2V28a2,2,0,0,0,2,2H24a2,2,0,0,0,2-2V2ZM24,28H8V16H24Zm0-14H8V10H24ZM8,8V4H24V8Z"),
        "ChevronDown" to listOf("M16 22 6 12 7.4 10.6 16 19.2 24.6 10.6 26 12z"),
        "CheckmarkFilled" to listOf("M16,2A14,14,0,1,0,30,16,14,14,0,0,0,16,2ZM14,21.5908l-5-5L10.5906,15,14,18.4092,21.41,11l1.5957,1.5859Z"),
        "Attachment" to listOf("M28.1,18.9L13.1,3.9c-2.5-2.6-6.6-2.6-9.2-0.1S1.3,10.5,3.9,13c0,0,0.1,0.1,0.1,0.1L6.8,16l1.4-1.4l-2.9-2.9 C3.6,10,3.6,7.1,5.3,5.4s4.6-1.8,6.3-0.1c0,0,0,0,0.1,0.1l14.9,14.9c1.8,1.7,1.8,4.6,0.1,6.3c-1.7,1.8-4.6,1.8-6.3,0.1 c0,0,0,0-0.1-0.1l-7.4-7.4c-1-1-0.9-2.6,0-3.5c1-0.9,2.5-0.9,3.5,0l4.1,4.1l1.4-1.4c0,0-4.2-4.2-4.2-4.2c-1.8-1.7-4.6-1.6-6.3,0.2 c-1.6,1.7-1.6,4.4,0,6.2l7.5,7.5c2.5,2.6,6.6,2.6,9.2,0.1S30.7,21.5,28.1,18.9C28.1,19,28.1,18.9,28.1,18.9L28.1,18.9z"),
        "MagicWand" to listOf("M29.4141,24,12,6.5859a2.0476,2.0476,0,0,0-2.8281,0l-2.586,2.586a2.0021,2.0021,0,0,0,0,2.8281L23.999,29.4141a2.0024,2.0024,0,0,0,2.8281,0l2.587-2.5865a1.9993,1.9993,0,0,0,0-2.8281ZM8,10.5859,10.5859,8l5,5-2.5866,2.5869-5-5ZM25.4131,28l-11-10.999L17,14.4141l11,11Z", "M2.586 14.586H5.414V17.414H2.586z", "M14.586 2.586H17.414V5.414H14.586z", "M2.586 2.586H5.414V5.414H2.586z"),
        "Tools" to listOf("M12.1,2A9.8,9.8,0,0,0,6.7,3.6L13.1,10a2.1,2.1,0,0,1,.2,3,2.1,2.1,0,0,1-3-.2L3.7,6.4A9.84,9.84,0,0,0,2,12.1,10.14,10.14,0,0,0,12.1,22.2a10.9,10.9,0,0,0,2.6-.3l6.7,6.7a5,5,0,0,0,7.1-7.1l-6.7-6.7a10.9,10.9,0,0,0,.3-2.6A10,10,0,0,0,12.1,2Zm8,10.1a7.61,7.61,0,0,1-.3,2.1l-.3,1.1.8.8L27,22.8a2.88,2.88,0,0,1,.9,2.1A2.72,2.72,0,0,1,27,27a2.9,2.9,0,0,1-4.2,0l-6.7-6.7-.8-.8-1.1.3a7.61,7.61,0,0,1-2.1.3,8.27,8.27,0,0,1-5.7-2.3A7.63,7.63,0,0,1,4,12.1a8.33,8.33,0,0,1,.3-2.2l4.4,4.4a4.14,4.14,0,0,0,5.9.2,4.14,4.14,0,0,0-.2-5.9L10,4.2a6.45,6.45,0,0,1,2-.3,8.27,8.27,0,0,1,5.7,2.3A8.49,8.49,0,0,1,20.1,12.1Z"),
        "Category" to listOf("M27,22.1414V18a2,2,0,0,0-2-2H17V12h2a2.0023,2.0023,0,0,0,2-2V4a2.0023,2.0023,0,0,0-2-2H13a2.002,2.002,0,0,0-2,2v6a2.002,2.002,0,0,0,2,2h2v4H7a2,2,0,0,0-2,2v4.1421a4,4,0,1,0,2,0V18h8v4.142a4,4,0,1,0,2,0V18h8v4.1414a4,4,0,1,0,2,0ZM13,4h6l.001,6H13ZM8,26a2,2,0,1,1-2-2A2.0023,2.0023,0,0,1,8,26Zm10,0a2,2,0,1,1-2-2A2.0027,2.0027,0,0,1,18,26Zm8,2a2,2,0,1,1,2-2A2.0023,2.0023,0,0,1,26,28Z"),
        "Checkmark" to listOf("M13 24 4 15 5.414 13.586 13 21.171 26.586 7.586 28 9 13 24z"),
        "Close" to listOf("M17.4141 16 24 9.4141 22.5859 8 16 14.5859 9.4143 8 8 9.4141 14.5859 16 8 22.5859 9.4143 24 16 17.4141 22.5859 24 24 22.5859 17.4141 16z"),
        "User" to listOf("M16,4a5,5,0,1,1-5,5,5,5,0,0,1,5-5m0-2a7,7,0,1,0,7,7A7,7,0,0,0,16,2Z", "M26,30H24V25a5,5,0,0,0-5-5H13a5,5,0,0,0-5,5v5H6V25a7,7,0,0,1,7-7h6a7,7,0,0,1,7,7Z"),
        "Copy" to listOf("M28,10V28H10V10H28m0-2H10a2,2,0,0,0-2,2V28a2,2,0,0,0,2,2H28a2,2,0,0,0,2-2V10a2,2,0,0,0-2-2Z", "M4,18H2V4A2,2,0,0,1,4,2H18V4H4Z"),
        "CenterSquare" to listOf("M6 12 4 12 4 4 12 4 12 6 6 6 6 12z", "M28 12 26 12 26 6 20 6 20 4 28 4 28 12z", "M12 28 4 28 4 20 6 20 6 26 12 26 12 28z", "M28 28 20 28 20 26 26 26 26 20 28 20 28 28z", "M15 10H17V14H15z", "M10 15H14V17H10z", "M18 15H22V17H18z", "M15 18H17V22H15z"),
        "Dashboard" to listOf("M24 21H26V26H24z", "M20 16H22V26H20z", "M11,26a5.0059,5.0059,0,0,1-5-5H8a3,3,0,1,0,3-3V16a5,5,0,0,1,0,10Z", "M28,2H4A2.002,2.002,0,0,0,2,4V28a2.0023,2.0023,0,0,0,2,2H28a2.0027,2.0027,0,0,0,2-2V4A2.0023,2.0023,0,0,0,28,2Zm0,9H14V4H28ZM12,4v7H4V4ZM4,28V13H28.0007l.0013,15Z"),
        "TrashCan" to listOf("M12 12H14V24H12z", "M18 12H20V24H18z", "M4,6V8H6V28a2,2,0,0,0,2,2H24a2,2,0,0,0,2-2V8h2V6ZM8,28V8H24V28Z", "M12 2H20V4H12z"),
        "Document" to listOf("M25.7,9.3l-7-7C18.5,2.1,18.3,2,18,2H8C6.9,2,6,2.9,6,4v24c0,1.1,0.9,2,2,2h16c1.1,0,2-0.9,2-2V10C26,9.7,25.9,9.5,25.7,9.3 z M18,4.4l5.6,5.6H18V4.4z M24,28H8V4h8v6c0,1.1,0.9,2,2,2h6V28z", "M10 22H22V24H10z", "M10 16H22V18H10z"),
        "Floorplan" to listOf("M28,2H4C2.9,2,2,2.9,2,4v24c0,1.1,0.9,2,2,2h15v-2c0-2.8,2.2-5,5-5v-2c-3.9,0-7,3.1-7,7h-3v-4h-2v4H4V4h8v14h2v-5h4v-2h-4V4 h14v7h-4v2h4v15h-4v2h4c1.1,0,2-0.9,2-2V4C30,2.9,29.1,2,28,2z"),
        "Download" to listOf("M26,24v4H6V24H4v4H4a2,2,0,0,0,2,2H26a2,2,0,0,0,2-2h0V24Z", "M26 14 24.59 12.59 17 20.17 17 2 15 2 15 20.17 7.41 12.59 6 14 16 24 26 14z"),
        "Edit" to listOf("M2 26H30V28H2z", "M25.4,9c0.8-0.8,0.8-2,0-2.8c0,0,0,0,0,0l-3.6-3.6c-0.8-0.8-2-0.8-2.8,0c0,0,0,0,0,0l-15,15V24h6.4L25.4,9z M20.4,4L24,7.6 l-3,3L17.4,7L20.4,4z M6,22v-3.6l10-10l3.6,3.6l-10,10H6z"),
        "ChevronUp" to listOf("M16 10 26 20 24.6 21.4 16 12.8 7.4 21.4 6 20z"),
        "FolderOpen" to listOf("M28,8H20.8284L17.4143,4.5859A2,2,0,0,0,16,4H4A2,2,0,0,0,2,6V26a2,2,0,0,0,2,2H28a2,2,0,0,0,2-2V10A2,2,0,0,0,28,8ZM8,26V14h8v6.17l-2.59-2.58L12,19l5,5,5-5-1.41-1.41L18,20.17V14a2.0025,2.0025,0,0,0-2-2H8a2.0025,2.0025,0,0,0-2,2V26H4V6H16l4,4h8v2H22v2h6V26Z"),
        "PaintBrush" to listOf("M28.83,23.17,23,17.33V13a1,1,0,0,0-.29-.71l-10-10a1,1,0,0,0-1.42,0l-9,9a1,1,0,0,0,0,1.42l10,10A1,1,0,0,0,13,23h4.34l5.83,5.84a4,4,0,0,0,5.66-5.66ZM6,10.41l2.29,2.3,1.42-1.42L7.41,9,9,7.41l4.29,4.3,1.42-1.42L10.41,6,12,4.41,18.59,11,11,18.59,4.41,12Zm21.41,17a2,2,0,0,1-2.82,0l-6.13-6.12a1.8,1.8,0,0,0-.71-.29H13.41l-1-1L20,12.41l1,1v4.34a1,1,0,0,0,.29.7l6.12,6.14a2,2,0,0,1,0,2.82Z"),
        "Policy" to listOf("M30,18A6,6,0,1,0,20,22.46v7.54l4-1.8926,4,1.8926V22.46A5.98,5.98,0,0,0,30,18Zm-4,8.84-2-.9467L22,26.84V23.65a5.8877,5.8877,0,0,0,4,0ZM24,22a4,4,0,1,1,4-4A4.0045,4.0045,0,0,1,24,22Z", "M9 14H16V16H9z", "M9 8H19V10H9z", "M6,30a2.0021,2.0021,0,0,1-2-2V4A2.0021,2.0021,0,0,1,6,2H22a2.0021,2.0021,0,0,1,2,2V8H22V4H6V28H16v2Z"),
        "Table" to listOf("M29,5a2,2,0,0,0-2-2H5A2,2,0,0,0,3,5V27a2,2,0,0,0,2,2H27a2,2,0,0,0,2-2ZM27,5V9H5V5Zm0,22H5V23H27Zm0-6H5V17H27Zm0-6H5V11H27Z"),
        "Group" to listOf("M31,30H29V27a3,3,0,0,0-3-3H22a3,3,0,0,0-3,3v3H17V27a5,5,0,0,1,5-5h4a5,5,0,0,1,5,5Z", "M24,12a3,3,0,1,1-3,3,3,3,0,0,1,3-3m0-2a5,5,0,1,0,5,5A5,5,0,0,0,24,10Z", "M15,22H13V19a3,3,0,0,0-3-3H6a3,3,0,0,0-3,3v3H1V19a5,5,0,0,1,5-5h4a5,5,0,0,1,5,5Z", "M8,4A3,3,0,1,1,5,7,3,3,0,0,1,8,4M8,2a5,5,0,1,0,5,5A5,5,0,0,0,8,2Z"),
        "Time" to listOf("M16,30A14,14,0,1,1,30,16,14,14,0,0,1,16,30ZM16,4A12,12,0,1,0,28,16,12,12,0,0,0,16,4Z", "M20.59 22 15 16.41 15 7 17 7 17 15.58 22 20.59 20.59 22z"),
        "Home" to listOf("M16.6123,2.2138a1.01,1.01,0,0,0-1.2427,0L1,13.4194l1.2427,1.5717L4,13.6209V26a2.0041,2.0041,0,0,0,2,2H26a2.0037,2.0037,0,0,0,2-2V13.63L29.7573,15,31,13.4282ZM18,26H14V18h4Zm2,0V18a2.0023,2.0023,0,0,0-2-2H14a2.002,2.002,0,0,0-2,2v8H6V12.0615l10-7.79,10,7.8005V26Z"),
        "Information" to listOf("M17 22 17 14 13 14 13 16 15 16 15 22 12 22 12 24 20 24 20 22 17 22z", "M16,8a1.5,1.5,0,1,0,1.5,1.5A1.5,1.5,0,0,0,16,8Z", "M16,30A14,14,0,1,1,30,16,14,14,0,0,1,16,30ZM16,4A12,12,0,1,0,28,16,12,12,0,0,0,16,4Z"),
        "Share" to listOf("M23,20a5,5,0,0,0-3.89,1.89L11.8,17.32a4.46,4.46,0,0,0,0-2.64l7.31-4.57A5,5,0,1,0,18,7a4.79,4.79,0,0,0,.2,1.32l-7.31,4.57a5,5,0,1,0,0,6.22l7.31,4.57A4.79,4.79,0,0,0,18,25a5,5,0,1,0,5-5ZM23,4a3,3,0,1,1-3,3A3,3,0,0,1,23,4ZM7,19a3,3,0,1,1,3-3A3,3,0,0,1,7,19Zm16,9a3,3,0,1,1,3-3A3,3,0,0,1,23,28Z"),
        "Layers" to listOf("M16,24a.9967.9967,0,0,1-.4741-.12l-13-7L3.4741,15.12,16,21.8643,28.5259,15.12l.9482,1.7607-13,7A.9967.9967,0,0,1,16,24Z", "M16,30a.9967.9967,0,0,1-.4741-.12l-13-7L3.4741,21.12,16,27.8643,28.5259,21.12l.9482,1.7607-13,7A.9967.9967,0,0,1,16,30Z", "M16,18a.9967.9967,0,0,1-.4741-.12l-13-7a1,1,0,0,1,0-1.7607l13-7a.9982.9982,0,0,1,.9482,0l13,7a1,1,0,0,1,0,1.7607l-13,7A.9967.9967,0,0,1,16,18ZM5.1094,10,16,15.8643,26.8906,10,16,4.1358Z"),
        "Location" to listOf("M16,18a5,5,0,1,1,5-5A5.0057,5.0057,0,0,1,16,18Zm0-8a3,3,0,1,0,3,3A3.0033,3.0033,0,0,0,16,10Z", "M16,30,7.5645,20.0513c-.0479-.0571-.3482-.4515-.3482-.4515A10.8888,10.8888,0,0,1,5,13a11,11,0,0,1,22,0,10.8844,10.8844,0,0,1-2.2148,6.5973l-.0015.0025s-.3.3944-.3447.4474ZM8.8125,18.395c.001.0007.2334.3082.2866.3744L16,26.9079l6.91-8.15c.0439-.0552.2783-.3649.2788-.3657A8.901,8.901,0,0,0,25,13,9,9,0,1,0,7,13a8.9054,8.9054,0,0,0,1.8125,5.395Z"),
        "OverflowMenuHorizontal" to listOf("M 6 16 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0", "M 14 16 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0", "M 22 16 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"),
        "OverflowMenuVertical" to listOf("M 14 8 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0", "M 14 16 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0", "M 14 24 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"),
        "Phone" to listOf("M26,29h-.17C6.18,27.87,3.39,11.29,3,6.23A3,3,0,0,1,5.76,3h5.51a2,2,0,0,1,1.86,1.26L14.65,8a2,2,0,0,1-.44,2.16l-2.13,2.15a9.37,9.37,0,0,0,7.58,7.6l2.17-2.15A2,2,0,0,1,24,17.35l3.77,1.51A2,2,0,0,1,29,20.72V26A3,3,0,0,1,26,29ZM6,5A1,1,0,0,0,5,6v.08C5.46,12,8.41,26,25.94,27A1,1,0,0,0,27,26.06V20.72l-3.77-1.51-2.87,2.85L19.88,22C11.18,20.91,10,12.21,10,12.12l-.06-.48,2.84-2.87L11.28,5Z"),
        "Image" to listOf("M19,14a3,3,0,1,0-3-3A3,3,0,0,0,19,14Zm0-4a1,1,0,1,1-1,1A1,1,0,0,1,19,10Z", "M26,4H6A2,2,0,0,0,4,6V26a2,2,0,0,0,2,2H26a2,2,0,0,0,2-2V6A2,2,0,0,0,26,4Zm0,22H6V20l5-5,5.59,5.59a2,2,0,0,0,2.82,0L21,19l5,5Zm0-4.83-3.59-3.59a2,2,0,0,0-2.82,0L18,19.17l-5.59-5.59a2,2,0,0,0-2.82,0L6,17.17V6H26Z"),
        "DocumentPdf" to listOf("M30 18 30 16 24 16 24 26 26 26 26 22 29 22 29 20 26 20 26 18 30 18z", "M19,26H15V16h4a3.0033,3.0033,0,0,1,3,3v4A3.0033,3.0033,0,0,1,19,26Zm-2-2h2a1.0011,1.0011,0,0,0,1-1V19a1.0011,1.0011,0,0,0-1-1H17Z", "M11,16H6V26H8V23h3a2.0027,2.0027,0,0,0,2-2V18A2.0023,2.0023,0,0,0,11,16ZM8,21V18h3l.001,3Z", "M22,14V10a.9092.9092,0,0,0-.3-.7l-7-7A.9087.9087,0,0,0,14,2H4A2.0059,2.0059,0,0,0,2,4V28a2,2,0,0,0,2,2H20V28H4V4h8v6a2.0059,2.0059,0,0,0,2,2h6v2Zm-8-4V4.4L19.6,10Z"),
        "PlayFilled" to listOf("M11,23a1,1,0,0,1-1-1V10a1,1,0,0,1,1.4473-.8945l12,6a1,1,0,0,1,0,1.789l-12,6A1.001,1.001,0,0,1,11,23Z", "M16,2A14,14,0,1,0,30,16,14,14,0,0,0,16,2Zm7.4473,14.8945-12,6A1,1,0,0,1,10,22V10a1,1,0,0,1,1.4473-.8945l12,6a1,1,0,0,1,0,1.789Z"),
        "Printer" to listOf("M28,9H25V3H7V9H4a2,2,0,0,0-2,2V21a2,2,0,0,0,2,2H7v6H25V23h3a2,2,0,0,0,2-2V11A2,2,0,0,0,28,9ZM9,5H23V9H9ZM23,27H9V17H23Zm5-6H25V15H7v6H4V11H28Z"),
        "Subtract" to listOf("M8 15H24V17H8z"),
        "Warning" to listOf("M16,2A14,14,0,1,0,30,16,14,14,0,0,0,16,2Zm0,26A12,12,0,1,1,28,16,12,12,0,0,1,16,28Z", "M15 8H17V19H15z", "M16,22a1.5,1.5,0,1,0,1.5,1.5A1.5,1.5,0,0,0,16,22Z"),
        "Search" to listOf("M29,27.5859l-7.5521-7.5521a11.0177,11.0177,0,1,0-1.4141,1.4141L27.5859,29ZM4,13a9,9,0,1,1,9,9A9.01,9.01,0,0,1,4,13Z"),
        "Ruler" to listOf("M29,10H3a1,1,0,0,0-1,1V21a1,1,0,0,0,1,1H29a1,1,0,0,0,1-1V11A1,1,0,0,0,29,10ZM28,20H4V12H8v4h2V12h5v4h2V12h5v4h2V12h4Z"),
        "ArrowsHorizontal" to listOf("M11.41 26.59 7.83 23 28 23 28 21 7.83 21 11.41 17.41 10 16 4 22 10 28 11.41 26.59z", "M28 10 22 4 20.59 5.41 24.17 9 4 9 4 11 24.17 11 20.59 14.59 22 16 28 10z"),
        "Upload" to listOf("M6 18 7.41 19.41 15 11.83 15 30 17 30 17 11.83 24.59 19.41 26 18 16 8 6 18z", "M6,8V4H26V8h2V4a2,2,0,0,0-2-2H6A2,2,0,0,0,4,4V8Z"),
        "RainDrop" to listOf("M16,24V22a3.2965,3.2965,0,0,0,3-3h2A5.2668,5.2668,0,0,1,16,24Z", "M16,28a9.0114,9.0114,0,0,1-9-9,9.9843,9.9843,0,0,1,1.4941-4.9554L15.1528,3.4367a1.04,1.04,0,0,1,1.6944,0l6.6289,10.5564A10.0633,10.0633,0,0,1,25,19,9.0114,9.0114,0,0,1,16,28ZM16,5.8483l-5.7817,9.2079A7.9771,7.9771,0,0,0,9,19a7,7,0,0,0,14,0,8.0615,8.0615,0,0,0-1.248-3.9953Z"),
        "Wifi" to listOf("M10.47,19.2334l1.4136,1.4131a5.9688,5.9688,0,0,1,8.2229-.0093L21.52,19.2236a7.9629,7.9629,0,0,0-11.05.01Z", "M6.229,14.9927l1.4136,1.4135a11.955,11.955,0,0,1,16.7041-.01L25.76,14.9829a13.9514,13.9514,0,0,0-19.5313.01Z", "M30,10.7412a19.94,19.94,0,0,0-28,0v.0225L3.4043,12.168a17.9336,17.9336,0,0,1,25.1811-.01L30,10.7432Z", "M 14 25 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"),
        "WifiOff" to listOf("M30,3.4141,28.5859,2,2,28.5859,3.4141,30,14.0962,19.3179a5.9359,5.9359,0,0,1,6.01,1.3193L21.52,19.2236a7.9669,7.9669,0,0,0-5.125-2.2041l3.3875-3.3877a11.9908,11.9908,0,0,1,4.5647,2.7647L25.76,14.9829A13.975,13.975,0,0,0,21.334,12.08L24.3308,9.083a17.9364,17.9364,0,0,1,4.2546,3.0747L30,10.7432v-.002a20.02,20.02,0,0,0-4.1895-3.1377Z", "M14.68,13.0776l2.0415-2.0415C16.481,11.0234,16.2437,11,16,11a13.9447,13.9447,0,0,0-9.771,3.9927l1.4136,1.4136A11.97,11.97,0,0,1,14.68,13.0776Z", "M16,7a17.87,17.87,0,0,1,4.2324.5254L21.875,5.8828A19.9537,19.9537,0,0,0,2,10.7412v.0225L3.4043,12.168A17.9193,17.9193,0,0,1,16,7Z", "M 14 25 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"),
    )

    private fun icon(name: String): ImageVector = cache.getOrPut(name) {
        ImageVector.Builder(
            name = "Carbon.$name",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
            autoMirror = name in mirrored
        ).apply {
            definitions.getValue(name).forEach { path ->
                addPath(
                    pathData = PathParser().parsePathString(path).toNodes(),
                    fill = SolidColor(Color.Black)
                )
            }
        }.build()
    }

    val ArrowBack: ImageVector get() = icon("ArrowLeft")
    val ArrowForward: ImageVector get() = icon("ArrowRight")
    val ArrowForwardIos: ImageVector get() = icon("ArrowRight")
    val Assignment: ImageVector get() = icon("Task")
    val EventNote: ImageVector get() = icon("Calendar")
    val FactCheck: ImageVector get() = icon("Task")
    val MenuBook: ImageVector get() = icon("Book")
    val OpenInNew: ImageVector get() = icon("Launch")
    val Rule: ImageVector get() = icon("Rule")
    val Undo: ImageVector get() = icon("Undo")
    val AccountTree: ImageVector get() = icon("TreeViewAlt")
    val Add: ImageVector get() = icon("Add")
    val AddAPhoto: ImageVector get() = icon("Camera")
    val AddCircle: ImageVector get() = icon("AddFilled")
    val AddCircleOutline: ImageVector get() = icon("AddFilled")
    val AddLink: ImageVector get() = icon("Link")
    val Apartment: ImageVector get() = icon("Building")
    val Architecture: ImageVector get() = icon("Construction")
    val Archive: ImageVector get() = icon("Archive")
    val ArrowDropDown: ImageVector get() = icon("ChevronDown")
    val AssignmentTurnedIn: ImageVector get() = icon("CheckmarkFilled")
    val AttachFile: ImageVector get() = icon("Attachment")
    val AutoFixHigh: ImageVector get() = icon("MagicWand")
    val BuildCircle: ImageVector get() = icon("Tools")
    val Business: ImageVector get() = icon("Building")
    val Category: ImageVector get() = icon("Category")
    val Check: ImageVector get() = icon("Checkmark")
    val CheckCircle: ImageVector get() = icon("CheckmarkFilled")
    val Checklist: ImageVector get() = icon("Task")
    val ChevronRight: ImageVector get() = icon("ArrowRight")
    val Clear: ImageVector get() = icon("Close")
    val Close: ImageVector get() = icon("Close")
    val ContactPage: ImageVector get() = icon("User")
    val ContentCopy: ImageVector get() = icon("Copy")
    val CorporateFare: ImageVector get() = icon("Building")
    val CropFree: ImageVector get() = icon("CenterSquare")
    val Dashboard: ImageVector get() = icon("Dashboard")
    val Delete: ImageVector get() = icon("TrashCan")
    val DeleteOutline: ImageVector get() = icon("TrashCan")
    val Description: ImageVector get() = icon("Document")
    val Domain: ImageVector get() = icon("Building")
    val DoorFront: ImageVector get() = icon("Floorplan")
    val Download: ImageVector get() = icon("Download")
    val Edit: ImageVector get() = icon("Edit")
    val Engineering: ImageVector get() = icon("Construction")
    val Event: ImageVector get() = icon("Calendar")
    val ExpandLess: ImageVector get() = icon("ChevronUp")
    val ExpandMore: ImageVector get() = icon("ChevronDown")
    val FileDownload: ImageVector get() = icon("Download")
    val FolderOpen: ImageVector get() = icon("FolderOpen")
    val FormatPaint: ImageVector get() = icon("PaintBrush")
    val Foundation: ImageVector get() = icon("Construction")
    val Gavel: ImageVector get() = icon("Policy")
    val GridOn: ImageVector get() = icon("Table")
    val Groups: ImageVector get() = icon("Group")
    val History: ImageVector get() = icon("Time")
    val Home: ImageVector get() = icon("Home")
    val Info: ImageVector get() = icon("Information")
    val IosShare: ImageVector get() = icon("Share")
    val Layers: ImageVector get() = icon("Layers")
    val Link: ImageVector get() = icon("Link")
    val LocationOn: ImageVector get() = icon("Location")
    val MoreHoriz: ImageVector get() = icon("OverflowMenuHorizontal")
    val MoreVert: ImageVector get() = icon("OverflowMenuVertical")
    val Person: ImageVector get() = icon("User")
    val Phone: ImageVector get() = icon("Phone")
    val Photo: ImageVector get() = icon("Image")
    val PhotoCamera: ImageVector get() = icon("Camera")
    val PictureAsPdf: ImageVector get() = icon("DocumentPdf")
    val PlayArrow: ImageVector get() = icon("PlayFilled")
    val Print: ImageVector get() = icon("Printer")
    val Remove: ImageVector get() = icon("Subtract")
    val ReportProblem: ImageVector get() = icon("Warning")
    val Roofing: ImageVector get() = icon("Construction")
    val RuleFolder: ImageVector get() = icon("Rule")
    val Search: ImageVector get() = icon("Search")
    val Share: ImageVector get() = icon("Share")
    val SquareFoot: ImageVector get() = icon("Ruler")
    val Straighten: ImageVector get() = icon("Ruler")
    val SwapHoriz: ImageVector get() = icon("ArrowsHorizontal")
    val SyncAlt: ImageVector get() = icon("ArrowsHorizontal")
    val TableChart: ImageVector get() = icon("Table")
    val TableRows: ImageVector get() = icon("Table")
    val TaskAlt: ImageVector get() = icon("CheckmarkFilled")
    val Today: ImageVector get() = icon("Calendar")
    val UploadFile: ImageVector get() = icon("Upload")
    val Verified: ImageVector get() = icon("CheckmarkFilled")
    val ViewAgenda: ImageVector get() = icon("Table")
    val ViewColumn: ImageVector get() = icon("Table")
    val Warning: ImageVector get() = icon("Warning")
    val WaterDrop: ImageVector get() = icon("RainDrop")
    val Wifi: ImageVector get() = icon("Wifi")
    val WifiOff: ImageVector get() = icon("WifiOff")
}
