package cl.duoc.coegio_pablo_saladana.domain.model

/** Catálogo de cursos escolares disponibles en la aplicación. */
object SchoolCourses {
    const val GENERAL = "General"
    const val CUSTOM = "Asignación personalizada"
    const val DEFAULT_STUDENT_COURSE = "5° Básico A"

    val STUDENT_COURSES = listOf(
        "5° Básico A",
        "5° Básico B",
        "6° Básico A",
        "6° Básico B",
        "7° Básico A",
        "8° Básico A"
    )

    val NOTEBOOK_COURSES = STUDENT_COURSES
}

/** Asignaturas ficticias para el MVP (5.º a 8.º básico). */
object SchoolSubjects {
    val ALL = listOf(
        "Lenguaje y Comunicación",
        "Matemática",
        "Ciencias Naturales",
        "Historia y Geografía",
        "Inglés"
    )
    const val DEFAULT = "Lenguaje y Comunicación"
}
