package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import java.security.MessageDigest

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
        gender: UserGender
    ): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val normalizedRut = rut.trim().uppercase()
        if (userDao.findByEmail(normalizedEmail) != null || userDao.findByRut(normalizedRut) != null) {
            return false
        }

        val user = UserEntity(
            firstName = firstName.trim(),
            middleName = middleName.trim(),
            paternalSurname = paternalSurname.trim(),
            maternalSurname = maternalSurname.trim(),
            rut = normalizedRut,
            email = normalizedEmail,
            role = role.name,
            gender = gender.name,
            passwordHash = hash(password)
        )
        return userDao.insert(user) != -1L
    }

    suspend fun login(email: String, password: String): UserAccount? {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.findByEmail(normalizedEmail) ?: return null
        if (user.passwordHash != hash(password)) return null
        return user.toUserAccount()
    }

    fun saveSession(account: UserAccount) {
        preferences.edit()
            .putString(ACTIVE_SESSION_KEY, account.email)
            .apply()
    }

    suspend fun getActiveSession(): UserAccount? {
        val email = preferences.getString(ACTIVE_SESSION_KEY, null) ?: return null
        return userDao.findByEmail(email.trim().lowercase())?.toUserAccount()
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
            passwordHash = passwordHash
        )
    }

    private fun hash(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val PREFERENCES_NAME = "bitacora_session"
        const val ACTIVE_SESSION_KEY = "active_session_email"
    }
}
