package com.example.gooter_proyecto

import adapters.CanalAdapter
import adapters.ComunidadAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityComunidadesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import models.Canal
import models.Comunidad

class ComunidadesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComunidadesBinding
    private lateinit var comunidadAdapter: ComunidadAdapter
    private lateinit var canalAdapter: CanalAdapter
    private lateinit var database: DatabaseReference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComunidadesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance().reference
        loadComunidades()
        loadCanales()
        binding.btnBack.setOnClickListener { startActivity(Intent(this, HomeActivity::class.java)) }
        binding.ButtonCrearComunidad.setOnClickListener {
            startActivity(Intent(baseContext, CrearComunidadActivity::class.java))
        }
    }

    private fun loadComunidades() {
        if (userId == null) return

        val comunidadesRef = database.child("comunidad")
        comunidadesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comunidades = mutableListOf<Comunidad>()
                for (comunidadSnap in snapshot.children) {
                    val adminId = comunidadSnap.child("administrador").getValue(String::class.java)
                    val participantes = comunidadSnap.child("participantes").children.mapNotNull { it.getValue(String::class.java) }
                    val idChat = comunidadSnap.child("chatId").getValue(String::class.java) ?: "sim chat"
                    if (adminId == userId || participantes.contains(userId)) {
                        val nombre = comunidadSnap.child("nombreGrupo").getValue(String::class.java) ?: "Sin nombre"
                        val miembros = comunidadSnap.child("miembros").getValue(Int::class.java) ?: participantes.size
                        comunidades.add(Comunidad(nombre, R.drawable.background_username, miembros, participantes,idChat))
                    }
                }
                comunidadAdapter = ComunidadAdapter(comunidades) { comunidad ->
                    val intent = Intent(this@ComunidadesActivity, ChatActivity::class.java).apply {
                        putExtra("nombreGrupo", comunidad.nombre)
                        putExtra("chatId", comunidad.idChat)
                    }
                    startActivity(intent)
                }
                binding.ListaComunidades.layoutManager = LinearLayoutManager(this@ComunidadesActivity)
                binding.ListaComunidades.adapter = comunidadAdapter
                Log.d("Firebase", "Comunidades encontradas: ${comunidades.size}")
                if (comunidades.isEmpty()) {
                    binding.ListaComunidades.visibility = View.GONE
                    binding.TusComunidades.text = "No estás en ninguna comunidad aún"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadCanales() {
        if (userId == null) return

        val canalesRef = database.child("canal")
        canalesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val canales = mutableListOf<Canal>()
                for (canalSnap in snapshot.children) {
                    val adminId = canalSnap.child("administrador").getValue(String::class.java)
                    val miembros = canalSnap.child("miembros").children.mapNotNull { it.getValue(String::class.java) }
                    val idChat = canalSnap.child("chatId").getValue(String::class.java) ?: "sim chat"
                    if (adminId == userId || miembros.contains(userId)) {
                        val nombre = canalSnap.child("nombreGrupo").getValue(String::class.java) ?: "Sin nombre"
                        val privado = canalSnap.child("privado").getValue(Boolean::class.java) ?: false
                        canales.add(Canal(nombre, R.drawable.background_username, privado, miembros.size, idChat))
                    }
                }

                canalAdapter = CanalAdapter(canales) { canal ->
                    val intent = Intent(this@ComunidadesActivity, ChatActivity::class.java).apply {
                        intent.putExtra("nombreGrupo", canal.nombre)
                        intent.putExtra("chatId", canal.idChat)
                    }
                    startActivity(intent)
                }
                binding.rvListaCanales.layoutManager = LinearLayoutManager(this@ComunidadesActivity)
                binding.rvListaCanales.adapter = canalAdapter
                Log.d("Firebase", "Canales encontrados: ${canales.size}")
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



}