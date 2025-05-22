package models

data class Canal(
    val id: String,
    val nombre: String,
    val imagen: Int,
    var seguido: Boolean = false,
    var miembros: Int = 0,
    val administrador: String = "",
    val idChat : String
) {

    constructor(nom: String, imagen: Int, seguido: Boolean, numMiembros: Int, idChat: String) :
            this(
                id = "",
                nombre = nom,
                imagen = imagen,
                seguido = seguido,
                miembros = numMiembros,
                idChat = idChat
            )

    constructor(id: String, nom: String, imagen: Int, seguido: Boolean, numMiembros: Int, idChat: String) :
            this(
                id = id,
                nombre = nom,
                imagen = imagen,
                seguido = seguido,
                miembros = numMiembros,
                idChat = idChat
            )

    // Constructor para crear desde datos de Firebase
    constructor(id: String, canalData: Map<String, Any>, idChat: String) : this(
        id = id,
        nombre = canalData["nombreGrupo"] as? String ?: "",
        imagen = 0,
        seguido = false,
        miembros = (canalData["miembros"] as? List<*>)?.size ?: 0,
        administrador = canalData["administrador"] as? String ?: "",
        idChat = idChat
    )

    
}