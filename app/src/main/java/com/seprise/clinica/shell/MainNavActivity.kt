package com.seprise.clinica.shell

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seprise.clinica.R
import com.seprise.clinica.auth.LoginActivity
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Turno
import com.seprise.clinica.pacientes.PerfilPacienteActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * CU01 — shell de navegación. El contenido central es el inicio (home);
 * cada módulo se abre como Activity independiente.
 */
class MainNavActivity : AppCompatActivity() {

    private data class Modulo(val clave: String, @param:StringRes val nombre: Int, val clase: String)

    private data class FilaHome(val texto: String, val color: Int? = null)

    private val pacientes = Modulo("pacientes", R.string.cu01_nav_pacientes, "com.seprise.clinica.pacientes.GestionPacientesActivity")
    private val agenda = Modulo("agenda", R.string.cu01_nav_agenda, "com.seprise.clinica.agenda.GestionAgendaActivity")
    private val turnos = Modulo("turnos", R.string.cu01_nav_turnos, "com.seprise.clinica.turnos.GestionTurnosActivity")
    private val atencion = Modulo("atencion", R.string.cu01_nav_atencion, "com.seprise.clinica.atencion.AtencionActivity")
    private val estudios = Modulo("estudios", R.string.cu01_nav_estudios, "com.seprise.clinica.estudios.GestionEstudiosActivity")
    private val insumos = Modulo("insumos", R.string.cu01_nav_insumos, "com.seprise.clinica.insumos.GestionInsumosActivity")
    private val honorarios = Modulo("honorarios", R.string.cu01_nav_honorarios, "com.seprise.clinica.honorarios.HonorariosActivity")

