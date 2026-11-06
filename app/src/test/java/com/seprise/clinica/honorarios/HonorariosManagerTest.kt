package com.seprise.clinica.honorarios

import com.seprise.clinica.core.AppData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class HonorariosManagerTest {

    private val medicoId = 1

    @Before
    fun limpiarLiquidaciones() {
        AppData.HONORARIOS.clear()
    }

    @After
    fun restaurarLiquidaciones() {
        AppData.HONORARIOS.clear()
    }

    @Test
    fun reporteFiltraMedicoRangoYLiquidadas() {
        val hoy = LocalDate.now()
        val pendientes = HonorariosManager.obtenerAtencionesPendientes(medicoId, hoy.minusDays(10), hoy)

        assertEquals(2, pendientes.size)
        assertEquals(29000.0, HonorariosManager.calcularTotal(pendientes), 0.0)
        assertTrue(HonorariosManager.obtenerAtencionesPendientes(2, hoy.minusDays(10), hoy).isEmpty())

        val soloReciente = HonorariosManager.obtenerAtencionesPendientes(medicoId, hoy, hoy)
        assertEquals(1, soloReciente.size)
        assertEquals(1, soloReciente.single().id)
    }

    @Test
    fun rangoInvalidoYMedicoInexistenteNoLiquidan() {
        val hoy = LocalDate.now()

        assertNull(HonorariosManager.registrarLiquidacion(medicoId, hoy, hoy.minusDays(1)))
        assertNull(HonorariosManager.registrarLiquidacion(999, hoy.minusDays(10), hoy))
        assertTrue(AppData.HONORARIOS.isEmpty())
    }

    @Test
    fun liquidaYEvitaDobleCobro() {
        val hoy = LocalDate.now()

        val liquidacion = HonorariosManager.registrarLiquidacion(medicoId, hoy.minusDays(10), hoy)

        assertNotNull(liquidacion)
        assertEquals(2, liquidacion!!.atenciones.size)
        assertEquals(29000.0, liquidacion.total, 0.0)
        assertEquals(1, AppData.HONORARIOS.size)

        assertNull(HonorariosManager.registrarLiquidacion(medicoId, hoy.minusDays(10), hoy))
        assertEquals(1, AppData.HONORARIOS.size)
    }

    @Test
    fun subperiodoSoloExcluyeSusAtenciones() {
        val hoy = LocalDate.now()

        val liquidacion = HonorariosManager.registrarLiquidacion(medicoId, hoy.minusDays(10), hoy.minusDays(1))

        assertNotNull(liquidacion)
        assertEquals(listOf(2), liquidacion!!.atenciones)
        assertEquals(14000.0, liquidacion.total, 0.0)

        val restantes = HonorariosManager.obtenerAtencionesPendientes(medicoId, hoy.minusDays(10), hoy)
        assertEquals(listOf(1), restantes.map { it.id })
    }
}
