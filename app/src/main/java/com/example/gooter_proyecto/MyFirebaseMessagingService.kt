package com.example.gooter_proyecto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Prioriza el manejo de la carga de datos, que es lo que enviaría la Cloud Function
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            // Obtener datos comunes
            val title = remoteMessage.data["title"] ?: "Notificación"
            val body = remoteMessage.data["body"] ?: "Tienes una nueva notificación"

            // Comprobar tipo de notificación
            when (remoteMessage.data["type"]) {
                // Notificación de carrera en comunidad
                "community_challenge" -> {
                    val carreraUid = remoteMessage.data["carreraUid"]
                    val comunidadNombre = remoteMessage.data["comunidadNombre"]
                    val comunidadId = remoteMessage.data["comunidadId"]

                    Log.d(TAG, "Carrera disponible: $carreraUid en comunidad: $comunidadNombre ($comunidadId)")

                    // Enviar notificación con intent específico para abrir la vista de carrera
                    sendCommunityRaceNotification(title, body, carreraUid, comunidadId)
                }

                // Notificación de jugador disponible (caso existente)
                else -> {
                    val correo = remoteMessage.data["correo"]
                    Log.d(TAG, "Notificación estándar - User ID from message: $correo")
                    sendNotification(title, body)
                }
            }
        } else if (remoteMessage.notification != null) {
            Log.d(TAG, "Message Notification Body: ${remoteMessage.notification!!.body}")
            sendNotification(
                remoteMessage.notification!!.title ?: "Título predeterminado",
                remoteMessage.notification!!.body ?: "Mensaje"
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        token?.let {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                // Guarda el token en Realtime Database bajo el ID del usuario
                val database = FirebaseDatabase.getInstance().getReference("usuarios")
                database.child(userId).child("fcmToken").setValue(token)
                    .addOnSuccessListener { Log.d(TAG, "FCM token saved to Realtime Database") }
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to save FCM token", e) }
            } else {
                Log.w(TAG, "User not logged in, cannot save FCM token.")
            }
        }
    }

    /**
     * Crea y muestra una notificación para carreras de comunidad
     */
    private fun sendCommunityRaceNotification(title: String, messageBody: String, carreraUid: String?, comunidadId: String?) {
        // Intent específico para abrir la vista de la carrera
        val intent = Intent(this, MapsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("carrera_id", carreraUid)
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 1 /* Request code diferente para distinguir notificaciones */, intent,
            pendingIntentFlag
        )

        val channelId = getString(R.string.race_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Desde Android Oreo (API 26), los Canales de Notificación son obligatorios.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gooter - Vamos a correr",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones cuando hay carreras disponibles en tus comunidades"
            notificationManager.createNotificationChannel(channel)
        }

        // Usar el ID de carrera como parte del ID de notificación para asegurar que cada carrera tenga su propia notificación
        val notificationId = carreraUid?.hashCode() ?: 1
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Crea y muestra una notificación simple.
     */
    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            pendingIntentFlag
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Desde Android Oreo (API 26), los Canales de Notificación son obligatorios.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Jugador Disponible", // Nombre visible del canal en la configuración del teléfono
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones cuando hay jugadores disponibles"
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* Un ID único para esta notificación */, notificationBuilder.build())
    }
}