package com.holagundi.archiman.ui.theme

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

@Composable internal fun rememberAppearanceMode(): AppearanceMode {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("appearance",0) }
    fun read() = AppearanceMode.entries.firstOrNull { it.name == preferences.getString("theme","SYSTEM") } ?: AppearanceMode.SYSTEM
    var mode by remember(preferences) { mutableStateOf(read()) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if(key == "theme") mode = read() }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return mode
}

data class CarbonSupportColors(val success: Color, val successContainer: Color, val warning: Color, val warningContainer: Color, val info: Color, val infoContainer: Color)
val CarbonLightSupport = CarbonSupportColors(Color(0xFF198038),Color(0xFFDEFBE6),Color(0xFF8E6A00),Color(0xFFFCF4D6),Color(0xFF0043CE),Color(0xFFEDF5FF))
val CarbonDarkSupport = CarbonSupportColors(Color(0xFF42BE65),Color(0xFF022D0D),Color(0xFFF1C21B),Color(0xFF302400),Color(0xFF78A9FF),Color(0xFF001D6C))
val carbonSupport: CarbonSupportColors
    @Composable get() = if(MaterialTheme.colorScheme.background.luminance() < 0.5f) CarbonDarkSupport else CarbonLightSupport

@Composable fun AppearanceCard() {
    val mode = rememberAppearanceMode()
    val preferences = LocalContext.current.getSharedPreferences("appearance",0)
    ArchiCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Appearance",style = MaterialTheme.typography.titleMedium)
            Text("IBM Carbon · Blue highlights",color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppearanceMode.entries.forEach { option -> ChoiceChip(option.name.lowercase().replaceFirstChar { it.uppercase() },mode == option) { preferences.edit().putString("theme",option.name).apply() } }
            }
            Text("Light: Gray 10 · Dark: Gray 100",style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Success",color = carbonSupport.success,style = MaterialTheme.typography.labelMedium)
                Text("Warning",color = carbonSupport.warning,style = MaterialTheme.typography.labelMedium)
                Text("Error",color = MaterialTheme.colorScheme.error,style = MaterialTheme.typography.labelMedium)
                Text("Info",color = carbonSupport.info,style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
