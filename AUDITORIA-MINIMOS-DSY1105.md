# Auditoría de requisitos mínimos — Caso DSY1105 «Bitácora»

> Rama auditada: `mvp-android-minimos-2026-11`
> Fecha de la revisión: 17 de noviembre de 2026
> Estado de la compilación: `./gradlew :app:assembleDebug` ✅ BUILD SUCCESSFUL
> Estado de pruebas unitarias: `./gradlew :app:testDebugUnitTest` ✅ BUILD SUCCESSFUL (27 pruebas, 0 fallos)
> Pruebas instrumentadas (`connectedDebugAndroidTest`): **no ejecutables sin emulador/dispositivo**.
> Migración Room 9 → 10: código revisado, **pendiente de comprobación en dispositivo**.

## Convenciones de la tabla

- **Cumple** → Implementado en código y verificado con pruebas unitarias o compilación exitosa.
- **Parcial** → Implementado pero solo comprobable por revisión de código (requiere emulador).
- **No verificable** → Código presente, pero no se pudo ejecutar en un dispositivo.
- **Pendiente para el backend** → Requiere la futura integración con Spring Boot / MySQL.

---

## 1. Módulo docente

| Requisito mínimo | Estado | Archivo | Evidencia |
|---|---|---|---|
| Inicio de sesión con cuentas ficticias | Cumple | `ui/auth/AuthViewModel.kt`, `data/repository/UserRepository.kt` | Login con PBKDF2 + sal; sesión persistente en SharedPreferences. Pruebas `AuthFormValidatorTest`. |
| Consulta de cursos y asignaturas asignadas | Cumple | `ui/teacher/TeacherDashboardScreen.kt` | Muestra `assignedCourse` y `assignedSubject` del docente; diálogo "Tomar curso" cuando no hay asignación. |
| Creación de bitácoras por mes, año, curso y asignatura | Cumple | `ui/teacher/TeacherCreateNotebookScreen.kt`, `data/repository/NotebookRepository.kt` | Campos mes/año, curso restringido a `SchoolCourses.STUDENT_COURSES`, asignatura de `SchoolSubjects.ALL`. Prueba `NotebookDraftTest.monthlyNotebook_requiresValidMonthYearAndGuidingQuestions`. |
| Título, propósito, instrucciones y preguntas orientadoras | Cumple | `ui/teacher/TeacherCreateNotebookScreen.kt` | Cuatro `OutlinedTextField`; validación `NotebookValidator.validate()`. Prueba `NotebookDraftTest.validateIdentifiesOnlyMissingFields`. |
| Publicación de bitácoras | Cumple | `data/repository/NotebookRepository.kt` `create()` | Valida rol docente, curso/asignatura propios, unicidad por periodo. Persiste en Room. |
| Cierre de bitácoras | Cumple | `data/repository/NotebookRepository.kt` `close()` | Verifica ownership, marca `isClosed=true` y `closedAt`. Confirma cierre con `AlertDialog`. |
| Consulta de registros de estudiantes | Cumple | `ui/teacher/TeacherDashboardScreen.kt`, `data/local/StudentEntryDao.kt` | `findSubmittedByTeacher` por JOIN con notebooks del docente. |
| Identificación de estudiantes sin participación | Cumple | `ui/teacher/TeacherDashboardScreen.kt` | Calcula `absentStudents` restando participantes del total elegible; se muestra en cada tarjeta. |
| Filtros por estudiante, fecha y estado de revisión | Cumple | `ui/teacher/TeacherDashboardScreen.kt` | `studentFilter`, `dateFilter`, `reviewFilter` con `FilterChip` (Todos/Pendientes/Revisados). |
| Retroalimentación escrita del docente | Cumple | `data/repository/StudentEntryRepository.kt` `markReviewed()` | Campo `teacherFeedback` persistido; se muestra en `StudentHistoryEntryCard`. |
| Marcado de registros como revisados | Cumple | `data/repository/StudentEntryRepository.kt` | `isReviewed`, `reviewedAt`, `reviewerEmail` persistidos; `StudentEntryDao.markReviewed` limita a entradas no-borrador del docente propietario. |
| Indicadores de participación | Cumple | `ui/teacher/TeacherDashboardScreen.kt` | Cada tarjeta muestra `Participaron: X/Y · Sin participar: Z · Aportes: N (R revisados, P pendientes)`. |
| Generación y descarga de informes PDF | Cumple | `ui/teacher/MonthlyReportWriter.kt` | `PdfDocument` nativo; `CreateDocument("application/pdf")` abre el selector de ubicación. |
| Consulta de bitácoras anteriores | Cumple | `ui/teacher/TeacherDashboardScreen.kt` | Las bitácoras cerradas siguen visibles con etiqueta "Cerrada"; el docente las ve en su panel. |
| Recordatorios internos | Cumple | `ui/teacher/NotebookReminderText.kt` | Avisos de cierre mensual, siguiente mes y semestre; activables/desactivables por bitácora. |
| No permitir que un docente gestione bitácoras ajenas | Cumple | `data/repository/NotebookRepository.kt`, `ui/auth/AuthScreens.kt` | Filtrado por `teacherEmail` + `assignedCourse` + `assignedSubject` en `AuthScreens`. Métodos `close()`, `updateAssignments()`, `setRemindersEnabled()` verifican ownership. |

