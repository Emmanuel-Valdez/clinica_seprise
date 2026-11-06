package com.seprise.clinica.turnos

import android.app.DatePickerDialog
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
import com.seprise.clinica.models.Turno
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

class GestionTurnosActivity : AppCompatActivity() {

    private lateinit var containerMedicos: LinearLayout
    private lateinit var etBuscarMedico: EditText
    private lateinit var rgCircuitoTurnos: RadioGroup
    private lateinit var rgFiltroAdmin: RadioGroup
    private lateinit var tvSubtitulo: TextView

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
        rgCircuitoTurnos = findViewById(R.id.rg_circuito_turnos)
        rgFiltroAdmin = findViewById(R.id.rg_filtro_turnos_admin)
        tvSubtitulo = findViewById(R.id.tv_subtitulo_gestion)

        if (SessionState.rol == Rol.ADMINISTRATIVO) {
            rgFiltroAdmin.visibility = View.VISIBLE
            rgCircuitoTurnos.visibility = View.GONE
            tvSubtitulo.text = "Visualice y gestione los turnos por paciente o por médico."
            etBuscarMedico.hint = "Buscar por nombre o DNI de paciente..."

            rgFiltroAdmin.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId == R.id.rb_turnos_pacientes) {
                    etBuscarMedico.hint = "Buscar por nombre o DNI de paciente..."
                } else {
                    etBuscarMedico.hint = "Buscar por nombre o especialidad de médico..."
                }
                cargarLista(etBuscarMedico.text.toString())
            }
        } else {
            rgFiltroAdmin.visibility = View.GONE
            rgCircuitoTurnos.visibility = View.VISIBLE
            tvSubtitulo.text = "Seleccione un circuito para solicitar su turno."
            etBuscarMedico.hint = "Buscar por nombre o servicio..."

            rgCircuitoTurnos.setOnCheckedChangeListener { _, _ ->
                cargarLista(etBuscarMedico.text.toString())
            }
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

    /** Carga la lista según el filtro activo. */
    private fun cargarLista(filtro: String = "") {
        containerMedicos.removeAllViews()

        if (SessionState.rol == Rol.ADMINISTRATIVO) {
            val esPacientes = rgFiltroAdmin.checkedRadioButtonId == R.id.rb_turnos_pacientes
            if (esPacientes) {
                cargarTurnosPacientesAdmin(filtro)
            } else {
                cargarTurnosMedicosAdmin(filtro)
            }
        } else {
            val esEstudios = rgCircuitoTurnos.checkedRadioButtonId == R.id.rb_turnos_estudios
            if (esEstudios) {
                cargarEstudiosClinicos(filtro)
            } else {
                cargarMedicosConsultorio(filtro)
            }
        }
    }

    /** Renderiza la lista de pacientes con sus turnos asignados para el rol Administrativo. */
    private fun cargarTurnosPacientesAdmin(filtro: String) {
        val filtroNorm = normalizarTexto(filtro)
        val pacientesBase = AppData.PACIENTES.filter { it.activo }

        val pacientesFiltrados = if (filtroNorm.isBlank()) {
            pacientesBase
        } else {
            pacientesBase.filter {
                normalizarTexto(it.fullName).contains(filtroNorm) ||
                normalizarTexto(it.dni).contains(filtroNorm)
            }
        }

        if (pacientesFiltrados.isEmpty()) {
            mostrarMensajeVacio("No se encontraron pacientes")
            return
        }

        for (paciente in pacientesFiltrados) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_medico)
            val tvEspecialidad = item.findViewById<TextView>(R.id.tv_especialidad_medico)
            val tvEstado = item.findViewById<TextView>(R.id.tv_estado_agenda)
            val btnAccion = item.findViewById<Button>(R.id.btn_accion_agenda)

            tvNombre.text = paciente.fullName
            tvEspecialidad.text = "DNI: ${paciente.dni} | Mail: ${paciente.mail}"

            val turnosPaciente = AppData.TURNOS.filter { it.pacienteId == paciente.id }

            if (turnosPaciente.isEmpty()) {
                tvEstado.text = "Sin turnos asignados"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
            } else {
                tvEstado.text = "${turnosPaciente.size} turno(s) asignado(s)"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_blue))
            }

            btnAccion.text = "Ver Turnos"
            btnAccion.setOnClickListener { mostrarTurnosDialog("Paciente ${paciente.fullName}", turnosPaciente) }
            item.setOnClickListener { mostrarTurnosDialog("Paciente ${paciente.fullName}", turnosPaciente) }

            containerMedicos.addView(item)
        }
    }

    /** Renderiza la lista de médicos con sus turnos asignados para el rol Administrativo. */
    private fun cargarTurnosMedicosAdmin(filtro: String) {
        val filtroNorm = normalizarTexto(filtro)
        val medicosBase = AppData.MEDICOS

        val medicosFiltrados = if (filtroNorm.isBlank()) {
            medicosBase
        } else {
            medicosBase.filter {
                normalizarTexto(it.fullName).contains(filtroNorm) ||
                normalizarTexto(it.especialidad.name).contains(filtroNorm) ||
                normalizarTexto(nombreEspecialidad(it.especialidad.name)).contains(filtroNorm)
            }
        }

        if (medicosFiltrados.isEmpty()) {
            mostrarMensajeVacio("No se encontraron médicos")
            return
        }

        for (medico in medicosFiltrados) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_medico)
            val tvEspecialidad = item.findViewById<TextView>(R.id.tv_especialidad_medico)
            val tvEstado = item.findViewById<TextView>(R.id.tv_estado_agenda)
            val btnAccion = item.findViewById<Button>(R.id.btn_accion_agenda)

            tvNombre.text = "Dr/a. ${medico.fullName}"
            tvEspecialidad.text = "Especialidad: ${nombreEspecialidad(medico.especialidad.name)}"

            val turnosMedico = AppData.TURNOS.filter { it.medicoId == medico.id }

            if (turnosMedico.isEmpty()) {
                tvEstado.text = "Sin turnos asignados"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
            } else {
                tvEstado.text = "${turnosMedico.size} turno(s) asignado(s)"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_blue))
            }

            btnAccion.text = "Ver Turnos"
            btnAccion.setOnClickListener { mostrarTurnosDialog("Médico Dr/a. ${medico.fullName}", turnosMedico) }
            item.setOnClickListener { mostrarTurnosDialog("Médico Dr/a. ${medico.fullName}", turnosMedico) }

            containerMedicos.addView(item)
        }
    }

    /** Renderiza la lista de médicos de Consultorio Externo. */
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
                normalizarTexto(it.fullName).contains(filtroNorm) || normalizarTexto(it.especialidad.name).contains(filtroNorm)
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
            tvEspecialidad.text = "Especialidad: ${nombreEspecialidad(medico.especialidad.name)}"

            val agendaExistente = TurnosManager.obtenerAgendaPorMedico(medico.id)
            val turnosReservados = filtrarTurnosPorRol(TurnosManager.obtenerTurnosPorMedico(medico.id))

            if (agendaExistente != null && agendaExistente.circuito == Circuito.CONSULTORIOS_EXTERNOS) {
                tvEstado.text = "Agenda disponible (${turnosReservados.size} turnos)"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_success))
                btnAccion.text = "Sacar Turno"
                btnAccion.isEnabled = true
                btnAccion.setOnClickListener { abrirModalSacarTurnoMedico(medico, agendaExistente) }

                item.setOnClickListener { mostrarTurnosDialog(medico.fullName, turnosReservados) }
            } else {
                tvEstado.text = "Sin agenda configurada"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
                btnAccion.text = "Sin Agenda"
                btnAccion.isEnabled = false
            }

            containerMedicos.addView(item)
        }
    }

    /** Renderiza la lista de Estudios Clínicos disponibles para reservar turno. */
    private fun cargarEstudiosClinicos(filtro: String) {
        val agendasEstudios = AppData.AGENDAS.filter { it.circuito == Circuito.ESTUDIOS_CLINICOS }
        val filtroNorm = normalizarTexto(filtro)

        val estudiosFiltrados = if (filtroNorm.isBlank()) {
            agendasEstudios
        } else {
            agendasEstudios.filter { normalizarTexto(it.servicio.name).contains(filtroNorm) }
        }

        if (estudiosFiltrados.isEmpty()) {
            mostrarMensajeVacio("No hay estudios clínicos configurados actualmente")
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

            btnAccion.text = "Sacar Turno"
            btnAccion.isEnabled = true
            btnAccion.setOnClickListener { abrirModalSacarTurnoEstudio(agenda) }

            val turnosEstudio = filtrarTurnosPorRol(AppData.TURNOS.filter { it.agendaId == agenda.id })
            item.setOnClickListener { mostrarTurnosDialog("Estudio ${agenda.servicio.name}", turnosEstudio) }

            containerMedicos.addView(item)
        }
    }

    private fun mostrarMensajeVacio(mensaje: String) {
        val tvVacio = TextView(this).apply {
            text = mensaje
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@GestionTurnosActivity, R.color.seprise_muted))
            setPadding(0, 24, 0, 0)
        }
        containerMedicos.addView(tvVacio)
    }

    /** Abre el modal para sacar turno con un Médico de Consultorio Externo. */
    private fun abrirModalSacarTurnoMedico(medico: Medico, agenda: Agenda) {
        val infoStr = "Médico: Dr/a. ${medico.fullName}\nEspecialidad: ${nombreEspecialidad(medico.especialidad.name)}\nDías de atención: ${agenda.diasSemana.joinToString(", ") { it.name }}\nHorario: ${agenda.horaInicio} a ${agenda.horaFin}"
        mostrarDialogoFormularioTurno(
            titulo = "Sacar Turno - Consultorio",
            infoText = infoStr,
            medicoId = medico.id,
            agenda = agenda
        )
    }

    /** Abre el modal para sacar turno para un Estudio Clínico. */
    private fun abrirModalSacarTurnoEstudio(agenda: Agenda) {
        val infoStr = "Estudio Clínico: ${agenda.servicio.name}\nDías de atención: ${agenda.diasSemana.joinToString(", ") { it.name }}\nHorario: ${agenda.horaInicio} a ${agenda.horaFin}\nAyuno: ${if (agenda.requiereAyuno) "Sí" else "No"}"
        mostrarDialogoFormularioTurno(
            titulo = "Sacar Turno - Estudio Clínico",
            infoText = infoStr,
            medicoId = agenda.medicoId,
            agenda = agenda
        )
    }

    /** Modal unificado para reserva de turno. */
    private fun mostrarDialogoFormularioTurno(titulo: String, infoText: String, medicoId: Int, agenda: Agenda) {
        if (AppData.PACIENTES.isEmpty()) {
            Toast.makeText(this, "No hay pacientes registrados en el sistema", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sacar_turno, null)

        dialogView.findViewById<TextView>(R.id.tv_dialog_turno_titulo).text = titulo
        dialogView.findViewById<TextView>(R.id.tv_dialog_turno_info).text = infoText

        val spinnerPaciente = dialogView.findViewById<Spinner>(R.id.spinner_paciente)
        val listaPacientesStr = AppData.PACIENTES.map { "ID ${it.id}: ${it.fullName} (DNI ${it.dni})" }
        val adapterPaciente = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaPacientesStr)
        adapterPaciente.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaciente.adapter = adapterPaciente

        if (SessionState.rol == Rol.PACIENTE && SessionState.loggedPersonId != null) {
            val indexPaciente = AppData.PACIENTES.indexOfFirst { it.id == SessionState.loggedPersonId }
            if (indexPaciente >= 0) {
                spinnerPaciente.setSelection(indexPaciente)
                spinnerPaciente.isEnabled = false
            }
        }

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

            val incluirOcupados = cbSobreturno.isChecked
            horariosDisponibles = generarHorariosDisponibles(agenda, fechaIngresada, incluirOcupados).toMutableList()
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

        cbSobreturno.setOnCheckedChangeListener { _, _ -> actualizarHorarios() }

        etFecha.isFocusable = false
        etFecha.isClickable = true
        etFecha.setOnClickListener {
            val base = try {
                LocalDate.parse(etFecha.text.toString().trim())
            } catch (e: Exception) {
                fechaInicial
            }
            DatePickerDialog(
                this,
                { _, anio, mes, dia ->
                    etFecha.setText(LocalDate.of(anio, mes + 1, dia).toString())
                    actualizarHorarios()
                },
                base.year,
                base.monthValue - 1,
                base.dayOfMonth
            ).show()
        }

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

            val diaSemanaTurno = fechaTurno.dayOfWeek
            if (!agenda.diasSemana.contains(diaSemanaTurno) && !cbSobreturno.isChecked) {
                val diasPermitidosStr = agenda.diasSemana.joinToString(", ") { it.name }
                Toast.makeText(
                    this,
                    "No se atiende los $diaSemanaTurno. Días permitidos: $diasPermitidosStr (o marque sobreturno)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (horariosDisponibles.isEmpty()) {
                Toast.makeText(this, "No hay horarios libres en esta fecha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val horaTurno = horariosDisponibles.getOrNull(spinnerHorario.selectedItemPosition) ?: agenda.horaInicio

            if (cbSobreturno.isChecked && AppData.TURNOS.any {
                    it.agendaId == agenda.id && it.fecha == fechaTurno &&
                        it.estado == EstadoTurno.RESERVADO && it.esSobreturno &&
                        it.hora.hour == horaTurno.hour
                }
            ) {
                Toast.makeText(this, "Ya hay un sobreturno reservado en esa hora", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoTurno = Turno(
                id = (AppData.TURNOS.maxOfOrNull { it.id } ?: 0) + 1,
                pacienteId = pacienteSeleccionado.id,
                medicoId = medicoId,
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
                cargarLista(etBuscarMedico.text.toString())
            } else {
                Toast.makeText(this, "Error al crear el turno", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generarHorariosDisponibles(agenda: Agenda, fecha: LocalDate, incluirOcupados: Boolean = false): List<LocalTime> {
        val slots = mutableListOf<LocalTime>()
        var actual = agenda.horaInicio
        val fin = agenda.horaFin
        val duracion = if (agenda.duracionCita > 0) agenda.duracionCita.toLong() else 20L

        while (actual.plusMinutes(duracion) <= fin) {
            slots.add(actual)
            actual = actual.plusMinutes(duracion)
        }

        if (incluirOcupados) return slots

        val ocupados = AppData.TURNOS.filter {
            it.agendaId == agenda.id && it.fecha == fecha && it.estado == EstadoTurno.RESERVADO && !it.esSobreturno
        }.map { it.hora }

        return slots.filter { it !in ocupados }
    }

    /** Muestra la lista de turnos reservados. */
    private fun mostrarTurnosDialog(nombreHeader: String, turnos: List<Turno>) {
        if (turnos.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Turnos de $nombreHeader")
                .setMessage("No hay turnos registrados.")
                .setNegativeButton("Cerrar", null)
                .show()
            return
        }

        val detalleTurnos = turnos.joinToString("\n\n") { turno ->
            val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
            val medico = AppData.MEDICOS.find { it.id == turno.medicoId }
            val pacienteNombre = paciente?.fullName ?: "Paciente ID ${turno.pacienteId}"
            val medicoNombre = medico?.fullName ?: "Médico ID ${turno.medicoId}"
            "• Fecha: ${turno.fecha} ${turno.hora}\n  Paciente: $pacienteNombre\n  Médico: Dr/a. $medicoNombre\n  Estado: ${turno.estado}\n  Sobreturno: ${if (turno.esSobreturno) "Sí" else "No"}"
        }

        val reservados = turnos.filter { it.estado == EstadoTurno.RESERVADO }

        val builder = AlertDialog.Builder(this)
            .setTitle("Turnos de $nombreHeader")
            .setMessage(detalleTurnos)
            .setNegativeButton("Cerrar", null)

        if (reservados.isNotEmpty()) {
            builder.setPositiveButton("Cancelar turno") { _, _ ->
                mostrarSelectorCancelar(reservados)
            }
        }

        builder.show()
    }

    private fun mostrarSelectorCancelar(turnos: List<Turno>) {
        val etiquetas = turnos.map { turno ->
            val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
            "${turno.fecha} ${turno.hora} - ${paciente?.fullName ?: "Paciente ${turno.pacienteId}"}${if (turno.esSobreturno) " (sobreturno)" else ""}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Cancelar turno")
            .setItems(etiquetas) { _, indice ->
                confirmarCancelacion(turnos[indice])
            }
            .setNegativeButton("Volver", null)
            .show()
    }

    private fun confirmarCancelacion(turno: Turno) {
        val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
        AlertDialog.Builder(this)
            .setTitle("Cancelar turno")
            .setMessage("¿Cancelar el turno de ${paciente?.fullName ?: "Paciente ${turno.pacienteId}"} el ${turno.fecha} a las ${turno.hora}?")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                if (TurnosManager.cancelarTurno(turno.id)) {
                    Toast.makeText(this, "Turno cancelado", Toast.LENGTH_SHORT).show()
                    cargarLista(etBuscarMedico.text.toString())
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun filtrarTurnosPorRol(turnos: List<Turno>): List<Turno> =
        if (SessionState.rol == Rol.PACIENTE && SessionState.loggedPersonId != null) {
            turnos.filter { it.pacienteId == SessionState.loggedPersonId }
        } else {
            turnos
        }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }

    private fun nombreEspecialidad(valor: String): String = when (valor) {
        "CARDIOLOGIA" -> "Cardiología"
        "ORTOPEDIA" -> "Ortopedia"
        "PEDIATRIA" -> "Pediatría"
        "FISIOTERAPIA" -> "Fisioterapia"
        "KINESIOLOGIA" -> "Kinesiología"
        "SALUD_MENTAL" -> "Salud mental"
        else -> valor.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
