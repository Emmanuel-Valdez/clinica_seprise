package com.seprise.clinica.models

import java.time.LocalDate

data class ConsumoInsumo(
    val id: Int = 0,
    val insumoId: Int,
    val cantidad: Int,
    val prestacionId: Int,
    val tipoPrestacion: String,
    val fecha: LocalDate = LocalDate.now(),
)
