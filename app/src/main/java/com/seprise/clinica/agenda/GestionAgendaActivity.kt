package com.seprise.clinica.agenda

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
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Circuito
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Medico
import com.seprise.clinica.models.Modalidad
import com.seprise.clinica.models.Servicio
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeParseException

class GestionAgendaActivity : AppCompatActivity() {

    private lateinit var containerMedicos: LinearLayout
    private lateinit var etBuscarMedico: EditText
    private lateinit var rgCircuitoAgenda: RadioGroup
    private lateinit var btnCargarAgendaEstudios: Button

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
        etBuscarMedico = findViewById(R.id.et_buscar_medico)
        rgCircuitoAgenda = findViewById(R.id.rg_circuito_agenda)
        btnCargarAgendaEstudios = findViewById(R.id.btn_cargar_agenda_estudios)

        rgCircuitoAgenda.setOnCheckedChangeListener { _, _ ->
            cargarLista(etBuscarMedico.text.toString())
        }

        btnCargarAgendaEstudios.setOnClickListener {
            abrirModalCargarAgendaEstudios()
        }

        etBuscarMedico.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarLista(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarLista()
    }

    /** Carga la lista según el circuito seleccionado (Consultorio Externo o Estudios Clínicos). */
    private fun cargarLista(filtro: String = "") {
        containerMedicos.removeAllViews()

        val esEstudios = rgCircuitoAgenda.checkedRadioButtonId == R.id.rb_agenda_estudios

        if (esEstudios && SessionState.rol != Rol.MEDICO) {
            btnCargarAgendaEstudios.visibility = View.VISIBLE
        } else {
            btnCargarAgendaEstudios.visibility = View.GONE
        }

        if (esEstudios) {
            cargarAgendasEstudios(filtro)
        } else {
            cargarMedicosConsultorio(filtro)
        }
    }