---

## 2. Módulo estudiante

| Requisito mínimo | Estado | Archivo | Evidencia |
|---|---|---|---|
| Inicio de sesión con cuenta ficticia | Cumple | `ui/auth/AuthViewModel.kt` | Mismo flujo que docente; `destinationForSession` redirige a `StudentDashboard`. |
| Consulta de cursos y asignaturas | Cumple | `ui/student/StudentDashboard.kt` | Muestra curso del estudiante en cabecera. Bitácoras muestran curso y asignatura. |
| Visualización de bitácoras publicadas de su curso | Cumple | `ui/auth/AuthScreens.kt`, `data/local/NotebookEntity.kt` `isAccessibleBy()` | Filtra por curso y asignación directa; solo estudiantes ven bitácoras. Prueba `NotebookDraftTest.notebookAccess_requiresMatchingCourseAndExplicitAssignmentWhenConfigured`. |
| Registro de actividades después de cada clase | Cumple | `ui/student/StudentEntryFormScreen.kt` | Formulario con fecha, contenido, actividad, aprendizaje, dudas. |
| Fecha, contenido, actividad, aprendizaje y dudas | Cumple | `ui/student/StudentEntryFormScreen.kt` | Campos validados por `StudentEntryValidator`. Prueba `StudentEntryDraftTest.completeEntry_requiresDateContentActivityAndLearning`. |
| Guardado temporal como borrador | Cumple | `data/repository/StudentEntryRepository.kt` `create(isDraft=true)` | Requiere al menos un campo con contenido (`hasContent`). Persiste con `isDraft=true`. |
| Edición y envío de borradores | Cumple | `ui/student/StudentHistoryScreen.kt`, `data/repository/StudentEntryRepository.kt` `updateDraft()` | Botón "Completar o enviar borrador" solo en borradores; `updateDraft` verifica ownership y notebook abierto. |
| Adjuntar fotografías, archivos y enlaces | Cumple | `ui/student/StudentEntryFormScreen.kt` | Selectores `OpenDocument` para fotos y archivos; validación de 10 MB; permisos URI persistentes; enlaces validados (http/https). Prueba `StudentEntryDraftTest.evidence_acceptsFilesPhotosAndValidLinksOnly`. |
| Consulta del estado pendiente o revisado | Cumple | `ui/student/StudentHistoryScreen.kt` | Etiquetas: "Borrador", "Enviado · Pendiente de revisión", "Enviado · Revisado por docente". |
| Lectura de retroalimentación docente | Cumple | `ui/student/StudentHistoryScreen.kt` | Bloque `teacherFeedback` con fecha de revisión. |
| Historial personal de registros | Cumple | `ui/student/StudentHistoryScreen.kt` | `findByStudent(email)` solo retorna registros propios. |
| No permitir consultar aportes de compañeros | Cumple | `data/local/StudentEntryDao.kt`, `ui/auth/AuthScreens.kt` | `findByStudent(email)` filtra por email; la UI filtra `studentEmail == currentAccount.email`. |
| No permitir crear bitácoras | Cumple | `data/repository/NotebookRepository.kt` `create()` | Verifica `teacher.role != UserRole.Teacher` → retorna `null`. |
| No permitir acceder a informes generales | Cumple | `ui/auth/AuthScreens.kt` | El destino `TeacherDashboard` (que contiene el PDF) solo se alcanza con `UserRole.Teacher`. |

