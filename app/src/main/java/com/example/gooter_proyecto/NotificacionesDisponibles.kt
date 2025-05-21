package com.example.gooter_proyecto

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class NotificacionesDisponibles {
    private val TAG = "NotificacionesDisponibles"

    companion object {
        // Instancia singleton
        private var instance: NotificacionesDisponibles? = null

        fun getInstance(): NotificacionesDisponibles {
            if (instance == null) {
                instance = NotificacionesDisponibles()
            }
            return instance!!
        }
    }

    // Esta función debe llamarse al iniciar la aplicación
    fun inicializar(context: Context) {
        actualizarTokenFCM()
    }

    // Actualiza el token FCM en la base de datos de Firebase
    private fun actualizarTokenFCM() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "No se pudo obtener el token FCM", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            // Guardar token en la base de datos
            val database = FirebaseDatabase.getInstance().getReference("usuarios")
            database.child(userId).child("fcmToken").setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "Token FCM actualizado: $token")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar token FCM", e)
                }
        }
    }
}