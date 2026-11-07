package com.seprise.clinica.shell

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seprise.clinica.R
import com.seprise.clinica.auth.LoginActivity
import com.seprise.clinica.core.AppData
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.pacientes.PerfilPacienteActivity

abstract class BaseNavActivity : AppCompatActivity() {

    protected data class NavItem(val clave: String, @StringRes val nombre: Int, val activityClass: Class<*>)

    private lateinit var contenedorContenido: FrameLayout
    private lateinit var contenedorItems: LinearLayout
    private var activo: TextView? = null

    protected abstract fun getContentLayoutId(): Int
    protected abstract fun getActiveNavItemKey(): String
    protected open fun onContentViewCreated(view: View) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSePriseEdgeToEdge()
        setContentView(R.layout.activity_base_nav)

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

        findViewById<TextView>(R.id.txt_nombre_usuario)?.text = obtenerNombrePersonaLogueada()

        findViewById<ImageView>(R.id.btn_home_logo).setOnClickListener {
            startActivity(Intent(this, MainNavActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.cu01_btn_perfil).apply {
            visibility = if (rol == Rol.PACIENTE) View.VISIBLE else View.GONE
            setOnClickListener { startActivity(Intent(this@BaseNavActivity, PerfilPacienteActivity::class.java)) }
        }

        findViewById<Button>(R.id.cu01_btn_logout).setOnClickListener {
            SessionState.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        armarItems(modulosDe(rol))

        val contentLayoutId = getContentLayoutId()
        if (contentLayoutId != 0) {
            val contentView = LayoutInflater.from(this).inflate(contentLayoutId, contenedorContenido, false)
            contenedorContenido.addView(contentView)
            onContentViewCreated(contentView)
        }
    }

    override fun onResume() {
        super.onResume()
        resaltarActivo()
    }

    protected fun marcarActivoPorClave(clave: String) {
        for (i in 0 until contenedorItems.childCount) {
            val child = contenedorItems.getChildAt(i)
            if (child is TextView && child.tag == clave) {
                resaltar(child)
                return
            }
        }
    }

    private fun resaltarActivo() {
        val clave = getActiveNavItemKey()
        marcarActivoPorClave(clave)
    }

    private fun resaltar(item: TextView) {
        activo?.apply {
            setBackgroundResource(0)
            setTextColor(getColor(R.color.seprise_muted))
        }
        item.setBackgroundResource(R.drawable.bg_nav_active)
        item.setTextColor(getColor(R.color.seprise_nav_text))
        activo = item
    }

    private fun modulosDe(rol: Rol): List<NavItem> = when (rol) {
        Rol.ADMINISTRATIVO -> listOf(
            NavItem("pacientes", R.string.cu01_nav_pacientes, com.seprise.clinica.pacientes.GestionPacientesActivity::class.java),
            NavItem("agenda", R.string.cu01_nav_agenda, com.seprise.clinica.agenda.GestionAgendaActivity::class.java),
            NavItem("turnos", R.string.cu01_nav_turnos, com.seprise.clinica.turnos.GestionTurnosActivity::class.java),
            NavItem("estudios", R.string.cu01_nav_estudios, com.seprise.clinica.estudios.GestionEstudiosActivity::class.java),
            NavItem("insumos", R.string.cu01_nav_insumos, com.seprise.clinica.insumos.GestionInsumosActivity::class.java),
            NavItem("honorarios", R.string.cu01_nav_honorarios, com.seprise.clinica.honorarios.HonorariosActivity::class.java)
        )
        Rol.MEDICO -> listOf(
            NavItem("atencion", R.string.cu01_nav_atencion, com.seprise.clinica.atencion.AtencionActivity::class.java),
            NavItem("agenda", R.string.cu01_nav_agenda, com.seprise.clinica.agenda.GestionAgendaActivity::class.java),
            NavItem("estudios", R.string.cu01_nav_estudios, com.seprise.clinica.estudios.GestionEstudiosActivity::class.java)
        )
        Rol.PACIENTE -> listOf(
            NavItem("turnos", R.string.cu01_nav_turnos, com.seprise.clinica.turnos.GestionTurnosActivity::class.java),
            NavItem("estudios", R.string.cu01_nav_estudios, com.seprise.clinica.estudios.GestionEstudiosActivity::class.java)
        )
    }

    private fun armarItems(modulos: List<NavItem>) {
        contenedorItems.removeAllViews()
        val pad = dp(12)

        val inicio = TextView(this).apply {
            text = "Inicio"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_nav))
            setPadding(pad, pad, pad, pad)
            setTextColor(getColor(R.color.seprise_muted))
            isClickable = true
            tag = "inicio"
            setOnClickListener {
                startActivity(Intent(this@BaseNavActivity, MainNavActivity::class.java))
                finish()
            }
        }
        contenedorItems.addView(inicio)

        modulos.forEach { item ->
            val navItem = TextView(this).apply {
                setText(item.nombre)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_nav))
                setPadding(pad, pad, pad, pad)
                setTextColor(getColor(R.color.seprise_muted))
                isClickable = true
                tag = item.clave
                setOnClickListener {
                    if (item.clave != getActiveNavItemKey()) {
                        startActivity(Intent(this@BaseNavActivity, item.activityClass))
                    }
                }
            }
            contenedorItems.addView(navItem)
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
