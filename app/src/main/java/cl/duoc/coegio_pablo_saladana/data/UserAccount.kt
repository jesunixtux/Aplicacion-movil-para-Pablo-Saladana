package cl.duoc.coegio_pablo_saladana.data

/** Roles disponibles en la aplicación. */
enum class UserRole {
    Teacher,
    Student
}

/** Dato usado para personalizar el saludo sin cambiar los permisos. */
enum class UserGender {
    Male,
    Female,
    Other
}

/** Modelo de usuario que usa la interfaz; nunca contiene la contraseña ni su hash. */
data class UserAccount(
    val firstName: String,
    val middleName: String,
    val paternalSurname: String,
    val maternalSurname: String,
    val rut: String,
    val email: String,
    val role: UserRole,
    val gender: UserGender
) {
    val fullName: String
        get() = listOf(firstName, middleName, paternalSurname, maternalSurname)
            .filter(String::isNotBlank)
            .joinToString(" ")
}
