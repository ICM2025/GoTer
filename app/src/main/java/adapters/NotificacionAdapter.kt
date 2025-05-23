package adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gooter_proyecto.databinding.ItemNotificacionBinding
import models.Notificacion
import com.example.gooter_proyecto.CarreraActivity
import com.example.gooter_proyecto.ChatActivity
import com.example.gooter_proyecto.MapsActivity
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
            }
        }

        private fun handleNotificationAction(notificacion: Notificacion) {
            try {
                val metadatos = JSONObject(notificacion.metadatos)

                when (notificacion.accion) {
                    "ver_carrera" -> {
                        val carreraId = metadatos.optString("carrera_id", "")
                        val intent = Intent(context, MapsActivity::class.java).apply {
                            putExtra("carrera_id", carreraId)
                        }
                        context.startActivity(intent)
                    }

                    "ver_mensajes" -> {
                        val grupoId = metadatos.optString("grupo_id", "")
                        val intent = Intent(context, ChatActivity::class.java).apply {
                            putExtra("grupo_id", grupoId)
                            metadatos.optString("mensaje_id").takeIf { it.isNotEmpty() }?.let {
                                putExtra("mensaje_id", it)
                            }
                        }
                        context.startActivity(intent)
                    }

                    "responder_invitacion" -> {
                        val comunidadId = metadatos.optString("comunidad_id", "")
                        val intent = Intent(context, ChatActivity::class.java).apply {
                            putExtra("comunidad_id", comunidadId)
                        }
                        context.startActivity(intent)
                    }

                    else -> {
                        // Acción por defecto o notificaciones sin acción específica
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