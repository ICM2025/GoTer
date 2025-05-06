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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
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


        configComunidadesRecyclerView()
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

    private fun configComunidadesRecyclerView() {
        comunidadHomeAdapter = ComunidadHomeAdapter(getComunidades()) { comunidad ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("nombreGrupo", comunidad.nombre)
            startActivity(intent)
        }

        binding.rvListaComunidades.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = comunidadHomeAdapter
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

    private fun getComunidades(): List<Comunidad> {
        return listOf(
            Comunidad("Grupo 1", R.drawable.ic_user,5),
            Comunidad("Grupo 2", R.drawable.ic_user,7),
            Comunidad("Grupo 3", R.drawable.ic_user,0),
            Comunidad("Grupo 4", R.drawable.ic_user,7),
            Comunidad("Grupo 5", R.drawable.ic_user,34),
            Comunidad("Grupo 7", R.drawable.ic_user,21)
        )
    }
}