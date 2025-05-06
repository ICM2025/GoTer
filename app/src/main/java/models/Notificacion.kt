package models

data class Notificacion(
    val titulo: String,
    val descripcion: String,
    val remitente: String,
    val destinatario: String,
    val fecha: String
) {
    // Constructor secundario que solo recibe título y descripción
    constructor(titulo: String, descripcion: String) : this(
        titulo = titulo,
        descripcion = descripcion,
        remitente = "Desconocido",   // Valor predeterminado
        destinatario = "Desconocido", // Valor predeterminado
        fecha = "Desconocida"         // Valor predeterminado
    )
}