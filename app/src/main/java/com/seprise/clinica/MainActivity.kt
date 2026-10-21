package com.seprise.clinica

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seprise.clinica.agenda.AgendaManager
import com.seprise.clinica.auth.LoginActivity
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Circuito
import com.seprise.clinica.models.Modalidad
import com.seprise.clinica.models.Servicio
import com.seprise.clinica.shell.MainNavActivity
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Launcher neutro (CU01): delega a login si no hay sesión, o al shell si la hay.
 * La sesión es en memoria, así que un arranque en frío siempre cae en login.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSePriseEdgeToEdge()
        setContentView(R.layout.activity_main)
        probarAgenda()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val destino = if (SessionState.rol == null) LoginActivity::class.java else MainNavActivity::class.java
        startActivity(Intent(this, destino))
        finish()
    }

    private fun probarAgenda() {
        val agenda = Agenda(
            id = 1,
            circuito = Circuito.CONSULTORIOS_EXTERNOS,
            servicio = Servicio.PEDIATRIA,
            medicoId = 1,
            diasSemana = listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            horaInicio = LocalTime.of(9, 0),
            horaFin = LocalTime.of(12, 0),
            duracionCita = 15,
            requiereAyuno = false,
            modalidad = Modalidad.CON_TURNO
        )

        val creada = AgendaManager.crearAgenda(agenda)
        Log.d("PRUEBA_AGENDA", "Agenda creada: $creada")

        // Ver agenda (obtener la agenda por su ID)
        val agendaEncontrada = AgendaManager.obtenerAgendaPorId(1)
        Log.d("PRUEBA_AGENDA", "Ver agenda (ID 1): $agendaEncontrada")

        val modificada = AgendaManager.modificarAgenda(
            agenda.copy(servicio = Servicio.CLINICA_MEDICA)
        )
        Log.d("PRUEBA_AGENDA", "Todas las agendas: ${AgendaManager.obtenerAgendas()}")

        Log.d("PRUEBA_AGENDA", "Agenda modificada: $modificada")

        val eliminada = AgendaManager.eliminarAgenda(1)
        Log.d("PRUEBA_AGENDA", "Agenda eliminada: $eliminada")

    }
}
