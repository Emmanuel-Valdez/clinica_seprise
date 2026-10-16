package com.seprise.clinica

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** CU01 — ingreso por rol (administrativo / médico / paciente) contra credenciales mock. */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        val root = findViewById<View>(R.id.cu01_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val usuario = findViewById<EditText>(R.id.cu01_input_usuario)
        val password = findViewById<EditText>(R.id.cu01_input_password)
        val roles = findViewById<RadioGroup>(R.id.cu01_roles)
        val error = findViewById<TextView>(R.id.cu01_error)

        findViewById<Button>(R.id.cu01_btn_login).setOnClickListener {
            val u = usuario.text.toString().trim()
            val p = password.text.toString()
            val rol = when (roles.checkedRadioButtonId) {
                R.id.cu01_role_medico -> Rol.MEDICO
                R.id.cu01_role_paciente -> Rol.PACIENTE
                else -> Rol.ADMINISTRATIVO
            }

            when {
                u.isEmpty() || p.isEmpty() -> mostrarError(error, R.string.cu01_error_campos_vacios)
                SessionState.login(u, p, rol) -> {
                    startActivity(Intent(this, MainNavActivity::class.java))
                    finish()
                }
                else -> mostrarError(error, R.string.cu01_error_credenciales)
            }
        }
    }

    private fun mostrarError(vista: TextView, mensaje: Int) {
        vista.setText(mensaje)
        vista.visibility = View.VISIBLE
    }
}
