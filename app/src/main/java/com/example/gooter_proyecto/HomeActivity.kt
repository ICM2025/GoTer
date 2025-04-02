package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding : ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapButton.setOnClickListener {
                startActivity(Intent(baseContext, MapsActivity::class.java))
        }
        binding.entrarCarrer.setOnClickListener{
            startActivity(Intent(baseContext, MapsActivity::class.java))
        }
        binding.buscarRuta.setOnClickListener {
            startActivity(Intent(baseContext, MapsActivity::class.java))
        }
        binding.perfilButton.setOnClickListener{
            startActivity(Intent(baseContext,PerfilUsuarioActivity::class.java))
        }
        binding.informeSalud.setOnClickListener{
            startActivity(Intent(baseContext,EstadisticasFechaInicioActivity::class.java))
        }
        binding.comunidadButton.setOnClickListener{
            startActivity(Intent(baseContext,ComunidadesActivity::class.java))
        }
        binding.notificacionesButton.setOnClickListener{
            startActivity(Intent(baseContext,NotificacionesActivity::class.java))
        }

    }
}