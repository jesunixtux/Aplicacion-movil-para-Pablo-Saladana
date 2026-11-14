package cl.duoc.coegio_pablo_saladana.ui.navigation

import cl.duoc.coegio_pablo_saladana.domain.model.UserRole

/** Destinos que puede mostrar el contenedor principal de Compose. */
enum class AuthDestination {
    Welcome,
    Login,
    Register,
    StudentDashboard,
    StudentNotebook,
    StudentEntry,
    StudentHistory,
    TeacherDashboard,
    TeacherCreateNotebook,
    Profile
}

internal fun destinationForSession(
    savedDestination: AuthDestination?,
    role: UserRole?,
    hasNotebook: Boolean
): AuthDestination = when (role) {
    null -> when (savedDestination) {
        AuthDestination.Login, AuthDestination.Register -> savedDestination
        else -> AuthDestination.Welcome
    }
    UserRole.Student -> when (savedDestination) {
        AuthDestination.StudentDashboard, AuthDestination.StudentHistory, AuthDestination.Profile -> savedDestination
        AuthDestination.StudentNotebook, AuthDestination.StudentEntry ->
            if (hasNotebook) savedDestination else AuthDestination.StudentDashboard
        else -> AuthDestination.StudentDashboard
    }
    UserRole.Teacher -> when (savedDestination) {
        AuthDestination.TeacherDashboard, AuthDestination.TeacherCreateNotebook, AuthDestination.Profile -> savedDestination
        else -> AuthDestination.TeacherDashboard
    }
}
