package com.seprise.clinica.core

import com.seprise.clinica.models.Administrador
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Circuito
import com.seprise.clinica.models.Especialidad
import com.seprise.clinica.models.Estudio
import com.seprise.clinica.models.Insumo
import com.seprise.clinica.models.Medico
import com.seprise.clinica.models.Modalidad
import com.seprise.clinica.models.Paciente
import com.seprise.clinica.models.Servicio
import com.seprise.clinica.models.Turno
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Almacenes en memoria del TP1 (CU01..CU08). Los crea y congela dev1; cada lane
 * llena su propio store desde su Activity. Nadie más modifica este archivo en TP1.
 *
 * Elemento tipado como [Any] hasta que cada CU defina su modelo; se acota
 * por consenso (cambiar `mutableListOf<Any>()` por el tipo real cuando exista).
 */
object AppData {
    val PACIENTES = mutableListOf<Paciente>(
        Paciente(id = 1, nombre = "Ana", apellido = "López", dni = "35123456", mail = "ana@mail.com", telefono = "1144445555", direccion = "Av. Mayo 123"),
        Paciente(id = 2, nombre = "Roberto", apellido = "Fernández", dni = "28987654", mail = "roberto@mail.com", telefono = "1155556666", direccion = "Belgrano 456")
    )
    val ADMINISTRADORES = mutableListOf<Administrador>(
        Administrador(id = 1, nombre = "Laura", apellido = "Ríos", dni= "22333111"),
        Administrador(id = 2, nombre = "Camila", apellido = "Gómez", dni = "11222333"),
   )
    val MEDICOS = mutableListOf<Medico>(
        Medico(id = 1, nombre = "Carlos", apellido = "Pérez", especialidad = Especialidad.CARDIOLOGIA),
        Medico(id = 2, nombre = "María", apellido = "Gómez", especialidad = Especialidad.ORTOPEDIA),
        Medico(id = 3, nombre = "Juan", apellido = "Rodríguez", especialidad = Especialidad.PEDIATRIA)
    )
    val TURNOS = mutableListOf<Turno>()
    val AGENDAS = mutableListOf<Agenda>(
        Agenda(
            id = 3,
            circuito = Circuito.ESTUDIOS_CLINICOS,
            servicio = Servicio.RADIOGRAFIA,
            medicoId = 0,
            diasSemana = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            horaInicio = LocalTime.of(8, 0),
            horaFin = LocalTime.of(16, 0),
            duracionCita = 15,
            requiereAyuno = false,
            modalidad = Modalidad.CON_TURNO
        ),
        Agenda(
            id = 4,
            circuito = Circuito.ESTUDIOS_CLINICOS,
            servicio = Servicio.ANALISIS_LABORATORIO,
            medicoId = 0,
            diasSemana = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            horaInicio = LocalTime.of(7, 30),
            horaFin = LocalTime.of(11, 0),
            duracionCita = 10,
            requiereAyuno = true,
            modalidad = Modalidad.CON_TURNO
        )
    )

    val HONORARIOS = mutableListOf<Any>()
    val ATENCIONES = mutableListOf<Any>()


    val ESTUDIOS = mutableListOf<Estudio>()
    val INSUMOS = mutableListOf<Insumo>(Insumo(id = 1, nombre = "Jeringa", cantidad = 120),
        Insumo(id = 2, nombre = "Algodon", cantidad = 20),
        Insumo(id = 3, nombre = "Venda", cantidad = 100))

}
