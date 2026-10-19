# Bitácora para docentes y estudiantes

Aplicación móvil Android para registrar y revisar el aprendizaje de estudiantes de 5.º a 8.º básico mediante bitácoras mensuales por curso y asignatura.

## Estado actual

- Pantalla inicial con acceso a registro e inicio de sesión.
- Registro manual de cuentas ficticias como estudiante o docente.
- Validación y formateo de RUT chileno, incluyendo dígito verificador numérico o `K`.
- Registro de género masculino, femenino u otro para personalizar el saludo.
- Usuarios y credenciales guardados localmente en SQLite mediante Room.
- Creación automática de la base de datos `bitacora.db` al registrar o consultar una cuenta.
- Sesión activa persistente y restauración automática del dashboard correspondiente.
- Fotos de perfil recortables, guardadas localmente en formato cuadrado de 64 × 64 píxeles.
- Dashboard de estudiante con las bitácoras publicadas y detalle persistente.
- Registros de clase enviados o guardados como borrador en SQLite, con historial persistente.
- Dashboard docente con listado y creación persistente de bitácoras.
- Estados de carga, validaciones y mensajes de confirmación en las acciones principales.
- Transiciones animadas entre pantallas, aparición escalonada de tarjetas y movimiento al filtrar listas.
- Efecto elástico al llegar al inicio o final del contenido y aviso animado al terminar cada lista.

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

La base de datos se crea automáticamente mediante `AppDatabase`. Contiene las tablas `users`, `notebooks` y `student_entries`, con sus DAO y repositorios. La versión 4 agrega los registros de clase mediante una migración que conserva las cuentas y bitácoras existentes. Cada registro guarda la bitácora, estudiante, fecha, contenido, actividad, aprendizaje, dudas y si está enviado o en borrador.

Las contraseñas nuevas se almacenan con una sal única y PBKDF2. Se usa HMAC-SHA-256 cuando está disponible y HMAC-SHA-1 en Android 7 para mantener compatibilidad con la versión mínima. Las cuentas anteriores con hash SHA-256 siguen funcionando: el hash se actualiza al iniciar sesión correctamente. El hash no se expone en el modelo que usa la interfaz.

## Fotos de perfil

Las fotos elegidas desde galería o cámara pasan por un editor con giro, zoom y ajuste horizontal o vertical. Al guardar, la aplicación recorta la selección en formato cuadrado y genera un archivo JPEG de 64 × 64 píxeles. Las imágenes grandes se reducen al cargarlas en el editor para evitar usar memoria innecesaria. Las fotos guardadas por versiones anteriores se convierten automáticamente a 64 × 64 al cargar el perfil.

Los valores visuales se pueden cambiar en `ProfilePhotoDefaults`, dentro de `ui/profile/ProfileAvatar.kt`:

- `AvatarSize`: tamaño del avatar en pantalla.
- `EditorPreviewSize`: tamaño de la vista previa.
- `AnimationDurationMillis`: duración de las animaciones.
- `AvatarDecodeMaxImagePixels`: resolución máxima usada al mostrar avatares antiguos.
- `EditorMaxImagePixels`: resolución máxima usada por el editor.
- `MaxZoom`: ampliación máxima del recorte.

La resolución del archivo final se cambia mediante `PHOTO_SIZE_PIXELS` en `data/ProfilePhotoStorage.kt`.

## Animaciones y desplazamiento

Los tiempos y escalas generales están centralizados en `AppMotionDefaults`, dentro de `ui/components/AppMotion.kt`:

- `NavigationDurationMillis`: duración del cambio entre pantallas.
- `ItemDurationMillis`: duración de entrada de tarjetas y bloques.
- `ItemDelayMillis`: separación entre apariciones consecutivas.
- `MaximumStaggerDelayMillis`: retraso máximo para listas largas.
- `EndOfContentPulseMillis`: velocidad del aviso de fin de contenido.
- `ItemInitialScale`: escala inicial de cada bloque.
- `EndOfContentMinimumScale`: intensidad del pulso final.

El estiramiento usa el efecto de desplazamiento de la plataforma configurado en `MainActivity.kt`. En versiones compatibles de Android se muestra como estiramiento; en versiones anteriores se mantiene el efecto visual propio del sistema.

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
- [ ] Las tarjetas aparecen suavemente y no cambian de posición con saltos al filtrar.
- [ ] Al arrastrar después del inicio o final se muestra el efecto elástico del sistema.
- [ ] El aviso de fin de contenido aparece al terminar cada lista.

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
- [ ] Muestra las bitácoras publicadas después de reiniciar la aplicación.
- [ ] Permite abrir el detalle de una bitácora.
- [ ] Permite abrir el formulario de nuevo registro.
- [ ] Valida campos del registro.
- [ ] Permite guardar un borrador y conservarlo después de reiniciar.
- [ ] Permite enviar un registro y conservarlo después de reiniciar.
- [ ] Muestra el historial real con estado **Borrador** o **Enviado**.
- [ ] Muestra perfil y permite cerrar sesión.

### Dashboard docente

- [ ] Muestra nombre completo y RUT.
- [ ] Muestra la cantidad y el listado de bitácoras publicadas por el docente activo.
- [ ] Permite abrir la creación de una bitácora.
- [ ] Valida título, propósito e instrucciones.
- [ ] Publica una bitácora y la conserva después de reiniciar la aplicación.
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


