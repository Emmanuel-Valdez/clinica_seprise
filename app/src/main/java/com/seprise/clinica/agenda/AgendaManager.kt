
package com.seprise.clinica.agenda

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Agenda

object AgendaManager {

    fun crearAgenda(agenda: Agenda): Boolean {
        // Regla: Solo se permite cargar una agenda por médico
        if (AppData.AGENDAS.any { it.id == agenda.id || it.medicoId == agenda.medicoId }) {
            return false
        }

        AppData.AGENDAS.add(agenda)
        return true
    }

    fun modificarAgenda(agenda: Agenda): Boolean {
        val indice = AppData.AGENDAS.indexOfFirst {
            it.id == agenda.id
        }

        if (indice == -1) {
            return false
        }

        AppData.AGENDAS[indice] = agenda
        return true
    }

    fun eliminarAgenda(id: Int): Boolean {
        val tieneTurnos = AppData.TURNOS.any {
            it.agendaId == id
        }

        if (tieneTurnos) {
            return false
        }

        return AppData.AGENDAS.removeAll {
            it.id == id
        }
    }

    fun obtenerAgendas(): List<Agenda> {
        return AppData.AGENDAS.toList()
    }

    fun obtenerAgendaPorId(id: Int): Agenda? {
        return AppData.AGENDAS.find { it.id == id }
    }

    fun obtenerAgendaPorMedico(medicoId: Int): Agenda? {
        return AppData.AGENDAS.find { it.medicoId == medicoId }
    }
}

