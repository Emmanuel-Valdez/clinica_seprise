package com.seprise.clinica.models

data class Paciente(
    val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val mail: String,
    val telefono: String,
    val direccion: String,
) {
    val fullName: String
        get() = "$nombre $apellido".trim()
}


