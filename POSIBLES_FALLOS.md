# Posibles fallos y cómo diagnosticarlos

Guía de diagnóstico de problemas comunes al compilar, instalar y usar la aplicación Android de Bitácora.

## Antes de corregir

1. Identifica si el problema está en la aplicación, Android Studio, Gradle, el emulador o los datos locales.
2. Revisa `git status --short` y conserva los cambios existentes.
3. Anota el mensaje completo, el paso que lo produce y las versiones de Android Studio, Gradle, AGP, JDK y SDK.
4. Reproduce el problema y cambia una cosa a la vez. No actualices versiones ni borres datos solo para quitar un aviso.
5. No incluyas contraseñas, RUT reales, tokens ni rutas locales en capturas o reportes.

## Compatibilidad de Android Studio y Gradle

La configuración de referencia adaptada del proyecto es:

- Android Gradle Plugin (AGP): `9.0.0`.
- Gradle Wrapper: `9.1.0`.
- JDK del daemon de Gradle: `17`.
- `compileSdk`: Android API `36.1`.
- `targetSdk`: Android API `36`.

Antes de cambiar versiones, confirma los valores vigentes en `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/gradle-daemon-jvm.properties` y `app/build.gradle.kts`. Si difieren de esta referencia, sigue la configuración del proyecto y comprueba la matriz oficial de compatibilidad.

### Android Studio indica que AGP es incompatible

- Revisa la versión del IDE desde **Help → About** (Windows/Linux) o **Android Studio → About Android Studio** (macOS).
- No aceptes una actualización automática de AGP durante la sincronización sin comprobar compatibilidad.
- Usa el Gradle Wrapper incluido: `./gradlew` en macOS/Linux o `gradlew.bat` en Windows.
- Si el IDE no admite el AGP del proyecto, usa una versión compatible de Android Studio o acuerda ajustar AGP y SDK. No bajes `compileSdk` o `targetSdk` sin considerar sus efectos.

### Android Studio recomienda una versión de Gradle más nueva

Un aviso de que Gradle no está actualizado no necesariamente significa que la compilación falle. Comprueba qué versión requiere el AGP actual y conserva la fijada en `gradle/wrapper/gradle-wrapper.properties` si es compatible.

### Gradle no encuentra Java o usa un JDK inesperado

- Selecciona el JDK incluido con Android Studio en **Settings/Preferences → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.
- El proyecto fija JDK 17 mediante `gradle/gradle-daemon-jvm.properties`; Gradle puede descargarlo automáticamente.
- En macOS, para probar el JDK incluido con Android Studio desde la terminal:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew --version
```

No guardes rutas de una computadora en archivos compartidos del proyecto.

### Falta el Android SDK solicitado

Instala la plataforma indicada por `compileSdk` desde **Tools → SDK Manager → SDK Platforms** y vuelve a sincronizar. `local.properties` contiene la ruta local del SDK: no la copies entre computadoras ni la agregues al repositorio.

### Aviso `android.r8.gradual.support=true` experimental

Es un aviso, no necesariamente un fallo. No retires la propiedad sin comprobar si las reglas de optimización de la compilación de lanzamiento dependen de ella.

### Aviso de una versión más nueva de “SDK XML”

Puede deberse a diferencias entre Android Studio y Android SDK Command-line Tools. Comprueba **SDK Manager → SDK Tools → Android SDK Command-line Tools**. Si la compilación termina correctamente, registra el aviso y actualiza las herramientas de forma controlada; no cambies AGP a ciegas.

## Compilar y validar

Ejecuta estos comandos desde la raíz del proyecto:

```bash
./gradlew --version
./gradlew test
./gradlew assembleDebug
```

En Windows usa `gradlew.bat` en lugar de `./gradlew`. Para pruebas instrumentadas, inicia un emulador y ejecuta `./gradlew connectedDebugAndroidTest`. Guarda el primer error completo: los siguientes pueden ser consecuencia del primero.

Si sospechas que las salidas de compilación están inconsistentes, confirma que no haya tareas Gradle activas y ejecuta `./gradlew clean assembleDebug`. `clean` borra salidas generadas de compilación; no borres bases de datos, fotos ni datos de la aplicación para solucionar un error de Gradle.

## Problemas al usar la aplicación

### No permite crear una cuenta

- Envía el formulario para que se muestren los errores de validación.
- Comprueba nombres y apellidos, RUT válido, correo y que la contraseña tenga al menos 8 caracteres y coincida con su confirmación.
- Si la cuenta ya existe, prueba con datos ficticios distintos.
- No borres la base de datos para evitar duplicados salvo que se haya autorizado limpiar los datos de prueba.

### La app vuelve al dashboard o no muestra la pantalla de acceso

La aplicación puede restaurar la sesión local al abrirse. Cierra sesión desde la app para volver al acceso. No limpies los datos como primer paso: también eliminaría cuentas, fotos y bitácoras locales.

### Falla la cámara, galería o foto de perfil

- Revisa el permiso de cámara y prueba también seleccionar una imagen de la galería.
- Confirma que la app reciba el resultado de la cámara o del selector antes de abrir el editor.
- Evita imágenes de prueba enormes si el emulador tiene poca memoria.
- No borres archivos de fotos ni datos de la app sin autorización: podrías perder imágenes de perfil.

### Una lista aparece vacía

Comprueba el rol de la cuenta, si hay registros guardados y si están publicados o como borrador. Confirma que sea un estado vacío real antes de cambiar consultas o datos.

### El emulador se cierra o no instala la app

Comprueba que el dispositivo virtual esté encendido, tenga espacio y use una versión de Android compatible. Confirma primero que `assembleDebug` termine correctamente y luego revisa el error de instalación o Logcat. No elimines ni recrees un emulador que contenga datos necesarios.

## Cierre de un fallo

Registra el síntoma, la causa comprobada, los archivos modificados y las validaciones realizadas. Añade una prueba si cambia un comportamiento verificable. Revisa el diff y `git status --short`; no incluyas datos de prueba, rutas locales ni artefactos de compilación.
