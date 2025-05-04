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
import models.Badge
import java.io.File

class PerfilUsuarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilUsuarioBinding
    private lateinit var uri: Uri

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        if (it != null) loadImage(it)
    }

    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {
        if (it) loadImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val uid = auth.currentUser?.uid

        binding.btnBack.setOnClickListener {
            startActivity(Intent(baseContext, MainActivity::class.java))
        }

        binding.ivProfilePhoto.setOnClickListener {
            mostrarOpcionesImagen()
        }

        if (uid == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("usuarios").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
                    val apellidos = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                    val usuario = snapshot.child("nombreUsuario").getValue(String::class.java) ?: ""
                    val biografia = snapshot.child("biografia").getValue(String::class.java) ?: ""
                    val links = snapshot.child("links").getValue(String::class.java) ?: ""
                    val mensajeInsignias =
                        "Estas son las insignias forjadas por @$usuario durante el uso de Go-Race"

                    binding.tvName.text = "$nombre $apellidos"
                    binding.tvUsername.text = "@$usuario"
                    binding.tvBiographyContent.text = biografia
                    binding.tvLinksContent.text = links
                    binding.tvBadgesDescription.text = mensajeInsignias
                    aplicarLinksClicables(links)

                    val userBadgesRef = database.child("usuarios").child(uid).child("insignias")

                    val badgeMap = mapOf(
                        "First Race" to true,
                        "10 K" to true,
                        "Tiempos superados" to true
                    )

                    userBadgesRef.setValue(badgeMap)
                        .addOnSuccessListener {
                            Log.d("PerfilUsuarioActivity", "Insignias guardadas correctamente")
                        }
                        .addOnFailureListener {
                            Log.e("PerfilUsuarioActivity", "Error al guardar insignias", it)
                        }

                    val badgeList = listOf(
                        Badge("First Race", "100+", R.drawable.ic_star, true),
                        Badge("10 K", "100+", R.drawable.ic_star, true),
                        Badge("Nuevo trayecto", "100+", R.drawable.ic_star, true),
                        Badge("Tiempos superados", "100+", R.drawable.ic_star, true),
                        Badge("Primer año", "100+", R.drawable.ic_star, true)
                    )


                    val adapter = BadgeAdapter(badgeList)
                    binding.rvBadges.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                    binding.rvBadges.adapter = adapter

                } else {
                    Toast.makeText(this, "No hay datos del usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
            }

        binding.tvBiographyContent.setOnClickListener {
            val editText = android.widget.EditText(this)
            editText.setText(binding.tvBiographyContent.text)

            AlertDialog.Builder(this)
                .setTitle("Editar biografía")
                .setView(editText)
                .setPositiveButton("Guardar") { _, _ ->
                    val nuevaBio = editText.text.toString()
                    database.child("usuarios").child(uid).child("biografia").setValue(nuevaBio)
                        .addOnSuccessListener {
                            binding.tvBiographyContent.text = nuevaBio
                            Toast.makeText(this, "Biografía actualizada", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al guardar biografía", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.tvLinksContent.setOnClickListener {
            val editText = android.widget.EditText(this)
            editText.setText(binding.tvLinksContent.text)

            AlertDialog.Builder(this)
                .setTitle("Agregar Links")
                .setView(editText)
                .setPositiveButton("Guardar") { _, _ ->
                    val nuevoLink = editText.text.toString()
                    database.child("usuarios").child(uid).child("links").setValue(nuevoLink)
                        .addOnSuccessListener {
                            binding.tvLinksContent.text = nuevoLink
                            Toast.makeText(this, "Links actualizados", Toast.LENGTH_SHORT).show()
                            aplicarLinksClicables(nuevoLink)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al guardar links", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun aplicarLinksClicables(links: String) {
        val spannableString = SpannableString(links)
        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        binding.tvLinksContent.text = spannableString
        binding.tvLinksContent.movementMethod = android.text.method.LinkMovementMethod.getInstance()
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
}
