package com.seprise.clinica.models

import java.time.LocalDate

data class LiquidacionHonorario(
    val id: Int = 0,
    val medicoId: Int,
    val fechaDesde: LocalDate,
    val fechaHasta: LocalDate,
    val fechaEmision: LocalDate = LocalDate.now(),
    val atenciones: List<Int>,
    val total: Double
)
