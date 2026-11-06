package com.holagundi.archiman.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.holagundi.archiman.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Test

/** Normal-sized text must retain WCAG AA contrast when themes or tokens change. */
class CarbonContrastTest {
    private fun aa(label: String, foreground: Color, background: Color) {
        val first = foreground.luminance(); val second = background.luminance()
        val ratio = (maxOf(first,second)+0.05f)/(minOf(first,second)+0.05f)
        assertTrue("$label contrast $ratio must be at least 4.5:1",ratio >= 4.5f)
    }
    private fun theme(colors: ColorScheme) {
        aa("primary text",colors.primary,colors.background)
        aa("button",colors.onPrimary,colors.primary)
        aa("selection",colors.onPrimaryContainer,colors.primaryContainer)
        aa("body",colors.onSurface,colors.surface)
        aa("card",colors.onSurface,colors.surfaceContainerLowest)
        aa("secondary text",colors.onSurfaceVariant,colors.surfaceContainerLowest)
        aa("error",colors.error,colors.surfaceContainerLowest)
        aa("error alert",colors.onErrorContainer,colors.errorContainer)
    }
    @Test fun lightTextRolesRemainAccessible() = theme(ArchiLight)
    @Test fun darkTextRolesRemainAccessible() = theme(ArchiDark)
    @Test fun supportTextRemainsAccessible() {
        listOf(CarbonLightSupport,CarbonDarkSupport).forEach {
            aa("success",it.success,it.successContainer)
            aa("warning",it.warning,it.warningContainer)
            aa("information",it.info,it.infoContainer)
        }
    }
}
