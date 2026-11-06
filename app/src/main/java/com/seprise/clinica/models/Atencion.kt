package com.seprise.clinica.models

import java.time.LocalDate
import java.time.LocalTime

data class Atencion(
    val id: Int = 0,
    val medicoId: Int,
    val pacienteId: Int,
    val turnoId: Int,
    val fecha: LocalDate = LocalDate.now(),
    val hora: LocalTime = LocalTime.now(),
    val duracion: Int = 15,
    val insumos: List<Insumo> = emptyList(),
    val observaciones: String = "",
    val valorConsulta: Double
)
