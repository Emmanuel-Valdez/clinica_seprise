package com.seprise.clinica.models

import java.time.DayOfWeek
import java.time.LocalTime

enum class Circuito {
    CONSULTORIOS_EXTERNOS,
    ESTUDIOS_CLINICOS
}
enum class Modalidad {
    CON_TURNO,
    ORDEN_LLEGADA
}

enum class Servicio {
    // Especialidades médicas
    CARDIOLOGIA,
    ORTOPEDIA,
    PEDIATRIA,
    CLINICA_MEDICA,

    // Estudios clínicos (a agregar)
    ECOGRAFIA,
    RADIOGRAFIA,
    ANALISIS_LABORATORIO
}

data class Agenda(
    val id: Int = 0,
    val circuito: Circuito = Circuito.CONSULTORIOS_EXTERNOS,
    val servicio: Servicio = Servicio.CLINICA_MEDICA,
    val medicoId: Int,
    val diasSemana: List<DayOfWeek>,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val duracionCita: Int,
    val requiereAyuno: Boolean,
    val modalidad: Modalidad = Modalidad.CON_TURNO,
)
