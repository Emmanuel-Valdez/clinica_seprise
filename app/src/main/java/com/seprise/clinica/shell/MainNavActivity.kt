package com.seprise.clinica.shell

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seprise.clinica.R
import com.seprise.clinica.agenda.AgendaManager
import com.seprise.clinica.auth.LoginActivity
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.estudios.EstudiosManager
import com.seprise.clinica.insumos.InsumosManager
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Circuito
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Estudio
import com.seprise.clinica.models.Insumo
import com.seprise.clinica.models.Medico
import com.seprise.clinica.models.Modalidad
import com.seprise.clinica.models.Paciente
import com.seprise.clinica.models.Servicio
import com.seprise.clinica.models.Turno
import com.seprise.clinica.pacientes.PacientesManager
import com.seprise.clinica.pacientes.PerfilPacienteActivity
import com.seprise.clinica.turnos.TurnosManager
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

class MainNavActivity : AppCompatActivity() {

    private data class Modulo(val clave: String, @param:StringRes val nombre: Int, val clase: String)

    private val pacientes = Modulo("pacientes", R.string.cu01_nav_pacientes, "com.seprise.clinica.pacientes.PacientesActivity")
    private val agenda = Modulo("agenda", R.string.cu01_nav_agenda, "com.seprise.clinica.agenda.GestionAgendaActivity")
    private val turnos = Modulo("turnos", R.string.cu01_nav_turnos, "com.seprise.clinica.turnos.GestionTurnosActivity")
    private val atencion = Modulo("atencion", R.string.cu01_nav_atencion, "com.seprise.clinica.atencion.AtencionActivity")
    private val estudios = Modulo("estudios", R.string.cu01_nav_estudios, "com.seprise.clinica.estudios.GestionEstudiosActivity")
    private val insumos = Modulo("insumos", R.string.cu01_nav_insumos, "com.seprise.clinica.insumos.GestionInsumosActivity")
    private val honorarios = Modulo("honorarios", R.string.cu01_nav_honorarios, "com.seprise.clinica.honorarios.HonorariosActivity")

    private lateinit var contenedorItems: LinearLayout
    private lateinit var contenedorContenido: FrameLayout
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

        contenedorContenido = findViewById(R.id.cu01_contenido_container)
        contenedorItems = findViewById(R.id.cu01_nav_items)

        // Nombre de la persona logueada en la barra superior
        findViewById<TextView>(R.id.txt_nombre_usuario)?.text = obtenerNombrePersonaLogueada()

        // Logo superior redirige a Inicio
        findViewById<ImageView>(R.id.btn_home_logo).setOnClickListener {
            mostrarPantallaInicio()
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

    /** Regla de acceso por rol: agenda solo para Administrativos y Médicos; turnos solo para Pacientes. */
    private fun modulosDe(rol: Rol): List<Modulo> = when (rol) {
        Rol.ADMINISTRATIVO -> listOf(pacientes, agenda, estudios, insumos, honorarios)
        Rol.MEDICO -> listOf(atencion, agenda, estudios)
        Rol.PACIENTE -> listOf(turnos, estudios)
    }

    private fun armarItems(modulos: List<Modulo>) {
        contenedorItems.removeAllViews()
        val pad = dp(12)

        // Item "Inicio"
        val itemInicio = TextView(this).apply {
            text = "Inicio"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_nav))
            setPadding(pad, pad, pad, pad)
            setTextColor(getColor(R.color.seprise_muted))
            isClickable = true
            setOnClickListener { seleccionarInicio(this) }
        }
        contenedorItems.addView(itemInicio)
        seleccionarInicio(itemInicio)

