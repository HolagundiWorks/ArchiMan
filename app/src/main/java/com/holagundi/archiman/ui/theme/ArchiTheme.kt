package com.holagundi.archiman.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// IBM Carbon Gray 10 / Gray 100 semantic colors, adapted to Material 3 roles.
internal val ArchiLight = lightColorScheme(
    primary = Color(0xFFB83E16), onPrimary = Color.White,
    primaryContainer = Color(0xFFF8E8E0), onPrimaryContainer = Color(0xFF6E2008),
    secondary = Color(0xFF393939), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0), onSecondaryContainer = Color(0xFF161616),
    tertiary = Color(0xFF8E6A00), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCF4D6), onTertiaryContainer = Color(0xFF161616),
    error = Color(0xFFDA1E28), onError = Color.White,
    errorContainer = Color(0xFFFFF1F1), onErrorContainer = Color(0xFFA2191F),
    background = Color(0xFFF4F4F4), onBackground = Color(0xFF161616),
    surface = Color(0xFFF4F4F4), onSurface = Color(0xFF161616),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF4F4F4), surfaceContainerHigh = Color(0xFFE0E0E0),
    surfaceContainerHighest = Color(0xFFC6C6C6),
    surfaceBright = Color.White, surfaceDim = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFFE0E0E0), onSurfaceVariant = Color(0xFF525252),
    outline = Color(0xFF8D8D8D), outlineVariant = Color(0xFFC6C6C6),
    inverseSurface = Color(0xFF393939), inverseOnSurface = Color(0xFFF4F4F4),
    inversePrimary = Color(0xFF78A9FF), surfaceTint = Color.Transparent,
    scrim = Color.Black,
)

internal val ArchiDark = darkColorScheme(
    // The lighter Ember accent keeps text and selected icons legible on Gray 100.
    primary = Color(0xFFE0703A), onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF3A2218), onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFC6C6C6), onSecondary = Color(0xFF161616),
    secondaryContainer = Color(0xFF393939), onSecondaryContainer = Color(0xFFF4F4F4),
    tertiary = Color(0xFFF1C21B), onTertiary = Color(0xFF161616),
    tertiaryContainer = Color(0xFF302400), onTertiaryContainer = Color(0xFFF1C21B),
    error = Color(0xFFFF8389), onError = Color(0xFF520408),
    errorContainer = Color(0xFF520408), onErrorContainer = Color(0xFFFFB3B8),
    background = Color(0xFF161616), onBackground = Color(0xFFF4F4F4),
    surface = Color(0xFF161616), onSurface = Color(0xFFF4F4F4),
    surfaceContainerLowest = Color(0xFF262626), surfaceContainerLow = Color(0xFF262626),
    surfaceContainer = Color(0xFF393939), surfaceContainerHigh = Color(0xFF393939),
    surfaceContainerHighest = Color(0xFF525252),
    surfaceBright = Color(0xFF393939), surfaceDim = Color(0xFF161616),
    surfaceVariant = Color(0xFF393939), onSurfaceVariant = Color(0xFFC6C6C6),
    outline = Color(0xFF8D8D8D), outlineVariant = Color(0xFF525252),
    inverseSurface = Color(0xFFF4F4F4), inverseOnSurface = Color(0xFF161616),
    inversePrimary = Color(0xFF0F62FE), surfaceTint = Color.Transparent,
    scrim = Color.Black,
)

private val ArchiShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun ArchiManTheme(content: @Composable () -> Unit) {
    val mode = rememberAppearanceMode()
    val dark = mode == AppearanceMode.DARK || (mode == AppearanceMode.SYSTEM && isSystemInDarkTheme())
    val context = LocalContext.current
    DisposableEffect(dark, context) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(0x80161616.toInt()) else SystemBarStyle.light(0xE6F4F4F4.toInt(),0x80161616.toInt()),
        )
        onDispose { }
    }
    MaterialTheme(
        colorScheme = if (dark) ArchiDark else ArchiLight,
        typography = CarbonTypography,
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

/** @deprecated Prefer [CarbonTile]. Thin alias for gradual migration. */
@Composable
fun ArchiCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    alert: CarbonAlertTone = CarbonAlertTone.NONE,
    size: CarbonTileSize = CarbonTileSize.MEDIUM,
    content: @Composable ColumnScope.() -> Unit,
) {
    CarbonTile(modifier = modifier, onClick = onClick, alert = alert, size = size, content = content)
}

/** @deprecated Prefer [CarbonExpandableTile]. */
@Composable
fun DisclosureCard(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    menu: @Composable (() -> Unit)? = null,
    alert: CarbonAlertTone = CarbonAlertTone.NONE,
    size: CarbonTileSize = CarbonTileSize.LARGE,
    content: @Composable ColumnScope.() -> Unit,
) {
    CarbonExpandableTile(
        title = title,
        summary = summary,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        leadingIcon = leadingIcon,
        menu = menu,
        alert = alert,
        size = size,
        content = content,
    )
}

@Composable
fun DetailRow(label: String, value: String, monospace: Boolean = false) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(CarbonSpacing.hairline)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

/** @deprecated Prefer [CarbonTile] for inline panels. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(CarbonSpacing.card),
    content: @Composable BoxScope.() -> Unit,
) {
    CarbonTile(modifier = modifier, contentPadding = false) {
        Box(Modifier.padding(contentPadding), content = content)
    }
}

/** @deprecated Prefer [CarbonTextInput]. */
@Composable
fun ArchiField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    errorText: String? = null,
) {
    CarbonTextInput(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        errorText = errorText,
    )
}

/** @deprecated Prefer [CarbonTag] or [CarbonContentSwitcher]. */
@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    CarbonTag(label = label, selected = selected, onClick = onClick)
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

fun Modifier.announce(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }
