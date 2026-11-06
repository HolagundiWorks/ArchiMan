package com.holagundi.archiman.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.holagundi.archiman.ui.theme.CarbonIcons
import org.junit.Assert.*
import org.junit.Test

class CarbonIconsTest {
    @Test fun allBundledIconsConstructWithoutInvalidSvgPaths() {
        val getters=CarbonIcons.javaClass.methods.filter{it.parameterCount==0&&it.returnType==ImageVector::class.java}
        assertTrue("Navigation and action icons must be bundled",getters.size>=40)
        getters.forEach { getter ->
            val vector=getter.invoke(CarbonIcons) as ImageVector
            assertTrue(vector.name.startsWith("Carbon."))
            assertEquals(32f,vector.viewportWidth,0f)
            assertEquals(32f,vector.viewportHeight,0f)
        }
    }
}
