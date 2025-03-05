package com.example.gooter_proyecto

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityEstadisticasFechaInicioBinding

class EstadisticasFechaInicio : AppCompatActivity() {
    private lateinit var binding : ActivityEstadisticasFechaInicioBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityEstadisticasFechaInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}