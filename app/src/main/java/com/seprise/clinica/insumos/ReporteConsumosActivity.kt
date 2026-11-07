package com.seprise.clinica.insumos

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.seprise.clinica.R
import java.time.format.DateTimeFormatter

class ReporteConsumosActivity : com.seprise.clinica.shell.BaseNavActivity() {

    override fun getContentLayoutId() = R.layout.activity_reporte_consumos
    override fun getActiveNavItemKey() = "insumos"

    private lateinit var containerConsumos: LinearLayout
    private lateinit var rgFiltro: RadioGroup

    override fun onContentViewCreated(view: View) {
        containerConsumos = view.findViewById(R.id.container_consumos)
        rgFiltro = view.findViewById(R.id.rg_filtro_tipo)

        rgFiltro.setOnCheckedChangeListener { _, _ -> cargarConsumos() }

        cargarConsumos()
    }

    private fun cargarConsumos() {
        containerConsumos.removeAllViews()

        val tipo = when (rgFiltro.checkedRadioButtonId) {
            R.id.rb_atenciones -> "atencion"
            R.id.rb_estudios -> "estudio"
            else -> null
        }

        val consumos = InsumosManager.obtenerConsumosPorPrestacion(tipo)

        if (consumos.isEmpty()) {
            val tvVacio = TextView(this).apply {
                text = "Sin consumos registrados"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@ReporteConsumosActivity, R.color.seprise_muted))
                setPadding(0, 24, 0, 0)
            }
            containerConsumos.addView(tvVacio)
            return
        }

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        for (consumo in consumos) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundResource(R.drawable.bg_nav_active)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dp(8)
                layoutParams = lp
            }

            val tvDetalle = TextView(this).apply {
                text = consumo.detalle
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTextColor(ContextCompat.getColor(this@ReporteConsumosActivity, R.color.seprise_ink))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            card.addView(tvDetalle)

            for ((nombre, cantidad) in consumo.insumos) {
                val tvInsumo = TextView(this).apply {
                    text = "$nombre x$cantidad"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTextColor(ContextCompat.getColor(this@ReporteConsumosActivity, R.color.seprise_ink))
                    setPadding(dp(8), dp(2), 0, 0)
                }
                card.addView(tvInsumo)
            }

            val tvFecha = TextView(this).apply {
                text = consumo.fecha.format(formatter)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(ContextCompat.getColor(this@ReporteConsumosActivity, R.color.seprise_muted))
                setPadding(0, dp(6), 0, 0)
            }
            card.addView(tvFecha)

            containerConsumos.addView(card)
        }
    }
}
