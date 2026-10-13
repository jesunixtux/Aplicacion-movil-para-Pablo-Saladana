# Bitácora para docentes y estudiantes

Aplicación móvil Android para registrar y revisar el aprendizaje de estudiantes de 5.º a 8.º básico mediante bitácoras mensuales por curso y asignatura.

## Estado actual

- Pantalla inicial con acceso a registro e inicio de sesión.
- Registro manual de cuentas ficticias como estudiante o docente.
- Validación y formateo de RUT chileno, incluyendo dígito verificador numérico o `K`.
- Registro de género masculino, femenino u otro para personalizar el saludo.
- Usuarios y credenciales guardados localmente en SQLite mediante Room.
- Creación automática de la base de datos `bitacora.db` al iniciar la aplicación.
- Sesión activa persistente y restauración automática del dashboard correspondiente.
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

La base de datos se crea automáticamente mediante `AppDatabase`. Actualmente contiene la tabla `users`, su `UserDao` y el repositorio de autenticación. La versión actual incluye una migración automática para agregar el género a cuentas existentes, usando `Other` como valor predeterminado. Las demás entidades se agregarán con nuevas migraciones de Room cuando se implementen cursos, asignaturas, bitácoras, registros, evidencias, retroalimentación e indicadores.

## Datos de prueba

Usar únicamente datos ficticios. No ingresar RUT, nombres, correos, fotografías ni credenciales reales de estudiantes o docentes.

Las cuentas de prueba deben crearse manualmente desde **Registrarse** y pueden eliminarse borrando los datos de la aplicación o desinstalándola del emulador. Para limpiar los datos del emulador:

```bash
adb shell pm clear cl.duoc.coegio_pablo_saladana
```

## Lista de pruebas de autenticación y SQLite

### Inicio y navegación

- [ ] La aplicación abre en la pantalla de bienvenida.
- [ ] **Iniciar sesión** abre el formulario correcto.
- [ ] **Registrarse** abre el formulario correcto.
- [ ] Los botones **Volver** funcionan.
- [ ] El botón atrás del sistema Android respeta la navegación.
- [ ] Las transiciones no muestran pantallas superpuestas ni bloqueadas.

### Registro de cuenta

- [ ] No permite enviar campos obligatorios vacíos.
- [ ] Muestra el error debajo del campo correspondiente.
- [ ] No permite números en nombres ni apellidos.
- [ ] Permite segundo nombre vacío o con letras válidas.
- [ ] Formatea el RUT con puntos y guion.
- [ ] Acepta RUT válido con dígito numérico.
- [ ] Acepta RUT válido con dígito `K`.
- [ ] Rechaza RUT inválido.
- [ ] Rechaza correos sin formato válido.
- [ ] Exige contraseña de mínimo 8 caracteres.
- [ ] Exige que la confirmación de contraseña coincida.
- [ ] Mostrar/ocultar contraseña funciona.
- [ ] El botón de crear cuenta se bloquea mientras carga.
- [ ] Al crear la cuenta, inicia sesión automáticamente.
- [ ] El perfil seleccionado dirige al dashboard correcto.

### Inicio de sesión

- [ ] Rechaza correo o contraseña incorrectos.
- [ ] Muestra errores debajo de cada campo.
- [ ] El botón se desactiva con datos incompletos.
- [ ] Muestra el estado de carga.
- [ ] Dirige al dashboard de estudiante o docente.

### Dashboard estudiante

- [ ] Muestra nombre completo y RUT.
- [ ] Permite cambiar entre bitácoras activas e historial.
- [ ] Permite abrir el detalle de una bitácora.
- [ ] Permite abrir el formulario de nuevo registro.
- [ ] Valida campos del registro.
- [ ] Permite guardar borrador simulado.
- [ ] Permite enviar registro simulado.
- [ ] Muestra historial y estados de revisión.
- [ ] Muestra perfil y permite cerrar sesión.

### Dashboard docente

- [ ] Muestra nombre completo y RUT.
- [ ] Muestra indicadores de participación simulados.
- [ ] Permite abrir la creación de una bitácora.
- [ ] Valida título, propósito e instrucciones.
- [ ] Permite publicar una bitácora simulada.
- [ ] Permite revisar aportes.
- [ ] Permite marcar aportes como revisados.
- [ ] Muestra perfil y permite cerrar sesión.

### Estado general

- [ ] Los estados de carga bloquean acciones duplicadas.
- [ ] Los mensajes de éxito y error son claros.
- [ ] La interfaz se puede desplazar en pantallas pequeñas.
- [ ] No se muestran datos demo precargados.
- [ ] La app vuelve a iniciar sin cuentas después de limpiar sus datos.

## Convenciones de trabajo

- Kotlin, Jetpack Compose y Material Design 3.
- Arquitectura MVVM.
- Cambios pequeños y commits técnicos.
- Ejecutar `./gradlew test` y `./gradlew assembleDebug` antes de entregar cambios.


