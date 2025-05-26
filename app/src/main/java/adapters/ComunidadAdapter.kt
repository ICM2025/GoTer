// ComunidadAdapter.kt
package adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gooter_proyecto.R
import com.example.gooter_proyecto.databinding.ItemComunidadBinding
import models.Comunidad

class ComunidadAdapter(
    private val comunidades: List<Comunidad>,
    private val onItemClick: (Comunidad) -> Unit
) : RecyclerView.Adapter<ComunidadAdapter.ComunidadViewHolder>() {

    class ComunidadViewHolder(val binding: ItemComunidadBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComunidadViewHolder {
        val binding = ItemComunidadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ComunidadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ComunidadViewHolder, position: Int) {
        val comunidad = comunidades[position]
        holder.binding.NombreGrupo.text = comunidad.nombre
        holder.binding.MiembrosGrupo.text = comunidad.miembros.toString()

        // Cargar imagen con Glide
        if (comunidad.imagen.isNotEmpty()) {
            Glide.with(holder.itemView)
                .load(comunidad.imagen)
                .circleCrop()
                .placeholder(R.drawable.background_username)
                .into(holder.binding.FotoGrupo)
        } else {
            holder.binding.FotoGrupo.setImageResource(R.drawable.background_username)
        }

        holder.itemView.setOnClickListener { onItemClick(comunidad) }
    }

    override fun getItemCount(): Int = comunidades.size
}
