package com.example.gooter_proyecto

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityPerfilUsuarioBinding
import java.io.File
import androidx.core.content.FileProvider

class PerfilUsuarioActivity : AppCompatActivity() {

    private lateinit var binding : ActivityPerfilUsuarioBinding
    private lateinit var uri: Uri

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(), ActivityResultCallback {
            loadImage(it!!)
        }
    )

    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if (it) {
                loadImage(uri)
            }
        }
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botonBack.setOnClickListener {
            finish()
        }
        binding.ivFotoPerfil.setOnClickListener {
            mostrarOpcionesImagen()
        }
    }

    private fun mostrarOpcionesImagen() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccionar imagen de perfil")
        builder.setItems(opciones) { dialog, which ->
            when (which) {
                0 -> abrirCamara()
                1 -> abrirGaleria()
            }
        }

        builder.show()
    }

    private fun abrirGaleria() {
        galeriaLauncher.launch("image/*")
    }

    private fun abrirCamara() {
        val file = File(filesDir, "picFromCamera")
        uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        camaraLauncher.launch(uri)
    }

    private fun loadImage(uri: Uri) {
        val imagenStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(imagenStream)
        binding.ivFotoPerfil.setImageBitmap(bitmap)
    }

}