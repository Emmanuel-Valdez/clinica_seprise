package com.seprise.clinica.shell

import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import com.seprise.clinica.R
import com.seprise.clinica.agenda.GestionAgendaActivity
import com.seprise.clinica.atencion.AtencionActivity
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.estudios.GestionEstudiosActivity
import com.seprise.clinica.honorarios.HonorariosActivity
import com.seprise.clinica.insumos.GestionInsumosActivity
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Turno
import com.seprise.clinica.pacientes.PerfilPacienteActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainNavActivity : BaseNavActivity() {

    override fun getContentLayoutId() = R.layout.contenido_home
    override fun getActiveNavItemKey() = "inicio"

    private data class FilaHome(val texto: String, val color: Int? = null)

    private var homeContenido: LinearLayout? = null

    override fun onContentViewCreated(view: View) {
        homeContenido = view.findViewById(R.id.home_contenido)
        SessionState.rol?.let { renderHome(it) }
    }

    override fun onResume() {
        super.onResume()
        SessionState.rol?.let { renderHome(it) }
    }

    private fun abrirModulo(clase: Class<*>) {
        startActivity(Intent(this, clase))
    }

    private fun renderHome(rol: Rol) {
        val contenedor = homeContenido ?: return
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
            "Sin turnos programados para hoy.",
            "Ver agendas"
        ) { abrirModulo(GestionAgendaActivity::class.java) }

        val ingresosEstimados = turnosHoy.size * 15000
        agregarKpis(
            contenedor,
            listOf(
                "Turnos hoy" to turnosHoy.size.toString(),
                "Pacientes hoy" to turnosHoy.map { it.pacienteId }.distinct().size.toString(),
                "Facturación aprox." to "$$ingresosEstimados"
            )
        )

        agregarAccionPrincipal(contenedor, "Cargar agenda médica") { abrirModulo(GestionAgendaActivity::class.java) }

        val acciones: List<Pair<String, () -> Unit>> = listOf(
            "Cargar agenda" to { abrirModulo(GestionAgendaActivity::class.java) },
            "Nuevo estudio" to { abrirModulo(GestionEstudiosActivity::class.java) },
            "Nuevo insumo" to { abrirModulo(GestionInsumosActivity::class.java) }
        )
        agregarAccionesSecundarias(contenedor, acciones)

        val estudiosRecientes = AppData.ESTUDIOS.sortedByDescending { it.fecha }.take(3).map { estudio ->
            val paciente = AppData.PACIENTES.find { it.id == estudio.pacienteId }
            FilaHome("${estudio.nombre} · ${paciente?.fullName ?: "Paciente"} · ${estudio.fecha.format(DateTimeFormatter.ofPattern("dd/MM"))}")
        }
        agregarTarjeta(contenedor, "Últimos estudios", estudiosRecientes, "Sin estudios registrados.", "Ver estudios") {
            abrirModulo(GestionEstudiosActivity::class.java)
        }

        val stockBajo = AppData.INSUMOS.filter { it.cantidad < 20 }.sortedBy { it.cantidad }.take(3).map { insumo ->
            FilaHome("${insumo.nombre} · stock ${insumo.cantidad}", R.color.seprise_warn)
        }
        agregarTarjeta(contenedor, "Stock bajo", stockBajo, "Sin insumos bajo stock.", "Ver insumos") {
            abrirModulo(GestionInsumosActivity::class.java)
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
        ) { abrirModulo(GestionAgendaActivity::class.java) }

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

        agregarAccionPrincipal(contenedor, "Registrar atención") { abrirModulo(AtencionActivity::class.java) }
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
        ) { abrirModulo(com.seprise.clinica.turnos.GestionTurnosActivity::class.java) }

        val activos = misTurnos.count { it.estado == EstadoTurno.RESERVADO && esFuturo(it) }
        agregarKpis(
            contenedor,
            listOf(
                "Turnos activos" to activos.toString(),
                "Estudios" to AppData.ESTUDIOS.count { it.pacienteId == id }.toString()
            )
        )

        agregarAccionPrincipal(contenedor, "Sacar turno") { abrirModulo(com.seprise.clinica.turnos.GestionTurnosActivity::class.java) }
        agregarAccionesSecundarias(
            contenedor,
            listOf(
                "Mis estudios" to { abrirModulo(GestionEstudiosActivity::class.java) },
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
        contenedor.addView(android.widget.Button(this).apply {
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
}
