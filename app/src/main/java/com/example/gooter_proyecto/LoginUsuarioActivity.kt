package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityLoginUsuarioBinding

class LoginUsuarioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginUsuarioBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonIngresar.setOnClickListener {
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }
    }
}