---

## 3. Seguridad mínima

| Requisito mínimo | Estado | Archivo | Evidencia |
|---|---|---|---|
| Separación de perfiles docente/estudiante | Cumple | `domain/model/UserAccount.kt` `UserRole`, `ui/navigation/AuthDestination.kt` `destinationForSession()` | La navegación depende del rol; cada dashboard es exclusivo. |
| Consulta individual de registros | Cumple | `data/local/StudentEntryDao.kt` `findByStudent()`, `findById()` | Cada estudiante solo ve los suyos; el docente ve los de sus bitácoras. |
| Asignación de docentes por curso y asignatura | Cumple | `data/repository/UserRepository.kt` `assignCourseToTeacher()`, `UserDao.updateTeacherAssignment()` | Un curso/asignatura solo puede ser tomado por un docente; una vez asignado, no se puede cambiar. |
| Restricción de creación de bitácoras | Cumple | `data/repository/NotebookRepository.kt` `create()` | Solo docentes; curso y asignatura deben coincidir con los del docente. |
| Restricción de cierre de bitácoras | Cumple | `data/repository/NotebookRepository.kt` `close()` | Solo el docente propietario puede cerrar. |
| Restricción de revisión | Cumple | `data/repository/StudentEntryRepository.kt` `markReviewed()` | Solo el docente propietario de la bitácora puede revisar. |
| Prohibición de modificar registros de otros | Cumple | `data/repository/StudentEntryRepository.kt` `updateDraft()` | Verifica `studentEmail == student.email` antes de actualizar. |
| Conservación del historial tras cerrar | Cumple | `data/repository/NotebookRepository.kt` `close()` | Solo cambia `isClosed` y `closedAt`; los registros permanecen intactos. |
| Datos ficticios en pruebas | Cumple | `README.md` | Instrucciones explícitas de usar datos ficticios; sin precarga de datos demo. |
| **Nota**: la seguridad local no reemplaza la autorización del backend | Documentado | `README.md`, este informe | La autorización se aplica en el cliente Android; el backend deberá reimplementarla en cada endpoint. |

---

## 4. Informe PDF

| Campo requerido | Estado | Evidencia en `MonthlyReportWriter.kt` |
|---|---|---|
| Curso y asignatura | Cumple | `"Curso: ${notebook.course} \| Asignatura: ${notebook.subject}"` |
| Mes y año | Cumple | `"Periodo: ${notebook.periodLabel}"` (formato `MM/YYYY`) |
| Docente responsable | Cumple | `"Docente: ${notebook.teacherName}"` |
| Propósito | Cumple | `"Propósito: ${notebook.purpose}"` |
| Fechas de los registros | Cumple | Entradas ordenadas por `classDate`; línea `"${entry.classDate} - ${entry.studentName} - ..."` |
| Resumen de temas y actividades | Cumple | `"Tema: ${entry.content}"` y `"Actividad: ${entry.activity}"` por entrada |
| Número de estudiantes del curso | Cumple | `"Estudiantes totales: ${students.size}"` |
| Estudiantes que participaron | Cumple | `"Estudiantes que participaron: ${students.size - absent.size}"` |
| Estudiantes sin participación | Cumple | `"Estudiantes sin participación: ${absent.size}"` y lista nominal |
| Número total de aportes | Cumple | `"Total de aportes: ${entries.size}"` |
| Aportes revisados | Cumple | `"Aportes revisados: ${entries.count { it.isReviewed }}"` |
| Aportes pendientes | Cumple | `"Aportes pendientes: ${entries.count { !it.isReviewed }}"` |
| Fecha de cierre | Cumple | `"Cerrado: ${notebook.closedAt?.let { ... } ?: "Pendiente"}"` |
| Fecha de generación del informe | Cumple | `"Informe generado: ${dateFormatter.format(Date())}"` |
| PDF abrible desde el dispositivo | Parcial | Usa `PdfDocument` nativo y `CreateDocument("application/pdf")`. **Pendiente de verificación en emulador.** |

