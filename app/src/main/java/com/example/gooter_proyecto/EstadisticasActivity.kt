package com.example.gooter_proyecto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.gooter_proyecto.databinding.ActivityEstadisticasBinding
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class CalorieEntry(val date: String, val calories: Int)
data class CaloriesPayload(
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date")   val endDate: String,
    val data: List<CalorieEntry>
)

class EstadisticasActivity : AppCompatActivity() {
    lateinit var binding: ActivityEstadisticasBinding
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // URL de tu Cloud Function
    private val cloudFunctionUrl = "https://us-central1-go-oter-ee454.cloudfunctions.net/generate_chart"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botonBack.setOnClickListener{
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }


        // Cargar los datos de calorías y generar el gráfico
        loadCaloriesChart()
    }

    private fun loadCaloriesChart() {

        val caloriesData = CaloriesPayload(
            startDate = "2025-05-01",
            endDate   = "2025-05-10",
            data = listOf(
                CalorieEntry("2025-05-01", 350),
                CalorieEntry("2025-05-03", 420),
                CalorieEntry("2025-05-05", 380),
                CalorieEntry("2025-05-07", 310),
                CalorieEntry("2025-05-09", 450)
            )
        )
        //getSampleCaloriesData()

        // Convertir datos a JSON
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = Gson().toJson(caloriesData).toRequestBody(jsonMediaType)

        // Crear la solicitud POST
        val request = Request.Builder()
            .url(cloudFunctionUrl)
            .post(requestBody)
            .build()

        // Realizar la solicitud
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

                // La respuesta es la imagen JPG directamente
                val responseBody = response.body
                if (responseBody != null) {
                    // Utilizamos Glide para cargar la imagen desde los bytes
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

    private fun getSampleCaloriesData(): Map<String, Any> {

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Fecha final (hoy)
        val endDate = dateFormat.format(calendar.time)

        // Fecha inicial (7 días atrás)
        calendar.add(Calendar.DAY_OF_MONTH, -6)
        val startDate = dateFormat.format(calendar.time)

        // Crear entradas de datos con fechas y calorías aleatorias
        val dataEntries = mutableListOf<Map<String, Any>>()

        for (i in 0..6) {
            val entryDate = dateFormat.format(calendar.time)
            // Calorías aleatorias entre 300 y 500
            val calories = (300 + (Math.random() * 200)).toInt()

            dataEntries.add(mapOf(
                "date" to entryDate,
                "calories" to calories
            ))

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return mapOf(
            "start_date" to startDate,
            "end_date" to endDate,
            "data" to dataEntries
        )
    }


}