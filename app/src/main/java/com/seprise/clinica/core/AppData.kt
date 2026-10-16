package com.seprise.clinica.core

/**
 * Almacenes en memoria del TP1 (CU01..CU08). Los crea y congela dev1; cada lane
 * llena su propio store desde su Activity. Nadie más modifica este archivo en TP1.
 *
 * Elemento tipado como [Any] hasta que cada CU defina su modelo; se acota
 * por consenso (cambiar `mutableListOf<Any>()` por el tipo real cuando exista).
 */
object AppData {
    val PACIENTES = mutableListOf<Any>()
    val TURNOS = mutableListOf<Any>()
    val AGENDAS = mutableListOf<Any>()
    val ATENCIONES = mutableListOf<Any>()
    val ESTUDIOS = mutableListOf<Any>()
    val INSUMOS = mutableListOf<Any>()
    val HONORARIOS = mutableListOf<Any>()
}
