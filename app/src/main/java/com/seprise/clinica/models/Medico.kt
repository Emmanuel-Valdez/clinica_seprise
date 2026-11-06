package com.seprise.clinica.models

enum class Especialidad {
    CARDIOLOGIA,
    ORTOPEDIA,
    PEDIATRIA,
    FISIOTERAPIA,
    KINESIOLOGIA,
    SALUD_MENTAL
}

/** Duración de turno sugerida según especialidad: 15 general, 25 fisio/kinesio, 30 salud mental. */
fun Especialidad.duracionSugerida(): Int = when (this) {
    Especialidad.FISIOTERAPIA, Especialidad.KINESIOLOGIA -> 25
    Especialidad.SALUD_MENTAL -> 30
    else -> 15
}

data class Medico(
    val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val especialidad: Especialidad = Especialidad.ORTOPEDIA,
) {
    val fullName: String
        get() = "$nombre $apellido".trim()
}
