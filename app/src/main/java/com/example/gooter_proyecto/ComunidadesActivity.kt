package com.example.gooter_proyecto

import adapters.CanalAdapter
import adapters.ComunidadAdapter
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooter_proyecto.databinding.ActivityComunidadesBinding
import models.Canal
import models.Comunidad

class ComunidadesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComunidadesBinding
    private lateinit var comunidadAdapter: ComunidadAdapter
    private lateinit var CanalAdapter: CanalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComunidadesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configComunidadesRecyclerView()
        configCanalesRecyclerView()

        binding.ButtonBack.setOnClickListener { finish() }
        binding.ButtonCrearComunidad.setOnClickListener {
            startActivity(Intent(baseContext, CrearComunidadActivity::class.java))
        }
    }

    private fun configComunidadesRecyclerView() {
        comunidadAdapter = ComunidadAdapter(getComunidades())
        binding.ListaComunidades.apply {
            layoutManager = LinearLayoutManager(this@ComunidadesActivity)
            adapter = comunidadAdapter
        }
    }

    private fun configCanalesRecyclerView() {
        CanalAdapter = CanalAdapter(getCanales())
        binding.ListaCanales.apply {
            layoutManager = LinearLayoutManager(this@ComunidadesActivity)
            adapter = CanalAdapter
        }
    }

    private fun getComunidades(): List<Comunidad> {
        return listOf(
            Comunidad("Grupo 1", R.drawable.images),
            Comunidad("Grupo 2", R.drawable.images),
            Comunidad("Grupo 3", R.drawable.images),
            Comunidad("Grupo 4", R.drawable.images),
            Comunidad("Grupo 5", R.drawable.images),
            Comunidad("Grupo 6", R.drawable.images),
            Comunidad("Grupo 7", R.drawable.images)
        )
    }

    private fun getCanales(): List<Canal> {
        return listOf(
            Canal("Canal 1", R.drawable.images, false),
            Canal("Canal 2", R.drawable.images, true),
            Canal("Canal 3", R.drawable.images, false),
            Canal("Canal 4", R.drawable.images, false),
            Canal("Canal 5", R.drawable.images, true)
        )
    }
}