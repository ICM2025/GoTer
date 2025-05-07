package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.gooter_proyecto.databinding.ActivityEstadisticasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class CalorieEntry(val date: String, val calories: Int)
data class CaloriesPayload(
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    val data: List<CalorieEntry>
)

class EstadisticasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEstadisticasBinding
    private lateinit var database: DatabaseReference
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val dataList = mutableListOf<CalorieEntry>()

    // Variables para almacenar los totales
    private var totalDistance = 0.0
    private var activeDaysCount = 0

    private val cloudFunctionUrl = "https://us-central1-go-oter-ee454.cloudfunctions.net/generate_chart"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        binding.botonBack.setOnClickListener {
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }

        // Obtener fechas desde el intent
        val startDateStr = intent.getStringExtra("fecha_inicio")
        val endDateStr = intent.getStringExtra("fecha_final")

        if (startDateStr.isNullOrEmpty() || endDateStr.isNullOrEmpty()) {
            Toast.makeText(this, "Fechas no proporcionadas", Toast.LENGTH_LONG).show()
            return
        }

        loadCaloriesChart(startDateStr, endDateStr)
    }

    private fun loadCaloriesChart(startDateStr: String, endDateStr: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_LONG).show()
            return
        }
        val userId = currentUser.uid
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = dateFormat.parse(startDateStr)!!
        val endDate = dateFormat.parse(endDateStr)!!

        // Resetear los totales
        totalDistance = 0.0
        activeDaysCount = 0
        dataList.clear()

        // Cambiar la referencia para apuntar a estadisticasUsuarios y luego a diarias
        val statsRef = database.child("estadisticasUsuarios").child(userId).child("diarias")

        statsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dateSnapshot in snapshot.children) {
                    val dateKey = dateSnapshot.key ?: continue
                    try {
                        val entryDate = dateFormat.parse(dateKey)
                        if (entryDate != null && (entryDate >= startDate && entryDate <= endDate)) {
                            val calorias = dateSnapshot.child("caloriasGastadas").getValue(Int::class.java)
                            val distancia = dateSnapshot.child("distanciaRecorrida").getValue(Double::class.java)

                            // Sumar distancia si existe
                            distancia?.let {
                                totalDistance += it
                                activeDaysCount++ // Solo contamos días con distancia recorrida
                            }

                            if (calorias != null && calorias > 0) {
                                dataList.add(CalorieEntry(dateKey, calorias))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ParseError", "Fecha inválida: $dateKey", e)
                    }
                }

                if (dataList.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@EstadisticasActivity,
                            "No hay datos en ese rango de fechas",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return
                }

                // Ordenar los datos por fecha
                dataList.sortBy { dateFormat.parse(it.date) }

                // Actualizar la UI con los totales
                runOnUiThread {
                    binding.tvKilometros.text = "${String.format("%.1f", totalDistance)} Km"
                    binding.tvDescripcion.text = "Recorridos en los últimos $activeDaysCount días"
                }

                val payload = CaloriesPayload(startDateStr, endDateStr, dataList)
                sendToCloudFunction(payload)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer la base de datos", error.toException())
                Toast.makeText(this@EstadisticasActivity, "Error de Firebase", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun sendToCloudFunction(caloriesData: CaloriesPayload) {
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = Gson().toJson(caloriesData).toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(cloudFunctionUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EstadisticasActivity", "Error al llamar a la Cloud Function", e)
                runOnUiThread {
                    Toast.makeText(
                        this@EstadisticasActivity,
                        "Error al cargar el gráfico: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("EstadisticasActivity", "Respuesta no exitosa: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(
                            this@EstadisticasActivity,
                            "Error del servidor: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return
                }

                val responseBody = response.body
                if (responseBody != null) {
                    val imageBytes = responseBody.bytes()
                    runOnUiThread {
                        Glide.with(this@EstadisticasActivity)
                            .load(imageBytes)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(binding.ivGrafico)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@EstadisticasActivity,
                            "No se recibieron datos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }
}