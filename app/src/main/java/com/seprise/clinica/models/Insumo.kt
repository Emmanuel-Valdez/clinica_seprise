package com.seprise.clinica.models

import java.time.LocalDate

data class Insumo(
    val id: Int = 0,
    val nombre: String,
    val cantidad: Int = 1,
    val fechaVencimiento: LocalDate = LocalDate.now().plusYears(100),
    val activo: Boolean = true,
)