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
    private var comunidadesDelUsuario: List<Comunidad> = emptyList()
    private var todasLasComunidades: List<Comunidad> = emptyList()


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
        binding.Buscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                filtrarComunidades(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadComunidades() {
        if (userId == null) return

        val comunidadesRef = database.child("comunidad")
        comunidadesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comunidadesUsuarioTemp = mutableListOf<Comunidad>()
                val todasTemp = mutableListOf<Comunidad>()

                for (comunidadSnap in snapshot.children) {
                    val adminId = comunidadSnap.child("administrador").getValue(String::class.java)
                    val participantes = comunidadSnap.child("participantes").children.mapNotNull { it.getValue(String::class.java) }
                    val idChat = comunidadSnap.child("chatId").getValue(String::class.java) ?: "sim chat"
                    val idComunidad = comunidadSnap.key
                    val nombre = comunidadSnap.child("nombreGrupo").getValue(String::class.java) ?: "Sin nombre"
                    val miembros = comunidadSnap.child("miembros").getValue(Int::class.java) ?: participantes.size
                    val fotoUrl = comunidadSnap.child("fotoUrl")
                        .getValue(String::class.java) ?: ""

                    val comunidad = Comunidad(idComunidad.toString(), nombre, fotoUrl, miembros, participantes, idChat)

                    todasTemp.add(comunidad) // guardar todas

                    if (adminId == userId || participantes.contains(userId)) {
                        comunidadesUsuarioTemp.add(comunidad)
                    }
                }

                comunidadesDelUsuario = comunidadesUsuarioTemp
                todasLasComunidades = todasTemp

                mostrarComunidades(comunidadesDelUsuario, true)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarComunidades(lista: List<Comunidad>, esDelUsuario: Boolean) {
        comunidadAdapter = ComunidadAdapter(lista) { comunidad ->
            if (esDelUsuario) {
                val intent = Intent(this@ComunidadesActivity, ChatActivity::class.java).apply {
                    putExtra("comunidadId", comunidad.id)
                    putExtra("nombreGrupo", comunidad.nombre)
                    putExtra("chatId", comunidad.idChat)
                    putExtra("fotoUrl", comunidad.imagen)
                }
                startActivity(intent)
            } else {
                solicitudComunidad(comunidad)
            }
        }

        binding.ListaComunidades.apply {
            layoutManager = LinearLayoutManager(this@ComunidadesActivity)
            adapter = comunidadAdapter
            visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        }

        binding.tvSinComunidades.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun solicitudComunidad(comunidad: Comunidad) {
        val database = FirebaseDatabase.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacionId = database.reference.child("notificaciones").push().key ?: return
        val fechaHora = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        // Obtener el nombre del usuario para el mensaje
        database.reference.child("usuarios").child(uid).child("nombre").get()
            .addOnSuccessListener { snapshot ->
                val nombreUsuario = snapshot.getValue(String::class.java) ?: "Un usuario"

                // Armar metadatos
                val metadatos = mapOf(
                    "usuarioId" to uid,
                    "comunidadId" to comunidad.id
                )

                // Crear la notificación
                val notificacion = mapOf(
                    "idNotificacion" to notificacionId,
                    "emisorId" to uid,
                    "destinatarioId" to comunidad.participantes.first(), // o comunidad.administrador si lo tienes en el objeto
                    "fechaHora" to fechaHora,
                    "leida" to false,
                    "tipo" to "Solicitud",
                    "mensaje" to "$nombreUsuario ha solicitado unirse a tu comunidad \"${comunidad.nombre}\"",
                    "accion" to "solicitud_usuario",
                    "metadatos" to org.json.JSONObject(metadatos).toString()
                )

                database.reference.child("notificaciones").child(notificacionId)
                    .setValue(notificacion)
                    .addOnSuccessListener {
                        android.widget.Toast.makeText(
                            this,
                            "Solicitud enviada correctamente",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        android.widget.Toast.makeText(
                            this,
                            "Error al enviar solicitud",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }


    private fun filtrarComunidades(texto: String) {
        if (texto.isBlank()) {
            binding.MisComunidades.text = "Mis Comunidades"
            mostrarComunidades(comunidadesDelUsuario, true)
        } else {
            binding.MisComunidades.text = "Comunidades a las que puedes solicitar unirte"
            val resultado = todasLasComunidades.filter { comunidad ->
                comunidad.nombre.contains(texto.trim(), ignoreCase = true) &&
                        !comunidadesDelUsuario.any { it.id == comunidad.id }
            }
            mostrarComunidades(resultado, false)
        }
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
                    val idCanal = canalSnap.key
                    val nombre = canalSnap.child("nombreGrupo").getValue(String::class.java) ?: "Sin nombre"
                    val privado = canalSnap.child("privado").getValue(Boolean::class.java) ?: false
                    val fotoUrl = canalSnap.child("fotoUrl")
                        .getValue(String::class.java) ?: ""

                    canales.add(
                        Canal(
                            idCanal.toString(),
                            nombre,
                            fotoUrl,
                            privado,
                            miembros.size,
                            idChat
                        )
                    )
                }

                canalAdapter = CanalAdapter(canales) { canal ->
                    verificarPertenenciaACanal(userId!!, canal.id) { pertenece ->
                        if (pertenece) {
                            val intent = Intent(this@ComunidadesActivity, ChatActivity::class.java).apply {
                                putExtra("nombreGrupo", canal.nombre)
                                putExtra("chatId", canal.idChat)
                                putExtra("canalId", canal.id)
                            }
                            startActivity(intent)
                        } else {
                            Log.i("CanalAcceso", "El usuario no pertenece al canal ${canal.nombre}")
                            runOnUiThread {
                                android.widget.Toast.makeText(
                                    this@ComunidadesActivity,
                                    "Debes seguir el canal para entrar",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }


                binding.rvListaCanales.layoutManager = LinearLayoutManager(this@ComunidadesActivity)
                binding.rvListaCanales.adapter = canalAdapter
                Log.d("Firebase", "Canales encontrados: ${canales.size}")
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun verificarPertenenciaACanal(usuarioId: String, canalId: String, callback: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        val participantesRef = database.child("canal").child(canalId).child("participantes")

        participantesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pertenece = snapshot.children.any { it.getValue(String::class.java) == usuarioId }
                callback(pertenece)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al verificar pertenencia: ${error.message}")
                callback(false)
            }
        })
    }



}