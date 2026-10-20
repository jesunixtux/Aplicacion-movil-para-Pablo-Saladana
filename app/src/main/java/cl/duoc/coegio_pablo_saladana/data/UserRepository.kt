package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Centraliza cuentas, validaciones de registro y la sesión persistida localmente. */
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
            password.length < 8
        ) return null
        if (userDao.findByEmail(normalizedEmail) != null || userDao.findByRut(normalizedRut) != null) {
            return null
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
            passwordHash = passwordHash
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
            gender = runCatching { UserGender.valueOf(gender) }.getOrDefault(UserGender.Other)
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "bitacora_session"
        const val ACTIVE_SESSION_KEY = "active_session_email"
    }
}
