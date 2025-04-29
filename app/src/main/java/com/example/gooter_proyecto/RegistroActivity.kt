package com.example.gooter_proyecto

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityRegistroBinding
import java.util.Calendar

class RegistroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ingresarRegistro.setOnClickListener {
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }

        binding.fechaNacimiento.isFocusable = false
        binding.fechaNacimiento.isClickable = true
        binding.fechaNacimiento.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            binding.fechaNacimiento.setText(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }
}
