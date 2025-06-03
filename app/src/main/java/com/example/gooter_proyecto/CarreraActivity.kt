package com.example.gooter_proyecto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gooter_proyecto.databinding.ActivityCarreraBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.abs

class CarreraActivity : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance().reference
    private val email = FirebaseAuth.getInstance().currentUser?.email
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    // Cliente de ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val PROXIMITY_RADIUS_METERS = 400 // 400 metros
    val RADIUS_OF_EARTH_KM = 6378

    // Handler para actualizaciones periódicas de ubicación
    private lateinit var locationUpdateHandler: Handler
    private lateinit var locationUpdateRunnable: Runnable
    private val LOCATION_UPDATE_INTERVAL = 5000L // 5 segundos
    private var isLocationUpdateActive = false

    // Inicializar como HashMap mutable con tipos específicos
    private val usuario = hashMapOf<String, Any?>(
        "email" to email,
        "uid" to uid,
        "latitude" to 0.0,
        "longitude" to 0.0
    )

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
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
    private var idUnicoUser: String = uid ?: ""

    private lateinit var binding: ActivityCarreraBinding
    private val carrerasRef = database.child("carreras")
    private val docRef = database.child("usuariosDisponibles")
    private var mapCompetidores = HashMap<String, String>()
    private var mapCompetidoresLocation = HashMap<String, Pair<Double, Double>>() // Para almacenar ubicaciones
    private var customKey: String = "Carrera_$uid"

    // Variable para el modo de prueba (útil en emuladores)
    private var testModeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCarreraBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar el handler para actualizaciones periódicas
        locationUpdateHandler = Handler(Looper.getMainLooper())
        setupLocationUpdateRunnable()

        // Solicitar permisos de ubicación
        checkLocationPermissions()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        accelerometerEventListener = createAccelerometerListener()

        // Botón original para crear carreras
        binding.button2.setOnClickListener {
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
                //Toast.makeText(this, "Modo de prueba activado", Toast.LENGTH_SHORT).show()
            } else {
                // Simular detección de sacudida
                binding.busquedaText.text = "Buscando jugadores cercanos (200m)"
                binding.listDisponibles.visibility = View.VISIBLE
                getCurrentLocationAndAddToList()
            }
            true
        }

        carrerasRef.addChildEventListener(object : ChildEventListener {
            val userCarreraKeyPrefix = "Carrera_$uid"
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Podríamos detectar nuevas carreras aquí si es necesario
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key
                if (key != null && key.startsWith(userCarreraKeyPrefix)) {
                    if (snapshot.hasChild("location")) {
                        val intent = Intent(baseContext, MapsActivity::class.java).apply {
                     //       Toast.makeText(baseContext, customKey, Toast.LENGTH_SHORT).show()
                            putExtra("carrera_id", customKey)
                        }
                        startActivity(intent)
                    }
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        binding.listDisponibles.setOnItemClickListener { parent, view, position, id ->
            val emailWithDistance = parent.getItemAtPosition(position).toString()

            val email = if (emailWithDistance.contains("(")) {
                emailWithDistance.substring(0, emailWithDistance.indexOf("(")).trim()
            } else {
                emailWithDistance.trim()
            }
            val contrarioUid = mapCompetidores[email]
            Log.i("CONTRARIO ID: ", contrarioUid?: "null")
            // Add null check to prevent NullPointerException
            if (contrarioUid != null && contrarioUid.isNotEmpty()) {
                val customKeyTmp = "Carrera_$contrarioUid"
                customKey = customKeyTmp
                val jugadores = ArrayList<String>()

                // Also add null check for uid
                uid?.let { currentUid ->
                    jugadores.add(currentUid)
                    jugadores.add(contrarioUid)

                    val carrera = mapOf(
                        "jugadores" to jugadores,
                    )

                    database.child("carreras").child(customKey).setValue(carrera)
                        .addOnSuccessListener {
                            val intent = Intent(baseContext, CrearCarrerasActivity::class.java).apply {
                                putExtra("modo_directo", true)
                                putExtra("carrera_id", customKey)
                            }
                            startActivity(intent)
                        }.addOnFailureListener {
                            Toast.makeText(baseContext, "Error al crear carrera", Toast.LENGTH_SHORT).show()
                        }
                } ?: run {
                    Toast.makeText(baseContext, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(baseContext, "Error: No se pudo encontrar el usuario seleccionado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    Toast.makeText(
                        this,
                        "Permisos de ubicación necesarios para encontrar jugadores cercanos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupLocationUpdateRunnable() {
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                if (isLocationUpdateActive) {
                    updateLocationInDatabase()
                    locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (!isLocationUpdateActive) {
            isLocationUpdateActive = true
            locationUpdateHandler.post(locationUpdateRunnable)
          //  Toast.makeText(this, "Actualizaciones de ubicación iniciadas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        isLocationUpdateActive = false
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable)
    }

    private fun updateLocationInDatabase() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude

                    // Actualizar ubicación local
                    updateUsuarioWithLocation()

                    // Actualizar en la base de datos
                    database.child("usuariosDisponibles").child(idUnicoUser).updateChildren(
                        mapOf(
                            "latitude" to currentLatitude,
                            "longitude" to currentLongitude
                        )
                    ).addOnSuccessListener {
                        println("Ubicación actualizada: ${String.format("%.4f", currentLatitude)}, ${String.format("%.4f", currentLongitude)}")
                    }.addOnFailureListener {
                        println("Error al actualizar ubicación: ${it.message}")
                    }
                }
            }
        }
    }

    private fun updateUsuarioWithLocation() {
        usuario["latitude"] = currentLatitude
        usuario["longitude"] = currentLongitude
        usuario["email"] = email
        usuario["uid"] = uid
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude

                    // Actualizar el objeto usuario con la ubicación
                    updateUsuarioWithLocation()

                  /*  Toast.makeText(
                        this,
                        "Ubicación obtenida: ${String.format("%.4f", currentLatitude)}, ${String.format("%.4f", currentLongitude)}",
                        Toast.LENGTH_SHORT
                    ).show()*/
                } ?: run {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterNearbyUsers() {
        val listTemporal = ArrayList<String>()
        docRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                    for (child: DataSnapshot in snapshot.children) {
                        val userEmail = child.child("email").getValue(String::class.java)
                        val userLat = child.child("latitude").getValue(Double::class.java)
                        val userLng = child.child("longitude").getValue(Double::class.java)
                        if (userEmail != email && userLat!= null && userLng != null) {

                            val distance = calculateDistance(currentLatitude, currentLongitude, userLat, userLng)
                            if (distance <= PROXIMITY_RADIUS_METERS) {
                                Log.i("Distance", "Distance: $distance")
                                listTemporal.add("$userEmail (${String.format("%.0f", distance)}m)")
                                mapCompetidores[userEmail!!] = child.key.toString()
                                mapCompetidoresLocation[userEmail!!] = Pair(userLat, userLng)
                                addUsersList(listTemporal)
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                //    Toast.makeText(baseContext, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun calculateDistance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val result = RADIUS_OF_EARTH_KM * c
        return Math.round(result * 100.0) / 100.0
    }

    private fun addUsersList(listTemporal: ArrayList<String>) {
        binding.listDisponibles.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listTemporal)
    }

    private fun createAccelerometerListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    val now = System.currentTimeMillis()

                    if ((now - mLastForce) > SHAKE_TIMEOUT) {
                        mShakeCount = 0
                    }

                    if ((now - mLastTime) > TIME_THRESHOLD) {
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
                            if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
                                mLastShake = now
                                mShakeCount = 0

                                // Log para depuración
                                println("Sacudida detectada con fuerza: $speed")
                                Toast.makeText(baseContext, "¡Sacudida detectada!", Toast.LENGTH_SHORT).show()
                                binding.busquedaText.text = "Buscando jugadores cercanos"
                                filterNearbyUsers()
                                binding.listDisponibles.visibility = View.VISIBLE
                                sensorManager.unregisterListener(this)
                                getCurrentLocationAndAddToList()
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

    private fun getCurrentLocationAndAddToList() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude

                    // Actualizar el objeto usuario con la ubicación actual
                    updateUsuarioWithLocation()

                    // Actualizar la entrada en la base de datos
                    agregarALista()
                } ?: run {
                    Toast.makeText(this, "No se pudo obtener ubicación actual", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Permisos de ubicación necesarios", Toast.LENGTH_SHORT).show()
        }
    }

    fun agregarALista() {
        val usuariosDisponiblesRef = database.child("usuariosDisponibles")

        // Actualizar la entrada del usuario con su UID como clave
        usuariosDisponiblesRef.child(idUnicoUser).setValue(usuario).addOnSuccessListener {
          //  Toast.makeText(this, "Buscando jugadores en tu área (200m)...", Toast.LENGTH_SHORT).show()
            // Iniciar las actualizaciones periódicas de ubicación
            startLocationUpdates()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al buscar jugadores", Toast.LENGTH_SHORT).show()
        }

        // Configurar onDisconnect para eliminar la entrada cuando el usuario se desconecte
        usuariosDisponiblesRef.child(idUnicoUser).onDisconnect().removeValue()
    }

    override fun onResume() {
        super.onResume()
        // Obtener ubicación actual cuando se resume la actividad
        getCurrentLocation()

        // Utilizar SENSOR_DELAY_GAME para mayor precisión y velocidad
        sensorManager.registerListener(
            accelerometerEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener las actualizaciones de ubicación
        stopLocationUpdates()

        // Limpiar la referencia en Firebase
        if (idUnicoUser.isNotEmpty()) {
            database.child("usuariosDisponibles").child(idUnicoUser).removeValue()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accelerometerEventListener)
        // Detener actualizaciones cuando la app se pausa
        stopLocationUpdates()
    }
}