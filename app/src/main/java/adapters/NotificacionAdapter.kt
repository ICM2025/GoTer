package adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gooter_proyecto.databinding.ItemNotificacionBinding
import models.Notificacion

class NotificacionAdapter(private var lista: List<Notificacion>) :
    RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder>() {

    inner class NotificacionViewHolder(val binding: ItemNotificacionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notificacion: Notificacion) {
            binding.tituloNotificacion.text = notificacion.titulo
            binding.descripcionNotificacion.text = notificacion.descripcion

            binding.btnGo.setOnClickListener {
                // TODO: Manejar el clic en el botón "GO"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val binding = ItemNotificacionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
