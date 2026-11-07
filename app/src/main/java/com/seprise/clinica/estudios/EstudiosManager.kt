package com.seprise.clinica.estudios

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.EstadoEstudio
import com.seprise.clinica.models.Estudio
import java.time.LocalDate

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

    fun obtenerEstudiosActivos(): List<Estudio> {
        return AppData.ESTUDIOS.filter { it.estado != EstadoEstudio.ANULADO }
    }

    fun obtenerEstudiosPorPaciente(pacienteId: Int): List<Estudio> {
        return AppData.ESTUDIOS.filter { it.pacienteId == pacienteId && it.estado != EstadoEstudio.ANULADO }
    }

    fun anularEstudio(id: Int, motivo: String): Boolean {
        if (motivo.isBlank()) return false
        val indice = AppData.ESTUDIOS.indexOfFirst { it.id == id }
        if (indice == -1) return false
        val actual = AppData.ESTUDIOS[indice]
        AppData.ESTUDIOS[indice] = actual.copy(estado = EstadoEstudio.ANULADO, motivoAnulacion = motivo)
        return true
    }

    fun marcarComoInformado(id: Int): Boolean {
        val indice = AppData.ESTUDIOS.indexOfFirst { it.id == id }
        if (indice == -1) return false
        val actual = AppData.ESTUDIOS[indice]
        AppData.ESTUDIOS[indice] = actual.copy(estado = EstadoEstudio.INFORMADO)
        return true
    }

    fun marcarComoRetirado(id: Int): String {
        val indice = AppData.ESTUDIOS.indexOfFirst { it.id == id }
        if (indice == -1) return ""
        val actual = AppData.ESTUDIOS[indice]
        val nroComprobante = "CR-${actual.id}-${System.currentTimeMillis() / 1000}"
        AppData.ESTUDIOS[indice] = actual.copy(
            estado = EstadoEstudio.RETIRADO,
            comprobanteRetirado = true,
            fechaRetiro = LocalDate.now(),
            nroComprobante = nroComprobante
        )
        return nroComprobante
    }
}
