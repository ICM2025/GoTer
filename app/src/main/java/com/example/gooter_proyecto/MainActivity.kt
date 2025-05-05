package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registrate.setOnClickListener {
            startActivity(Intent(baseContext, CrearCarreraActivity::class.java))
        }

        binding.iniciarSesion.setOnClickListener {
            startActivity(Intent(baseContext, LoginUsuarioActivity::class.java))
        }
        //bindign pa llamar la primera pantalla,  de esa a login o registro
    }
}