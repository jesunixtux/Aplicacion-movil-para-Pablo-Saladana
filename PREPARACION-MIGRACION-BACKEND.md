# Preparación de migración al backend Spring Boot / MySQL

> **Documento histórico.** Describe la preparación anterior. La integración real quedó implementada y validada; consulta `README.md` para el estado vigente.

Rama: `preparacion-migracion-backend` · Aplicación Android **Bitácora DSY1105**

> **Alcance de esta entrega:** solo se modificó el repositorio Android. El backend
> (`pablo-salandra-backend`), MySQL y sus scripts NO se tocaron. La sincronización
> remota, la autenticación remota y la subida de evidencias quedan **preparadas pero
> DESACTIVADAS**. Room sigue siendo la única fuente de datos activa.

---

## 1. Estado inicial de la aplicación

- MVP académico funcional: Room (v10), Kotlin, Jetpack Compose, Material 3, MVVM.
- Registro/login local, bitácoras mensuales por curso/asignatura, registros de clase
  (borrador/enviado), evidencias locales (fotos, documentos, enlaces), revisión docente,
  historial, indicadores e informe PDF generado en el dispositivo.
- Cliente Retrofit apuntando a una URL fija `http://10.0.2.2:8080/`, con payloads
  legacy (`NotebookPayload`, `StudentEntryPayload`) que cubren solo parte del modelo.
- Sistema de errores específicos (`AppErrors`, `lastError` en repositorios) ya integrado.
- Contrato real verificado del backend actual (solo lectura, vía GitHub):
  - `NotebookEntity`: id, title, purpose, instructions, questions, teacherEmail,
    teacherName, createdAt, course, assignedStudentEmails.
  - `StudentEntryEntity`: id, notebookId, notebookTitle, teacherEmail, studentEmail,
    studentName, classDate, content, activity, learning, doubts, isDraft, createdAt,
    isReviewed, teacherFeedback.
  - Endpoints existentes: CRUD `/api/notebooks` y `/api/student-entries`
    (filtros por `studentEmail`/`teacherEmail`, que **no son autenticación**).

## 2. Archivos modificados y nuevos

**Modificados**
| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts` | BuildConfig: `API_BASE_URL` y `REMOTE_SYNC_ENABLED` (Tarea 5) |
| `data/local/AppDatabase.kt` | Versión 11 + `MIGRATION_10_11` aditiva (Tarea 3) |
| `data/local/NotebookEntity.kt`, `StudentEntryEntity.kt` | Columnas `syncState`, `syncError`, `updatedAt` con defaults conservadores |
| `data/local/NotebookDao.kt`, `StudentEntryDao.kt` | Consulta de solo lectura `findPendingSync()` |
| `data/remote/NotebookRemoteDataSource.kt` | URL desde BuildConfig, interceptor de sesión, puerta `REMOTE_SYNC_ENABLED` |
| `data/remote/StudentEntryRemoteDataSource.kt` | Puerta `REMOTE_SYNC_ENABLED` en todas las operaciones |
| `data/repository/NotebookRepository.kt`, `StudentEntryRepository.kt` | `updatedAt` en cada escritura (marca real para conflictos futuros) |
| `domain/error/AppErrors.kt` | Constante `REMOTE_DISABLED` |

**Nuevos**
| Archivo | Propósito |
|---|---|
| `data/remote/dto/NotebookDto.kt` | DTO completo de bitácora + mapeo Entity↔DTO (Tarea 1) |
| `data/remote/dto/StudentEntryDto.kt` | DTO completo de registro + mapeo (Tarea 1) |
| `data/remote/dto/EvidenceDto.kt` | DTO de evidencia con estado de transferencia (Tarea 4) |
| `data/remote/dto/EvidenceMappers.kt` | Traducción líneas locales ↔ DTO, sin filtrar `content://` |
| `domain/auth/AuthProvider.kt` | Identidad separada del almacenamiento; proveedores local y remoto (Tarea 2) |
| `data/remote/auth/SessionTokenInterceptor.kt` | Interceptor OkHttp para token de sesión futuro (Tarea 2) |
| `domain/sync/SyncState.kt` | Estados de sincronización y política de conflictos (Tarea 3) |
| `domain/sync/SyncPlanner.kt` | Planificador puro: emparejado, pendientes, conflictos, idempotencia (Tarea 3) |
| `data/remote/EvidenceUploader.kt` | Abstracción multipart de subida + contrato propuesto sin usar (Tarea 4) |
| `app/src/test/.../tests/` (6 archivos) | Pruebas unitarias nuevas (Tarea 7) |
| `app/src/androidTest/.../Migration10To11Test.kt` | Prueba instrumentada de migración (Tarea 7) |

