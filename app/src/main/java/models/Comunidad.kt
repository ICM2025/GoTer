package models

data class Comunidad(
    val id: String,                   // ID de la comunidad en Firebase
    val nombre: String,               // Nombre del grupo
    val imagen: Int,                  // Recurso de imagen
    val miembros: Int,                // Número de miembros
    val participantes: List<String>,   // Lista de IDs de participantes
    val idChat : String
) {
    // Constructor secundario simplificado para compatibilidad
    constructor(nombre: String, imagen: Int, miembros: Int, participantes: List<String>, idChat: String) :
            this("", nombre, imagen, miembros, participantes, idChat)
}