package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val Primary = Color(0xFF173B40)
private val OnPrimary = Color(0xFFF5F5F1)
private val Accent = Color(0xFF247C80)
private val SurfaceLight = Color(0xFFF5F5F1)
private val OnSurfaceLight = Color(0xFF1A2424)
private val WarningText = Color(0xFF6B4710)
private val WarningContainer = Color(0xFFF8E6C4)

private val ArchiLight = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFD5E4E2),
    onPrimaryContainer = Color(0xFF062326),
    secondary = Accent,
    onSecondary = OnPrimary,
    secondaryContainer = Color(0xFFD3EDEC),
    onSecondaryContainer = Color(0xFF062326),
    tertiary = WarningText,
    onTertiary = Color(0xFFFFF8F0),
    tertiaryContainer = WarningContainer,
    onTertiaryContainer = Color(0xFF2C1C04),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE4EEEC),
    onSurfaceVariant = Color(0xFF3E4C4A),
    outline = Color(0xFF5E6E6B),
    outlineVariant = Color(0xFFDCE5E3),
)

private val ArchiDark = darkColorScheme(
    primary = Color(0xFF9FDBD6),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF1F4E52),
    onPrimaryContainer = Color(0xFFD5E4E2),
    secondary = Color(0xFF8FCFCB),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF1A4C4F),
    onSecondaryContainer = Color(0xFFD3EDEC),
    tertiary = Color(0xFFF0C27A),
    onTertiary = Color(0xFF3D2A08),
    tertiaryContainer = Color(0xFF5C4314),
    onTertiaryContainer = WarningContainer,
    background = Color(0xFF101918),
    onBackground = Color(0xFFE7EEEA),
    surface = Color(0xFF101918),
    onSurface = Color(0xFFE7EEEA),
    surfaceVariant = Color(0xFF2A3634),
    onSurfaceVariant = Color(0xFFC5D4D0),
    outline = Color(0xFF8FA19D),
    outlineVariant = Color(0xFF3E4E4B),
)

private val ArchiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun ArchiManTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) ArchiDark else ArchiLight,
        typography = Typography(),
        shapes = ArchiShapes,
        content = content,
    )
}

@Composable
fun ArchiBackdrop(content: @Composable BoxScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), content = content)
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Card(modifier, shape = RoundedCornerShape(cornerRadius)) {
        Box(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun ArchiField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
    )
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else {
            null
        },
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

fun Modifier.announce(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }
