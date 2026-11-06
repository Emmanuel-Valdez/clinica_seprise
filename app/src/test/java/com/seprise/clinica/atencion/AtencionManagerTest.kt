package com.seprise.clinica.atencion

import com.seprise.clinica.models.Atencion
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Turno
import com.seprise.clinica.turnos.TurnosManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AtencionManagerTest {

    private fun atencion(
        id: Int,
        turnoId: Int,
        diagnostico: String = "Control",
        duracion: Int = 15,
        valor: Double = 1000.0
    ) = Atencion(
        id = id,
        medicoId = 999,
        pacienteId = 999,
        turnoId = turnoId,
        fecha = LocalDate.now(),
        hora = LocalTime.of(11, 0),
        duracion = duracion,
        valorConsulta = valor,
        diagnostico = diagnostico
    )

    @Test
    fun validaYVinculaAtencionAlTurno() {
        val turno = Turno(
            id = 9001,
            pacienteId = 999,
            medicoId = 999,
            agendaId = 1,
            fecha = LocalDate.now(),
            hora = LocalTime.of(11, 0),
            duracion = 15
        )
        assertTrue(TurnosManager.crearTurno(turno))

        assertFalse(AtencionManager.registrarAtencion(atencion(id = 9001, turnoId = turno.id, diagnostico = " ")))
        assertFalse(AtencionManager.registrarAtencion(atencion(id = 9001, turnoId = turno.id, duracion = 0)))
        assertFalse(AtencionManager.registrarAtencion(atencion(id = 9001, turnoId = turno.id, valor = -1.0)))

        assertTrue(AtencionManager.registrarAtencion(atencion(id = 9001, turnoId = turno.id)))
        assertEquals(EstadoTurno.ATENDIDO, TurnosManager.obtenerTurnoPorId(turno.id)?.estado)
        assertFalse(TurnosManager.marcarAtendido(turno.id))

        assertFalse(AtencionManager.registrarAtencion(atencion(id = 9002, turnoId = turno.id)))
        assertTrue(AtencionManager.registrarAtencion(atencion(id = 9003, turnoId = 0)))
    }
}
