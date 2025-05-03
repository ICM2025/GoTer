package adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gooter_proyecto.databinding.ItemComunidadBinding
import models.Comunidad

class ComunidadAdapter(private val comunidades: List<Comunidad>) :
    RecyclerView.Adapter<ComunidadAdapter.ComunidadViewHolder>() {

        //Almacena la vista de cada item en el RecyclerView
    class ComunidadViewHolder(val binding: ItemComunidadBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComunidadViewHolder {
        val binding = ItemComunidadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ComunidadViewHolder(binding)
    }

    //Asigna los datos de una comunidad a la vista
    override fun onBindViewHolder(holder: ComunidadViewHolder, position: Int) {
        val comunidad = comunidades[position]
        holder.binding.NombreGrupo.text = comunidad.nombre
        holder.binding.FotoGrupo.setImageResource(comunidad.imagen)
        holder.binding.MiembrosGrupo.text = comunidad.miembros.toString()
    }

    override fun getItemCount(): Int = comunidades.size
}