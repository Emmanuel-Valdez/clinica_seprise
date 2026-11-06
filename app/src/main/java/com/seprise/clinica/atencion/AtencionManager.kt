package com.seprise.clinica.atencion

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Atencion

object AtencionManager {

    fun registrarAtencion(atencion: Atencion): Boolean {
        AppData.ATENCIONES.add(atencion)
        return true
    }

    fun obtenerAtenciones(): List<Atencion> {
        return AppData.ATENCIONES.toList()
    }

    fun obtenerAtencionesPorMedico(medicoId: Int): List<Atencion> {
        return AppData.ATENCIONES.filter { it.medicoId == medicoId }
    }

    fun obtenerAtencionesPorPaciente(pacienteId: Int): List<Atencion> {
        return AppData.ATENCIONES.filter { it.pacienteId == pacienteId }
    }
}
