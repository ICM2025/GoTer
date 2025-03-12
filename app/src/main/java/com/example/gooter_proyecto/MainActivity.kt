package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pruebaFechaInicio.setOnClickListener {
            startActivity(Intent(baseContext, EstadisticasFechaInicio::class.java))
        }
        binding.pruebaIrHome.setOnClickListener {
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }
        binding.pruebaLogin.setOnClickListener {
            startActivity(Intent(baseContext, LoginUsuario::class.java))
        }
        binding.pruebaMapa.setOnClickListener {
            startActivity(Intent(baseContext, Principal_Mapa::class.java))
        }
        binding.PruebaComunidades.setOnClickListener {
            startActivity(Intent(baseContext, ComunidadesActivity::class.java))
        }
    }
}
