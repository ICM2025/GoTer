package com.example.gooter_proyecto

import adapters.ChatAdapter
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityChatBinding
import models.Mensaje
import models.TipoMensaje
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val mensajes = mutableListOf<Mensaje>()
    private lateinit var adapter: ChatAdapter
    private lateinit var uriImagen: Uri

    // Lanzador para seleccionar imagen de galería
    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback { uri ->
            uri?.let {
                mensajes.add(Mensaje("Imagen seleccionada", TipoMensaje.IMAGEN, uri = it.toString()))
                adapter.notifyItemInserted(mensajes.size - 1)
            }
        }
    )

    // Lanzador para tomar foto
    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback { resultado ->
            if (resultado) {
                mensajes.add(Mensaje("Foto tomada", TipoMensaje.IMAGEN, uri = uriImagen.toString()))
                adapter.notifyItemInserted(mensajes.size - 1)
            }
        }
    )

    // Lanzador para grabar audio
    private val audioLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback { resultado ->
            if (resultado.resultCode == Activity.RESULT_OK) {
                val audioUri: Uri? = resultado.data?.data
                audioUri?.let {
                    mensajes.add(Mensaje("Audio grabado", TipoMensaje.AUDIO, uri = it.toString()))
                    adapter.notifyItemInserted(mensajes.size - 1)
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(mensajes)
        binding.listaMensajes.layoutManager = LinearLayoutManager(this)
        binding.listaMensajes.adapter = adapter

        // Enviar texto
        binding.btnEnviar.setOnClickListener {
            val texto = binding.editTextMensaje.text.toString()
            if (texto.isNotEmpty()) {
                mensajes.add(Mensaje(texto, TipoMensaje.TEXTO))
                adapter.notifyItemInserted(mensajes.size - 1)
                binding.editTextMensaje.text.clear()
            }
        }

        // Seleccionar imagen de la galería
        binding.btnCargarImagen.setOnClickListener {
            galeriaLauncher.launch("image/*")
        }

        // Tomar foto con la cámara
        binding.btnTomarFoto.setOnClickListener {
            val archivo = crearArchivoImagen()
            uriImagen = FileProvider.getUriForFile(baseContext, "${packageName}.fileprovider", archivo)
            camaraLauncher.launch(uriImagen)
        }

        // Grabar audio
        binding.btnGrabarAudio.setOnClickListener {
            //Abre app de grabacion de audio, usuario finaliza y se guarda en Uri
            val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
            audioLauncher.launch(intent)
        }
    }

    private fun crearArchivoImagen(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${timestamp}_", ".jpg", directorio)
    }
}