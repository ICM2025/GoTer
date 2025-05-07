package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityCrearComunidadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CrearComunidadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrearComunidadBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearComunidadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, ComunidadesActivity::class.java))
        }

        binding.ButtonCrear.setOnClickListener {
            crearGrupo()
        }
    }

    private fun crearGrupo() {
        val nombreGrupo = binding.NombreGrupo.text.toString()
        val esPrivado = binding.ButtonPrivado.isChecked
        val tipoPrivacidad = if (esPrivado) "Privado" else "Público"

        if (nombreGrupo.isEmpty()) {
            Toast.makeText(this, "Ingrese un nombre de grupo", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioActual = auth.currentUser
        if (usuarioActual == null) {
            Toast.makeText(this, "Debe iniciar sesión para crear un grupo", Toast.LENGTH_SHORT).show()
            return
        }

        // Generar un ID único para el grupo
        val idGrupo = UUID.randomUUID().toString()

        // Fecha actual en formato ISO
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val fechaCreacion = formatoFecha.format(Date())

        if (esPrivado) {
            // Crear una comunidad (privada)
            crearComunidad(idGrupo, nombreGrupo, fechaCreacion, usuarioActual.uid)
        } else {
            // Crear un canal (público)
            crearCanal(idGrupo, nombreGrupo, fechaCreacion, usuarioActual.uid)
        }
    }

    private fun crearComunidad(idComunidad: String, nombre: String, fechaCreacion: String, administradorId: String) {
        val comunidadMap = hashMapOf(
            "nombreGrupo" to nombre,
            "descripcion" to "Esta es la descripción de la comunidad",
            "fechaCreacion" to fechaCreacion,
            "administrador" to administradorId,
            "miembros" to 1,  // Inicialmente solo el creador
            "participantes" to listOf(administradorId),
            "eventos" to listOf<String>(),
            "imagenGrupo" to "",  // Sin imagen por ahora
            "mensajes" to HashMap<String, Any>()  // Mensajes vacíos inicialmente
        )

        // Guardar en Firebase
        database.child("comunidad").child(idComunidad).setValue(comunidadMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Comunidad '$nombre' creada exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear comunidad: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun crearCanal(idCanal: String, nombre: String, fechaCreacion: String, administradorId: String) {
        val canalMap = hashMapOf(
            "nombreGrupo" to nombre,
            "descripcion" to "Esta es la descripción de la comunidad",
            "fechaCreacion" to fechaCreacion,
            "administrador" to administradorId,
            "miembros" to listOf(administradorId),
            "participantes" to listOf(administradorId),
            "eventos" to listOf<String>(),
            "imagenGrupo" to "",  // Sin imagen por ahora
            "mensajes" to HashMap<String, Any>(),  // Mensajes vacíos inicialmente
            "privado" to false  // Es público (canal)
        )

        // Guardar en Firebase
        database.child("canal").child(idCanal).setValue(canalMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Canal '$nombre' creado exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear canal: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}