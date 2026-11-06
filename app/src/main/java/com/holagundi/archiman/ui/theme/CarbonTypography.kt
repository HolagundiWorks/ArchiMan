package com.holagundi.archiman.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.holagundi.archiman.R

val CarbonPlex = FontFamily(Font(R.font.ibm_plex_sans_regular,FontWeight.Normal),Font(R.font.ibm_plex_sans_medium,FontWeight.Medium),Font(R.font.ibm_plex_sans_semibold,FontWeight.SemiBold))
private fun type(size:Int,height:Int,weight:FontWeight=FontWeight.Normal) = TextStyle(fontFamily=CarbonPlex,fontWeight=weight,fontSize=size.sp,lineHeight=height.sp)
val CarbonTypography = Typography(
    displayLarge=type(54,64),displayMedium=type(42,50),displaySmall=type(32,40),
    headlineLarge=type(32,40),headlineMedium=type(28,36),headlineSmall=type(24,32),
    titleLarge=type(20,28),titleMedium=type(16,24,FontWeight.SemiBold),titleSmall=type(14,20,FontWeight.SemiBold),
    bodyLarge=type(16,24),bodyMedium=type(14,20),bodySmall=type(12,16),
    labelLarge=type(14,20),labelMedium=type(12,16),labelSmall=type(12,16),
)
