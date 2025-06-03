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
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.abs

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

    // Umbral de fuerza ajustado para mayor sensibilidad
    private val FORCE_THRESHOLD = 150 // Reducido de 350 para mayor sensibilidad
    private val TIME_THRESHOLD = 80   // Reducido de 100 para detección más rápida
    private val SHAKE_TIMEOUT = 500
    private val SHAKE_DURATION = 1000
    private val SHAKE_COUNT = 2       // Reducido de 3 para activarse más fácilmente

    private var mLastX = -1.0f
    private var mLastY = -1.0f
    private var mLastZ = -1.0f
    private var mLastTime: Long = 0
    private var mShakeCount = 0
    private var mLastShake: Long = 0
    private var mLastForce: Long = 0
    private var idUnicoUser : String = ""
    private lateinit var childEventListener: ChildEventListener

    lateinit var binding: ActivityCarreraBinding
    val carrerasRef = database.child("carreras")
    val docRef = database.child("usuariosDisponibles")
    var mapCompetidores = HashMap<String, String>()
    var customKey : String = "Carrera_$uid"

    // Variable para el modo de prueba (útil en emuladores)
    private var testModeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCarreraBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        accelerometerEventListener = createAccelerometerListener()

        // Botón original para crear carreras
        binding.button2.setOnClickListener{
            val intent = Intent(this, CrearCarrerasActivity::class.java).apply {
                putExtra("modo_directo", false)
            }
            startActivity(intent)
        }

        // Botón para volver al home
        binding.imageButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        // Agregamos un botón para forzar la búsqueda (útil en emuladores)
        binding.busquedaText.setOnLongClickListener {
            if (!testModeEnabled) {
                testModeEnabled = true
                Toast.makeText(this, "Modo de prueba activado", Toast.LENGTH_SHORT).show()
            } else {
                // Simular detección de sacudida
                binding.busquedaText.text = "Buscando jugadores"
                binding.listDisponibles.visibility = View.VISIBLE
                agregarALista()
            }
            true
        }

        docRef.limitToFirst(3).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listTemporal = ArrayList<String>()
                for(child : DataSnapshot in snapshot.children) {
                    if(child.child("email").getValue(String::class.java) != email) {
                        val userEmail = child.child("email").getValue(String::class.java)!!
                        listTemporal.add(userEmail)
                        val userUid = child.child("uid").getValue(String::class.java)!!
                        mapCompetidores[userEmail] = userUid
                    }
                }
                addUsersList(listTemporal)
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
        })

        childEventListener = object : ChildEventListener {
            val userCarreraKeyPrefix = "Carrera_$uid"
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Detectar nuevas carreras si se desea
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key
                if (key != null && key.startsWith(userCarreraKeyPrefix)) {
                    if (snapshot.hasChild("location")) {
                        val intent = Intent(baseContext, MapsActivity::class.java).apply {
                            Toast.makeText(baseContext, key, Toast.LENGTH_SHORT).show()
                            putExtra("carrera_id", key)
                        }
                        startActivity(intent)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        binding.listDisponibles.setOnItemClickListener { parent, view, position, id ->
            val email = parent.getItemAtPosition(position).toString()
            val contrarioUid = mapCompetidores[email]
            val customKeyTmp = "Carrera_$contrarioUid"
            customKey = customKeyTmp
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
                Toast.makeText(baseContext, "Error al crear carrera", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addUsersList(listTemporal: ArrayList<String>) {
        binding.listDisponibles.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listTemporal)
    }

    private fun createAccelerometerListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if(event != null) {
                    val now = System.currentTimeMillis()

                    if ((now - mLastForce) > SHAKE_TIMEOUT) {
                        mShakeCount = 0
                    }

                    if((now - mLastTime) > TIME_THRESHOLD) {
                        val diff = now - mLastTime

                        // Cálculo mejorado de aceleración
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        // Calcular la fuerza de manera más precisa
                        val deltaX = x - mLastX
                        val deltaY = y - mLastY
                        val deltaZ = z - mLastZ

                        // Calcular la magnitud del cambio
                        val speed = (abs(deltaX) + abs(deltaY) + abs(deltaZ)) / diff * 10000

                        if (speed > FORCE_THRESHOLD) {
                            if((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
                                mLastShake = now
                                mShakeCount = 0

                                // Log para depuración
                                println("Sacudida detectada con fuerza: $speed")
                                Toast.makeText(baseContext, "¡Sacudida detectada!", Toast.LENGTH_SHORT).show()

                                binding.busquedaText.text = "Buscando jugadores"
                                binding.listDisponibles.visibility = View.VISIBLE
                                sensorManager.unregisterListener(this)
                                agregarALista()
                            }
                            mLastForce = now
                        }

                        mLastTime = now
                        mLastX = x
                        mLastY = y
                        mLastZ = z
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No necesitamos implementar nada aquí
            }
        }
    }

    fun agregarALista() {
        val usuariosDisponiblesRef = database.child("usuariosDisponibles")
        val nuevaReferenciaUsuario = usuariosDisponiblesRef.push()
        nuevaReferenciaUsuario.onDisconnect().removeValue()
        idUnicoUser = nuevaReferenciaUsuario.key!!
        nuevaReferenciaUsuario.setValue(usuario).addOnSuccessListener {
            Toast.makeText(this, "Usuario agregado: $idUnicoUser", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this, "Error al agregar usuario", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Utilizar SENSOR_DELAY_GAME para mayor precisión y velocidad
        sensorManager.registerListener(
            accelerometerEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (idUnicoUser.isNotEmpty()) {
            database.child("usuariosDisponibles").child(idUnicoUser).removeValue()
        }
    }

    override fun onPause() {
        super.onPause()
        carrerasRef.removeEventListener(childEventListener)
        sensorManager.unregisterListener(accelerometerEventListener)
    }
}