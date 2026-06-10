package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = CyberPrimaryGreen,
  secondary = CyberSecondaryBlue,
  tertiary = CyberTertiaryAmber,
  background = CyberDarkBackground,
  surface = CyberDarkSurface,
  onBackground = OnCyberDarkBackground,
  onSurface = OnCyberDarkSurface,
  error = CyberCriticalRed,
  surfaceVariant = CyberDarkSurfaceVariant
)

private val LightColorScheme = DarkColorScheme // Always use cyber dark for elite security aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for security dashboard
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve brand identity
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
