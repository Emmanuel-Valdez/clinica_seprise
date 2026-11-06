package com.seprise.clinica.core

import com.seprise.clinica.models.Administrador
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Atencion
import com.seprise.clinica.models.Circuito
import com.seprise.clinica.models.Especialidad
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Estudio
import com.seprise.clinica.models.Insumo
import com.seprise.clinica.models.Medico
import com.seprise.clinica.models.Modalidad
import com.seprise.clinica.models.Paciente
import com.seprise.clinica.models.Servicio
import com.seprise.clinica.models.Turno
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Almacenes en memoria del TP1 (CU01..CU08). Los crea y congela dev1; cada lane
 * llena su propio store desde su Activity. Nadie más modifica este archivo en TP1.
 *
 * Elemento tipado como [Any] hasta que cada CU defina su modelo; se acota
 * por consenso (cambiar `mutableListOf<Any>()` por el tipo real cuando exista).
 */
object AppData {
    // ponytail: seeding mock de demo en memoria; reemplazar por backend y borrar este bloque.
    private val hoy = LocalDate.now()
    private val manana = hoy.plusDays(1)
    private val todosLosDias = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    val PACIENTES = mutableListOf(
        Paciente(id = 1, nombre = "Ana", apellido = "López", dni = "35123456", mail = "ana@mail.com", telefono = "1144445555", direccion = "Av. Mayo 123", empadronamiento = "EMP-1"),
        Paciente(id = 2, nombre = "Roberto", apellido = "Fernández", dni = "28987654", mail = "roberto@mail.com", telefono = "1155556666", direccion = "Belgrano 456", empadronamiento = "EMP-2"),
        Paciente(id = 3, nombre = "Lucía", apellido = "Martínez", dni = "41234567", mail = "lucia@mail.com", telefono = "1166667777", direccion = "Sarmiento 789", empadronamiento = "EMP-3"),
        Paciente(id = 4, nombre = "Diego", apellido = "Sosa", dni = "32765432", mail = "diego@mail.com", telefono = "1177778888", direccion = "Mitre 321", empadronamiento = "EMP-4"),
        Paciente(id = 5, nombre = "Valentina", apellido = "Romero", dni = "38901234", mail = "valentina@mail.com", telefono = "1188889999", direccion = "Rivadavia 654", empadronamiento = "EMP-5"),
        Paciente(id = 6, nombre = "Martín", apellido = "Álvarez", dni = "29543210", mail = "martin@mail.com", telefono = "1199990000", direccion = "San Martín 987", empadronamiento = "EMP-6"),
        Paciente(id = 7, nombre = "Sofía", apellido = "Benítez", dni = "40111222", mail = "sofia@mail.com", telefono = "1100001111", direccion = "Alsina 147", empadronamiento = "EMP-7"),
        Paciente(id = 8, nombre = "Nicolás", apellido = "Ferreyra", dni = "36444555", mail = "nicolas@mail.com", telefono = "1111112222", direccion = "Moreno 258", empadronamiento = "EMP-8"),
        Paciente(id = 9, nombre = "Camila", apellido = "Duarte", dni = "42333444", mail = "camila@mail.com", telefono = "1122223333", direccion = "Urquiza 369", empadronamiento = "EMP-9"),
        Paciente(id = 10, nombre = "Tomás", apellido = "Rivas", dni = "31666777", mail = "tomas@mail.com", telefono = "1133334444", direccion = "9 de Julio 741", activo = false, empadronamiento = "EMP-10")
    )

    val ADMINISTRADORES = mutableListOf<Administrador>(
        Administrador(id = 1, nombre = "Laura", apellido = "Ríos", dni = "22333111"),
        Administrador(id = 2, nombre = "Camila", apellido = "Gómez", dni = "11222333"),
    )

    val MEDICOS = mutableListOf<Medico>(
        Medico(id = 1, nombre = "Carlos", apellido = "Pérez", especialidad = Especialidad.CARDIOLOGIA),
        Medico(id = 2, nombre = "María", apellido = "Gómez", especialidad = Especialidad.ORTOPEDIA),
        Medico(id = 3, nombre = "Juan", apellido = "Rodríguez", especialidad = Especialidad.PEDIATRIA)
    )

    val TURNOS = mutableListOf(
        Turno(id = 1, pacienteId = 1, medicoId = 1, agendaId = 1, fecha = hoy, hora = LocalTime.of(9, 0), duracion = 15),
        Turno(id = 2, pacienteId = 3, medicoId = 1, agendaId = 1, fecha = hoy, hora = LocalTime.of(9, 15), duracion = 15),
        Turno(id = 3, pacienteId = 7, medicoId = 1, agendaId = 1, fecha = hoy, hora = LocalTime.of(9, 30), duracion = 15, estado = EstadoTurno.ATENDIDO),
        Turno(id = 4, pacienteId = 4, medicoId = 2, agendaId = 2, fecha = hoy, hora = LocalTime.of(9, 0), duracion = 25),
        Turno(id = 5, pacienteId = 2, medicoId = 1, agendaId = 1, fecha = hoy, hora = LocalTime.of(10, 0), duracion = 15, estado = EstadoTurno.CANCELADO),
        Turno(id = 6, pacienteId = 5, medicoId = 3, agendaId = 3, fecha = hoy, hora = LocalTime.of(14, 0), duracion = 30),
        Turno(id = 7, pacienteId = 6, medicoId = 2, agendaId = 2, fecha = hoy, hora = LocalTime.of(11, 5), duracion = 25, esSobreturno = true),
        Turno(id = 8, pacienteId = 1, medicoId = 1, agendaId = 1, fecha = manana, hora = LocalTime.of(9, 0), duracion = 15),
        Turno(id = 9, pacienteId = 2, medicoId = 3, agendaId = 3, fecha = manana, hora = LocalTime.of(14, 30), duracion = 30),
        Turno(id = 10, pacienteId = 3, medicoId = 2, agendaId = 2, fecha = manana, hora = LocalTime.of(9, 0), duracion = 25),
        Turno(id = 11, pacienteId = 4, medicoId = 2, agendaId = 2, fecha = manana, hora = LocalTime.of(10, 15), duracion = 25, estado = EstadoTurno.CANCELADO)
    )

