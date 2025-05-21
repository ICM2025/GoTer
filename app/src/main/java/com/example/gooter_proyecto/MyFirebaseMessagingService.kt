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

        // Si viene payload de datos, lo procesamos
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            // Extraemos los campos comunes
            val title = remoteMessage.data["title"] ?: "Notificación"
            val body  = remoteMessage.data["body"]  ?: "Tienes una nueva notificación"

            when (remoteMessage.data["type"]) {
                "community_challenge" -> {
                    // Estos campos vienen de nuestra Cloud Function
                    val carreraUid      = remoteMessage.data["carreraUid"]
                    val comunidadId     = remoteMessage.data["comunidadId"]
                    val comunidadNombre = remoteMessage.data["comunidadNombre"]

                    Log.d(TAG, "🏃‍♀️ Nueva carrera $carreraUid en comunidad $comunidadNombre ($comunidadId)")

                    sendCommunityRaceNotification(
                        title,
                        body,
                        carreraUid,
                        comunidadId,
                        comunidadNombre
                    )
                }
                else -> {
                    // Cualquier otra notificación genérica
                    sendGenericNotification(title, body)
                }
            }

        } else if (remoteMessage.notification != null) {
            // Fallback a notification payload si hubiera
            Log.d(TAG, "Message Notification Body: ${remoteMessage.notification!!.body}")
            sendGenericNotification(
                remoteMessage.notification!!.title ?: "Notificación",
                remoteMessage.notification!!.body  ?: ""
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
                FirebaseDatabase.getInstance()
                    .getReference("usuarios")
                    .child(userId)
                    .child("fcmToken")
                    .setValue(token)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token saved to Realtime Database")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to save FCM token", e)
                    }
            } else {
                Log.w(TAG, "User not logged in, cannot save FCM token.")
            }
        }
    }

    /**
     * Notificación específica para carreras de comunidad.
     */
    private fun sendCommunityRaceNotification(
        title: String,
        messageBody: String,
        carreraUid: String?,
        comunidadId: String?,
        comunidadNombre: String?
    ) {
        Log.d(TAG, "Carrera n: $carreraUid")
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
            this,
            carreraUid.hashCode(),
            intent,
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

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si es necesario (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gooter - Carreras en comunidad",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando hay nuevas carreras en tus comunidades"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Usamos hash de carreraUid para tener IDs únicos
        notificationManager.notify(carreraUid.hashCode(), notificationBuilder.build())
    }

    /**
     * Notificación genérica.
     */
    private fun sendGenericNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
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

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gooter - General",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones generales de la app"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
