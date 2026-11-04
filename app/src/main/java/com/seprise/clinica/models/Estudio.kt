package com.seprise.clinica.models

import java.time.LocalDate
import java.time.LocalTime


data class `Estudio`(
    val id: Int = 0,
    val nombre: String,
    val pacienteId: Int,
    val agendaId: Int,
    val fecha: LocalDate,
    val hora: LocalTime,
    val duracion: Int,
    val insumos: List<Insumo>
)