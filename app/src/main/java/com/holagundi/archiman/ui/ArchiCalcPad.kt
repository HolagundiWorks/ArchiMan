package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holagundi.archiman.domain.ArchiCalcEngine
import com.holagundi.archiman.domain.ArchiCalcMeasure
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.GlassSurface
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArchiCalcPad(onInsert: (Double) -> Unit) {
    var expr by rememberSaveable { mutableStateOf("") }
    val result = ArchiCalcEngine.evaluate(expr)
    val grid = ArchiCalcMeasure.formatGrid(expr.ifBlank { "0" })
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Archi Calc", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Text(
            expr.ifBlank { "0" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            result,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 28.sp,
            fontFamily = FontFamily.Monospace,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("m", "m²", "m³", "ft", "ft²", "ft³").zip(listOf("m", "m2", "m3", "ft", "ft2", "ft3")).forEach { (label, key) ->
                Text("${label} ${grid[key]}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        val keys = listOf(
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "0", ".", "^", "+",
            "(", ")", "x", "C",
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            keys.forEach { key ->
                TextButton(onClick = {
                    expr = if (key == "C") "" else expr + key
                }) { Text(key, color = MaterialTheme.colorScheme.onSurface) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("sin", "cos", "tan", "sqrt", "x2", "1/x", "+/-").forEach { fn ->
                TextButton(onClick = {
                    val next = ArchiCalcEngine.applyFn(if (result == "Error") expr else result, fn)
                    if (next != "Error") expr = next
                }) { Text(fn, color = MaterialTheme.colorScheme.primary) }
            }
        }
        ArchiField(expr, { expr = it }, "Expression", Modifier.fillMaxWidth())
        Button(
            onClick = {
                val number = result.toDoubleOrNull() ?: ArchiCalcEngine.eval(expr)
                if (number.isFinite()) onInsert(number)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Insert into dimension")
        }
    }
}
