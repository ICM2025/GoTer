package com.example.gooter_proyecto

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.util.Linkify
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityPerfilUsuarioBinding
import java.io.File
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class PerfilUsuarioActivity : AppCompatActivity() {

    private lateinit var binding : ActivityPerfilUsuarioBinding
    private lateinit var uri: Uri

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference


    //Foto de Perfil del usuario
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

        binding.btnBack.setOnClickListener {
            startActivity(Intent(baseContext, MainActivity::class.java))
        }
        binding.ivProfilePhoto.setOnClickListener {
            mostrarOpcionesImagen()
        }

        //Recuperacion datos del usuario
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val uid = auth.currentUser?.uid

        if (uid == null) {
            Log.w("PerfilUsuarioActivity", "UID de usuario es null. El usuario no ha iniciado sesión.")
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("PerfilUsuarioActivity", "UID del usuario: $uid")

            database.child("usuarios").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
                        val apellidos = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                        val usuario = snapshot.child("nombreUsuario").getValue(String::class.java) ?: ""
                        val biografia = snapshot.child("biografia").getValue(String::class.java) ?: ""
                        val links = snapshot.child("links").getValue(String::class.java) ?: ""

                        Log.d("PerfilUsuarioActivity", "Datos obtenidos: nombre=$nombre, apellidos=$apellidos, usuario=$usuario")

                        binding.tvName.text = "$nombre $apellidos"
                        binding.tvUsername.text = "@$usuario"
                        binding.tvBiographyContent.text = biografia
                        binding.tvLinksContent.text = links
                        aplicarLinksClicables(links)

                    } else {
                        Log.w("PerfilUsuarioActivity", "El snapshot no existe. El nodo usuarios/$uid está vacío.")
                        Toast.makeText(this, "No hay datos del usuario", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("PerfilUsuarioActivity", "Error al obtener datos del usuario: ", exception)
                    Toast.makeText(this, "No se pudo obtener el nombre del usuario", Toast.LENGTH_SHORT).show()
                }
        }


        //Biografia por el usuario
        binding.tvBiographyContent.setOnClickListener {
            val editText = android.widget.EditText(this)
            editText.setText(binding.tvBiographyContent.text)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Editar biografía")
                .setView(editText)
                .setPositiveButton("Guardar") { _, _ ->
                    val nuevaBio = editText.text.toString()
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        database.child("usuarios").child(uid).child("biografia").setValue(nuevaBio)
                            .addOnSuccessListener {
                                binding.tvBiographyContent.text = nuevaBio
                                Toast.makeText(this, "Biografía actualizada", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al guardar biografía", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.show()
        }

        //Poner link en la biografia
        binding.tvLinksContent.setOnClickListener {
            val editText = android.widget.EditText(this)
            editText.setText(binding.tvLinksContent.text)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Agregar Links")
                .setView(editText)
                .setPositiveButton("Guardar") { _, _ ->
                    val nuevoLink = editText.text.toString()
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        database.child("usuarios").child(uid).child("links").setValue(nuevoLink)
                            .addOnSuccessListener {
                                binding.tvLinksContent.text = nuevoLink
                                Toast.makeText(this, "Biografía actualizada", Toast.LENGTH_SHORT).show()
                                aplicarLinksClicables(nuevoLink)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al guardar biografía", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.show()
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
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.ivProfilePhoto)
    }

}