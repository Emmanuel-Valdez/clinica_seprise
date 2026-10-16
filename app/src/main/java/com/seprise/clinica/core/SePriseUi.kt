package com.seprise.clinica.core

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.seprise.clinica.R

/**
 * Edge-to-edge SIEMPRE en claro: el diseño no cambia con el tema del sistema.
 * `enableEdgeToEdge()` a secas detecta el modo oscuro del sistema y pondría iconos
 * blancos sobre un fondo claro. Acá fijamos iconos oscuros pase el tema que pase.
 */
fun ComponentActivity.enableSePriseEdgeToEdge() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, getColor(R.color.seprise_ink)),
    )
}
