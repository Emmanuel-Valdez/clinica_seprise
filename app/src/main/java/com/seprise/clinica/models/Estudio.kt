package com.seprise.clinica.models

import java.time.LocalDate
import java.time.LocalTime

data class Estudio(
    val id: Int = 0,
    val nombre: String,
    val pacienteId: Int,
    val agendaId: Int = 0,
    val fecha: LocalDate = LocalDate.now(),
    val hora: LocalTime = LocalTime.now(),
    val duracion: Int = 15,
    val insumos: List<Insumo> = emptyList(),
    val observaciones: String = "",
    val nroOrden: String = "",
    val autorizacionOS: String = "",
    val estado: EstadoEstudio = EstadoEstudio.PENDIENTE,
    val motivoAnulacion: String = "",
    val comprobanteRetirado: Boolean = false,
    val fechaRetiro: LocalDate? = null,
    val nroComprobante: String = ""
)
