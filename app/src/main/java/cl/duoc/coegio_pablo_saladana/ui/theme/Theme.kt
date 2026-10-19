package cl.duoc.coegio_pablo_saladana.ui.theme

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
    primary = BitacoraColors.PrimaryDark,
    onPrimary = BitacoraColors.OnPrimaryDark,
    secondary = BitacoraColors.SecondaryDark,
    onSecondary = BitacoraColors.OnSecondaryDark,
    tertiary = BitacoraColors.TertiaryDark,
    onTertiary = BitacoraColors.OnTertiaryDark,
    background = BitacoraColors.BackgroundDark,
    surface = BitacoraColors.SurfaceDark,
    onSurface = BitacoraColors.OnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = BitacoraColors.PrimaryLight,
    onPrimary = BitacoraColors.OnPrimaryLight,
    secondary = BitacoraColors.SecondaryLight,
    onSecondary = BitacoraColors.OnSecondaryLight,
    tertiary = BitacoraColors.TertiaryLight,
    onTertiary = BitacoraColors.OnTertiaryLight,
    background = BitacoraColors.BackgroundLight,
    surface = BitacoraColors.SurfaceLight,
    onSurface = BitacoraColors.OnSurfaceLight
)

@Composable
fun CoegioPabloSaladanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep the palette predictable. Set true only to use Android 12+ wallpaper colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
