package com.example.gooter_proyecto

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.gooter_proyecto.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

class MapsActivity : AppCompatActivity() {

    val RADIUS_OF_EARTH_KM = 6378
    private lateinit var binding: ActivityMapsBinding
    private lateinit var map: MapView
    private val bogota = GeoPoint(4.62, -74.07)
    private lateinit var geocoder: Geocoder
    private var routeOverlay: Polyline? = null
    lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: GeoPoint? = null
    private var currentLocationMarker: Marker? = null
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var sensorEventListener: SensorEventListener
    private var destinationLocation: GeoPoint? = null
    private var destinationMarker: Marker? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var locationUpdateHandler: Handler
    private lateinit var locationUpdateRunnable: Runnable
    private var carreraId: String = ""
    private var carreraEnCurso = false
    private var distanciaRecorrida = 0.0
    private var ultimaUbicacion: GeoPoint? = null
    private var carreraDestino: GeoPoint? = null
    private var permisoSolicitado = false
    private var gpsDialogShown = false
    private lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null
    private val participantMarkers = mutableMapOf<String, Marker>()
    private var tiempoInicioCarrera: Long = 0
    private var tiempoActividad: Long = 0 // en segundos
    private var cronometroRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private var velocidadMaxima: Double = 0.0
    private var ultimaPosicionCamara: GeoPoint? = null
    private var admin : Boolean = false


    private val estacionamientoMarkers: MutableList<Marker> = mutableListOf()

