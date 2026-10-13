package cl.duoc.coegio_pablo_saladana.data

enum class UserRole {
    Teacher,
    Student
}

enum class UserGender {
    Male,
    Female,
    Other
}

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