    private lateinit var contenedorItems: LinearLayout
    private var itemInicio: TextView? = null
    private var activo: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSePriseEdgeToEdge()
        setContentView(R.layout.activity_main_nav)
        val root = findViewById<View>(R.id.cu01_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val rol = SessionState.rol
        if (rol == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        contenedorItems = findViewById(R.id.cu01_nav_items)

        // Nombre de la persona logueada en la barra superior
        findViewById<TextView>(R.id.txt_nombre_usuario)?.text = obtenerNombrePersonaLogueada()

        // El inicio es el contenido por defecto; el logo solo vuelve a marcar la pestaña.
        findViewById<ImageView>(R.id.btn_home_logo).setOnClickListener {
            itemInicio?.let { marcarActivo(it) }
        }

        findViewById<Button>(R.id.cu01_btn_perfil).apply {
            visibility = if (rol == Rol.PACIENTE) View.VISIBLE else View.GONE
            setOnClickListener { startActivity(Intent(this@MainNavActivity, PerfilPacienteActivity::class.java)) }
        }

        findViewById<Button>(R.id.cu01_btn_logout).setOnClickListener {
            SessionState.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        armarItems(modulosDe(rol))
    }

    override fun onResume() {
        super.onResume()
        itemInicio?.let { marcarActivo(it) }
        SessionState.rol?.let { renderHome(it) }
    }

    /** Regla de acceso por rol: administrativo gestiona todo; médico atiende/agenda; paciente agenda lo propio. */
    private fun modulosDe(rol: Rol): List<Modulo> = when (rol) {
        Rol.ADMINISTRATIVO -> listOf(pacientes, agenda, turnos, estudios, insumos, honorarios)
        Rol.MEDICO -> listOf(atencion, agenda, estudios)
        Rol.PACIENTE -> listOf(turnos, estudios)
    }

    private fun armarItems(modulos: List<Modulo>) {
        contenedorItems.removeAllViews()
        val pad = dp(12)

        val inicio = TextView(this).apply {
            text = "Inicio"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_nav))
            setPadding(pad, pad, pad, pad)
            setTextColor(getColor(R.color.seprise_muted))
            isClickable = true
            setOnClickListener { marcarActivo(this) }
        }
        itemInicio = inicio
        contenedorItems.addView(inicio)
        marcarActivo(inicio)

        modulos.forEach { modulo ->
            val item = TextView(this).apply {
                setText(modulo.nombre)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_nav))
                setPadding(pad, pad, pad, pad)
                setTextColor(getColor(R.color.seprise_muted))
                isClickable = true
                setOnClickListener {
                    marcarActivo(this)
                    abrirModulo(modulo)
                }
            }
            contenedorItems.addView(item)
        }
    }

    private fun marcarActivo(item: TextView) {
        activo?.apply {
            setBackgroundResource(0)
            setTextColor(getColor(R.color.seprise_muted))
        }
        item.setBackgroundResource(R.drawable.bg_nav_active)
        item.setTextColor(getColor(R.color.seprise_nav_text))
        activo = item
    }

    /** Navegación defensiva: si el módulo aún no existe, avisa en vez de crashear. */
    private fun abrirModulo(modulo: Modulo) {
        val intent = Intent().setClassName(packageName, modulo.clase)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.cu01_proximamente, Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderHome(rol: Rol) {
        val contenedor = findViewById<LinearLayout>(R.id.home_contenido) ?: return
        contenedor.removeAllViews()
        val hoy = LocalDate.now()
        when (rol) {
            Rol.ADMINISTRATIVO -> renderHomeAdmin(contenedor, hoy)
            Rol.MEDICO -> renderHomeMedico(contenedor, hoy)
            Rol.PACIENTE -> renderHomePaciente(contenedor, hoy)
        }
    }

    private fun renderHomeAdmin(contenedor: LinearLayout, hoy: LocalDate) {
        val admin = SessionState.loggedPersonId?.let { id -> AppData.ADMINISTRADORES.find { it.id == id } }
        agregarEncabezado(contenedor, "Hola, ${admin?.nombre ?: "Laura"}", "Administrativo · ${fechaLarga(hoy)}")

        val turnosHoy = AppData.TURNOS.filter { it.fecha == hoy }.sortedBy { it.hora }
        agregarTarjeta(
            contenedor,
            "Turnos de hoy",
            turnosHoy.take(5).map { FilaHome(descripcionTurno(it, incluirMedico = true), colorEstado(it.estado)) },
            "No hay turnos para hoy.",
            "Ver agenda"
        ) { abrirModulo(agenda) }

        agregarKpis(
            contenedor,
            listOf(
                "Turnos hoy" to turnosHoy.size.toString(),
                "Pacientes" to AppData.PACIENTES.count { it.activo }.toString(),
                "Estudios pendientes" to AppData.ESTUDIOS.count { !it.fecha.isBefore(hoy) }.toString(),
                "Insumos bajos" to AppData.INSUMOS.count { it.cantidad < 20 }.toString()
            )
        )

        agregarAccionPrincipal(contenedor, "Sacar turno") { abrirModulo(turnos) }

        val acciones: List<Pair<String, () -> Unit>> = listOf(
            "Cargar agenda" to { abrirModulo(agenda) },
            "Nuevo estudio" to { abrirModulo(estudios) },
            "Nuevo insumo" to { abrirModulo(insumos) }
        )
        agregarAccionesSecundarias(contenedor, acciones)

        val estudiosRecientes = AppData.ESTUDIOS.sortedByDescending { it.fecha }.take(3).map { estudio ->
            val paciente = AppData.PACIENTES.find { it.id == estudio.pacienteId }
            FilaHome("${estudio.nombre} · ${paciente?.fullName ?: "Paciente"} · ${estudio.fecha.format(DateTimeFormatter.ofPattern("dd/MM"))}")
        }
        agregarTarjeta(contenedor, "Últimos estudios", estudiosRecientes, "Sin estudios registrados.", "Ver estudios") {
            abrirModulo(estudios)
        }

        val stockBajo = AppData.INSUMOS.filter { it.cantidad < 20 }.sortedBy { it.cantidad }.take(3).map { insumo ->
            FilaHome("${insumo.nombre} · stock ${insumo.cantidad}", R.color.seprise_warn)
        }
        agregarTarjeta(contenedor, "Stock bajo", stockBajo, "Sin insumos bajo stock.", "Ver insumos") {
            abrirModulo(insumos)
        }
    }

    private fun renderHomeMedico(contenedor: LinearLayout, hoy: LocalDate) {
        val id = SessionState.loggedPersonId ?: 1
        val medico = AppData.MEDICOS.find { it.id == id }
        agregarEncabezado(
            contenedor,
            "Hola, ${medico?.nombre ?: "Doctor"}",
            "${nombreEspecialidad(medico?.especialidad?.name ?: "Médico")} · ${fechaLarga(hoy)}"
        )

        val turnosHoy = AppData.TURNOS.filter { it.medicoId == id && it.fecha == hoy }.sortedBy { it.hora }
        agregarTarjeta(
            contenedor,
            "Mi agenda de hoy",
            turnosHoy.take(5).map { FilaHome(descripcionTurno(it, incluirMedico = false), colorEstado(it.estado)) },
            "No tenés turnos para hoy.",
            "Ver agenda"
        ) { abrirModulo(agenda) }

        val atendidos = turnosHoy.count { it.estado == EstadoTurno.ATENDIDO }
        val estudiosDerivados = AppData.ESTUDIOS.count { estudio ->
            AppData.TURNOS.any { it.medicoId == id && it.pacienteId == estudio.pacienteId }
        }
        agregarKpis(
            contenedor,
            listOf(
                "Turnos hoy" to turnosHoy.size.toString(),
                "Atendidos" to atendidos.toString(),
                "Estudios derivados" to estudiosDerivados.toString()
            )
        )

        agregarAccionPrincipal(contenedor, "Registrar atención") { abrirModulo(atencion) }
    }

    private fun renderHomePaciente(contenedor: LinearLayout, hoy: LocalDate) {
        val id = SessionState.loggedPersonId ?: 1
        val paciente = AppData.PACIENTES.find { it.id == id }
        agregarEncabezado(contenedor, "Hola, ${paciente?.nombre ?: ""}", "Paciente · ${fechaLarga(hoy)}")

        val misTurnos = AppData.TURNOS.filter { it.pacienteId == id }
        val proximo = misTurnos
            .filter { it.estado == EstadoTurno.RESERVADO && esFuturo(it) }
            .minWithOrNull(compareBy({ it.fecha }, { it.hora }))
        agregarTarjeta(
            contenedor,
            "Mi próximo turno",
            proximo?.let { listOf(FilaHome(descripcionTurnoPaciente(it), colorEstado(it.estado))) } ?: emptyList(),
            "No tenés turnos reservados.",
            "Ver turnos"
        ) { abrirModulo(turnos) }

        val activos = misTurnos.count { it.estado == EstadoTurno.RESERVADO && esFuturo(it) }
        agregarKpis(
            contenedor,
            listOf(
                "Turnos activos" to activos.toString(),
                "Estudios" to AppData.ESTUDIOS.count { it.pacienteId == id }.toString()
            )
        )

        agregarAccionPrincipal(contenedor, "Sacar turno") { abrirModulo(turnos) }
        agregarAccionesSecundarias(
            contenedor,
            listOf(
                "Mis estudios" to { abrirModulo(estudios) },
                "Mi perfil" to { startActivity(Intent(this, PerfilPacienteActivity::class.java)) }
            )
        )
    }

    private fun agregarEncabezado(contenedor: LinearLayout, titulo: String, subtitulo: String) {
        contenedor.addView(texto(titulo, R.dimen.text_display, R.color.seprise_ink, negrita = true))
        contenedor.addView(texto(subtitulo, R.dimen.text_subtitle, R.color.seprise_muted).apply {
            setPadding(0, dp(4), 0, dp(16))
        })
    }

    private fun agregarKpis(contenedor: LinearLayout, items: List<Pair<String, String>>) {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
        }
        items.forEach { (etiqueta, valor) ->
            val columna = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            columna.addView(texto(etiqueta, R.dimen.text_micro, R.color.seprise_muted).apply { maxLines = 2 })
            columna.addView(texto(valor, R.dimen.text_stat, R.color.seprise_ink, negrita = true))
            fila.addView(columna)
        }
        contenedor.addView(fila)
    }

    private fun agregarTarjeta(
        contenedor: LinearLayout,
        titulo: String,
        filas: List<FilaHome>,
        vacio: String,
        accionTexto: String? = null,
        accion: (() -> Unit)? = null
    ) {
        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
        }
        tarjeta.addView(texto(titulo, R.dimen.text_title, R.color.seprise_ink, negrita = true))
        if (filas.isEmpty()) {
            tarjeta.addView(texto(vacio, R.dimen.text_small, R.color.seprise_muted).apply {
                setPadding(0, dp(8), 0, 0)
            })
        } else {
            filas.forEach { fila ->
                tarjeta.addView(texto(fila.texto, R.dimen.text_small, fila.color ?: R.color.seprise_ink).apply {
                    setPadding(0, dp(8), 0, 0)
                })
            }
        }
        if (accionTexto != null && accion != null) {
            tarjeta.addView(texto(accionTexto, R.dimen.text_small, R.color.seprise_nav_text, negrita = true).apply {
                setPadding(0, dp(12), 0, 0)
                isClickable = true
                setOnClickListener { accion() }
            })
        }
        contenedor.addView(tarjeta)
    }

    private fun agregarAccionPrincipal(contenedor: LinearLayout, etiqueta: String, accion: () -> Unit) {
        contenedor.addView(Button(this).apply {
            text = etiqueta
            isAllCaps = false
            setTextColor(getColor(R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_btn))
            setBackgroundResource(R.drawable.bg_btn_primary)
            minHeight = dp(48)
            setOnClickListener { accion() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
        })
    }

    private fun agregarAccionesSecundarias(contenedor: LinearLayout, acciones: List<Pair<String, () -> Unit>>) {
        if (acciones.isEmpty()) return
        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
        }
        acciones.chunked(2).forEach { grupo ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    if (tarjeta.childCount > 0) topMargin = dp(8)
                }
            }
            grupo.forEachIndexed { indice, itemAccion ->
                val (etiqueta, accion) = itemAccion
                val params = if (grupo.size == 1) {
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                } else {
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (indice > 0) marginStart = dp(8)
                    }
                }
                fila.addView(texto(etiqueta, R.dimen.text_small, R.color.seprise_nav_text, negrita = true).apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(12), dp(8), dp(12))
                    setBackgroundResource(R.drawable.bg_nav_active)
                    isClickable = true
                    setOnClickListener { accion() }
                    layoutParams = params
                })
            }
            tarjeta.addView(fila)
        }
        contenedor.addView(tarjeta)
    }

    private fun texto(contenido: String, dimension: Int, color: Int, negrita: Boolean = false): TextView =
        TextView(this).apply {
            text = contenido
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(dimension))
            setTextColor(getColor(color))
            if (negrita) setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    private fun descripcionTurno(turno: Turno, incluirMedico: Boolean): String {
        val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
        val medico = AppData.MEDICOS.find { it.id == turno.medicoId }
        val partes = mutableListOf(fechaTurno(turno), turno.hora.toString(), paciente?.fullName ?: "Paciente")
        if (incluirMedico) partes.add("Dr/a. ${medico?.fullName ?: "Médico"}")
        partes.add(estadoTexto(turno.estado))
        return partes.joinToString(" · ")
    }

    private fun descripcionTurnoPaciente(turno: Turno): String {
        val agenda = AppData.AGENDAS.find { it.id == turno.agendaId }
        val medico = AppData.MEDICOS.find { it.id == turno.medicoId }
        val detalle = if (turno.medicoId == 0) {
            "Estudio: ${nombreServicio(agenda?.servicio?.name ?: "Estudio")}"
        } else {
            "Dr/a. ${medico?.fullName ?: "Médico"}"
        }
        return listOf(fechaTurno(turno), turno.hora.toString(), detalle, estadoTexto(turno.estado)).joinToString(" · ")
    }

    private fun fechaTurno(turno: Turno): String {
        val hoy = LocalDate.now()
        return when (turno.fecha) {
            hoy -> "Hoy"
            hoy.plusDays(1) -> "Mañana"
            else -> turno.fecha.format(DateTimeFormatter.ofPattern("dd/MM"))
        }
    }

    private fun esFuturo(turno: Turno): Boolean {
        val hoy = LocalDate.now()
        return turno.fecha.isAfter(hoy) || (turno.fecha == hoy && !turno.hora.isBefore(LocalTime.now()))
    }

    private fun estadoTexto(estado: EstadoTurno): String = when (estado) {
        EstadoTurno.RESERVADO -> "Reservado"
        EstadoTurno.CANCELADO -> "Cancelado"
        EstadoTurno.ATENDIDO -> "Atendido"
        EstadoTurno.EN_ESPERA -> "En espera"
    }

    private fun colorEstado(estado: EstadoTurno): Int = when (estado) {
        EstadoTurno.RESERVADO -> R.color.seprise_blue
        EstadoTurno.CANCELADO -> R.color.seprise_error
        EstadoTurno.ATENDIDO -> R.color.seprise_success
        EstadoTurno.EN_ESPERA -> R.color.seprise_warn
    }

    private fun fechaLarga(fecha: LocalDate): String =
        fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR")))
            .replaceFirstChar { it.uppercase() }

    private fun formatearEnum(valor: String): String =
        valor.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun nombreEspecialidad(valor: String): String = when (valor) {
        "CARDIOLOGIA" -> "Cardiología"
        "ORTOPEDIA" -> "Ortopedia"
        "PEDIATRIA" -> "Pediatría"
        else -> formatearEnum(valor)
    }

    private fun nombreServicio(valor: String): String = when (valor) {
        "ANALISIS_LABORATORIO" -> "Análisis de laboratorio"
        "RADIOGRAFIA" -> "Radiografía"
        "ECOGRAFIA" -> "Ecografía"
        "CLINICA_MEDICA" -> "Clínica médica"
        else -> formatearEnum(valor)
    }

    private fun obtenerNombrePersonaLogueada(): String {
        val id = SessionState.loggedPersonId
        val rol = SessionState.rol
        if (id != null && rol != null) {
            when (rol) {
                Rol.ADMINISTRATIVO -> {
                    val admin = AppData.ADMINISTRADORES.find { it.id == id }
                    if (admin != null) return "${admin.nombre} ${admin.apellido}"
                }
                Rol.MEDICO -> {
                    val medico = AppData.MEDICOS.find { it.id == id }
                    if (medico != null) return "${medico.nombre} ${medico.apellido}"
                }
                Rol.PACIENTE -> {
                    val paciente = AppData.PACIENTES.find { it.id == id }
                    if (paciente != null) return "${paciente.nombre} ${paciente.apellido}"
                }
            }
        }
        return SessionState.userId ?: ""
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
