package models

data class Notificacion(
    val idNotificacion: String,
    val titulo: String,
    val descripcion: String,
    val remitente: String,
    val destinatario: String,
    val fecha: String,
    val leida: Boolean,
    val accion: String, // Acción a realizar ("ver_carrera", "ver_mensajes", "responder_invitacion")
    val tipo: String,   // Tipo de notificación ("Carrera", "Mensaje", "Invitación")
    val metadatos: String // Metadatos como String JSON
)