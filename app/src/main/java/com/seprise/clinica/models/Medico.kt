package com.seprise.clinica.models

enum class Especialidad {
    CARDIOLOGIA,
    ORTOPEDIA,
    PEDIATRIA
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
