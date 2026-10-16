package com.seprise.clinica.core

/** Rol del usuario logueado. */
enum class Rol { ADMINISTRATIVO, MEDICO, PACIENTE }

/**
 * Sesión en memoria, compartida por todos los lanes. No se persiste en disco: al
 * morir el proceso la app arranca sin sesión (ver Scenario "Sin sesión al reabrir la app").
 *
 * Congelado por dev1: otros lanes solo leen [rol] y [userId].
 */
object SessionState {
    var rol: Rol? = null
    var userId: String? = null

    data class Credencial(val usuario: String, val password: String, val rol: Rol)

    // Usuarios mock hardcodeados (uno por rol); reemplazar por AppData
    // cuando CU02/backend traigan altas de usuarios reales. Credenciales en memoria, sin hashing.
    val credenciales = listOf(
        Credencial("admin", "admin123", Rol.ADMINISTRATIVO),
        Credencial("medico", "medico123", Rol.MEDICO),
        Credencial("paciente", "paciente123", Rol.PACIENTE),
    )

    /** Valida usuario + contraseña + rol contra las credenciales mock. */
    fun login(usuario: String, password: String, rol: Rol): Boolean {
        val ok = credenciales.any { it.usuario == usuario && it.password == password && it.rol == rol }
        if (ok) {
            this.rol = rol
            this.userId = usuario
        }
        return ok
    }

    fun logout() {
        rol = null
        userId = null
    }
}
