package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = ItiPrimaryBlue,
  onPrimary = Color.White,
  primaryContainer = ItiPrimaryContainer,
  onPrimaryContainer = ItiOnPrimaryContainer,
  secondary = ItiSecondaryBlue,
  onSecondary = Color.White,
  secondaryContainer = ItiSecondaryContainer,
  onSecondaryContainer = ItiOnSecondaryContainer,
  tertiary = ItiTertiaryTeal,
  onTertiary = Color.White,
  tertiaryContainer = ItiTertiaryContainer,
  onTertiaryContainer = ItiOnTertiaryContainer,
  background = ItiBackgroundWhite,
  onBackground = ItiTextPrimary,
  surface = ItiSurfaceWhite,
  onSurface = ItiTextPrimary,
  surfaceVariant = ItiSurfaceVariant,
  onSurfaceVariant = ItiTextSecondary,
  outline = ItiOutlineBlueGrey,
  error = StatusAbsentRed,
  onError = Color.White,
  errorContainer = StatusAbsentBg,
  onErrorContainer = StatusAbsentRed
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF90CAF9),
  onPrimary = Color(0xFF072B66),
  primaryContainer = Color(0xFF0D47A1),
  onPrimaryContainer = Color(0xFFE3F2FD),
  secondary = Color(0xFF81D4FA),
  onSecondary = Color(0xFF013A5E),
  secondaryContainer = Color(0xFF0277BD),
  onSecondaryContainer = Color(0xFFE1F5FE),
  background = Color(0xFF0F172A),
  onBackground = Color(0xFFF1F5F9),
  surface = Color(0xFF1E293B),
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
