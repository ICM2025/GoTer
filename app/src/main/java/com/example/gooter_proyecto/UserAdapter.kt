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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import models.Usuario
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        uniqueId.setValue(userSnapshot.key)
                        enviarNotificacion(userSnapshot.key.toString())
                    }
                }
            }
            miembros.get().addOnSuccessListener { snapshot ->
                val miembrosActuales = snapshot.getValue(Int::class.java) ?: 0
                miembros.setValue(miembrosActuales + 1)
            }
        }

        return view
    }

    private fun enviarNotificacion(usuarioId: String) {
        val fechaHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val database = FirebaseDatabase.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.reference.child("usuarios").child(uid).get()
            .addOnSuccessListener { snapshotEmisor ->
                val nombre = snapshotEmisor.child("nombre").getValue(String::class.java) ?: ""

                database.reference.child("usuarios").child(usuarioId).child("nombre").get()
                    .addOnSuccessListener {
                        val notificacionId = database.reference.child("notificaciones").push().key

                        if (notificacionId != null) {
                            val comunidadQuery = database.reference.child("comunidad").orderByKey().equalTo(comunidadId)
                            comunidadQuery.get().addOnSuccessListener { snapshotComunidad ->
                                if (snapshotComunidad.exists()) {
                                    for (comunidadSnapshot in snapshotComunidad.children) {
                                        val nombreGrupo = comunidadSnapshot.child("nombreGrupo").value.toString()
                                        val chatId = comunidadSnapshot.child("chatId").value.toString()

                                        val metadatos = mapOf(
                                            "nombreGrupo" to nombreGrupo,
                                            "chatId" to chatId,
                                            "comunidadId" to comunidadId
                                        )

                                        val notificacion = mapOf(
                                            "idNotificacion" to notificacionId,
                                            "emisorId" to uid,
                                            "destinatarioId" to usuarioId,
                                            "fechaHora" to fechaHora,
                                            "leida" to false,
                                            "tipo" to nombreGrupo,
                                            "mensaje" to "$nombre te ha agregado a la comunidad $nombreGrupo",
                                            "accion" to "launch_comunidad",
                                            "metadatos" to JSONObject(metadatos).toString()
                                        )

                                        database.reference.child("notificaciones").child(notificacionId)
                                            .setValue(notificacion)
                                            .addOnSuccessListener {
                                                Log.i("NOTI", "Notificación de invitación enviada")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("NOTI", "Error al enviar notificación: ${e.message}")
                                            }
                                    }
                                }
                            }
                        }
                    }
            }
    }


}
