package com.example.gooter_proyecto

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityAddPersonasComunidadBinding
import com.example.gooter_proyecto.databinding.ActivityChatBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import models.Usuario

class AddPersonasComunidadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPersonasComunidadBinding
    private lateinit var userAdapter: UserAdapter
    private lateinit var listaUsuarios: MutableList<Usuario>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPersonasComunidadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idComunidad = intent.getStringExtra("id_comunidad")

        val databaseRef = FirebaseDatabase.getInstance().reference

        Log.i("idComunidad", idComunidad.toString())

        listaUsuarios = mutableListOf()

        idComunidad.let {
            if (it != null) {
                loadUsuarios(databaseRef, it)
            }
        }

        binding.botonBack.setOnClickListener {
            finish()
        }
    }

    private fun loadUsuarios(databaseRef: DatabaseReference, idComunidad: String) {
        databaseRef.child("usuarios").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                for (usuarioSnapshot in snapshot.children) {
                    val nombre = usuarioSnapshot.child("nombre").value?.toString() ?: ""
                    val apellidos = usuarioSnapshot.child("apellidos").value?.toString() ?: ""
                    val nombreUsuario =
                        usuarioSnapshot.child("nombreUsuario").value?.toString() ?: ""
                    val fechaNacimiento =
                        usuarioSnapshot.child("fechaNacimiento").value?.toString() ?: ""
                    val correo = usuarioSnapshot.child("correo").value?.toString() ?: ""

                    val usuario = Usuario(nombre, apellidos, nombreUsuario, fechaNacimiento, correo)
                    listaUsuarios.add(usuario)
                }

                userAdapter = UserAdapter(this, R.layout.customrowusers, listaUsuarios, idComunidad)
                binding.listaUsuarios.adapter = userAdapter
            }
        }.addOnFailureListener { e ->
            Log.e("Firebase", "Error al cargar usuarios: ${e.message}")
        }
    }
}