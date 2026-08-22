package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.theme.*

@Composable
fun CarbonStepHeader(stepNumber: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(20.dp).background(CarbonBlue60, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(stepNumber, color = CarbonWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CarbonGray100, letterSpacing = 0.2.sp)
    }
}

@Composable
fun CarbonDimensionBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    unit: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        color = CarbonGray10,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, CarbonGray30),
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontSize = 10.sp, color = CarbonGray70, fontWeight = FontWeight.Bold)
                Text(unit, fontSize = 9.sp, color = CarbonGray50)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CarbonGray100),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                cursorBrush = SolidColor(CarbonBlue60),
                modifier = Modifier.fillMaxWidth().testTag(testTag),
                decorationBox = { field ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, color = CarbonGray50)
                        field()
                    }
                }
            )
        }
    }
}
