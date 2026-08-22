package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Carbon Light Design System Color Scheme (Pure Crisp Light Theme)
private val CarbonLightColorScheme = lightColorScheme(
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

// Carbon Design System standard sharp/minimal corner radii (0-4dp)
val CarbonShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

@Composable
fun CarbonInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String = ""
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CarbonGray70,
            letterSpacing = 0.5.sp
        )
        Surface(
            color = CarbonGray10,
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, CarbonGray30),
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CarbonGray100
                ),
                cursorBrush = SolidColor(CarbonBlue60),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, color = CarbonGray50, fontSize = 13.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun CarbonSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    testTag: String = "input_search"
) {
    Surface(
        color = CarbonGray10,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, CarbonGray30),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = CarbonGray60,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = CarbonGray100,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(CarbonBlue60),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(placeholder, color = CarbonGray50, fontSize = 13.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Pure default Light Theme on all screens per user requirement
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CarbonLightColorScheme,
        typography = Typography,
        shapes = CarbonShapes,
        content = content
    )
}