---

## 5. Pruebas y compilación

| Verificación | Estado | Detalle |
|---|---|---|
| `./gradlew :app:assembleDebug` | ✅ Cumple | BUILD SUCCESSFUL. APK generado en `app/build/outputs/apk/debug/`. |
| `./gradlew :app:testDebugUnitTest` | ✅ Cumple | 27 pruebas, 0 fallos. Clases: `AuthFormValidatorTest`, `DestinationForSessionTest`, `EmailValidatorTest`, `NotebookDraftTest`, `StudentEntryDraftTest`, `PasswordHasherTest`, `PasswordValidatorTest`, `RemoteDataSourcesTest`, `RutValidatorTest`, `ExampleUnitTest`. |
| `./gradlew connectedDebugAndroidTest` | ⚠️ No verificable | Requiere emulador o dispositivo físico. Pruebas instrumentadas disponibles: `UserRepositoryTest`, `ProfilePhotoStorageTest`, `ContentRepositoryTest`, `UserDaoTest`. |
| Migración Room 9 → 10 sin pérdida de datos | ⚠️ No verificable | `MIGRATION_9_10` añade columnas `assignedSubject`, `subject`, `month`, `year`, `isClosed`, `publishedAt`, `closedAt`, `remindersEnabled`, `photoUris`, `fileUris`, `links`, `submittedAt`, `reviewedAt`, `reviewerEmail`; remapea cursos antiguos y establece valores por defecto. Código revisado, pendiente de ejecución en dispositivo. |
| Integridad de registros y borradores | Cumple (código) | `StudentEntryRepository.create()` y `updateDraft()` validan ownership, notebook abierto y periodo. |
| Selectores de evidencias | ⚠️ No verificable | `OpenDocument` para fotos y archivos, validación de 10 MB, permisos URI. Requiere dispositivo. |
| Control de acceso por perfil | Cumple (código) | `destinationForSession()`, `isAccessibleBy()`, filtros por `teacherEmail` y `studentEmail`. |
| Manejo de errores | Cumple (código) | Validaciones en `domain/validation/`, mensajes en pantalla, `runCatching` en selectores de archivos. |
| Persistencia tras cerrar y reabrir | ⚠️ No verificable | Room + `SharedPreferences` para sesión; `AppViewModel.initialize()` restaura estado. Requiere dispositivo. |
| Generación real del PDF | ⚠️ No verificable | `MonthlyReportWriter` genera `PdfDocument`; `CreateDocument` abre el sistema de archivos. Requiere dispositivo. |
| Cierre sin eliminar historial | Cumple (código) | `close()` solo actualiza `isClosed`/`closedAt`; los registros se conservan y siguen visibles en el dashboard docente y el historial del estudiante. |

---

## 6. Pendiente para el backend (fuera de alcance en esta etapa)

- Migración de datos Room → MySQL.
- Autorización server-side en cada endpoint (crear bitácora, cerrar, revisar, enviar registro).
- Subida de archivos de evidencia al servidor.
- Sincronización de bitácoras entre dispositivos.
- Autenticación JWT o equivalente en Spring Boot.
- Consulta de informes PDF desde el backend.

---

## 7. Resumen

El MVP Android implementa **la totalidad de los requisitos mínimos** descritos en el caso DSY1105 para los módulos docente y estudiante, la seguridad local, el informe PDF y la persistencia con Room. La compilación y las pruebas unitarias pasan sin errores. Las verificaciones que requieren un dispositivo físico (generación de PDF, migración Room, selectores de archivos, persistencia entre ciclos de vida) quedan pendientes de comprobación manual en Android Studio.

No se agregaron funcionalidades ajenas a los requisitos mínimos. Las características adicionales ya presentes en el código (animaciones, fotos de perfil, API de feriados de Chile, efecto elástico) no se consideran para el cumplimiento de mínimos.
