package com.example.gooter_proyecto

import adapters.ComunidadHomeAdapter
import adapters.NotificacionAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
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
        NotificacionesDisponibles.getInstance().inicializar(this)
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
        borrarCarrerasPendientes()

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
        permisoNotificaciones()
    }

    private fun borrarCarrerasPendientes() {
        val uid = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("carreras")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (carreraSnapshot in snapshot.children) {
                    val carreraId = carreraSnapshot.key ?: continue
                    val ubicaciones = carreraSnapshot.child("ubicacionesParticipantes")

                    if (ubicaciones.hasChild(uid)) {
                        // El usuario participó en esta carrera, así que eliminamos toda la carrera
                        dbRef.child(carreraId).removeValue()
                            .addOnSuccessListener {
                                Log.d("CARRERA", "Carrera eliminada: $carreraId")
                            }
                            .addOnFailureListener { e ->
                                Log.e("CARRERA", "Error al eliminar carrera $carreraId: ${e.message}")
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CARRERA", "Error al consultar carreras: ${error.message}")
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            val prefs = getSharedPreferences("permisos", MODE_PRIVATE)
            val editor = prefs.edit()

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                NotificacionesDisponibles.getInstance().inicializar(this)
                editor.putInt("rechazos_notificaciones", 0).apply()
            } else {
                val rechazosActuales = prefs.getInt("rechazos_notificaciones", 0)
                editor.putInt("rechazos_notificaciones", rechazosActuales + 1).apply()
            }
        }
    }


    private fun permisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val prefs = getSharedPreferences("permisos", MODE_PRIVATE)
            val rechazos = prefs.getInt("rechazos_notificaciones", 0)

            val permiso = android.Manifest.permission.POST_NOTIFICATIONS

            if (checkSelfPermission(permiso) == PackageManager.PERMISSION_GRANTED) {
                NotificacionesDisponibles.getInstance().inicializar(this)
            } else {
                // Si ya ha rechazado dos veces, no pedimos más
                if (rechazos >= 2) return

                // Si rechazó una vez, mostramos racional si aplica
                if (rechazos == 1 && shouldShowRequestPermissionRationale(permiso)) {
                    Toast.makeText(
                        this,
                        "La app necesita el permiso de notificaciones para avisarte de novedades importantes.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                requestPermissions(arrayOf(permiso), 1001)
            }
        } else {
            NotificacionesDisponibles.getInstance().inicializar(this)
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
                        val idNotificacion = notificacionSnap.key ?: continue
                        val titulo = notificacionSnap.child("tipo").getValue(String::class.java) ?: "Notificación"
                        val descripcion = notificacionSnap.child("mensaje").getValue(String::class.java) ?: ""
                        val remitente = notificacionSnap.child("emisorId").getValue(String::class.java) ?: "Sistema"
                        val destinatario = userId
                        val leida = notificacionSnap.child("leida").getValue(Boolean::class.java) ?: false
                        val accion = notificacionSnap.child("accion").getValue(String::class.java) ?: ""
                        val tipo = notificacionSnap.child("tipo").getValue(String::class.java) ?: "General"

                        // Corrección para manejar fechaHora
                        val fechaValue = notificacionSnap.child("fechaHora").value
                        val fecha = when (fechaValue) {
                            is Long -> fechaValue.toString()
                            is String -> fechaValue
                            else -> ""
                        }

                        // Skip finalizar_carrera notifications
                        if (accion == "finalizar_carrera") {
                            Log.d("Notificaciones", "Skipping finalizar_carrera notification: $idNotificacion")
                            notificacionSnap.ref.removeValue()
                                .addOnSuccessListener {
                                    Log.d("Notificaciones", "Deleted finalizar_carrera notification: $idNotificacion")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Notificaciones", "Error deleting notification: ${e.message}")
                                }
                            continue
                        }

                        // Safely handle metadatos
                        val metadatosValue = notificacionSnap.child("metadatos").value
                        val metadatosString = when (metadatosValue) {
                            is HashMap<*, *> -> {
                                val idCarreraRaw = metadatosValue["idCarrera"]
                                val idCarrera = when (idCarreraRaw) {
                                    is String -> idCarreraRaw
                                    is Long -> idCarreraRaw.toString()
                                    else -> "unknown"
                                }
                                metadatosValue.entries.joinToString(", ") {
                                    if (it.key == "idCarrera") "idCarrera=$idCarrera" else "${it.key}=${it.value}"
                                }
                            }
                            is String -> metadatosValue
                            else -> ""
                        }

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
                            metadatos = metadatosString
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
                    Log.i("COMUNIDADES_EMPTY:", notificaciones.isEmpty().toString())
                    binding.tvSinNotificaciones.visibility = if (notificaciones.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Error al cargar notificaciones: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    private fun loadComunidades() {
        val userId = auth.currentUser?.uid ?: return
        val ref = database.child("comunidad")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<Comunidad>()
                snapshot.children.forEach { snap ->
                    val id = snap.key ?: return@forEach
                    val nombre = snap.child("nombreGrupo").getValue(String::class.java) ?: ""
                    val participantes = snap.child("participantes").children.mapNotNull { it.getValue(String::class.java) }
                    val admin = snap.child("administrador").getValue(String::class.java)
                    val chatId = snap.child("chatId").getValue(String::class.java) ?: ""
                    val fotoUrl = snap.child("fotoUrl").getValue(String::class.java) ?: ""

                    if (admin == userId || participantes.contains(userId)) {
                        val miembros = snap.child("miembros").getValue(Int::class.java) ?: participantes.size
                        lista.add(Comunidad(id, nombre, fotoUrl, miembros, participantes, chatId))
                    }
                }
                runOnUiThread {
                    comunidadHomeAdapter = ComunidadHomeAdapter(lista) { comunidad ->
                        val intent = Intent(this@HomeActivity, ChatActivity::class.java).apply {
                            putExtra("nombreGrupo", comunidad.nombre)
                            putExtra("chatId", comunidad.idChat)
                            putExtra("comunidadId", comunidad.id)
                        }
                        startActivity(intent)
                    }
                    binding.rvListaComunidades.apply {
                        layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
                        adapter = comunidadHomeAdapter
                    }
                    binding.tvSinComunidades.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
