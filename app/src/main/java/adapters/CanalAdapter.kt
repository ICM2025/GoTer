// CanalAdapter.kt
package adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gooter_proyecto.R
import com.example.gooter_proyecto.databinding.ItemCanalBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import models.Canal

class CanalAdapter(
    private val canales: List<Canal>,
    private val onClick: (Canal) -> Unit,
) : RecyclerView.Adapter<CanalAdapter.CanalViewHolder>() {

    inner class CanalViewHolder(val binding: ItemCanalBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CanalViewHolder {
        val binding = ItemCanalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CanalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CanalViewHolder, position: Int) {
        val canal = canales[position]
        holder.binding.NombreCanal.text = canal.nombre
        holder.binding.MiembrosCanal.text = canal.miembros.toString()

        // Cargar imagen con Glide
        if (canal.imagen.isNotEmpty()) {
            Glide.with(holder.itemView)
                .load(canal.imagen)
                .circleCrop()
                .placeholder(R.drawable.background_username)
                .into(holder.binding.FotoCanal)
        } else {
            holder.binding.FotoCanal.setImageResource(R.drawable.background_username)
        }

        // Estado de seguimiento inicial
        val usuarioActual = FirebaseAuth.getInstance().currentUser?.uid ?: return
        verificarPertenenciaACanal(usuarioActual, canal.id) { pertenece ->
            canal.seguido = pertenece
            holder.binding.ButtonSeguir.text = if (pertenece) "Siguiendo" else "Seguir"
        }

        // Click en seguir
        holder.binding.ButtonSeguir.setOnClickListener {
            canal.seguido = !canal.seguido
            holder.binding.ButtonSeguir.text = if (canal.seguido) "Siguiendo" else "Seguir"
            cambiarEstadoSeguimiento(canal, usuarioActual)
        }

        // Click en item
        holder.binding.root.setOnClickListener { onClick(canal) }
    }

    override fun getItemCount(): Int = canales.size

    private fun cambiarEstadoSeguimiento(canal: Canal, usuarioActual: String) {
        val database = FirebaseDatabase.getInstance().reference
        val participantesRef = database.child("canal").child(canal.id).child("participantes")
        if (canal.seguido) {
            participantesRef.push().setValue(usuarioActual)
        } else {
            participantesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children) {
                        if (snap.getValue(String::class.java) == usuarioActual) {
                            snap.ref.removeValue()
                            break
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun verificarPertenenciaACanal(
        usuarioId: String,
        canalId: String,
        callback: (Boolean) -> Unit
    ) {
        val participantesRef = FirebaseDatabase.getInstance().reference
            .child("canal").child(canalId).child("participantes")
        participantesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pertenece = snapshot.children
                    .any { it.getValue(String::class.java) == usuarioId }
                callback(pertenece)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("CanalAdapter", "Error al leer: ${error.message}")
                callback(false)
            }
        })
    }
}
