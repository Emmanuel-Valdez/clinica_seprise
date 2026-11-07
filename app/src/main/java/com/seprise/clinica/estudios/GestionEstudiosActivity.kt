package com.seprise.clinica.estudios

import android.app.DatePickerDialog
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
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.seprise.clinica.R
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.models.Estudio
import com.seprise.clinica.models.Paciente
import com.seprise.clinica.models.Servicio
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

class GestionEstudiosActivity : com.seprise.clinica.shell.BaseNavActivity() {

    override fun getContentLayoutId() = R.layout.activity_gestion_estudios
    override fun getActiveNavItemKey() = "estudios"

    private lateinit var containerEstudios: LinearLayout
    private lateinit var etBuscarEstudio: EditText
    private lateinit var btnCargarEstudio: Button

    override fun onContentViewCreated(view: View) {
        containerEstudios = view.findViewById(R.id.container_estudios)
        etBuscarEstudio = view.findViewById(R.id.et_buscar_estudio)
        btnCargarEstudio = view.findViewById(R.id.btn_cargar_estudio)

        if (SessionState.rol == Rol.ADMINISTRATIVO) {
            btnCargarEstudio.visibility = View.VISIBLE
            btnCargarEstudio.setOnClickListener {
                abrirModalCargarEstudio()
            }
        } else {
            btnCargarEstudio.visibility = View.GONE
        }

        etBuscarEstudio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarEstudios(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarEstudios()
    }

    private fun cargarEstudios(filtro: String = "") {
        containerEstudios.removeAllViews()

        val estudiosBase = if (SessionState.rol == Rol.PACIENTE && SessionState.loggedPersonId != null) {
            AppData.ESTUDIOS.filter { it.pacienteId == SessionState.loggedPersonId }
        } else {
            AppData.ESTUDIOS
        }

        val filtroNorm = normalizarTexto(filtro)

        val estudiosFiltrados = if (filtroNorm.isBlank()) {
            estudiosBase
        } else {
            estudiosBase.filter { estudio ->
                val paciente = AppData.PACIENTES.find { it.id == estudio.pacienteId }
                val nombrePaciente = paciente?.fullName ?: ""
                normalizarTexto(estudio.nombre).contains(filtroNorm) ||
                        normalizarTexto(nombrePaciente).contains(filtroNorm) ||
                        normalizarTexto(estudio.observaciones).contains(filtroNorm)
            }
        }

        if (estudiosFiltrados.isEmpty()) {
            val tvVacio = TextView(this).apply {
                text = "No hay estudios clínicos registrados"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@GestionEstudiosActivity, R.color.seprise_muted))
                setPadding(0, 24, 0, 0)
            }
            containerEstudios.addView(tvVacio)
            return
        }

