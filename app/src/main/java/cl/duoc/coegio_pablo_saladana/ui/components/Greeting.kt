package cl.duoc.coegio_pablo_saladana.ui.components

import cl.duoc.coegio_pablo_saladana.data.UserGender

/** Saludo personalizado según el género elegido en el registro. */
fun greetingFor(fullName: String, gender: UserGender): String {
    return when (gender) {
        UserGender.Male -> "Hola Sr. $fullName"
        UserGender.Female -> "Hola Sra. $fullName"
        UserGender.Other -> "Hola, $fullName"
    }
}
