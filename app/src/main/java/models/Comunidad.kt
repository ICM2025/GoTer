package models

class Comunidad (val nom: String, val icono: Int, val numMiembros: Int) {
    val nombre: String = nom
    val imagen: Int = icono
    val miembros: Int = numMiembros
}