        for (estudio in estudiosFiltrados) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_estudio, containerEstudios, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_estudio)
            val tvPaciente = item.findViewById<TextView>(R.id.tv_paciente_estudio)
            val tvFecha = item.findViewById<TextView>(R.id.tv_fecha_estudio)
            val tvInfo = item.findViewById<TextView>(R.id.tv_info_estudio)

            val paciente = AppData.PACIENTES.find { it.id == estudio.pacienteId }

            tvNombre.text = "Estudio: ${estudio.nombre}"
            tvPaciente.text = "Paciente: ${paciente?.fullName ?: "ID ${estudio.pacienteId}"}"
            tvFecha.text = "Fecha: ${estudio.fecha} ${estudio.hora}"
            tvInfo.text = if (estudio.observaciones.isNotEmpty()) "Observaciones: ${estudio.observaciones}" else "Sin observaciones"

            item.setOnClickListener {
                mostrarDetalleEstudioDialog(estudio)
            }

            containerEstudios.addView(item)
        }
    }

    private fun abrirModalCargarEstudio() {
        if (AppData.PACIENTES.isEmpty()) {
            Toast.makeText(this, "No hay pacientes registrados en el sistema", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_estudio, null)

        val actvPaciente = dialogView.findViewById<AutoCompleteTextView>(R.id.actv_paciente_estudio)
        val pacientesActivos = AppData.PACIENTES.filter { it.activo }
        val pacientesInfoList = pacientesActivos.map { "${it.fullName} (DNI: ${it.dni})" }

        val adapterPaciente = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pacientesInfoList)
        actvPaciente.setAdapter(adapterPaciente)
        actvPaciente.threshold = 1

        var pacienteSeleccionado: Paciente? = null
        actvPaciente.setOnItemClickListener { parent, _, position, _ ->
            val seleccion = parent.getItemAtPosition(position) as String
            pacienteSeleccionado = pacientesActivos.find { "${it.fullName} (DNI: ${it.dni})" == seleccion }
        }

        actvPaciente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pacienteSeleccionado = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val spinnerTipo = dialogView.findViewById<Spinner>(R.id.spinner_tipo_estudio)
        val listaTipos = listOf(
            Servicio.ANALISIS_LABORATORIO.name,
            Servicio.RADIOGRAFIA.name,
            Servicio.ECOGRAFIA.name,
            "ESTUDIO_GENERAL"
        )
        val adapterTipo = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaTipos)
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterTipo

        val etFecha = dialogView.findViewById<EditText>(R.id.et_fecha_estudio)
        val etHora = dialogView.findViewById<EditText>(R.id.et_hora_estudio)
        val etObservaciones = dialogView.findViewById<EditText>(R.id.et_observaciones_estudio)

        val fechaInicial = LocalDate.now()
        etFecha.setText(fechaInicial.toString())
        etHora.setText(LocalTime.now().withSecond(0).withNano(0).toString())

        etFecha.isFocusable = false
        etFecha.isClickable = true
        etFecha.setOnClickListener {
            val base = try { LocalDate.parse(etFecha.text.toString().trim()) } catch (e: Exception) { fechaInicial }
            DatePickerDialog(
                this,
                { _, anio, mes, dia -> etFecha.setText(LocalDate.of(anio, mes + 1, dia).toString()) },
                base.year,
                base.monthValue - 1,
                base.dayOfMonth
            ).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar Estudio", null)
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

            val tipoEstudio = spinnerTipo.selectedItem.toString()
            val fechaStr = etFecha.text.toString().trim()
            val horaStr = etHora.text.toString().trim()
            val observaciones = etObservaciones.text.toString().trim()

            val fecha = try {
                LocalDate.parse(fechaStr)
            } catch (e: DateTimeParseException) {
                Toast.makeText(this, "Formato de fecha inválido (AAAA-MM-DD)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hora = try {
                LocalTime.parse(horaStr)
            } catch (e: DateTimeParseException) {
                Toast.makeText(this, "Formato de hora inválido (HH:MM)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoEstudio = Estudio(
                id = (AppData.ESTUDIOS.maxOfOrNull { it.id } ?: 0) + 1,
                nombre = tipoEstudio,
                pacienteId = paciente.id,
                fecha = fecha,
                hora = hora,
                observaciones = observaciones
            )

            val creado = EstudiosManager.crearEstudio(nuevoEstudio)
            if (creado) {
                Toast.makeText(this, "Estudio cargado con éxito para ${paciente.fullName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarEstudios(etBuscarEstudio.text.toString())
            } else {
                Toast.makeText(this, "Error al guardar el estudio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDetalleEstudioDialog(estudio: Estudio) {
        val paciente = AppData.PACIENTES.find { it.id == estudio.pacienteId }

        val mensaje = """
            Estudio: ${estudio.nombre}
            Paciente: ${paciente?.fullName ?: "ID ${estudio.pacienteId}"}
            Fecha y Hora: ${estudio.fecha} ${estudio.hora}
            Observaciones: ${if (estudio.observaciones.isNotEmpty()) estudio.observaciones else "Ninguna"}
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("Detalle del Estudio")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)

        if (SessionState.rol == Rol.ADMINISTRATIVO) {
            builder.setNeutralButton("Eliminar Estudio") { _, _ ->
                EstudiosManager.eliminarEstudio(estudio.id)
                Toast.makeText(this, "Estudio eliminado", Toast.LENGTH_SHORT).show()
                cargarEstudios(etBuscarEstudio.text.toString())
            }
        }

        builder.show()
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }
}
