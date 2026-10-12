package cl.duoc.coegio_pablo_saladana.data

enum class UserRole {
    Teacher,
    Student
}

data class UserAccount(
    val firstName: String,
    val middleName: String,
    val paternalSurname: String,
    val maternalSurname: String,
    val rut: String,
    val email: String,
    val role: UserRole,
    val passwordHash: String
) {
    val fullName: String
        get() = listOf(firstName, middleName, paternalSurname, maternalSurname)
            .filter(String::isNotBlank)
            .joinToString(" ")
}
