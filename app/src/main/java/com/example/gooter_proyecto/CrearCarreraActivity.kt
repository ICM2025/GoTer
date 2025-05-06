package com.example.gooter_proyecto

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooter_proyecto.databinding.ActivityCrearCarreraBinding
import kotlin.math.sqrt

class CrearCarreraActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer : Sensor
    private lateinit var accelerometerEventListener: SensorEventListener
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
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    var floatSum = Math.abs(x) + Math.abs(y) + Math.abs(z)
                    if(floatSum >14){
                        binding.busquedaText.text = "Sacudiendo"
                    }
                    else {
                        binding.busquedaText.text = "No sacudiendo"
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        return ret
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            accelerometerEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
            //prueba commit
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accelerometerEventListener)
    }
}