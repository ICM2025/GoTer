package adapters

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gooter_proyecto.databinding.ItemMensajeBinding
import models.Mensaje
import models.TipoMensaje



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
                    binding.textViewMensaje.visibility = View.VISIBLE
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
                    binding.textViewMensaje.visibility = View.GONE
                    binding.imageViewImagen.visibility = View.GONE

                    binding.audioPlayer.setOnClickListener {
                        val uriAudio = mensaje.uri
                        if (uriAudio != null) {
                            try {
                                val mediaPlayer = MediaPlayer()
                                val context = binding.root.context

                                val audioUri = Uri.parse(uriAudio)
                                Log.d("REPRODUCCION", "URI del audio: $uriAudio")
                                mediaPlayer.setDataSource(context, audioUri)  // CORRECTO PARA URI
                                mediaPlayer.prepare()
                                mediaPlayer.start()

                                Toast.makeText(context, "Reproduciendo audio...", Toast.LENGTH_SHORT).show()
                                binding.audioPlayer.setText("Reproduccion")

                            } catch (e: Exception) {
                                Toast.makeText(binding.root.context, "Error al reproducir audio", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                            }
                        } else {
                            Toast.makeText(binding.root.context, "No se encontró el archivo de audio", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