    val AGENDAS = mutableListOf(
        Agenda(
            id = 1,
            circuito = Circuito.CONSULTORIOS_EXTERNOS,
            servicio = Servicio.CARDIOLOGIA,
            medicoId = 1,
            diasSemana = todosLosDias,
            horaInicio = LocalTime.of(9, 0),
            horaFin = LocalTime.of(13, 0),
            duracionCita = 15,
            requiereAyuno = false,
            modalidad = Modalidad.CON_TURNO
        ),
        Agenda(
            id = 2,
            circuito = Circuito.CONSULTORIOS_EXTERNOS,
            servicio = Servicio.ORTOPEDIA,
            medicoId = 2,
            diasSemana = todosLosDias,
            horaInicio = LocalTime.of(9, 0),
            horaFin = LocalTime.of(13, 0),
            duracionCita = 25,
            requiereAyuno = false,
            modalidad = Modalidad.CON_TURNO
        ),
        Agenda(
            id = 3,
            circuito = Circuito.CONSULTORIOS_EXTERNOS,
            servicio = Servicio.PEDIATRIA,
            medicoId = 3,
            diasSemana = todosLosDias,
            horaInicio = LocalTime.of(14, 0),
            horaFin = LocalTime.of(18, 0),
            duracionCita = 30,
            requiereAyuno = false,
            modalidad = Modalidad.CON_TURNO
        ),
        Agenda(
            id = 4,
            circuito = Circuito.ESTUDIOS_CLINICOS,
            servicio = Servicio.RADIOGRAFIA,
            medicoId = 0,
            diasSemana = todosLosDias,
            horaInicio = LocalTime.of(8, 0),
            horaFin = LocalTime.of(16, 0),
            duracionCita = 15,
            requiereAyuno = false,
            modalidad = Modalidad.CON_TURNO
        ),
        Agenda(
            id = 5,
            circuito = Circuito.ESTUDIOS_CLINICOS,
            servicio = Servicio.ANALISIS_LABORATORIO,
            medicoId = 0,
            diasSemana = todosLosDias,
            horaInicio = LocalTime.of(7, 30),
            horaFin = LocalTime.of(11, 0),
            duracionCita = 10,
            requiereAyuno = true,
            modalidad = Modalidad.CON_TURNO
        )
    )

    val HONORARIOS = mutableListOf<Any>()
    val ATENCIONES = mutableListOf(
        Atencion(
            id = 1,
            medicoId = 1,
            pacienteId = 7,
            turnoId = 3,
            fecha = hoy,
            hora = LocalTime.of(9, 30),
            duracion = 15,
            observaciones = "",
            valorConsulta = 15000.0,
            diagnostico = "Control cardiológico",
            indicaciones = "Continuar con tratamiento habitual"
        ),
        Atencion(
            id = 2,
            medicoId = 1,
            pacienteId = 1,
            turnoId = 0,
            fecha = hoy.minusDays(7),
            hora = LocalTime.of(10, 0),
            duracion = 15,
            observaciones = "",
            valorConsulta = 14000.0,
            diagnostico = "Hipertensión leve",
            indicaciones = "Dieta hiposódica y control en 30 días"
        )
    )

    val ESTUDIOS = mutableListOf(
        Estudio(id = 1, nombre = "Análisis de sangre", pacienteId = 1, agendaId = 5, fecha = hoy, hora = LocalTime.of(7, 30), duracion = 10, observaciones = "Ayuno de 8 horas"),
        Estudio(id = 2, nombre = "Radiografía de tórax", pacienteId = 2, agendaId = 4, fecha = hoy, hora = LocalTime.of(10, 0), duracion = 15),
        Estudio(id = 3, nombre = "Ecografía abdominal", pacienteId = 3, fecha = manana, hora = LocalTime.of(11, 0), duracion = 20, observaciones = "Ayuno de 6 horas"),
        Estudio(id = 4, nombre = "Análisis de orina", pacienteId = 1, agendaId = 5, fecha = manana.plusDays(1), hora = LocalTime.of(8, 0), duracion = 10),
        Estudio(id = 5, nombre = "Radiografía de rodilla", pacienteId = 4, agendaId = 4, fecha = manana.plusDays(2), hora = LocalTime.of(9, 0), duracion = 15),
        Estudio(id = 6, nombre = "Ecografía tiroidea", pacienteId = 5, fecha = manana.plusDays(3), hora = LocalTime.of(15, 0), duracion = 20)
    )

    val INSUMOS = mutableListOf(
        Insumo(id = 1, nombre = "Jeringa", cantidad = 120),
        Insumo(id = 2, nombre = "Algodón", cantidad = 20),
        Insumo(id = 3, nombre = "Venda", cantidad = 100),
        Insumo(id = 4, nombre = "Guantes de nitrilo", cantidad = 45),
        Insumo(id = 5, nombre = "Alcohol en gel", cantidad = 5),
        Insumo(id = 6, nombre = "Gasas", cantidad = 60),
        Insumo(id = 7, nombre = "Solución fisiológica", cantidad = 30)
    )
}
