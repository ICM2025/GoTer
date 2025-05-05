package com.example.gooter_proyecto

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityCrearCarreraBinding

class CrearCarreraActivity : AppCompatActivity() {

    lateinit var binding: ActivityCrearCarreraBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCrearCarreraBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}