package com.example.gooter_proyecto

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.gooter_proyecto.databinding.ActivityCrearComunidadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CrearComunidadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrearComunidadBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    // Variables para imagen
    private var selectedImageUri: Uri? = null
    private var imageUrl: String = ""

    // Lanzadores para Intents
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Mostrar la imagen seleccionada en el ImageView
            Glide.with(this).load(it).circleCrop().into(binding.ivProfilePhoto)
            uploadImageToStorage(it)
        }
    }
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            val uri = saveBitmapToCache(it)
            selectedImageUri = uri
            // Mostrar la foto capturada en el ImageView
            Glide.with(this).load(uri).circleCrop().into(binding.ivProfilePhoto)
            uploadImageToStorage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearComunidadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, ComunidadesActivity::class.java))
        }

        binding.ivProfilePhoto.setOnClickListener {
            showImagePickerDialog()
        }

        binding.ButtonCrear.setOnClickListener {
            // Verificar si imagen fue subida antes de crear
            if (selectedImageUri != null && imageUrl.isEmpty()) {
                Toast.makeText(this, "Espere a que la imagen termine de subir", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            crearGrupo()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> takePhotoLauncher.launch(null)
                }
            }
            .show()
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "temp_image_\${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return FileProvider.getUriForFile(this, "\${packageName}.provider", file)
    }

    private fun uploadImageToStorage(uri: Uri) {
        val folder = "groupeUris"
        val fileName = UUID.randomUUID().toString() + ".jpg"
        val ref = storage.reference.child("$folder/$fileName")
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    imageUrl = downloadUri.toString()
                    Toast.makeText(this, "Imagen subida", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al subir imagen: \${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun crearGrupo() {
        val nombreGrupo = binding.NombreGrupo.text.toString()
        val esPrivado = binding.ButtonPrivado.isChecked

        if (nombreGrupo.isEmpty()) {
            Toast.makeText(this, "Ingrese un nombre de grupo", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioActual = auth.currentUser
        if (usuarioActual == null) {
            Toast.makeText(this, "Debe iniciar sesión para crear un grupo", Toast.LENGTH_SHORT).show()
            return
        }

        val idGrupo = UUID.randomUUID().toString()
        val fechaCreacion = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        if (esPrivado) crearComunidad(idGrupo, nombreGrupo, fechaCreacion, usuarioActual.uid)
        else crearCanal(idGrupo, nombreGrupo, fechaCreacion, usuarioActual.uid)
    }

    private fun crearComunidad(idComunidad: String, nombre: String, fechaCreacion: String, administradorId: String) {
        val newChatRef = database.child("chats").push()
        val chatId = newChatRef.key
        newChatRef.setValue(mapOf("name" to "chat de $nombre"))

        val comunidadMap = hashMapOf(
            "nombreGrupo" to nombre,
            "descripcion" to "Esta es la descripción de la comunidad",
            "fechaCreacion" to fechaCreacion,
            "administrador" to administradorId,
            "miembros" to 1,
            "participantes" to listOf(administradorId),
            "eventos" to listOf<String>(),
            "fotoUrl" to imageUrl,
            "chatId" to chatId
        )

        database.child("comunidad").child(idComunidad).setValue(comunidadMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Comunidad '$nombre' creada exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear comunidad: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun crearCanal(idCanal: String, nombre: String, fechaCreacion: String, administradorId: String) {
        val newChatRef = database.child("chats").push()
        val chatId = newChatRef.key
        newChatRef.setValue(mapOf("name" to "chat de $nombre"))

        val canalMap = hashMapOf(
            "nombreGrupo" to nombre,
            "descripcion" to "Esta es la descripción de la comunidad",
            "fechaCreacion" to fechaCreacion,
            "administrador" to administradorId,
            "miembros" to listOf(administradorId),
            "participantes" to listOf(administradorId),
            "eventos" to listOf<String>(),
            "fotoUrl" to imageUrl,
            "mensajes" to hashMapOf<String, Any>(),
            "privado" to false,
            "chatId" to chatId
        )

        database.child("canal").child(idCanal).setValue(canalMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Canal '$nombre' creado exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear canal: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}