package com.seprise.clinica.estudios

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Estudio

object EstudiosManager {

    fun crearEstudio(estudio: Estudio): Boolean {
        if (AppData.ESTUDIOS.any { it.id == estudio.id }) {
            return false
        }
        AppData.ESTUDIOS.add(estudio)
        return true
    }

    fun modificarEstudio(estudio: Estudio): Boolean {
        val indice = AppData.ESTUDIOS.indexOfFirst { it.id == estudio.id }
        if (indice == -1) return false
        AppData.ESTUDIOS[indice] = estudio
        return true
    }

    fun eliminarEstudio(id: Int): Boolean {
        return AppData.ESTUDIOS.removeAll { it.id == id }
    }

    fun obtenerEstudios(): List<Estudio> {
        return AppData.ESTUDIOS.toList()
    }

    fun obtenerEstudiosPorPaciente(pacienteId: Int): List<Estudio> {
        return AppData.ESTUDIOS.filter { it.pacienteId == pacienteId }
    }
}
