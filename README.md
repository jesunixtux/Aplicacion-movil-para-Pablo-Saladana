# Bitácora para docentes y estudiantes

Aplicación móvil Android para registrar y revisar el aprendizaje de estudiantes de 5.º a 8.º básico mediante bitácoras mensuales por curso y asignatura.

## Estado actual

- Pantalla inicial con acceso a registro e inicio de sesión.
- Registro de cuentas ficticias como estudiante o docente.
- Validación y formateo de RUT chileno, incluyendo dígito verificador numérico o `K`.
- Credenciales guardadas localmente para el MVP.
- Room preparado como dependencia, pero todavía no utilizado en la maqueta.
- Dashboard de estudiante con bitácoras activas e historial simulado.
- Flujo visual de detalle de bitácora y creación de registros.
- Dashboard docente con indicadores, creación de bitácora y revisión de aportes simulados.
- Estados de carga, validaciones y mensajes de confirmación en las acciones principales.
- Transiciones animadas entre las pantallas principales.

## Requisitos

- Android Studio instalado.
- JDK 17 o superior. Android Studio incluye un JDK compatible en macOS.
- Android SDK con API 37.
- Emulador Android o dispositivo físico con depuración USB.

Si Gradle no encuentra Java en macOS, usa el JDK incluido en Android Studio:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

## Abrir el proyecto

1. Abrir Android Studio.
2. Seleccionar la carpeta `GitHub/Aplicacion-movil-para-Pablo-Saladana`.
3. Esperar la sincronización de Gradle.
4. Seleccionar un emulador o dispositivo.
5. Presionar **Run**.

## Comandos Gradle

Ejecutar desde la raíz de este proyecto:

```bash
# Verificar y descargar dependencias
./gradlew tasks

# Ejecutar pruebas unitarias
./gradlew test

# Compilar la aplicación de desarrollo
./gradlew assembleDebug

# Instalar la aplicación en un dispositivo conectado o emulador
./gradlew installDebug

# Limpiar compilaciones anteriores
./gradlew clean
```

El APK de desarrollo se genera en:

`app/build/outputs/apk/debug/app-debug.apk`

## SQLite y Room

SQLite está incluido en Android. El proyecto usa Room como capa de acceso tipado y verificable sobre SQLite. Las dependencias ya están declaradas en `gradle/libs.versions.toml` y el compilador de Room se ejecuta mediante KSP.

La base de datos se implementará después de cerrar la maqueta visual, mediante entidades, DAOs y repositorios para usuarios, cursos, asignaturas, bitácoras, registros, evidencias, retroalimentación e indicadores.

## Datos de prueba

Usar únicamente datos ficticios. No ingresar RUT, nombres, correos, fotografías ni credenciales reales de estudiantes o docentes.

### Cuentas demo

```text
Estudiante: estudiante@bitacora.cl / 12345678
Docente:    docente@bitacora.cl / 12345678
```

## Convenciones de trabajo

- Kotlin, Jetpack Compose y Material Design 3.
- Arquitectura MVVM.
- Cambios pequeños y commits técnicos.
- Ejecutar `./gradlew test` y `./gradlew assembleDebug` antes de entregar cambios.