Las pantallas y su diseño **no se modificaron**. Los payloads legacy se conservan
para compatibilidad con el backend actual.

## 3. Modelos y DTO preparados

### NotebookDto (contrato propuesto completo)
`id` (remoto MySQL), `title`, `purpose`, `instructions`, `questions`, `teacherEmail`,
`teacherName`, `createdAt`, `course`, `assignedStudentEmails` (cadena separada por
comas, como hoy) + **propuestos**: `subject`, `month`, `year`, `publishedAt`,
`isClosed`, `closedAt`, `remindersEnabled`, `updatedAt`.

- `toEntity(localId)`: convierte a Room; sin título → null; campos ausentes usan los
  defaults de Room (asignatura por defecto, mes 1, año 2026, abierta, recordatorios
  activos, `publishedAt = createdAt`). Respuestas legacy del backend actual se
  deserializan sin fallo (probado).
- `fromEntity`: vuelca la entidad local completa sin perder campos del MVP.

### StudentEntryDto (contrato propuesto completo)
`id`, `notebookId` (**remoto**), `notebookTitle`, `teacherEmail`, `studentEmail`,
`studentName`, `classDate`, `content`, `activity`, `learning`, `doubts`, `isDraft`,
`createdAt`, `isReviewed`, `teacherFeedback` + **propuestos**: `submittedAt`,
`reviewedAt`, `reviewerEmail`, `evidences: List<EvidenceDto>`, `updatedAt`.

- `toEntity(notebook)`: usa el id **local** de la bitácora; sin `submittedAt` remoto,
  un registro enviado hereda `createdAt` (misma regla que la migración Room 9→10).
- `fromEntity(entry, remoteNotebookId)`: exige el id remoto de la bitácora; si no
  existe, el registro queda pendiente de sincronización (no se envía).

### EvidenceDto
`kind` (PHOTO/FILE/LINK), `fileName`, `mimeType`, `sizeBytes`, `remoteRef`, `url`,
`transferState` (PENDING/UPLOADED/FAILED/NOT_APPLICABLE). `isShareable` garantiza que
una URI `content://` jamás se considere descargable por otro dispositivo.

## 4. Contrato REST propuesto para el backend (NO implementado aún)

> Nada de esto existe hoy en `pablo-salandra-backend`. Es la propuesta que Spring
> Boot deberá implementar para activar la integración. No se asume soporte actual.

### Bitácoras — ampliar `/api/notebooks`
Añadir al modelo y a `NotebookInput`/respuesta:
`subject` (string), `month` (int 1–12), `year` (int), `publishedAt` (long),
`isClosed` (bool), `closedAt` (long nullable), `remindersEnabled` (bool),
`updatedAt` (long, marca de última modificación para conflictos).
- `PUT /api/notebooks/{id}` debe aceptar cierre y recordatorios (o endpoints
  dedicados `POST /api/notebooks/{id}/close`, `PUT /api/notebooks/{id}/reminders`).
- Validación servidor: mes/año válidos, docente responsable, unicidad
  (docente, curso, asignatura, mes, año) para idempotencia.

### Registros — ampliar `/api/student-entries`
Añadir: `submittedAt` (long nullable), `reviewedAt` (long nullable),
`reviewerEmail` (string), `evidences` (lista de EvidenceDto), `updatedAt` (long).
- `PUT /api/student-entries/{id}/review` para revisión docente con permisos.
- El servidor debe validar que el estudiante solo edita sus borradores y que el
  docente solo revisa registros de sus bitácoras (hoy los filtros por correo no
  autentican nada).

### Autenticación (nuevo)
- `POST /api/auth/register` y `POST /api/auth/login` → identidad + token de sesión.
- `POST /api/auth/logout` (invalidación del token).
- El token viaja en `Authorization: Bearer <token>`; el interceptor Android ya está
  listo para adjuntarlo cuando exista un proveedor real.

### Evidencias (nuevo)
- `POST /api/evidences` (multipart): parte `file` (bytes), `entryRemoteId`, `kind`.
  Respuesta: `EvidenceDto` con `remoteRef` real (URL https o id del servidor).
