package com.seprise.clinica.models

data class Administrador(
    val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val dni: String,
) {
    val fullName: String
        get() = "$nombre $apellido".trim()
}
