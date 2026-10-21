package com.seprise.clinica.models

import java.time.LocalDate
import java.time.LocalTime

enum class EstadoTurno {
    RESERVADO,
    CANCELADO,
    ATENDIDO,
    EN_ESPERA
}

data class Turno(
    val id: Int = 0,
    val pacienteId: Int,
    val agendaId: Int,
    val fecha: LocalDate,
    val hora: LocalTime,
    val duracion: Int,
    val estado: EstadoTurno = EstadoTurno.RESERVADO,
    val esSobreturno: Boolean = false
)