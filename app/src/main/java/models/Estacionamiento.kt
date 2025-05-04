package com.example.gooter_proyecto.models

data class Estacionamiento(
    val id: String = "",
    val nombre: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val altitud: Double = 0.0,
    val disponible: Boolean = true,
    val capacidad: Int = 0
) {
    constructor() : this("", "", 0.0, 0.0, 0.0, true, 0)
}