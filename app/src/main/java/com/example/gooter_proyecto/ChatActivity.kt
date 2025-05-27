package com.example.gooter_proyecto

import adapters.ChatAdapter
import android.app.Activity
import android.content.Intent
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
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import models.Mensaje
import java.io.File
import java.io.IOException
import java.util.UUID

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val mensajes = mutableListOf<Mensaje>()
    private lateinit var adapter: ChatAdapter
    private lateinit var uri: Uri
    var grabadora: MediaRecorder? = null
    var rutaAudio: String? = null
    var estaGrabando = false
    private lateinit var databaseRef: DatabaseReference
    private lateinit var chatRef: DatabaseReference
    private lateinit var storageRef: StorageReference
    private val email = FirebaseAuth.getInstance().currentUser?.email
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private var idChat: String? = null
    private var menuVisible = false // Variable para controlar la visibilidad del menú
    val usuario = hashMapOf(
        "nombre" to "",
        "email" to email,
        "uid" to uid
    )

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

    // Permiso para grabar audio
    val audioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permiso de audio concedido", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Permiso para escribir en almacenamiento externo
    val storagePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permiso de almacenamiento concedido", Toast.LENGTH_SHORT).show()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nombreGrupo = intent.getStringExtra("nombreGrupo")
        idChat = intent.getStringExtra("chatId")
        val idComunidad = intent.getStringExtra("comunidadId")
        cargarNombreUsuario()

        Log.i("idComunidad", idComunidad.toString())

        databaseRef = FirebaseDatabase.getInstance().getReference("chats").child(idChat!!).child("mensajes")
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(idChat!!)
        storageRef = FirebaseStorage.getInstance().reference.child("chatsUris")

        cargarMensajesDesdeFirebase()
        binding.tvTitulo.text = nombreGrupo

        // Configurar el menú hamburguesa
        configurarMenuHamburguesa(idComunidad)

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

        // Ocultar menú si es canal
        if (intent.getStringExtra("canalId") != null) {
            binding.btnMenuHamburguesa.visibility = View.GONE
        }

        // Enviar texto
        binding.btnEnviar.setOnClickListener {
            val texto = binding.editTextMensaje.text.toString()
            Log.e("el nombre es" , usuario["nombre"].toString())
            if (texto.isNotEmpty()) {
                val mensaje = Mensaje(
                    nombre = usuario["nombre"].toString(),
                    correo = email,
                    propioMensaje = false,
                    contenido = texto,
                    tipo = "TEXTO",
                    uri = "",
                    timestamp = System.currentTimeMillis()
                )
                databaseRef.push().setValue(mensaje)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Mensaje enviado", Toast.LENGTH_SHORT).show()
                        binding.editTextMensaje.text.clear()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al enviar el mensaje", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Seleccionar imagen de la galería
        binding.btnCargarImagen.setOnClickListener {
            galeriaLauncher.launch("image/*")
        }

        // Tomar foto con la cámara
        binding.btnTomarFoto.setOnClickListener {
            val file = File(getFilesDir(), "picFromCamera")
            uri = FileProvider.getUriForFile(
                baseContext,
                baseContext.packageName + ".fileprovider",
                file
            )
            camaraLauncher.launch(uri)
        }

        // Grabar audio
        binding.btnGrabarAudio.setOnClickListener {
            grabarAudio()
        }

        binding.botonBack.setOnClickListener {
            finish()
        }
    }

    private fun configurarMenuHamburguesa(idComunidad: String?) {
        // Botón del menú hamburguesa
        binding.btnMenuHamburguesa.setOnClickListener {
            toggleMenu()
        }

        // Opción agregar persona
        binding.opcionAgregarPersona.setOnClickListener {
            ocultarMenu()
            val i = Intent(this, AddPersonasComunidadActivity::class.java)
            i.putExtra("id_comunidad", idComunidad)
            startActivity(i)
        }

        // Opción salir del grupo
        binding.opcionSalirGrupo.setOnClickListener {
            ocultarMenu()
            val i = Intent(this, ComunidadesActivity::class.java)
            if (idComunidad != null) {
                salirComunidad(idComunidad)
            }
            startActivity(i)
        }
    }

    private fun toggleMenu() {
        if (menuVisible) {
            ocultarMenu()
        } else {
            mostrarMenu()
        }
    }

    private fun mostrarMenu() {
        binding.menuOpciones.visibility = View.VISIBLE
        menuVisible = true
        // Opcional: rotar el ícono del menú
        binding.btnMenuHamburguesa.animate()
            .rotation(90f)
            .setDuration(200)
            .start()
    }

    private fun ocultarMenu() {
        binding.menuOpciones.visibility = View.GONE
        menuVisible = false
        // Opcional: restaurar la rotación del ícono
        binding.btnMenuHamburguesa.animate()
            .rotation(0f)
            .setDuration(200)
            .start()
    }

    // Ocultar menú cuando se toque fuera de él
    override fun onBackPressed() {
        if (menuVisible) {
            ocultarMenu()
        } else {
            super.onBackPressed()
        }
    }

    private fun salirComunidad(idComunidad : String) {
        val participantesRef = FirebaseDatabase.getInstance().getReference("comunidad").child(idComunidad).child("participantes")
        participantesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (participanteSnap in snapshot.children) {
                    val userIdInList = participanteSnap.getValue(String::class.java)
                    if (userIdInList == FirebaseAuth.getInstance().currentUser?.uid) {
                        participanteSnap.ref.removeValue()
                        Log.i("COMUNIDAD", "Usuario eliminado de la comunidad ${idComunidad}")
                        break
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al intentar eliminar: ${error.message}")
            }
        })
    }

    private fun cargarNombreUsuario() {
        var database = FirebaseDatabase.getInstance().reference
        if (uid != null) {
            database.child("usuarios").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val nombreSnapshot = snapshot.child("nombre").getValue(String::class.java) ?: ""
                        val apellidoSnapshot = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                        usuario["nombre"] = nombreSnapshot + " " +  apellidoSnapshot[0] + "."
                    } else {
                        Toast.makeText(this, "No se encontró el usuario", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarMensajesDesdeFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mensajes.clear()
                for (mensajeSnap in snapshot.children) {
                    val mensaje = mensajeSnap.getValue(Mensaje::class.java)
                    if (mensaje != null) {
                        if(mensaje.correo == email){
                            mensaje.propioMensaje = true
                            mensaje.nombre = "Tu"
                        }
                        mensajes.add(mensaje)
                    }
                }
                adapter.notifyDataSetChanged()
                binding.listaMensajes.scrollToPosition(mensajes.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Error al cargar mensajes", Toast.LENGTH_SHORT).show()
            }
        })
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

            rutaAudio?.let { rutaLocal ->
                subirAudioAStorage(rutaLocal)
            }
            Toast.makeText(this, "Grabación terminada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun subirAudioAStorage(rutaLocal: String) {
        Toast.makeText(this, "Subiendo audio...", Toast.LENGTH_SHORT).show()

        val nombreAudio = "audio_${UUID.randomUUID()}.3gp"
        val audioRef = storageRef.child(nombreAudio)
        val archivoAudio = Uri.fromFile(File(rutaLocal))

        audioRef.putFile(archivoAudio)
            .addOnSuccessListener { taskSnapshot ->
                audioRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val urlAudio = downloadUri.toString()

                    val mensaje = Mensaje(
                        nombre = usuario["nombre"].toString(),
                        correo = email,
                        propioMensaje = false,
                        contenido = "",
                        tipo = "AUDIO",
                        uri = urlAudio,
                        timestamp = System.currentTimeMillis()
                    )

                    databaseRef.push().setValue(mensaje)
                        .addOnSuccessListener {
                            guardarUrlAudioEnChat(urlAudio)
                            Toast.makeText(this, "Audio enviado", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al enviar audio", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al subir audio: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("ChatActivity", "Error al subir audio", exception)
            }
    }

    private fun guardarUrlAudioEnChat(urlAudio: String) {
        chatRef.get().addOnSuccessListener { snapshot ->
            var contadorAudios = 1

            while (snapshot.hasChild("urlAudio$contadorAudios")) {
                contadorAudios++
            }

            val campoAudio = "urlAudio$contadorAudios"
            chatRef.child(campoAudio).setValue(urlAudio)
                .addOnSuccessListener {
                    Log.d("ChatActivity", "URL de audio guardada como $campoAudio")
                }
                .addOnFailureListener { exception ->
                    Log.e("ChatActivity", "Error al guardar URL de audio", exception)
                }
        }.addOnFailureListener { exception ->
            Log.e("ChatActivity", "Error al obtener información del chat", exception)
        }
    }

    private fun loadImage(uri: Uri) {
        val nombreImagen = "imagen_${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(nombreImagen)

        imageRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val urlImagen = downloadUri.toString()
                    val mensaje = Mensaje(
                        nombre = usuario["nombre"].toString(),
                        correo = email,
                        propioMensaje = false,
                        contenido = "",
                        tipo = "IMAGEN",
                        uri = urlImagen,
                        timestamp = System.currentTimeMillis()
                    )
                    databaseRef.push().setValue(mensaje)
                        .addOnSuccessListener {
                            guardarUrlImagenEnChat(urlImagen)
                            Toast.makeText(this, "Imagen enviada", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al enviar imagen", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al subir imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("ChatActivity", "Error al subir imagen", exception)
            }
    }

    private fun guardarUrlImagenEnChat(urlImagen: String) {
        chatRef.get().addOnSuccessListener { snapshot ->
            var contadorImagenes = 1
            while (snapshot.hasChild("urlImagen$contadorImagenes")) {
                contadorImagenes++
            }
            val campoImagen = "urlImagen$contadorImagenes"
            chatRef.child(campoImagen).setValue(urlImagen)
                .addOnSuccessListener {
                    Log.d("ChatActivity", "URL de imagen guardada como $campoImagen")
                }
                .addOnFailureListener { exception ->
                    Log.e("ChatActivity", "Error al guardar URL de imagen", exception)
                }
        }.addOnFailureListener { exception ->
            Log.e("ChatActivity", "Error al obtener información del chat", exception)
        }
    }
}