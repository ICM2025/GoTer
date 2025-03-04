package com.example.gooter_proyecto

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityLoginUsuarioBinding

class login_usuario : AppCompatActivity() {
    private lateinit var binding: ActivityLoginUsuarioBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}