package com.seprise.clinica.insumos

import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.seprise.clinica.R
import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Insumo
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class GestionInsumosActivity : com.seprise.clinica.shell.BaseNavActivity() {

    override fun getContentLayoutId() = R.layout.activity_gestion_insumos
    override fun getActiveNavItemKey() = "insumos"

    private lateinit var containerInsumos: LinearLayout
    private lateinit var etBuscarInsumo: EditText
    private lateinit var cbSoloActivos: CheckBox

    override fun onContentViewCreated(view: View) {
        containerInsumos = view.findViewById(R.id.container_insumos)
        etBuscarInsumo = view.findViewById(R.id.et_buscar_insumo)
        cbSoloActivos = view.findViewById(R.id.cb_solo_activos)

        view.findViewById<Button>(R.id.btn_agregar_insumo).setOnClickListener {
            abrirModalAgregarInsumo()
        }

        view.findViewById<Button>(R.id.btn_ver_consumos).setOnClickListener {
            startActivity(android.content.Intent(this, ReporteConsumosActivity::class.java))
        }

        cbSoloActivos.setOnCheckedChangeListener { _, _ ->
            cargarInsumos(etBuscarInsumo.text.toString())
        }

        etBuscarInsumo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarInsumos(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarInsumos()
    }

    private fun cargarInsumos(filtro: String = "") {
        InsumosManager.actualizarEstadoVencimiento()
        containerInsumos.removeAllViews()

        val filtroNorm = normalizarTexto(filtro)
        val insumosFiltrados = if (filtroNorm.isBlank()) {
            AppData.INSUMOS
        } else {
            AppData.INSUMOS.filter {
                normalizarTexto(it.nombre).contains(filtroNorm)
            }
        }

        val insumosMostrar = if (cbSoloActivos.isChecked) {
            insumosFiltrados.filter { it.activo }
        } else {
            insumosFiltrados
        }

        if (insumosMostrar.isEmpty()) {
            mostrarMensajeVacio("No se encontraron insumos en el inventario")
            return
        }

        for (insumo in insumosMostrar) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_insumo, containerInsumos, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_insumo)
            val tvCantidad = item.findViewById<TextView>(R.id.tv_cantidad_insumo)
            val tvEstado = item.findViewById<TextView>(R.id.tv_estado_insumo)
            val btnConsumir = item.findViewById<Button>(R.id.btn_consumir_insumo)

            tvNombre.text = insumo.nombre
            tvCantidad.text = "Stock: ${insumo.cantidad}"

            val hoy = LocalDate.now()
            val diasRestantes = ChronoUnit.DAYS.between(hoy, insumo.fechaVencimiento)

            if (!insumo.activo) {
                tvEstado.text = "VENCIDO"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_error))
                tvEstado.visibility = View.VISIBLE
                tvNombre.paintFlags = tvNombre.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                btnConsumir.isEnabled = false
                btnConsumir.alpha = 0.4f
            } else if (diasRestantes <= 30) {
                tvEstado.text = "PRÓXIMO A VENCER"
                tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_warn))
                tvEstado.visibility = View.VISIBLE
            } else {
                tvEstado.visibility = View.GONE
            }

            btnConsumir.setOnClickListener {
                abrirModalConsumirInsumo(insumo)
            }

            item.setOnClickListener {
                abrirModalEditarInsumo(insumo)
            }

            containerInsumos.addView(item)
        }
    }

    private fun mostrarMensajeVacio(mensaje: String) {
        val tvVacio = TextView(this).apply {
            text = mensaje
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@GestionInsumosActivity, R.color.seprise_muted))
            setPadding(0, 24, 0, 0)
        }
        containerInsumos.addView(tvVacio)
    }

    private fun abrirModalAgregarInsumo() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_insumo, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.et_nombre_insumo)
        val etCantidad = dialogView.findViewById<EditText>(R.id.et_cantidad_insumo)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val cantidadStr = etCantidad.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre del insumo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cantidad = cantidadStr.toIntOrNull() ?: 0

            val nuevoInsumo = Insumo(
                id = (AppData.INSUMOS.maxOfOrNull { it.id } ?: 0) + 1,
                nombre = nombre,
                cantidad = cantidad
            )

            val creado = InsumosManager.crearInsumo(nuevoInsumo)
            if (creado) {
                Toast.makeText(this, "Insumo agregado con éxito", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarInsumos(etBuscarInsumo.text.toString())
            } else {
                Toast.makeText(this, "Error al agregar el insumo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirModalEditarInsumo(insumo: Insumo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_insumo, null)

        dialogView.findViewById<TextView>(R.id.tv_dialog_insumo_titulo).text = "Modificar Insumo"

        val etNombre = dialogView.findViewById<EditText>(R.id.et_nombre_insumo).apply { setText(insumo.nombre) }
        val etCantidad = dialogView.findViewById<EditText>(R.id.et_cantidad_insumo).apply { setText(insumo.cantidad.toString()) }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Eliminar") { _, _ ->
                InsumosManager.eliminarInsumo(insumo.id)
                Toast.makeText(this, "Insumo eliminado", Toast.LENGTH_SHORT).show()
                cargarInsumos(etBuscarInsumo.text.toString())
            }
            .setPositiveButton("Guardar Cambios") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val cantidad = etCantidad.text.toString().toIntOrNull() ?: insumo.cantidad

                if (nombre.isNotEmpty()) {
                    val actualizado = insumo.copy(nombre = nombre, cantidad = cantidad)
                    InsumosManager.modificarInsumo(actualizado)
                    Toast.makeText(this, "Insumo actualizado", Toast.LENGTH_SHORT).show()
                    cargarInsumos(etBuscarInsumo.text.toString())
                }
            }
            .show()
    }

    private fun abrirModalConsumirInsumo(insumo: Insumo) {
        if (!insumo.activo) {
            Toast.makeText(this, "No se puede consumir un insumo vencido", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_consumir_insumo, null)

        dialogView.findViewById<TextView>(R.id.tv_insumo_info).text = "${insumo.nombre} · Stock actual: ${insumo.cantidad}"

        val etCantidad = dialogView.findViewById<EditText>(R.id.et_cantidad_consumo)
        val rgTipo = dialogView.findViewById<RadioGroup>(R.id.rg_tipo_prestacion)
        val etPrestacionId = dialogView.findViewById<EditText>(R.id.et_prestacion_id)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Consumir", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0
            val prestacionId = etPrestacionId.text.toString().toIntOrNull() ?: 0

            if (cantidad <= 0) {
                Toast.makeText(this, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rgTipo.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Seleccione el tipo de prestación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (prestacionId <= 0) {
                Toast.makeText(this, "Ingrese un ID de prestación válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipoPrestacion = if (dialogView.findViewById<RadioButton>(rgTipo.checkedRadioButtonId).id == R.id.rb_atencion) {
                "atencion"
            } else {
                "estudio"
            }

            val resultado = InsumosManager.consumirInsumo(insumo.id, cantidad, prestacionId, tipoPrestacion)
            if (resultado) {
                val insumoActualizado = InsumosManager.obtenerInsumoPorId(insumo.id)
                if (insumoActualizado != null && insumoActualizado.cantidad < 0) {
                    Toast.makeText(this, "Atención: stock negativo (${insumoActualizado.cantidad})", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Consumo registrado", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                cargarInsumos(etBuscarInsumo.text.toString())
            } else {
                Toast.makeText(this, "Error al registrar el consumo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }
}
