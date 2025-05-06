package adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gooter_proyecto.databinding.ItemComunidadHomeBinding
import models.Comunidad

class ComunidadHomeAdapter(
    private val lista: List<Comunidad>,
    private val onItemClick: (Comunidad) -> Unit
) : RecyclerView.Adapter<ComunidadHomeAdapter.ComunidadHomeViewHolder>() {

    inner class ComunidadHomeViewHolder(val binding: ItemComunidadHomeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComunidadHomeViewHolder {
        val binding = ItemComunidadHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ComunidadHomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ComunidadHomeViewHolder, position: Int) {
        val comunidad = lista[position]
        holder.binding.imagenComunidad.setImageResource(comunidad.imagen)
        holder.binding.nombreComunidad.text = comunidad.nombre
        holder.binding.miembrosComunidad.text = comunidad.miembros.toString()

        holder.itemView.setOnClickListener {
            onItemClick(comunidad)
        }
    }

    override fun getItemCount(): Int = lista.size
}