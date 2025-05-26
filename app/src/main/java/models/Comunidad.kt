package models

data class Comunidad(
    val id: String,
    val nombre: String,
    val imagen: String,           // ← Cambiado a String
    val miembros: Int,
    val participantes: List<String>,
    val idChat: String
) {
    constructor(
        nombre: String,
        imagen: String,
        miembros: Int,
        participantes: List<String>,
        idChat: String
    ) : this("", nombre, imagen, miembros, participantes, idChat)
}
