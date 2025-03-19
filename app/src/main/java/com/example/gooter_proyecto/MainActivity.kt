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

        binding.registrate.setOnClickListener {
            startActivity(Intent(baseContext, RegistroActivity::class.java))
        }

        binding.iniciarSesion.setOnClickListener {
            startActivity(Intent(baseContext, LoginUsuarioActivity::class.java))
        }
        //bindign pa llamar la primera pantalla,  de esa a login o registro
    }
}