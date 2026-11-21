# Bitácora para docentes y estudiantes

Aplicación móvil Android para registrar y revisar el aprendizaje de estudiantes ficticios de 5.º a 8.º básico mediante bitácoras mensuales por curso y asignatura.

> **Etapa actual: MVP integrado con Spring Boot / MySQL.** MySQL es la fuente central de cuentas, bitácoras, registros, evidencias y revisiones. Room se conserva como caché y almacenamiento de borradores sin conexión.

## Estado actual

- Pantalla inicial con acceso a registro e inicio de sesión.
- Registro público de cuentas ficticias de estudiante; las cuentas docentes se cargan únicamente con el perfil de desarrollo del backend.
- Validación y formateo de RUT chileno, incluyendo dígito verificador numérico o `K`.
- Registro de género masculino, femenino u otro para personalizar el saludo.
- Autenticación real mediante `/api/auth/login` y `/api/auth/me`, con token cifrado por Android Keystore y cierre de sesión al expirar.
- Creación automática de la base de datos `bitacora.db` al registrar o consultar una cuenta.
- Sesión activa persistente y restauración automática del dashboard correspondiente.
- Fotos de perfil recortables, guardadas localmente en formato cuadrado de 64 × 64 píxeles.
- Dashboard de estudiante con las bitácoras publicadas y detalle persistente.
- Registros de clase enviados o guardados como borrador en Room, historial persistente y opción de editar/enviar borradores.
- Dashboard docente con listado, creación persistente de bitácoras y sistema de revisión/retroalimentación de registros enviados.
- Bitácoras por mes, año, curso y asignatura, con cierre mensual y bloqueo de nuevas entregas tras el cierre.
- Evidencias de clase: fotos, documentos de hasta 10 MB y enlaces HTTP/HTTPS; los archivos se conservan como URI locales con permiso de lectura.
- Indicadores por bitácora (estudiantes con/sin participación, entregas pendientes y revisadas), filtros de entregas por estudiante/fecha/estado e informe PDF mensual.
- Recordatorios internos de cierre, siguiente mes y semestre, activables por bitácora.
- Sincronización real con Spring Boot: bitácoras, borradores, entregas, revisiones y evidencias multipart; Room conserva el trabajo sin conexión y evita duplicados mediante identificadores remotos.
- Consumo de API externa de Feriados de Chile (`https://date.nager.at/api/v3/PublicHolidays/{year}/CL`) para el calendario escolar y validación contextual de fechas de clase.
- Configuración de firma de APK release (`signingConfigs`) mediante `keystore.properties`.
- Estados de carga, validaciones y mensajes de confirmación en las acciones principales.
- Transiciones animadas entre pantallas, aparición escalonada de tarjetas y movimiento al filtrar listas.
- Efecto elástico al llegar al inicio o final del contenido y aviso animado al terminar cada lista.

## Requisitos

- Android Studio Panda 1 (2025.3.1) o posterior.
- JDK 17. Android Studio incluye un JDK compatible en macOS.
- Android SDK Platform 36.1.
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

# Generar informe de cobertura de pruebas unitarias
./gradlew :app:createDebugUnitTestCoverageReport

# Compilar la aplicación de desarrollo
./gradlew assembleDebug

# Instalar la aplicación en un dispositivo conectado o emulador
./gradlew installDebug

# Limpiar compilaciones anteriores
./gradlew clean
```

El APK de desarrollo se genera en:

`app/build/outputs/apk/debug/app-debug.apk`

El informe de cobertura se genera en `app/build/reports/coverage/test/debug/index.html`.

## Backend Spring Boot y API externa

El backend integrado está en el repositorio hermano `jesunixtux/pablo-salandra-backend`. Requiere JDK 21 y MySQL local. Desde esa carpeta:

```bash
./MYSQL-Local/start-mysql.sh
./gradlew test
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

El emulador usa por defecto `http://10.0.2.2:8090/`. Se puede cambiar sin modificar código:

```bash
./gradlew installDebug -PapiBaseUrl=http://IP_O_HOST:8090/
```

El HTTP sin cifrar se acepta solo para `localhost`, el emulador, redes privadas y nombres `.local`; fuera del entorno local se debe usar HTTPS. La sincronización viene activa y se puede deshabilitar explícitamente con `-PremoteSyncEnabled=false`.