    /** Renderiza la lista de médicos para la gestión de su agenda en Consultorio Externo. */
    private fun cargarMedicosConsultorio(filtro: String) {
        val medicosBase = if (SessionState.rol == Rol.MEDICO && SessionState.loggedPersonId != null) {
            AppData.MEDICOS.filter { it.id == SessionState.loggedPersonId }
        } else {
            AppData.MEDICOS
        }

        val filtroNorm = normalizarTexto(filtro)
        val medicosFiltrados = if (filtroNorm.isBlank()) {
            medicosBase
        } else {
            medicosBase.filter {
                normalizarTexto(it.fullName).contains(filtroNorm)
            }
        }

        if (medicosFiltrados.isEmpty()) {
            mostrarMensajeVacio("No se encontraron médicos en consultorio externo")
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

            val agendaExistente = AgendaManager.obtenerAgendaPorMedico(medico.id)

            if (agendaExistente != null && agendaExistente.circuito == Circuito.CONSULTORIOS_EXTERNOS) {
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

    /** Renderiza la lista de agendas configuradas para Estudios Clínicos / Análisis. */
    private fun cargarAgendasEstudios(filtro: String) {
        val agendasEstudios = AppData.AGENDAS.filter { it.circuito == Circuito.ESTUDIOS_CLINICOS }
        val filtroNorm = normalizarTexto(filtro)

        val estudiosFiltrados = if (filtroNorm.isBlank()) {
            agendasEstudios
        } else {
            agendasEstudios.filter { normalizarTexto(it.servicio.name).contains(filtroNorm) }
        }

        if (estudiosFiltrados.isEmpty()) {
            mostrarMensajeVacio("No hay agendas de estudios clínicos configuradas")
            return
        }

        for (agenda in estudiosFiltrados) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_medico)
            val tvEspecialidad = item.findViewById<TextView>(R.id.tv_especialidad_medico)
            val tvEstado = item.findViewById<TextView>(R.id.tv_estado_agenda)
            val btnAccion = item.findViewById<Button>(R.id.btn_accion_agenda)

            val diasAtencionStr = agenda.diasSemana.joinToString(", ") { it.name }

            tvNombre.text = "Estudio: ${agenda.servicio.name}"
            tvEspecialidad.text = "Días: $diasAtencionStr (${agenda.horaInicio} - ${agenda.horaFin})"
            tvEstado.text = if (agenda.requiereAyuno) "Requiere ayuno previo" else "Sin requerimiento de ayuno"
            tvEstado.setTextColor(ContextCompat.getColor(this, if (agenda.requiereAyuno) R.color.seprise_warn else R.color.seprise_blue))

            btnAccion.text = "Ver Agenda"
            btnAccion.setOnClickListener { mostrarDetalleAgendaEstudioDialog(agenda) }

            containerMedicos.addView(item)
        }
    }

    private fun mostrarMensajeVacio(mensaje: String) {
        val tvVacio = TextView(this).apply {
            text = mensaje
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@GestionAgendaActivity, R.color.seprise_muted))
            setPadding(0, 24, 0, 0)
        }
        containerMedicos.addView(tvVacio)
    }

    /** Abre el modal para cargar agenda a un Médico (tomando la especialidad del objeto médico). */
    private fun abrirModalCargarAgenda(medico: Medico) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_agenda, null)

        dialogView.findViewById<TextView>(R.id.tv_dialog_medico_nombre).text = "Médico: Dr/a. ${medico.fullName} (${medico.especialidad.name})"

        dialogView.findViewById<View>(R.id.layout_servicio).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_circuito).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_modalidad).visibility = View.GONE

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

            val servicioDelMedico = try {
                Servicio.valueOf(medico.especialidad.name)
            } catch (e: Exception) {
                Servicio.CLINICA_MEDICA
            }

            val nuevaAgenda = Agenda(
                id = (AppData.AGENDAS.maxOfOrNull { it.id } ?: 0) + 1,
                circuito = Circuito.CONSULTORIOS_EXTERNOS,
                servicio = servicioDelMedico,
                medicoId = medico.id,
                diasSemana = diasSeleccionados,
                horaInicio = horaInicio,
                horaFin = horaFin,
                duracionCita = duracionMins,
                requiereAyuno = cbAyuno.isChecked,
                modalidad = Modalidad.CON_TURNO
            )

            val creada = AgendaManager.crearAgenda(nuevaAgenda)
            if (creada) {
                Toast.makeText(this, "Agenda guardada con éxito para Dr/a. ${medico.apellido}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarLista(etBuscarMedico.text.toString())
            } else {
                Toast.makeText(this, "Error: El médico ya tiene una agenda cargada", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Abre el modal para cargar una agenda del circuito Análisis Clínicos / Estudios. */
    private fun abrirModalCargarAgendaEstudios() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_agenda, null)

        dialogView.findViewById<TextView>(R.id.tv_dialog_titulo).text = "Cargar Agenda - Análisis Clínicos / Estudios"
        dialogView.findViewById<TextView>(R.id.tv_dialog_medico_nombre).text = "Configuración del servicio de estudio"

        dialogView.findViewById<View>(R.id.layout_servicio).visibility = View.VISIBLE
        dialogView.findViewById<View>(R.id.layout_circuito).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_modalidad).visibility = View.GONE

        val spinnerServicio = dialogView.findViewById<Spinner>(R.id.spinner_servicio)
        val listaEstudios = listOf(
            Servicio.ANALISIS_LABORATORIO.name,
            Servicio.RADIOGRAFIA.name,
            Servicio.ECOGRAFIA.name
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaEstudios)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerServicio.adapter = adapter

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
            val duracionMins = etDuracionCita.text.toString().toIntOrNull() ?: 15

            val servicioSeleccionado = Servicio.valueOf(spinnerServicio.selectedItem.toString())

            val nuevaAgenda = Agenda(
                id = (AppData.AGENDAS.maxOfOrNull { it.id } ?: 0) + 1,
                circuito = Circuito.ESTUDIOS_CLINICOS,
                servicio = servicioSeleccionado,
                medicoId = 0,
                diasSemana = diasSeleccionados,
                horaInicio = horaInicio,
                horaFin = horaFin,
                duracionCita = duracionMins,
                requiereAyuno = cbAyuno.isChecked,
                modalidad = Modalidad.CON_TURNO
            )

            AppData.AGENDAS.add(nuevaAgenda)
            Toast.makeText(this, "Agenda de estudio cargada con éxito", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            cargarLista(etBuscarMedico.text.toString())
        }
    }

    /** Muestra los datos de la agenda configurada para un médico. */
    private fun mostrarDetalleAgendaDialog(medico: Medico, agenda: Agenda) {
        val diasStr = agenda.diasSemana.joinToString(", ") { it.name }

        val slots = mutableListOf<LocalTime>()
        var actual = agenda.horaInicio
        val fin = agenda.horaFin
        val duracion = if (agenda.duracionCita > 0) agenda.duracionCita.toLong() else 20L

        while (actual.plusMinutes(duracion) <= fin) {
            slots.add(actual)
            actual = actual.plusMinutes(duracion)
        }

        val grillaHorariosStr = slots.joinToString("\n") { hora ->
            val turno = AppData.TURNOS.find {
                it.medicoId == medico.id && it.hora == hora && it.estado == EstadoTurno.RESERVADO
            }
            if (turno != null) {
                val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
                val nombrePaciente = paciente?.fullName ?: "Paciente ID ${turno.pacienteId}"
                "• $hora - Reservado (Paciente: $nombrePaciente)"
            } else {
                "• $hora - Disponible"
            }
        }

        val mensaje = """
            Médico: Dr/a. ${medico.fullName}
            Servicio: ${agenda.servicio.name} | Circuito: ${agenda.circuito.name}
            Días de atención: $diasStr
            
            --- Horarios de Atención ---
            $grillaHorariosStr
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("Agenda de Dr/a. ${medico.fullName}")
            .setMessage(mensaje)

        if (SessionState.rol != Rol.MEDICO) {
            builder.setPositiveButton("Modificar Agenda") { _, _ ->
                abrirModalModificarAgenda(medico, agenda)
            }
            builder.setNeutralButton("Eliminar Agenda") { _, _ ->
                AgendaManager.eliminarAgenda(agenda.id)
                Toast.makeText(this, "Agenda eliminada", Toast.LENGTH_SHORT).show()
                cargarLista(etBuscarMedico.text.toString())
            }
            builder.setNegativeButton("Cerrar", null)
        } else {
            builder.setPositiveButton("Cerrar", null)
        }

        builder.show()
    }

    /** Muestra los datos de una agenda de estudio clínico con la grilla de horarios disponibles y turnos reservados. */
    private fun mostrarDetalleAgendaEstudioDialog(agenda: Agenda) {
        val diasStr = agenda.diasSemana.joinToString(", ") { it.name }

        val slots = mutableListOf<LocalTime>()
        var actual = agenda.horaInicio
        val fin = agenda.horaFin
        val duracion = if (agenda.duracionCita > 0) agenda.duracionCita.toLong() else 15L

        while (actual.plusMinutes(duracion) <= fin) {
            slots.add(actual)
            actual = actual.plusMinutes(duracion)
        }

        val grillaHorariosStr = slots.joinToString("\n") { hora ->
            val turno = AppData.TURNOS.find {
                it.agendaId == agenda.id && it.hora == hora && it.estado == EstadoTurno.RESERVADO
            }
            if (turno != null) {
                val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
                val nombrePaciente = paciente?.fullName ?: "Paciente ID ${turno.pacienteId}"
                "• $hora - Reservado (Paciente: $nombrePaciente)"
            } else {
                "• $hora - Disponible"
            }
        }

        val mensaje = """
            Estudio: ${agenda.servicio.name}
            Circuito: Estudios Clínicos
            Días de atención: $diasStr
            Requiere ayuno: ${if (agenda.requiereAyuno) "Sí" else "No"}
            
            --- Horarios de Atención ---
            $grillaHorariosStr
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("Agenda de ${agenda.servicio.name}")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)

        if (SessionState.rol != Rol.MEDICO) {
            builder.setNeutralButton("Eliminar Agenda") { _, _ ->
                AppData.AGENDAS.removeAll { it.id == agenda.id }
                Toast.makeText(this, "Agenda de estudio eliminada", Toast.LENGTH_SHORT).show()
                cargarLista(etBuscarMedico.text.toString())
            }
        }

        builder.show()
    }

    /** Abre el modal para modificar una agenda existente de un médico. */
    private fun abrirModalModificarAgenda(medico: Medico, agenda: Agenda) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_agenda, null)

        dialogView.findViewById<TextView>(R.id.tv_dialog_titulo).text = "Modificar Agenda"
        dialogView.findViewById<TextView>(R.id.tv_dialog_medico_nombre).text = "Médico: Dr/a. ${medico.fullName}"

        dialogView.findViewById<View>(R.id.layout_servicio).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_circuito).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_modalidad).visibility = View.GONE

        val cbLunes = dialogView.findViewById<CheckBox>(R.id.cb_lunes).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.MONDAY) }
        val cbMartes = dialogView.findViewById<CheckBox>(R.id.cb_martes).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.TUESDAY) }
        val cbMiercoles = dialogView.findViewById<CheckBox>(R.id.cb_miercoles).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.WEDNESDAY) }
        val cbJueves = dialogView.findViewById<CheckBox>(R.id.cb_jueves).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.THURSDAY) }
        val cbViernes = dialogView.findViewById<CheckBox>(R.id.cb_viernes).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.FRIDAY) }
        val cbSabado = dialogView.findViewById<CheckBox>(R.id.cb_sabado).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.SATURDAY) }
        val cbDomingo = dialogView.findViewById<CheckBox>(R.id.cb_domingo).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.SUNDAY) }

        val etHoraInicio = dialogView.findViewById<EditText>(R.id.et_hora_inicio).apply { setText(agenda.horaInicio.toString()) }
        val etHoraFin = dialogView.findViewById<EditText>(R.id.et_hora_fin).apply { setText(agenda.horaFin.toString()) }
        val etDuracionCita = dialogView.findViewById<EditText>(R.id.et_duracion_cita).apply { setText(agenda.duracionCita.toString()) }
        val cbAyuno = dialogView.findViewById<CheckBox>(R.id.cb_requiere_ayuno).apply { isChecked = agenda.requiereAyuno }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar Cambios", null)
            .create()

        dialog.show()

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

            val horaInicio = parsearHora(etHoraInicio.text.toString().trim(), agenda.horaInicio)
            val horaFin = parsearHora(etHoraFin.text.toString().trim(), agenda.horaFin)
            val duracionMins = etDuracionCita.text.toString().toIntOrNull() ?: agenda.duracionCita

            val agendaModificada = agenda.copy(
                diasSemana = diasSeleccionados,
                horaInicio = horaInicio,
                horaFin = horaFin,
                duracionCita = duracionMins,
                requiereAyuno = cbAyuno.isChecked
            )

            val cancelados = AgendaManager.modificarAgendaYCancelarTurnos(agendaModificada)
            if (cancelados >= 0) {
                if (cancelados > 0) {
                    Toast.makeText(this, "Agenda modificada. Se cancelaron $cancelados turnos reservados.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Agenda modificada con éxito", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                cargarLista(etBuscarMedico.text.toString())
            } else {
                Toast.makeText(this, "Error al modificar la agenda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }

    private fun parsearHora(texto: String, porDefecto: LocalTime): LocalTime {
        return try {
            LocalTime.parse(texto)
        } catch (e: DateTimeParseException) {
            porDefecto
        }
    }
}
