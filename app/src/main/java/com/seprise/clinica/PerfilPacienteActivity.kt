package com.seprise.clinica

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CU01 — perfil del paciente. Solo permite editar teléfono, dirección y contraseña.
 * HC y n° de empadronamiento se muestran pero no se editan.
 * Cada cambio se registra en [PerfilPacienteStore.auditoria] para que un administrativo
 * lo analice más adelante (fuera del alcance de CU01); el paciente no ve el log.
 */
class PerfilPacienteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil_paciente)
        val root = findViewById<View>(R.id.cu01_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val usuario = SessionState.userId
        if (SessionState.rol != Rol.PACIENTE || usuario == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val perfil = PerfilPacienteStore.obtener(usuario)
        val noEditable = getString(R.string.cu01_perfil_no_editable)

        findViewById<TextView>(R.id.cu01_perfil_usuario).text = usuario
        findViewById<TextView>(R.id.cu01_perfil_hc).text = "${perfil.hc}  $noEditable"
        findViewById<TextView>(R.id.cu01_perfil_empadronamiento).text = "${perfil.empadronamiento}  $noEditable"

        val telefono = findViewById<EditText>(R.id.cu01_perfil_telefono).apply { setText(perfil.telefono) }
        val direccion = findViewById<EditText>(R.id.cu01_perfil_direccion).apply { setText(perfil.direccion) }
        val password = findViewById<EditText>(R.id.cu01_perfil_password)

        findViewById<Button>(R.id.cu01_perfil_btn_guardar).setOnClickListener {
            val tel = telefono.text.toString().trim()
            val dir = direccion.text.toString().trim()
            if (tel != perfil.telefono) {
                registrarAuditoria(usuario, getString(R.string.cu01_perfil_label_telefono), perfil.telefono, tel)
                perfil.telefono = tel
            }
            if (dir != perfil.direccion) {
                registrarAuditoria(usuario, getString(R.string.cu01_perfil_label_direccion), perfil.direccion, dir)
                perfil.direccion = dir
            }
            val nueva = password.text.toString()
            if (nueva.isNotEmpty() && nueva != perfil.password) {
                // No se registra el valor de la contraseña (secreto); solo se marca el cambio.
                registrarAuditoria(usuario, getString(R.string.cu01_perfil_label_password), "(oculta)", "(oculta)")
                perfil.password = nueva
            }
            password.setText("")
            Toast.makeText(this, R.string.cu01_perfil_guardado, Toast.LENGTH_SHORT).show()
        }
    }

    private fun registrarAuditoria(usuario: String, campo: String, anterior: String, nuevo: String) {
        PerfilPacienteStore.auditoria.add(RegistroAuditoria(ahora(), usuario, campo, anterior, nuevo))
    }

    private fun ahora(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}

data class PerfilPacienteData(
    val hc: String,
    val empadronamiento: String,
    var telefono: String = "",
    var direccion: String = "",
    var password: String = "",
)

data class RegistroAuditoria(
    val fecha: String,
    val usuario: String,
    val campo: String,
    val anterior: String,
    val nuevo: String,
)

/**
 * Datos y auditoría en memoria, locales al CU01; mover a AppData.PACIENTES cuando
 * CU02 (ABM pacientes) defina el modelo del paciente.
 * La auditoría la consume el administrativo (a futuro), no el paciente.
 */
object PerfilPacienteStore {
    private val perfiles = mutableMapOf<String, PerfilPacienteData>()

    /** Log de cambios de todos los pacientes, con el usuario que los hizo (para auditoría del admin). */
    val auditoria = mutableListOf<RegistroAuditoria>()

    fun obtener(usuario: String): PerfilPacienteData = perfiles.getOrPut(usuario) {
        PerfilPacienteData(hc = "HC-$usuario", empadronamiento = "EMP-$usuario")
    }
}
