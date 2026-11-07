package com.seprise.clinica.insumos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.seprise.clinica.R
import com.seprise.clinica.core.AppData
import com.seprise.clinica.models.Insumo
import java.text.Normalizer

class GestionInsumosActivity : com.seprise.clinica.shell.BaseNavActivity() {

    override fun getContentLayoutId() = R.layout.activity_gestion_insumos
    override fun getActiveNavItemKey() = "insumos"

    private lateinit var containerInsumos: LinearLayout
    private lateinit var etBuscarInsumo: EditText

    override fun onContentViewCreated(view: View) {
        containerInsumos = view.findViewById(R.id.container_insumos)
        etBuscarInsumo = view.findViewById(R.id.et_buscar_insumo)

        view.findViewById<Button>(R.id.btn_agregar_insumo).setOnClickListener {
            abrirModalAgregarInsumo()
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
        containerInsumos.removeAllViews()

        val filtroNorm = normalizarTexto(filtro)
        val insumosFiltrados = if (filtroNorm.isBlank()) {
            AppData.INSUMOS
        } else {
            AppData.INSUMOS.filter {
                normalizarTexto(it.nombre).contains(filtroNorm)
            }
        }

        if (insumosFiltrados.isEmpty()) {
            mostrarMensajeVacio("No se encontraron insumos en el inventario")
            return
        }

        for (insumo in insumosFiltrados) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_insumo, containerInsumos, false)

            val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_insumo)
            val tvCantidad = item.findViewById<TextView>(R.id.tv_cantidad_insumo)

            tvNombre.text = insumo.nombre
            tvCantidad.text = "Stock: ${insumo.cantidad}"

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

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }
}
