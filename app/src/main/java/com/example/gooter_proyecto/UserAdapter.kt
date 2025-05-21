package com.example.gooter_proyecto

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.core.snap
import com.google.firebase.database.*
import models.Usuario

class UserAdapter(
    var contexto: Context,
    var recurso: Int,
    var listadoUsuarios: List<Usuario>,
    var comunidadId: String
) : ArrayAdapter<Usuario>(contexto, recurso, listadoUsuarios) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater: LayoutInflater = LayoutInflater.from(contexto)
        val view = layoutInflater.inflate(recurso, parent, false)

        val nombreUsuario = view.findViewById<TextView>(R.id.nombreUsuario)
        val botonAgregar = view.findViewById<Button>(R.id.btnAgregar)

        val usuario = listadoUsuarios[position]
        nombreUsuario.text = "${usuario.nombre} ${usuario.apellidos}"

        var databaseRef = FirebaseDatabase.getInstance().reference

        botonAgregar.setOnClickListener {
            val participantes =
                databaseRef.child("comunidad").child(comunidadId).child("participantes")
            val miembros =
                databaseRef.child("comunidad").child(comunidadId).child("miembros")
            Log.i("miembros", miembros.toString())
            Log.i("communidad_id desde el adapter:", comunidadId)
            val refUsuarios = databaseRef.child("usuarios")
            val query = refUsuarios.orderByChild("correo").equalTo(usuario.correo)
            Log.i("Correo del usuario buscado: ", usuario.correo.toString())
            val uniqueId = participantes.push()
            query.get().addOnSuccessListener { snapshot ->
                if(snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        uniqueId.setValue(userSnapshot.key)
                    }
                }
            }
        }

        return view
    }
}
