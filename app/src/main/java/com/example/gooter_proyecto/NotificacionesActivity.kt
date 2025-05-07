package com.example.gooter_proyecto

import adapters.NotificacionAdapter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityNotificacionesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import models.Notificacion

class NotificacionesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificacionesBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var notificacionAdapter: NotificacionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFirebase()
        setupUI()
        setupRecyclerView()
        loadNotificacionesTiempoReal()
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }

    private fun setupUI() {
        binding.ButtonBack.setOnClickListener { finish() }
        binding.ButtonRedactar.setOnClickListener {
            Toast.makeText(this, "Función de redacción en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        notificacionAdapter = NotificacionAdapter(emptyList())
        binding.rvNotificaciones.apply {
            layoutManager = LinearLayoutManager(this@NotificacionesActivity)
            adapter = notificacionAdapter
        }
    }

    private fun loadNotificacionesTiempoReal() {
        val userId = auth.currentUser?.uid ?: return

        // Consulta optimizada: solo notificaciones del usuario actual
        database.child("notificaciones")
            .orderByChild("destinatarioId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notificaciones = mutableListOf<Notificacion>()

                    if (!snapshot.exists()) {
                        // Mostrar estado vacío si no hay notificaciones
                        binding.rvNotificaciones.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                        return
                    }

                    for (notifSnapshot in snapshot.children) {
                        val titulo = notifSnapshot.child("tipo").getValue(String::class.java) ?: "Notificación"
                        val descripcion = notifSnapshot.child("mensaje").getValue(String::class.java) ?: ""
                        val remitente = notifSnapshot.child("emisorId").getValue(String::class.java) ?: "Sistema"
                        val destinatario = userId // Ya sabemos que es el usuario actual
                        val fecha = notifSnapshot.child("fechaHora").getValue(String::class.java) ?: "Sin fecha"

                        notificaciones.add(Notificacion(titulo, descripcion, remitente, destinatario, fecha))
                    }

                    // Ordenar por fecha (más recientes primero)
                    notificaciones.sortByDescending { it.fecha }

                    notificacionAdapter.updateList(notificaciones)
                    binding.rvNotificaciones.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@NotificacionesActivity,
                        "Error al cargar notificaciones: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
