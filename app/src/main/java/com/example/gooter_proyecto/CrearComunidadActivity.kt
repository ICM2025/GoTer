package com.example.gooter_proyecto

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityCrearComunidadBinding

class CrearComunidadActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCrearComunidadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearComunidadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ButtonCancelar.setOnClickListener {
            finish()
        }

        binding.ButtonCrear.setOnClickListener {
            crearGrupo()
        }
    }

    private fun crearGrupo() {
        val nombreGrupo = binding.NombreGrupo.text.toString()
        val esPrivado = binding.ButtonPrivado.isChecked
        val tipoPrivacidad = if (esPrivado) "Privado" else "Público"

        if (nombreGrupo.isEmpty()) {
            Toast.makeText(this, "Ingrese un nombre de grupo", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Grupo '$nombreGrupo' creado como $tipoPrivacidad", Toast.LENGTH_LONG).show()
        }
    }
}
