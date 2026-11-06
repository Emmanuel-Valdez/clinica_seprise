
package com.seprise.clinica.turnos

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Turno
import java.time.LocalDate

object TurnosManager {

    fun crearTurno(turno: Turno): Boolean {
        if (AppData.TURNOS.any { it.id == turno.id }) {
            return false
        }
        AppData.TURNOS.add(turno)
        return true
    }

    fun modificarTurno(turno: Turno): Boolean {
        val indice = AppData.TURNOS.indexOfFirst { it.id == turno.id }
        if (indice == -1) return false
        AppData.TURNOS[indice] = turno
        return true
    }

    fun eliminarTurno(id: Int): Boolean {
        return AppData.TURNOS.removeAll { it.id == id }
    }

    fun obtenerTurnos(): List<Turno> {
        return AppData.TURNOS.toList()
    }

    fun obtenerTurnoPorId(id: Int): Turno? {
        return AppData.TURNOS.find { it.id == id }
    }

    fun obtenerTurnosPorMedico(medicoId: Int): List<Turno> {
        return AppData.TURNOS.filter { it.medicoId == medicoId }
    }

    /** Primer turno de hoy (por hora) para médico y paciente, en cualquier estado. */
    fun buscarTurnoDeHoy(medicoId: Int, pacienteId: Int, fecha: LocalDate = LocalDate.now()): Turno? {
        return AppData.TURNOS
            .filter { it.medicoId == medicoId && it.pacienteId == pacienteId && it.fecha == fecha }
            .minByOrNull { it.hora }
    }

    /** Primer turno RESERVADO de hoy (por hora) para médico y paciente. */
    fun buscarTurnoReservadoDeHoy(medicoId: Int, pacienteId: Int, fecha: LocalDate = LocalDate.now()): Turno? {
        return AppData.TURNOS
            .filter {
                it.medicoId == medicoId && it.pacienteId == pacienteId &&
                    it.fecha == fecha && it.estado == EstadoTurno.RESERVADO
            }
            .minByOrNull { it.hora }
    }

    /** Marca ATENDIDO solo si el turno sigue RESERVADO. */
    fun marcarAtendido(turnoId: Int): Boolean {
        val indice = AppData.TURNOS.indexOfFirst { it.id == turnoId }
        if (indice == -1) return false
        if (AppData.TURNOS[indice].estado != EstadoTurno.RESERVADO) return false
        AppData.TURNOS[indice] = AppData.TURNOS[indice].copy(estado = EstadoTurno.ATENDIDO)
        return true
    }

    fun obtenerAgendaPorMedico(medicoId: Int): Agenda? {
        return AppData.AGENDAS.find { it.medicoId == medicoId }
    }
}

