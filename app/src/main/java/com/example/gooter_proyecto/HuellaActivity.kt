package com.example.gooter_proyecto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class HuellaActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GooterPrefs", Context.MODE_PRIVATE)
        val uidGuardado = prefs.getString("uid_guardado", null)

        // Si no hay UID, redirigir al login manual
        if (uidGuardado == null) {
            Toast.makeText(this, "No hay usuario registrado con huella", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUsuarioActivity::class.java))
            finish()
            return
        }

        // Verificar si el dispositivo puede usar huella
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "La autenticación biométrica no está disponible", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUsuarioActivity::class.java))
            finish()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@HuellaActivity, "Autenticación exitosa", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@HuellaActivity, HomeActivity::class.java)
                    intent.putExtra("usuario_uid", uidGuardado)
                    startActivity(intent)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@HuellaActivity, "Error: $errString", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@HuellaActivity, LoginUsuarioActivity::class.java))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@HuellaActivity, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Iniciar sesión con huella")
            .setSubtitle("Usa tu huella registrada en el dispositivo")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
