package models

data class Mensaje(
    val nombre: String,
    val propioMensaje: Boolean,
    val contenido: String, // Texto, ruta de imagen o audio
    val tipo: String,
    val uri: String? = null, // Para almacenar la URI de la imagen o audio
    val timestamp: Long = System.currentTimeMillis()
)
