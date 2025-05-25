package adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gooter_proyecto.databinding.ItemNotificacionBinding
import models.Notificacion
import com.example.gooter_proyecto.CarreraActivity
import com.example.gooter_proyecto.ChatActivity
import com.example.gooter_proyecto.MapsActivity
import com.example.gooter_proyecto.PerfilUsuarioActivity
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

class NotificacionAdapter(
    private var lista: List<Notificacion>,
    private val context: Context
) : RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder>() {

    inner class NotificacionViewHolder(val binding: ItemNotificacionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notificacion: Notificacion) {
            binding.tituloNotificacion.text = notificacion.titulo
            binding.descripcionNotificacion.text = notificacion.descripcion

            binding.btnGo.setOnClickListener {
                handleNotificationAction(notificacion)
                eliminarNotificacion(notificacion.idNotificacion)
            }
        }

        private fun eliminarNotificacion(idNotificacion: String) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            database.reference.child("notificaciones").child(idNotificacion)
                .removeValue()
                .addOnSuccessListener {
                    Log.i("NOTI", "Notificacion borrada con éxito")
                }
                .addOnFailureListener { e ->
                    Log.e("NOTI", "Error al eliminar la notificación: ${e.message}")
                }
        }

        private fun handleNotificationAction(notificacion: Notificacion) {
            try {
                val metadatos = JSONObject(notificacion.metadatos)

                when (notificacion.accion) {

                    "launch_comunidad" -> {
                        val intent = Intent(context, ChatActivity::class.java)
                        intent.putExtra("nombreGrupo", metadatos.getString("nombreGrupo"))
                        intent.putExtra("chatId", metadatos.getString("chatId"))
                        intent.putExtra("comunidadId", metadatos.getString("comunidadId"))
                        context.startActivity(intent)
                    }

                    "launch_canal" -> {
                        val intent = Intent(context, ChatActivity::class.java)
                        intent.putExtra("nombreGrupo", metadatos.getString("nombreGrupo"))
                        intent.putExtra("chatId", metadatos.getString("chatId"))
                        intent.putExtra("canalId", metadatos.getString("canalId"))
                        context.startActivity(intent)
                    }

                    "solicitud_usuario" -> {
                        val comunidadId = metadatos.getString("comunidadId")
                        val usuarioId = metadatos.getString("usuarioId")
                        val database = com.google.firebase.database.FirebaseDatabase.getInstance()

                        val comunidadRef = database.reference.child("comunidad").child(comunidadId)

                        comunidadRef.get().addOnSuccessListener { comunidadSnapshot ->
                            val nombreGrupo = comunidadSnapshot.child("nombreGrupo").getValue(String::class.java) ?: "Comunidad"
                            val chatId = comunidadSnapshot.child("chatId").getValue(String::class.java) ?: ""

                            // Agregar al usuario a la comunidad
                            val participantesRef = comunidadRef.child("participantes")
                            participantesRef.push().setValue(usuarioId)
                                .addOnSuccessListener {
                                    // Actualizar el número de miembros
                                    val miembrosRef = comunidadRef.child("miembros")
                                    miembrosRef.get().addOnSuccessListener { snapshot ->
                                        val miembrosActuales = snapshot.getValue(Int::class.java) ?: 0
                                        miembrosRef.setValue(miembrosActuales + 1)
                                    }

                                    // Crear notificación para el usuario que solicitó unirse
                                    val notiId = database.reference.child("notificaciones").push().key ?: return@addOnSuccessListener
                                    val fechaHora = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

                                    val notiMetadatos = mapOf(
                                        "comunidadId" to comunidadId,
                                        "chatId" to chatId,
                                        "nombreGrupo" to nombreGrupo
                                    )

                                    val notificacionAceptado = mapOf(
                                        "idNotificacion" to notiId,
                                        "emisorId" to FirebaseAuth.getInstance().currentUser?.uid,
                                        "destinatarioId" to usuarioId,
                                        "fechaHora" to fechaHora,
                                        "leida" to false,
                                        "tipo" to nombreGrupo,
                                        "mensaje" to "¡Has sido agregado a la comunidad $nombreGrupo!",
                                        "accion" to "launch_comunidad",
                                        "metadatos" to JSONObject(notiMetadatos).toString()
                                    )

                                    database.reference.child("notificaciones").child(notiId).setValue(notificacionAceptado)
                                        .addOnSuccessListener {
                                            Log.i("NOTI", "Notificación de aceptación enviada a $usuarioId")
                                        }
                                        .addOnFailureListener {
                                            Log.e("NOTI", "Error al enviar notificación de aceptación: ${it.message}")
                                        }

                                    android.widget.Toast.makeText(
                                        context,
                                        "Usuario agregado y notificado",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Log.e("NOTI", "Error al agregar usuario a comunidad: ${it.message}")
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error al agregar usuario",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }



                    else -> {
                        Log.i("NOTI", "Notificacion leída")
                    }
                }
            } catch (e: Exception) {
                // Handle JSON parsing error
                e.printStackTrace()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val binding = ItemNotificacionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificacionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    fun updateList(nuevaLista: List<Notificacion>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}