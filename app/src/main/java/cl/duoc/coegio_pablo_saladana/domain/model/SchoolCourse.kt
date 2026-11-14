package cl.duoc.coegio_pablo_saladana.domain.model

/** Catálogo de cursos escolares disponibles en la aplicación. */
object SchoolCourses {
    const val GENERAL = "General"
    const val CUSTOM = "Asignación personalizada"
    const val DEFAULT_STUDENT_COURSE = "1° Medio A"

    val STUDENT_COURSES = listOf(
        "1° Medio A",
        "1° Medio B",
        "2° Medio A",
        "2° Medio B",
        "3° Medio A",
        "4° Medio A"
    )

    val NOTEBOOK_COURSES = listOf(GENERAL, CUSTOM) + STUDENT_COURSES
}
