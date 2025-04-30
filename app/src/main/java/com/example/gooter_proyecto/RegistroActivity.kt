package com.example.gooter_proyecto

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityRegistroBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import java.util.Calendar

class RegistroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
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

        if (nombre.isEmpty() || apellidos.isEmpty() || usuario.isEmpty() || email.isEmpty() || password.isEmpty() || fechaNacimiento.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
        } else {
            registerUser(nombre, apellidos, usuario, fechaNacimiento, email, password)
        }
    }

    private fun registerUser(nombre: String, apellidos: String, usuario: String, fechaNacimiento: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    guardarDatosUsuario(nombre, apellidos, usuario, fechaNacimiento)
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginUsuarioActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error en el registro", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun guardarDatosUsuario(nombre: String, apellidos: String, nombreUsuario: String, fechaNacimiento: String) {
        val usuario = FirebaseAuth.getInstance().currentUser
        val uid = usuario?.uid
        if (uid != null) {
            val datosUsuario = mapOf(
                "nombre" to nombre,
                "apellidos" to apellidos,
                "nombreUsuario" to nombreUsuario,
                "fechaNacimiento" to fechaNacimiento,
                "correo" to usuario.email
            )

            val database = Firebase.database.reference
            database.child("usuarios").child(uid)
                .setValue(datosUsuario)
                .addOnSuccessListener {
                    println("Datos de usuario guardados correctamente")
                }
                .addOnFailureListener { e ->
                    println("Error al guardar los datos del usuario: ${e.message}")
                }
        } else {
            println("No hay un usuario autenticado.")
        }
    }

    private fun setUpBinding() {
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