- **Persistencia:** Spring Boot/MySQL centraliza los datos compartidos. Room mantiene caché, asociación `remoteId` y borradores offline; al volver la conexión, `SyncPlanner` sincroniza sin borrar los registros locales.
- **Consumo de API externa con Retrofit:** `HolidayRemoteDataSource` consulta los feriados de Chile en `https://date.nager.at/api/v3/PublicHolidays/{year}/CL` (con respaldo local offline), mostrándolos en los paneles de docente/estudiante y avisando en el formulario cuando la fecha de clase coincide con un feriado.

### Validación de integración (21 de noviembre de 2026)

- Inicio de sesión y restauración de sesión desde Android contra Spring Boot/MySQL.
- Consulta de bitácoras autorizadas y aislamiento entre estudiantes.
- Creación de borrador, subida multipart de fotografía, envío y revisión docente.
- Descarga autenticada de la evidencia y visualización de retroalimentación.
- Borrador creado sin conexión, conservado tras reiniciar Android y sincronizado una sola vez al recuperar el backend.
- Reinicio de Android y Spring Boot sin pérdida ni duplicación de datos.
- Pruebas unitarias y `assembleDebug` de Android; pruebas H2, pruebas MySQL aisladas y `bootJar` del backend.

Queda pendiente repetir el recorrido en un dispositivo físico y configurar HTTPS para cualquier servidor fuera de la red local controlada.

## Generación de APK firmado (Release)

El módulo `app/build.gradle.kts` incluye la configuración `signingConfigs.release` que lee credenciales desde `keystore.properties` en la raíz del proyecto (ignorado por Git para no exponer claves privadas):

1. Generar el keystore (una sola vez, fuera de control de versiones):
   ```bash
   keytool -genkeypair -v -keystore bitacora-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bitacora
   ```
2. Crear el archivo `keystore.properties` en la raíz del repositorio:
   ```properties
   storeFile=bitacora-release.jks
   storePassword=TU_PASSWORD
   keyAlias=bitacora
   keyPassword=TU_PASSWORD
   ```
3. Generar el APK firmado desde terminal o desde Android Studio (**Build → Generate Signed App Bundle / APK**):
   ```bash
   ./gradlew assembleRelease
   ```
   El APK se genera en `app/build/outputs/apk/release/`.

## Dónde están las validaciones

- `app/src/main/java/cl/duoc/coegio_pablo_saladana/domain/validation/`: reglas de correo, nombre, RUT, contraseña, bitácoras y registros de clase; los validadores de formularios devuelven errores por campo.
- `ui/auth/AuthScreens.kt`: selecciona la vista; `LoginScreen.kt` y `RegisterScreen.kt` contienen cada formulario por separado.
- `ui/`: recoge los datos mediante `UiState` y `ViewModel`, y muestra los errores junto a los campos.
- `data/repository/`: normaliza los datos y vuelve a validarlos en los repositorios antes de guardarlos.
- `app/src/test/`: prueba los casos válidos e inválidos de las reglas.

Para depurar un rechazo, revisa primero la regla en `domain/validation/`, luego el mensaje en la pantalla y finalmente la llamada desde el repositorio.

## SQLite y Room

SQLite está incluido en Android. El proyecto usa Room como capa de acceso tipado y verificable sobre SQLite. Las dependencias ya están declaradas en `gradle/libs.versions.toml` y el compilador de Room se ejecuta mediante KSP.

La base de datos se crea automáticamente mediante `AppDatabase`. Contiene las tablas `users`, `notebooks` y `student_entries`, con sus DAO y repositorios. Incluye sistema de cursos y asignación directa de estudiantes a bitácoras, además del estado de revisión docente (`isReviewed`) y retroalimentación (`teacherFeedback`). Cada registro guarda la bitácora, estudiante, fecha, contenido, actividad, aprendizaje, dudas, si está enviado o en borrador, y la revisión del docente.

En el modo remoto, las contraseñas se validan exclusivamente en Spring Boot y no se guardan en Room. El almacenamiento PBKDF2 local se conserva solo para compatibilidad cuando la sincronización remota está deshabilitada.

## Fotos de perfil

Las fotos elegidas desde galería o cámara pasan por un editor con giro, zoom y ajuste horizontal o vertical. Al guardar, la aplicación recorta la selección en formato cuadrado y genera un archivo JPEG de 64 × 64 píxeles. Las imágenes grandes se reducen al cargarlas en el editor para evitar usar memoria innecesaria. Las fotos guardadas por versiones anteriores se convierten automáticamente a 64 × 64 al cargar el perfil.

