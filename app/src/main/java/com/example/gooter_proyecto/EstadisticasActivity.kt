package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityEstadisticasBinding
import com.example.gooter_proyecto.databinding.ActivityHomeBinding
import com.example.gooter_proyecto.databinding.ActivityMainBinding

class EstadisticasActivity : AppCompatActivity() {
    lateinit var binding : ActivityEstadisticasBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botonBack.setOnClickListener{
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }
    }
}