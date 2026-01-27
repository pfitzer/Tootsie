package com.pfitzer.tootsie.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.pfitzer.tootsie.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFont = GoogleFont("Inter")

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = provider),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Bold)
)

/**
 * Creates a Typography with scaled font sizes based on the provided scale factor.
 * @param scaleFactor The multiplier to apply to font sizes (1.0 = normal)
 */
fun createScaledTypography(scaleFactor: Float = 1.0f): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (57 * scaleFactor).sp,
        lineHeight = (64 * scaleFactor).sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (45 * scaleFactor).sp,
        lineHeight = (52 * scaleFactor).sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (36 * scaleFactor).sp,
        lineHeight = (44 * scaleFactor).sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (32 * scaleFactor).sp,
        lineHeight = (40 * scaleFactor).sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (28 * scaleFactor).sp,
        lineHeight = (36 * scaleFactor).sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (24 * scaleFactor).sp,
        lineHeight = (32 * scaleFactor).sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (22 * scaleFactor).sp,
        lineHeight = (28 * scaleFactor).sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (16 * scaleFactor).sp,
        lineHeight = (24 * scaleFactor).sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (14 * scaleFactor).sp,
        lineHeight = (20 * scaleFactor).sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * scaleFactor).sp,
        lineHeight = (24 * scaleFactor).sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * scaleFactor).sp,
        lineHeight = (20 * scaleFactor).sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (12 * scaleFactor).sp,
        lineHeight = (16 * scaleFactor).sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (14 * scaleFactor).sp,
        lineHeight = (20 * scaleFactor).sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12 * scaleFactor).sp,
        lineHeight = (16 * scaleFactor).sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11 * scaleFactor).sp,
        lineHeight = (16 * scaleFactor).sp,
        letterSpacing = 0.5.sp
    )
)

// Default typography with no scaling
val Typography = createScaledTypography(1.0f)
