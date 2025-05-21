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
            if (!mensaje.propioMensaje) {
                // Show sender, hide receiver
                binding.senderLayout.visibility = View.VISIBLE
                binding.receiverLayout.visibility = View.GONE

                binding.textSenderName.text = mensaje.nombre ?: "You"

                when (mensaje.tipo) {
                    "TEXTO" -> {
                        binding.textSenderMessage.text = mensaje.contenido
                        binding.textSenderMessage.visibility = View.VISIBLE
                        binding.imageSender.visibility = View.GONE
                        binding.audioSender.visibility = View.GONE
                    }
                    "IMAGEN" -> {
                        binding.imageSender.visibility = View.VISIBLE
                        Glide.with(binding.root.context).load(mensaje.uri).into(binding.imageSender)
                        binding.textSenderMessage.visibility = View.GONE
                        binding.audioSender.visibility = View.GONE
                    }
                    "AUDIO" -> {
                        binding.audioSender.visibility = View.VISIBLE
                        binding.textSenderMessage.visibility = View.GONE
                        binding.imageSender.visibility = View.GONE

                        binding.audioSender.setOnClickListener {
                            val uriAudio = mensaje.uri
                            if (uriAudio != null) {
                                try {
                                    val mediaPlayer = MediaPlayer()
                                    val context = binding.root.context
                                    val audioUri = Uri.parse(uriAudio)
                                    mediaPlayer.setDataSource(context, audioUri)
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()

                                    Toast.makeText(context, "Reproduciendo audio...", Toast.LENGTH_SHORT).show()
                                    binding.audioSender.text = "Reproduciendo"

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
            } else {
                // Show receiver, hide sender
                binding.senderLayout.visibility = View.GONE
                binding.receiverLayout.visibility = View.VISIBLE

                binding.textReceiverName.text = mensaje.nombre ?: "Other"

                when (mensaje.tipo) {
                    "TEXTO" -> {
                        binding.textReceiverMessage.text = mensaje.contenido
                        binding.textReceiverMessage.visibility = View.VISIBLE
                        binding.imageReceiver.visibility = View.GONE
                        binding.audioReceiver.visibility = View.GONE
                    }
                    "IMAGEN" -> {
                        binding.imageReceiver.visibility = View.VISIBLE
                        Glide.with(binding.root.context).load(mensaje.uri).into(binding.imageReceiver)
                        binding.textReceiverMessage.visibility = View.GONE
                        binding.audioReceiver.visibility = View.GONE
                    }
                    "AUDIO" -> {
                        binding.audioReceiver.visibility = View.VISIBLE
                        binding.textReceiverMessage.visibility = View.GONE
                        binding.imageReceiver.visibility = View.GONE

                        binding.audioReceiver.setOnClickListener {
                            val uriAudio = mensaje.uri
                            if (uriAudio != null) {
                                try {
                                    val mediaPlayer = MediaPlayer()
                                    val context = binding.root.context
                                    val audioUri = Uri.parse(uriAudio)
                                    mediaPlayer.setDataSource(context, audioUri)
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()

                                    Toast.makeText(context, "Reproduciendo audio...", Toast.LENGTH_SHORT).show()
                                    binding.audioReceiver.text = "Reproduciendo"

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
}

