package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import java.security.MessageDigest

class UserRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun register(
        firstName: String,
        middleName: String,
        paternalSurname: String,
        maternalSurname: String,
        rut: String,
        email: String,
        password: String,
        role: UserRole
    ): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val normalizedRut = rut.trim().uppercase()
        if (preferences.contains(userKey(normalizedEmail)) || preferences.contains(rutKey(normalizedRut))) {
            return false
        }

        preferences.edit()
            .putString(userKey(normalizedEmail), firstName.trim())
            .putString(middleNameKey(normalizedEmail), middleName.trim())
            .putString(paternalSurnameKey(normalizedEmail), paternalSurname.trim())
            .putString(maternalSurnameKey(normalizedEmail), maternalSurname.trim())
            .putString(rutKey(normalizedRut), normalizedEmail)
            .putString(userRutKey(normalizedEmail), normalizedRut)
            .putString(roleKey(normalizedEmail), role.name)
            .putString(passwordKey(normalizedEmail), hash(password))
            .apply()

        return true
    }

    fun login(email: String, password: String): UserAccount? {
        val normalizedEmail = email.trim().lowercase()
        val firstName = preferences.getString(userKey(normalizedEmail), null) ?: return null
        val storedHash = preferences.getString(passwordKey(normalizedEmail), null) ?: return null
        if (storedHash != hash(password)) return null

        val roleName = preferences.getString(roleKey(normalizedEmail), UserRole.Student.name)
        return UserAccount(
            firstName = firstName,
            middleName = preferences.getString(middleNameKey(normalizedEmail), "") ?: "",
            paternalSurname = preferences.getString(paternalSurnameKey(normalizedEmail), "") ?: "",
            maternalSurname = preferences.getString(maternalSurnameKey(normalizedEmail), "") ?: "",
            rut = preferences.getString(userRutKey(normalizedEmail), "") ?: "",
            email = normalizedEmail,
            role = UserRole.valueOf(roleName ?: UserRole.Student.name),
            passwordHash = storedHash
        )
    }

    private fun hash(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun userKey(email: String) = "user_name_$email"
    private fun middleNameKey(email: String) = "user_middle_name_$email"
    private fun paternalSurnameKey(email: String) = "user_paternal_surname_$email"
    private fun maternalSurnameKey(email: String) = "user_maternal_surname_$email"
    private fun userRutKey(email: String) = "user_rut_$email"
    private fun rutKey(rut: String) = "rut_owner_$rut"
    private fun roleKey(email: String) = "user_role_$email"
    private fun passwordKey(email: String) = "user_password_$email"

    private companion object {
        const val PREFERENCES_NAME = "bitacora_users"
    }
}
