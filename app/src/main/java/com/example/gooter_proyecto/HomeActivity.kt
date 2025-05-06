package com.example.gooter_proyecto

import adapters.CanalAdapter
import adapters.ComunidadAdapter
import adapters.ComunidadHomeAdapter
import adapters.NotificacionAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityHomeBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import models.Canal
import models.Comunidad
import models.Notificacion

class HomeActivity : AppCompatActivity() {
    private lateinit var binding : ActivityHomeBinding
    private lateinit var comunidadHomeAdapter: ComunidadHomeAdapter
    private lateinit var notificacionHomeAdapter: NotificacionAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference


        val uid = auth.currentUser?.uid

        if (uid != null) {
            database.child("usuarios").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
                        binding.tvSaludo.text = "Hola, $nombre!"
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


        loadComunidades()
        configNotificacionesRecyclerView()

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)

        toolbar.setNavigationOnClickListener {
            val popup = PopupMenu(this, toolbar)
            popup.menuInflater.inflate(R.menu.menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_notifications -> {
                        val i = Intent(baseContext, NotificacionesActivity::class.java)
                        startActivity(i)
                        Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.menu_perfil -> {
                        val i = Intent(baseContext, PerfilUsuarioActivity::class.java)
                        startActivity(i)
                        Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.menu_logout -> {
                        auth.signOut()
                        val i = Intent(this, MainActivity::class.java)
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(i)
                        Toast.makeText(this, "SignOut", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        binding.btnMapa.setOnClickListener{
            startActivity(Intent(this, MapsActivity::class.java))
        }
        binding.btnCorrer.setOnClickListener{
            startActivity(Intent(this, MapsActivity::class.java))
        }
        binding.btnGrupos.setOnClickListener{
            startActivity(Intent(this, ComunidadesActivity::class.java))
        }
        binding.btnActividad.setOnClickListener{
            startActivity(Intent(this, EstadisticasFechaInicioActivity::class.java))
        }

    }

    //Se esta usando adaptador de comunidades para la pantalla de notificaciones
    //Hay que cambiarlo
    private fun configNotificacionesRecyclerView() {
        notificacionHomeAdapter = NotificacionAdapter(getNotificaciones())
        binding.rvListaNotificaciones.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity,LinearLayoutManager.VERTICAL, false)
            adapter = notificacionHomeAdapter
        }
    }

    private fun getNotificaciones(): List<Notificacion> {
        return listOf(
            Notificacion("Carrera con Diego", "En 15 minutos • 3km"),
            Notificacion("Invitación a Grupo", "Corredores Matutinos • 8 miembros"),
            Notificacion("Puntos Obtenidos", "+150 puntos esta semana"),
            Notificacion("Tus estadisticas de la semana", "Haz mejorado 15% tu rendimiento"),
        )
    }

    private fun loadComunidades() {
        val userId = auth.currentUser?.uid ?: return

        val comunidadesRef = database.child("comunidad")
        comunidadesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comunidades = mutableListOf<Comunidad>()
                for (comunidadSnap in snapshot.children) {
                    val adminId = comunidadSnap.child("administrador").getValue(String::class.java)
                    val participantes = comunidadSnap.child("participantes").children.mapNotNull { it.getValue(String::class.java) }

                    if (adminId == userId || participantes.contains(userId)) {
                        val nombre = comunidadSnap.child("nombreGrupo").getValue(String::class.java) ?: "Sin nombre"
                        val miembros = comunidadSnap.child("miembros").getValue(Int::class.java) ?: participantes.size
                        comunidades.add(Comunidad(nombre, R.drawable.ic_user, miembros))
                    }
                }

                comunidadHomeAdapter = ComunidadHomeAdapter(comunidades) { comunidad ->
                    val intent = Intent(this@HomeActivity, ChatActivity::class.java).apply {
                        putExtra("nombreGrupo", comunidad.nombre)
                    }
                    startActivity(intent)
                }

                binding.rvListaComunidades.apply {
                    layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
                    adapter = comunidadHomeAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

}