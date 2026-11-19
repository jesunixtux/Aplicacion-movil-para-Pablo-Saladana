package cl.duoc.coegio_pablo_saladana.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta central de Bitácora.
 *
 * Para cambiar el color global de los botones principales, edita [PrimaryLight]
 * (o [PrimaryDark] en modo oscuro). El texto sobre ellos: [OnPrimaryLight].
 *
 * Para cambiar el color de los mensajes y bordes de error, edita [ErrorLight]
 * (o [ErrorDark] en modo oscuro). Todas las pantallas usan
 * `MaterialTheme.colorScheme.error`, así que basta cambiar este valor.
 *
 * El formato es 0xAARRGGBB: `FF` significa completamente opaco y los últimos
 * seis dígitos son el color hexadecimal habitual. Ejemplo: azul `#1565C0` es
 * `Color(0xFF1565C0)`.
 *
 * No es necesario modificar las pantallas para cambiar la paleta. Los `Button`
 * normales usan `MaterialTheme.colorScheme.primary` automáticamente.
 */
object BitacoraColors {
    // Tema claro: colores usados normalmente por la aplicación.
    val PrimaryLight = Color(0xFF6650A4)
    val OnPrimaryLight = Color.White
    val SecondaryLight = Color(0xFF625B71)
    val OnSecondaryLight = Color.White
    val TertiaryLight = Color(0xFF7D5260)
    val OnTertiaryLight = Color.White
    val BackgroundLight = Color(0xFFFFFBFE)
    val SurfaceLight = Color(0xFFFFFBFE)
    val OnSurfaceLight = Color(0xFF1C1B1F)
    // Errores: mensajes, bordes de campos inválidos y avisos.
    val ErrorLight = Color(0xFFB3261E)
    val OnErrorLight = Color.White

    // Tema oscuro: se muestra cuando el dispositivo usa modo oscuro.
    val PrimaryDark = Color(0xFFD0BCFF)
    val OnPrimaryDark = Color(0xFF381E72)
    val SecondaryDark = Color(0xFFCCC2DC)
    val OnSecondaryDark = Color(0xFF332D41)
    val TertiaryDark = Color(0xFFEFB8C8)
    val OnTertiaryDark = Color(0xFF492532)
    val BackgroundDark = Color(0xFF1C1B1F)
    val SurfaceDark = Color(0xFF1C1B1F)
    val OnSurfaceDark = Color(0xFFE6E1E5)
    val ErrorDark = Color(0xFFF2B8B5)
    val OnErrorDark = Color(0xFF601410)
}
