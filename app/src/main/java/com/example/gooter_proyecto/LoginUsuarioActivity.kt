package com.example.gooter_proyecto

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityLoginUsuarioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class LoginUsuarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginUsuarioBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.buttonIngresar.setOnClickListener {
            validarYAutenticarUsuario()
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }

        binding.botonBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.buttonHuella.setOnClickListener {
            autenticarConHuella()
        }
    }

    private fun validarYAutenticarUsuario() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.editTextEmail.error = "El correo es obligatorio"
            binding.editTextEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Ingrese un correo válido"
            binding.editTextEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.editTextPassword.error = "La contraseña es obligatoria"
            binding.editTextPassword.requestFocus()
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = firebaseAuth.currentUser
                    Toast.makeText(this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()

                    val uid = user?.uid
                    if (uid != null) {
                        val sharedPref = getSharedPreferences("GooterPrefs", MODE_PRIVATE)
                        sharedPref.edit()
                            .putString("uid_guardado", uid)
                            .putString("email_guardado", email)
                            .putString("password_guardado", password)
                            .apply()
                    }

                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(
                        baseContext,
                        "Falló la autenticación: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun autenticarConHuella() {
        val prefs = getSharedPreferences("GooterPrefs", MODE_PRIVATE)
        val email = prefs.getString("email_guardado", null)
        val password = prefs.getString("password_guardado", null)

        if (email == null || password == null) {
            Toast.makeText(this, "No hay credenciales guardadas para autenticación biométrica", Toast.LENGTH_LONG).show()
            return
        }

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            Toast.makeText(this@LoginUsuarioActivity, "Autenticación exitosa", Toast.LENGTH_SHORT).show()

                            firebaseAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val intent = Intent(this@LoginUsuarioActivity, HomeActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this@LoginUsuarioActivity, "Error al iniciar sesión con huella", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(this@LoginUsuarioActivity, "Error: $errString", Toast.LENGTH_LONG).show()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(this@LoginUsuarioActivity, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                        }
                    })

                promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Iniciar sesión con huella")
                    .setSubtitle("Usa tu huella registrada en el dispositivo")
                    .setNegativeButtonText("Cancelar")
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No hay huellas registradas en este dispositivo", Toast.LENGTH_LONG).show()
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "La autenticación biométrica no está disponible", Toast.LENGTH_LONG).show()
            }
        }
    }
}
