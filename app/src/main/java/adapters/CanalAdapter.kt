package adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        var database = FirebaseDatabase.getInstance().reference
        val canal = canales[position]
        holder.binding.NombreCanal.text = canal.nombre
        holder.binding.FotoCanal.setImageResource(canal.imagen)
        holder.binding.MiembrosCanal.text = canal.miembros.toString()
        var refPush: DatabaseReference? = null
        // Configurar el botón según el estado actual
        val usuarioActual = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.i("CanalAdapter", "Usuario Actual " + usuarioActual)

        // Cambiar estado de seguimiento al hacer clic
        holder.binding.ButtonSeguir.setOnClickListener {
            verificarPertenenciaACanal(usuarioActual, canal.id) { pertenece ->
                if (pertenece) {
                    Log.i("CanalAdapter", "El usuario actual va a dejar de seguir el canal: " + canal.nombre)
                    canal.seguido = false
                    holder.binding.ButtonSeguir.text = "Seguir"
                    cambiarEstadoSeguimiento(canal, usuarioActual)
                } else {
                    Log.i("CanalAdapter", "El usuario actual empezara a seguir el canal: " + canal.nombre)
                    canal.seguido = true
                    holder.binding.ButtonSeguir.text = "Siguiendo"
                    cambiarEstadoSeguimiento(canal, usuarioActual)
                }
            }
        }

        verificarPertenenciaACanal(usuarioActual, canal.id) { pertenece ->
            if (pertenece) {
                canal.seguido = true
                Log.i("CanalAdapter", "El usuario actual ya sigue el canal: " + canal.nombre)
                holder.binding.ButtonSeguir.text = "Siguiendo"
            } else {
                canal.seguido = false
                Log.i("CanalAdapter", "El usuario actual NO sigue el canal: " + canal.nombre)
                holder.binding.ButtonSeguir.text = "Seguir"
            }
        }

        holder.binding.root.setOnClickListener {
            onClick(canal)
        }
    }

    fun cambiarEstadoSeguimiento(canal: Canal, usuarioActual: String) {
        val database = FirebaseDatabase.getInstance().reference
        val participantesRef = database.child("canal").child(canal.id).child("participantes")
        val refUsuarios = database.child("usuarios")
        val query = refUsuarios.orderByKey().equalTo(usuarioActual)

        if (canal.seguido) {
            // Agregar usuario
            query.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        Log.i("CanalAdapter", "Usuario encontrado: ${userSnapshot.key}")
                        participantesRef.push().setValue(userSnapshot.key)
                    }
                }
            }
        } else {
            // Eliminar usuario
            participantesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (participanteSnap in snapshot.children) {
                        val userIdInList = participanteSnap.getValue(String::class.java)
                        if (userIdInList == usuarioActual) {
                            participanteSnap.ref.removeValue()
                            Log.i("CanalAdapter", "Usuario eliminado del canal ${canal.nombre}")
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error al intentar eliminar: ${error.message}")
                }
            })
        }
    }

    override fun getItemCount(): Int = canales.size

    fun verificarPertenenciaACanal(
        usuarioId: String,
        canalId: String,
        callback: (Boolean) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance().reference
        val participantesRef = database.child("canal").child(canalId).child("participantes")

        participantesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pertenece =
                    snapshot.children.any { it.getValue(String::class.java) == usuarioId }
                callback(pertenece)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer datos: ${error.message}")
                callback(false)
            }
        })
    }
}