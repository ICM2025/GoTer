package com.example.gooter_proyecto

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityCrearCarreraBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CarreraActivity : AppCompatActivity() {

    val database = FirebaseDatabase.getInstance().reference
    private val email = FirebaseAuth.getInstance().currentUser?.email
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    val usuario = hashMapOf(
        "email" to email,
        "uid" to uid
    )
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer : Sensor
    private lateinit var accelerometerEventListener: SensorEventListener
    private val FORCE_THRESHOLD = 350
    private val TIME_THRESHOLD = 100
    private val SHAKE_TIMEOUT = 500
    private val SHAKE_DURATION = 1000
    private val SHAKE_COUNT = 3
    private var mLastX = -1.0f
    private var mLastY = -1.0f
    private var mLastZ = -1.0f
    private var mLastTime: Long = 0
    private var mShakeCount = 0
    private var mLastShake: Long = 0
    private var mLastForce: Long = 0

    lateinit var binding: ActivityCrearCarreraBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCrearCarreraBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        accelerometerEventListener = createAccelerometerListener()

    }
    private fun createAccelerometerListener(): SensorEventListener {
        val ret : SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event : SensorEvent?) {
                if(event != null ) {
                    val now = System.currentTimeMillis()
                    if ((now - mLastForce) > SHAKE_TIMEOUT) {
                        mShakeCount = 0;
                    }
                    if((now - mLastTime) > TIME_THRESHOLD ) {
                        var diff = now - mLastTime
                        var speed = Math.abs(event.values[0] + event.values[1] + event.values[2] - mLastX - mLastY - mLastZ) / diff * 10000
                        if (speed > FORCE_THRESHOLD) {
                            if((mShakeCount++ >= SHAKE_COUNT) && (now -mLastShake >SHAKE_DURATION)) {
                                mLastShake = now
                                mShakeCount = 0
                                binding.busquedaText.text = "Buscando jugadores"
                                binding.listDisponibles.visibility = View.INVISIBLE
                                agregarALista()
                            }
                            mLastForce = now
                        }
                    }
                    mLastTime = now
                    mLastX = event.values[0]
                    mLastY = event.values[1]
                    mLastZ = event.values[2]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        return ret
    }

    fun agregarALista() {
        database.child("usuariosDisponibles").push().setValue(usuario).addOnSuccessListener {
            Toast.makeText(this, "Usuario agregado", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this, "No fue agregado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            accelerometerEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        //database.child("usuariosDisponibles").removeValue(usuario)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accelerometerEventListener)
    }
}