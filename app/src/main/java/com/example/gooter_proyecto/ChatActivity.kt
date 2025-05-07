package com.example.gooter_proyecto

import adapters.ChatAdapter
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityChatBinding
import models.Mensaje
import models.TipoMensaje
import java.io.File
import java.io.IOException

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val mensajes = mutableListOf<Mensaje>()
    private lateinit var adapter: ChatAdapter
    private lateinit var uri: Uri
    var grabadora: MediaRecorder? = null
    var rutaAudio: String? = null
    var estaGrabando = false

    // Lanzador para seleccionar imagen de galería
    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(), ActivityResultCallback {
            // it es el uri
            loadImage(it!!)
        }
    )

    // Lanzador de cámara
    private val camaraLauncher = registerForActivityResult(
        // TakePicture guarda la imagen en el uri
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if(it){
                loadImage(uri)
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
                    binding.listaMensajes.scrollToPosition(mensajes.size - 1)
                }
            }
        }
    )

    // Permiso para grabar audio
    val audioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permiso de audio concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de audio denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Permiso para escribir en almacenamiento externo
    val storagePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permiso de almacenamiento concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nombreGrupo = intent.getStringExtra("nombreGrupo")
        binding.tvTitulo.text = nombreGrupo
        adapter = ChatAdapter(mensajes)
        binding.listaMensajes.layoutManager = LinearLayoutManager(this)
        binding.listaMensajes.adapter = adapter

        requestAudioPermission()
        requestStoragePermission()

        val readPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, readPermission) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(readPermission)) {
                Toast.makeText(this, "Se necesita acceso al almacenamiento para leer archivos.", Toast.LENGTH_LONG).show()
            }
            storagePermission.launch(readPermission)
        } else {
            Toast.makeText(this, "Permiso de lectura ya concedido", Toast.LENGTH_SHORT).show()
        }

        // Enviar texto
        binding.btnEnviar.setOnClickListener {
            val texto = binding.editTextMensaje.text.toString()
            if (texto.isNotEmpty()) {
                Toast.makeText(this, "Mensaje enviado", Toast.LENGTH_SHORT).show()
                mensajes.add(Mensaje(texto, TipoMensaje.TEXTO))
                adapter.notifyItemInserted(mensajes.size - 1)
                binding.editTextMensaje.text.clear()
                binding.listaMensajes.scrollToPosition(mensajes.size - 1)
            }
        }

        // Seleccionar imagen de la galería
        binding.btnCargarImagen.setOnClickListener {
            galeriaLauncher.launch("image/*")
        }

        // Tomar foto con la cámara
        binding.btnTomarFoto.setOnClickListener {
            // Crea archivo pocFromCamera dentro de almacenamiento interno
            val file = File(getFilesDir(), "picFromCamera")
            // Genera uri para acceder al archivo
            uri = FileProvider.getUriForFile(
                baseContext,
                baseContext.packageName + ".fileprovider",
                file
            )
            // Lanza la camara
            camaraLauncher.launch(uri)
        }

        // Grabar audio
        binding.btnGrabarAudio.setOnClickListener {
            grabarAudio()
        }

        binding.botonBack.setOnClickListener {
            //regresa a ComunidadesActivity
            finish()
        }
    }

    private fun requestAudioPermission() {
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(permission)) {
                Toast.makeText(this, "Se necesita acceso al micrófono para grabar audio.", Toast.LENGTH_LONG).show()
            }
            audioPermission.launch(permission)
        } else {
            Toast.makeText(this, "Permiso de audio ya concedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestStoragePermission() {
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(permission)) {
                Toast.makeText(this, "Se necesita acceso al almacenamiento para guardar archivos.", Toast.LENGTH_LONG).show()
            }
            storagePermission.launch(permission)
        } else {
            Toast.makeText(this, "Permiso de almacenamiento ya concedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun grabarAudio() {
        if (!estaGrabando) {
            rutaAudio = "${getExternalFilesDir(null)?.absolutePath}/grabacion.3gp"
            grabadora = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(rutaAudio)
                prepare()
                start()
            }
            estaGrabando = true
            binding.btnGrabarAudio.setBackgroundColor(Color.BLUE)
            Toast.makeText(this, "Grabando audio...", Toast.LENGTH_SHORT).show()
        } else {
            grabadora?.apply {
                stop()
                release()
            }
            grabadora = null
            estaGrabando = false
            binding.btnGrabarAudio.setBackgroundColor(Color.GRAY)

            // Agrega el mensaje al chat
            rutaAudio?.let {
                mensajes.add(Mensaje("Audio grabado", TipoMensaje.AUDIO, uri = it))
                adapter.notifyItemInserted(mensajes.size - 1)
                binding.listaMensajes.scrollToPosition(mensajes.size - 1)
            }
            Toast.makeText(this, "Grabación terminada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImage(uri: Uri) {
        // Abre iamgen desde la ruta
        val imageStream = getContentResolver().openInputStream(uri)
        // Convierte los bytes de la imagen en un objeto que Android puede mostrar
        val bitmap = BitmapFactory.decodeStream(imageStream)
        mensajes.add(Mensaje("Mensaje", TipoMensaje.IMAGEN, uri = uri.toString()))
        adapter.notifyItemInserted(mensajes.size - 1)
        binding.listaMensajes.scrollToPosition(mensajes.size - 1)
    }

}