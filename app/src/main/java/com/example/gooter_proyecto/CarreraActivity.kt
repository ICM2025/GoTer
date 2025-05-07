package com.example.gooter_proyecto

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gooter_proyecto.databinding.ActivityCarreraBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import models.Usuario

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
    private var idUnicoUser : String = ""
    lateinit var binding: ActivityCarreraBinding
    val docRef = database.child("usuariosDisponibles")
    var mapCompetidores = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCarreraBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        accelerometerEventListener = createAccelerometerListener()
        binding.button2.setOnClickListener{
            val intent = Intent(this, CrearCarrerasActivity::class.java).apply {
                putExtra("modo_directo", false)
            }
            startActivity(intent)
        }
        binding.imageButton.setOnClickListener {
            startActivity(Intent(this,HomeActivity::class.java))
        }
        docRef.limitToFirst(3).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var listTemporal = ArrayList<String>()
                for(child : DataSnapshot in snapshot.children) {
                    if(child.child("email").getValue(String::class.java) != email) {
                        var email = child.child("email").getValue(String::class.java)!!
                        listTemporal.add(email)
                        var uid = child.child("uid").getValue(String::class.java)!!
                        mapCompetidores = hashMapOf(
                            email to uid
                        )
                    }
                }
                addUsersList(listTemporal)
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
        binding.listDisponibles.setOnItemClickListener { parent, view, position, id ->
            val email = parent.getItemAtPosition(position).toString()
            var contrarioUid = mapCompetidores[email]
            val customKey = "Carrera_$contrarioUid"
            val jugadores = ArrayList<String>()
            jugadores.add(uid!!)
            jugadores.add(contrarioUid!!)
            val carrera = mapOf(
                "jugadores" to jugadores,
            )
            database.child("carreras").child(customKey).setValue(carrera).
            addOnSuccessListener {
                val intent = Intent(baseContext, CrearCarrerasActivity::class.java).apply {
                    putExtra("modo_directo", true)
                    putExtra("carrera_id", customKey)
                }
                startActivity(intent)
            }.addOnFailureListener{
            }
        }
    }

    private fun addUsersList(listTemporal: ArrayList<String>) {
        binding.listDisponibles.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listTemporal)
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
                                binding.listDisponibles.visibility = View.VISIBLE
                                sensorManager.unregisterListener(accelerometerEventListener)
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
        val usuariosDisponiblesRef = database.child("usuariosDisponibles")
        val nuevaReferenciaUsuario = usuariosDisponiblesRef.push()
        nuevaReferenciaUsuario.onDisconnect().removeValue()
        idUnicoUser = nuevaReferenciaUsuario.key!!
        nuevaReferenciaUsuario.setValue(usuario).addOnSuccessListener {
            Toast.makeText(this, "Usuario agregado" + idUnicoUser, Toast.LENGTH_SHORT).show()
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
        database.child("usuariosDisponibles").child(idUnicoUser).removeValue()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accelerometerEventListener)
    }
}