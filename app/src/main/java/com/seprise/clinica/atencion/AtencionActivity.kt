package com.seprise.clinica.atencion

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
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
import com.seprise.clinica.models.Paciente
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime

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

        val atencionesFiltradas = if (filtroNorm.isBlank()) {
            atencionesDelMedico
        } else {
            atencionesDelMedico.filter { atencion ->
                val paciente = AppData.PACIENTES.find { it.id == atencion.pacienteId }
                val nombrePaciente = normalizarTexto(paciente?.fullName ?: "")
                val dniPaciente = normalizarTexto(paciente?.dni ?: "")
                val obs = normalizarTexto(atencion.observaciones)

                nombrePaciente.contains(filtroNorm) || dniPaciente.contains(filtroNorm) || obs.contains(filtroNorm)
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
            val tvObs = item.findViewById<TextView>(R.id.tv_observaciones_atencion)

            val paciente = AppData.PACIENTES.find { it.id == atencion.pacienteId }
            tvPaciente.text = paciente?.fullName ?: "Paciente ID ${atencion.pacienteId}"
            tvValor.text = "$${atencion.valorConsulta}"
            tvFechaHora.text = "Fecha: ${atencion.fecha} ${atencion.hora} | Duración: ${atencion.duracion} min"
            tvObs.text = if (atencion.observaciones.isNotEmpty()) "Obs: ${atencion.observaciones}" else "Sin observaciones"

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
        val etValor = dialogView.findViewById<EditText>(R.id.et_valor_consulta)
        val etDuracion = dialogView.findViewById<EditText>(R.id.et_duracion_atencion)
        val etObservaciones = dialogView.findViewById<EditText>(R.id.et_observaciones_atencion)

        val medico = AppData.MEDICOS.find { it.id == medicoIdLogueado }
        val medicoNombre = medico?.fullName ?: "Dr/a. Médico"
        tvMedicoInfoModal.text = "Atendiendo como: Dr/a. $medicoNombre"

        var pacienteSeleccionado: Paciente? = null

        val pacientesActivos = AppData.PACIENTES.filter { it.activo }

        val pacientesInfoList = pacientesActivos.map {
            "${it.fullName} (DNI: ${it.dni})"
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            pacientesInfoList
        )

        actvPaciente.setAdapter(adapter)
        actvPaciente.threshold = 1

        actvPaciente.setOnItemClickListener { parent, _, position, _ ->
            val seleccion = parent.getItemAtPosition(position) as String

            pacienteSeleccionado = pacientesActivos.find {
                "${it.fullName} (DNI: ${it.dni})" == seleccion
            }
        }

        // Si modifica el texto después de seleccionar,
        // deja de considerarse válida la selección anterior.
        actvPaciente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                pacienteSeleccionado = null
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
            val paciente = pacienteSeleccionado ?: pacientesActivos.find {
                "${it.fullName} (DNI: ${it.dni})" == textoIngresado || normalizarTexto(it.fullName) == normalizarTexto(textoIngresado)
            }

            if (paciente == null) {
                Toast.makeText(this, "Seleccione un paciente válido de la lista", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val valorStr = etValor.text.toString().trim()
            val valor = valorStr.toDoubleOrNull() ?: 0.0
            val duracion = etDuracion.text.toString().toIntOrNull() ?: 15
            val observaciones = etObservaciones.text.toString().trim()

            val nuevaAtencion = Atencion(
                id = (AppData.ATENCIONES.maxOfOrNull { it.id } ?: 0) + 1,
                medicoId = medicoIdLogueado,
                pacienteId = paciente.id,
                turnoId = 0,
                fecha = LocalDate.now(),
                hora = LocalTime.now(),
                duracion = duracion,
                observaciones = observaciones,
                valorConsulta = valor
            )

            AtencionManager.registrarAtencion(nuevaAtencion)
            Toast.makeText(this, "Atención registrada con éxito para ${paciente.fullName}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            cargarAtenciones(etBuscarAtencion.text.toString())
        }
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }
}
