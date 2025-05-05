package models

data class Mensaje(
    val contenido: String, // Texto, ruta de imagen o audio
    val tipo: TipoMensaje,
    val uri: String? = null, // Para almacenar la URI de la imagen o audio
    val timestamp: Long = System.currentTimeMillis()
)

enum class TipoMensaje {
    TEXTO, IMAGEN, AUDIO
}