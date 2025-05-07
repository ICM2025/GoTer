package com.example.gooter_proyecto

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityEstadisticasFechaInicioBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EstadisticasFechaInicioActivity : AppCompatActivity() {
    private lateinit var binding : ActivityEstadisticasFechaInicioBinding
    private var selectedDate: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasFechaInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dateInput.setOnClickListener {
            showDatePicker()
        }
        binding.back.setOnClickListener{
            startActivity(Intent(baseContext, MainActivity::class.java))
        }
        binding.btnOk.setOnClickListener{
            if (selectedDate == null) {
                Toast.makeText(this, "Por favor selecciona la fecha final", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(baseContext, EstadisticasFechaFinalActivity::class.java)
            intent.putExtra("fecha_inicio", selectedDate)
            startActivity(intent)
        }

    }

    private fun showDatePicker(){
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Establece la fecha seleccionada en el calendario
            calendar.set(selectedYear, selectedMonth, selectedDay)

            // Formatea la fecha como "yyyy-MM-dd"
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = format.format(calendar.time)

            // Muestra la fecha en el TextInputEditText
            binding.dateInput.setText(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }
}