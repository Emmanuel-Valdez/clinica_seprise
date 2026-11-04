
package com.seprise.clinica.turnos

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Turno

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

    fun obtenerAgendaPorMedico(medicoId: Int): Agenda? {
        return AppData.AGENDAS.find { it.medicoId == medicoId }
    }
}