- `GET /api/evidences/{ref}` para descarga autenticada.
- Límite actual del cliente: 10 MB por archivo con tamaño conocido (se conserva).

## 5. Campos y endpoints que Spring Boot todavía debe implementar

| Área | Existe hoy | Falta |
|---|---|---|
| Notebooks | CRUD básico + course + assignedStudentEmails | subject, month, year, publishedAt, isClosed, closedAt, remindersEnabled, updatedAt, cierre/recordatorios |
| Student entries | CRUD + filtros por correo | submittedAt, reviewedAt, reviewerEmail, evidences, updatedAt, endpoint de revisión con permisos |
| Auth | Nada | register/login/logout, tokens, validación de permisos por endpoint |
| Evidencias | Nada | subida multipart, almacenamiento, referencias remotas, descarga |
| Usuarios | Nada (cuentas solo en Room) | cuentas centralizadas con roles y curso/asignatura |

## 6. Estrategia de autenticación y autorización futura

- **Identidad separada del almacenamiento**: `UserAccount` es la identidad (nunca
  contiene contraseña ni hash) e incluye rol (Student/Teacher), curso y asignatura.
- `AuthProvider` (interfaz) con dos implementaciones:
  - `LocalAuthProvider` — activo hoy; envuelve `UserRepository.login`; la contraseña
    nunca sale del dispositivo.
  - `RemoteAuthProvider` — preparado y **desactivado**: cualquier intento devuelve un
    fallo explícito ("aún no está habilitada"). No hay tokens JWT falsos en ningún lugar.
- `SessionTokenInterceptor` + `SessionTokenProvider`: adjunta `Authorization: Bearer`
  solo si hay token real; el proveedor por defecto devuelve null (probado).
- **Limitación declarada**: no existe seguridad completa hasta que el backend valide
  permisos en cada endpoint. Los filtros por correo actuales no son autenticación.
- Las restricciones locales de acceso (curso, invitaciones, docente responsable) se
  conservan intactas en Room y repositorios.

## 7. Estrategia de sincronización y prevención de duplicados

- **Identificadores**: `id` local de Room (autoincremental) vs `remoteId` (MySQL).
  Nunca se mezclan: los DTO usan el remoto; `toEntity` recibe el local.
- **Estado por fila** (Room v11): `syncState` ∈ {LOCAL_ONLY, PENDING_UPLOAD, SYNCED,
  CONFLICT, ERROR}, `syncError` (motivo del último fallo), `updatedAt` (marca de
  modificación, inicializada a `createdAt` en la migración).
- **Correspondencia estable**: `SyncPlanner` empareja EXCLUSIVAMENTE por `remoteId`.
  Emparejar por título/correo está prohibido (produciría duplicados); hay prueba que
  lo verifica.
- **Idempotencia**: `alreadyHasRemoteIdentity(remoteId)` — una fila con remoteId no se
  vuelve a crear en el servidor; se actualiza. El backend debe además garantizar
  unicidad (docente, curso, asignatura, mes, año).
- **Conflictos**: last-write-wins por `updatedAt` cuando el backend lo exponga;
  mientras no exista marca remota, **gana lo local** (conservador: no pierde trabajo).
- **Pendientes**: `findPendingSync()` (DAO, solo lectura) y
  `SyncPlanner.pending*Uploads()` excluyen SYNCED; estados desconocidos se tratan
  como LOCAL_ONLY.
- El planificador es **puro**: calcula planes, nunca escribe ni borra. Ningún registro
  local se modificó ni eliminó durante esta preparación.

## 8. Estrategia de transferencia de evidencias

- Separación en cuatro capas: (1) referencias locales `content://` en Room,
  (2) metadatos (`fileName`, `mimeType`, `sizeBytes`, se completarán al subir leyendo
  el ContentResolver), (3) `remoteRef` entregada por el backend, (4) `transferState`.
- `EvidenceMappers.fromLocalLines`: archivos locales → PENDING sin referencia
  compartible; enlaces http/https → NOT_APPLICABLE (ya compartibles); esquemas
  peligrosos (javascript:, ftp:) se descartan. **Prueba verifica que `content://`
  jamás aparece en el JSON enviado al servidor.**
- `EvidenceUploader` (abstracción con streams) + `DisabledEvidenceUploader` (activo):
  no abre streams ni inventa URLs; reporta el motivo. `ProposedEvidenceUploadApi`
  documenta el multipart propuesto sin instanciarse.
