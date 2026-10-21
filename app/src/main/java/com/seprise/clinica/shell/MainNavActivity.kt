package com.seprise.clinica.shell

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seprise.clinica.R
import com.seprise.clinica.auth.LoginActivity
import com.seprise.clinica.core.Rol
import com.seprise.clinica.core.SessionState
import com.seprise.clinica.core.enableSePriseEdgeToEdge
import com.seprise.clinica.pacientes.PerfilPacienteActivity

/**
 * CU01 — shell de navegación. Los ítems dependen del rol de la sesión (ver [modulosDe]).
 * Barra custom scrollable: BottomNavigationView de Material topa en 5 ítems y administrativo
 * necesita 6 (ver design decisión 5).
 */
class MainNavActivity : AppCompatActivity() {

    private data class Modulo(val clave: String, @param:StringRes val nombre: Int, val clase: String)

    private val pacientes = Modulo("pacientes", R.string.cu01_nav_pacientes, "com.seprise.clinica.pacientes.PacientesActivity")
    private val agenda = Modulo("agenda", R.string.cu01_nav_agenda, "com.seprise.clinica.agenda.GestionAgendaActivity")
    private val turnos = Modulo("turnos", R.string.cu01_nav_turnos, "com.seprise.clinica.turnos.TurnosActivity")
    private val atencion = Modulo("atencion", R.string.cu01_nav_atencion, "com.seprise.clinica.atencion.AtencionActivity")
    private val estudios = Modulo("estudios", R.string.cu01_nav_estudios, "com.seprise.clinica.estudios.EstudiosActivity")
    private val insumos = Modulo("insumos", R.string.cu01_nav_insumos, "com.seprise.clinica.insumos.InsumosActivity")
    private val honorarios = Modulo("honorarios", R.string.cu01_nav_honorarios, "com.seprise.clinica.honorarios.HonorariosActivity")

    private lateinit var contenedorItems: LinearLayout
    private lateinit var contenido: TextView
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

        contenido = findViewById(R.id.cu01_contenido)
        contenedorItems = findViewById(R.id.cu01_nav_items)

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

    /** Regla de acceso por rol (ver Scenario "Ítems del ..."). */
    private fun modulosDe(rol: Rol): List<Modulo> = when (rol) {
        Rol.ADMINISTRATIVO -> listOf(pacientes, agenda, turnos, estudios, insumos, honorarios)
        Rol.MEDICO -> listOf(atencion, turnos, estudios)
        Rol.PACIENTE -> listOf(turnos, estudios)
    }

    private fun armarItems(modulos: List<Modulo>) {
        val pad = dp(12)
        modulos.forEach { modulo ->
            val item = TextView(this).apply {
                setText(modulo.nombre)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_nav))
                setPadding(pad, pad, pad, pad)
                setTextColor(getColor(R.color.seprise_muted))
                isClickable = true
                setOnClickListener { seleccionar(modulo, this) }
            }
            contenedorItems.addView(item)
        }
    }

    private fun seleccionar(modulo: Modulo, item: TextView) {
        activo?.apply {
            setBackgroundResource(0)
            setTextColor(getColor(R.color.seprise_muted))
        }
        item.setBackgroundResource(R.drawable.bg_nav_active)
        item.setTextColor(getColor(R.color.seprise_nav_text))
        activo = item

        contenido.text = getString(modulo.nombre)
        abrirModulo(modulo)
    }

    /** Navegación defensiva: si el módulo (otro lane) aún no existe, avisa en vez de crashear. */
    private fun abrirModulo(modulo: Modulo) {
        // Por nombre hasta que mergeen los lanes; luego Intent(this, XActivity::class.java).
        // La clase puede no estar declarada aún: startActivity lanza ActivityNotFoundException.
        val intent = Intent().setClassName(packageName, modulo.clase)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.cu01_proximamente, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
