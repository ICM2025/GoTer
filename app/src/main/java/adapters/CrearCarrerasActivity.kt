package adapters

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.ChatActivity
import com.example.gooter_proyecto.R
import com.example.gooter_proyecto.databinding.ActivityCrearCarrerasBinding
import com.example.gooter_proyecto.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import models.Comunidad


class CrearCarrerasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrearCarrerasBinding
    private lateinit var comunidadHomeAdapter: ComunidadHomeAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityCrearCarrerasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
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
                        comunidades.add(Comunidad(nombre, R.drawable.ic_user, miembros))
                    }
                }

                comunidadHomeAdapter = ComunidadHomeAdapter(comunidades) { comunidad ->
                    val intent = Intent(this@CrearCarrerasActivity, ChatActivity::class.java).apply {
                        putExtra("nombreGrupo", comunidad.nombre)
                    }
                    startActivity(intent)
                }

                binding.rvListaComunidades.apply {
                    layoutManager = LinearLayoutManager(this@CrearCarrerasActivity, LinearLayoutManager.HORIZONTAL, false)
                    adapter = comunidadHomeAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}