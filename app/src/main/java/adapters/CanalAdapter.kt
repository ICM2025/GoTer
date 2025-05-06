package adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gooter_proyecto.databinding.ItemCanalBinding
import models.Canal

class CanalAdapter (private val canales: List<Canal>) :
    RecyclerView.Adapter<CanalAdapter.CanalViewHolder>() {

    inner class CanalViewHolder(val binding: ItemCanalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CanalViewHolder {
        val binding = ItemCanalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CanalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CanalViewHolder, position: Int) {
        val canal = canales[position]
        holder.binding.NombreCanal.text = canal.nombre
        holder.binding.FotoCanal.setImageResource(canal.imagen)
        holder.binding.MiembrosCanal.text = canal.miembros.toString()

        // Configurar el botón según el estado actual
        holder.binding.ButtonSeguir.text = if (canal.seguido) "Siguiendo" else "Seguir"

        // Cambiar estado de seguimiento al hacer clic
        holder.binding.ButtonSeguir.setOnClickListener {
            canal.seguido = !canal.seguido
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = canales.size
}