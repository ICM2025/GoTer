package models

class Canal (nom: String, imagen: Int, seguido: Boolean, numMiembros: Int) {
    val nombre: String = nom
    val imagen: Int = imagen
    var seguido: Boolean = seguido
    var miembros: Int = numMiembros
}