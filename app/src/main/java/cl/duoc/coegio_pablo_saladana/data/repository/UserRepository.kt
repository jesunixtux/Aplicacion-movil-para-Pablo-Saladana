package cl.duoc.coegio_pablo_saladana.data.repository

import android.content.Context
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.PasswordHasher
import cl.duoc.coegio_pablo_saladana.data.local.UserEntity
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
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
    ): UserAccount? {
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
        ) return null
        if (userDao.findByEmail(normalizedEmail) != null || userDao.findByRut(normalizedRut) != null) {
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
        return if (id == -1L) null else user.copy(id = id).toUserAccount()
    }

    suspend fun login(email: String, password: String): UserAccount? {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.findByEmail(normalizedEmail) ?: return null
        val verified = withContext(Dispatchers.Default) {
            PasswordHasher.verify(password, user.passwordHash)
        }
        if (!verified) return null
        // Una cuenta antigua se actualiza después de verificar correctamente su contraseña.
        if (PasswordHasher.isLegacyHash(user.passwordHash)) {
            val upgradedHash = withContext(Dispatchers.Default) { PasswordHasher.create(password) }
            userDao.updatePasswordHash(user.id, user.passwordHash, upgradedHash)
        }
        return user.toUserAccount()
    }

    suspend fun findAccountByEmail(email: String): UserAccount? {
        return userDao.findByEmail(email.trim().lowercase())?.toUserAccount()
    }

    suspend fun findAllStudents(): List<UserAccount> {
        return userDao.findAllStudents().map { it.toUserAccount() }
    }

    suspend fun findAllStudentsByCourse(course: String): List<UserAccount> {
        return userDao.findAllStudents().filter { it.course == course }.map { it.toUserAccount() }
    }

    suspend fun isCourseAvailable(course: String): Boolean {
        return userDao.findTeacherByAssignedCourse(course) == null
    }

    suspend fun assignCourseToTeacher(teacherEmail: String, course: String): Boolean {
        if (!isCourseAvailable(course)) return false
        val updated = userDao.updateAssignedCourse(teacherEmail, course)
        return updated > 0
    }

    fun saveSession(account: UserAccount) {
        preferences.edit()
            .putString(ACTIVE_SESSION_KEY, account.email)
            .apply()
    }

    suspend fun getActiveSession(): UserAccount? {
        val email = preferences.getString(ACTIVE_SESSION_KEY, null) ?: return null
        val account = userDao.findByEmail(email.trim().lowercase())?.toUserAccount()
        if (account == null) clearSession()
        return account
    }

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
            assignedCourse = assignedCourse
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "bitacora_session"
        const val ACTIVE_SESSION_KEY = "active_session_email"
    }
}