La galería se abre con el selector de Android. La cámara solicita su permiso al usarla y abre la aplicación de cámara del dispositivo.

Los valores visuales se pueden cambiar en `ProfilePhotoDefaults`, dentro de `ui/profile/ProfileAvatar.kt`:

- `AvatarSize`: tamaño del avatar en pantalla.
- `EditorPreviewSize`: tamaño de la vista previa.
- `AnimationDurationMillis`: duración de las animaciones.
- `AvatarDecodeMaxImagePixels`: resolución máxima usada al mostrar avatares antiguos.
- `EditorMaxImagePixels`: resolución máxima usada por el editor.
- `MaxZoom`: ampliación máxima del recorte.

La resolución del archivo final se cambia mediante `PHOTO_SIZE_PIXELS` en `data/local/ProfilePhotoStorage.kt`.

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

## Entorno limpio para pruebas

La aplicación inicia sin datos Room precargados. Con el perfil `dev`, el backend carga de forma idempotente las cuentas y bitácoras ficticias documentadas en `readme-datos-pruebas.md` y `cuentaa-de-ejemplos.txt`. No ingresar RUT, nombres, correos, fotografías ni credenciales reales. Para limpiar por completo los datos locales del emulador:

```bash
adb shell pm clear cl.duoc.coegio_pablo_saladana
```

### Prueba manual del mínimo del caso DSY1105 (etapa Android)

1. Crear cuentas **ficticias**: un docente y al menos dos estudiantes del mismo curso de 5.º a 8.º básico.
2. Iniciar como docente, asignar curso y asignatura, y crear la bitácora del mes actual con título, propósito, instrucciones y preguntas.
3. Iniciar como el primer estudiante, añadir un registro de clase, adjuntar una foto o archivo menor de 10 MB, añadir un enlace, guardarlo como borrador, editarlo y enviarlo.
4. Iniciar como el segundo estudiante y comprobar que no ve los aportes del primero.
5. Iniciar como docente, comprobar que aparece un estudiante sin participación, filtrar registros, añadir retroalimentación y marcar el aporte como revisado.
6. Descargar el informe mensual en PDF, abrirlo y comprobar que incluye cifras de participación y aportes.
7. Cerrar la bitácora, regresar como estudiante y comprobar que el historial sigue visible pero que ya no es posible enviar nuevos registros.

La integración, los selectores de archivos y la persistencia se comprobaron en emulador. No se repitió el recorrido completo en un dispositivo físico; el PDF y el APK release no forman parte de esta migración.

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
- [ ] Al girar el dispositivo, el formulario abierto conserva los datos escritos.

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
- [ ] Al enviar un registro incompleto, muestra el error bajo cada campo obligatorio.
- [ ] Permite guardar un borrador y conservarlo después de reiniciar.
- [ ] Permite enviar un registro y conservarlo después de reiniciar.
- [ ] Muestra el historial real con estado **Borrador** o **Enviado**.
- [ ] Muestra perfil y permite cerrar sesión.

### Dashboard docente

- [ ] Muestra nombre completo y RUT.
- [ ] Muestra la cantidad y el listado de bitácoras publicadas por el docente activo.
- [ ] Permite abrir la creación de una bitácora.
- [ ] Valida título, propósito e instrucciones.
- [ ] Al intentar publicar una bitácora incompleta, muestra el error bajo cada campo obligatorio.
- [ ] Publica una bitácora y la conserva después de reiniciar la aplicación.
- [ ] Permite revisar aportes.
- [ ] Permite marcar aportes como revisados.
- [ ] Muestra perfil y permite cerrar sesión.

### Estado general

- [ ] Los estados de carga bloquean acciones duplicadas.
- [ ] Los mensajes de éxito y error son claros.
- [ ] La interfaz se puede desplazar en pantallas pequeñas.
- [ ] Solo se muestran los datos demo autorizados para la cuenta activa.
- [ ] La app vuelve a iniciar sin cuentas después de limpiar sus datos.

## Convenciones de trabajo

Para esta integración se trabajó directamente en `main`, según la instrucción de entrega, con commits técnicos después de validar ambos proyectos.

- Kotlin, Jetpack Compose y Material Design 3.
- Arquitectura MVVM.
- Cambios pequeños y commits técnicos.
- Ejecutar `./gradlew test` y `./gradlew assembleDebug` antes de entregar cambios.