- Restricciones conservadas: 10 MB máximos, tamaño conocido, permisos de lectura
  persistidos al seleccionar (`takePersistableUriPermission`, ya existente y con
  mensajes de error específicos).

## 9. Migraciones Room realizadas

- **10 → 11 (`MIGRATION_10_11`)**: exclusivamente aditiva.
  - `notebooks` y `student_entries`: `syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'`,
    `syncError TEXT`, `updatedAt INTEGER NOT NULL DEFAULT 0`.
  - `UPDATE ... SET updatedAt = createdAt` para que las filas existentes tengan una
    marca real. No se borra ni altera ningún dato existente.
- Todas las migraciones 1→10 se conservan sin cambios. No hay migraciones destructivas.
- Prueba instrumentada `Migration10To11Test`: ejecuta la migración sobre una BD v10
  con datos y verifica preservación + defaults; y prueba instalación nueva v11 con
  round-trip de DAOs y `findPendingSync()`. **Pendiente de ejecutar en emulador.**

## 10. Pruebas ejecutadas y resultados

| Comando | Resultado |
|---|---|
| `./gradlew :app:assembleDebug` | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | ✅ **140 pruebas, 0 fallos** (26 clases) |
| `./gradlew connectedDebugAndroidTest` | ❌ **No ejecutada: "No connected devices!"** — no hay emulador/dispositivo disponible en esta máquina. El APK de pruebas compila y empaqueta correctamente. |

Pruebas unitarias nuevas (todas pasan):
- `NotebookDtoTest` — serialización completa, compatibilidad legacy, defaults, ids.
- `StudentEntryDtoTest` — legacy, fecha de envío derivada, id local vs remoto,
  `content://` nunca viaja al servidor.
- `EvidenceMappersTest` — pendientes, compartibles, esquemas descartados, round-trip JSON.
- `SyncPlannerTest` — emparejado solo por remoteId, prevención de duplicados,
  pendientes, conflictos local-first, idempotencia.
- `SessionTokenInterceptorTest` — sin token no hay cabecera; con token real, Bearer.
- `RemoteIntegrationDisabledTest` — flag apagado, fuentes remotas rechazadas con
  `REMOTE_DISABLED`, auth remota desactivada, subida de evidencias desactivada sin
  abrir streams.

Pruebas previas (109) intactas y passing: validadores, hash, DTOs legacy, navegación,
errores, etc. **Ninguna prueba fue adulterada.**

## 11. Pendiente hasta conectar MySQL (integración NO activa)

1. Backend: implementar el contrato propuesto (secciones 4–5), incluida validación de
   permisos por endpoint y cuentas centralizadas.
2. Android: activar `REMOTE_SYNC_ENABLED` solo cuando el contrato exista; conectar
   `RemoteAuthProvider` al endpoint real y el `SessionTokenProvider` al token recibido.
3. Ejecutor de sincronización que consuma `SyncPlanner` (hoy solo planifica) y
   `EvidenceUploader` real (multipart con streams).
4. Ejecutar `connectedDebugAndroidTest` en emulador/dispositivo (migración 10→11 y
   pruebas Room existentes).
5. Resolución de conflictos con `updatedAt` remoto cuando el backend lo exponga.
6. Completar metadatos de evidencias (nombre/tipo/tamaño) desde ContentResolver al subir.

### Leyenda de estado
- **Preparado y probado (unitario):** DTOs, mapeos, planificador de sync, interceptor,
  puertas de desactivación, migración SQL (lógica), configuración BuildConfig.
- **Preparado, prueba pendiente de dispositivo:** migración 10→11 instrumentada,
  comportamiento Room v11 en dispositivo real.
- **Integración pendiente (no activa):** login remoto, sincronización real, subida de
  evidencias, cuentas centralizadas en MySQL.

---

## 12. Revisión final sobre `main` (comparación con el backend real)

Revisión realizada directamente sobre `main`, sin ramas ni PR. Backend consultado
en modo lectura (`pablo-salandra-backend`: entidades, servicios, controladores y
`application.properties`).

### 12.1 Qué estaba correcto (verificado, sin cambios)
- **DTOs completos**: `NotebookDto`/`StudentEntryDto` conservan todos los campos del
  MVP; round-trip y compatibilidad legacy cubiertos por pruebas.
- **Identificadores**: local (Room) y remoto (MySQL) nunca se confunden;
  `toEntity` usa el id local de la bitácora y `remoteId` solo proviene del servidor.
