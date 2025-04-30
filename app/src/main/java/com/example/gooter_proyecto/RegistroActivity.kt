package com.example.gooter_proyecto

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityRegistroBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class RegistroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpBinding()
        binding.ingresarRegistro.setOnClickListener {
            validarYRegistrarUsuario()
        }
    }

    private fun validarYRegistrarUsuario() {
        val nombre = binding.nombreRegistro.text.toString()
        val apellidos = binding.apellidosRegistro.text.toString()
        val usuario = binding.usuarioRegistro.text.toString()
        val email = binding.correoRegistro.text.toString()
        val password = binding.contraseARegistro.text.toString()
        val fechaNacimiento = binding.fechaNacimiento.text.toString()

        if (nombre.isEmpty() || apellidos.isEmpty() || usuario.isEmpty() || email.isEmpty() || password.isEmpty() || fechaNacimiento.isEmpty()){
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
        } else {
            registerUser(email, password)
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginUsuarioActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setUpBinding() {
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
