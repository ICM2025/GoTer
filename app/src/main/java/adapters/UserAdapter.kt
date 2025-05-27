package adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import com.example.gooter_proyecto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import models.Usuario
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserAdapter(
    private val contexto: Context,
    private val recurso: Int,
    private val listadoUsuarios: List<Usuario>,
    private val comunidadId: String
) : ArrayAdapter<Usuario>(contexto, recurso, listadoUsuarios) {

    // Cache de estado real por correo
    private val userStateCache = mutableMapOf<String, String>()
    private val databaseRef = FirebaseDatabase.getInstance().reference

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(contexto).inflate(recurso, parent, false)
            viewHolder = ViewHolder(
                view.findViewById(R.id.nombreUsuario),
                view.findViewById(R.id.btnAgregar)
            )
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val usuario = listadoUsuarios[position]
        // Asegurar non-null correo
        val correo = usuario.correo ?: return view

        viewHolder.nombre.text = "${usuario.nombre} ${usuario.apellidos}"

        // Aplicar estado cacheado o estado temporal
        val estadoCache = userStateCache[correo]
        if (estadoCache != null) {
            configurarBoton(viewHolder.boton, estadoCache)
        } else {
            configurarBotonTemporal(viewHolder.boton)
            verificarEstadoEnFirebase(correo, viewHolder.boton)
        }

        viewHolder.boton.setOnClickListener {
            val estadoActual = userStateCache[correo] ?: return@setOnClickListener
            if (estadoActual == "agregar") {
                // Actualizar cache y UI inmediatamente
                userStateCache[correo] = "enviado"
                configurarBoton(viewHolder.boton, "enviado")
                agregarUsuarioAComunidad(correo)
            }
        }

        return view
    }

    private fun verificarEstadoEnFirebase(correo: String, boton: Button) {
        // Obtener userId por correo
        databaseRef.child("usuarios")
            .orderByChild("correo").equalTo(correo)
            .get().addOnSuccessListener { snapUsuarios ->
                if (!snapUsuarios.exists()) return@addOnSuccessListener
                val userId = snapUsuarios.children.first().key ?: return@addOnSuccessListener

                // Verificar en participantes
                databaseRef.child("comunidad")
                    .child(comunidadId)
                    .child("participantes")
                    .orderByValue().equalTo(userId)
                    .get().addOnSuccessListener { snapPart ->
                        val estado = if (snapPart.exists()) "agregado" else "agregar"
                        userStateCache[correo] = estado
                        configurarBoton(boton, estado)
                    }
            }
    }

    private fun agregarUsuarioAComunidad(correo: String) {
        // Obtener userId por correo
        databaseRef.child("usuarios")
            .orderByChild("correo").equalTo(correo)
            .get().addOnSuccessListener { snap ->
                if (!snap.exists()) return@addOnSuccessListener
                val userId = snap.children.first().key ?: return@addOnSuccessListener

                // Agregar a participantes
                val participantesRef = databaseRef.child("comunidad")
                    .child(comunidadId)
                    .child("participantes")
                participantesRef.push().setValue(userId)

                // Actualizar contador de miembros
                val miembrosRef = databaseRef.child("comunidad")
                    .child(comunidadId)
                    .child("miembros")
                miembrosRef.get().addOnSuccessListener { countSnap ->
                    val actuales = countSnap.getValue(Int::class.java) ?: 0
                    miembrosRef.setValue(actuales + 1)
                }

                // Enviar notificación
                enviarNotificacion(userId)
            }
    }

    private fun enviarNotificacion(destinatarioId: String) {
        val fechaHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val uidEmisor = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Obtener nombre del emisor
        databaseRef.child("usuarios").child(uidEmisor)
            .get().addOnSuccessListener { snapEmisor ->
                val nombreEmisor = snapEmisor.child("nombre").getValue(String::class.java) ?: ""

                // Obtener datos de la comunidad
                databaseRef.child("comunidad").child(comunidadId)
                    .get().addOnSuccessListener { snapComunidad ->
                        val nombreGrupo = snapComunidad.child("nombreGrupo").value.toString()
                        val chatId = snapComunidad.child("chatId").value.toString()
                        val notificacionId = databaseRef.child("notificaciones").push().key

                        if (notificacionId != null) {
                            val metadatos = JSONObject().apply {
                                put("nombreGrupo", nombreGrupo)
                                put("chatId", chatId)
                                put("comunidadId", comunidadId)
                            }.toString()

                            val notificacion = mapOf(
                                "idNotificacion" to notificacionId,
                                "emisorId" to uidEmisor,
                                "destinatarioId" to destinatarioId,
                                "fechaHora" to fechaHora,
                                "leida" to false,
                                "tipo" to nombreGrupo,
                                "mensaje" to "$nombreEmisor te ha agregado a la comunidad $nombreGrupo",
                                "accion" to "launch_comunidad",
                                "metadatos" to metadatos
                            )

                            databaseRef.child("notificaciones")
                                .child(notificacionId)
                                .setValue(notificacion)
                        }
                    }
            }
    }

    private fun configurarBoton(boton: Button, estado: String) {
        when (estado) {
            "agregar" -> {
                boton.text = "AGREGAR"
                boton.setTextColor(Color.WHITE)
                boton.setBackgroundColor(Color.parseColor("#2196F3"))
                boton.isEnabled = true
            }
            "enviado" -> {
                boton.text = "ENVIADO"
                boton.setTextColor(Color.BLACK)
                boton.setBackgroundColor(Color.parseColor("#FFA500"))
                boton.isEnabled = false
            }
            "agregado" -> {
                boton.text = "AGREGADO"
                boton.setTextColor(Color.WHITE)
                boton.setBackgroundColor(Color.parseColor("#4CAF50"))
                boton.isEnabled = false
            }
        }
    }

    private fun configurarBotonTemporal(boton: Button) {
        boton.text = "VERIFICANDO…"
        boton.setTextColor(Color.WHITE)
        boton.setBackgroundColor(Color.GRAY)
        boton.isEnabled = false
    }

    private data class ViewHolder(
        val nombre: TextView,
        val boton: Button
    )
}
