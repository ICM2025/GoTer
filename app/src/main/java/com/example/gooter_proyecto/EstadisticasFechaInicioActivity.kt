package com.example.gooter_proyecto

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityEstadisticasFechaInicioBinding
import java.util.Calendar

class EstadisticasFechaInicioActivity : AppCompatActivity() {
    private lateinit var binding : ActivityEstadisticasFechaInicioBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityEstadisticasFechaInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.dateInput.setOnClickListener{
            showDatePicker()
        }
        binding.back.setOnClickListener{
            startActivity(Intent(baseContext, MainActivity::class.java))
        }
        binding.siguiente.setOnClickListener {
            startActivity(Intent(baseContext, EstadisticasFechaFinalActivity::class.java))
        }
    }

    private fun showDatePicker(){
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Formatear la fecha y mostrarla en el TextInputEditText
            val selectedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
            binding.dateInput.setText(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }
}