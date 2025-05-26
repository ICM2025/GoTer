// ComunidadHomeAdapter.kt
package adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gooter_proyecto.R
import com.example.gooter_proyecto.databinding.ItemComunidadHomeBinding
import models.Comunidad

class ComunidadHomeAdapter(
    var lista: List<Comunidad>,
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
        holder.binding.nombreComunidad.text = comunidad.nombre
        holder.binding.miembrosComunidad.text = comunidad.miembros.toString()

        // Cargar imagen con Glide
        if (comunidad.imagen.isNotEmpty()) {
            Glide.with(holder.itemView)
                .load(comunidad.imagen)
                .centerCrop()
                .placeholder(R.drawable.background_username)
                .into(holder.binding.imagenComunidad)
        } else {
            holder.binding.imagenComunidad.setImageResource(R.drawable.background_username)
        }

        holder.itemView.setOnClickListener { onItemClick(comunidad) }
    }

    override fun getItemCount(): Int = lista.size
}