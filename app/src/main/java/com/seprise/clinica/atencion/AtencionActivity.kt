package com.seprise.clinica.atencion

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seprise.clinica.R
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.models.Atencion
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Paciente
import com.seprise.clinica.models.Turno
import com.seprise.clinica.turnos.TurnosManager
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AtencionActivity : AppCompatActivity() {

    private lateinit var containerAtenciones: LinearLayout
    private lateinit var etBuscarAtencion: EditText
    private lateinit var tvMedicoInfo: TextView

    private val medicoIdLogueado: Int
        get() = SessionState.loggedPersonId ?: 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSePriseEdgeToEdge()
        setContentView(R.layout.activity_gestion_atencion)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        containerAtenciones = findViewById(R.id.container_atenciones)
        etBuscarAtencion = findViewById(R.id.et_buscar_atencion)
        tvMedicoInfo = findViewById(R.id.tv_medico_atencion_info)

        val medico = AppData.MEDICOS.find { it.id == medicoIdLogueado }
        val medicoNombre = medico?.fullName ?: "Dr/a. Médico"
        tvMedicoInfo.text = "Médico atendiendo: Dr/a. $medicoNombre"

        findViewById<Button>(R.id.btn_cargar_atencion).setOnClickListener {
            abrirModalCargarAtencion()
        }

        etBuscarAtencion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarAtenciones(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarAtenciones()
    }

    private fun cargarAtenciones(filtro: String = "") {
        containerAtenciones.removeAllViews()

        val filtroNorm = normalizarTexto(filtro)
        val atencionesDelMedico = AtencionManager.obtenerAtencionesPorMedico(medicoIdLogueado)
            .sortedWith(compareByDescending<Atencion> { it.fecha }.thenByDescending { it.hora })

        val atencionesFiltradas = if (filtroNorm.isBlank()) {
            atencionesDelMedico
        } else {
            atencionesDelMedico.filter { atencion ->
                val paciente = AppData.PACIENTES.find { it.id == atencion.pacienteId }
                val nombrePaciente = normalizarTexto(paciente?.fullName ?: "")
                val empadronamiento = normalizarTexto(paciente?.empadronamiento ?: "")
                val diagnostico = normalizarTexto(atencion.diagnostico)

                nombrePaciente.contains(filtroNorm) ||
                    empadronamiento.contains(filtroNorm) ||
                    diagnostico.contains(filtroNorm)
            }
        }

        if (atencionesFiltradas.isEmpty()) {
            mostrarMensajeVacio("No hay atenciones registradas")
            return
        }

        for (atencion in atencionesFiltradas) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_atencion, containerAtenciones, false)

            val tvPaciente = item.findViewById<TextView>(R.id.tv_paciente_atencion)
            val tvValor = item.findViewById<TextView>(R.id.tv_valor_atencion)
            val tvFechaHora = item.findViewById<TextView>(R.id.tv_fecha_hora_atencion)
            val tvDiagnostico = item.findViewById<TextView>(R.id.tv_diagnostico_atencion)
            val tvIndicaciones = item.findViewById<TextView>(R.id.tv_indicaciones_atencion)
            val tvObs = item.findViewById<TextView>(R.id.tv_observaciones_atencion)
            val tvTurno = item.findViewById<TextView>(R.id.tv_turno_atencion)

            val paciente = AppData.PACIENTES.find { it.id == atencion.pacienteId }
            tvPaciente.text = paciente?.fullName ?: "Paciente ID ${atencion.pacienteId}"
            tvValor.text = "$${atencion.valorConsulta}"
            tvFechaHora.text = "Fecha: ${atencion.fecha} ${atencion.hora} | Duración: ${atencion.duracion} min"
            tvDiagnostico.text = "Diagnóstico: ${atencion.diagnostico}"
            tvIndicaciones.text = if (atencion.indicaciones.isNotEmpty()) {
                "Indicaciones: ${atencion.indicaciones}"
            } else {
                "Indicaciones: sin indicaciones"
            }
            tvObs.text = if (atencion.observaciones.isNotEmpty()) {
                "Obs: ${atencion.observaciones}"
            } else {
                "Sin observaciones"
            }
            tvTurno.text = if (atencion.turnoId != 0) {
                val turno = TurnosManager.obtenerTurnoPorId(atencion.turnoId)
                "Turno #${atencion.turnoId} · ${turno?.hora ?: ""}".trimEnd(' ', '·')
            } else {
                "Sin turno asociado"
            }

            containerAtenciones.addView(item)
        }
    }

    private fun mostrarMensajeVacio(mensaje: String) {
        val tvVacio = TextView(this).apply {
            text = mensaje
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@AtencionActivity, R.color.seprise_muted))
            setPadding(0, 24, 0, 0)
        }
        containerAtenciones.addView(tvVacio)
    }

    private fun abrirModalCargarAtencion() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_atencion, null)

        val tvMedicoInfoModal = dialogView.findViewById<TextView>(R.id.tv_medico_logueado_info)
        val actvPaciente = dialogView.findViewById<AutoCompleteTextView>(R.id.actv_paciente)
        val tvPacienteInfo = dialogView.findViewById<TextView>(R.id.tv_paciente_info)
        val tvHcResumen = dialogView.findViewById<TextView>(R.id.tv_hc_resumen)
        val tvTurnoDia = dialogView.findViewById<TextView>(R.id.tv_turno_dia)
        val etDiagnostico = dialogView.findViewById<EditText>(R.id.et_diagnostico_atencion)
        val etIndicaciones = dialogView.findViewById<EditText>(R.id.et_indicaciones_atencion)
        val etObservaciones = dialogView.findViewById<EditText>(R.id.et_observaciones_atencion)
        val etDuracion = dialogView.findViewById<EditText>(R.id.et_duracion_atencion)
        val etValor = dialogView.findViewById<EditText>(R.id.et_valor_consulta)

        val medico = AppData.MEDICOS.find { it.id == medicoIdLogueado }
        val medicoNombre = medico?.fullName ?: "Dr/a. Médico"
        tvMedicoInfoModal.text = "Atendiendo como: Dr/a. $medicoNombre"

        var pacienteSeleccionado: Paciente? = null

        val pacientes = AppData.PACIENTES.toList()
        val textoPaciente = { paciente: Paciente ->
            "${paciente.fullName} · ${paciente.empadronamiento} · DNI: ${paciente.dni}"
        }

        val adapter = PacienteAdapter(this, pacientes, textoPaciente, ::normalizarTexto)
        actvPaciente.setAdapter(adapter)
        actvPaciente.threshold = 1

        actvPaciente.setOnItemClickListener { parent, _, position, _ ->
            val seleccion = parent.getItemAtPosition(position) as String
            val paciente = pacientes.find { textoPaciente(it) == seleccion }
            pacienteSeleccionado = paciente

            if (paciente != null) {
                tvPacienteInfo.text = if (!paciente.activo) {
                    "Paciente INHABILITADO · EMP: ${paciente.empadronamiento} · DNI: ${paciente.dni}"
                } else {
                    "EMP: ${paciente.empadronamiento} · DNI: ${paciente.dni}"
                }
                tvHcResumen.text = resumenHistoriaClinica(paciente.id)
                tvTurnoDia.text = textoTurnoDelDia(paciente.id)
            }
        }

        // Si modifica el texto después de seleccionar,
        // deja de considerarse válida la selección anterior.
        actvPaciente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pacienteSeleccionado = null
                tvPacienteInfo.text = ""
                tvHcResumen.text = ""
                tvTurnoDia.text = ""
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Registrar Atención", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val textoIngresado = actvPaciente.text.toString().trim()
            val paciente = pacienteSeleccionado ?: pacientes.find {
                textoPaciente(it) == textoIngresado
            }

            if (paciente == null) {
                Toast.makeText(this, "Seleccione un paciente válido de la lista", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val diagnostico = etDiagnostico.text.toString().trim()
            if (diagnostico.isEmpty()) {
                Toast.makeText(this, "El diagnóstico es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val valor = etValor.text.toString().trim().toDoubleOrNull()
            if (valor == null || valor < 0) {
                Toast.makeText(this, "Ingrese un valor de consulta válido (no negativo)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duracion = etDuracion.text.toString().trim().toIntOrNull()
            if (duracion == null || duracion <= 0) {
                Toast.makeText(this, "Ingrese una duración válida (mayor a cero)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val turnoReservado = TurnosManager.buscarTurnoReservadoDeHoy(medicoIdLogueado, paciente.id)
            if (turnoReservado == null) {
                val turnoHoy = TurnosManager.buscarTurnoDeHoy(medicoIdLogueado, paciente.id)
                if (turnoHoy?.estado == EstadoTurno.ATENDIDO) {
                    Toast.makeText(this, "El turno de hoy ya fue atendido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val nuevaAtencion = Atencion(
                id = (AppData.ATENCIONES.maxOfOrNull { it.id } ?: 0) + 1,
                medicoId = medicoIdLogueado,
                pacienteId = paciente.id,
                turnoId = turnoReservado?.id ?: 0,
                fecha = LocalDate.now(),
                hora = LocalTime.now(),
                duracion = duracion,
                observaciones = etObservaciones.text.toString().trim(),
                valorConsulta = valor,
                diagnostico = diagnostico,
                indicaciones = etIndicaciones.text.toString().trim()
            )

            if (AtencionManager.registrarAtencion(nuevaAtencion)) {
                Toast.makeText(this, "Atención registrada con éxito para ${paciente.fullName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarAtenciones(etBuscarAtencion.text.toString())
            } else {
                Toast.makeText(this, "No se pudo registrar la atención (turno ya atendido o datos inválidos)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resumenHistoriaClinica(pacienteId: Int): String {
        val atenciones = AtencionManager.obtenerAtencionesPorPaciente(pacienteId)
        if (atenciones.isEmpty()) return "Sin antecedentes registrados."

        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return atenciones.joinToString("\n") { atencion ->
            val medico = AppData.MEDICOS.find { it.id == atencion.medicoId }
            val indicaciones = atencion.indicaciones.ifBlank { "sin indicaciones" }
            "${atencion.fecha.format(formato)} · Dr/a. ${medico?.fullName ?: "Médico"} · " +
                "${atencion.diagnostico} — $indicaciones"
        }
    }

    private fun textoTurnoDelDia(pacienteId: Int): String {
        val turnoReservado = TurnosManager.buscarTurnoReservadoDeHoy(medicoIdLogueado, pacienteId)
        if (turnoReservado != null) {
            return textoTurno(turnoReservado).let { "Turno reservado hoy: $it (se vinculará a la atención)" }
        }

        val turnoHoy = TurnosManager.buscarTurnoDeHoy(medicoIdLogueado, pacienteId)
        if (turnoHoy?.estado == EstadoTurno.ATENDIDO) {
            return "Turno de hoy ${turnoHoy.hora} ya ATENDIDO: no se puede registrar otra atención sobre ese turno."
        }

        return "Sin turno reservado hoy: la atención se guarda sin turno."
    }

    private fun textoTurno(turno: Turno): String {
        val tipo = if (turno.esSobreturno) "sobreturno" else "consulta"
        return "${turno.hora} · ${turno.duracion} min · $tipo"
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }
}

/** Autocomplete que busca por nombre, apellido o n° de empadronamiento (sin tildes). */
private class PacienteAdapter(
    context: Context,
    private val pacientes: List<Paciente>,
    private val texto: (Paciente) -> String,
    private val normalizar: (String) -> String
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val consulta = normalizar(constraint?.toString().orEmpty())
            val items = if (consulta.isBlank()) {
                pacientes.map(texto)
            } else {
                pacientes.filter {
                    normalizar(it.fullName).contains(consulta) ||
                        normalizar(it.empadronamiento).contains(consulta)
                }.map(texto)
            }
            return FilterResults().apply {
                values = items
                count = items.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            setNotifyOnChange(false)
            clear()
            (results?.values as? List<String>)?.let { addAll(it) }
            notifyDataSetChanged()
        }
    }
}
