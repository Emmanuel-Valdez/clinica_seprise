
package com.seprise.clinica.insumos

import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Insumo

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

}

