package com.seprise.clinica.honorarios

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Atencion
import com.seprise.clinica.models.LiquidacionHonorario
import java.time.LocalDate

object HonorariosManager {

    /** Atenciones del médico en el período inclusivo que todavía no fueron liquidadas. */
    fun obtenerAtencionesPendientes(medicoId: Int, desde: LocalDate, hasta: LocalDate): List<Atencion> {
        if (desde.isAfter(hasta)) return emptyList()
        val liquidadas = AppData.HONORARIOS.flatMap { it.atenciones }.toSet()
        return AppData.ATENCIONES
            .filter {
                it.medicoId == medicoId &&
                    !it.fecha.isBefore(desde) &&
                    !it.fecha.isAfter(hasta) &&
                    it.id !in liquidadas
            }
            .sortedWith(compareBy<Atencion> { it.fecha }.thenBy { it.hora })
    }

    fun calcularTotal(atenciones: List<Atencion>): Double = atenciones.sumOf { it.valorConsulta }

    /** Registra la liquidación; devuelve null si el período o el médico no son válidos o no hay atenciones pendientes. */
    fun registrarLiquidacion(medicoId: Int, desde: LocalDate, hasta: LocalDate): LiquidacionHonorario? {
        if (desde.isAfter(hasta)) return null
        if (AppData.MEDICOS.none { it.id == medicoId }) return null
        val pendientes = obtenerAtencionesPendientes(medicoId, desde, hasta)
        if (pendientes.isEmpty()) return null

        val liquidacion = LiquidacionHonorario(
            id = (AppData.HONORARIOS.maxOfOrNull { it.id } ?: 0) + 1,
            medicoId = medicoId,
            fechaDesde = desde,
            fechaHasta = hasta,
            fechaEmision = LocalDate.now(),
            atenciones = pendientes.map { it.id },
            total = calcularTotal(pendientes)
        )
        AppData.HONORARIOS.add(liquidacion)
        return liquidacion
    }

    fun obtenerLiquidaciones(): List<LiquidacionHonorario> = AppData.HONORARIOS.toList()
}
