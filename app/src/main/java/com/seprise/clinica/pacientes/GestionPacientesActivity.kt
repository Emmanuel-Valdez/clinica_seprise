package com.seprise.clinica.pacientes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.seprise.clinica.R
import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Paciente
import java.text.Normalizer

class GestionPacientesActivity : com.seprise.clinica.shell.BaseNavActivity() {

    override fun getContentLayoutId() = R.layout.activity_gestion_pacientes
    override fun getActiveNavItemKey() = "pacientes"

    private lateinit var containerPacientes: LinearLayout
    private lateinit var etBuscarPaciente: EditText

    override fun onContentViewCreated(view: View) {
        containerPacientes = view.findViewById(R.id.container_pacientes)
        etBuscarPaciente = view.findViewById(R.id.et_buscar_paciente)

        view.findViewById<Button>(R.id.btn_agregar_paciente).setOnClickListener {
            abrirModalAgregarPaciente()
        }

        etBuscarPaciente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarPacientes(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarPacientes()
    }

    private fun cargarPacientes(filtro: String = "") {
        containerPacientes.removeAllViews()

        val filtroNorm = normalizarTexto(filtro)
        val pacientesFiltrados = if (filtroNorm.isBlank()) {
            AppData.PACIENTES
        } else {
            AppData.PACIENTES.filter {
                normalizarTexto(it.fullName).contains(filtroNorm) ||
                normalizarTexto(it.dni).contains(filtroNorm)
            }
        }

        if (pacientesFiltrados.isEmpty()) {
            mostrarMensajeVacio("No se encontraron pacientes registrados")
            return
        }

        for (paciente in pacientesFiltrados) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_paciente, containerPacientes, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_paciente)
            val tvDni = item.findViewById<TextView>(R.id.tv_dni_paciente)
            val tvContacto = item.findViewById<TextView>(R.id.tv_contacto_paciente)
            val tvEstado = item.findViewById<TextView>(R.id.tv_estado_paciente)

            tvNombre.text = paciente.fullName
            tvDni.text = "DNI: ${paciente.dni} · Empadronamiento: ${paciente.empadronamiento}"
            tvContacto.text = "Mail: ${paciente.mail} | Tel: ${paciente.telefono}"

            if (paciente.activo) {
                tvEstado.text = "Activo"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_success))
            } else {
                tvEstado.text = "Inhabilitado"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_error))
            }

            item.setOnClickListener {
                abrirModalEditarPaciente(paciente)
            }

            containerPacientes.addView(item)
        }
    }

    private fun mostrarMensajeVacio(mensaje: String) {
        val tvVacio = TextView(this).apply {
            text = mensaje
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@GestionPacientesActivity, R.color.seprise_muted))
            setPadding(0, 24, 0, 0)
        }
        containerPacientes.addView(tvVacio)
    }

    private fun abrirModalAgregarPaciente() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_paciente, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.et_nombre_paciente)
        val etApellido = dialogView.findViewById<EditText>(R.id.et_apellido_paciente)
        val etDni = dialogView.findViewById<EditText>(R.id.et_dni_paciente)
        val etMail = dialogView.findViewById<EditText>(R.id.et_mail_paciente)
        val etTelefono = dialogView.findViewById<EditText>(R.id.et_telefono_paciente)
        val etDireccion = dialogView.findViewById<EditText>(R.id.et_direccion_paciente)
        val cbActivo = dialogView.findViewById<CheckBox>(R.id.cb_activo_paciente)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val dni = etDni.text.toString().trim()
            val mail = etMail.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val direccion = etDireccion.text.toString().trim()
            val activo = cbActivo.isChecked

            if (nombre.isEmpty() || apellido.isEmpty() || dni.isEmpty()) {
                Toast.makeText(this, "Complete los campos obligatorios (Nombre, Apellido, DNI)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoId = (AppData.PACIENTES.maxOfOrNull { it.id } ?: 0) + 1
            val nuevoPaciente = Paciente(
                id = nuevoId,
                nombre = nombre,
                apellido = apellido,
                dni = dni,
                mail = mail,
                telefono = telefono,
                direccion = direccion,
                activo = activo
            )

            val creado = PacientesManager.crearPaciente(nuevoPaciente)
            if (creado) {
                Toast.makeText(this, "Paciente registrado con éxito", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarPacientes(etBuscarPaciente.text.toString())
            } else {
                Toast.makeText(this, "El paciente ya se encuentra registrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirModalEditarPaciente(paciente: Paciente) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_paciente, null)

        dialogView.findViewById<TextView>(R.id.tv_dialog_paciente_titulo).text = "Modificar Paciente"

        val etNombre = dialogView.findViewById<EditText>(R.id.et_nombre_paciente).apply { setText(paciente.nombre) }
        val etApellido = dialogView.findViewById<EditText>(R.id.et_apellido_paciente).apply { setText(paciente.apellido) }
        val etDni = dialogView.findViewById<EditText>(R.id.et_dni_paciente).apply { setText(paciente.dni) }
        val etMail = dialogView.findViewById<EditText>(R.id.et_mail_paciente).apply { setText(paciente.mail) }
        val etTelefono = dialogView.findViewById<EditText>(R.id.et_telefono_paciente).apply { setText(paciente.telefono) }
        val etDireccion = dialogView.findViewById<EditText>(R.id.et_direccion_paciente).apply { setText(paciente.direccion) }
        val cbActivo = dialogView.findViewById<CheckBox>(R.id.cb_activo_paciente).apply { isChecked = paciente.activo }

        val textoAccionInhabilitar = if (paciente.activo) "Inhabilitar" else "Habilitar"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton(textoAccionInhabilitar) { _, _ ->
                if (paciente.activo) {
                    PacientesManager.inhabilitarPaciente(paciente.id)
                    Toast.makeText(this, "Paciente inhabilitado con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    PacientesManager.habilitarPaciente(paciente.id)
                    Toast.makeText(this, "Paciente habilitado con éxito", Toast.LENGTH_SHORT).show()
                }
                cargarPacientes(etBuscarPaciente.text.toString())
            }
            .setPositiveButton("Guardar Cambios") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val apellido = etApellido.text.toString().trim()
                val dni = etDni.text.toString().trim()
                val mail = etMail.text.toString().trim()
                val telefono = etTelefono.text.toString().trim()
                val direccion = etDireccion.text.toString().trim()
                val activo = cbActivo.isChecked

                if (nombre.isNotEmpty() && apellido.isNotEmpty() && dni.isNotEmpty()) {
                    val actualizado = paciente.copy(
                        nombre = nombre,
                        apellido = apellido,
                        dni = dni,
                        mail = mail,
                        telefono = telefono,
                        direccion = direccion,
                        activo = activo
                    )
                    PacientesManager.modificarPaciente(actualizado)
                    Toast.makeText(this, "Paciente actualizado", Toast.LENGTH_SHORT).show()
                    cargarPacientes(etBuscarPaciente.text.toString())
                } else {
                    Toast.makeText(this, "Nombre, Apellido y DNI son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }
}
