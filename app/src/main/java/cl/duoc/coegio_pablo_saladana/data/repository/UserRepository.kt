package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.PasswordHasher
import cl.duoc.coegio_pablo_saladana.data.local.UserEntity
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.EmailValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.PasswordValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.PersonNameValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.RutValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Centraliza cuentas, cursos de estudiantes y la sesión persistida localmente. */
class UserRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val userDao = AppDatabase.getInstance(context).userDao()

    /** Motivo específico del último fallo; null si la última operación fue exitosa. */
    var lastError: String? = null
        private set

    suspend fun register(
        firstName: String,
        middleName: String,
        paternalSurname: String,
        maternalSurname: String,
        rut: String,
        email: String,
        password: String,
        role: UserRole,
        gender: UserGender,
        course: String = SchoolCourses.DEFAULT_STUDENT_COURSE
    ): UserAccount? = runCatching {
        lastError = null
        val normalizedEmail = email.trim().lowercase()
        val normalizedRut = RutValidator.format(rut.trim())
        if (
            !PersonNameValidator.isValid(firstName) ||
            (middleName.isNotBlank() && !PersonNameValidator.isValid(middleName)) ||
            !PersonNameValidator.isValid(paternalSurname) ||
            !PersonNameValidator.isValid(maternalSurname) ||
            !RutValidator.isValid(normalizedRut) ||
            !EmailValidator.isValid(normalizedEmail) ||
            !PasswordValidator.isValid(password)
        ) {
            lastError = "Los datos del registro no son válidos. Revisa los campos marcados."
            return null
        }
        if (userDao.findByEmail(normalizedEmail) != null || userDao.findByRut(normalizedRut) != null) {
            lastError = "Ya existe una cuenta con ese correo o RUT."
            return null
        }

        val normalizedCourse = if (role == UserRole.Student) {
            course.trim().ifBlank { SchoolCourses.DEFAULT_STUDENT_COURSE }
        } else {
            SchoolCourses.GENERAL
        }
        val passwordHash = withContext(Dispatchers.Default) { PasswordHasher.create(password) }
        val user = UserEntity(
            firstName = firstName.trim(),
            middleName = middleName.trim(),
            paternalSurname = paternalSurname.trim(),
            maternalSurname = maternalSurname.trim(),
            rut = normalizedRut,
            email = normalizedEmail,
            role = role.name,
            gender = gender.name,
            passwordHash = passwordHash,
            course = normalizedCourse
        )
        val id = userDao.insert(user)
        if (id == -1L) {
            lastError = "La base de datos local no pudo crear la cuenta."
            return null
        }
        user.copy(id = id).toUserAccount()
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun login(email: String, password: String): UserAccount? = runCatching {
        lastError = null
        val normalizedEmail = email.trim().lowercase()
        // Mensaje combinado a propósito: no revela si el correo existe (evita enumerar cuentas).
        val user = userDao.findByEmail(normalizedEmail) ?: run {
            lastError = "El correo o la contraseña no son correctos."
            return null
        }
        val verified = withContext(Dispatchers.Default) {
            PasswordHasher.verify(password, user.passwordHash)
        }
        if (!verified) {
            lastError = "El correo o la contraseña no son correctos."
            return null
        }
        // Una cuenta antigua se actualiza después de verificar correctamente su contraseña.
        if (PasswordHasher.isLegacyHash(user.passwordHash)) {
            val upgradedHash = withContext(Dispatchers.Default) { PasswordHasher.create(password) }
            userDao.updatePasswordHash(user.id, user.passwordHash, upgradedHash)
        }
        user.toUserAccount()
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun findAccountByEmail(email: String): UserAccount? = runCatching {
        lastError = null
        userDao.findByEmail(email.trim().lowercase())?.toUserAccount()
    }.getOrElse { lastError = AppErrors.describe(it); null }

    suspend fun findAllStudents(): List<UserAccount> = runCatching {
        lastError = null
        userDao.findAllStudents().map { it.toUserAccount() }
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun findAllStudentsByCourse(course: String): List<UserAccount> = runCatching {
        lastError = null
        userDao.findAllStudents().filter { it.course == course }.map { it.toUserAccount() }
    }.getOrElse { lastError = AppErrors.describe(it); emptyList() }

    suspend fun isAssignmentAvailable(course: String, subject: String): Boolean = runCatching {
        lastError = null
        course in SchoolCourses.STUDENT_COURSES && subject in SchoolSubjects.ALL &&
            userDao.findTeacherByAssignment(course, subject) == null
    }.getOrElse { lastError = AppErrors.describe(it); false }

    suspend fun assignCourseToTeacher(teacherEmail: String, course: String, subject: String): Boolean = runCatching {
        lastError = null
        if (!isAssignmentAvailable(course, subject)) {
            lastError = "Ese curso y asignatura no están disponibles: ya los tomó otro docente o no existen."
            return false
        }
        val updated = userDao.updateTeacherAssignment(teacherEmail, course, subject)
        if (updated == 0) lastError = "No se encontró la cuenta docente para asignarle el curso."
        updated > 0
    }.getOrElse { lastError = AppErrors.describe(it); false }

    fun saveSession(account: UserAccount) {
        preferences.edit()
            .putString(ACTIVE_SESSION_KEY, account.email)
            .apply()
    }

    suspend fun getActiveSession(): UserAccount? = runCatching {
        lastError = null
        val email = preferences.getString(ACTIVE_SESSION_KEY, null) ?: return null
        val account = userDao.findByEmail(email.trim().lowercase())?.toUserAccount()
        if (account == null) clearSession()
        account
    }.getOrElse { lastError = AppErrors.describe(it); null }

    fun clearSession() {
        preferences.edit()
            .remove(ACTIVE_SESSION_KEY)
            .apply()
    }

    private fun UserEntity.toUserAccount(): UserAccount {
        return UserAccount(
            firstName = firstName,
            middleName = middleName,
            paternalSurname = paternalSurname,
            maternalSurname = maternalSurname,
            rut = rut,
            email = email,
            role = UserRole.valueOf(role),
            gender = runCatching { UserGender.valueOf(gender) }.getOrDefault(UserGender.Other),
            course = course.ifBlank { SchoolCourses.DEFAULT_STUDENT_COURSE },
            assignedCourse = assignedCourse,
            assignedSubject = assignedSubject
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "bitacora_session"
        const val ACTIVE_SESSION_KEY = "active_session_email"
    }
}
