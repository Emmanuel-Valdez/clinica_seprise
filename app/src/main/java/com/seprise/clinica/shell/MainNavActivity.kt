package com.seprise.clinica.shell

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
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
import com.seprise.clinica.pacientes.PerfilPacienteActivity

/**
 * CU01 — shell de navegación. El contenido central es el inicio (home);
 * cada módulo se abre como Activity independiente.
 */
class MainNavActivity : AppCompatActivity() {

    private data class Modulo(val clave: String, @param:StringRes val nombre: Int, val clase: String)

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
