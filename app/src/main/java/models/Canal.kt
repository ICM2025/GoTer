package models

data class Canal(
    val id: String,
    val nombre: String,
    val imagen: Int,
    var seguido: Boolean = false,
    var miembros: Int = 0,
    val administrador: String = ""
) {

    constructor(nom: String, imagen: Int, seguido: Boolean, numMiembros: Int) :
            this(
                id = "",
                nombre = nom,
                imagen = imagen,
                seguido = seguido,
                miembros = numMiembros
            )

    // Constructor para crear desde datos de Firebase
    constructor(id: String, canalData: Map<String, Any>) : this(
        id = id,
        nombre = canalData["nombreGrupo"] as? String ?: "",
        imagen = 0,
        seguido = false,
        miembros = (canalData["miembros"] as? List<*>)?.size ?: 0,
        administrador = canalData["administrador"] as? String ?: ""
    )

    
}