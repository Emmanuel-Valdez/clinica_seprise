package com.seprise.clinica.core

import java.text.Normalizer

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
    var loggedPersonId: Int? = null

    data class Credencial(val usuario: String, val password: String, val rol: Rol)

    /** Valida usuario + contraseña + rol contra AppData según el rol y almacena el ID de la persona. */
    fun login(usuario: String, password: String, rol: Rol): Boolean {
        if (password.isEmpty() || usuario.trim().isEmpty()) {
            return false
        }

        val uNormal = normalizarTexto(usuario.trim())
        var personIdEncontrado: Int? = null

        val valido = when (rol) {
            Rol.ADMINISTRATIVO -> {
                val admin = AppData.ADMINISTRADORES.find { generarUsername(it.nombre, it.apellido) == uNormal }
                if (admin != null) {
                    personIdEncontrado = admin.id
                    true
                } else uNormal == "admin"
            }
            Rol.MEDICO -> {
                val medico = AppData.MEDICOS.find { generarUsername(it.nombre, it.apellido) == uNormal }
                if (medico != null) {
                    personIdEncontrado = medico.id
                    true
                } else uNormal == "medico"
            }
            Rol.PACIENTE -> {
                val paciente = AppData.PACIENTES.find { generarUsername(it.nombre, it.apellido) == uNormal }
                if (paciente != null) {
                    personIdEncontrado = paciente.id
                    true
                } else uNormal == "paciente"
            }
        }

        if (valido) {
            this.rol = rol
            this.userId = uNormal
            this.loggedPersonId = personIdEncontrado ?: 1
            return true
        }

        return false
    }

    /** Genera el nombre de usuario de una persona (2 primeras letras del nombre + apellido en minúsculas y sin tildes). */
    fun generarUsername(nombre: String, apellido: String): String {
        val nom = normalizarTexto(nombre)
        val dosLetras = if (nom.length >= 2) nom.substring(0, 2) else nom
        val ape = normalizarTexto(apellido)
        return (dosLetras + ape).replace(" ", "")
    }

    private fun normalizarTexto(texto: String): String {
        val temp = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "").lowercase()
    }

    fun logout() {
        rol = null
        userId = null
        loggedPersonId = null
    }
}
