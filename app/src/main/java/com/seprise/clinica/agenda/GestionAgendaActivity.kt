package com.seprise.clinica.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
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
import com.seprise.clinica.models.Circuito
import com.seprise.clinica.models.Medico
import com.seprise.clinica.models.Modalidad
import com.seprise.clinica.models.Servicio
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeParseException

class GestionAgendaActivity : AppCompatActivity() {

    private lateinit var containerMedicos: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSePriseEdgeToEdge()
        setContentView(R.layout.activity_gestion_agenda)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        containerMedicos = findViewById(R.id.container_medicos)

        cargarListaMedicos()
    }

    /** Renderiza la lista de médicos consumiendo directamente de AppData.MEDICOS. */
    private fun cargarListaMedicos() {
        containerMedicos.removeAllViews()

        for (medico in AppData.MEDICOS) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_medico)
            val tvEspecialidad = item.findViewById<TextView>(R.id.tv_especialidad_medico)
            val tvEstado = item.findViewById<TextView>(R.id.tv_estado_agenda)
            val btnAccion = item.findViewById<Button>(R.id.btn_accion_agenda)

            tvNombre.text = "Dr/a. ${medico.fullName}"
            tvEspecialidad.text = "Especialidad: ${medico.especialidad.name}"

            val agendaExistente = AgendaManager.obtenerAgendaPorMedico(medico.id)

            if (agendaExistente != null) {
                tvEstado.text = "Agenda Configurada"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_success))
                btnAccion.text = "Ver Agenda"
                btnAccion.setOnClickListener { mostrarDetalleAgendaDialog(medico, agendaExistente) }
            } else {
                tvEstado.text = "Sin Agenda"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
                btnAccion.text = "Cargar Agenda"
                btnAccion.setOnClickListener { abrirModalCargarAgenda(medico) }
            }

            containerMedicos.addView(item)
        }
    }

    /** Abre el modal con el formulario para cargar una nueva agenda para el médico. */
    private fun abrirModalCargarAgenda(medico: Medico) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_agenda, null)

        dialogView.findViewById<TextView>(R.id.tv_dialog_medico_nombre).text = "Médico: Dr/a. ${medico.fullName}"

        val spinnerServicio = dialogView.findViewById<Spinner>(R.id.spinner_servicio)
        val listaServicios = Servicio.entries.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaServicios)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerServicio.adapter = adapter

        // Preselecciona el servicio si coincide con la especialidad del médico
        val indexEspecialidad = listaServicios.indexOf(medico.especialidad.name)
        if (indexEspecialidad >= 0) {
            spinnerServicio.setSelection(indexEspecialidad)
        }

        val rgCircuito = dialogView.findViewById<RadioGroup>(R.id.rg_circuito)
        val rgModalidad = dialogView.findViewById<RadioGroup>(R.id.rg_modalidad)

        val cbLunes = dialogView.findViewById<CheckBox>(R.id.cb_lunes)
        val cbMartes = dialogView.findViewById<CheckBox>(R.id.cb_martes)
        val cbMiercoles = dialogView.findViewById<CheckBox>(R.id.cb_miercoles)
        val cbJueves = dialogView.findViewById<CheckBox>(R.id.cb_jueves)
        val cbViernes = dialogView.findViewById<CheckBox>(R.id.cb_viernes)
        val cbSabado = dialogView.findViewById<CheckBox>(R.id.cb_sabado)
        val cbDomingo = dialogView.findViewById<CheckBox>(R.id.cb_domingo)

        val etHoraInicio = dialogView.findViewById<EditText>(R.id.et_hora_inicio)
        val etHoraFin = dialogView.findViewById<EditText>(R.id.et_hora_fin)
        val etDuracionCita = dialogView.findViewById<EditText>(R.id.et_duracion_cita)
        val cbAyuno = dialogView.findViewById<CheckBox>(R.id.cb_requiere_ayuno)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar Agenda", null)
            .create()

        dialog.show()

        // Sobrescribimos el botón para validar antes de cerrar el modal
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val diasSeleccionados = mutableListOf<DayOfWeek>()
            if (cbLunes.isChecked) diasSeleccionados.add(DayOfWeek.MONDAY)
            if (cbMartes.isChecked) diasSeleccionados.add(DayOfWeek.TUESDAY)
            if (cbMiercoles.isChecked) diasSeleccionados.add(DayOfWeek.WEDNESDAY)
            if (cbJueves.isChecked) diasSeleccionados.add(DayOfWeek.THURSDAY)
            if (cbViernes.isChecked) diasSeleccionados.add(DayOfWeek.FRIDAY)
            if (cbSabado.isChecked) diasSeleccionados.add(DayOfWeek.SATURDAY)
            if (cbDomingo.isChecked) diasSeleccionados.add(DayOfWeek.SUNDAY)

            if (diasSeleccionados.isEmpty()) {
                Toast.makeText(this, "Debe seleccionar al menos un día de atención", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val horaInicio = parsearHora(etHoraInicio.text.toString().trim(), LocalTime.of(8, 0))
            val horaFin = parsearHora(etHoraFin.text.toString().trim(), LocalTime.of(16, 0))
            val duracionMins = etDuracionCita.text.toString().toIntOrNull() ?: 20

            val circuito = if (rgCircuito.checkedRadioButtonId == R.id.rb_estudios) {
                Circuito.ESTUDIOS_CLINICOS
            } else {
                Circuito.CONSULTORIOS_EXTERNOS
            }

            val modalidad = if (rgModalidad.checkedRadioButtonId == R.id.rb_orden_llegada) {
                Modalidad.ORDEN_LLEGADA
            } else {
                Modalidad.CON_TURNO
            }

            val servicioSeleccionado = Servicio.valueOf(spinnerServicio.selectedItem.toString())

            val nuevaAgenda = Agenda(
                id = (AppData.AGENDAS.maxOfOrNull { it.id } ?: 0) + 1,
                circuito = circuito,
                servicio = servicioSeleccionado,
                medicoId = medico.id,
                diasSemana = diasSeleccionados,
                horaInicio = horaInicio,
                horaFin = horaFin,
                duracionCita = duracionMins,
                requiereAyuno = cbAyuno.isChecked,
                modalidad = modalidad
            )

            val creada = AgendaManager.crearAgenda(nuevaAgenda)
            if (creada) {
                Toast.makeText(this, "Agenda guardada con éxito para Dr/a. ${medico.apellido}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarListaMedicos()
            } else {
                Toast.makeText(this, "Error: El médico ya tiene una agenda cargada", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Muestra los datos de la agenda configurada para el médico. */
    private fun mostrarDetalleAgendaDialog(medico: Medico, agenda: Agenda) {
        val diasStr = agenda.diasSemana.joinToString(", ") { it.name }

        val mensaje = """
            Médico: Dr/a. ${medico.fullName}
            Servicio: ${agenda.servicio.name}
            Circuito: ${agenda.circuito.name}
            Modalidad: ${agenda.modalidad.name}
            Días de atención: $diasStr
            Horario: ${agenda.horaInicio} - ${agenda.horaFin}
            Duración cita: ${agenda.duracionCita} min
            Requiere ayuno: ${if (agenda.requiereAyuno) "Sí" else "No"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Detalle de la Agenda")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Eliminar Agenda") { _, _ ->
                AgendaManager.eliminarAgenda(agenda.id)
                Toast.makeText(this, "Agenda eliminada", Toast.LENGTH_SHORT).show()
                cargarListaMedicos()
            }
            .show()
    }

    private fun parsearHora(texto: String, porDefecto: LocalTime): LocalTime {
        return try {
            LocalTime.parse(texto)
        } catch (e: DateTimeParseException) {
            porDefecto
        }
    }
}
