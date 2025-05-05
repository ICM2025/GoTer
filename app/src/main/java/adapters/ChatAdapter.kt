package adapters

import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gooter_proyecto.databinding.ItemMensajeBinding
import models.Mensaje
import models.TipoMensaje
import java.io.IOException

class ChatAdapter(private val mensajes: List<Mensaje>) : RecyclerView.Adapter<ChatAdapter.MensajeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensajeViewHolder {
        val binding = ItemMensajeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MensajeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MensajeViewHolder, position: Int) {
        val mensaje = mensajes[position]
        holder.bind(mensaje)
    }

    override fun getItemCount(): Int = mensajes.size

    class MensajeViewHolder(private val binding: ItemMensajeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mensaje: Mensaje) {
            when (mensaje.tipo) {
                TipoMensaje.TEXTO -> {
                    binding.textViewMensaje.text = mensaje.contenido
                    binding.imageViewImagen.visibility = View.GONE
                    binding.audioPlayer.visibility = View.GONE
                }
                TipoMensaje.IMAGEN -> {
                    binding.imageViewImagen.visibility = View.VISIBLE
                    Glide.with(binding.root.context).load(mensaje.uri).into(binding.imageViewImagen)
                    binding.textViewMensaje.visibility = View.GONE
                    binding.audioPlayer.visibility = View.GONE
                }
                TipoMensaje.AUDIO -> {
                    binding.audioPlayer.visibility = View.VISIBLE
                    // Aquí podrías agregar un reproductor de audio
                    binding.textViewMensaje.visibility = View.GONE
                    binding.imageViewImagen.visibility = View.GONE
                }
            }
        }
    }
}

