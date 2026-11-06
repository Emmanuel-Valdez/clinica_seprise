package com.seprise.clinica.agenda

import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Especialidad
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Servicio
import com.seprise.clinica.models.Turno
import com.seprise.clinica.models.duracionSugerida
import com.seprise.clinica.turnos.TurnosManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AgendaTurnosManagerTest {

    @Test
    fun duracionSugeridaPorEspecialidad() {
        assertEquals(15, Especialidad.CARDIOLOGIA.duracionSugerida())
        assertEquals(15, Especialidad.ORTOPEDIA.duracionSugerida())
        assertEquals(15, Especialidad.PEDIATRIA.duracionSugerida())
        assertEquals(25, Especialidad.FISIOTERAPIA.duracionSugerida())
        assertEquals(25, Especialidad.KINESIOLOGIA.duracionSugerida())
        assertEquals(30, Especialidad.SALUD_MENTAL.duracionSugerida())
    }

    @Test
    fun agendaRechazaRangoODuracionInvalidos() {
        val valida = Agenda(
            id = 9001,
            servicio = Servicio.FISIOTERAPIA,
            medicoId = 997,
            diasSemana = listOf(DayOfWeek.MONDAY),
            horaInicio = LocalTime.of(9, 0),
            horaFin = LocalTime.of(13, 0),
            duracionCita = 25,
            requiereAyuno = false
        )
        assertTrue(AgendaManager.crearAgenda(valida))

        assertFalse(AgendaManager.modificarAgenda(valida.copy(horaFin = LocalTime.of(9, 0))))
        assertFalse(AgendaManager.modificarAgenda(valida.copy(duracionCita = 0)))
        assertEquals(-1, AgendaManager.modificarAgendaYCancelarTurnos(valida.copy(horaFin = LocalTime.of(8, 0))))

        val otraInvalida = valida.copy(id = 9002, medicoId = 998, duracionCita = -5)
        assertFalse(AgendaManager.crearAgenda(otraInvalida))
    }

    @Test
    fun turnosRespetanCupoSobreturnoYCancelacion() {
        val fecha = LocalDate.now().plusDays(20)
        fun turno(id: Int, esSobreturno: Boolean = false) = Turno(
            id = id,
            pacienteId = 999,
            medicoId = 997,
            agendaId = 9001,
            fecha = fecha,
            hora = LocalTime.of(10, 0),
            duracion = 25,
            esSobreturno = esSobreturno
        )

        assertTrue(TurnosManager.crearTurno(turno(id = 9101)))
        assertFalse(TurnosManager.crearTurno(turno(id = 9102)))
        assertTrue(TurnosManager.crearTurno(turno(id = 9103, esSobreturno = true)))
        assertFalse(TurnosManager.crearTurno(turno(id = 9104, esSobreturno = true)))

        assertTrue(TurnosManager.cancelarTurno(9101))
        assertEquals(EstadoTurno.CANCELADO, TurnosManager.obtenerTurnoPorId(9101)?.estado)
        assertFalse(TurnosManager.cancelarTurno(9101))
        assertTrue(TurnosManager.crearTurno(turno(id = 9105)))
    }
}
