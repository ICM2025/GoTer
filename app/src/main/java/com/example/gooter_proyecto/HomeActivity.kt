package com.example.gooter_proyecto

import adapters.CanalAdapter
import adapters.ComunidadAdapter
import adapters.ComunidadHomeAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityHomeBinding
import models.Comunidad

class HomeActivity : AppCompatActivity() {
    private lateinit var binding : ActivityHomeBinding
    private lateinit var comunidadHomeAdapter: ComunidadHomeAdapter
    private lateinit var notificacionHomeAdapter: ComunidadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configComunidadesRecyclerView()
        configNotificacionesRecyclerView()

        binding.btnCampana.setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }
        binding.btnUsuario.setOnClickListener {
            startActivity(Intent(this, PerfilUsuarioActivity::class.java))
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
        comunidadHomeAdapter = ComunidadHomeAdapter(getComunidades())
        binding.rvListaComunidades.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity,LinearLayoutManager.HORIZONTAL, false)
            adapter = comunidadHomeAdapter
        }
    }

    //Se esta usando adaptador de comunidades para la pantalla de notificaciones
    //Hay que cambiarlo
    private fun configNotificacionesRecyclerView() {
        notificacionHomeAdapter = ComunidadAdapter(getNotificaciones())
        binding.rvListaNotificaciones.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity,LinearLayoutManager.VERTICAL, false)
            adapter = notificacionHomeAdapter
        }
    }

    private fun getNotificaciones(): List<Comunidad> {
        return listOf(
            Comunidad("Notificacion 1", R.drawable.background_username,5),
            Comunidad("Notificacion 2", R.drawable.background_username,7),
            Comunidad("Notificacion 3", R.drawable.background_username,0),
            Comunidad("Notificacion 4", R.drawable.background_username,7),
            Comunidad("Notificacion 5", R.drawable.background_username,46),
            Comunidad("Notificacion 6", R.drawable.background_username,34),
            Comunidad("Notificacion 7", R.drawable.background_username,21)
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