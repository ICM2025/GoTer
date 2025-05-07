package com.example.gooter_proyecto

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
import com.google.firebase.database.*
import models.Canal
import models.Comunidad
import models.Notificacion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var comunidadHomeAdapter: ComunidadHomeAdapter
    private lateinit var notificacionHomeAdapter: NotificacionAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
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
                        loadActivitySummary(uid)
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
        loadNotificaciones()

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)

        toolbar.setNavigationOnClickListener {
            val popup = PopupMenu(this, toolbar)
            popup.menuInflater.inflate(R.menu.menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_notifications -> {
                        val i = Intent(baseContext, NotificacionesActivity::class.java)
                        startActivity(i)
                        true
                    }
                    R.id.menu_perfil -> {
                        val i = Intent(baseContext, PerfilUsuarioActivity::class.java)
                        startActivity(i)
                        true
                    }
                    R.id.menu_logout -> {
                        auth.signOut()
                        val i = Intent(this, MainActivity::class.java)
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(i)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        binding.btnMapa.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }
        binding.btnCorrer.setOnClickListener {
            startActivity(Intent(this, CarreraActivity::class.java))
        }
        binding.btnGrupos.setOnClickListener {
            startActivity(Intent(this, ComunidadesActivity::class.java))
        }
        binding.btnActividad.setOnClickListener {
            startActivity(Intent(this, EstadisticasFechaInicioActivity::class.java))
        }
    }

    private fun loadActivitySummary(userId: String) {
        val statsRef = database.child("estadisticasUsuarios").child(userId)

        statsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val resumenTotal = snapshot.child("resumenTotal")

                val totalDistance = resumenTotal.child("distanciaTotal").getValue(Double::class.java) ?: 0.0
                val totalCalories = resumenTotal.child("caloriasTotal").getValue(Int::class.java) ?: 0
                val totalTime = resumenTotal.child("tiempoTotal").getValue(Long::class.java) ?: 0

                val diarias = snapshot.child("diarias")
                var recentDistance = 0.0
                var recentCalories = 0
                var recentTime = 0L

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()

                for (i in 0..6) {
                    val dateKey = dateFormat.format(calendar.time)
                    val dailyData = diarias.child(dateKey)

                    recentDistance += dailyData.child("distanciaRecorrida").getValue(Double::class.java) ?: 0.0
                    recentCalories += dailyData.child("caloriasGastadas").getValue(Int::class.java) ?: 0
                    recentTime += dailyData.child("tiempoActividad").getValue(Long::class.java) ?: 0

                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }

                runOnUiThread {
                    binding.tvNumKilometros.text = String.format("%.1f", recentDistance)
                    binding.tvNumCalorias.text = recentCalories.toString()
                    val minutes = (recentTime / 60).toInt()
                    binding.tvNumMinutos.text = minutes.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Error al cargar estadísticas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadNotificaciones() {
        val userId = auth.currentUser?.uid ?: return
        val notificacionesRef = database.child("notificaciones")

        notificacionesRef.orderByChild("destinatarioId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notificaciones = mutableListOf<Notificacion>()
                    for (notificacionSnap in snapshot.children) {
                        val idNotificacion = notificacionSnap.key ?: ""
                        val titulo = notificacionSnap.child("tipo").getValue(String::class.java) ?: "Notificación"
                        val descripcion = notificacionSnap.child("mensaje").getValue(String::class.java) ?: ""
                        val remitente = notificacionSnap.child("emisorId").getValue(String::class.java) ?: "Sistema"
                        val destinatario = userId
                        val fecha = notificacionSnap.child("fechaHora").getValue(String::class.java) ?: ""
                        val leida = notificacionSnap.child("leida").getValue(Boolean::class.java) ?: false
                        val accion = notificacionSnap.child("accion").getValue(String::class.java) ?: ""
                        val tipo = notificacionSnap.child("tipo").getValue(String::class.java) ?: "General"

                        val metadatos = notificacionSnap.child("metadatos").getValue(String::class.java)
                            ?: notificacionSnap.child("metadatos").children.associate {
                                it.key to it.value.toString()
                            }.toString()

                        notificaciones.add(Notificacion(
                            idNotificacion = idNotificacion,
                            titulo = titulo,
                            descripcion = descripcion,
                            remitente = remitente,
                            destinatario = destinatario,
                            fecha = fecha,
                            leida = leida,
                            accion = accion,
                            tipo = tipo,
                            metadatos = metadatos
                        ))
                    }

                    notificacionHomeAdapter = NotificacionAdapter(notificaciones, this@HomeActivity)
                    binding.rvListaNotificaciones.apply {
                        layoutManager = LinearLayoutManager(
                            this@HomeActivity,
                            LinearLayoutManager.VERTICAL,
                            false
                        )
                        adapter = notificacionHomeAdapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Error al cargar notificaciones", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadComunidades() {
        val userId = auth.currentUser?.uid ?: return

        val comunidadesRef = database.child("comunidad")
        comunidadesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comunidades = mutableListOf<Comunidad>()
                for (comunidadSnap in snapshot.children) {
                    val adminId = comunidadSnap.child("administrador").getValue(String::class.java)
                    val participantes = comunidadSnap.child("participantes").children.mapNotNull { it.getValue(String::class.java) }

                    if (adminId == userId || participantes.contains(userId)) {
                        val nombre = comunidadSnap.child("nombreGrupo").getValue(String::class.java) ?: "Sin nombre"
                        val miembros = comunidadSnap.child("miembros").getValue(Int::class.java) ?: participantes.size
                        comunidades.add(Comunidad(nombre, R.drawable.ic_user, miembros, participantes))
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