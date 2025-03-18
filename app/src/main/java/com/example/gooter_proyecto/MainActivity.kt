package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pruebaFechaInicio.setOnClickListener {
            startActivity(Intent(baseContext, EstadisticasFechaInicioActivity::class.java))
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
        binding.pruebaEstadisticas.setOnClickListener {
            startActivity(Intent(baseContext, EstadisticasActivity::class.java))
        }
        binding.PruebaComunidades.setOnClickListener {
            startActivity(Intent(baseContext, ComunidadesActivity::class.java))
        }
        binding.pruebaPerfil.setOnClickListener {
            startActivity(Intent(baseContext, PerfilUsuario::class.java))
        }
        binding.pruebaRegistro.setOnClickListener {
            startActivity(Intent(baseContext, Registro::class.java))
        }
    }
}