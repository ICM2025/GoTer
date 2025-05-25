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