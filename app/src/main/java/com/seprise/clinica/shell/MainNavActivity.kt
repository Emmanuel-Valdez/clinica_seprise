package com.seprise.clinica.shell

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.seprise.clinica.atencion.AtencionManager
import com.seprise.clinica.auth.LoginActivity
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.estudios.EstudiosManager
import com.seprise.clinica.honorarios.HonorariosManager
import com.seprise.clinica.insumos.InsumosManager
import com.seprise.clinica.models.Agenda
import com.seprise.clinica.models.Atencion
import com.seprise.clinica.models.Circuito
import com.seprise.clinica.models.EstadoTurno
import com.seprise.clinica.models.Estudio
import com.seprise.clinica.models.Insumo
import com.seprise.clinica.models.LiquidacionHonorario
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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * CU01 — Shell de navegación principal con barra superior e inferior siempre visibles.
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
    private lateinit var contenedorContenido: FrameLayout
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

        contenedorContenido = findViewById(R.id.cu01_contenido_container)
        contenedorItems = findViewById(R.id.cu01_nav_items)

        // Nombre de la persona logueada en la barra superior
        findViewById<TextView>(R.id.txt_nombre_usuario)?.text = obtenerNombrePersonaLogueada()

        // El logo redirige a Inicio
        findViewById<ImageView>(R.id.btn_home_logo).setOnClickListener {
            itemInicio?.let { marcarActivo(it) }
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

    override fun onResume() {
        super.onResume()
        if (activo == itemInicio) {
            SessionState.rol?.let { renderHome(it) }
        }
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
            setOnClickListener {
                marcarActivo(this)
                mostrarPantallaInicio()
            }
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

    private fun mostrarPantallaInicio() {
        contenedorContenido.removeAllViews()
        val homeView = LayoutInflater.from(this).inflate(R.layout.activity_main_nav, contenedorContenido, false)
        val homeLayout = homeView.findViewById<View>(R.id.layout_inicio_home)
        if (homeLayout != null) {
            (homeLayout.parent as? ViewGroup)?.removeView(homeLayout)
            contenedorContenido.addView(homeLayout)
        }
        SessionState.rol?.let { renderHome(it) }
    }

    /** Carga e inicializa la vista del módulo seleccionado dentro del contenedor manteniendo la barra visible. */
    private fun abrirModulo(modulo: Modulo) {
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
            "atencion" -> {
                val v = LayoutInflater.from(this).inflate(R.layout.activity_gestion_atencion, contenedorContenido, false)
                inicializarAtencionView(v)
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
            "honorarios" -> {
                val v = LayoutInflater.from(this).inflate(R.layout.activity_honorarios, contenedorContenido, false)
                inicializarHonorariosView(v)
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

    private fun abrirModalAgregarInsumo(onUpdate: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_insumo, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.et_nombre_insumo)
        val etCantidad = dialogView.findViewById<EditText>(R.id.et_cantidad_insumo)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0
                if (nombre.isNotEmpty()) {
                    val nuevoInsumo = Insumo((AppData.INSUMOS.maxOfOrNull { it.id } ?: 0) + 1, nombre, cantidad)
                    InsumosManager.crearInsumo(nuevoInsumo)
                    Toast.makeText(this, "Insumo agregado con éxito", Toast.LENGTH_SHORT).show()
                    onUpdate()
                }
            }
            .show()
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
                    InsumosManager.modificarInsumo(insumo.copy(nombre = nombre, cantidad = cantidad))
                    Toast.makeText(this, "Insumo actualizado", Toast.LENGTH_SHORT).show()
                    onUpdate()
                }
            }
            .show()
    }

    // ==========================================
    // VISTA Y LÓGICA DE ATENCIÓN EN EL SHELL
    // ==========================================
    private fun inicializarAtencionView(v: View) {
        val containerAtenciones = v.findViewById<LinearLayout>(R.id.container_atenciones)
        val etBuscar = v.findViewById<EditText>(R.id.et_buscar_atencion)
        val btnCargar = v.findViewById<Button>(R.id.btn_cargar_atencion)
        val tvMedicoInfo = v.findViewById<TextView>(R.id.tv_medico_atencion_info)

        val medicoIdLogueado = SessionState.loggedPersonId ?: 1
        val medico = AppData.MEDICOS.find { it.id == medicoIdLogueado }
        val medicoNombre = medico?.fullName ?: "Dr/a. Médico"
        tvMedicoInfo.text = "Médico atendiendo: Dr/a. $medicoNombre"

        fun cargarLista(filtro: String = "") {
            containerAtenciones.removeAllViews()
            val filtroNorm = normalizarTexto(filtro)
            val atencionesDelMedico = AtencionManager.obtenerAtencionesPorMedico(medicoIdLogueado)

            val filtradas = if (filtroNorm.isBlank()) {
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

            if (filtradas.isEmpty()) {
                mostrarMensajeVacio(containerAtenciones, "No hay atenciones registradas")
                return
            }

            for (atencion in filtradas) {
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

        btnCargar.setOnClickListener {
            abrirModalCargarAtencion(medicoIdLogueado) { cargarLista(etBuscar.text.toString()) }
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarLista(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarLista()
    }

    private fun abrirModalCargarAtencion(medicoIdLogueado: Int, onUpdate: () -> Unit) {
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
        val pacientesInfoList = pacientesActivos.map { "${it.fullName} (DNI: ${it.dni})" }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pacientesInfoList)
        actvPaciente.setAdapter(adapter)
        actvPaciente.threshold = 1

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
            onUpdate()
        }
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
            } else {
                AppData.ESTUDIOS
            }

            val filtroNorm = normalizarTexto(filtro)
            val filtrados = if (filtroNorm.isBlank()) {
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

            if (filtrados.isEmpty()) {
                mostrarMensajeVacio(containerEstudios, "No hay estudios clínicos registrados")
                return
            }

            for (estudio in filtrados) {
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

    private fun abrirModalCargarEstudio(onUpdate: () -> Unit) {
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
                onUpdate()
            } else {
                Toast.makeText(this, "Error al guardar el estudio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDetalleEstudioDialog(estudio: Estudio, onUpdate: () -> Unit) {
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
                onUpdate()
            }
        }

        builder.show()
    }

    // ==========================================
    // VISTA Y LÓGICA DE AGENDAS EN EL SHELL
    // ==========================================
    private fun inicializarAgendaView(v: View) {
        val containerMedicos = v.findViewById<LinearLayout>(R.id.container_medicos)
        val etBuscar = v.findViewById<EditText>(R.id.et_buscar_medico)
        val rgCircuito = v.findViewById<RadioGroup>(R.id.rg_circuito_agenda)

        fun cargarLista(filtro: String = "") {
            containerMedicos.removeAllViews()
            val esEstudios = rgCircuito.checkedRadioButtonId == R.id.rb_agenda_estudios
            val filtroNorm = normalizarTexto(filtro)

            if (esEstudios) {
                val agendasEstudios = AppData.AGENDAS.filter { it.circuito == Circuito.ESTUDIOS_CLINICOS }
                val filtradas = if (filtroNorm.isBlank()) {
                    agendasEstudios
                } else {
                    agendasEstudios.filter { normalizarTexto(it.servicio.name).contains(filtroNorm) }
                }

                if (filtradas.isEmpty()) {
                    mostrarMensajeVacio(containerMedicos, "No hay agendas de estudios configuradas.")
                    return
                }
                for (agenda in filtradas) {
                    val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)
                    val diasStr = agenda.diasSemana.joinToString(", ") { it.name }
                    item.findViewById<TextView>(R.id.tv_nombre_medico).text = "Estudio: ${agenda.servicio.name}"
                    item.findViewById<TextView>(R.id.tv_especialidad_medico).text = "Días: $diasStr (${agenda.horaInicio} - ${agenda.horaFin})"
                    item.findViewById<TextView>(R.id.tv_estado_agenda).text = "Estudio Clínico"
                    item.findViewById<Button>(R.id.btn_accion_agenda).visibility = View.GONE
                    containerMedicos.addView(item)
                }
            } else {
                val medicosBase = if (SessionState.rol == Rol.MEDICO && SessionState.loggedPersonId != null) {
                    AppData.MEDICOS.filter { it.id == SessionState.loggedPersonId }
                } else {
                    AppData.MEDICOS
                }

                val medicosFiltrados = if (filtroNorm.isBlank()) {
                    medicosBase
                } else {
                    medicosBase.filter { normalizarTexto(it.fullName).contains(filtroNorm) }
                }

                if (medicosFiltrados.isEmpty()) {
                    mostrarMensajeVacio(containerMedicos, "No se encontraron médicos.")
                    return
                }

                for (medico in medicosFiltrados) {
                    val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerMedicos, false)

                    val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_medico)
                    val tvEspecialidad = item.findViewById<TextView>(R.id.tv_especialidad_medico)
                    val tvEstado = item.findViewById<TextView>(R.id.tv_estado_agenda)
                    val btnAccion = item.findViewById<Button>(R.id.btn_accion_agenda)

                    tvNombre.text = "Dr/a. ${medico.fullName}"

                    val agendaExistente = AgendaManager.obtenerAgendaPorMedico(medico.id)

                    if (agendaExistente != null && agendaExistente.circuito == Circuito.CONSULTORIOS_EXTERNOS) {
                        val diasStr = agendaExistente.diasSemana.joinToString(", ") { it.name }
                        tvEspecialidad.text = "Esp: ${medico.especialidad.name} | Días: $diasStr"
                        tvEstado.text = "Agenda Configurada"
                        tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_success))
                        btnAccion.text = "Ver Agenda"
                        btnAccion.setOnClickListener { mostrarDetalleAgendaDialog(medico, agendaExistente) { cargarLista(etBuscar.text.toString()) } }
                    } else {
                        tvEspecialidad.text = "Especialidad: ${medico.especialidad.name}"
                        tvEstado.text = "Sin Agenda"
                        tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
                        if (SessionState.rol == Rol.MEDICO) {
                            btnAccion.text = "Sin Agenda"
                            btnAccion.isEnabled = false
                        } else {
                            btnAccion.text = "Cargar Agenda"
                            btnAccion.setOnClickListener { abrirModalCargarAgenda(medico) { cargarLista(etBuscar.text.toString()) } }
                        }
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

        etHoraInicio.setText("08:00")
        etHoraFin.setText("16:00")
        etDuracionCita.setText("15")

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

            val horaInicio = try { LocalTime.parse(etHoraInicio.text.toString().trim()) } catch (e: Exception) { LocalTime.of(8, 0) }
            val horaFin = try { LocalTime.parse(etHoraFin.text.toString().trim()) } catch (e: Exception) { LocalTime.of(16, 0) }
            val duracionMins = etDuracionCita.text.toString().toIntOrNull() ?: 15

            val servicioDelMedico = try {
                Servicio.valueOf(medico.especialidad.name)
            } catch (e: Exception) {
                Servicio.CLINICA_MEDICA
            }

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

            val creada = AgendaManager.crearAgenda(nuevaAgenda)
            if (creada) {
                Toast.makeText(this, "Agenda guardada con éxito para Dr/a. ${medico.apellido}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onUpdate()
            } else {
                Toast.makeText(this, "Error al guardar la agenda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDetalleAgendaDialog(medico: Medico, agenda: Agenda, onUpdate: () -> Unit) {
        val diasStr = agenda.diasSemana.joinToString(", ") { it.name }
        val mensaje = """
            Médico: Dr/a. ${medico.fullName}
            Especialidad: ${medico.especialidad.name}
            Días de Atención: $diasStr
            Horario: ${agenda.horaInicio} - ${agenda.horaFin}
            Duración por Cita: ${agenda.duracionCita} min
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("Detalle de Agenda")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)

        if (SessionState.rol == Rol.ADMINISTRATIVO) {
            builder.setNeutralButton("Eliminar Agenda") { _, _ ->
                AgendaManager.eliminarAgenda(agenda.id)
                Toast.makeText(this, "Agenda eliminada", Toast.LENGTH_SHORT).show()
                onUpdate()
            }
        }

        builder.show()
    }

    // ==========================================
    // VISTA Y LÓGICA DE HONORARIOS EN EL SHELL
    // ==========================================
    private fun inicializarHonorariosView(v: View) {
        val spinnerMedico = v.findViewById<Spinner>(R.id.spinner_medico_honorarios)
        val etDesde = v.findViewById<EditText>(R.id.et_honorario_desde)
        val etHasta = v.findViewById<EditText>(R.id.et_honorario_hasta)
        val tvResumen = v.findViewById<TextView>(R.id.tv_resumen_honorarios)
        val containerHonorarios = v.findViewById<LinearLayout>(R.id.container_honorarios)
        val btnLiquidar = v.findViewById<Button>(R.id.btn_liquidar_honorarios)
        val btnCalcular = v.findViewById<Button>(R.id.btn_calcular_honorarios)

        var pendientes: List<Atencion> = emptyList()
        var medicoActualId = 0
        var desdeActual: LocalDate? = null
        var hastaActual: LocalDate? = null

        val medicosInfo = AppData.MEDICOS.map { "Dr/a. ${it.fullName} · ${nombreEspecialidad(it.especialidad.name)}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, medicosInfo)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMedico.adapter = adapter

        val hoy = LocalDate.now()
        etDesde.setText(hoy.withDayOfMonth(1).toString())
        etHasta.setText(hoy.withDayOfMonth(hoy.lengthOfMonth()).toString())

        fun render() {
            containerHonorarios.removeAllViews()

            if (pendientes.isEmpty()) {
                tvResumen.text = getString(R.string.cu06_sin_atenciones)
                btnLiquidar.visibility = View.GONE
                return
            }

            for (atencion in pendientes) {
                val item = LayoutInflater.from(this).inflate(R.layout.item_honorario_atencion, containerHonorarios, false)
                val paciente = AppData.PACIENTES.find { it.id == atencion.pacienteId }

                item.findViewById<TextView>(R.id.tv_paciente_honorario).text = paciente?.fullName ?: "ID ${atencion.pacienteId}"
                item.findViewById<TextView>(R.id.tv_valor_honorario).text = "$${atencion.valorConsulta}"
                item.findViewById<TextView>(R.id.tv_fecha_honorario).text = "${atencion.fecha} ${atencion.hora}"
                item.findViewById<TextView>(R.id.tv_diagnostico_honorario).text = if (atencion.observaciones.isNotEmpty()) atencion.observaciones else "Consulta médica"

                containerHonorarios.addView(item)
            }

            val total = HonorariosManager.calcularTotal(pendientes)
            tvResumen.text = getString(R.string.cu06_resumen, pendientes.size, "$$total")
            btnLiquidar.visibility = View.VISIBLE
        }

        fun calcular() {
            val desdeStr = etDesde.text.toString().trim()
            val hastaStr = etHasta.text.toString().trim()

            val desde = try { LocalDate.parse(desdeStr) } catch (e: Exception) { null }
            val hasta = try { LocalDate.parse(hastaStr) } catch (e: Exception) { null }

            if (desde == null || hasta == null) {
                Toast.makeText(this, R.string.cu06_error_fecha, Toast.LENGTH_SHORT).show()
                containerHonorarios.removeAllViews()
                tvResumen.text = ""
                btnLiquidar.visibility = View.GONE
                return
            }

            if (desde.isAfter(hasta)) {
                Toast.makeText(this, R.string.cu06_error_rango, Toast.LENGTH_SHORT).show()
                containerHonorarios.removeAllViews()
                tvResumen.text = ""
                btnLiquidar.visibility = View.GONE
                return
            }

            val medico = AppData.MEDICOS.getOrNull(spinnerMedico.selectedItemPosition) ?: return
            medicoActualId = medico.id
            desdeActual = desde
            hastaActual = hasta
            pendientes = HonorariosManager.obtenerAtencionesPendientes(medico.id, desde, hasta)
            render()
        }

        btnCalcular.setOnClickListener { calcular() }

        btnLiquidar.setOnClickListener {
            val medico = AppData.MEDICOS.find { it.id == medicoActualId } ?: return@setOnClickListener
            val desde = desdeActual ?: return@setOnClickListener
            val hasta = hastaActual ?: return@setOnClickListener
            val total = HonorariosManager.calcularTotal(pendientes)

            AlertDialog.Builder(this)
                .setTitle(R.string.cu06_confirm_title)
                .setMessage("Médico: Dr/a. ${medico.fullName}\nPeríodo: $desde - $hasta\nAtenciones: ${pendientes.size}\nTotal: $$total")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.cu06_confirm_btn) { _, _ ->
                    val liquidacion = HonorariosManager.registrarLiquidacion(medico.id, desde, hasta)
                    if (liquidacion == null) {
                        Toast.makeText(this, R.string.cu06_sin_atenciones, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.cu06_liquidado, Toast.LENGTH_SHORT).show()
                    }
                    calcular()
                }
                .show()
        }

        calcular()
    }

    // ==========================================
    // VISTA Y LÓGICA DE TURNOS EN EL SHELL
    // ==========================================
    private fun inicializarTurnosView(v: View) {
        val containerTurnos = v.findViewById<LinearLayout>(R.id.container_medicos)
        val etBuscar = v.findViewById<EditText>(R.id.et_buscar_medico)
        val rgCircuito = v.findViewById<RadioGroup>(R.id.rg_circuito_turnos)
        val rgAdmin = v.findViewById<RadioGroup>(R.id.rg_filtro_turnos_admin)
        val tvSubtitulo = v.findViewById<TextView>(R.id.tv_subtitulo_gestion)

        fun cargarTurnosPacientesAdmin(filtro: String) {
            val filtroNorm = normalizarTexto(filtro)
            val pacientesBase = AppData.PACIENTES.filter { it.activo }

            val pacientesFiltrados = if (filtroNorm.isBlank()) {
                pacientesBase
            } else {
                pacientesBase.filter {
                    normalizarTexto(it.fullName).contains(filtroNorm) ||
                    normalizarTexto(it.dni).contains(filtroNorm)
                }
            }

            if (pacientesFiltrados.isEmpty()) {
                mostrarMensajeVacio(containerTurnos, "No se encontraron pacientes")
                return
            }

            for (paciente in pacientesFiltrados) {
                val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerTurnos, false)

                val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_medico)
                val tvEspecialidad = item.findViewById<TextView>(R.id.tv_especialidad_medico)
                val tvEstado = item.findViewById<TextView>(R.id.tv_estado_agenda)
                val btnAccion = item.findViewById<Button>(R.id.btn_accion_agenda)

                tvNombre.text = paciente.fullName
                tvEspecialidad.text = "DNI: ${paciente.dni} | Mail: ${paciente.mail}"

                val turnosPaciente = AppData.TURNOS.filter { it.pacienteId == paciente.id }

                if (turnosPaciente.isEmpty()) {
                    tvEstado.text = "Sin turnos asignados"
                    tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
                } else {
                    tvEstado.text = "${turnosPaciente.size} turno(s) asignado(s)"
                    tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_blue))
                }

                btnAccion.text = "Ver Turnos"
                btnAccion.setOnClickListener { mostrarTurnosDialog("Paciente ${paciente.fullName}", turnosPaciente) }
                item.setOnClickListener { mostrarTurnosDialog("Paciente ${paciente.fullName}", turnosPaciente) }

                containerTurnos.addView(item)
            }
        }

        fun cargarTurnosMedicosAdmin(filtro: String) {
            val filtroNorm = normalizarTexto(filtro)
            val medicosBase = AppData.MEDICOS

            val medicosFiltrados = if (filtroNorm.isBlank()) {
                medicosBase
            } else {
                medicosBase.filter {
                    normalizarTexto(it.fullName).contains(filtroNorm) ||
                    normalizarTexto(it.especialidad.name).contains(filtroNorm) ||
                    normalizarTexto(nombreEspecialidad(it.especialidad.name)).contains(filtroNorm)
                }
            }

            if (medicosFiltrados.isEmpty()) {
                mostrarMensajeVacio(containerTurnos, "No se encontraron médicos")
                return
            }

            for (medico in medicosFiltrados) {
                val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerTurnos, false)

                val tvNombre = item.findViewById<TextView>(R.id.tv_nombre_medico)
                val tvEspecialidad = item.findViewById<TextView>(R.id.tv_especialidad_medico)
                val tvEstado = item.findViewById<TextView>(R.id.tv_estado_agenda)
                val btnAccion = item.findViewById<Button>(R.id.btn_accion_agenda)

                tvNombre.text = "Dr/a. ${medico.fullName}"
                tvEspecialidad.text = "Especialidad: ${nombreEspecialidad(medico.especialidad.name)}"

                val turnosMedico = AppData.TURNOS.filter { it.medicoId == medico.id }

                if (turnosMedico.isEmpty()) {
                    tvEstado.text = "Sin turnos asignados"
                    tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_muted))
                } else {
                    tvEstado.text = "${turnosMedico.size} turno(s) asignado(s)"
                    tvEstado.setTextColor(ContextCompat.getColor(this, R.color.seprise_blue))
                }

                btnAccion.text = "Ver Turnos"
                btnAccion.setOnClickListener { mostrarTurnosDialog("Médico Dr/a. ${medico.fullName}", turnosMedico) }
                item.setOnClickListener { mostrarTurnosDialog("Médico Dr/a. ${medico.fullName}", turnosMedico) }

                containerTurnos.addView(item)
            }
        }

        fun cargarLista(filtro: String = "") {
            containerTurnos.removeAllViews()

            if (SessionState.rol == Rol.ADMINISTRATIVO) {
                val esPacientes = rgAdmin.checkedRadioButtonId == R.id.rb_turnos_pacientes
                if (esPacientes) {
                    cargarTurnosPacientesAdmin(filtro)
                } else {
                    cargarTurnosMedicosAdmin(filtro)
                }
            } else {
                val esEstudios = rgCircuito.checkedRadioButtonId == R.id.rb_turnos_estudios
                val filtroNorm = normalizarTexto(filtro)

                val turnosBase = if (SessionState.rol == Rol.PACIENTE && SessionState.loggedPersonId != null) {
                    AppData.TURNOS.filter { it.pacienteId == SessionState.loggedPersonId }
                } else {
                    AppData.TURNOS
                }

                val filtrados = if (filtroNorm.isBlank()) {
                    turnosBase
                } else {
                    turnosBase.filter { turno ->
                        val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
                        val medico = AppData.MEDICOS.find { it.id == turno.medicoId }
                        normalizarTexto(paciente?.fullName ?: "").contains(filtroNorm) ||
                                normalizarTexto(medico?.fullName ?: "").contains(filtroNorm)
                    }
                }

                if (filtrados.isEmpty()) {
                    mostrarMensajeVacio(containerTurnos, "No hay turnos registrados")
                    return
                }

                for (turno in filtrados) {
                    val item = LayoutInflater.from(this).inflate(R.layout.item_medico_agenda, containerTurnos, false)
                    val medico = AppData.MEDICOS.find { it.id == turno.medicoId }
                    val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }

                    item.findViewById<TextView>(R.id.tv_nombre_medico).text = "Dr/a. ${medico?.fullName ?: "Médico"}"
                    item.findViewById<TextView>(R.id.tv_especialidad_medico).text = "Paciente: ${paciente?.fullName ?: "Paciente"} | Fecha: ${turno.fecha} ${turno.hora}"
                    containerTurnos.addView(item)
                }
            }
        }

        if (SessionState.rol == Rol.ADMINISTRATIVO) {
            rgAdmin.visibility = View.VISIBLE
            rgCircuito.visibility = View.GONE
            tvSubtitulo.text = "Visualice y gestione los turnos por paciente o por médico."
            etBuscar.hint = "Buscar por nombre o DNI de paciente..."

            rgAdmin.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId == R.id.rb_turnos_pacientes) {
                    etBuscar.hint = "Buscar por nombre o DNI de paciente..."
                } else {
                    etBuscar.hint = "Buscar por nombre o especialidad de médico..."
                }
                cargarLista(etBuscar.text.toString())
            }
        } else {
            rgAdmin.visibility = View.GONE
            rgCircuito.visibility = View.VISIBLE
            tvSubtitulo.text = "Seleccione un circuito para solicitar su turno."
            etBuscar.hint = "Buscar por nombre o servicio..."

            rgCircuito.setOnCheckedChangeListener { _, _ -> cargarLista(etBuscar.text.toString()) }
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { cargarLista(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarLista()
    }

    private fun mostrarTurnosDialog(nombreHeader: String, turnos: List<Turno>) {
        if (turnos.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Turnos de $nombreHeader")
                .setMessage("No hay turnos registrados.")
                .setNegativeButton("Cerrar", null)
                .show()
            return
        }

        val detalleTurnos = turnos.joinToString("\n\n") { turno ->
            val paciente = AppData.PACIENTES.find { it.id == turno.pacienteId }
            val medico = AppData.MEDICOS.find { it.id == turno.medicoId }
            val pacienteNombre = paciente?.fullName ?: "Paciente ID ${turno.pacienteId}"
            val medicoNombre = medico?.fullName ?: "Médico ID ${turno.medicoId}"
            "• Fecha: ${turno.fecha} ${turno.hora}\n  Paciente: $pacienteNombre\n  Médico: Dr/a. $medicoNombre\n  Estado: ${turno.estado}\n  Sobreturno: ${if (turno.esSobreturno) "Sí" else "No"}"
        }

        AlertDialog.Builder(this)
            .setTitle("Turnos de $nombreHeader")
            .setMessage(detalleTurnos)
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun mostrarMensajeVacio(container: LinearLayout, mensaje: String) {
        val tvVacio = TextView(this).apply {
            text = mensaje
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainNavActivity, R.color.seprise_muted))
            setPadding(0, 24, 0, 0)
        }
        container.addView(tvVacio)
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
            "Sin turnos programados para hoy.",
            "Ver agendas"
        ) { abrirModulo(agenda) }

        val ingresosEstimados = turnosHoy.size * 15000
        agregarKpis(
            contenedor,
            listOf(
                "Turnos hoy" to turnosHoy.size.toString(),
                "Pacientes hoy" to turnosHoy.map { it.pacienteId }.distinct().size.toString(),
                "Facturación aprox." to "$$ingresosEstimados"
            )
        )

        agregarAccionPrincipal(contenedor, "Cargar agenda médica") { abrirModulo(agenda) }

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

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "").lowercase()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