- **Evidencias**: `content://` jamás se serializa como URL compartible (prueba
  explícita que inspecciona el JSON).
- **Autenticación remota**: desactivada, sin tokens ficticios; el interceptor no
  adjunta cabecera sin token real (probado).
- **Room**: migración 10→11 exclusivamente aditiva; ningún registro local se modifica
  ni elimina durante la preparación.
- **Metadatos de sync**: todas las escrituras locales (crear, editar borrador, enviar,
  cerrar, asignar, recordar, revisar) actualizan `updatedAt`.
- **Errores de conexión**: se capturan y reportan vía `AppErrors`/`lastError` sin
  perder datos locales.
- **Compatibilidad de contrato verificada contra el backend real**:
  - El parseo estricto de fecha del backend (`dd/MM/uuuu`, STRICT) coincide con
    `StudentEntryValidator` (dd/MM/yyyy no lenient).
  - `StudentEntryInput` de Android coincide campo a campo con el record del backend
    (el backend deriva `teacherEmail` desde la bitácora; Android no lo envía). ✔
  - El backend normaliza correos a minúsculas y curso en blanco → "General", igual
    que los mapeos Android.

### 12.2 Problemas encontrados y corregidos en Android
1. **Puerto por defecto incorrecto (confirmado)**: `application.properties` del
   backend define `server.port= 8090`, pero Android asumía `http://10.0.2.2:8080/`.
   Corregido el default de `BuildConfig.API_BASE_URL` a `http://10.0.2.2:8090/`
   (sobrescribible con `-PapiBaseUrl`). Las dos pruebas que fijaban la URL por
   defecto se actualizaron al nuevo valor correcto (no se alteró ninguna aserción
   de comportamiento).
2. **Duplicación de mapeo REST (confirmada)**: `NotebookPayload.toEntity` y
   `StudentEntryPayload.toEntity` repetían la lógica de conversión de los DTOs.
   Ahora ambos delegan en `NotebookDto`/`StudentEntryDto`: una sola lógica de
   conversión. Efecto secundario deseado: el payload legacy de registros enviados
   ahora deriva `submittedAt = createdAt`, igual que la migración Room 9→10 y el
   DTO (antes quedaba null). Todas las pruebas existentes siguen pasando.

### 12.3 Hallazgos del backend que NO se corrigieron (requieren Spring Boot)
- **`server.address=127.0.0.1`**: el backend solo escucha en loopback. El emulador
  funciona vía `10.0.2.2`, pero un **dispositivo físico** no podrá conectarse hasta
  que el backend escuche en la interfaz LAN (cambio de backend, no realizado).
- **Límites de tamaño**: el backend valida `@Size` (title ≤ 200, notebookTitle ≤ 200,
  classDate ≤ 30, textos ≤ 10000, correos ≤ 255). Android no impone hoy esos límites;
  un título largo sería rechazado con HTTP 400 al activar la integración. Decisión
  pendiente: añadir límites en Android o ampliarlos en el backend. No se modificaron
  pantallas ni validadores del MVP.
- **Borradores solo con evidencia**: el backend exige texto en
  content/activity/learning/doubts para aceptar un borrador; Android también acepta
  borradores que solo tienen fotos/archivos/enlaces. Serán HTTP 400 hasta que el
  backend soporte evidencias. No se cambió el comportamiento local del MVP.
- Sigue pendiente todo el contrato propuesto de las secciones 4–5 (modelo mensual,
  auth, evidencias, `updatedAt`, permisos por endpoint).

### 12.4 Pruebas de la revisión
| Comando | Resultado |
|---|---|
| `./gradlew :app:assembleDebug` | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | ✅ **140 pruebas, 0 fallos** |
| `./gradlew connectedDebugAndroidTest` | ❌ No ejecutada: "No connected devices!" (sin emulador/dispositivo). La migración 10→11 sigue pendiente de verificación en dispositivo. |

### 12.5 ¿Está Android listo para iniciar la integración?
**Sí, del lado Android**: modelos, mapeos, identificadores, estados de sync,
interceptor de sesión, configuración de URL/puerto (ya alineada con el puerto real
8090) y puertas de desactivación están preparados y probados. La integración no
puede activarse todavía porque depende de que Spring Boot implemente el contrato
propuesto (secciones 4–5) y los puntos de 12.3; nada de eso se modificó.
