package com.example.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val ArchiManLightColorScheme = lightColorScheme(
    primary = CarbonBlue60,
    onPrimary = CarbonWhite,
    primaryContainer = CarbonBlue10,
    onPrimaryContainer = CarbonBlue70,
    secondary = CarbonGray100,
    onSecondary = CarbonWhite,
    secondaryContainer = CarbonGray10,
    onSecondaryContainer = CarbonGray100,
    tertiary = CarbonBlue70,
    onTertiary = CarbonWhite,
    background = CarbonWhite,
    surface = CarbonWhite,
    surfaceVariant = CarbonGray10,
    outline = CarbonGray20,
    outlineVariant = CarbonGray30,
    onBackground = CarbonGray100,
    onSurface = CarbonGray100,
    onSurfaceVariant = CarbonGray70,
    error = CarbonRed60,
    onError = CarbonWhite,
    errorContainer = CarbonRed10,
    onErrorContainer = CarbonRed70
)

val ArchiManShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ArchiManInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String = ""
) = OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    singleLine = true,
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.fillMaxWidth().then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
)

@Composable
fun ArchiManSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    testTag: String = "input_search"
) = OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = { Text(placeholder) },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
    singleLine = true,
    shape = MaterialTheme.shapes.extraLarge,
    modifier = Modifier.fillMaxWidth().testTag(testTag)
)

/** Compatibility wrappers for screens awaiting mechanical symbol renaming. */
@Composable
fun CarbonInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String = ""
) = ArchiManInputField(label, value, onValueChange, placeholder, keyboardType, testTag)

@Composable
fun CarbonSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    testTag: String = "input_search"
) = ArchiManSearchField(query, onQueryChange, placeholder, testTag)

@Composable
fun ArchiManTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ArchiManLightColorScheme,
        typography = ArchiManTypography,
        shapes = ArchiManShapes,
        content = content
    )
}
