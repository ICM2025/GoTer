package models
data class Mensaje(
    var nombre: String? = null,
    var propioMensaje: Boolean = false,
    var contenido: String? = null,
    var tipo: String? = null,
    var uri: String? = null,
    var timestamp: Long = 0
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", false, "", "", null, 0)
}