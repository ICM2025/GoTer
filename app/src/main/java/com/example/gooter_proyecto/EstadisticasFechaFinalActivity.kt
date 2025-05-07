package com.example.gooter_proyecto

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityEstadisticasFechaFinalBinding
import com.example.gooter_proyecto.databinding.ActivityEstadisticasFechaInicioBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EstadisticasFechaFinalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEstadisticasFechaFinalBinding
    private var selectedDate: String? = null
    private var fechaInicio: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasFechaFinalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fechaInicio = intent.getStringExtra("fecha_inicio")

        binding.dateInput.setOnClickListener {
            showDatePicker()
        }
        binding.back.setOnClickListener{
            startActivity(Intent(baseContext, EstadisticasFechaInicioActivity::class.java))
        }
        binding.btnOk.setOnClickListener{
            if (selectedDate == null) {
                Toast.makeText(this, "Por favor selecciona la fecha final", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fechaInicioDate = sdf.parse(fechaInicio!!)
            val fechaFinalDate = sdf.parse(selectedDate!!)

            if (fechaFinalDate.before(fechaInicioDate)) {
                Toast.makeText(this, "La fecha final no puede ser anterior a la fecha de inicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(baseContext, EstadisticasActivity::class.java)
            intent.putExtra("fecha_inicio", fechaInicio)
            intent.putExtra("fecha_final", selectedDate)
            startActivity(intent)
        }

    }



    private fun showDatePicker(){
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Format the date as "yyyy-MM-dd"
            val formattedMonth = String.format("%02d", selectedMonth + 1)
            val formattedDay = String.format("%02d", selectedDay)
            this.selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
            binding.dateInput.setText(this.selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }
}