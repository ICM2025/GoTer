package com.example.gooter_proyecto

import adapters.BadgeAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.util.Linkify
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.gooter_proyecto.databinding.ActivityPerfilUsuarioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import models.Badge
import java.io.File

class PerfilUsuarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilUsuarioBinding
    private lateinit var uri: Uri

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storageRef: StorageReference

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        it?.let { uri ->
            loadImage(uri)
            uploadProfilePhoto(uri)
        }
    }

    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {
        if (it) {
            loadImage(uri)
            uploadProfilePhoto(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storageRef = FirebaseStorage.getInstance().reference.child("profileFotos")

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            startActivity(Intent(baseContext, MainActivity::class.java))
        }

        binding.ivProfilePhoto.setOnClickListener {
            mostrarOpcionesImagen()
        }

        // Cargar datos del usuario
        database.child("usuarios").child(uid).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Datos básicos
                    val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
                    val apellidos = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                    val usuario = snapshot.child("nombreUsuario").getValue(String::class.java) ?: ""
                    val biografia = snapshot.child("biografia").getValue(String::class.java) ?: ""
                    val links = snapshot.child("links").getValue(String::class.java) ?: ""
                    val fotoUrl = snapshot.child("urlFotoPerfil").getValue(String::class.java)

                    binding.tvName.text = "$nombre $apellidos"
                    binding.tvUsername.text = "@$usuario"
                    binding.tvBiographyContent.text = biografia
                    binding.tvLinksContent.text = links
                    aplicarLinksClicables(links)

                    // Cargar foto de perfil si existe URL
                    fotoUrl?.let {
                        Glide.with(this@PerfilUsuarioActivity)
                            .load(it)
                            .circleCrop()
                            .into(binding.ivProfilePhoto)
                    }

                    // Insignias (sin cambios)
                    val badgeMap = mapOf(
                        "First Race" to true,
                        "10 K" to true,
                        "Tiempos superados" to true
                    )
                    database.child("usuarios").child(uid).child("insignias").setValue(badgeMap)

                    val badgeList = listOf(
                        Badge("First Race", "100+", R.drawable.ic_star, true),
                        Badge("10 K", "100+", R.drawable.ic_star, true),
                        Badge("Nuevo trayecto", "100+", R.drawable.ic_star, true),
                        Badge("Tiempos superados", "100+", R.drawable.ic_star, true),
                        Badge("Primer año", "100+", R.drawable.ic_star, true)
                    )
                    val adapter = BadgeAdapter(badgeList)
                    binding.rvBadges.layoutManager = LinearLayoutManager(this@PerfilUsuarioActivity)
                    binding.rvBadges.adapter = adapter

                } else {
                    Toast.makeText(this@PerfilUsuarioActivity, "No hay datos del usuario", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PerfilUsuarioActivity, "Error al obtener datos", Toast.LENGTH_SHORT).show()
            }
        })

        // Editar biografía y links (sin cambios)
        binding.tvBiographyContent.setOnClickListener { /*...*/ }
        binding.tvLinksContent.setOnClickListener { /*...*/ }
    }

    private fun mostrarOpcionesImagen() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen de perfil")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirCamara()
                    1 -> abrirGaleria()
                }
            }
            .show()
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
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.ivProfilePhoto)
    }

    private fun uploadProfilePhoto(fileUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val photoRef = storageRef.child("$uid.jpg")
        photoRef.putFile(fileUri)
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    database.child("usuarios").child(uid)
                        .child("urlFotoPerfil").setValue(downloadUri.toString())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun aplicarLinksClicables(links: String) {
        // 1. Creamos un SpannableString a partir del texto bruto
        val spannableString = SpannableString(links)
        // 2. Detectamos patrones de URLs y los convertimos en enlaces
        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        // 3. Asignamos el texto enriquecido al TextView
        binding.tvLinksContent.text = spannableString
        // 4. Habilitamos el movimiento para que al tocar abra el navegador
        binding.tvLinksContent.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

}
