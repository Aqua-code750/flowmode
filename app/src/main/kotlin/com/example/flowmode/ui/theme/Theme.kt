package com.example.flowmode.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

@Immutable
data class FlowColors(
    val trigger: Color = TriggerColor,
    val action: Color = ActionColor,
    val canvasBackground: Color = CanvasBackground,
    val wire: Color = WireColor,
    val nodeBorder: Color = NodeBorderColor,
    val nodeHeader: Color = NodeHeaderBg
)

val LocalFlowColors = staticCompositionLocalOf { FlowColors() }

object FlowTheme {
    val colors: FlowColors
        @Composable
        get() = LocalFlowColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = BlueVibrant,
    secondary = GreenVibrant,
    tertiary = OrangeVibrant,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BlueVibrant,
    secondary = GreenVibrant,
    tertiary = OrangeVibrant,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = Color.Black,
    onSurface = Color.Black
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun FlowModeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for vibrant consistency
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFlowColors provides FlowColors(
        trigger = TriggerColor,
        action = ActionColor,
        canvasBackground = if (darkTheme) BackgroundDark else BackgroundLight,
        wire = if (darkTheme) WireColor else WireColor,
        nodeBorder = if (darkTheme) Color(0xFF303438) else NodeBorderColor,
        nodeHeader = if (darkTheme) Color(0xFF25282C) else NodeHeaderBg
    )) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
