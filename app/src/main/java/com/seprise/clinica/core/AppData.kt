package com.seprise.clinica.core

import com.seprise.clinica.models.Administrador
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Especialidad
import com.seprise.clinica.models.Medico
import com.seprise.clinica.models.Paciente
import com.seprise.clinica.models.Turno

/**
 * Almacenes en memoria del TP1 (CU01..CU08). Los crea y congela dev1; cada lane
 * llena su propio store desde su Activity. Nadie más modifica este archivo en TP1.
 *
 * Elemento tipado como [Any] hasta que cada CU defina su modelo; se acota
 * por consenso (cambiar `mutableListOf<Any>()` por el tipo real cuando exista).
 */
object AppData {
    val PACIENTES = mutableListOf<Paciente>()
    val ADMINISTRADORES = mutableListOf<Administrador>()
    val MEDICOS = mutableListOf<Medico>(
        Medico(id = 1, nombre = "Carlos", apellido = "Pérez", especialidad = Especialidad.CARDIOLOGIA),
        Medico(id = 2, nombre = "María", apellido = "Gómez", especialidad = Especialidad.ORTOPEDIA),
        Medico(id = 3, nombre = "Juan", apellido = "Rodríguez", especialidad = Especialidad.PEDIATRIA)
    )
    val TURNOS = mutableListOf<Turno>()
    val AGENDAS = mutableListOf<Agenda>()
    val ATENCIONES = mutableListOf<Any>()
    val ESTUDIOS = mutableListOf<Any>()
    val INSUMOS = mutableListOf<Any>()
    val HONORARIOS = mutableListOf<Any>()
}
