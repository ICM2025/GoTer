package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityLoginUsuarioBinding

class LoginUsuario : AppCompatActivity() {
    private lateinit var binding: ActivityLoginUsuarioBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(baseContext, Principal_Mapa::class.java))
        }
    }
}