    val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if (it.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "El GPS está apagado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            permisoSolicitado = true
            if (it) {
                locationSettings()
            }
        }
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notificaciones permitidas en MapMenu", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Las notificaciones están deshabilitadas en MapMenu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        roadManager = OSRMRoadManager(this, "ANDROID")
        gpsDialogShown = false
        auth = FirebaseAuth.getInstance()
        // Solicita permiso para notificaciones
        NotificacionesDisponibles.getInstance().inicializar(this)
        carreraId = intent.getStringExtra("carrera_id") ?: ""
        admin = intent.getBooleanExtra("admin", false)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            FirebaseDatabase.getInstance().reference.child("usuarios").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
                        binding.textSaludo.text = "Hola, $nombre!"
                    } else {
                        Toast.makeText(this, "No se encontró el usuario", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
                }
        }

        if (carreraId.isNotEmpty())
        {
            binding.normalLayout.visibility = View.GONE
            binding.goOnlyButton.visibility = View.VISIBLE
            iniciarCronometro()
            registrarEnCarrera()
            cargarDestinoCarrera()
            observarParticipantes()
            observarEstadoCarrera()
            binding.goOnlyButton.setOnClickListener {
                if (admin) {
                    if (!carreraEnCurso) {
                        FirebaseDatabase.getInstance().getReference("carreras").child(carreraId).child("estado")
                            .setValue("en_curso")
                        borrarNotificacionesAsociadas()
                        Toast.makeText(this, "¡Carrera iniciada!", Toast.LENGTH_SHORT).show()
                        carreraEnCurso = true
                        binding.goOnlyButton.text = "FINISH RACE"
                    } else {
                        finalizarCarreraYRegistrarEstadisticas()
                        detenerCronometro()
                    }
                }
                else {
                    Toast.makeText(baseContext,"Para hacer esto debes ser administrador", Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            binding.normalLayout.visibility = View.VISIBLE
            binding.goOnlyButton.visibility = View.GONE

            cargarEstacionamientos()

            binding.botonGo.setOnClickListener {
                val searchSuccessful = searchLocation()
                if (searchSuccessful && destinationLocation != null && currentLocation != null) {
                    drawDirectLine(currentLocation!!, destinationLocation!!)
                } else if (!searchSuccessful) {
                    // searchLocation already shows a toast if search fails
                } else {
                    Toast.makeText(this, "No se pudo trazar la ruta: ubicación actual desconocida.", Toast.LENGTH_SHORT).show()
                }
            }

            binding.editUbicacion.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    searchLocation()
                    hideKeyboard()
                    true
                }
                false
            }
            binding.iconoEditar.setOnClickListener {
                binding.editUbicacion.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.editUbicacion, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorEventListener = createSensorEventListener()

        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )
        map = binding.osmMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        geocoder = Geocoder(this)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        inicializarSuscrLocalizacion()

        binding.btnMyLocation.setOnClickListener {
            goToMyLocation()
        }
        binding.botonBack.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        auth = FirebaseAuth.getInstance()
        locationUpdateHandler = Handler(Looper.getMainLooper())
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                guardarUsuarioUbicacionFirebase()
                locationUpdateHandler.postDelayed(this, 7000)
            }
        }

        askNotificationPermission()

    }

    private fun inicializarSuscrLocalizacion() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()
        suscribirLocalizacion()
    }

    private fun suscribirLocalizacion() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Permiso concedido, continuar con la lógica de localización
            locationSettings()
        } else {
            // Permiso no concedido
            if (!permisoSolicitado && shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Mostrar explicación si el usuario ya lo denegó antes
                Toast.makeText(
                    this,
                    "El permiso es necesario para acceder a las funciones de localización.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Solicitar el permiso (ya sea la primera vez o después de la explicación)
            if (!permisoSolicitado) {
                locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun cargarDestinoCarrera() {
        val carreraRef = FirebaseDatabase.getInstance().getReference("carreras").child(carreraId)
        carreraRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val destino = snapshot.child("location")
                val lat = destino.child("latitud").getValue(Double::class.java) ?: return
                val lon = destino.child("longitud").getValue(Double::class.java) ?: return
                val alt = destino.child("altitud").getValue(Double::class.java) ?: 0.0

                carreraDestino = GeoPoint(lat, lon, alt)
                destinationLocation = carreraDestino

                destinationMarker = createMarker(
                    carreraDestino!!,
                    "Destino de la carrera",
                    "Punto final de la competencia",
                    R.drawable.baseline_location_alt_24
                )
                destinationMarker?.let {
                    map.overlays.add(it)
                    map.invalidate()
                }

                currentLocation?.let {
                    drawDirectLine(it, carreraDestino!!)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MapsActivity, "Error al cargar destino de carrera", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun guardarUsuarioUbicacionFirebase() {
        val user = auth.currentUser
        val uid = user?.uid

        if (uid != null && currentLocation != null) {
            val database = FirebaseDatabase.getInstance()
            val userLocationRef = database.getReference("usuarios").child(uid).child("ubicacion")

            val locationData = hashMapOf(
                "latitud" to currentLocation!!.latitude,
                "longitud" to currentLocation!!.longitude,
                "altitud" to currentLocation!!.altitude
            )

            userLocationRef.setValue(locationData)
                .addOnSuccessListener {
                    Log.d("Firebase", "Location saved successfully for user: $uid")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Failed to save location for user: $uid", e)
                }
        } else {
            Log.d("Firebase", "User not logged in or current location not available.")
        }
    }

    private fun startSavingLocationUpdates() {
        locationUpdateHandler.post(locationUpdateRunnable)
    }

    private fun stopSavingLocationUpdates() {
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable)
    }

    private fun cargarEstacionamientos() {
        val database = FirebaseDatabase.getInstance()
        val estacionamientoRef = database.getReference("estacionamientos")

        estacionamientoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                estacionamientoMarkers.forEach { marker ->
                    map.overlays.remove(marker)
                }
                estacionamientoMarkers.clear()


                for (estacionamientoSnapshot in snapshot.children) {
                    try {
                        val parkingId = estacionamientoSnapshot.key ?: "Unknown Parking"
                        val ubicacionSnapshot = estacionamientoSnapshot.child("ubicacion")
                        val latitud = ubicacionSnapshot.child("latitud").getValue(Double::class.java)
                        val longitud = ubicacionSnapshot.child("longitud").getValue(Double::class.java)
                        val altitud = ubicacionSnapshot.child("altitud").getValue(Double::class.java)
                        val capacidad = estacionamientoSnapshot.child("capacidad").getValue(Int::class.java)
                        val disponibilidad = estacionamientoSnapshot.child("disponibilidad").getValue(Boolean::class.java)
                        val nombre = parkingId
                        if (latitud != null && longitud != null) {
                            val punto = if (altitud != null) {
                                GeoPoint(latitud, longitud, altitud)
                            } else {
                                GeoPoint(latitud, longitud)
                            }

                            val marker = createMarker(
                                punto,
                                nombre,
                                "Capacidad: ${capacidad ?: "N/A"}, Disponible: ${disponibilidad ?: "N/A"}",
                                R.drawable.star_ic
                            )

                            marker?.let {
                                map.overlays.add(it)
                                estacionamientoMarkers.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FIREBASE", "Error al cargar estacionamiento: ${e.message}")
                        Log.e("FIREBASE", "Error details for snapshot: ${estacionamientoSnapshot.key}")
                    }
                }

                map.invalidate()
                Toast.makeText(baseContext, "Se cargaron ${estacionamientoMarkers.size} estacionamientos", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Error al cargar estacionamientos: ${error.message}")
                Toast.makeText(baseContext, "Error al cargar estacionamientos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRouteButton() {
    }

    private fun borrarNotificacionesAsociadas() {
        val db = FirebaseDatabase.getInstance().getReference("notificaciones")
        db.get().addOnSuccessListener { snapshot ->
            for (noti in snapshot.children) {
                val meta = noti.child("metadatos")
                if (meta.child("idCarrera").getValue(String::class.java) == carreraId) {
                    noti.ref.removeValue()
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun finalizarCarreraYRegistrarEstadisticas() {
        val uid = auth.currentUser?.uid ?: return
        val hoy = LocalDate.now()
        val fechaHoy = hoy.toString() // 2025-05-25
        val mesActual = hoy.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")) // 2025-05
        val semanaActual = "${hoy.year}-S${hoy.get(WeekFields.ISO.weekOfYear())}" // 2025-S21

        // Calcular estadísticas de la sesión actual
        val tiempoEnSegundos = tiempoActividad
        val velocidadMediaKmh = if (tiempoEnSegundos > 0) (distanciaRecorrida * 3600) / tiempoEnSegundos else 0.0
        val caloriasGastadas = calcularCalorias(distanciaRecorrida, tiempoEnSegundos, velocidadMaxima)
        val distanciaEnMetros = (distanciaRecorrida * 1000).toInt()
        val puntosGanados = (distanciaEnMetros * 0.5).toInt()

        val database = FirebaseDatabase.getInstance()
        val statsRef = database.getReference("estadisticasUsuarios").child(uid)
        val userRef = database.getReference("usuarios").child(uid)

        // Actualizar estadísticas diarias
        actualizarEstadisticasDiarias(statsRef, fechaHoy, caloriasGastadas, velocidadMediaKmh)

        // Actualizar estadísticas semanales
        actualizarEstadisticasSemanales(statsRef, semanaActual, fechaHoy, caloriasGastadas)

        // Actualizar estadísticas mensuales
        actualizarEstadisticasMensuales(statsRef, mesActual, caloriasGastadas)

        // Actualizar resumen total
        actualizarResumenTotal(statsRef, caloriasGastadas, tiempoEnSegundos)

        // Actualizar puntos del usuario
        actualizarPuntosUsuario(userRef, puntosGanados)

        // Limpiar carrera actual
        database.getReference("carreras").child(carreraId).removeValue()

        Toast.makeText(this, "Carrera finalizada. Distancia: ${distanciaRecorrida}km, Puntos ganados: $puntosGanados", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
        val carreraRef = FirebaseDatabase.getInstance().getReference("carreras").child(carreraId)
        carreraRef.removeValue()
    }

    private fun actualizarEstadisticasDiarias(statsRef: DatabaseReference, fecha: String, calorias: Double, velocidadMedia: Double) {
        val diariaRef = statsRef.child("diarias").child(fecha)

        diariaRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.value as? Map<String, Any> ?: mutableMapOf()

                val nuevasStats = mutableMapOf<String, Any>(
                    "distanciaRecorrida" to ((stats["distanciaRecorrida"] as? Double ?: 0.0) + distanciaRecorrida),
                    "tiempoActividad" to ((stats["tiempoActividad"] as? Long ?: 0L) + tiempoActividad),
                    "caloriasGastadas" to ((stats["caloriasGastadas"] as? Double ?: 0.0) + calorias),
                    "velocidadMedia" to velocidadMedia,
                    "velocidadMaxima" to maxOf(stats["velocidadMaxima"] as? Double ?: 0.0, velocidadMaxima)
                )

                currentData.value = nuevasStats
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("EstadisticasDiarias", "Error: ${error.message}")
                }
            }
        })
    }

    private fun actualizarEstadisticasSemanales(statsRef: DatabaseReference, semana: String, fecha: String, calorias: Double) {
        val semanalRef = statsRef.child("semanales").child(semana)

        semanalRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.value as? Map<String, Any> ?: mutableMapOf()

                val nuevasStats = mutableMapOf<String, Any>(
                    "distanciaRecorrida" to ((stats["distanciaRecorrida"] as? Double ?: 0.0) + distanciaRecorrida),
                    "tiempoActividad" to ((stats["tiempoActividad"] as? Long ?: 0L) + tiempoActividad),
                    "caloriasGastadas" to ((stats["caloriasGastadas"] as? Double ?: 0.0) + calorias),
                    "mejorDia" to (stats["mejorDia"] as? String ?: fecha)
                )

                currentData.value = nuevasStats
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("EstadisticasSemanales", "Error: ${error.message}")
                }
            }
        })
    }

    private fun actualizarEstadisticasMensuales(statsRef: DatabaseReference, mes: String, calorias: Double) {
        val mensualRef = statsRef.child("mensuales").child(mes)

        mensualRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.value as? Map<String, Any> ?: mutableMapOf()

                val nuevasStats = mutableMapOf<String, Any>(
                    "distanciaRecorrida" to ((stats["distanciaRecorrida"] as? Double ?: 0.0) + distanciaRecorrida),
                    "tiempoActividad" to ((stats["tiempoActividad"] as? Long ?: 0L) + tiempoActividad),
                    "caloriasGastadas" to ((stats["caloriasGastadas"] as? Double ?: 0.0) + calorias),
                    "carrerasParticipadas" to ((stats["carrerasParticipadas"] as? Long ?: 0L) + 1),
                    "carrerasGanadas" to (stats["carrerasGanadas"] as? Long ?: 0L) // Se actualiza cuando gane una carrera
                )

                currentData.value = nuevasStats
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("EstadisticasMensuales", "Error: ${error.message}")
                }
            }
        })
    }

    private fun actualizarResumenTotal(statsRef: DatabaseReference, calorias: Double, tiempo: Long) {
        val totalRef = statsRef.child("resumenTotal")

        totalRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.value as? Map<String, Any> ?: mutableMapOf()

                val nuevasStats = mutableMapOf<String, Any>(
                    "distanciaTotal" to ((stats["distanciaTotal"] as? Double ?: 0.0) + distanciaRecorrida),
                    "tiempoTotal" to ((stats["tiempoTotal"] as? Long ?: 0L) + tiempo),
                    "caloriasTotal" to ((stats["caloriasTotal"] as? Double ?: 0.0) + calorias),
                    "carrerasGanadas" to (stats["carrerasGanadas"] as? Long ?: 0L),
                    "mejorTiempo" to minOf(stats["mejorTiempo"] as? Long ?: Long.MAX_VALUE, tiempo)
                )

                currentData.value = nuevasStats
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("ResumenTotal", "Error: ${error.message}")
                }
            }
        })
    }

    private fun actualizarPuntosUsuario(userRef: DatabaseReference, puntosGanados: Int) {
        userRef.child("puntos").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val puntosActuales = currentData.value as? Long ?: 0L
                currentData.value = puntosActuales + puntosGanados
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("PuntosUsuario", "Error: ${error.message}")
                }
            }
        })
    }

    private fun calcularCalorias(distanciaKm: Double, tiempoSegundos: Long, velocidadMaxKmh: Double): Double {
        // Fórmula aproximada: MET * peso * tiempo_horas
        // Para ciclismo: MET base = 8.0, ajustado por velocidad
        val tiempoHoras = tiempoSegundos / 3600.0
        val pesoPromedio = 70.0 // kg (puedes hacer esto configurable por usuario)
        val velocidadPromedio = if (tiempoHoras > 0) distanciaKm / tiempoHoras else 0.0

        val met = when {
            velocidadPromedio < 16 -> 6.0  // Ritmo ligero
            velocidadPromedio < 20 -> 8.0  // Ritmo moderado
            velocidadPromedio < 25 -> 10.0 // Ritmo vigoroso
            else -> 12.0                   // Ritmo muy vigoroso
        }

        return met * pesoPromedio * tiempoHoras
    }


    private fun registrarEnCarrera() {
        val uid = auth.currentUser?.uid ?: return
        val carreraRef = FirebaseDatabase.getInstance().getReference("carreras").child(carreraId)

        // Verificar si el usuario ya está registrado en la carrera
        carreraRef.child("jugadores").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val participantes = mutableListOf<String>()

                // Obtener participantes existentes
                for (child in snapshot.children) {
                    val participante = child.getValue(String::class.java)
                    if (participante != null) {
                        participantes.add(participante)
                    }
                }

                // Verificar si el usuario actual ya está en la lista
                if (!participantes.contains(uid)) {
                    // Agregar el usuario actual a la lista
                    participantes.add(uid)

                    // Actualizar la lista en Firebase
                    carreraRef.child("jugadores").setValue(participantes)
                        .addOnSuccessListener {
                            Log.d("CARRERA", "Usuario agregado exitosamente a la carrera")
                        }
                        .addOnFailureListener { e ->
                            Log.e("CARRERA", "Error al agregar usuario a la carrera: ${e.message}")
                        }
                } else {
                    Log.d("CARRERA", "Usuario ya está registrado en la carrera")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CARRERA", "Error al verificar participantes: ${error.message}")
            }
        })
    }

    private fun drawRouteAfterDelay() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                Thread.sleep(500)
            }
            if (currentLocation != null && destinationLocation != null) {
                map.controller.animateTo(destinationLocation)
                map.controller.setZoom(16.0)

                drawDirectLine(currentLocation!!, destinationLocation!!)
            }
        }
    }

    private fun goToMyLocation() {
        currentLocation?.let { current ->
            map.controller.animateTo(current)
            map.controller.setZoom(18.0)
        }
    }

    private fun searchLocation(): Boolean {
        val text = binding.editUbicacion.text.toString().trim()

        if (text.isEmpty()) {
            Toast.makeText(this, "Ingresa una dirección para buscar", Toast.LENGTH_SHORT).show()
            return false
        }

        val latlong = findLocation(text)

        return if (this@MapsActivity::map.isInitialized && latlong != null) {
            val loc = GeoPoint(latlong.latitude, latlong.longitude)
            val address = findAddress(latlong)

            Log.i("SEARCH", "ADDRESS: $address")

            routeOverlay?.let {
                map.overlays.remove(it)
                routeOverlay = null
            }

            destinationLocation = loc

            destinationMarker?.let {
                map.overlays.remove(it)
            }

            if (!address.isNullOrEmpty()) {
                Log.d("MAP", "Moviendo a ubicacion buscada")
                map.controller.animateTo(loc)
                map.controller.setZoom(17.0)

                destinationMarker = createMarker(
                    loc,
                    "Destino",
                    address,
                    R.drawable.baseline_location_alt_24
                )
                destinationMarker?.let {
                    map.overlays.add(it)
                }

                showDistanceBetweenPoints(currentLocation, loc)

                map.invalidate()
                true
            } else {
                Toast.makeText(
                    baseContext,
                    "No se encontró información para esta ubicación",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
        } else {
            Toast.makeText(
                baseContext,
                "No se encontró la ubicación",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun showDistanceBetweenPoints(startPoint: GeoPoint?, endPoint: GeoPoint?) {
        if (startPoint != null && endPoint != null) {
            val distancia = distance(
                startPoint.latitude,
                startPoint.longitude,
                endPoint.latitude,
                endPoint.longitude
            )
            Toast.makeText(
                baseContext,
                "Distancia: $distancia km",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editUbicacion.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            sensorEventListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        map.onResume()
        map.controller.setZoom(18.0)

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationSettings()
        } else if (!permisoSolicitado) {
            locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val uims = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uims.nightMode == UiModeManager.MODE_NIGHT_YES) {
            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }

        currentLocation?.let {
            Log.d("MAP", "Moviendo a ubicacion actual desde onResume")
            map.controller.animateTo(it)
            // Solo ajustar la ruta en modo carrera si ultimaPosicionCamara es nula (inicio)
            if (carreraId.isNotEmpty() && carreraDestino != null && ultimaPosicionCamara == null) {
                adjustZoomToShowRoute(it, carreraDestino!!)
                Log.d("MAP", "Ajustando zoom para mostrar ruta en modo carrera (inicio)")
            }
        } ?: run {
            map.controller.animateTo(bogota)
        }

        startSavingLocationUpdates()
    }

    private fun createSensorEventListener(): SensorEventListener {
        val sel = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (this@MapsActivity::map.isInitialized) {
                    if (event != null) {
                        if (event.values[0] > 5000) {
                            map.overlayManager.tilesOverlay.setColorFilter(null)
                        } else {
                            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        return sel
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MapMenuActivity", "Notification permission already granted.")
                // Ya tienes el permiso
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                // Opcional: Muestra una UI explicando por qué necesitas el permiso.
                // Por ahora, solo lo solicitamos.
                Log.d("MapMenuActivity", "Showing rationale or requesting permission.")
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Solicita el permiso
                Log.d("MapMenuActivity", "Requesting notification permission.")
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()
        sensorManager.unregisterListener(sensorEventListener)
        stopSavingLocationUpdates()
    }

    fun createMarker(p: GeoPoint, title: String, desc: String, iconID: Int): Marker? {
        var marker: Marker? = null;
        if (map != null) {
            marker = Marker(map);
            marker.title = title
            marker.subDescription = desc
            if (iconID != 0) {
                val myIcon = resources.getDrawable(iconID, this.theme)
                marker.icon = myIcon
            }
            marker.position = p
            marker.setAnchor(
                Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM
            );
        }
        return marker
    }

    private fun drawDirectLine(start: GeoPoint, end: GeoPoint) {
        val routePoints = ArrayList<GeoPoint>()

        // Remover overlay anterior si existe
        roadOverlay?.let {
            map.overlays.remove(it)
            roadOverlay = null
        }

        routePoints.add(start)
        routePoints.add(end)
        val road = roadManager.getRoad(routePoints)
        roadOverlay = RoadManager.buildRoadOverlay(road).apply {
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 10f
        }

        map.overlays.add(roadOverlay)
        map.invalidate()
    }

    private fun adjustZoomToShowRoute(start: GeoPoint, end: GeoPoint) {
        val north = Math.max(start.latitude, end.latitude)
        val south = Math.min(start.latitude, end.latitude)
        val east = Math.max(start.longitude, end.longitude)
        val west = Math.min(start.longitude, end.longitude)

        val latSpan = (north - south) * 1.5
        val lonSpan = (east - west) * 1.5

        val newNorth = Math.min(north + latSpan * 0.25, 90.0)
        val newSouth = Math.max(south - latSpan * 0.25, -90.0)
        val newEast = Math.min(east + lonSpan * 0.25, 180.0)
        val newWest = Math.max(west - lonSpan * 0.25, -180.0)

        val centerLat = (newNorth + newSouth) / 2
        val centerLon = (newEast + newWest) / 2

        map.zoomToBoundingBox(org.osmdroid.util.BoundingBox(
            newNorth, newEast, newSouth, newWest
        ), true, 100)
    }

    fun locationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            startLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val isr: IntentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettings.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "El dispositivo no tiene GPS", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest(): LocationRequest {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return request
    }

    private fun createLocationCallback(): LocationCallback {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                if (loc != null) {
                    val velocidadActual = loc.speed.toDouble()

                    // 2. Comparar y actualizar máxima
                    if (velocidadActual > velocidadMaxima) {
                        velocidadMaxima = velocidadActual
                    }
                    updateUI(loc)
                }
            }
        }
        return callback
    }

    fun updateUI(location: Location) {
        val newLocation = GeoPoint(location.latitude, location.longitude)
        val address = findAddress(LatLng(location.latitude, location.longitude))
        var moverCamara = false

        // Calcular distancia recorrida
        ultimaUbicacion?.let {
            val d = distance(it.latitude, it.longitude, newLocation.latitude, newLocation.longitude)
            distanciaRecorrida += d
            Log.i("MAPA", "Distancia desde anterior actualizacion: $d")
        }
        ultimaUbicacion = newLocation

        // Determinar si mover la cámara (solo si cambió más de 50 metros)
        ultimaPosicionCamara?.let { ultimaPos ->
            val distanciaCamara = distance(
                ultimaPos.latitude, ultimaPos.longitude,
                newLocation.latitude, newLocation.longitude
            )
            if (distanciaCamara > 0.05) { // 50 metros = 0.05 km
                moverCamara = true
                ultimaPosicionCamara = newLocation
            }
        } ?: run {
            // Primera vez, mover cámara
            moverCamara = true
            ultimaPosicionCamara = newLocation
        }

        currentLocation = newLocation
        updateCurrentLocationMarker(newLocation, address)

        if (moverCamara) {
            map.controller.animateTo(newLocation)
            Log.i("MAPA", "Moviendo cámara a ubicación actual (cambio >50m)")
        }

        // Redibujar ruta si existe destino y estamos en modo carrera
        if (carreraId.isNotEmpty() && carreraDestino != null) {
            // Borrar ruta anterior
            roadOverlay?.let { overlay ->
                map.overlays.remove(overlay)
                roadOverlay = null
            }
            // Dibujar nueva ruta
            drawDirectLine(newLocation, carreraDestino!!)
        }
        revisarVictoria(newLocation)
        map.invalidate()
    }

    fun revisarVictoria(newLocation : GeoPoint) {
        val distancia  = distance(newLocation.latitude, newLocation.longitude, carreraDestino!!.latitude, carreraDestino!!.longitude)
        Log.i("MAPA", "Distancia a destino: $distancia")
        if (distancia < 0.4 ){
            val ref = FirebaseDatabase.getInstance().getReference("carreras")
                .child(carreraId)
                .child("estado")
            ref.setValue("finalizada")
            finalizarCarreraYRegistrarEstadisticas()
            detenerCronometro()
        }
    }

    private fun updateCurrentLocationMarker(location: GeoPoint, address: String?) {
        currentLocationMarker?.let {
            map.overlays.remove(it)
        }

        val user = auth.currentUser
        val uid = user?.uid

        if (uid != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("usuarios").child(uid)

            userRef.child("urlFotoPerfil").get().addOnSuccessListener { snapshot ->
                val urlFotoPerfil = snapshot.getValue(String::class.java)

                if (!urlFotoPerfil.isNullOrEmpty()) {
                    // Si tiene foto de perfil, cargarla y usarla como icono
                    loadProfileImageAsMarker(location, address, urlFotoPerfil)
                } else {
                    // Si no tiene foto de perfil, usar el icono por defecto
                    createAndAddMarkerWithDefaultIcon(location, address)
                }
            }.addOnFailureListener {
                // En caso de error, usar el icono por defecto
                createAndAddMarkerWithDefaultIcon(location, address)
            }
        } else {
            // Si no hay usuario autenticado, usar icono por defecto
            createAndAddMarkerWithDefaultIcon(location, address)
        }
    }

    private fun loadProfileImageAsMarker(location: GeoPoint, address: String?, imageUrl: String) {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .placeholder(R.drawable.baseline_directions_bike_24)
            .error(R.drawable.baseline_directions_bike_24)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val resizedBitmap = resizeBitmapForMarker(resource, false) // false = usuario actual (rojo)

                    createMarkerWithCustomIcon(location, address, resizedBitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    createAndAddMarkerWithDefaultIcon(location, address)
                }
            })
    }

    private fun createMarkerWithCustomIcon(location: GeoPoint, address: String?, bitmap: Bitmap) {
        val marker = Marker(map)
        marker.title = "Tu ubicación"
        marker.subDescription = address ?: "Estás aquí"
        marker.position = location
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        val drawable = BitmapDrawable(resources, bitmap)
        marker.icon = drawable

        map.overlays.add(marker)
        currentLocationMarker = marker
        map.invalidate()
    }
    private fun createAndAddMarkerWithDefaultIcon(location: GeoPoint, address: String?) {
        val newMarker = createMarker(
            location,
            "Tu ubicación",
            address ?: "Estás aquí",
            R.drawable.baseline_directions_bike_24
        )

        newMarker?.let {
            map.overlays.add(it)
            currentLocationMarker = it
            map.invalidate()
        }
    }

    private fun resizeBitmapForMarker(bitmap: Bitmap, isRival: Boolean = false): Bitmap {
        val size = (50 * resources.displayMetrics.density).toInt()  // Era 80
        val markerSize = (70 * resources.displayMetrics.density).toInt()  // Era 100

        // Crear un canvas para dibujar el marcador personalizado
        val markerBitmap = Bitmap.createBitmap(markerSize, markerSize + 20, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(markerBitmap)

        // Configurar los paints
        val borderPaint = Paint().apply {
            color = if (isRival) Color.parseColor("#38A169") else Color.parseColor("#fc6603") // Verde para rivales, rojo para usuario
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val innerPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val imagePaint = Paint().apply {
            isAntiAlias = true
        }

        // Calcular el centro del círculo
        val centerX = markerSize / 2f
        val centerY = markerSize / 2f
        val outerRadius = (markerSize / 2f) * 0.9f
        val innerRadius = outerRadius * 0.85f
        val imageRadius = innerRadius * 0.8f

        // Dibujar el círculo exterior (borde)
        canvas.drawCircle(centerX, centerY, outerRadius, borderPaint)

        // Dibujar el círculo interior (fondo blanco)
        canvas.drawCircle(centerX, centerY, innerRadius, innerPaint)

        // Redimensionar y hacer circular la imagen de perfil
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, (imageRadius * 2).toInt(), (imageRadius * 2).toInt(), true)
        val circularBitmap = createCircularBitmap(scaledBitmap)

        // Dibujar la imagen circular en el centro
        val imageLeft = centerX - imageRadius
        val imageTop = centerY - imageRadius
        canvas.drawBitmap(circularBitmap, imageLeft, imageTop, imagePaint)

        // Dibujar la parte puntiaguda del marcador (como un pin)
        val path = Path()
        val bottomPointX = centerX
        val bottomPointY = centerY + outerRadius + 10  // Era + 15
        val leftPointX = centerX - 10  // Era - 15
        val rightPointX = centerX + 10  // Era + 15
        val leftPointY = centerY + outerRadius - 7  // Era - 10
        val rightPointY = centerY + outerRadius - 7  // Era - 10

        path.moveTo(bottomPointX, bottomPointY)
        path.lineTo(leftPointX, leftPointY)
        path.lineTo(rightPointX, rightPointY)
        path.close()

        canvas.drawPath(path, borderPaint)

        return markerBitmap
    }

    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    fun findAddress(location: LatLng): String? {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if (addresses != null && !addresses.isEmpty()) {
            val addr = addresses.get(0)
            val locname = addr.getAddressLine(0)
            return locname
        }
        return null
    }

    fun findLocation(address: String): LatLng? {
        val addresses = geocoder.getFromLocationName(address, 2)
        if (addresses != null && !addresses.isEmpty()) {
            val addr = addresses.get(0)
            val location = LatLng(
                addr.latitude, addr.longitude
            )
            return location
        }
        return null
    }

    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val result = RADIUS_OF_EARTH_KM * c
        return Math.round(result * 100.0) / 100.0
    }

    private fun observarParticipantes() {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("usuarios")

        FirebaseDatabase.getInstance().getReference("carreras").child(carreraId).child("jugadores")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val participantes = snapshot.children.mapNotNull {
                        val value = it.value
                        if (value is String) value else null
                    }

                    for (uid in participantes) {
                        if (uid != userId) {
                            // Primero obtener la foto de perfil del rival
                            ref.child(uid).child("urlFotoPerfil").get().addOnSuccessListener { fotoSnapshot ->
                                val urlFotoPerfil = fotoSnapshot.getValue(String::class.java)

                                // Luego observar la ubicación del rival
                                ref.child(uid).child("ubicacion")
                                    .addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(snap: DataSnapshot) {
                                            val lat = snap.child("latitud").getValue(Double::class.java) ?: return
                                            val lon = snap.child("longitud").getValue(Double::class.java) ?: return
                                            val alt = snap.child("altitud").getValue(Double::class.java) ?: 0.0

                                            // Remover marcador anterior si existe
                                            participantMarkers[uid]?.let { oldMarker ->
                                                map.overlays.remove(oldMarker)
                                            }

                                            val punto = GeoPoint(lat, lon, alt)

                                            if (!urlFotoPerfil.isNullOrEmpty()) {
                                                // Si tiene foto de perfil, cargarla
                                                loadRivalProfileImageAsMarker(punto, uid, urlFotoPerfil)
                                            } else {
                                                // Si no tiene foto, usar icono por defecto
                                                createAndAddRivalMarkerWithDefaultIcon(punto, uid)
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }.addOnFailureListener {
                                // Si falla obtener la foto, observar ubicación con icono por defecto
                                ref.child(uid).child("ubicacion")
                                    .addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(snap: DataSnapshot) {
                                            val lat = snap.child("latitud").getValue(Double::class.java) ?: return
                                            val lon = snap.child("longitud").getValue(Double::class.java) ?: return
                                            val alt = snap.child("altitud").getValue(Double::class.java) ?: 0.0

                                            participantMarkers[uid]?.let { oldMarker ->
                                                map.overlays.remove(oldMarker)
                                            }

                                            val punto = GeoPoint(lat, lon, alt)
                                            createAndAddRivalMarkerWithDefaultIcon(punto, uid)
                                        }

                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadRivalProfileImageAsMarker(location: GeoPoint, uid: String, imageUrl: String) {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .placeholder(R.drawable.baseline_bike_scooter_24)
            .error(R.drawable.baseline_bike_scooter_24)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val resizedBitmap = resizeBitmapForMarker(resource, true) // true = rival (verde)
                    createRivalMarkerWithCustomIcon(location, uid, resizedBitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    createAndAddRivalMarkerWithDefaultIcon(location, uid)
                }
            })
    }

    private fun createRivalMarkerWithCustomIcon(location: GeoPoint, uid: String, bitmap: Bitmap) {
        val marker = Marker(map)
        marker.title = "Rival"
        marker.subDescription = "Participante"
        marker.position = location
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        val drawable = BitmapDrawable(resources, bitmap)
        marker.icon = drawable

        participantMarkers[uid] = marker
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun createAndAddRivalMarkerWithDefaultIcon(location: GeoPoint, uid: String) {
        val marker = createMarker(
            location,
            "Rival",
            "Participante",
            R.drawable.baseline_bike_scooter_24
        )

        marker?.let {
            participantMarkers[uid] = it
            map.overlays.add(it)
            map.invalidate()
        }
    }
    
    private fun observarEstadoCarrera() {
        val ref = FirebaseDatabase.getInstance().getReference("carreras").child(carreraId).child("estado")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var estadoCarrera = snapshot.getValue(String::class.java)
                carreraEnCurso = snapshot.getValue(String::class.java) == "en_curso"
                if(estadoCarrera == "finalizada") {
                    startActivity(Intent(baseContext, HomeActivity::class.java))
                }
                else if (estadoCarrera == "en_curso"){
                    binding.goOnlyButton.text = "FINISH RACE"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun iniciarCronometro() {
        tiempoInicioCarrera = System.currentTimeMillis()
        tiempoActividad = 0

        cronometroRunnable = object : Runnable {
            override fun run() {
                val tiempoTranscurrido = (System.currentTimeMillis() - tiempoInicioCarrera) / 1000
                tiempoActividad = tiempoTranscurrido


                // Continuar el cronómetro cada segundo
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(cronometroRunnable!!)
    }


    private fun detenerCronometro() {
        cronometroRunnable?.let { handler.removeCallbacks(it) }
        cronometroRunnable = null
    }

    private fun formatearTiempo(segundos: Long): String {
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val segs = segundos % 60
        return String.format("%02d:%02d:%02d", horas, minutos, segs)
    }

}