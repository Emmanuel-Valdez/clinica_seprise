package com.seprise.clinica.atencion

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Atencion
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.turnos.TurnosManager

object AtencionManager {

    fun registrarAtencion(atencion: Atencion): Boolean {
        if (atencion.diagnostico.isBlank()) return false
        if (atencion.duracion <= 0) return false
        if (atencion.valorConsulta < 0) return false
        if (atencion.turnoId != 0) {
            val turno = AppData.TURNOS.find { it.id == atencion.turnoId } ?: return false
            if (turno.estado != EstadoTurno.RESERVADO) return false
            if (AppData.ATENCIONES.any { it.turnoId == atencion.turnoId }) return false
        }

        AppData.ATENCIONES.add(atencion)
        if (atencion.turnoId != 0) {
            TurnosManager.marcarAtendido(atencion.turnoId)
        }
        return true
    }

    fun obtenerAtenciones(): List<Atencion> {
        return AppData.ATENCIONES.toList()
    }

    fun obtenerAtencionesPorMedico(medicoId: Int): List<Atencion> {
        return AppData.ATENCIONES.filter { it.medicoId == medicoId }
    }

    /** HC del paciente, de la atención más reciente a la más antigua. */
    fun obtenerAtencionesPorPaciente(pacienteId: Int): List<Atencion> {
        return AppData.ATENCIONES
            .filter { it.pacienteId == pacienteId }
            .sortedWith(compareByDescending<Atencion> { it.fecha }.thenByDescending { it.hora })
    }
}
