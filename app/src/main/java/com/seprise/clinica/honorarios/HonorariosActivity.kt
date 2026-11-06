package com.seprise.clinica.honorarios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seprise.clinica.R
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.models.Atencion
import com.seprise.clinica.models.LiquidacionHonorario
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class HonorariosActivity : AppCompatActivity() {

    private lateinit var spinnerMedico: Spinner
    private lateinit var etDesde: EditText
    private lateinit var etHasta: EditText
    private lateinit var tvResumen: TextView
    private lateinit var containerHonorarios: LinearLayout
    private lateinit var btnLiquidar: Button

    private var pendientes: List<Atencion> = emptyList()
    private var medicoActualId = 0
    private var desdeActual: LocalDate? = null
    private var hastaActual: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSePriseEdgeToEdge()
        setContentView(R.layout.activity_honorarios)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val margen = dp(20)
            v.setPadding(
                systemBars.left + margen,
                systemBars.top + margen,
                systemBars.right + margen,
                systemBars.bottom + margen
            )
            insets
        }

        if (SessionState.rol != Rol.ADMINISTRATIVO) {
            Toast.makeText(this, R.string.cu06_sin_permiso, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        spinnerMedico = findViewById(R.id.spinner_medico_honorarios)
        etDesde = findViewById(R.id.et_honorario_desde)
        etHasta = findViewById(R.id.et_honorario_hasta)
        tvResumen = findViewById(R.id.tv_resumen_honorarios)
        containerHonorarios = findViewById(R.id.container_honorarios)
        btnLiquidar = findViewById(R.id.btn_liquidar_honorarios)

        val medicos = AppData.MEDICOS.map { "Dr/a. ${it.fullName} · ${nombreEspecialidad(it.especialidad.name)}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, medicos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMedico.adapter = adapter

        val hoy = LocalDate.now()
        etDesde.setText(hoy.withDayOfMonth(1).toString())
        etHasta.setText(hoy.withDayOfMonth(hoy.lengthOfMonth()).toString())

        findViewById<Button>(R.id.btn_calcular_honorarios).setOnClickListener { calcular() }
        btnLiquidar.setOnClickListener { confirmarLiquidacion() }

        calcular()
    }

    private fun calcular() {
        val desde = parsearFecha(etDesde) ?: return
        val hasta = parsearFecha(etHasta) ?: return
        if (desde.isAfter(hasta)) {
            Toast.makeText(this, R.string.cu06_error_rango, Toast.LENGTH_SHORT).show()
            limpiarResultado()
            return
        }
        val medico = AppData.MEDICOS.getOrNull(spinnerMedico.selectedItemPosition) ?: return

        medicoActualId = medico.id
        desdeActual = desde
        hastaActual = hasta
        pendientes = HonorariosManager.obtenerAtencionesPendientes(medico.id, desde, hasta)
        render()
    }

    private fun parsearFecha(campo: EditText): LocalDate? = try {
        LocalDate.parse(campo.text.toString().trim())
    } catch (e: DateTimeParseException) {
        Toast.makeText(this, R.string.cu06_error_fecha, Toast.LENGTH_SHORT).show()
        limpiarResultado()
        null
    }

    private fun render() {
        containerHonorarios.removeAllViews()

        if (pendientes.isEmpty()) {
            tvResumen.text = getString(R.string.cu06_sin_atenciones)
            btnLiquidar.visibility = View.GONE
            return
        }

        pendientes.forEach { atencion ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_honorario_atencion, containerHonorarios, false)
            val paciente = AppData.PACIENTES.find { it.id == atencion.pacienteId }

            item.findViewById<TextView>(R.id.tv_paciente_honorario).text = paciente?.fullName ?: "ID ${atencion.pacienteId}"
            item.findViewById<TextView>(R.id.tv_valor_honorario).text = moneda(atencion.valorConsulta)
            item.findViewById<TextView>(R.id.tv_fecha_honorario).text = "${atencion.fecha} ${atencion.hora}"
            item.findViewById<TextView>(R.id.tv_diagnostico_honorario).text = atencion.diagnostico

            containerHonorarios.addView(item)
        }

        tvResumen.text = getString(
            R.string.cu06_resumen,
            pendientes.size,
            moneda(HonorariosManager.calcularTotal(pendientes))
        )
        btnLiquidar.visibility = View.VISIBLE
    }

    private fun confirmarLiquidacion() {
        val medico = AppData.MEDICOS.find { it.id == medicoActualId } ?: return
        val desde = desdeActual ?: return
        val hasta = hastaActual ?: return
        val total = HonorariosManager.calcularTotal(pendientes)

        AlertDialog.Builder(this)
            .setTitle(R.string.cu06_confirm_title)
            .setMessage(
                getString(
                    R.string.cu06_confirm_msg,
                    medico.fullName,
                    periodo(desde, hasta),
                    pendientes.size,
                    moneda(total)
                )
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.cu06_confirm_btn) { _, _ ->
                val liquidacion = HonorariosManager.registrarLiquidacion(medico.id, desde, hasta)
                if (liquidacion == null) {
                    Toast.makeText(this, R.string.cu06_sin_atenciones, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.cu06_liquidado, Toast.LENGTH_SHORT).show()
                    mostrarRecibo(liquidacion)
                }
                calcular()
            }
            .show()
    }

    private fun mostrarRecibo(liquidacion: LiquidacionHonorario) {
        val medico = AppData.MEDICOS.find { it.id == liquidacion.medicoId }
        val atenciones = AppData.ATENCIONES.filter { it.id in liquidacion.atenciones }
        val detalle = atenciones.joinToString("\n") { atencion ->
            val paciente = AppData.PACIENTES.find { it.id == atencion.pacienteId }
            "${fechaLarga(atencion.fecha)} ${atencion.hora} · ${paciente?.fullName ?: "Paciente"} · " +
                "${atencion.diagnostico} · ${moneda(atencion.valorConsulta)}"
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.cu06_recibo_title)
            .setMessage(
                getString(
                    R.string.cu06_recibo_msg,
                    medico?.fullName ?: "Médico ${liquidacion.medicoId}",
                    periodo(liquidacion.fechaDesde, liquidacion.fechaHasta),
                    fechaLarga(liquidacion.fechaEmision),
                    liquidacion.atenciones.size,
                    detalle,
                    moneda(liquidacion.total)
                )
            )
            .setPositiveButton(R.string.cu06_btn_cerrar, null)
            .show()
    }

    private fun limpiarResultado() {
        pendientes = emptyList()
        desdeActual = null
        hastaActual = null
        containerHonorarios.removeAllViews()
        tvResumen.text = ""
        btnLiquidar.visibility = View.GONE
    }

    private fun periodo(desde: LocalDate, hasta: LocalDate): String =
        "${fechaLarga(desde)} - ${fechaLarga(hasta)}"

    private fun fechaLarga(fecha: LocalDate): String = fecha.format(FECHA)

    private fun moneda(valor: Double): String = MONEDA.format(valor)

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private fun nombreEspecialidad(valor: String): String = when (valor) {
        "CARDIOLOGIA" -> "Cardiología"
        "ORTOPEDIA" -> "Ortopedia"
        "PEDIATRIA" -> "Pediatría"
        "FISIOTERAPIA" -> "Fisioterapia"
        "KINESIOLOGIA" -> "Kinesiología"
        "SALUD_MENTAL" -> "Salud mental"
        else -> valor.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private companion object {
        val FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val MONEDA: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"))
    }
}
