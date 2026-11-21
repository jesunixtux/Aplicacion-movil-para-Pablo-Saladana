package cl.duoc.coegio_pablo_saladana.data.remote.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AssignmentDto(val course: String? = null, val subject: String? = null)

data class RemoteUserDto(
    val id: Long = 0,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val course: String? = null,
    val active: Boolean = true,
    val assignments: List<AssignmentDto> = emptyList()
) {
    fun toAccount(): UserAccount? {
        if (id <= 0 || name.isNullOrBlank() || email.isNullOrBlank() || !active) return null
        val mappedRole = when (role) {
            "TEACHER" -> UserRole.Teacher
            "STUDENT" -> UserRole.Student
            else -> return null
        }
        val assignment = assignments.firstOrNull()
        val verifiedCourse = when (mappedRole) {
            UserRole.Student -> course?.trim()?.takeIf {
                it in cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses.STUDENT_COURSES
            } ?: return null
            UserRole.Teacher -> assignment?.course?.trim().orEmpty()
        }
        return UserAccount(
            firstName = name.trim(),
            middleName = "",
            paternalSurname = "",
            maternalSurname = "",
            rut = "",
            email = email.trim().lowercase(),
            role = mappedRole,
            gender = UserGender.Other,
            course = verifiedCourse,
            assignedCourse = assignment?.course,
            assignedSubject = assignment?.subject,
            remoteId = id
        )
    }
}

data class AuthResponseDto(
    val token: String = "",
    val expiresAt: Long = 0,
    val user: RemoteUserDto? = null
)

/** Guarda token e identidad cifrados con una clave no exportable de Android Keystore. */
class RemoteSessionStore(context: Context) : SessionTokenProvider {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val gson = Gson()

    @Synchronized
    fun save(response: AuthResponseDto) {
        require(response.token.isNotBlank() && response.expiresAt > System.currentTimeMillis())
        requireNotNull(response.user?.toAccount())
        preferences.edit().putString(SESSION, encrypt(gson.toJson(response))).commit()
    }

    @Synchronized
    fun currentUser(): UserAccount? {
        val response = read() ?: return null
        if (response.expiresAt <= System.currentTimeMillis()) {
            clear()
            return null
        }
        return response.user?.toAccount()
    }

    @Synchronized
    override fun currentToken(): String? {
        val response = read() ?: return null
        if (response.expiresAt <= System.currentTimeMillis()) {
            clear()
            return null
        }
        return response.token.takeIf(String::isNotBlank)
    }

    @Synchronized
    fun updateUser(user: RemoteUserDto) {
        val response = read() ?: return
        save(response.copy(user = user))
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(SESSION).commit()
    }

    private fun read(): AuthResponseDto? = runCatching {
        preferences.getString(SESSION, null)?.let(::decrypt)?.let {
            gson.fromJson(it, AuthResponseDto::class.java)
        }
    }.getOrElse {
        clear()
        null
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, IV_BYTES)))
        return cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size)).toString(Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES = "remote_session_secure"
        const val SESSION = "encrypted_session"
        const val KEY_ALIAS = "bitacora_remote_session_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}
