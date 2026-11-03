package com.seprise.clinica.turnos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seprise.clinica.R
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Medico
import com.seprise.clinica.models.Turno
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

class GestionTurnosActivity : AppCompatActivity() {

    private lateinit var containerMedicos: LinearLayout
    private lateinit var etBuscarMedico: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSePriseEdgeToEdge()
        setContentView(R.layout.activity_gestion_turnos)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        containerMedicos = findViewById(R.id.container_medicos)
        etBuscarMedico = findViewById(R.id.et_buscar_medico)

        etBuscarMedico.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarListaMedicos(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarListaMedicos()
    }

    /** Renderiza la lista de médicos para la gestión de turnos consumiendo AppData.MEDICOS. */
    private fun cargarListaMedicos(filtro: String = "") {
        containerMedicos.removeAllViews()

        val filtroNormalizado = normalizarTexto(filtro)

        val medicosFiltrados = if (filtroNormalizado.isBlank()) {
            AppData.MEDICOS
        } else {
            AppData.MEDICOS.filter {
                normalizarTexto(it.fullName).contains(filtroNormalizado)
            }
        }

        if (medicosFiltrados.isEmpty()) {
            val tvVacio = TextView(this).apply {
                text = "No se encontraron médicos"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@GestionTurnosActivity, R.color.seprise_muted))
                setPadding(0, 24, 0, 0)
            }
            containerMedicos.addView(tvVacio)
            return
        }

        for (medico in medicosFiltrados) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_medico)
            val tvEspecialidad = item.findViewById<TextView>(R.id.tv_especialidad_medico)
            val tvEstado = item.findViewById<TextView>(R.id.tv_estado_agenda)
            val btnAccion = item.findViewById<Button>(R.id.btn_accion_agenda)

            tvNombre.text = "Dr/a. ${medico.fullName}"
            tvEspecialidad.text = "Especialidad: ${medico.especialidad.name}"

            val agendaExistente = TurnosManager.obtenerAgendaPorMedico(medico.id)
            val turnosReservados = TurnosManager.obtenerTurnosPorMedico(medico.id)

            if (agendaExistente != null) {
                tvEstado.text = "Agenda disponible (${turnosReservados.size} turnos)"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_success))
                btnAccion.text = "Sacar Turno"
                btnAccion.isEnabled = true
                btnAccion.setOnClickListener { abrirModalSacarTurno(medico, agendaExistente) }

                item.setOnClickListener { mostrarTurnosDialog(medico, turnosReservados, agendaExistente) }
            } else {
                tvEstado.text = "Sin agenda configurada"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
                btnAccion.text = "Sin Agenda"
                btnAccion.isEnabled = false
            }

            containerMedicos.addView(item)
        }
    }

    /** Abre el modal con el formulario para reservar un nuevo turno mostrando los horarios disponibles del médico. */
    private fun abrirModalSacarTurno(medico: Medico, agenda: Agenda) {
        if (AppData.PACIENTES.isEmpty()) {
            Toast.makeText(this, "No hay pacientes registrados en el sistema", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sacar_turno, null)

        val diasAtencionStr = agenda.diasSemana.joinToString(", ") { it.name }
        dialogView.findViewById<TextView>(R.id.tv_dialog_turno_info).text =
            "Médico: Dr/a. ${medico.fullName}\nServicio: ${agenda.servicio.name}\nDías de atención: $diasAtencionStr\nHorario: ${agenda.horaInicio} a ${agenda.horaFin} (${agenda.duracionCita} min/cita)"

        val spinnerPaciente = dialogView.findViewById<Spinner>(R.id.spinner_paciente)
        val listaPacientesStr = AppData.PACIENTES.map { "ID ${it.id}: ${it.fullName} (DNI ${it.dni})" }
        val adapterPaciente = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaPacientesStr)
        adapterPaciente.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaciente.adapter = adapterPaciente

        val etFecha = dialogView.findViewById<EditText>(R.id.et_fecha_turno)
        val spinnerHorario = dialogView.findViewById<Spinner>(R.id.spinner_horario_disponible)
        val cbSobreturno = dialogView.findViewById<CheckBox>(R.id.cb_es_sobreturno)

        val fechaInicial = LocalDate.now()
        etFecha.setText(fechaInicial.toString())

        var horariosDisponibles = mutableListOf<LocalTime>()

        fun actualizarHorarios() {
            val fechaIngresada = try {
                LocalDate.parse(etFecha.text.toString().trim())
            } catch (e: Exception) {
                fechaInicial
            }

            horariosDisponibles = generarHorariosDisponibles(medico.id, agenda, fechaIngresada).toMutableList()
            val horariosStr = if (horariosDisponibles.isEmpty()) {
                listOf("Sin horarios libres en esta fecha")
            } else {
                horariosDisponibles.map { it.toString() }
            }

            val adapterHorarios = ArrayAdapter(this@GestionTurnosActivity, android.R.layout.simple_spinner_item, horariosStr)
            adapterHorarios.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerHorario.adapter = adapterHorarios
        }

        actualizarHorarios()

        etFecha.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarHorarios()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar Turno", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pacienteSeleccionado = AppData.PACIENTES.getOrNull(spinnerPaciente.selectedItemPosition)
            if (pacienteSeleccionado == null) {
                Toast.makeText(this, "Seleccione un paciente válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaStr = etFecha.text.toString().trim()
            val fechaTurno = try {
                LocalDate.parse(fechaStr)
            } catch (e: DateTimeParseException) {
                Toast.makeText(this, "Formato de fecha inválido (Use AAAA-MM-DD)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar que el día de la semana coincida con la agenda del médico
            val diaSemanaTurno = fechaTurno.dayOfWeek
            if (!agenda.diasSemana.contains(diaSemanaTurno) && !cbSobreturno.isChecked) {
                Toast.makeText(
                    this,
                    "El médico no atiende los $diaSemanaTurno. Días permitidos: $diasAtencionStr (o marque sobreturno)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val horaTurno = if (horariosDisponibles.isNotEmpty()) {
                horariosDisponibles.getOrNull(spinnerHorario.selectedItemPosition) ?: agenda.horaInicio
            } else {
                agenda.horaInicio
            }

            val nuevoTurno = Turno(
                id = (AppData.TURNOS.maxOfOrNull { it.id } ?: 0) + 1,
                pacienteId = pacienteSeleccionado.id,
                medicoId = medico.id,
                agendaId = agenda.id,
                fecha = fechaTurno,
                hora = horaTurno,
                duracion = agenda.duracionCita,
                estado = EstadoTurno.RESERVADO,
                esSobreturno = cbSobreturno.isChecked
            )

            val creado = TurnosManager.crearTurno(nuevoTurno)
            if (creado) {
                Toast.makeText(this, "Turno reservado con éxito para ${pacienteSeleccionado.fullName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarListaMedicos(etBuscarMedico.text.toString())
            } else {
                Toast.makeText(this, "Error al crear el turno", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Genera los horarios de citas disponibles excluyendo turnos ya reservados. */
    private fun generarHorariosDisponibles(medicoId: Int, agenda: Agenda, fecha: LocalDate): List<LocalTime> {
        val slots = mutableListOf<LocalTime>()
        var actual = agenda.horaInicio
        val fin = agenda.horaFin
        val duracion = if (agenda.duracionCita > 0) agenda.duracionCita.toLong() else 20L

        while (actual.plusMinutes(duracion) <= fin) {
            slots.add(actual)
            actual = actual.plusMinutes(duracion)
        }

        val ocupados = AppData.TURNOS.filter {
            it.medicoId == medicoId && it.fecha == fecha && it.estado == EstadoTurno.RESERVADO && !it.esSobreturno
        }.map { it.hora }

        return slots.filter { it !in ocupados }
    }

    /** Muestra la lista de turnos reservados para un médico. */
    private fun mostrarTurnosDialog(medico: Medico, turnos: List<Turno>, agenda: Agenda) {
        if (turnos.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Turnos de Dr/a. ${medico.fullName}")
                .setMessage("No hay turnos registrados para este médico.")
                .setPositiveButton("Sacar Turno") { _, _ -> abrirModalSacarTurno(medico, agenda) }
                .setNegativeButton("Cerrar", null)
                .show()
            return
        }

        val detalleTurnos = turnos.joinToString("\n\n") { turno ->
            val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
            val pacienteNombre = paciente?.fullName ?: "Paciente ID ${turno.pacienteId}"
            "• Fecha: ${turno.fecha} ${turno.hora}\n  Paciente: $pacienteNombre\n  Estado: ${turno.estado}\n  Sobreturno: ${if (turno.esSobreturno) "Sí" else "No"}"
        }

        AlertDialog.Builder(this)
            .setTitle("Turnos de Dr/a. ${medico.fullName}")
            .setMessage(detalleTurnos)
            .setPositiveButton("Sacar Nuevo Turno") { _, _ -> abrirModalSacarTurno(medico, agenda) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }
}
