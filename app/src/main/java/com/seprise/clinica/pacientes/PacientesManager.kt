package com.seprise.clinica.pacientes

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Paciente

object PacientesManager {

    fun crearPaciente(paciente: Paciente): Boolean {
        if (AppData.PACIENTES.any { it.id == paciente.id }) {
            return false
        }
        val conEmpadronamiento = if (paciente.empadronamiento.isBlank()) {
            paciente.copy(empadronamiento = "EMP-${paciente.id}")
        } else {
            paciente
        }
        AppData.PACIENTES.add(conEmpadronamiento)
        return true
    }

    fun modificarPaciente(paciente: Paciente): Boolean {
        val indice = AppData.PACIENTES.indexOfFirst { it.id == paciente.id }
        if (indice == -1) return false
        AppData.PACIENTES[indice] = paciente
        return true
    }

    fun inhabilitarPaciente(id: Int): Boolean {
        val indice = AppData.PACIENTES.indexOfFirst { it.id == id }
        if (indice == -1) return false
        val paciente = AppData.PACIENTES[indice]
        AppData.PACIENTES[indice] = paciente.copy(activo = false)
        return true
    }

    fun habilitarPaciente(id: Int): Boolean {
        val indice = AppData.PACIENTES.indexOfFirst { it.id == id }
        if (indice == -1) return false
        val paciente = AppData.PACIENTES[indice]
        AppData.PACIENTES[indice] = paciente.copy(activo = true)
        return true
    }

    fun eliminarPaciente(id: Int): Boolean {
        return AppData.PACIENTES.removeAll { it.id == id }
    }

    fun obtenerPaciente(): List<Paciente> {
        return AppData.PACIENTES.toList()
    }

    fun obtenerPacientePorId(id: Int): Paciente? {
        return AppData.PACIENTES.find { it.id == id }
    }
}
