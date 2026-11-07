
package com.seprise.clinica.insumos

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.ConsumoInsumo
import com.seprise.clinica.models.Insumo
import java.time.LocalDate

data class ConsumoPrestacion(
    val tipoPrestacion: String,
    val prestacionId: Int,
    val fecha: LocalDate,
    val detalle: String,
    val insumos: List<Pair<String, Int>>,
)

object InsumosManager {

    fun crearInsumo(insumo: Insumo): Boolean {
        if (AppData.INSUMOS.any { it.id == insumo.id }) {
            return false
        }
        AppData.INSUMOS.add(insumo)
        return true
    }

    fun modificarInsumo(insumo: Insumo): Boolean {
        val indice = AppData.INSUMOS.indexOfFirst { it.id == insumo.id }
        if (indice == -1) return false
        AppData.INSUMOS[indice] = insumo
        return true
    }

    fun eliminarInsumo(id: Int): Boolean {
        return AppData.INSUMOS.removeAll { it.id == id }
    }

    fun obtenerInsumos(): List<Insumo> {
        return AppData.INSUMOS.toList()
    }

    fun obtenerInsumoPorId(id: Int): Insumo? {
        return AppData.INSUMOS.find { it.id == id }
    }

    fun actualizarEstadoVencimiento() {
        val hoy = LocalDate.now()
        for (i in AppData.INSUMOS.indices) {
            if (AppData.INSUMOS[i].fechaVencimiento < hoy) {
                AppData.INSUMOS[i] = AppData.INSUMOS[i].copy(activo = false)
            }
        }
    }

    fun consumirInsumo(insumoId: Int, cantidad: Int, prestacionId: Int, tipoPrestacion: String): Boolean {
        val insumo = AppData.INSUMOS.find { it.id == insumoId } ?: return false
        if (!insumo.activo) return false

        AppData.INSUMOS[AppData.INSUMOS.indexOf(insumo)] = insumo.copy(cantidad = insumo.cantidad - cantidad)

        val consumo = ConsumoInsumo(
            id = (AppData.CONSUMOS.maxOfOrNull { it.id } ?: 0) + 1,
            insumoId = insumoId,
            cantidad = cantidad,
            prestacionId = prestacionId,
            tipoPrestacion = tipoPrestacion
        )
        AppData.CONSUMOS.add(consumo)
        return true
    }

    fun obtenerConsumosPorPrestacion(tipo: String? = null): List<ConsumoPrestacion> {
        val consumosAgrupados = AppData.CONSUMOS
            .filter { tipo == null || it.tipoPrestacion == tipo }
            .groupBy { it.tipoPrestacion to it.prestacionId }

        return consumosAgrupados.map { (key, consumos) ->
            val (tipoPrest, idPrest) = key
            val primerConsumo = consumos.first()

            val detalle = when (tipoPrest) {
                "atencion" -> {
                    val atencion = AppData.ATENCIONES.find { it.id == idPrest }
                    val paciente = atencion?.let { AppData.PACIENTES.find { p -> p.id == it.pacienteId } }
                    val medico = atencion?.let { AppData.MEDICOS.find { m -> m.id == it.medicoId } }
                    val base = "Atención #${idPrest} · ${atencion?.fecha} · Dr/a. ${medico?.fullName ?: "?"}"
                    if (paciente != null) "$base · Pac: ${paciente.fullName}" else base
                }
                "estudio" -> {
                    val estudio = AppData.ESTUDIOS.find { it.id == idPrest }
                    val paciente = estudio?.let { AppData.PACIENTES.find { p -> p.id == it.pacienteId } }
                    val base = "Estudio #${idPrest} · ${estudio?.fecha} · ${estudio?.nombre}"
                    if (paciente != null) "$base · Pac: ${paciente.fullName}" else base
                }
                else -> "${tipoPrest} #${idPrest}"
            }

            ConsumoPrestacion(
                tipoPrestacion = tipoPrest,
                prestacionId = idPrest,
                fecha = primerConsumo.fecha,
                detalle = detalle,
                insumos = consumos.map { consumo ->
                    val insumo = AppData.INSUMOS.find { it.id == consumo.insumoId }
                    "${insumo?.nombre ?: "?"}" to consumo.cantidad
                }
            )
        }.sortedByDescending { it.fecha }
    }

}

