package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import cl.duoc.coegio_pablo_saladana.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Inserta datos de demostración faltantes sin sustituir información existente. */
object DebugDataSeeder {
    private const val DEMO_PASSWORD = "BitacoraDemo26!"
    private const val NOTEBOOK_TITLE = "Demostración: ciclo del agua"
    private const val SUBMITTED_CONTENT = "Etapas del ciclo del agua: evaporación, condensación y precipitación."
    private const val DRAFT_CONTENT = "Borrador de prueba: observar ejemplos de evaporación en casa."

    suspend fun seedIfNeeded(
        context: Context,
        users: UserRepository,
        notebooks: NotebookRepository,
        entries: StudentEntryRepository
    ) {
        if (!BuildConfig.DEBUG) return
        val userDao = AppDatabase.getInstance(context).userDao()

        val teacher = ensureUser(
            users,
            userDao,
            DemoUser(
                firstName = "Camila",
                middleName = "Andrea",
                paternalSurname = "Rojas",
                maternalSurname = "Mella",
                rut = "DEMO-001",
                email = "docente.prueba@example.com",
                role = UserRole.Teacher,
                gender = UserGender.Female
            )
        ) ?: return

        val students = listOf(
            DemoUser(
                firstName = "Martín",
                middleName = "",
                paternalSurname = "Soto",
                maternalSurname = "Vera",
                rut = "DEMO-002",
                email = "estudiante.prueba@example.com",
                role = UserRole.Student,
                gender = UserGender.Male
            ),
            DemoUser(
                firstName = "Alex",
                middleName = "Paz",
                paternalSurname = "Fuentes",
                maternalSurname = "León",
                rut = "DEMO-003",
                email = "tercer.estudiante@example.com",
                role = UserRole.Student,
                gender = UserGender.Other
            )
        ).mapNotNull { ensureUser(users, userDao, it) }

        val notebookDraft = NotebookDraft(
            title = NOTEBOOK_TITLE,
            purpose = "Reconocer las etapas principales del ciclo del agua y relacionarlas con el entorno.",
            instructions = "Lee el contenido de la clase. Registra la actividad realizada, qué aprendiste y cualquier duda.",
            questions = "¿Qué etapas tiene el ciclo del agua? ¿Dónde observas evaporación o condensación en tu vida diaria?"
        )
        val notebook = notebooks.findAll().firstOrNull {
            it.title == NOTEBOOK_TITLE && it.teacherEmail == teacher.email
        } ?: notebooks.create(notebookDraft, teacher) ?: return

        val classDate = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date())
        students.forEach { student ->
            val existingEntries = entries.findByStudent(student.email)
                .filter { it.notebookId == notebook.id }
            if (existingEntries.none { !it.isDraft && it.content == SUBMITTED_CONTENT }) {
                entries.create(
                    StudentEntryDraft(
                        classDate = classDate,
                        content = SUBMITTED_CONTENT,
                        activity = "Dibujé el ciclo del agua y relacioné cada etapa con un ejemplo cotidiano.",
                        learning = "Aprendí que el agua cambia de estado y circula entre la superficie y la atmósfera.",
                        doubts = "Quiero entender mejor cómo influye la temperatura en la evaporación."
                    ),
                    isDraft = false,
                    student = student,
                    notebook = notebook
                )
            }
            if (existingEntries.none { it.isDraft && it.content == DRAFT_CONTENT }) {
                entries.create(
                    StudentEntryDraft(
                        classDate = classDate,
                        content = DRAFT_CONTENT,
                        activity = "",
                        learning = "",
                        doubts = ""
                    ),
                    isDraft = true,
                    student = student,
                    notebook = notebook
                )
            }
        }
    }

    private suspend fun ensureUser(
        users: UserRepository,
        userDao: UserDao,
        demo: DemoUser
    ): UserAccount? {
        users.findAccountByEmail(demo.email)?.let { existing ->
            return existing.takeIf { it.role == demo.role }
        }

        // Deliberately invalid RUT placeholders avoid real-looking identifiers in demo data.
        val passwordHash = withContext(Dispatchers.Default) { PasswordHasher.create(DEMO_PASSWORD) }
        userDao.insert(
            UserEntity(
                firstName = demo.firstName,
                middleName = demo.middleName,
                paternalSurname = demo.paternalSurname,
                maternalSurname = demo.maternalSurname,
                rut = demo.rut,
                email = demo.email,
                role = demo.role.name,
                gender = demo.gender.name,
                passwordHash = passwordHash
            )
        )
        return users.findAccountByEmail(demo.email)?.takeIf { it.role == demo.role }
    }

    private data class DemoUser(
        val firstName: String,
        val middleName: String,
        val paternalSurname: String,
        val maternalSurname: String,
        val rut: String,
        val email: String,
        val role: UserRole,
        val gender: UserGender
    )
}