        modulos.forEach { modulo ->
            val item = TextView(this).apply {
                setText(modulo.nombre)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_nav))
                setPadding(pad, pad, pad, pad)
                setTextColor(getColor(R.color.seprise_muted))
                isClickable = true
                setOnClickListener { seleccionarModulo(modulo, this) }
            }
            contenedorItems.addView(item)
        }
    }

    private fun seleccionarInicio(item: TextView) {
        activo?.apply {
            setBackgroundResource(0)
            setTextColor(getColor(R.color.seprise_muted))
        }
        item.setBackgroundResource(R.drawable.bg_nav_active)
        item.setTextColor(getColor(R.color.seprise_nav_text))
        activo = item

        mostrarPantallaInicio()
    }

    private fun seleccionarModulo(modulo: Modulo, item: TextView) {
        activo?.apply {
            setBackgroundResource(0)
            setTextColor(getColor(R.color.seprise_muted))
        }
        item.setBackgroundResource(R.drawable.bg_nav_active)
        item.setTextColor(getColor(R.color.seprise_nav_text))
        activo = item

        cargarVistaModulo(modulo)
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

    /** Muestra la pantalla de inicio con la imagen de fondo. */
    private fun mostrarPantallaInicio() {
        contenedorContenido.removeAllViews()
        val homeView = LayoutInflater.from(this).inflate(R.layout.activity_main_nav, containerView(), false)
        val homeLayout = homeView.findViewById<View>(R.id.layout_inicio_home)
        if (homeLayout != null) {
            (homeLayout.parent as? FrameLayout)?.removeView(homeLayout)
            contenedorContenido.addView(homeLayout)
        }
    }

    private fun containerView(): FrameLayout = contenedorContenido

    /** Carga e inicializa la vista del módulo seleccionado manteniendo visible la barra de navegación. */
    private fun cargarVistaModulo(modulo: Modulo) {
        contenedorContenido.removeAllViews()

        when (modulo.clave) {
            "pacientes" -> {
                val v = LayoutInflater.from(this).inflate(R.layout.activity_gestion_pacientes, contenedorContenido, false)
                inicializarPacientesView(v)
                contenedorContenido.addView(v)
            }
            "agenda" -> {
                val v = LayoutInflater.from(this).inflate(R.layout.activity_gestion_agenda, contenedorContenido, false)
                inicializarAgendaView(v)
                contenedorContenido.addView(v)
            }
            "turnos" -> {
                val v = LayoutInflater.from(this).inflate(R.layout.activity_gestion_turnos, contenedorContenido, false)
                inicializarTurnosView(v)
                contenedorContenido.addView(v)
            }
            "estudios" -> {
                val v = LayoutInflater.from(this).inflate(R.layout.activity_gestion_estudios, contenedorContenido, false)
                inicializarEstudiosView(v)
                contenedorContenido.addView(v)
            }
            "insumos" -> {
                val v = LayoutInflater.from(this).inflate(R.layout.activity_gestion_insumos, contenedorContenido, false)
                inicializarInsumosView(v)
                contenedorContenido.addView(v)
            }
            else -> {
                val intent = Intent().setClassName(packageName, modulo.clase)
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.cu01_proximamente, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ==========================================
    // VISTA Y LÓGICA DE AGENDAS EN EL SHELL
    // ==========================================
    private fun inicializarAgendaView(v: View) {
        val containerMedicos = v.findViewById<LinearLayout>(R.id.container_medicos)
        val etBuscar = v.findViewById<EditText>(R.id.et_buscar_medico)
        val rgCircuito = v.findViewById<RadioGroup>(R.id.rg_circuito_agenda)
        val btnCargarEstudios = v.findViewById<Button>(R.id.btn_cargar_agenda_estudios)

        fun cargarLista(filtro: String = "") {
            containerMedicos.removeAllViews()

            val esEstudios = rgCircuito.checkedRadioButtonId == R.id.rb_agenda_estudios

            if (esEstudios && SessionState.rol != Rol.MEDICO) {
                btnCargarEstudios.visibility = View.VISIBLE
            } else {
                btnCargarEstudios.visibility = View.GONE
            }

            if (esEstudios) {
                val agendasEstudios = AppData.AGENDAS.filter { it.circuito == Circuito.ESTUDIOS_CLINICOS }
                val filtroNorm = normalizarTexto(filtro)
                val filtradas = if (filtroNorm.isBlank()) agendasEstudios else agendasEstudios.filter { normalizarTexto(it.servicio.name).contains(filtroNorm) }

                if (filtradas.isEmpty()) {
                    mostrarMensajeVacio(containerMedicos, "No hay agendas de estudios clínicos configuradas")
                    return
                }

                for (agenda in filtradas) {
                    val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)
                    item.findViewById<TextView>(R.id.tv_nombre_medico).text = "Estudio: ${agenda.servicio.name}"
                    item.findViewById<TextView>(R.id.tv_especialidad_medico).text = "Días: ${agenda.diasSemana.joinToString { it.name }} (${agenda.horaInicio} - ${agenda.horaFin})"
                    val tvEst = item.findViewById<TextView>(R.id.tv_estado_agenda)
                    tvEst.text = if (agenda.requiereAyuno) "Requiere ayuno previo" else "Sin requerimiento de ayuno"
                    tvEst.setTextColor(ContextCompat.getColor(this, if (agenda.requiereAyuno) R.color.seprise_warn else R.color.seprise_blue))

                    val btn = item.findViewById<Button>(R.id.btn_accion_agenda)
                    btn.text = "Ver Agenda"
                    btn.setOnClickListener { mostrarDetalleAgendaEstudioDialog(agenda) { cargarLista(etBuscar.text.toString()) } }
                    containerMedicos.addView(item)
                }
            } else {
                val medicosBase = if (SessionState.rol == Rol.MEDICO && SessionState.loggedPersonId != null) {
                    AppData.MEDICOS.filter { it.id == SessionState.loggedPersonId }
                } else AppData.MEDICOS

                val filtroNorm = normalizarTexto(filtro)
                val filtrados = if (filtroNorm.isBlank()) medicosBase else medicosBase.filter { normalizarTexto(it.fullName).contains(filtroNorm) }

                if (filtrados.isEmpty()) {
                    mostrarMensajeVacio(containerMedicos, "No se encontraron médicos en consultorio externo")
                    return
                }

                for (medico in filtrados) {
                    val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)
                    item.findViewById<TextView>(R.id.tv_nombre_medico).text = "Dr/a. ${medico.fullName}"
                    item.findViewById<TextView>(R.id.tv_especialidad_medico).text = "Especialidad: ${medico.especialidad.name}"

                    val agendaExistente = AgendaManager.obtenerAgendaPorMedico(medico.id)
                    val tvEst = item.findViewById<TextView>(R.id.tv_estado_agenda)
                    val btn = item.findViewById<Button>(R.id.btn_accion_agenda)

                    if (agendaExistente != null && agendaExistente.circuito == Circuito.CONSULTORIOS_EXTERNOS) {
                        tvEst.text = "Agenda Configurada"
                        tvEst.setTextColor(ContextCompat.getColor(this, R.color.seprise_success))
                        btn.text = "Ver Agenda"
                        btn.setOnClickListener { mostrarDetalleAgendaDialog(medico, agendaExistente) { cargarLista(etBuscar.text.toString()) } }
                    } else {
                        tvEst.text = "Sin Agenda"
                        tvEst.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
                        btn.text = "Cargar Agenda"
                        btn.setOnClickListener { abrirModalCargarAgenda(medico) { cargarLista(etBuscar.text.toString()) } }
                    }
                    containerMedicos.addView(item)
                }
            }
        }

        rgCircuito.setOnCheckedChangeListener { _, _ -> cargarLista(etBuscar.text.toString()) }
        btnCargarEstudios.setOnClickListener { abrirModalCargarAgendaEstudios { cargarLista(etBuscar.text.toString()) } }
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { cargarLista(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarLista()
    }

    // ==========================================
    // VISTA Y LÓGICA DE TURNOS EN EL SHELL
    // ==========================================
    private fun inicializarTurnosView(v: View) {
        val containerMedicos = v.findViewById<LinearLayout>(R.id.container_medicos)
        val etBuscar = v.findViewById<EditText>(R.id.et_buscar_medico)
        val rgCircuito = v.findViewById<RadioGroup>(R.id.rg_circuito_turnos)

        fun cargarLista(filtro: String = "") {
            containerMedicos.removeAllViews()
            val esEstudios = rgCircuito.checkedRadioButtonId == R.id.rb_turnos_estudios
            val filtroNorm = normalizarTexto(filtro)

            if (esEstudios) {
                val agendasEstudios = AppData.AGENDAS.filter { it.circuito == Circuito.ESTUDIOS_CLINICOS }
                val filtradas = if (filtroNorm.isBlank()) agendasEstudios else agendasEstudios.filter { normalizarTexto(it.servicio.name).contains(filtroNorm) }

                if (filtradas.isEmpty()) {
                    mostrarMensajeVacio(containerMedicos, "No hay estudios clínicos configurados actualmente")
                    return
                }

                for (agenda in filtradas) {
                    val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)
                    item.findViewById<TextView>(R.id.tv_nombre_medico).text = "Estudio: ${agenda.servicio.name}"
                    item.findViewById<TextView>(R.id.tv_especialidad_medico).text = "Días: ${agenda.diasSemana.joinToString { it.name }} (${agenda.horaInicio} - ${agenda.horaFin})"
                    val tvEst = item.findViewById<TextView>(R.id.tv_estado_agenda)
                    tvEst.text = if (agenda.requiereAyuno) "Requiere ayuno previo" else "Sin requerimiento de ayuno"
                    tvEst.setTextColor(ContextCompat.getColor(this, if (agenda.requiereAyuno) R.color.seprise_warn else R.color.seprise_blue))

                    val btn = item.findViewById<Button>(R.id.btn_accion_agenda)
                    btn.text = "Sacar Turno"
                    btn.isEnabled = true
                    btn.setOnClickListener { abrirModalSacarTurnoEstudio(agenda) { cargarLista(etBuscar.text.toString()) } }
                    containerMedicos.addView(item)
                }
            } else {
                val medicosBase = if (SessionState.rol == Rol.MEDICO && SessionState.loggedPersonId != null) {
                    AppData.MEDICOS.filter { it.id == SessionState.loggedPersonId }
                } else AppData.MEDICOS

                val filtrados = if (filtroNorm.isBlank()) medicosBase else medicosBase.filter { normalizarTexto(it.fullName).contains(filtroNorm) || normalizarTexto(it.especialidad.name).contains(filtroNorm) }

                if (filtrados.isEmpty()) {
                    mostrarMensajeVacio(containerMedicos, "No se encontraron médicos en consultorio externo")
                    return
                }

                for (medico in filtrados) {
                    val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)
                    item.findViewById<TextView>(R.id.tv_nombre_medico).text = "Dr/a. ${medico.fullName}"
                    item.findViewById<TextView>(R.id.tv_especialidad_medico).text = "Especialidad: ${medico.especialidad.name}"

                    val agendaExistente = TurnosManager.obtenerAgendaPorMedico(medico.id)
                    val turnosReservados = TurnosManager.obtenerTurnosPorMedico(medico.id)
                    val tvEst = item.findViewById<TextView>(R.id.tv_estado_agenda)
                    val btn = item.findViewById<Button>(R.id.btn_accion_agenda)

                    if (agendaExistente != null && agendaExistente.circuito == Circuito.CONSULTORIOS_EXTERNOS) {
                        tvEst.text = "Agenda disponible (${turnosReservados.size} turnos)"
                        tvEst.setTextColor(ContextCompat.getColor(this, R.color.seprise_success))
                        btn.text = "Sacar Turno"
                        btn.isEnabled = true
                        btn.setOnClickListener { abrirModalSacarTurnoMedico(medico, agendaExistente) { cargarLista(etBuscar.text.toString()) } }
                    } else {
                        tvEst.text = "Sin agenda configurada"
                        tvEst.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
                        btn.text = "Sin Agenda"
                        btn.isEnabled = false
                    }
                    containerMedicos.addView(item)
                }
            }
        }

        rgCircuito.setOnCheckedChangeListener { _, _ -> cargarLista(etBuscar.text.toString()) }
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { cargarLista(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarLista()
    }

    // ==========================================
    // VISTA Y LÓGICA DE ESTUDIOS EN EL SHELL
    // ==========================================
    private fun inicializarEstudiosView(v: View) {
        val containerEstudios = v.findViewById<LinearLayout>(R.id.container_estudios)
        val etBuscar = v.findViewById<EditText>(R.id.et_buscar_estudio)
        val btnCargar = v.findViewById<Button>(R.id.btn_cargar_estudio)

        fun cargarLista(filtro: String = "") {
            containerEstudios.removeAllViews()

            val estudiosBase = if (SessionState.rol == Rol.PACIENTE && SessionState.loggedPersonId != null) {
                AppData.ESTUDIOS.filter { it.pacienteId == SessionState.loggedPersonId }
            } else AppData.ESTUDIOS

            val filtroNorm = normalizarTexto(filtro)
            val filtrados = if (filtroNorm.isBlank()) estudiosBase else estudiosBase.filter { estudio ->
                val paciente = AppData.PACIENTES.find { it.id == estudio.pacienteId }
                val nombrePac = paciente?.fullName ?: ""
                normalizarTexto(estudio.nombre).contains(filtroNorm) || normalizarTexto(nombrePac).contains(filtroNorm) || normalizarTexto(estudio.observaciones).contains(filtroNorm)
            }

            if (filtrados.isEmpty()) {
                mostrarMensajeVacio(containerEstudios, "No hay estudios clínicos registrados")
                return
            }

            for (estudio in filtrados) {
                val item = LayoutInflater.from(this).inflate(R.layout.item_estudio, containerEstudios, false)
                val paciente = AppData.PACIENTES.find { it.id == estudio.pacienteId }

                item.findViewById<TextView>(R.id.tv_nombre_estudio).text = "Estudio: ${estudio.nombre}"
                item.findViewById<TextView>(R.id.tv_paciente_estudio).text = "Paciente: ${paciente?.fullName ?: "ID ${estudio.pacienteId}"}"
                item.findViewById<TextView>(R.id.tv_fecha_estudio).text = "Fecha: ${estudio.fecha} ${estudio.hora}"
                item.findViewById<TextView>(R.id.tv_info_estudio).text = if (estudio.observaciones.isNotEmpty()) "Observaciones: ${estudio.observaciones}" else "Sin observaciones"

                item.setOnClickListener { mostrarDetalleEstudioDialog(estudio) { cargarLista(etBuscar.text.toString()) } }
                containerEstudios.addView(item)
            }
        }

        if (SessionState.rol == Rol.ADMINISTRATIVO) {
            btnCargar.visibility = View.VISIBLE
            btnCargar.setOnClickListener { abrirModalCargarEstudio { cargarLista(etBuscar.text.toString()) } }
        } else {
            btnCargar.visibility = View.GONE
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { cargarLista(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarLista()
    }

    // ==========================================
    // VISTA Y LÓGICA DE INSUMOS EN EL SHELL
    // ==========================================
    private fun inicializarInsumosView(v: View) {
        val containerInsumos = v.findViewById<LinearLayout>(R.id.container_insumos)
        val etBuscar = v.findViewById<EditText>(R.id.et_buscar_insumo)
        val btnAgregar = v.findViewById<Button>(R.id.btn_agregar_insumo)

        fun cargarLista(filtro: String = "") {
            containerInsumos.removeAllViews()
            val filtroNorm = normalizarTexto(filtro)
            val filtrados = if (filtroNorm.isBlank()) AppData.INSUMOS else AppData.INSUMOS.filter { normalizarTexto(it.nombre).contains(filtroNorm) }

            if (filtrados.isEmpty()) {
                mostrarMensajeVacio(containerInsumos, "No se encontraron insumos en el inventario")
                return
            }

            for (insumo in filtrados) {
                val item = LayoutInflater.from(this).inflate(R.layout.item_insumo, containerInsumos, false)
                item.findViewById<TextView>(R.id.tv_nombre_insumo).text = insumo.nombre
                item.findViewById<TextView>(R.id.tv_cantidad_insumo).text = "Stock: ${insumo.cantidad}"
                item.setOnClickListener { abrirModalEditarInsumo(insumo) { cargarLista(etBuscar.text.toString()) } }
                containerInsumos.addView(item)
            }
        }

        btnAgregar.setOnClickListener { abrirModalAgregarInsumo { cargarLista(etBuscar.text.toString()) } }
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { cargarLista(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarLista()
    }

    // ==========================================
    // VISTA Y LÓGICA DE PACIENTES EN EL SHELL
    // ==========================================
    private fun inicializarPacientesView(v: View) {
        val containerPacientes = v.findViewById<LinearLayout>(R.id.container_pacientes)
        val etBuscar = v.findViewById<EditText>(R.id.et_buscar_paciente)
        val btnAgregar = v.findViewById<Button>(R.id.btn_agregar_paciente)

        fun cargarLista(filtro: String = "") {
            containerPacientes.removeAllViews()
            val filtroNorm = normalizarTexto(filtro)
            val filtrados = if (filtroNorm.isBlank()) {
                AppData.PACIENTES
            } else {
                AppData.PACIENTES.filter {
                    normalizarTexto(it.fullName).contains(filtroNorm) ||
                    normalizarTexto(it.dni).contains(filtroNorm)
                }
            }

            if (filtrados.isEmpty()) {
                mostrarMensajeVacio(containerPacientes, "No se encontraron pacientes registrados")
                return
            }

            for (paciente in filtrados) {
                val item = LayoutInflater.from(this).inflate(R.layout.item_paciente, containerPacientes, false)

                val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_paciente)
                val tvDni = item.findViewById<TextView>(R.id.tv_dni_paciente)
                val tvContacto = item.findViewById<TextView>(R.id.tv_contacto_paciente)
                val tvEstado = item.findViewById<TextView>(R.id.tv_estado_paciente)

                tvNombre.text = paciente.fullName
                tvDni.text = "DNI: ${paciente.dni}"
                tvContacto.text = "Mail: ${paciente.mail} | Tel: ${paciente.telefono}"

                if (paciente.activo) {
                    tvEstado.text = "Activo"
                    tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_success))
                } else {
                    tvEstado.text = "Inhabilitado"
                    tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_error))
                }

                item.setOnClickListener { abrirModalEditarPaciente(paciente) { cargarLista(etBuscar.text.toString()) } }
                containerPacientes.addView(item)
            }
        }

        btnAgregar.setOnClickListener { abrirModalAgregarPaciente { cargarLista(etBuscar.text.toString()) } }
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { cargarLista(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarLista()
    }

    private fun abrirModalAgregarPaciente(onUpdate: () -> Unit) {
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
                onUpdate()
            } else {
                Toast.makeText(this, "El paciente ya se encuentra registrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirModalEditarPaciente(paciente: Paciente, onUpdate: () -> Unit) {
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
                onUpdate()
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
                    onUpdate()
                } else {
                    Toast.makeText(this, "Nombre, Apellido y DNI son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    // ==========================================
    // MÉTODOS Y DIÁLOGOS AUXILIARES
    // ==========================================
    private fun mostrarMensajeVacio(container: LinearLayout, mensaje: String) {
        val tvVacio = TextView(this).apply {
            text = mensaje
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainNavActivity, R.color.seprise_muted))
            setPadding(0, 24, 0, 0)
        }
        container.addView(tvVacio)
    }

    private fun abrirModalCargarAgenda(medico: Medico, onUpdate: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_agenda, null)
        dialogView.findViewById<TextView>(R.id.tv_dialog_medico_nombre).text = "Médico: Dr/a. ${medico.fullName} (${medico.especialidad.name})"
        dialogView.findViewById<View>(R.id.layout_servicio).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_circuito).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_modalidad).visibility = View.GONE

        val cbLunes = dialogView.findViewById<CheckBox>(R.id.cb_lunes)
        val cbMartes = dialogView.findViewById<CheckBox>(R.id.cb_martes)
        val cbMiercoles = dialogView.findViewById<CheckBox>(R.id.cb_miercoles)
        val cbJueves = dialogView.findViewById<CheckBox>(R.id.cb_jueves)
        val cbViernes = dialogView.findViewById<CheckBox>(R.id.cb_viernes)
        val cbSabado = dialogView.findViewById<CheckBox>(R.id.cb_sabado)
        val cbDomingo = dialogView.findViewById<CheckBox>(R.id.cb_domingo)

        val etHoraInicio = dialogView.findViewById<EditText>(R.id.et_hora_inicio)
        val etHoraFin = dialogView.findViewById<EditText>(R.id.et_hora_fin)
        val etDuracionCita = dialogView.findViewById<EditText>(R.id.et_duracion_cita)
        val cbAyuno = dialogView.findViewById<CheckBox>(R.id.cb_requiere_ayuno)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar Agenda", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val diasSeleccionados = mutableListOf<DayOfWeek>()
            if (cbLunes.isChecked) diasSeleccionados.add(DayOfWeek.MONDAY)
            if (cbMartes.isChecked) diasSeleccionados.add(DayOfWeek.TUESDAY)
            if (cbMiercoles.isChecked) diasSeleccionados.add(DayOfWeek.WEDNESDAY)
            if (cbJueves.isChecked) diasSeleccionados.add(DayOfWeek.THURSDAY)
            if (cbViernes.isChecked) diasSeleccionados.add(DayOfWeek.FRIDAY)
            if (cbSabado.isChecked) diasSeleccionados.add(DayOfWeek.SATURDAY)
            if (cbDomingo.isChecked) diasSeleccionados.add(DayOfWeek.SUNDAY)

            if (diasSeleccionados.isEmpty()) {
                Toast.makeText(this, "Debe seleccionar al menos un día de atención", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val horaInicio = parsearHora(etHoraInicio.text.toString().trim(), LocalTime.of(8, 0))
            val horaFin = parsearHora(etHoraFin.text.toString().trim(), LocalTime.of(16, 0))
            val duracionMins = etDuracionCita.text.toString().toIntOrNull() ?: 20

            val servicioDelMedico = try { Servicio.valueOf(medico.especialidad.name) } catch (e: Exception) { Servicio.CLINICA_MEDICA }

            val nuevaAgenda = Agenda(
                id = (AppData.AGENDAS.maxOfOrNull { it.id } ?: 0) + 1,
                circuito = Circuito.CONSULTORIOS_EXTERNOS,
                servicio = servicioDelMedico,
                medicoId = medico.id,
                diasSemana = diasSeleccionados,
                horaInicio = horaInicio,
                horaFin = horaFin,
                duracionCita = duracionMins,
                requiereAyuno = cbAyuno.isChecked,
                modalidad = Modalidad.CON_TURNO
            )

            if (AgendaManager.crearAgenda(nuevaAgenda)) {
                Toast.makeText(this, "Agenda guardada con éxito para Dr/a. ${medico.apellido}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onUpdate()
            } else {
                Toast.makeText(this, "Error: El médico ya tiene una agenda cargada", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun abrirModalCargarAgendaEstudios(onUpdate: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_agenda, null)
        dialogView.findViewById<TextView>(R.id.tv_dialog_titulo).text = "Cargar Agenda - Análisis Clínicos / Estudios"
        dialogView.findViewById<TextView>(R.id.tv_dialog_medico_nombre).text = "Configuración del servicio de estudio"

        dialogView.findViewById<View>(R.id.layout_servicio).visibility = View.VISIBLE
        dialogView.findViewById<View>(R.id.layout_circuito).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_modalidad).visibility = View.GONE

        val spinnerServicio = dialogView.findViewById<Spinner>(R.id.spinner_servicio)
        val listaEstudios = listOf(Servicio.ANALISIS_LABORATORIO.name, Servicio.RADIOGRAFIA.name, Servicio.ECOGRAFIA.name)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaEstudios)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerServicio.adapter = adapter

        val cbLunes = dialogView.findViewById<CheckBox>(R.id.cb_lunes)
        val cbMartes = dialogView.findViewById<CheckBox>(R.id.cb_martes)
        val cbMiercoles = dialogView.findViewById<CheckBox>(R.id.cb_miercoles)
        val cbJueves = dialogView.findViewById<CheckBox>(R.id.cb_jueves)
        val cbViernes = dialogView.findViewById<CheckBox>(R.id.cb_viernes)
        val cbSabado = dialogView.findViewById<CheckBox>(R.id.cb_sabado)
        val cbDomingo = dialogView.findViewById<CheckBox>(R.id.cb_domingo)

        val etHoraInicio = dialogView.findViewById<EditText>(R.id.et_hora_inicio)
        val etHoraFin = dialogView.findViewById<EditText>(R.id.et_hora_fin)
        val etDuracionCita = dialogView.findViewById<EditText>(R.id.et_duracion_cita)
        val cbAyuno = dialogView.findViewById<CheckBox>(R.id.cb_requiere_ayuno)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar Agenda", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val diasSeleccionados = mutableListOf<DayOfWeek>()
            if (cbLunes.isChecked) diasSeleccionados.add(DayOfWeek.MONDAY)
            if (cbMartes.isChecked) diasSeleccionados.add(DayOfWeek.TUESDAY)
            if (cbMiercoles.isChecked) diasSeleccionados.add(DayOfWeek.WEDNESDAY)
            if (cbJueves.isChecked) diasSeleccionados.add(DayOfWeek.THURSDAY)
            if (cbViernes.isChecked) diasSeleccionados.add(DayOfWeek.FRIDAY)
            if (cbSabado.isChecked) diasSeleccionados.add(DayOfWeek.SATURDAY)
            if (cbDomingo.isChecked) diasSeleccionados.add(DayOfWeek.SUNDAY)

            if (diasSeleccionados.isEmpty()) {
                Toast.makeText(this, "Debe seleccionar al menos un día de atención", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val horaInicio = parsearHora(etHoraInicio.text.toString().trim(), LocalTime.of(8, 0))
            val horaFin = parsearHora(etHoraFin.text.toString().trim(), LocalTime.of(16, 0))
            val duracionMins = etDuracionCita.text.toString().toIntOrNull() ?: 15

            val servicioSeleccionado = Servicio.valueOf(spinnerServicio.selectedItem.toString())

            val nuevaAgenda = Agenda(
                id = (AppData.AGENDAS.maxOfOrNull { it.id } ?: 0) + 1,
                circuito = Circuito.ESTUDIOS_CLINICOS,
                servicio = servicioSeleccionado,
                medicoId = 0,
                diasSemana = diasSeleccionados,
                horaInicio = horaInicio,
                horaFin = horaFin,
                duracionCita = duracionMins,
                requiereAyuno = cbAyuno.isChecked,
                modalidad = Modalidad.CON_TURNO
            )

            AppData.AGENDAS.add(nuevaAgenda)
            Toast.makeText(this, "Agenda de análisis clínico/estudio cargada con éxito", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onUpdate()
        }
    }

    private fun mostrarDetalleAgendaDialog(medico: Medico, agenda: Agenda, onUpdate: () -> Unit) {
        val diasStr = agenda.diasSemana.joinToString(", ") { it.name }
        val slots = mutableListOf<LocalTime>()
        var actual = agenda.horaInicio
        val fin = agenda.horaFin
        val duracion = if (agenda.duracionCita > 0) agenda.duracionCita.toLong() else 20L

        while (actual.plusMinutes(duracion) <= fin) {
            slots.add(actual)
            actual = actual.plusMinutes(duracion)
        }

        val grillaHorariosStr = slots.joinToString("\n") { hora ->
            val turno = AppData.TURNOS.find { it.medicoId == medico.id && it.hora == hora && it.estado == EstadoTurno.RESERVADO }
            if (turno != null) {
                val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
                val nombrePaciente = paciente?.fullName ?: "Paciente ID ${turno.pacienteId}"
                "• $hora - Reservado (Paciente: $nombrePaciente)"
            } else {
                "• $hora - Disponible"
            }
        }

        val mensaje = "Médico: Dr/a. ${medico.fullName}\nServicio: ${agenda.servicio.name} | Circuito: ${agenda.circuito.name}\nDías de atención: $diasStr\n\n--- Horarios de Atención ---\n$grillaHorariosStr"

        val builder = AlertDialog.Builder(this)
            .setTitle("Agenda de Dr/a. ${medico.fullName}")
            .setMessage(mensaje)

        if (SessionState.rol != Rol.MEDICO) {
            builder.setPositiveButton("Modificar Agenda") { _, _ -> abrirModalModificarAgenda(medico, agenda, onUpdate) }
            builder.setNeutralButton("Eliminar Agenda") { _, _ ->
                AgendaManager.eliminarAgenda(agenda.id)
                Toast.makeText(this, "Agenda eliminada", Toast.LENGTH_SHORT).show()
                onUpdate()
            }
            builder.setNegativeButton("Cerrar", null)
        } else {
            builder.setPositiveButton("Cerrar", null)
        }

        builder.show()
    }

    private fun mostrarDetalleAgendaEstudioDialog(agenda: Agenda, onUpdate: () -> Unit) {
        val diasStr = agenda.diasSemana.joinToString(", ") { it.name }
        val slots = mutableListOf<LocalTime>()
        var actual = agenda.horaInicio
        val fin = agenda.horaFin
        val duracion = if (agenda.duracionCita > 0) agenda.duracionCita.toLong() else 15L

        while (actual.plusMinutes(duracion) <= fin) {
            slots.add(actual)
            actual = actual.plusMinutes(duracion)
        }

        val grillaHorariosStr = slots.joinToString("\n") { hora ->
            val turno = AppData.TURNOS.find { it.agendaId == agenda.id && it.hora == hora && it.estado == EstadoTurno.RESERVADO }
            if (turno != null) {
                val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
                val nombrePaciente = paciente?.fullName ?: "Paciente ID ${turno.pacienteId}"
                "• $hora - Reservado (Paciente: $nombrePaciente)"
            } else {
                "• $hora - Disponible"
            }
        }

        val mensaje = "Estudio: ${agenda.servicio.name}\nCircuito: Estudios Clínicos\nDías de atención: $diasStr\nRequiere ayuno: ${if (agenda.requiereAyuno) "Sí" else "No"}\n\n--- Horarios de Atención ---\n$grillaHorariosStr"

        val builder = AlertDialog.Builder(this)
            .setTitle("Agenda de ${agenda.servicio.name}")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)

        if (SessionState.rol != Rol.MEDICO) {
            builder.setNeutralButton("Eliminar Agenda") { _, _ ->
                AppData.AGENDAS.removeAll { it.id == agenda.id }
                Toast.makeText(this, "Agenda de estudio eliminada", Toast.LENGTH_SHORT).show()
                onUpdate()
            }
        }

        builder.show()
    }

    private fun abrirModalModificarAgenda(medico: Medico, agenda: Agenda, onUpdate: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_agenda, null)
        dialogView.findViewById<TextView>(R.id.tv_dialog_titulo).text = "Modificar Agenda"
        dialogView.findViewById<TextView>(R.id.tv_dialog_medico_nombre).text = "Médico: Dr/a. ${medico.fullName}"

        dialogView.findViewById<View>(R.id.layout_servicio).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_circuito).visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_modalidad).visibility = View.GONE

        val cbLunes = dialogView.findViewById<CheckBox>(R.id.cb_lunes).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.MONDAY) }
        val cbMartes = dialogView.findViewById<CheckBox>(R.id.cb_martes).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.TUESDAY) }
        val cbMiercoles = dialogView.findViewById<CheckBox>(R.id.cb_miercoles).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.WEDNESDAY) }
        val cbJueves = dialogView.findViewById<CheckBox>(R.id.cb_jueves).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.THURSDAY) }
        val cbViernes = dialogView.findViewById<CheckBox>(R.id.cb_viernes).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.FRIDAY) }
        val cbSabado = dialogView.findViewById<CheckBox>(R.id.cb_sabado).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.SATURDAY) }
        val cbDomingo = dialogView.findViewById<CheckBox>(R.id.cb_domingo).apply { isChecked = agenda.diasSemana.contains(DayOfWeek.SUNDAY) }

        val etHoraInicio = dialogView.findViewById<EditText>(R.id.et_hora_inicio).apply { setText(agenda.horaInicio.toString()) }
        val etHoraFin = dialogView.findViewById<EditText>(R.id.et_hora_fin).apply { setText(agenda.horaFin.toString()) }
        val etDuracionCita = dialogView.findViewById<EditText>(R.id.et_duracion_cita).apply { setText(agenda.duracionCita.toString()) }
        val cbAyuno = dialogView.findViewById<CheckBox>(R.id.cb_requiere_ayuno).apply { isChecked = agenda.requiereAyuno }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar Cambios", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val diasSeleccionados = mutableListOf<DayOfWeek>()
            if (cbLunes.isChecked) diasSeleccionados.add(DayOfWeek.MONDAY)
            if (cbMartes.isChecked) diasSeleccionados.add(DayOfWeek.TUESDAY)
            if (cbMiercoles.isChecked) diasSeleccionados.add(DayOfWeek.WEDNESDAY)
            if (cbJueves.isChecked) diasSeleccionados.add(DayOfWeek.THURSDAY)
            if (cbViernes.isChecked) diasSeleccionados.add(DayOfWeek.FRIDAY)
            if (cbSabado.isChecked) diasSeleccionados.add(DayOfWeek.SATURDAY)
            if (cbDomingo.isChecked) diasSeleccionados.add(DayOfWeek.SUNDAY)

            if (diasSeleccionados.isEmpty()) {
                Toast.makeText(this, "Debe seleccionar al menos un día de atención", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val horaInicio = parsearHora(etHoraInicio.text.toString().trim(), agenda.horaInicio)
            val horaFin = parsearHora(etHoraFin.text.toString().trim(), agenda.horaFin)
            val duracionMins = etDuracionCita.text.toString().toIntOrNull() ?: agenda.duracionCita

            val agendaModificada = agenda.copy(
                diasSemana = diasSeleccionados,
                horaInicio = horaInicio,
                horaFin = horaFin,
                duracionCita = duracionMins,
                requiereAyuno = cbAyuno.isChecked
            )

            val cancelados = AgendaManager.modificarAgendaYCancelarTurnos(agendaModificada)
            if (cancelados >= 0) {
                if (cancelados > 0) {
                    Toast.makeText(this, "Agenda modificada. Se cancelaron $cancelados turnos reservados.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Agenda modificada con éxito", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                onUpdate()
            } else {
                Toast.makeText(this, "Error al modificar la agenda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirModalSacarTurnoMedico(medico: Medico, agenda: Agenda, onUpdate: () -> Unit) {
        if (AppData.PACIENTES.isEmpty()) {
            Toast.makeText(this, "No hay pacientes registrados en el sistema", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sacar_turno, null)
        val diasAtencionStr = agenda.diasSemana.joinToString(", ") { it.name }
        dialogView.findViewById<TextView>(R.id.tv_dialog_turno_info).text = "Médico: Dr/a. ${medico.fullName}\nServicio: ${agenda.servicio.name}\nDías de atención: $diasAtencionStr\nHorario: ${agenda.horaInicio} a ${agenda.horaFin} (${agenda.duracionCita} min/cita)"

        val spinnerPaciente = dialogView.findViewById<Spinner>(R.id.spinner_paciente)
        val listaPacientesStr = AppData.PACIENTES.map { "ID ${it.id}: ${it.fullName} (DNI ${it.dni})" }
        val adapterPaciente = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaPacientesStr)
        adapterPaciente.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaciente.adapter = adapterPaciente

        if (SessionState.rol == Rol.PACIENTE && SessionState.loggedPersonId != null) {
            val indexPaciente = AppData.PACIENTES.indexOfFirst { it.id == SessionState.loggedPersonId }
            if (indexPaciente >= 0) {
                spinnerPaciente.setSelection(indexPaciente)
                spinnerPaciente.isEnabled = false
            }
        }

        val etFecha = dialogView.findViewById<EditText>(R.id.et_fecha_turno)
        val spinnerHorario = dialogView.findViewById<Spinner>(R.id.spinner_horario_disponible)
        val cbSobreturno = dialogView.findViewById<CheckBox>(R.id.cb_es_sobreturno)

        val fechaInicial = LocalDate.now()
        etFecha.setText(fechaInicial.toString())

        var horariosDisponibles = mutableListOf<LocalTime>()

        fun actualizarHorarios() {
            val fechaIngresada = try { LocalDate.parse(etFecha.text.toString().trim()) } catch (e: Exception) { fechaInicial }
            horariosDisponibles = generarHorariosDisponibles(agenda, fechaIngresada).toMutableList()
            val horariosStr = if (horariosDisponibles.isEmpty()) listOf("Sin horarios libres en esta fecha") else horariosDisponibles.map { it.toString() }
            val adapterHorarios = ArrayAdapter(this@MainNavActivity, android.R.layout.simple_spinner_item, horariosStr)
            adapterHorarios.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerHorario.adapter = adapterHorarios
        }

        actualizarHorarios()

        etFecha.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { actualizarHorarios() }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar Turno", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pacienteSeleccionado = AppData.PACIENTES.getOrNull(spinnerPaciente.selectedItemPosition)
            if (pacienteSeleccionado == null) {
                Toast.makeText(this, "Seleccione un paciente válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaStr = etFecha.text.toString().trim()
            val fechaTurno = try { LocalDate.parse(fechaStr) } catch (e: DateTimeParseException) { Toast.makeText(this, "Formato de fecha inválido (AAAA-MM-DD)", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val diaSemanaTurno = fechaTurno.dayOfWeek
            if (!agenda.diasSemana.contains(diaSemanaTurno) && !cbSobreturno.isChecked) {
                Toast.makeText(this, "No se atiende los $diaSemanaTurno. Días permitidos: $diasAtencionStr (o marque sobreturno)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val horaTurno = if (horariosDisponibles.isNotEmpty()) horariosDisponibles.getOrNull(spinnerHorario.selectedItemPosition) ?: agenda.horaInicio else agenda.horaInicio

            val nuevoTurno = Turno(
                id = (AppData.TURNOS.maxOfOrNull { it.id } ?: 0) + 1,
                pacienteId = pacienteSeleccionado.id,
                medicoId = medico.id,
                agendaId = agenda.id,
                fecha = fechaTurno,
                hora = horaTurno,
                duracion = agenda.duracionCita,
                estado = EstadoTurno.RESERVADO,
                esSobreturno = cbSobreturno.isChecked
            )

            if (TurnosManager.crearTurno(nuevoTurno)) {
                Toast.makeText(this, "Turno reservado con éxito para ${pacienteSeleccionado.fullName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onUpdate()
            } else {
                Toast.makeText(this, "Error al crear el turno", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirModalSacarTurnoEstudio(agenda: Agenda, onUpdate: () -> Unit) {
        if (AppData.PACIENTES.isEmpty()) {
            Toast.makeText(this, "No hay pacientes registrados en el sistema", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sacar_turno, null)
        val diasAtencionStr = agenda.diasSemana.joinToString(", ") { it.name }
        dialogView.findViewById<TextView>(R.id.tv_dialog_turno_titulo).text = "Sacar Turno - Estudio Clínico"
        dialogView.findViewById<TextView>(R.id.tv_dialog_turno_info).text = "Estudio: ${agenda.servicio.name}\nDías de atención: $diasAtencionStr\nHorario: ${agenda.horaInicio} a ${agenda.horaFin}\nAyuno: ${if (agenda.requiereAyuno) "Sí" else "No"}"

        val spinnerPaciente = dialogView.findViewById<Spinner>(R.id.spinner_paciente)
        val listaPacientesStr = AppData.PACIENTES.map { "ID ${it.id}: ${it.fullName} (DNI ${it.dni})" }
        val adapterPaciente = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaPacientesStr)
        adapterPaciente.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaciente.adapter = adapterPaciente

        if (SessionState.rol == Rol.PACIENTE && SessionState.loggedPersonId != null) {
            val indexPaciente = AppData.PACIENTES.indexOfFirst { it.id == SessionState.loggedPersonId }
            if (indexPaciente >= 0) {
                spinnerPaciente.setSelection(indexPaciente)
                spinnerPaciente.isEnabled = false
            }
        }

        val etFecha = dialogView.findViewById<EditText>(R.id.et_fecha_turno)
        val spinnerHorario = dialogView.findViewById<Spinner>(R.id.spinner_horario_disponible)
        val cbSobreturno = dialogView.findViewById<CheckBox>(R.id.cb_es_sobreturno)

        val fechaInicial = LocalDate.now()
        etFecha.setText(fechaInicial.toString())

        var horariosDisponibles = mutableListOf<LocalTime>()

        fun actualizarHorarios() {
            val fechaIngresada = try { LocalDate.parse(etFecha.text.toString().trim()) } catch (e: Exception) { fechaInicial }
            horariosDisponibles = generarHorariosDisponibles(agenda, fechaIngresada).toMutableList()
            val horariosStr = if (horariosDisponibles.isEmpty()) listOf("Sin horarios libres en esta fecha") else horariosDisponibles.map { it.toString() }
            val adapterHorarios = ArrayAdapter(this@MainNavActivity, android.R.layout.simple_spinner_item, horariosStr)
            adapterHorarios.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerHorario.adapter = adapterHorarios
        }

        actualizarHorarios()

        etFecha.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { actualizarHorarios() }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar Turno", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pacienteSeleccionado = AppData.PACIENTES.getOrNull(spinnerPaciente.selectedItemPosition)
            if (pacienteSeleccionado == null) {
                Toast.makeText(this, "Seleccione un paciente válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaStr = etFecha.text.toString().trim()
            val fechaTurno = try { LocalDate.parse(fechaStr) } catch (e: DateTimeParseException) { Toast.makeText(this, "Formato de fecha inválido (AAAA-MM-DD)", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val diaSemanaTurno = fechaTurno.dayOfWeek
            if (!agenda.diasSemana.contains(diaSemanaTurno) && !cbSobreturno.isChecked) {
                Toast.makeText(this, "No se atiende los $diaSemanaTurno. Días permitidos: $diasAtencionStr (o marque sobreturno)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val horaTurno = if (horariosDisponibles.isNotEmpty()) horariosDisponibles.getOrNull(spinnerHorario.selectedItemPosition) ?: agenda.horaInicio else agenda.horaInicio

            val nuevoTurno = Turno(
                id = (AppData.TURNOS.maxOfOrNull { it.id } ?: 0) + 1,
                pacienteId = pacienteSeleccionado.id,
                medicoId = 0,
                agendaId = agenda.id,
                fecha = fechaTurno,
                hora = horaTurno,
                duracion = agenda.duracionCita,
                estado = EstadoTurno.RESERVADO,
                esSobreturno = cbSobreturno.isChecked
            )

            if (TurnosManager.crearTurno(nuevoTurno)) {
                Toast.makeText(this, "Turno reservado con éxito para ${pacienteSeleccionado.fullName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onUpdate()
            } else {
                Toast.makeText(this, "Error al crear el turno", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirModalAgregarInsumo(onUpdate: () -> Unit) {
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

            if (InsumosManager.crearInsumo(nuevoInsumo)) {
                Toast.makeText(this, "Insumo agregado con éxito", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onUpdate()
            } else {
                Toast.makeText(this, "Error al agregar el insumo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirModalEditarInsumo(insumo: Insumo, onUpdate: () -> Unit) {
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
                onUpdate()
            }
            .setPositiveButton("Guardar Cambios") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val cantidad = etCantidad.text.toString().toIntOrNull() ?: insumo.cantidad

                if (nombre.isNotEmpty()) {
                    val actualizado = insumo.copy(nombre = nombre, cantidad = cantidad)
                    InsumosManager.modificarInsumo(actualizado)
                    Toast.makeText(this, "Insumo actualizado", Toast.LENGTH_SHORT).show()
                    onUpdate()
                }
            }
            .show()
    }

    private fun abrirModalCargarEstudio(onUpdate: () -> Unit) {
        if (AppData.PACIENTES.isEmpty()) {
            Toast.makeText(this, "No hay pacientes registrados en el sistema", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cargar_estudio, null)

        val spinnerPaciente = dialogView.findViewById<Spinner>(R.id.spinner_paciente_estudio)
        val listaPacientesStr = AppData.PACIENTES.map { "ID ${it.id}: ${it.fullName} (DNI ${it.dni})" }
        val adapterPaciente = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaPacientesStr)
        adapterPaciente.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaciente.adapter = adapterPaciente

        val spinnerTipo = dialogView.findViewById<Spinner>(R.id.spinner_tipo_estudio)
        val listaTipos = listOf(Servicio.ANALISIS_LABORATORIO.name, Servicio.RADIOGRAFIA.name, Servicio.ECOGRAFIA.name, "ESTUDIO_GENERAL")
        val adapterTipo = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaTipos)
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterTipo

        val etFecha = dialogView.findViewById<EditText>(R.id.et_fecha_estudio)
        val etHora = dialogView.findViewById<EditText>(R.id.et_hora_estudio)
        val etObservaciones = dialogView.findViewById<EditText>(R.id.et_observaciones_estudio)

        etFecha.setText(LocalDate.now().toString())
        etHora.setText(LocalTime.now().withSecond(0).withNano(0).toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar Estudio", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pacienteSeleccionado = AppData.PACIENTES.getOrNull(spinnerPaciente.selectedItemPosition)
            if (pacienteSeleccionado == null) {
                Toast.makeText(this, "Seleccione un paciente válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipoEstudio = spinnerTipo.selectedItem.toString()
            val fechaStr = etFecha.text.toString().trim()
            val horaStr = etHora.text.toString().trim()
            val observaciones = etObservaciones.text.toString().trim()

            val fecha = try { LocalDate.parse(fechaStr) } catch (e: DateTimeParseException) { Toast.makeText(this, "Formato de fecha inválido (AAAA-MM-DD)", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val hora = try { LocalTime.parse(horaStr) } catch (e: DateTimeParseException) { Toast.makeText(this, "Formato de hora inválido (HH:MM)", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val nuevoEstudio = Estudio(
                id = (AppData.ESTUDIOS.maxOfOrNull { it.id } ?: 0) + 1,
                nombre = tipoEstudio,
                pacienteId = pacienteSeleccionado.id,
                fecha = fecha,
                hora = hora,
                observaciones = observaciones
            )

            if (EstudiosManager.crearEstudio(nuevoEstudio)) {
                Toast.makeText(this, "Estudio cargado con éxito para ${pacienteSeleccionado.fullName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onUpdate()
            } else {
                Toast.makeText(this, "Error al guardar el estudio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDetalleEstudioDialog(estudio: Estudio, onUpdate: () -> Unit) {
        val paciente = AppData.PACIENTES.find { it.id == estudio.pacienteId }
        val mensaje = "Estudio: ${estudio.nombre}\nPaciente: ${paciente?.fullName ?: "ID ${estudio.pacienteId}"}\nFecha y Hora: ${estudio.fecha} ${estudio.hora}\nObservaciones: ${if (estudio.observaciones.isNotEmpty()) estudio.observaciones else "Ninguna"}"

        val builder = AlertDialog.Builder(this)
            .setTitle("Detalle del Estudio")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)

        if (SessionState.rol == Rol.ADMINISTRATIVO) {
            builder.setNeutralButton("Eliminar Estudio") { _, _ ->
                EstudiosManager.eliminarEstudio(estudio.id)
                Toast.makeText(this, "Estudio eliminado", Toast.LENGTH_SHORT).show()
                onUpdate()
            }
        }

        builder.show()
    }

    private fun generarHorariosDisponibles(agenda: Agenda, fecha: LocalDate): List<LocalTime> {
        val slots = mutableListOf<LocalTime>()
        var actual = agenda.horaInicio
        val fin = agenda.horaFin
        val duracion = if (agenda.duracionCita > 0) agenda.duracionCita.toLong() else 15L

        while (actual.plusMinutes(duracion) <= fin) {
            slots.add(actual)
            actual = actual.plusMinutes(duracion)
        }

        val ocupados = AppData.TURNOS.filter { it.agendaId == agenda.id && it.fecha == fecha && it.estado == EstadoTurno.RESERVADO && !it.esSobreturno }.map { it.hora }
        return slots.filter { it !in ocupados }
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }

    private fun parsearHora(texto: String, porDefecto: LocalTime): LocalTime {
        return try {
            LocalTime.parse(texto)
        } catch (e: DateTimeParseException) {
            porDefecto
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
